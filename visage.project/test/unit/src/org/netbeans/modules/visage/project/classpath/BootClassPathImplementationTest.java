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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.visage.project.classpath;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.platform.JavaPlatformProvider;
import org.netbeans.modules.visage.project.VisageProjectGenerator;
import org.netbeans.modules.visage.project.TestUtil;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;

import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Lookup;

/**
 *
 * @author answer
 */
public class BootClassPathImplementationTest extends NbTestCase {

    private FileObject scratch;
    private FileObject projdir;
    private FileObject sources;
    private FileObject tests;
    private FileObject defaultPlatformBootRoot;
    private FileObject explicitPlatformBootRoot;
    private ProjectManager pm;
    private Project pp;
    private TestPlatformProvider tp;

    public BootClassPathImplementationTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        clearWorkDir();

        scratch = TestUtil.makeScratchDir(this);
        this.defaultPlatformBootRoot = scratch.createFolder("DefaultPlatformBootRoot");
        this.explicitPlatformBootRoot = scratch.createFolder("ExplicitPlatformBootRoot");
        ClassPath defBCP = ClassPathSupport.createClassPath(new URL[]{defaultPlatformBootRoot.getURL()});
        ClassPath expBCP = ClassPathSupport.createClassPath(new URL[]{explicitPlatformBootRoot.getURL()});
        tp = new TestPlatformProvider (defBCP, expBCP);
        TestUtil.setLookup(new Object[] {
            new org.netbeans.modules.visage.project.VisageProjectType(),
            new org.netbeans.modules.projectapi.SimpleFileOwnerQueryImplementation(),
            tp
        });
    }

    protected void tearDown() throws Exception {
        scratch = null;
        projdir = null;
        pm = null;
        TestUtil.setLookup(Lookup.EMPTY);

        super.tearDown();
    }

    private void prepareProject (String platformName) throws IOException {
        projdir = scratch.createFolder("proj");
        AntProjectHelper helper = null; // FIXME (not compilable): VisageProjectGenerator.createProject(FileUtil.toFile(projdir), "org.netbeans.modules.java.visageproject", null, null);
        pm = ProjectManager.getDefault();
        pp = pm.findProject(projdir);
        EditableProperties props = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        props.setProperty ("platform.active",platformName);
        helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
        sources = projdir.getFileObject("src");
        tests = projdir.getFileObject("test");
    }

    public void testBootClassPathImplementation () throws Exception {
        prepareProject("ExplicitPlatform");

        FileObject file = sources.createData("a.java");
        ClassPath bootCP = ClassPath.getClassPath(file, ClassPath.BOOT);
        assertNotNull("Boot ClassPath exists",bootCP);
        FileObject[] roots = bootCP.getRoots();
        assertEquals("Boot classpath size",1, roots.length);
        assertEquals("Boot classpath",explicitPlatformBootRoot, roots[0]);

        tp.setExplicitPlatformVisible(false);
        bootCP = ClassPath.getClassPath(file, ClassPath.BOOT);
        assertNotNull("Boot ClassPath exists",bootCP);
        roots = bootCP.getRoots();

        assertEquals("Boot classpath size",0, roots.length);

        tp.setExplicitPlatformVisible(true);
        bootCP = ClassPath.getClassPath(file, ClassPath.BOOT);
        assertNotNull("Boot ClassPath exists",bootCP);
        roots = bootCP.getRoots();
        assertEquals("Boot classpath size",1, roots.length);
        assertEquals("Boot classpath",explicitPlatformBootRoot, roots[0]);
    }



    private static class TestPlatformProvider implements JavaPlatformProvider {

        private JavaPlatform defaultPlatform;
        private JavaPlatform explicitPlatform;
        private PropertyChangeSupport support;
        private boolean hideExplicitPlatform;

        public TestPlatformProvider (ClassPath defaultPlatformBootClassPath, ClassPath explicitPlatformBootClassPath) {
            this.support = new PropertyChangeSupport (this);
            this.defaultPlatform = new TestPlatform ("DefaultPlatform", defaultPlatformBootClassPath);
            this.explicitPlatform = new TestPlatform ("ExplicitPlatform", explicitPlatformBootClassPath);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.support.removePropertyChangeListener (listener);
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.support.addPropertyChangeListener(listener);
        }

        public JavaPlatform[] getInstalledPlatforms()  {
            if (this.hideExplicitPlatform) {
                return new JavaPlatform[] {
                    this.defaultPlatform,
                };
            }
            else {
                return new JavaPlatform[] {
                    this.defaultPlatform,
                    this.explicitPlatform,
                };
            }
        }

        public JavaPlatform getDefaultPlatform () {
            return this.defaultPlatform;
        }

        public void setExplicitPlatformVisible (boolean value) {
            this.hideExplicitPlatform = !value;
            this.support.firePropertyChange(PROP_INSTALLED_PLATFORMS,null,null);
        }
    }

    private static class TestPlatform extends JavaPlatform {

        private String systemName;
        private Map properties;
        private ClassPath bootClassPath;

        public TestPlatform (String systemName, ClassPath bootCP) {
            this.systemName = systemName;
            this.bootClassPath = bootCP;
            this.properties = Collections.singletonMap("platform.ant.name",this.systemName);
        }

        public FileObject findTool(String toolName) {
            return null;
        }

        public String getVendor() {
            return "me";
        }

        public ClassPath getStandardLibraries() {
            return null;
        }

        public Specification getSpecification() {
            return new Specification ("j2se", new SpecificationVersion ("1.5"));
        }

        public ClassPath getSourceFolders() {
            return null;
        }

        public Map getProperties() {
            return this.properties;
        }

        public List getJavadocFolders() {
            return null;
        }

        public Collection getInstallFolders() {
            return null;
        }

        public String getDisplayName() {
            return this.systemName;
        }

        public ClassPath getBootstrapLibraries() {
            return this.bootClassPath;
        }

    }
}
