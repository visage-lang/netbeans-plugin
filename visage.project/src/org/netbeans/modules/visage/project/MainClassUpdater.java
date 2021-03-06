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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.visage.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import javax.lang.model.element.TypeElement;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.java.api.common.ant.UpdateHelper;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Zezula
 */
public class MainClassUpdater extends FileChangeAdapter implements PropertyChangeListener {
    
    private static final RequestProcessor RP = new RequestProcessor ("main-class-updater",1);       //NOI18N
    
    private final Project project;
    private final PropertyEvaluator eval;
    private final UpdateHelper helper;
    private final ClassPath sourcePath;
    private final String mainClassPropName;
    private FileObject current;
    private FileChangeListener listener;
    
    /** Creates a new instance of MainClassUpdater */
    public MainClassUpdater(final Project project, final PropertyEvaluator eval,
        final UpdateHelper helper, final ClassPath sourcePath, final String mainClassPropName) {
        assert project != null;
        assert eval != null;
        assert helper != null;
        assert sourcePath != null;
        assert mainClassPropName != null;
        this.project = project;
        this.eval = eval;
        this.helper = helper;
        this.sourcePath = sourcePath;
        this.mainClassPropName = mainClassPropName;        
        this.eval.addPropertyChangeListener(this);
        this.addFileChangeListener ();
    }
    
    public synchronized void unregister () {
        if (current != null) {
            current.removeFileChangeListener(this);
        }
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        if (this.mainClassPropName.equals(evt.getPropertyName())) {
            RP.post(new Runnable () {
                public void run() {
                    MainClassUpdater.this.addFileChangeListener ();
                }
            });            
        }
    }
    
    @Override
    public void fileRenamed (final FileRenameEvent evt) {
        if (!project.getProjectDirectory().isValid()) {
            return;
        }
        final FileObject _current;
        synchronized (this) {
            _current = this.current;
        }
        if (evt.getFile() == _current) {
            Runnable r = new Runnable () {
                public void run () {  
                    try {
                        final String oldMainClass = ProjectManager.mutex().readAccess(new Mutex.ExceptionAction<String>() {
                            public String run() throws Exception {
                                return eval.getProperty(mainClassPropName);
                            }
                        });

                        Collection<ElementHandle<TypeElement>> main = SourceUtils.getMainClasses(_current);
                        String newMainClass = null;
                        if (!main.isEmpty()) {
                            ElementHandle<TypeElement> mainHandle = main.iterator().next();
                            newMainClass = mainHandle.getQualifiedName();
                        }                    
                        if (newMainClass != null && !newMainClass.equals(oldMainClass) && helper.requestUpdate() &&
                                // XXX ##84806: ideally should update nbproject/configs/*.properties in this case:
                            eval.getProperty(VisageConfigurationProvider.PROP_CONFIG) == null) {
                            final String newMainClassFinal = newMainClass;
                            ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                                public Void run() throws Exception {                                                                                    
                                    EditableProperties props = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                                    props.put (mainClassPropName, newMainClassFinal);
                                    helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, props);
                                    ProjectManager.getDefault().saveProject (project);
                                    return null;
                                }
                            });
                        }
                    } catch (IOException e) {
                        Exceptions.printStackTrace(e);
                    }
                    catch (MutexException e) {
                        Exceptions.printStackTrace(e);
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            }
            else {
                SwingUtilities.invokeLater(r);
            }
        }
    }
    
    private void addFileChangeListener () {
        RP.post( new Runnable () {
            public void run() {
                try {
                    SourceUtils.waitScanFinished();
                    synchronized (MainClassUpdater.this) {
                        if (current != null) {
                            current.removeFileChangeListener(MainClassUpdater.this);
                            current = null;
                        }            
                    }
                    final String mainClassName = MainClassUpdater.this.eval.getProperty(mainClassPropName);
                    final FileObject[] _current = new FileObject[1];
                    if (mainClassName != null) {
                        FileObject[] roots = sourcePath.getRoots();
                        if (roots.length>0) {
                            ClassPath bootCp = ClassPath.getClassPath(roots[0], ClassPath.BOOT);
                            ClassPath compileCp = ClassPath.getClassPath(roots[0], ClassPath.COMPILE);
                            final ClasspathInfo cpInfo = ClasspathInfo.create(bootCp, compileCp, sourcePath);
                            JavaSource js = JavaSource.create(cpInfo);
                            js.runUserActionTask(new CancellableTask<CompilationController>() {
                                public void cancel() {                    
                                }
                                public void run(CompilationController c) throws Exception {
                                }                
                            }, true);
                        }
                    }
                    synchronized (MainClassUpdater.this) {
                        current = _current[0];
                        if (current != null && sourcePath.contains(current)) {
                            current.addFileChangeListener(MainClassUpdater.this);
                        }
                    }
                } catch (InterruptedException e) {
                    Exceptions.printStackTrace(e);
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }});
    }

}
