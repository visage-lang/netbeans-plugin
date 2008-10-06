/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.javafx.project.ui.customizer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectConfigurationProvider;

import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author mkleint
 */
public class JavaFXCompositePanelProvider implements ProjectCustomizer.CompositeCategoryProvider {
    
    private static final String SOURCES = "Sources";
    static final String LIBRARIES = "Libraries";
    
    private static final String BUILD = "Build";
//    private static final String BUILD_TESTS = "BuildTests";
    private static final String JAR = "Jar";
    private static final String JAVADOC = "Javadoc";
    public static final String RUN = "Run";
//    private static final String RUN_TESTS = "RunTests";
    private static final String APPLICATION = "Application";
    
    private static final String WEBSTART = "WebStart";
    
    private static final String APPLET = "Applet";
    
    private String name;
    
    /** Creates a new instance of JavaFXCompositePanelProvider */
    public JavaFXCompositePanelProvider(String name) {
        this.name = name;
    }

    public ProjectCustomizer.Category createCategory(Lookup context) {
        ResourceBundle bundle = NbBundle.getBundle( CustomizerProviderImpl.class );
        ProjectCustomizer.Category toReturn = null;
        if (SOURCES.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    SOURCES,
                    bundle.getString("LBL_Config_Sources"),
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (LIBRARIES.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    LIBRARIES,
                    bundle.getString( "LBL_Config_Libraries" ), // NOI18N
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (BUILD.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    BUILD,
                    bundle.getString( "LBL_Config_Build" ), // NOI18N
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (JAR.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    JAR,
                    bundle.getString( "LBL_Config_Jar" ), // NOI18N
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (JAVADOC.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    JAVADOC,
                    bundle.getString( "LBL_Config_Javadoc" ), // NOI18N
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (RUN.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    RUN,
                    bundle.getString( "LBL_Config_Run" ), // NOI18N
                    null,
                    (ProjectCustomizer.Category[])null);
/*            
        } else if (WEBSERVICECLIENTS.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    WEBSERVICECLIENTS,
                    bundle.getString( "LBL_Config_WebServiceClients" ), // NOI18N
                    null,
                    null);
 */ 
        } else if (APPLICATION.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(
                    APPLICATION,
                    bundle.getString( "LBL_Config_Application" ), // NOI18N,
                    null,
                    (ProjectCustomizer.Category[])null);
        } else if (WEBSTART.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(WEBSTART,
                    bundle.getString("LBL_Config_WebStart"), null, (ProjectCustomizer.Category[])null); //NOI18N
        } else if (APPLET.equals(name)) {
            toReturn = ProjectCustomizer.Category.create(APPLET,
                    bundle.getString("LBL_Config_Applet"), null, (ProjectCustomizer.Category[])null); //NOI18N
        }
        assert toReturn != null : "No category for name:" + name;
        return toReturn;
    }

    public JComponent createComponent(ProjectCustomizer.Category category, Lookup context) {
        String nm = category.getName();
        JavaFXProjectProperties uiProps = (JavaFXProjectProperties)context.lookup(JavaFXProjectProperties.class);
        if (SOURCES.equals(nm)) {
            return new CustomizerSources(uiProps);
        } else if (LIBRARIES.equals(nm)) {
            CustomizerProviderImpl.SubCategoryProvider prov = (CustomizerProviderImpl.SubCategoryProvider)context.lookup(CustomizerProviderImpl.SubCategoryProvider.class);
            assert prov != null : "Assuming CustomizerProviderImpl.SubCategoryProvider in customizer context";
            return new CustomizerLibraries(uiProps, prov);
        } else if (BUILD.equals(nm)) {
            return new CustomizerCompile(uiProps);
        } else if (JAR.equals(nm)) {
            return new CustomizerJar(uiProps);
        } else if (JAVADOC.equals(nm)) {
            return new CustomizerJavadoc(uiProps);
        } else if (RUN.equals(nm)) {
            return new CustomizerRun(uiProps);
        } /*else if (WEBSERVICECLIENTS.equals(nm)) {
            List serviceClientsSettings = null;
            Project project = (Project)context.lookup(Project.class);
            WebServicesClientSupport clientSupport = WebServicesClientSupport.getWebServicesClientSupport(project.getProjectDirectory());
            if (clientSupport != null) {
                serviceClientsSettings = clientSupport.getServiceClients();
            }

            if(serviceClientsSettings != null && serviceClientsSettings.size() > 0) {
                return new CustomizerWSClientHost( uiProps, serviceClientsSettings );
            } else {
                return new NoWebServiceClientsPanel();
            }
        }*/ else if (APPLICATION.equals(nm)) {
            CustomizerApplication ca = new CustomizerApplication(uiProps);
            ca.jPanel1.add(new CustomizerApplet(uiProps), BorderLayout.CENTER);
            WebStartProjectProperties jwsProps = uiProps.getWebStartProjectProperties();
            category.setOkButtonListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
            ca.jPanel2.add(new CustomizerWebStart(jwsProps), BorderLayout.CENTER);
            return ca;

        } else if (WEBSTART.equals(nm)) {
            //return new CustomizerWebStart(uiProps);
            WebStartProjectProperties jwsProps = uiProps.getWebStartProjectProperties();
            //WebStartProjectProperties jwsProps = context.lookup(org.netbeans.modules.javafx.project.ui.customizer.WebStartProjectProperties.class);
            category.setOkButtonListener(new SavePropsListener(jwsProps, context.lookup(Project.class)));
            JComponent component = new CustomizerWebStart(jwsProps);
            return component;
        } else if (APPLET.equals(nm)) {
            return new CustomizerApplet(uiProps);
        }
        return new JPanel();

    }

    public static JavaFXCompositePanelProvider createSources() {
        return new JavaFXCompositePanelProvider(SOURCES);
    }

    public static JavaFXCompositePanelProvider createLibraries() {
        return new JavaFXCompositePanelProvider(LIBRARIES);
    }

    public static JavaFXCompositePanelProvider createBuild() {
        return new JavaFXCompositePanelProvider(BUILD);
    }

    public static JavaFXCompositePanelProvider createJar() {
        return new JavaFXCompositePanelProvider(JAR);
    }

    public static JavaFXCompositePanelProvider createJavadoc() {
        return new JavaFXCompositePanelProvider(JAVADOC);
    }

    public static JavaFXCompositePanelProvider createRun() {
        return new JavaFXCompositePanelProvider(RUN);
    }
/*
    public static JavaFXCompositePanelProvider createWebServiceClients() {
        return new JavaFXCompositePanelProvider(WEBSERVICECLIENTS);
    }
*/    
    public static JavaFXCompositePanelProvider createApplication() {
        return new JavaFXCompositePanelProvider(APPLICATION);
    }
    public static JavaFXCompositePanelProvider createWebStart() {
        return new JavaFXCompositePanelProvider(WEBSTART);
    }
    public static JavaFXCompositePanelProvider createApplet() {
        return new JavaFXCompositePanelProvider(APPLET);
    }

    // ----------
    
    private static class SavePropsListener implements ActionListener {
        
        private WebStartProjectProperties jwsProps;
        private Project javafxProject;
        
        public SavePropsListener(WebStartProjectProperties props, Project proj) {
            jwsProps = props;
            javafxProject = proj;
        }
        
        public void actionPerformed(ActionEvent e) {
            // log("Saving Properties " + jwsProps + " ...");
            try {
                jwsProps.store();
            } catch (IOException ioe) {
                ErrorManager.getDefault().notify(ioe);
            }
            final ProjectConfigurationProvider configProvider = 
                    javafxProject.getLookup().lookup(ProjectConfigurationProvider.class);
//            try {
//                jwsProps.createConfigurationFiles(configProvider, true);
                /*
                if (enabled) {
                    // XXX logging
                    // test if the file already exists, if so do not generate, just set as active
                    JavaFXProjectConfigurations.createConfigurationFiles(javafxProject, "JWS_generated",
                            prepareSharedProps(), null ); // NOI18N
                    setActiveConfig(configProvider, NbBundle.getBundle(JavaFXCompositePanelProvider.class).getString("LBL_Category_WebStart"));
                    copyTemplate(javafxProject);
                    modifyBuildXml(javafxProject);
                } else {
                    setActiveConfig(configProvider, NbBundle.getBundle(JavaFXCompositePanelProvider.class).getString("LBL_Category_Default"));
                }
                 */
//            } catch (IOException ioe) {
//                ErrorManager.getDefault().notify(ioe);
//            }
        }
    }
}
