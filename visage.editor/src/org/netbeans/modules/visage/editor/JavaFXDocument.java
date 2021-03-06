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

package org.netbeans.modules.visage.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.visage.source.CompilationController;
import org.netbeans.api.visage.source.VisageSource;
import org.netbeans.api.visage.source.VisageSource.Phase;
import org.netbeans.api.visage.source.Task;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorDocument;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.visage.project.VisageProject;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author answer
 */
public class VisageDocument extends NbEditorDocument {
    
    private JEditorPane pane = null;
    private Component editor = null;
    boolean executionEnabled = false;
    boolean errorAndSyntaxEnabled = false;
    private Point previewLocation = new Point(0, 0);
    private Dimension previewSize = new Dimension(200, 200);
    
    public VisageDocument(String mimeType)  {
        super(mimeType);
    }
    
    public static Project getProject(Document doc){
        return getProject(NbEditorUtilities.getFileObject(doc));
    }

    public static Project getProject(FileObject fileObject){
        return FileOwnerQuery.getOwner(fileObject);
    }
    
    public JComponent getEditor() {
        return (JComponent) pane;
    }

    
    @Override
    public Component createEditor(JEditorPane pane) {
       
        DocumentListener changeListener = new DocumentListener(){
            public void removeUpdate(DocumentEvent e) {
                sourceChanged();
            }

            public void insertUpdate(DocumentEvent e) {
                sourceChanged();
            }
            
            public void changedUpdate(DocumentEvent e) {
            }
        };
        addDocumentListener(changeListener);

        editor = super.createEditor(pane);
        this.pane = pane;

        return editor;
    }
    
    
    
    public DataObject getDataObject(){
        return NbEditorUtilities.getDataObject(this);
    }
    
    public void  sourceChanged(){
        synchronized(this){
        }
    }
    
    public String getSourceToRender(){
        try{
            return (getText(0, getLength()));
        }catch(BadLocationException e){
            return ""; // NOI18N
        }
    }
    
   
    public void enableErrorAndSyntax(boolean enabled){
        errorAndSyntaxEnabled = enabled;
    }
    
    public boolean errorAndSyntaxAllowed(){
        return errorAndSyntaxEnabled;
    }
    
    public void setPreviewPlacement(Point previewLocation, Dimension previewSize) { 
        this.previewLocation = previewLocation;
        this.previewSize = previewSize;
    }
    
    public Point getPreviewLocation() { 
        return previewLocation;
    }
    
    public Dimension getPreviewSize() { 
        return previewSize;
    }

    public void enableExecution(boolean enabled) {
        if (enabled) {
            Project project = getProject(this);
            org.netbeans.spi.project.support.ant.PropertyEvaluator evaluator =((VisageProject)project).evaluator();
            Object profile = evaluator.getProperty("visage.profile");                                                   //NOI18N
            if (profile != null)
                if (profile.toString().contentEquals("mobile")) {                                                       //NOI18N
                    String message = NbBundle.getMessage(VisageDocument.class, "PREVIEW_DISABLED_BY_PROFILE");          //NOI18N
                    NotifyDescriptor d = new NotifyDescriptor.Message (message, NotifyDescriptor.INFORMATION_MESSAGE);  
                    DialogDisplayer.getDefault().notify(d);
                    return;
                }
        }
        for (Component component : createToolbar(pane).getComponents()) {
            if (component instanceof JToggleButton) {
                if (((JToggleButton)component).getClientProperty("enablePreviewMark") == Boolean.TRUE) {                //NOI18N
                    ((JToggleButton)component).setSelected(enabled);
                }
            }
            if (component instanceof JButton) {
                if (((JButton)component).getClientProperty("resetPreviewMark") == Boolean.TRUE || ((JButton)component).getClientProperty("printPreviewMark") == Boolean.TRUE) {     //NOI18N
                    component.setEnabled(enabled);
                }
            }
        }
        if (!enabled) {
            executionEnabled = false;
//
//
//
//            Bridge.closePreview(this);
//
//
//
//
        }
        else {
            if (executionEnabled && (executionEnabled == enabled)) {
//
//
//                Bridge.moveToFront(this);
//
//
            } else {
                executionEnabled = true;
                final VisageSource js = VisageSource.forDocument(this);
                try {
                    js.runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController controller) throws Exception {
                            controller.toPhase(Phase.CODE_GENERATED);
//
//
//                            PreviewCodeGenerate.process(controller);
//
//
                        }
                    }, true);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    public boolean executionAllowed(){
        return executionEnabled;
    }
}