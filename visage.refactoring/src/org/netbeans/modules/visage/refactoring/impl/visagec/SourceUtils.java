/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 *  Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 *  Other names may be trademarks of their respective owners.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common
 *  Development and Distribution License("CDDL") (collectively, the
 *  "License"). You may not use this file except in compliance with the
 *  License. You can obtain a copy of the License at
 *  http://www.netbeans.org/cddl-gplv2.html
 *  or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 *  specific language governing permissions and limitations under the
 *  License.  When distributing the software, include this License Header
 *  Notice in each file and include the License file at
 *  nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the GPL Version 2 section of the License file that
 *  accompanied this code. If applicable, add the following below the
 *  License Header, with the fields enclosed by brackets [] replaced by
 *  your own identifying information:
 *  "Portions Copyrighted [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 * 
 *  Portions Copyrighted 1997-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.visage.refactoring.impl.visagec;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.visage.lexer.VisageTokenId;
import org.netbeans.api.visage.source.ClassIndex;
import org.netbeans.api.visage.source.ClasspathInfo;
import org.netbeans.api.visage.source.ElementHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.visage.project.VisageProjectConstants;
import org.netbeans.api.visage.source.ClasspathInfoProvider;
import org.netbeans.api.visage.source.CompilationInfo;
import org.netbeans.api.visage.source.VisageSource;
import org.netbeans.api.visage.source.VisageSourceUtils;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.visage.refactoring.repository.ElementDef;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.visage.api.tree.ExpressionTree;
import org.visage.api.tree.Tree;
import org.visage.api.tree.UnitTree;
import org.visage.api.tree.VisageTreePath;
import org.visage.api.tree.VisageTreePathScanner;
import org.visage.tools.tree.VisageTree;
import org.visage.tools.tree.VisageTreeInfo;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class SourceUtils {
    final private static Logger LOG = Logger.getLogger(SourceUtils.class.getName());
    final private static boolean DEBUG = LOG.isLoggable(Level.FINEST);

    static private Object indexingController;
    static private Method dependentRoots;
    static {
        try {
            Class icClazz = Class.forName("org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController"); // NOI18N
            Method instanceCrtr = icClazz.getMethod("getDefault");
            dependentRoots = icClazz.getMethod("getRootDependencies");
            indexingController = instanceCrtr.invoke(null);
        } catch (Exception e) {
            indexingController = null;
            dependentRoots = null;
        }
    }


    public static String htmlize(String input) {
        String temp = input.replace("<", "&lt;"); // NOI18N
        temp = temp.replace(">", "&gt;"); // NOI18N
        return temp;
    }

    public static String getHtml(String text) {
        return getHtml(text, -1);
    }

    public static String getHtml(String text, int hilite) {
        StringBuffer buf = new StringBuffer();
        TokenHierarchy tokenH = TokenHierarchy.create(text, VisageTokenId.language());
        Lookup lookup = MimeLookup.getLookup(MimePath.get(VisageSourceUtils.VISAGE_MIME_TYPE));
        FontColorSettings settings = lookup.lookup(FontColorSettings.class);
        TokenSequence tok = tokenH.tokenSequence();
        while (tok.moveNext()) {
            Token<VisageTokenId> token = (Token) tok.token();
            String category = token.id().primaryCategory();
            if (category == null) {
                category = "whitespace"; //NOI18N
            }
            AttributeSet set = settings.getTokenFontColors(category);
            String htmlized = color(htmlize(token.text().toString()), set);
            if (hilite == tok.offset()) {
                buf.append("<b>").append(htmlized).append("</b>"); // NOI18N
            } else {
                buf.append(htmlized);
            }
        }
        return buf.toString();
    }

    private static String color(String string, AttributeSet set) {
        if (set==null)
            return string;
        if (string.trim().length() == 0) {
            return string.replace(" ", "&nbsp;").replace("\n", "<br>"); //NOI18N
        }
        StringBuilder buf = new StringBuilder(string);
        if (StyleConstants.isBold(set)) {
            buf.insert(0,"<b>"); //NOI18N
            buf.append("</b>"); //NOI18N
        }
        if (StyleConstants.isItalic(set)) {
            buf.insert(0,"<i>"); //NOI18N
            buf.append("</i>"); //NOI18N
        }
        if (StyleConstants.isStrikeThrough(set)) {
            buf.insert(0,"<s>"); // NOI18N
            buf.append("</s>"); // NOI18N
        }
        buf.insert(0,"<font color=" + getHTMLColor(StyleConstants.getForeground(set)) + ">"); //NOI18N
        buf.append("</font>"); //NOI18N
        return buf.toString();
    }

    private static String getHTMLColor(Color c) {
        String colorR = "0" + Integer.toHexString(c.getRed()); //NOI18N
        colorR = colorR.substring(colorR.length() - 2);
        String colorG = "0" + Integer.toHexString(c.getGreen()); //NOI18N
        colorG = colorG.substring(colorG.length() - 2);
        String colorB = "0" + Integer.toHexString(c.getBlue()); //NOI18N
        colorB = colorB.substring(colorB.length() - 2);
        String html_color = "#" + colorR + colorG + colorB; //NOI18N
        return html_color;
    }

    public static boolean isVisageFile(FileObject f) {
        return VisageSourceUtils.isVisageFile(f);
    }

    public static boolean isPlatformOk(FileObject f) {
        return VisageSourceUtils.isPlatformOk(f);
    }

    public static boolean isValidPackageName(String name) {
        if (name.endsWith(".")) //NOI18N
            return false;
        if (name.startsWith("."))  //NOI18N
            return  false;
        StringTokenizer tokenizer = new StringTokenizer(name, "."); // NOI18N
        while (tokenizer.hasMoreTokens()) {
            if (!Utilities.isJavaIdentifier(tokenizer.nextToken())) {
                return false;
            }
        }
        return true;
    }

    public static boolean isElementInOpenProject(FileObject f) {
        if (f==null)
            return false;
        Project p = FileOwnerQuery.getOwner(f);
        return isOpenProject(p);
    }

    public static boolean isFileInOpenProject(FileObject file) {
        assert file != null;
        Project p = FileOwnerQuery.getOwner(file);
        if (p == null) {
            return false;
        }
        return isOpenProject(p);
    }

    private static boolean isOpenProject(Project p) {
        return OpenProjects.getDefault().isProjectOpen(p);
    }

    public static boolean isOnSourceClasspath(FileObject fo) {
        Project p = FileOwnerQuery.getOwner(fo);
        if (p==null)
            return false;

        //workaround for 143542
        Project[] opened = OpenProjects.getDefault().getOpenProjects();
        for (Project pr : opened) {
            if (fo.isFolder()) {
                for (SourceGroup sg : ProjectUtils.getSources(pr).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
                    if (fo==sg.getRootFolder() || (FileUtil.isParentOf(sg.getRootFolder(), fo) && sg.contains(fo))) {
                        return ClassPath.getClassPath(fo, ClassPath.SOURCE) != null;
                    }
                }
            }
            // a story of interest ...
            // for some reason, right now (as of 2009/10/12) visage sources are referenced as "java sources"
            for (SourceGroup sg : ProjectUtils.getSources(pr).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
                if (fo==sg.getRootFolder() || (FileUtil.isParentOf(sg.getRootFolder(), fo) && sg.contains(fo))) {
                    return ClassPath.getClassPath(fo, ClassPath.SOURCE) != null;
                }
            }
            // ---
            for (SourceGroup sg : ProjectUtils.getSources(pr).getSourceGroups(VisageProjectConstants.SOURCES_TYPE_VISAGE)) {
                if (fo==sg.getRootFolder() || (FileUtil.isParentOf(sg.getRootFolder(), fo) && sg.contains(fo))) {
                    return ClassPath.getClassPath(fo, ClassPath.SOURCE) != null;
                }
            }
        }
        return false;
        //end of workaround
        //return ClassPath.getClassPath(fo, ClassPath.SOURCE)!=null;
    }

     public static boolean isFromLibrary(Element element, CompilationInfo cc) {
        FileObject file = getFile(element, cc);
        if (file==null) {
            //no source for given element. Element is from library
            return true;
        }
        return FileUtil.getArchiveFile(file)!=null;
    }

    public static boolean isClasspathRoot(FileObject fo) {
        ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
        return cp != null ? fo.equals(cp.findOwnerRoot(fo)) : false;
    }

    public static FileObject getClassPathRoot(URL url) throws IOException {
        FileObject result = URLMapper.findFileObject(url);
        File f = result != null ? null : FileUtil.normalizeFile(new File(URLDecoder.decode(url.getPath(), "UTF-8"))); //NOI18N
        while (result==null) {
            result = FileUtil.toFileObject(f);
            f = f.getParentFile();
        }
        return ClassPath.getClassPath(result, ClassPath.SOURCE).findOwnerRoot(result);
    }

    public static Collection<ExecutableElement> getOverridingMethods(ExecutableElement e, CompilationInfo info) {
        return VisageSourceUtils.getOverridingMethods(e, info);
    }

    public static Collection<ExecutableElement> getOverridenMethods(ExecutableElement e, CompilationInfo info) {
        return VisageSourceUtils.getOverridenMethods(e, info);
    }

    public static String getPackageName(FileObject folder) {
        assert folder.isFolder() : "argument must be folder"; // NI18N
        ClassPath cp = ClassPath.getClassPath(folder, ClassPath.SOURCE);
        if (cp == null) {
            // see http://www.netbeans.org/issues/show_bug.cgi?id=159228
            throw new IllegalStateException(String.format("No classpath for %s.", folder)); // NOI18N
        }
        return cp.getResourceName(folder, '.', false);
    }

    public static String getPackageName(UnitTree unit) {
        assert unit!=null;
        ExpressionTree name = unit.getPackageName();
        if (name==null) {
            //default package
            return ""; // NOI18N
        }
        return name.toString();
    }

    public static String getPackageName(URL url) {
        File f = null;
        try {
            String path = URLDecoder.decode(url.getPath(), "utf-8"); // NOI18N
            f = FileUtil.normalizeFile(new File(path));
        } catch (UnsupportedEncodingException u) {
            throw new IllegalArgumentException("Cannot create package name for url " + url); // NOI18N
        }
        String suffix = ""; // NOI18N

        do {
            FileObject fo = FileUtil.toFileObject(f);
            if (fo != null) {
                if (suffix.isEmpty())
                    return getPackageName(fo);
                String prefix = getPackageName(fo);
                return prefix + ("".equals(prefix)?"":".") + suffix; // NOI18N
            }
            if (!suffix.isEmpty()) {
                suffix = "." + suffix; // NOI18N
            }
            try {
                suffix = URLDecoder.decode(f.getPath().substring(f.getPath().lastIndexOf(File.separatorChar) + 1), "utf-8") + suffix; // NOI18N
            } catch (UnsupportedEncodingException u) {
                throw new IllegalArgumentException("Cannot create package name for url " + url); // NOI18N
            }
            f = f.getParentFile();
        } while (f!=null);
        throw new IllegalArgumentException("Cannot create package name for url " + url); // NOI18N
    }

    public static PathKind toJava(ClasspathInfo.PathKind kind) {
        return PathKind.valueOf(kind.name());
    }

    public static org.netbeans.api.java.source.ClasspathInfo toJava(ClasspathInfo cpInfo) {
        return org.netbeans.api.java.source.ClasspathInfo.create(cpInfo.getClassPath(ClasspathInfo.PathKind.BOOT), cpInfo.getClassPath(ClasspathInfo.PathKind.COMPILE), cpInfo.getClassPath(ClasspathInfo.PathKind.SOURCE));
    }

    public static FileObject getFile(final Element element, final CompilationInfo cc) {
        final FileObject[] rslt = new FileObject[]{getFile(ElementHandle.create(element), cc.getClasspathInfo())};
        if (rslt[0] == null) {
            rslt[0] = new VisageTreePathScanner<FileObject, Void>() {

                @Override
                public FileObject scan(Tree tree, Void p) {
                    if (tree == null) return null;
                    
                    VisageTreePath tp = cc.getTrees().getPath(cc.getCompilationUnit(), tree);
                    if (tp != null) {
                        Element e = cc.getTrees().getElement(tp);

                        if (e != null && e.equals(element)) {
                            rslt[0] = cc.getFileObject();
                            return null;
                        }
                    }
                    return super.scan(tree, p);
                }

            }.scan(cc.getCompilationUnit(), null);
        }
        return rslt[0];
    }

    public static FileObject getFile(ElementHandle handle, final ClasspathInfo info) {
        ClassIndex ci = info.getClassIndex();

        Set<FileObject> files = ci.getResources(handle, EnumSet.of(ClassIndex.SearchKind.TYPE_DEFS), EnumSet.of(ClassIndex.SearchScope.SOURCE));
        if (!files.isEmpty()) {
            return files.iterator().next();
        }

        return null;
    }

    public static String getEnclosingTypeName(ElementDef def) {
        ElementHandle handle = def.createHandle();
        if (handle.getKind().isInterface() || handle.getKind().isClass()) {
            return handle.getQualifiedName();
        }
        return new ElementHandle(ElementKind.CLASS, new String[]{handle.getSignatures()[0]}).getQualifiedName();
    }

    public static ClasspathInfo getClasspathInfoFor(FileObject ... files) {
        return getClasspathInfoFor(true, files);
    }

    public static ClasspathInfo getClasspathInfoFor(boolean dependencies, FileObject ... files) {
        return getClasspathInfoFor(dependencies, false, files);
    }

    public static ClasspathInfo getClasspathInfoFor(boolean dependencies, boolean backSource, FileObject ... files ) {
        assert files.length >0;
        Set<URL> dependentRoots = new HashSet();
        for (FileObject fo: files) {
            Project p = null;
            FileObject ownerRoot = null;
            if (fo != null) {
                p = FileOwnerQuery.getOwner(fo);
                ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
                if (cp!=null) {
                    ownerRoot = cp.findOwnerRoot(fo);
                }
            }
            if (p != null && ownerRoot != null) {
                URL sourceRoot = URLMapper.findURL(ownerRoot, URLMapper.INTERNAL);
                if (dependencies) {
                    dependentRoots.addAll(SourceUtils.getDependentRoots(sourceRoot));
                } else {
                    dependentRoots.add(sourceRoot);
                }
                for (SourceGroup root:ProjectUtils.getSources(p).getSourceGroups(VisageProjectConstants.SOURCES_TYPE_VISAGE)) {
                    dependentRoots.add(URLMapper.findURL(root.getRootFolder(), URLMapper.INTERNAL));
                }
            } else {
                for(ClassPath cp: GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
                    for (FileObject root:cp.getRoots()) {
                        dependentRoots.add(URLMapper.findURL(root, URLMapper.INTERNAL));
                    }
                }
            }
        }

        if (backSource) {
            for (FileObject file : files) {
                if (file!=null) {
                    ClassPath source = ClassPath.getClassPath(file, ClassPath.COMPILE);
                    for (ClassPath.Entry root : source.entries()) {
                        SourceForBinaryQuery.Result r = SourceForBinaryQuery.findSourceRoots(root.getURL());
                        for (FileObject root2 : r.getRoots()) {
                            dependentRoots.add(URLMapper.findURL(root2, URLMapper.INTERNAL));
                        }
                    }
                }
            }
        }

        ClassPath rcp = ClassPathSupport.createClassPath(dependentRoots.toArray(new URL[dependentRoots.size()]));
        ClassPath nullPath = ClassPathSupport.createClassPath(new FileObject[0]);
        ClassPath boot = files[0]!=null?ClassPath.getClassPath(files[0], ClassPath.BOOT):nullPath;
        ClassPath compile = files[0]!=null?ClassPath.getClassPath(files[0], ClassPath.COMPILE):nullPath;
        //When file[0] is a class file, there is no compile cp but execute cp
        //try to get it
        if (compile == null) {
            compile = ClassPath.getClassPath(files[0], ClassPath.EXECUTE);
        }
        //If no cp found at all log the file and use nullPath since the ClasspathInfo.create
        //doesn't accept null compile or boot cp.
        if (compile == null) {
            LOG.warning (NbBundle.getMessage(SourceUtils.class, "MSG_NoClassPath", FileUtil.getFileDisplayName(files[0]), FileOwnerQuery.getOwner(files[0]))); // NOI18N
            compile = nullPath;
        }
        ClasspathInfo cpInfo = ClasspathInfo.create(boot, compile, rcp);
        return cpInfo;
    }

    /**
     * Returns the dependent source path roots for given source root.
     * It returns all the open project source roots which have either
     * direct or transitive dependency on the given source root.
     * @param root to find the dependent roots for
     * @return {@link Set} of {@link URL}s containing at least the
     * incoming root, never returns null.
     * @since 0.10
     */
    public static Set<URL> getDependentRoots (final URL root) {
        if (indexingController == null) return Collections.EMPTY_SET;

        try {
            final Map<URL, List<URL>> deps = (Map<URL, List<URL>>) dependentRoots.invoke(indexingController);
            return getDependentRootsImpl(root, deps);
        } catch (IllegalAccessException illegalAccessException) {
        } catch (IllegalArgumentException illegalArgumentException) {
        } catch (InvocationTargetException invocationTargetException) {
        }
        return Collections.EMPTY_SET;
    }


    static Set<URL> getDependentRootsImpl (final URL root, final Map<URL, List<URL>> deps) {
        //Create inverse dependencies
        final Map<URL, List<URL>> inverseDeps = new HashMap<URL, List<URL>> ();
        for (Map.Entry<URL,List<URL>> entry : deps.entrySet()) {
            final URL u1 = entry.getKey();
            final List<URL> l1 = entry.getValue();
            for (URL u2 : l1) {
                List<URL> l2 = inverseDeps.get(u2);
                if (l2 == null) {
                    l2 = new ArrayList<URL>();
                    inverseDeps.put (u2,l2);
                }
                l2.add (u1);
            }
        }
        //Collect dependencies
        final Set<URL> result = new HashSet<URL>();
        final LinkedList<URL> todo = new LinkedList<URL> ();
        todo.add (root);
        while (!todo.isEmpty()) {
            final URL u = todo.removeFirst();
            if (!result.contains(u)) {
                result.add (u);
                final List<URL> ideps = inverseDeps.get(u);
                if (ideps != null) {
                    todo.addAll (ideps);
                }
            }
        }
        //Filter non opened projects
        Set<ClassPath> cps = GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE);
        Set<URL> toRetain = new HashSet<URL>();
        for (ClassPath cp : cps) {
            for (ClassPath.Entry e : cp.entries()) {
                toRetain.add(e.getURL());
            }
        }
        result.retainAll(toRetain);
        return result;
    }
    
    /**
     * Tests whether the initial scan is in progress.
     */
    public static boolean isScanInProgress () {
        return IndexingManager.getDefault().isIndexing();
    }

    /**
     * Waits for the end of the initial scan, this helper method 
     * is designed for tests which require to wait for end of initial scan.
     * @throws InterruptedException is thrown when the waiting thread is interrupted.
     * @deprecated use {@link VisageSource#runWhenScanFinished}
     */
    public static void waitScanFinished () throws InterruptedException {
        try {
            class T extends UserTask implements ClasspathInfoProvider {
                private final ClassPath EMPTY_PATH = ClassPathSupport.createClassPath(new URL[0]);
                private final ClasspathInfo cpinfo = ClasspathInfo.create(EMPTY_PATH, EMPTY_PATH, EMPTY_PATH);
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    // no-op
                }

                public ClasspathInfo getClasspathInfo() {
                    return cpinfo;
                }
            }
            Future<Void> f = ParserManager.parseWhenScanFinished(VisageSourceUtils.VISAGE_MIME_TYPE, new T());
            if (!f.isDone()) {
                f.get();
            }
        } catch (Exception ex) {
        }
    }

    /**
     * This is a helper method to provide support for delaying invocations of actions
     * depending on java model. See <a href="http://java.netbeans.org/ui/waitscanfinished.html">UI Specification</a>.
     * <br>Behavior of this method is following:<br>
     * If classpath scanning is not in progress, runnable's run() is called. <br>
     * If classpath scanning is in progress, modal cancellable notification dialog with specified
     * tile is opened.
     * </ul>
     * As soon as classpath scanning finishes, this dialog is closed and runnable's run() is called.
     * This method must be called in AWT EventQueue. Runnable is performed in AWT thread.
     *
     * @param runnable Runnable instance which will be called.
     * @param actionName Title of wait dialog.
     * @return true action was cancelled <br>
     *         false action was performed
     */
    public static boolean invokeAfterScanFinished(final Runnable runnable , final String actionName) {
        assert SwingUtilities.isEventDispatchThread();
        if (SourceUtils.isScanInProgress()) {
            final ActionPerformer ap = new ActionPerformer(runnable);
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ap.cancel();
                    waitTask.cancel();
                }
            };
            JLabel label = new JLabel(getString("MSG_WaitScan"), javax.swing.UIManager.getIcon("OptionPane.informationIcon"), SwingConstants.LEFT); // NOI18N
            label.setBorder(new EmptyBorder(12,12,11,11));
            DialogDescriptor dd = new DialogDescriptor(label, actionName, true, new Object[]{getString("LBL_CancelAction", new Object[]{actionName})}, null, 0, null, listener); // NOI18N
            waitDialog = DialogDisplayer.getDefault().createDialog(dd);
            waitDialog.pack();
            //100ms is workaround for 127536
            waitTask = RequestProcessor.getDefault().post(ap, 100);
            waitDialog.setVisible(true);
            waitTask = null;
            waitDialog = null;
            return ap.hasBeenCancelled();
        } else {
            runnable.run();
            return false;
        }
    }

    /**
     * Workaround for VisageC-3787
     * @param e
     * @return
     */
    public static VisageTreePath getPath(final Element e, CompilationInfo ci) {
        final VisageTreePath[] result = new VisageTreePath[1];
        new VisageTreePathScanner<Void, Void>() {

            @Override
            public Void scan(Tree tree, Void p) {
                if (result[0] != null) return null;
                Element el = VisageTreeInfo.symbolFor((VisageTree)tree);
                if (e.equals(el)) {
                    result[0] = VisageTreePath.getPath(getCurrentPath(), tree);
                    return null;
                }
                return super.scan(tree, p);
            }

        }.scan(ci.getCompilationUnit(), null);
        return result[0];
    }

    private static String getString(String key) {
        return NbBundle.getMessage(SourceUtils.class, key);
    }

    private static String getString(String key, Object values) {
        return new MessageFormat(getString(key)).format(values);
    }

    private static Dialog waitDialog = null;
    private static RequestProcessor.Task waitTask = null;

    private static class ActionPerformer implements Runnable {
        private Runnable action;
        private boolean cancel = false;

        ActionPerformer(Runnable a) {
            this.action = a;
        }

        public boolean hasBeenCancelled() {
            return cancel;
        }

        public void run() {
            try {
                SourceUtils.waitScanFinished();
            } catch (InterruptedException ie) {
                Exceptions.printStackTrace(ie);
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (!cancel) {
                        if (waitDialog != null) {
                            waitDialog.setVisible(false);
                            waitDialog.dispose();
                        }
                        action.run();
                    }
                }
            });
        }

        public void cancel() {
            assert SwingUtilities.isEventDispatchThread();
            // check if the scanning did not finish during cancel
            // invocation - in such case do not set cancel to true
            // and do not try to hide waitDialog window
            if (waitDialog != null) {
                cancel = true;
                waitDialog.setVisible(false);
                waitDialog.dispose();
            }
        }
    }

    /**
     * creates or finds FileObject according to
     * @param url
     * @return FileObject
     */
    public static FileObject getOrCreateFolder(URL url) throws IOException {
        try {
            FileObject result = URLMapper.findFileObject(url);
            if (result != null)
                return result;
            File f = new File(url.toURI());

            result = FileUtil.createFolder(f);
            return result;
        } catch (URISyntaxException ex) {
            throw (IOException) new IOException().initCause(ex);
        }
    }

    public static boolean isRefactorable(FileObject file) {
        return isVisageFile(file) && isFileInOpenProject(file) && isOnSourceClasspath(file) && isPlatformOk(file);
    }
}
