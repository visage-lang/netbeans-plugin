/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.api.javafx.source;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import org.netbeans.api.java.classpath.ClassPath;

import javax.lang.model.element.TypeElement;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;
import org.netbeans.api.project.ProjectManager;
import org.openide.util.Exceptions;

/**
 * A JavaFX class index that, for given class path, provide searching services
 * over both java and javafx classes.
 * @author nenik
 */
public class ClassIndex {
    //INV: Never null
    private final ClassPath bootPath;
    //INV: Never null
    private final ClassPath classPath;
    //INV: Never null
    private final ClassPath sourcePath;

    //INV: Never null
    //@GuardedBy (this)
    private final Set<Impl> sourceIndices;
    //INV: Never null
    //@GuardedBy (this)
    private final Set<Impl> depsIndices;

    private final org.netbeans.api.java.source.ClassIndex javaIndex;
    
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
        CAMEL_CASE_INSENSITIVE
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

    ClassIndex(final ClassPath bootPath, final ClassPath classPath, final ClassPath sourcePath) {
        assert bootPath != null;
        assert classPath != null;
        assert sourcePath != null;
        this.bootPath = bootPath;
        this.classPath = classPath;
        this.sourcePath = sourcePath;
        this.depsIndices = new HashSet<Impl>();
        this.sourceIndices = new HashSet<Impl>();
      
        javaIndex = org.netbeans.api.java.source.ClasspathInfo.create(bootPath, classPath, sourcePath).getClassIndex();
/*        this.oldBoot = new HashSet<URL>();
        this.oldCompile = new  HashSet<URL>();
        this.oldSources = new HashSet<URL>();
        
        final ClassIndexManager manager = ClassIndexManager.getDefault();
        manager.addClassIndexManagerListener(WeakListeners.create(ClassIndexManagerListener.class, (ClassIndexManagerListener) this.spiListener, manager));
        this.bootPath.addPropertyChangeListener(WeakListeners.propertyChange(spiListener, this.bootPath));
        this.classPath.addPropertyChangeListener(WeakListeners.propertyChange(spiListener, this.classPath));
        this.sourcePath.addPropertyChangeListener(WeakListeners.propertyChange(spiListener, this.sourcePath));                */
        reset (true, true);
    }

