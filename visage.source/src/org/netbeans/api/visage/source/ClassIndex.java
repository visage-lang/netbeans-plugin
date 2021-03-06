/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.api.visage.source;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import org.netbeans.api.java.classpath.ClassPath;

import javax.lang.model.element.TypeElement;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.modules.visage.source.indexing.IndexingUtilities;
import org.netbeans.modules.visage.source.indexing.VisageIndexer;
import org.netbeans.modules.parsing.impl.indexing.PathRecognizerRegistry;
import org.netbeans.modules.parsing.impl.indexing.PathRegistry;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.parsing.spi.indexing.support.QuerySupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import static org.netbeans.modules.parsing.spi.indexing.support.QuerySupport.Kind;

import org.openide.util.Exceptions;

/**
 * A Visage class index that, for given class path, provide searching services
 * over both java and visage classes.
 * @author nenik
 */
final public class ClassIndex {

    final private static java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ClassIndex.class.getName());
    private org.netbeans.api.java.source.ClassIndex javaIndex;

    private static Set<String> javaSearchKinds = new HashSet<String>() {
        {
            for(org.netbeans.api.java.source.ClassIndex.SearchKind value : org.netbeans.api.java.source.ClassIndex.SearchKind.values()) {
                add(value.name());
            }
        }
    };

    /**
     * Encodes a type of the name kind used by 
     * {@link ClassIndex#getDeclaredTypes} method.
     *
     */
    public enum NameKind {

        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes}
         * is an exact simple name of the package or declared type.
         */
        SIMPLE_NAME,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} 
         * is an case sensitive prefix of the package or declared type name.
         */
        PREFIX,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} is 
         * an case insensitive prefix of the declared type name.
         */
        CASE_INSENSITIVE_PREFIX,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} is 
         * an camel case of the declared type name.
         */
        CAMEL_CASE,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} is 
         * an regular expression of the declared type name.
         */
        REGEXP,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} is 
         * an case insensitive regular expression of the declared type name.
         */
        CASE_INSENSITIVE_REGEXP,
        /**
         * The name parameter of the {@link ClassIndex#getDeclaredTypes} is 
         * a camel case or case insensitive prefix of the declared type name.
         * For example all these names NPE, NulPoEx, NULLPOInter leads to NullPointerException returned.
         * @since 0.28.0
         */
        CAMEL_CASE_INSENSITIVE,
        /**
         * The name parameter is the exact declared type FQN
         */
        EXACT
    };

    /**
     * Scope used by {@link ClassIndex} to search in
     */
    public enum SearchScope {

        /**
         * Search is done in source path
         */
        SOURCE,
        /**
         * Search is done in compile and boot path
         */
        DEPENDENCIES
    };

    /**
     * Encodes a reference type,
     * used by {@link ClassIndex#getElements} and {@link ClassIndex#getResources}
     * to restrict the search.
     */
    public enum SearchKind {

        /**
         * The returned class has to extend or implement given element
         */
        IMPLEMENTORS,
        /**
         * The returned class has to call method on given element
         */
        METHOD_REFERENCES,
        /**
         * The returned class has to access a field on given element
         */
        FIELD_REFERENCES,
        /**
         * The returned class contains references to the element type
         */
        TYPE_REFERENCES,
        TYPE_DEFS,
        PACKAGES
    };

    final private ReentrantReadWriteLock cpLock = new ReentrantReadWriteLock();

    final private Set<FileObject> indexerSrcRoots = new HashSet<FileObject>();
    final private Set<FileObject> indexerDepRoots = new HashSet<FileObject>();
    final private Set<FileObject> indexerDepRevRoots = new HashSet<FileObject>();

    private ClassIndex(final ClassPath bootPath, final ClassPath classPath, final ClassPath sourcePath) {
        assert bootPath != null;
        assert classPath != null;
        assert sourcePath != null;

        applyClassPaths(sourcePath, classPath, bootPath);
    }

    public static ClassIndex forClasspathInfo(ClasspathInfo cpi) {
        return new ClassIndex(
            cpi.getClassPath(ClasspathInfo.PathKind.BOOT),
            cpi.getClassPath(ClasspathInfo.PathKind.COMPILE),
            cpi.getClassPath(ClasspathInfo.PathKind.SOURCE));
    }

    void applyClassPaths(ClassPath sourcePath, ClassPath compilePath, ClassPath bootPath) {
        try {
            cpLock.writeLock().lock();
            Set<FileObject> srcRoots = new HashSet();
            Set<FileObject> depRoots = new HashSet();
            srcRoots.addAll(sourcePath != null ? Arrays.asList(sourcePath.getRoots()) : Collections.EMPTY_SET);
            depRoots.addAll(compilePath != null ? Arrays.asList(compilePath.getRoots()) : Collections.EMPTY_SET);
            depRoots.addAll(bootPath != null ? Arrays.asList(bootPath.getRoots()) : Collections.EMPTY_SET);

            // #186163: need to add the new srcRoots before removing the unused ones
            getSrcRoots(true).retainAll(srcRoots);

            // #186163: need to add the new srcRoots before removing the unused ones
            getDepRoots(true).retainAll(depRoots);

            preferSources(getDepRoots());
            javaIndex = org.netbeans.api.java.source.ClasspathInfo.create(bootPath, compilePath, sourcePath).getClassIndex();
        } finally {
            cpLock.writeLock().unlock();
        }
    }

    /**
     * Returns {@link ElementHandle}s for all declared types in given classpath corresponding to the name.
     * All the types belonging to the JDK, Visage SDK and user classes are
     * considered, but even java types are returned in the form of Visage
     * ElementHandle.
     * 
     * @param name case sensitive prefix, case insensitive prefix, exact simple name,
     * camel case or regular expression depending on the kind parameter.
     * @param kind of the name {@see NameKind}
     * @param scope to search in {@see SearchScope}
     * @return set of all matched declared types
     * It may return null when the caller is a CancellableTask&lt;CompilationInfo&gt; and is cancelled
     * inside call of this method.
     */
    public Set<ElementHandle<TypeElement>> getDeclaredTypes(final String name, NameKind kind, Set<SearchScope> scope) {
        assert name != null;
        assert kind != null;
        try {
            cpLock.readLock().lock();
            final Set<ElementHandle<TypeElement>> result = new HashSet<ElementHandle<TypeElement>>();

            // get the partial result from java support:
            Set<?> javaEl = javaIndex.getDeclaredTypes(name, toJavaNameKind(kind), toJavaScope(scope));
            for (Object o : javaEl) {
                @SuppressWarnings("unchecked")
                ElementHandle<TypeElement> elem = ElementHandle.fromJava(
                        (org.netbeans.api.java.source.ElementHandle<TypeElement>) o);
                result.add(elem);
            }

            String searchValue = name;
            QuerySupport.Kind queryKind;
            VisageIndexer.IndexKey indexKey = VisageIndexer.IndexKey.CLASS_NAME_SIMPLE;

            switch (kind) {
                case CAMEL_CASE:
                    queryKind = QuerySupport.Kind.CAMEL_CASE;
                    break;
                case CAMEL_CASE_INSENSITIVE:
                    queryKind = QuerySupport.Kind.CASE_INSENSITIVE_CAMEL_CASE;
                    break;
                case CASE_INSENSITIVE_PREFIX:
                    searchValue = searchValue.toLowerCase(); // case-insensitive index uses all-lower case
                    indexKey = VisageIndexer.IndexKey.CLASS_NAME_INSENSITIVE;
                    queryKind = QuerySupport.Kind.CASE_INSENSITIVE_PREFIX;
                    break;
                case CASE_INSENSITIVE_REGEXP:
                    queryKind = QuerySupport.Kind.CASE_INSENSITIVE_REGEXP;
                    break;
                case PREFIX:
                    queryKind = QuerySupport.Kind.PREFIX;
                    break;
                case REGEXP:
                    queryKind = QuerySupport.Kind.REGEXP;
                    indexKey = VisageIndexer.IndexKey.CLASS_FQN;
                    break;
                case EXACT:
                    indexKey = VisageIndexer.IndexKey.CLASS_FQN;
                    queryKind = QuerySupport.Kind.EXACT;
                    break;
                default:
                    queryKind = QuerySupport.Kind.EXACT;
            }


            try {
                QuerySupport query = getQuery(scope);
                for (IndexResult ir : query.query(indexKey.toString(), searchValue, queryKind, VisageIndexer.IndexKey.CLASS_FQN.toString())) {
                    for (String fqn : matches(name, queryKind, false, ir.getValues(VisageIndexer.IndexKey.CLASS_FQN.toString()))) {
                        ElementHandle<TypeElement> handle = new ElementHandle<TypeElement>(ElementKind.CLASS, new String[]{fqn});
                        result.add(handle);
                    }
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }
            LOG.log(Level.FINER, "ClassIndex.getDeclaredTypes returned {0} elements\n", result.size());
            return result;
        } finally {
            cpLock.readLock().unlock();
        }
    }

    /**
     * Returns names af all packages in given classpath starting with prefix.
     * @param prefix of the package name
     * @param directOnly if true treats the packages as folders and returns only
     * the nearest component of the package.
     * @param scope to search in {@see SearchScope}
     * @return set of all matched package names
     * It may return null when the caller is a CancellableTask&lt;CompilationInfo&gt; and is cancelled
     * inside call of this method.
     */
    public Set<String> getPackageNames(final String prefix, boolean directOnly, final Set<SearchScope> scope) {
        assert prefix != null;
        try {
            cpLock.readLock().lock();
            final Set<String> result = new HashSet<String>();

            // get the partial result from java support:
            result.addAll(javaIndex.getPackageNames(prefix, directOnly, toJavaScope(scope)));

            try {
                QuerySupport query = getQuery(scope);
                for (IndexResult ir : query.query(VisageIndexer.IndexKey.PACKAGE_NAME.toString(), prefix, QuerySupport.Kind.PREFIX, VisageIndexer.IndexKey.PACKAGE_NAME.toString())) {
                    String pkgName = ir.getValue(VisageIndexer.IndexKey.PACKAGE_NAME.toString()); // only one package name per indexed document
                    if (pkgName.startsWith(prefix)) {
                        if (directOnly) {
                            if (pkgName.indexOf(".", prefix.length()) == -1) {
                                result.add(pkgName);
                            }
                        } else {
                            result.add(pkgName);
                        }
                    }
                }
            } catch (IOException e) {
                Exceptions.printStackTrace(e);
            }

            return result;
        } finally {
            cpLock.readLock().unlock();
        }
    }

    /**
     * Returns a set of {@link ElementHandle}s containing reference(s) to given element.
     * @param element for which usages should be found
     * @param searchKind type of reference, {@see SearchKind}
     * @param scope to search in {@see SearchScope}
     * @return set of {@link ElementHandle}s containing the reference(s)
     * It may return null when the caller is a CancellableTask&lt;CompilationInfo&gt; and is cancelled
     * inside call of this method.
     */
    public Set<? extends ElementHandle> getElements(final ElementHandle handle, final Set<SearchKind> searchKind, final Set<SearchScope> scope) {
        assert handle != null;
        assert handle.getSignatures()[0] != null;
        assert searchKind != null;

        try {
            cpLock.readLock().lock();
            if (!handle.getKind().isClass()  && !handle.getKind().isInterface()) {
                return Collections.EMPTY_SET;
            }

            final Set<ElementHandle> result = new HashSet<ElementHandle>();
            for(org.netbeans.api.java.source.ElementHandle<TypeElement> eh : javaIndex.getElements(handle.toJava(), toJavaSearchKind(searchKind), toJavaScope(scope))) {
                result.add(ElementHandle.fromJava(eh));
            }

            final String typeRegexp = ".*?" + escapePattern(handle.getQualifiedName()) + ";" + ".*?"; // NOI18N
            final String invocationRegex = ".+?#.+?#" + escapePattern(handle.getQualifiedName())  +"#.+";

            try {
                QuerySupport query = getUsageQuery(scope);
                for (SearchKind sk : searchKind) {

                    switch (sk) {
                        case TYPE_REFERENCES: {
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FUNCTION_DEF.toString(), typeRegexp, Kind.REGEXP, new String[]{VisageIndexer.IndexKey.FUNCTION_DEF.toString()})) {
                                for (String value : ir.getValues(VisageIndexer.IndexKey.FUNCTION_DEF.toString())) {
                                    if (value.contains(handle.getQualifiedName())) {
                                        result.add(IndexingUtilities.getLocationHandle(value));
                                    }
                                }
                            }
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FUNCTION_INV.toString(), typeRegexp, Kind.REGEXP, new String[]{VisageIndexer.IndexKey.FUNCTION_INV.toString()})) {
                                for (String value : ir.getValues(VisageIndexer.IndexKey.FUNCTION_INV.toString())) {
                                    if (value.contains(handle.getQualifiedName())) {
                                        result.add(IndexingUtilities.getLocationHandle(value));
                                    }
                                }
                            }
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FIELD_DEF.toString(), typeRegexp, Kind.REGEXP, new String[]{VisageIndexer.IndexKey.FIELD_DEF.toString()})) {
                                for (String value : ir.getValues(VisageIndexer.IndexKey.FIELD_DEF.toString())) {
                                    if (value.contains(handle.getQualifiedName())) {
                                        result.add(IndexingUtilities.getLocationHandle(value));
                                    }
                                }
                            }
                            break;
                        }
                        case METHOD_REFERENCES: {
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FUNCTION_INV.toString(), invocationRegex, Kind.REGEXP, new String[]{VisageIndexer.IndexKey.FUNCTION_INV.toString()})) {
                                for (String value : ir.getValues(VisageIndexer.IndexKey.FUNCTION_INV.toString())) {
                                    if (Pattern.matches(invocationRegex, value)) {
                                        result.add(IndexingUtilities.getLocationHandle(value));
                                    }
                                }
                            }
                        }
                        case IMPLEMENTORS: {
                            String indexingVal = IndexingUtilities.getIndexValue(handle);
                            for(IndexResult ir : query.query(VisageIndexer.IndexKey.TYPE_IMPL.toString(), indexingVal, Kind.EXACT)) {
                                result.addAll(VisageSourceUtils.getClasses(ir.getFile(), handle));
                            }
                        }
                        case PACKAGES: {
                            String indexingVal = IndexingUtilities.getIndexValue(handle);
                            for(IndexResult ir : query.query(VisageIndexer.IndexKey.PACKAGE_NAME.toString(), indexingVal, Kind.PREFIX)) {
                                for (String value : ir.getValues(VisageIndexer.IndexKey.PACKAGE_NAME.toString())) {
                                    result.add(IndexingUtilities.getLocationHandle(value));
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }

            return result;
        } finally {
            cpLock.readLock().unlock();
        }
    }

    /**
     * Returns a set of source files containing reference(s) to given element.
     * @param element for which usages should be found
     * @param searchKind type of reference, {@see SearchKind}
     * @param scope to search in {@see SearchScope}
     * @return set of {@link FileObject}s containing the reference(s)
     * It may return null when the caller is a CancellableTask&lt;CompilationInfo&gt; and is cancelled
     * inside call of this method.
     */
    public Set<FileObject> getResources (final ElementHandle<? extends Element> handle, final Set<SearchKind> searchKind, final Set<SearchScope> scope) {
        assert handle != null;
        assert handle.getSignatures()[0] != null;
        assert searchKind != null;

        try {
            cpLock.readLock().lock();
            final Set<FileObject> result = new HashSet<FileObject>();
            for(FileObject fo : javaIndex.getResources(handle.toJava(), toJavaSearchKind(searchKind), toJavaScope(scope))) {
                result.add(fo);
            }

            final String typeDef = (handle.getKind().isClass() || handle.getKind().isInterface()) ? handle.getQualifiedName() : "";

    //        final String typeRefRegexp = ".*?" + escapePattern(handle.getQualifiedName()) + ";" + ".*?"; // NOI18N
            final String indexingVal = IndexingUtilities.getIndexValue(handle);

            try {
                QuerySupport query = getUsageQuery(scope);
                for (SearchKind sk : searchKind) {

                    switch (sk) {
                        case TYPE_REFERENCES: {
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.TYPE_REF.toString(), typeDef, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            break;
                        }
                        case IMPLEMENTORS: {
                            for(IndexResult ir : query.query(VisageIndexer.IndexKey.TYPE_IMPL.toString(), indexingVal, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            break;
                        }
                        case METHOD_REFERENCES: {
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FUNCTION_DEF.toString(), indexingVal, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FUNCTION_INV.toString(), indexingVal, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            break;
                        }
                        case FIELD_REFERENCES: {
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FIELD_DEF.toString(), indexingVal, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            for (IndexResult ir : query.query(VisageIndexer.IndexKey.FIELD_REF.toString(), indexingVal, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            break;
                        }
                        case TYPE_DEFS: {
                            for(IndexResult ir : query.query(VisageIndexer.IndexKey.CLASS_FQN.toString(), typeDef, Kind.EXACT)) {
                                result.add(ir.getFile());
                            }
                            break;
                        }
                        case PACKAGES: {
                            for(IndexResult ir : query.query(VisageIndexer.IndexKey.PACKAGE_NAME.toString(), indexingVal, Kind.PREFIX)) {
                                result.add(ir.getFile());
                            }
                        }
                    }
                }
            } catch (IOException e) {
            }

            return result;
        } finally {
            cpLock.readLock().unlock();
        }
    }

    public Set<FileObject> getDependencyClosure(final ElementHandle<? extends Element> handle) {
        final Set<FileObject> closure = new HashSet<FileObject>();
        final Deque<ElementHandle<? extends Element>> toProcess = new ArrayDeque<ElementHandle<? extends Element>>();

        try {
            cpLock.readLock().lock();
            toProcess.push(handle);

            while (!toProcess.isEmpty()) {
                ElementHandle<? extends Element> processing = toProcess.pop();
                final String indexingVal = IndexingUtilities.getIndexValue(processing);
                QuerySupport query = getUsageQuery(EnumSet.allOf(SearchScope.class));
                for (IndexResult ir : query.query(VisageIndexer.IndexKey.TYPE_REF.toString(), indexingVal, Kind.EXACT)) {
                    if (ir.getFile() == null) continue;
                    if (!closure.contains(ir.getFile())) {
                        closure.add(ir.getFile());
                        for(String typeName : ir.getValues(VisageIndexer.IndexKey.CLASS_FQN.toString())) {
                            toProcess.push(IndexingUtilities.getTypeHandle(typeName));
                        }
                    }
                }
            }
        } catch (IOException iOException) {
            return Collections.EMPTY_SET;
        } finally {
            cpLock.readLock().unlock();
        }
        return closure;
    }

    public Set<FileObject> getInheritanceClosure(final ElementHandle<? extends Element> handle) {
        final Set<FileObject> closure = new HashSet<FileObject>();
        final Deque<ElementHandle<? extends Element>> toProcess = new ArrayDeque<ElementHandle<? extends Element>>();

        try {
            cpLock.readLock().lock();
            toProcess.push(handle);

            while (!toProcess.isEmpty()) {
                ElementHandle<? extends Element> processing = toProcess.pop();
                final String indexingVal = IndexingUtilities.getIndexValue(processing);
                QuerySupport query = getUsageQuery(EnumSet.allOf(SearchScope.class));
                for (IndexResult ir : query.query(VisageIndexer.IndexKey.TYPE_IMPL.toString(), indexingVal, Kind.EXACT)) {
                    if (!closure.contains(ir.getFile())) {
                        closure.add(ir.getFile());
                        for(String typeName : ir.getValues(VisageIndexer.IndexKey.CLASS_FQN.toString())) {
                            toProcess.push(IndexingUtilities.getTypeHandle(typeName));
                        }
                    }
                }
            }
        } catch (IOException iOException) {
            return Collections.EMPTY_SET;
        } finally {
            cpLock.readLock().unlock();
        }
        return closure;
    }

    private QuerySupport getQuery(Set<SearchScope> scope) throws IOException {
        Collection<FileObject> roots = getRoots(scope, false);
        return QuerySupport.forRoots(VisageIndexer.NAME, VisageIndexer.VERSION, roots.toArray(new FileObject[roots.size()]));
    }

    private QuerySupport getUsageQuery(Set<SearchScope> scope) throws IOException {
        Collection<FileObject> roots = new ArrayList<FileObject>();
        roots.addAll(getRoots(scope, true));
        roots.addAll(getRoots(scope, false));
        return QuerySupport.forRoots(VisageIndexer.NAME, VisageIndexer.VERSION, roots.toArray(new FileObject[roots.size()]));
    }

    private Collection<FileObject> getRoots(Set<SearchScope> scope, boolean reverse) {
        Set<FileObject> roots = new HashSet<FileObject>();
        for (SearchScope ss : scope) {
            switch (ss) {
                case SOURCE: {
                    roots.addAll(getSrcRoots());
                    break;
                }
                case DEPENDENCIES: {
                    roots.addAll(reverse ? getDepRevRoots() : getDepRoots());
                    break;
                }
            }
        }

        return roots;
    }

    private Set<FileObject> getSrcRoots() {
        return getSrcRoots(false);
    }
    
    private Set<FileObject> getSrcRoots(boolean refresh) {
        synchronized(indexerSrcRoots) {
            if (indexerSrcRoots.isEmpty() || refresh) {
                if (refresh) {
                    indexerSrcRoots.clear();
                }
                for (String classpathId : PathRecognizerRegistry.getDefault().getSourceIds()) {
                    collectRoots(classpathId, indexerSrcRoots);
                }
            }
        }
        return indexerSrcRoots;
    }

    private Set<FileObject> getDepRoots() {
        return getDepRoots(false);
    }

    private Set<FileObject> getDepRoots(boolean refresh) {
        synchronized(indexerDepRoots) {
            if (indexerDepRoots.isEmpty() || refresh) {
                if (refresh) {
                    indexerDepRoots.clear();
                }
                for (String classpathId : PathRecognizerRegistry.getDefault().getLibraryIds()) {
                    collectRoots(classpathId, indexerDepRoots);
                }
                for (String classpathId : PathRecognizerRegistry.getDefault().getBinaryLibraryIds()) {
                    collectRoots(classpathId, indexerDepRoots);
                }
            }
        }
        return indexerDepRoots;
    }

    private Set<FileObject> getDepRevRoots() {
        return getDepRevRoots(false);
    }

    private Set<FileObject> getDepRevRoots(boolean refresh) {
        synchronized(indexerDepRevRoots) {
            if (indexerDepRevRoots.isEmpty() || refresh) {
                if (refresh) {
                    indexerDepRevRoots.clear();
                }
                Set<Map.Entry<URL, List<URL>>> rootDeps = IndexingController.getDefault().getRootDependencies().entrySet();
                Set<FileObject> srcs = new HashSet<FileObject>();
                Set<FileObject> newSrcs = new HashSet<FileObject>(getSrcRoots());
                do {
                    srcs.clear();
                    srcs.addAll(newSrcs);
                    newSrcs.clear();
                    for(FileObject fo : srcs) {
                        if (fo == null) continue; // don't process NULL values (no idea why they are here)
                        for(Map.Entry<URL, List<URL>> entry : rootDeps) {
                            try {
                                if (entry != null) {
                                    List<URL> urls = entry.getValue();
                                    if (urls != null) {
                                        URL url = fo.getURL();
                                        if (url != null && urls.contains(fo.getURL())) {
                                            FileObject src = FileUtil.toFileObject(FileUtil.archiveOrDirForURL(entry.getKey()));
                                            if (!indexerDepRevRoots.contains(src)) {
                                                indexerDepRevRoots.add(src);
                                                newSrcs.add(src);
                                            }
                                        }
                                    }
                                }
                            } catch (FileStateInvalidException e) {
                                LOG.log(Level.SEVERE, null, e);
                            }
                        }
                    }
                } while (!newSrcs.isEmpty());
            }
        }
        return indexerDepRevRoots;
    }

    private static void collectRoots(String classpathId, final Collection<FileObject> roots) {
        Set<URL> urls = PathRegistry.getDefault().getRootsMarkedAs(classpathId);
        for (URL url : urls) {
            FileObject f = URLMapper.findFileObject(url);
            if (f != null) {
                roots.add(f);
            }
        }
    }

    private static void preferSources(Collection<FileObject> roots) {
        List<FileObject> newRoots = new ArrayList();
        for (Iterator<FileObject> iter = roots.iterator();iter.hasNext();) {
            try {
                FileObject root = iter.next();
                SourceForBinaryQuery.Result2 rslt = SourceForBinaryQuery.findSourceRoots2(root.getURL());
                if (rslt.preferSources()) {
                    iter.remove();
                    newRoots.addAll(Arrays.asList(rslt.getRoots()));
                }
            } catch (IOException e) {
            }
        }
        roots.addAll(newRoots);
    }

    private static Collection<String> matches(String pattern, Kind queryKind, boolean isFqn, String[] fqns) {
        Collection<String> result = new ArrayList<String>();
        Pattern regex = null;

        switch (queryKind) {
            case EXACT: {
                regex = Pattern.compile(isFqn ? escapePattern(pattern) : ".*?" + pattern); // fqn or simple class name
                break;
            }
            case PREFIX:
                if (pattern.length() == 0) {
                    //Special case (all) handle in different way
                    regex = Pattern.compile(".*");
                } else {
                    regex = Pattern.compile("(.+\\.)?" + escapePattern(pattern) + ".*");
                }
                break;
            case CASE_INSENSITIVE_PREFIX:
                if (pattern.length() == 0) {
                    //Special case (all) handle in different way
                    regex = Pattern.compile(".*");
                } else {
                    regex = Pattern.compile("(.+\\.)?" + escapePattern(pattern) + ".*", Pattern.CASE_INSENSITIVE);
                }
                break;
            case CAMEL_CASE:
            case CASE_INSENSITIVE_CAMEL_CASE:
                if (pattern.length() == 0) {
                    //Special case (all) handle in different way
                    regex = Pattern.compile(".*");
                } {
                StringBuilder sb = new StringBuilder("(.+\\.)?");
//                        String prefix = null;
                int lastIndex = 0;
                int index;
                do {
                    index = findNextUpper(pattern, lastIndex + 1);
                    String token = pattern.substring(lastIndex, index == -1 ? pattern.length() : index);
//                            if ( lastIndex == 0 ) {
//                                prefix = token;
//                            }
                    sb.append(token);
                    sb.append(index != -1 ? "[\\p{javaLowerCase}\\p{Digit}_\\$]*" : ".*"); // NOI18N
                    lastIndex = index;
                } while (index != -1);

                regex = Pattern.compile(sb.toString());
            }
            break;
            case CASE_INSENSITIVE_REGEXP:
                if (pattern.length() == 0) {
                    throw new IllegalArgumentException();
                } else {
                    regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    break;
                }
            case REGEXP:
                if (pattern.length() == 0) {
                    throw new IllegalArgumentException();
                } else {
                    regex = Pattern.compile(pattern);
                    break;
                }
            default:
                throw new UnsupportedOperationException(queryKind.toString());
        }
        for (String value : fqns) {
            if (regex != null && regex.matcher(value).matches()) {
                result.add(value);
            }
        }
        return result;
    }

    private static String escapePattern(String plainText) {
        return plainText.replace(".", "\\.");
    }

    private static int findNextUpper(String text, int offset) {
        for (int i = offset; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static Set<org.netbeans.api.java.source.ClassIndex.SearchScope> toJavaScope(Set<SearchScope> scopes) {
        Set<org.netbeans.api.java.source.ClassIndex.SearchScope> cScopes = EnumSet.noneOf(org.netbeans.api.java.source.ClassIndex.SearchScope.class);
        for (SearchScope scope : scopes) {
            cScopes.add(org.netbeans.api.java.source.ClassIndex.SearchScope.valueOf(scope.name()));
        }
        return cScopes;
    }

    private static Set<org.netbeans.api.java.source.ClassIndex.SearchKind> toJavaSearchKind(Set<SearchKind> kinds) {
        Set<org.netbeans.api.java.source.ClassIndex.SearchKind> cKinds = EnumSet.noneOf(org.netbeans.api.java.source.ClassIndex.SearchKind.class);
        for (SearchKind kind : kinds) {
            if (javaSearchKinds.contains(kind.name())) {
                cKinds.add(org.netbeans.api.java.source.ClassIndex.SearchKind.valueOf(kind.name()));
            }
        }
        return cKinds;
    }

    private static org.netbeans.api.java.source.ClassIndex.NameKind toJavaNameKind(NameKind kind) {
        return kind == NameKind.EXACT ? org.netbeans.api.java.source.ClassIndex.NameKind.SIMPLE_NAME : org.netbeans.api.java.source.ClassIndex.NameKind.valueOf(kind.name());
    }

    private static <T> void addAndRemove(Collection<T> target, Collection<T> newCol) {
        target.addAll(newCol);
    }
}
