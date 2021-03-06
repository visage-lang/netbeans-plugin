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

package org.netbeans.modules.visage.source.classpath;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import org.netbeans.api.java.classpath.ClassPath;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author nenik
 */
public class SourceFileManager implements JavaFileManager {
    private final ClassPath cp;

    public SourceFileManager(ClassPath cp) {
        this.cp = cp;
    }

    public ClassLoader getClassLoader(Location arg0) {
        return null;
    }

    public Iterable<JavaFileObject> list(final Location l, final String packageName, final Set<JavaFileObject.Kind> kinds, final boolean recursive) {
        //Todo: Caching of results, needs listening on FS
        List<JavaFileObject> result = new ArrayList<JavaFileObject> ();
        String _name = packageName.replace('.','/');    //NOI18N
        if (_name.length() != 0) {
            _name+='/';                                 //NOI18N
        }
        for (ClassPath.Entry entry : cp.entries()) {
            if (entry.includes(_name)) {
                FileObject root = entry.getRoot();
                if (root != null) {
                    FileObject tmpFile = root.getFileObject(_name);
                    if (tmpFile != null && tmpFile.isFolder()) {
                        Enumeration<? extends FileObject> files = tmpFile.getChildren (recursive);
                        while (files.hasMoreElements()) {
                            FileObject file = files.nextElement();
                            if (entry.includes(file)) {
                                JavaFileObject.Kind kind;
                                final String ext = file.getExt();
                                if (FileObjects.VISAGE.equalsIgnoreCase(ext) || FileObjects.JAVA.equalsIgnoreCase(ext)) {
                                    kind = JavaFileObject.Kind.SOURCE;
                                }
                                else if (FileObjects.CLASS.equalsIgnoreCase(ext) || "sig".equalsIgnoreCase(ext)) { // NOI18N
                                    kind = JavaFileObject.Kind.CLASS;
                                }
                                else if (FileObjects.HTML.equalsIgnoreCase(ext)) {
                                    kind = JavaFileObject.Kind.HTML;
                                }
                                else {
                                    kind = JavaFileObject.Kind.OTHER;
                                }
                                if (kinds.contains(kind)) {                        
                                    result.add (SourceFileObject.create(file, root));
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public String inferBinaryName (final Location l, final JavaFileObject jfo) {        
        try {            
            FileObject fo;
            FileObject root = null;
            if (jfo instanceof SourceFileObject) {
                fo = ((SourceFileObject)jfo).file;
                root = ((SourceFileObject)jfo).root;
            }
            else {
                //Should never happen in the IDE
                fo = URLMapper.findFileObject(jfo.toUri().toURL());
            }            
            
            if (root == null) {
                for (FileObject rc : cp.getRoots()) {
                    if (FileUtil.isParentOf(rc,fo)) {
                        root = rc;
                    }
                }
            }
            
            if (root != null) {
                String relativePath = FileUtil.getRelativePath(root,fo);
                int index = relativePath.lastIndexOf('.'); // NOI18N
                assert index > 0;                    
                final String result = relativePath.substring(0,index).replace('/','.');  // NOI18N
                return result;
            }
        } catch (MalformedURLException e) {
            ErrorManager.getDefault().notify(e);
        }        
        return null;

   }

    public boolean isSameFile(javax.tools.FileObject fileObject, javax.tools.FileObject fileObject0) {
        return fileObject instanceof SourceFileObject 
               && fileObject0 instanceof SourceFileObject
               && ((SourceFileObject)fileObject).file == ((SourceFileObject)fileObject0).file;
    }

    public boolean handleOption(String arg0, Iterator<String> arg1) {
        return false;
    }

    public boolean hasLocation(Location arg0) {
        return true;
    }

    public JavaFileObject getJavaFileForInput (Location l, final String className, JavaFileObject.Kind kind) {
        String[] namePair = FileObjects.getParentRelativePathAndName (className);
        if (namePair == null) {
            return null;
        }
        String ext = kind.extension.substring(1);   //Skeep the .
        for (ClassPath.Entry entry : cp.entries()) {
            FileObject root = entry.getRoot();
            if (root != null) {
                FileObject parent = root.getFileObject(namePair[0]);
                if (parent != null) {
                    FileObject[] children = parent.getChildren();
                    for (FileObject child : children) {
                        if (namePair[1].equals(child.getName()) && ext.equalsIgnoreCase(child.getExt()) && entry.includes(child)) {
                            return SourceFileObject.create (child, root);
                        }
                    }
                }
            }
        }
        return null;
    }

    public JavaFileObject getJavaFileForOutput(Location arg0, String arg1, Kind arg2, javax.tools.FileObject arg3) throws IOException {
        throw new UnsupportedOperationException("The SourceFileManager does not support write operations."); // NOI18N
    }

    public javax.tools.FileObject getFileForInput (final Location l, final String pkgName, final String relativeName) {
        String rp = FileObjects.getRelativePath (pkgName, relativeName);
        for (ClassPath.Entry entry : cp.entries()) {
            if (entry.includes(rp)) {
                FileObject root = entry.getRoot();            
                if (root != null) {
                    FileObject file = root.getFileObject(rp);
                    if (file != null) {
                        return SourceFileObject.create (file, root);
                    }
                }
            }
        }
        return null;
    }

    public javax.tools.FileObject getFileForOutput(Location arg0, String arg1, String arg2, javax.tools.FileObject arg3) throws IOException {
        throw new UnsupportedOperationException ("The SourceFileManager does not support write operations.");   // NOI18N
    }

    public void flush() throws IOException {
        //Nothing to do
    }

    public void close() throws IOException {
        //Nothing to do
    }

    public int isSupportedOption(String arg0) {
        return -1;
    }
}