    /**
     * Returns {@link ElementHandle}s for all declared types in given classpath corresponding to the name.
     * All the types belonging to the JDK, JavaFX SDK and user classes are
     * considered, but even java types are returned in the form of JavaFX
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
    public Set<ElementHandle<TypeElement>> getDeclaredTypes (final String name, final NameKind kind, final Set<SearchScope> scope) {
        assert name != null;
        assert kind != null;
        final Set<ElementHandle<TypeElement>> result = new HashSet<ElementHandle<TypeElement>>();

        // get the partial result from java support:
        Set<?> javaEl = javaIndex.getDeclaredTypes(name, toJava(kind), toJava(scope));
        for (Object o : javaEl) {
            result.add(ElementHandle.fromJava((org.netbeans.api.java.source.ElementHandle)o));
        }
        
        // and TODO add our data:
        final Iterable<? extends Impl> queries = getQueries (scope);        
//        final ResultConvertor<ElementHandle<TypeElement>> thConvertor = ResultConvertor.elementHandleConvertor();
        try {
            for (Impl query : queries) {
                try {
                    query.getDeclaredTypes (name, kind, result);
//                } catch (ClassIndexImpl.IndexAlreadyClosedException e) {
//                    logClosedIndex (query);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }
//            LOGGER.fine(String.format("ClassIndex.getDeclaredTypes returned %d elements\n", result.size()));
        } catch (InterruptedException e) {
            return null;
        }
        return result;
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
    public Set<String> getPackageNames (final String prefix, boolean directOnly, final Set<SearchScope> scope) {
        assert prefix != null;
        final Set<String> result = new HashSet<String> ();        

        // get the partial result from java support:
        result.addAll(javaIndex.getPackageNames(prefix, directOnly, toJava(scope)));

        // and TODO add our data:
        
        final Iterable<? extends ClassIndex.Impl> queries = getQueries (scope);
        try {
            for (ClassIndex.Impl query : queries) {
                try {
                    query.getPackageNames (prefix, directOnly, result);
//                } catch (ClassIndexImpl.IndexAlreadyClosedException e) {
//                    logClosedIndex (query);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }
            return Collections.unmodifiableSet(result);
        } catch (InterruptedException e) {
            return null;
        }
    }
    
    private static Set<org.netbeans.api.java.source.ClassIndex.SearchScope> toJava(Set<SearchScope> scopes) {
        Set<org.netbeans.api.java.source.ClassIndex.SearchScope> cScopes = EnumSet.noneOf(org.netbeans.api.java.source.ClassIndex.SearchScope.class);
        for (SearchScope scope : scopes) cScopes.add(org.netbeans.api.java.source.ClassIndex.SearchScope.valueOf(scope.toString()));
        return cScopes;
    }

    private org.netbeans.api.java.source.ClassIndex.NameKind toJava(NameKind kind) {
        return org.netbeans.api.java.source.ClassIndex.NameKind.valueOf(kind.name());
    }

    private synchronized Iterable<? extends Impl> getQueries (final Set<SearchScope> scope) {        
        Set<Impl> result = new HashSet<Impl> ();
        if (scope.contains(SearchScope.SOURCE)) {            
            result.addAll(this.sourceIndices);
        }        
        if (scope.contains(SearchScope.DEPENDENCIES)) {
            result.addAll(this.depsIndices);
        }
        return result;
    }

    
    private void reset (final boolean source, final boolean deps) {
        ProjectManager.mutex().readAccess(new Runnable() {
            public void run() {
                synchronized (ClassIndex.this) {
                    if (source) {            
//                        for (Impl impl : sourceIndices) {
//                            impl.removeClassIndexImplListener(spiListener);
//                        }
                        sourceIndices.clear();
                        createQueriesForRoots (sourcePath, true, sourceIndices);
                    }
                    if (deps) {
//                        for (Impl impl : depsIndeces) {
//                            impl.removeClassIndexImplListener(spiListener);
//                        }
                        depsIndices.clear();
                        createQueriesForRoots (bootPath, false, depsIndices);
                        createQueriesForRoots (classPath, false, depsIndices);	    
                    }
                }
            }
        });        
    }
        
    private void createQueriesForRoots (final ClassPath cp, final boolean sources, final Set<? super ClassIndex.Impl> queries) {
//        final GlobalSourcePath gsp = GlobalSourcePath.getDefault();
        List<ClassPath.Entry> entries = cp.entries();
	for (ClassPath.Entry entry : entries) {
            URL[] srcRoots;
//            if (!sources) {
//                srcRoots = gsp.getSourceRootForBinaryRoot (entry.getURL(), cp, true);
//                if (srcRoots == null) {
//                    srcRoots = new URL[] {entry.getURL()};
//                }
//            }
//            else {
                srcRoots = new URL[] {entry.getURL()};
//            }                
            for (URL srcRoot : srcRoots) {
                try {
                    ClassIndex.Impl ci = ClassIndexManager.createUsagesQuery(srcRoot, sources);
                    if (ci != null) {
//                        ci.addClassIndexImplListener(spiListener);
                        queries.add (ci);
                    }
                } catch (IOException ioe) {
                }
            }	    
	}
    }
    
    static final class TypeHolder {
        String pkg; // "javafx.lang."
        String name; // "Number"

        public TypeHolder(String pkg, String name) {
            this.pkg = pkg;
            this.name = name;
        }
        
        public String fullName() {
            return pkg + name;
        }
        
        public @Override int hashCode() {
            return fullName().hashCode();
        }

        public @Override boolean equals(Object obj) {
            if (obj instanceof TypeHolder) return ((TypeHolder)obj).fullName().equals(fullName());
            return false;
        }
    }
    
    static class Impl {
        private final Set<String> packages = new HashSet<String>();
        private final Set<TypeHolder> types = new HashSet<TypeHolder>();

        synchronized void setTypes(Set<TypeHolder> types) {
            this.types.clear();
            this.types.addAll(types);
        }
        
        synchronized Set<TypeHolder> getTypes() {
            return new HashSet<TypeHolder>(types);
        }
        
        public synchronized void getDeclaredTypes (String name, ClassIndex.NameKind kind, final Set<? super ElementHandle<TypeElement>> result) throws IOException, InterruptedException {
            switch(kind) {
                case SIMPLE_NAME:
                    for (TypeHolder type : types) {
                        if (type.name.startsWith(name)) {
                            result.add(getType(type));
                        }
                    }
                    break;
                    
                case PREFIX:
                    for (TypeHolder type : types) {
                        if (type.name.startsWith(name)) {
                            result.add(getType(type));
                        }
                    }
                    break;
                    
                case CASE_INSENSITIVE_PREFIX:
                    String nameLower = name.toLowerCase();
                    for (TypeHolder type : types) {
                        if (type.name.toLowerCase().startsWith(nameLower)) {
                            result.add(getType(type));
                        }
                    }
                    break;
        
                default:
                    Pattern patt = createPattern(kind, name);
                    for (TypeHolder type : types) {
                        if (patt.matcher(type.name).matches()) {
                            result.add(getType(type));
                        }
                    }
                    break;
            }
        }
        
        public synchronized void getPackageNames (String prefix, boolean directOnly, Set<String> result) throws IOException, InterruptedException {
            for (String pkg : packages) {
                if (pkg.startsWith(prefix)) {
                    // filter deep packages
                    result.add(pkg);
                }
            }
        }

        private static ElementHandle<TypeElement> getType(TypeHolder type) {
            return new ElementHandle<TypeElement>(ElementKind.CLASS, new String[] {type.fullName()});
        }

        private static Pattern createPattern(NameKind kind, String name) {
            switch (kind) {
                case SIMPLE_NAME:
                    return Pattern.compile(name);
                    
                case PREFIX:
                    return Pattern.compile(name.concat(".*")); // NOI18N
                    
                case CAMEL_CASE:
                    if (name.length() == 0) throw new IllegalArgumentException ();
                    
                    StringBuilder sb = new StringBuilder();
                    int lastIndex = 0;
                    int index = 0;
                    while(index != -1) {
                        index = findNextUpper(name, lastIndex + 1);
                        String token = name.substring(lastIndex, index == -1 ? name.length(): index);
                        sb.append(token); 
                        sb.append( index != -1 ?  "[\\p{javaLowerCase}\\p{Digit}_\\$]*" : ".*"); // NOI18N         
                        lastIndex = index;
                    }
                    return Pattern.compile(sb.toString());
                    
                case CASE_INSENSITIVE_REGEXP:
                    if (name.length() == 0) throw new IllegalArgumentException ();
                    return Pattern.compile(name,Pattern.CASE_INSENSITIVE);

                case REGEXP:
                    if (name.length() == 0) throw new IllegalArgumentException ();
                    return Pattern.compile(name);
                
                case CAMEL_CASE_INSENSITIVE:
                    lastIndex = 0;
                    index = 0;
                    sb = new StringBuilder();
                    while(index != -1) {
                        index = findNextUpper(name, lastIndex + 1);
                        String token = name.substring(lastIndex, index == -1 ? name.length(): index);
                        sb.append(token); 
                        sb.append( index != -1 ?  "[\\p{javaLowerCase}\\p{Digit}_\\$]*" : ".*"); // NOI18N         
                        lastIndex = index;
                    }

                    return Pattern.compile(sb.toString());
            }
            throw new IllegalArgumentException ();
        }
        
        private static int findNextUpper(String text, int offset ) {
            for( int i = offset; i < text.length(); i++ ) {
                if ( Character.isUpperCase(text.charAt(i)) ) {
                    return i;
                }
            }
            return -1;
        }
    }
            
}
