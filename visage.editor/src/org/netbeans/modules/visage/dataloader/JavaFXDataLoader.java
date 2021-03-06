/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.visage.dataloader;

import org.netbeans.api.java.classpath.ClassPath;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.*;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author answer
 */


public class VisageDataLoader extends MultiFileLoader {
    
    public static final String FX_MIME_TYPE = "text/x-fx";  //NOI18N
    
    /** The standard extension for Java source files. */
    public static final String FX_EXTENSION = "fx"; // NOI18N

    private static final String PACKAGE_INFO = "package-info";  //NOI18N
    
    static final long serialVersionUID =-6286836352608877232L;

    /** Create the loader.
    * Should <em>not</em> be used by subclasses.
    */
    public VisageDataLoader() {
        super("org.netbeans.modules.visage.dataloader.VisageDataObject"); // NOI18N
    }

    @Override
    protected String actionsContext () {
        return "Loaders/text/x-fx/Actions/"; // NOI18N
    }
    
    protected @Override String defaultDisplayName() {
        return NbBundle.getMessage(VisageDataLoader.class, "PROP_JavaLoader_Name"); // NOI18N
    }
    
    /** Create the <code>JavaDataObject</code>.
    * Subclasses should rather create their own data object type.
    *
    * @param primaryFile the primary file
    * @return the data object for this file
    * @exception DataObjectExistsException if the primary file already has a data object
    */
    protected MultiDataObject createMultiObject (FileObject primaryFile)
    throws DataObjectExistsException, java.io.IOException {
        if (primaryFile.getExt().equals(FX_EXTENSION))
            return new VisageDataObject(primaryFile, this);
        return null;
    }

    /** For a given file find the primary file.
    * Subclasses should override this, but still look for the {@link #JAVA_EXTENSION},
    * as the Java source file should typically remain the primary file for the data object.
    * @param fo the file to find the primary file for
    *
    * @return the primary file for this file or <code>null</code> if this file is not
    *   recognized by this loader
    */
    protected FileObject findPrimaryFile (FileObject fo) {
	// never recognize folders.
        if (fo.isFolder()) return null;
        
        // ignore templates using scripting
        if (fo.getAttribute("template") != null && fo.getAttribute("javax.script.ScriptEngine") != null) // NOI18N
            return null;
        
        if (fo.getExt().equals(FX_EXTENSION))
            return fo;
        return null;
    }

    /** Create the primary file entry.
    * Subclasses may override {@link JavaDataLoader.JavaFileEntry} and return a new instance
    * of the overridden entry type.
    *
    * @param primaryFile primary file recognized by this loader
    * @return primary entry for that file
    */
    protected MultiDataObject.Entry createPrimaryEntry (MultiDataObject obj, FileObject primaryFile) {
        if (FX_EXTENSION.equals(primaryFile.getExt())) {
//            return new JavaFileEntry (obj, primaryFile);
            return VisageDataSupport.createJavaFileEntry(obj, primaryFile);
        }
        else {
            return new FileEntry(obj, primaryFile);
        }
    }

    /** Create a secondary file entry.
    * By default, {@link FileEntry.Numb} is used for the class files; subclasses wishing to have useful
    * secondary files should override this for those files, typically to {@link FileEntry}.
    *
    * @param secondaryFile secondary file to create entry for
    * @return the entry
    */
    protected MultiDataObject.Entry createSecondaryEntry (MultiDataObject obj, FileObject secondaryFile) {
        //The JavaDataObject itself has no secondary entries, but its subclasses have.
        //So we have to keep it as MultiFileLoader
        ErrorManager.getDefault().log ("Subclass of VisageDataLoader ("+this.getClass().getName() // NOI18N
                +") has secondary entries but does not override createSecondaryEntries (MultidataObject, FileObject) method."); // NOI18N
        return new FileEntry.Numb(obj, secondaryFile);
    }   
    
    /** Create the map of replaceable strings which is used
    * in the <code>JavaFileEntry</code>. This method may be extended in subclasses
    * to provide the appropriate map for other loaders.
    * This implementation gets the map from the Java system option;
    * subclasses may add other key/value pairs which may be created without knowledge of the
    * file itself.
    *
    * @return the map of string which are replaced during instantiation
    *        from template
    */
    static Map<String, String> createStringsMap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("USER", System.getProperty("user.name")); // NOI18N
        Date d = new Date();
        map.put("DATE", DateFormat.getDateInstance(DateFormat.LONG).format(d)); // NOI18N
        map.put("TIME", DateFormat.getTimeInstance(DateFormat.SHORT).format(d)); // NOI18N
        return map;
    }
    
    
    /** This entry defines the format for replacing text during
    * instantiation the data object.
    * Used to substitute keys in the source file.
    */
    
    public static class VisageFileEntry extends IndentFileEntry {
        static final long serialVersionUID =8244159045498569616L;

        public VisageFileEntry(MultiDataObject obj, FileObject file) {
            super(obj, file);
        }

        protected java.text.Format createFormat (FileObject target, String n, String e) {
            Map<String, String> map = createStringsMap();

            modifyMap(map, target, n, e);

            JMapFormat format = new JMapFormat(map);
            format.setLeftBrace("__"); // NOI18N
            format.setRightBrace("__"); // NOI18N
            format.setCondDelimiter("$"); // NOI18N
            format.setExactMatch(false);
            return format;
        }

        protected void modifyMap(Map<String, String> map, FileObject target, String n, String e) {
            ClassPath cp = ClassPath.getClassPath(target, ClassPath.SOURCE);
            String resourcePath = ""; // NOI18N
            if (cp != null) {
                resourcePath = cp.getResourceName(target);
            } else {
                ErrorManager.getDefault().log(ErrorManager.WARNING, "No classpath was found for folder: "+target); // NOI18N
            }
            map.put("NAME", n); // NOI18N
            // Yes, this is package sans filename (target is a folder).
            map.put("PACKAGE", resourcePath.replace('/', '.')); // NOI18N
            map.put("PACKAGE_SLASHES", resourcePath); // NOI18N
	    // Fully-qualified name:
	    if (target.isRoot ()) {
		map.put ("PACKAGE_AND_NAME", n); // NOI18N
		map.put ("PACKAGE_AND_NAME_SLASHES", n); // NOI18N
	    } else {
		map.put ("PACKAGE_AND_NAME", resourcePath.replace('/', '.') + '.' + n); // NOI18N
		map.put ("PACKAGE_AND_NAME_SLASHES", resourcePath + '/' + n); // NOI18N
	    }
            map.put("QUOTES","\""); // NOI18N
            
            for (CreateFromTemplateAttributesProvider provider
                    : Lookup.getDefault().lookupAll(CreateFromTemplateAttributesProvider.class)) {
                Map<String, ?> attrs = provider.attributesFor(
                        getDataObject(),
                        DataFolder.findFolder(target),
                        n);
                if (attrs == null) //#123006
                    continue;
                Object aName = attrs.get("user"); // NOI18N
                if (aName instanceof String) {
                    map.put("USER", (String) aName); // NOI18N
                    break;
                }
            }
        }


        // XXX below are methods formly placed in JavaDataObject. It is
        // a question if rename and copy should be here at all or they should be
        // part of refactoring stuff only.
        
        @Override
        public FileObject rename(String name) throws IOException {
            if (!PACKAGE_INFO.equals(name) && !Utilities.isJavaIdentifier(name))
                throw new IOException(NbBundle.getMessage(VisageDataObject.class, "FMT_Not_Valid_FileName", name)); // NOI18N
            
            FileObject fo = super.rename(name);
            return fo;
        }
        
        @Override
        public FileObject copy(FileObject f, String suffix) throws IOException {
            final FileObject origFile = getFile();
            String origName = origFile.getName();
            FileObject fo = super.copy(f, suffix);
            final ClassPath cpOrig = ClassPath.getClassPath(origFile,ClassPath.SOURCE);
            final ClassPath cpNew = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            if (cpOrig != null && cpNew != null) {                
                final String pkgNameOrig = cpOrig.getResourceName(origFile.getParent(), '.', false); // NOI18N
                final String pkgNameNew = cpNew.getResourceName(f,'.',false); 
                final String newName = fo.getName();
                if (!pkgNameNew.equals(pkgNameOrig) || !newName.equals(origName)) {
                    VisageDataObject.renameFO(fo, pkgNameNew, newName, origName);
                    // unfortunately JavaDataObject.renameFO creates JavaDataObject but it is too soon
                    // in this stage. Loaders reusing this FileEntry will create further files.
                    destroyDataObject(fo);
                }                    
            }
            return fo;
        }
        
        @Override
        public FileObject createFromTemplate(FileObject f, String name) throws IOException {
            Logger.getLogger(VisageDataLoader.class.getName()).warning(
                    "Please replace template " + this.getFile().toString() + //NOI18N
                    " with the new scripting support. See " + //NOI18N
                    "http://www.netbeans.org/download/dev/javadoc/org-openide-loaders/apichanges.html#scripting"); //NOI18N
            if (name == null) {
                // special case: name is null (unspecified or from one-parameter createFromTemplate)
                name = FileUtil.findFreeFileName(f, f.getName(), "fx"); // NOI18N
            } else if (!PACKAGE_INFO.equals(name) && !Utilities.isJavaIdentifier(name)) {
                throw new IOException(NbBundle.getMessage(VisageDataObject.class, "FMT_Not_Valid_FileName", name)); // NOI18N
            }
            
            this.initializeIndentEngine();
            FileObject fo = super.createFromTemplate(f, name);
            
            ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            String pkgName;
            if (cp != null) {
                pkgName = cp.getResourceName(f, '.', false);
            } else {
                pkgName = "";   //NOI18N
            }
            VisageDataObject.renameFO(fo, pkgName, name, getFile().getName());
            
            // unfortunately JavaDataObject.renameFO creates JavaDataObject but it is too soon
            // in this stage. Loaders reusing this FileEntry will create further files.
            destroyDataObject(fo);
            
            return fo;
        }
        
        private void destroyDataObject(FileObject fo) throws IOException {
            DataObject dobj = DataObject.find(fo);
            DataObject orig = this.getDataObject();
            try {
                dobj.setValid(false);
            } catch (PropertyVetoException ex) {
                throw (IOException) new IOException().initCause(ex);
            }
        }

    }
}
