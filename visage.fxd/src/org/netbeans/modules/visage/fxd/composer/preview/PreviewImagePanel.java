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
package org.netbeans.modules.visage.fxd.composer.preview;

import com.sun.visage.geom.Bounds2D;
import java.net.URL;
import org.openide.util.Exceptions;

import org.openide.util.NbBundle;

import org.netbeans.modules.visage.fxd.composer.misc.ActionLookup;
import org.netbeans.modules.visage.fxd.composer.misc.ActionLookupUtils;
import org.netbeans.modules.visage.fxd.composer.model.actions.AbstractFXDAction;
import org.netbeans.modules.visage.fxd.dataloader.fxz.FXZDataObject;
import org.netbeans.modules.visage.fxd.composer.model.*;

import com.sun.visage.tools.fxd.PreviewLoader;
import com.sun.visage.tools.fxd.PreviewStatistics;
import com.sun.visage.tools.fxd.container.ContainerEntry;
import com.sun.visage.tools.fxd.container.misc.ProgressHandler;
import com.sun.visage.tools.fxd.loader.Profile;
import com.sun.visage.tools.fxd.PreviewLoaderUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import visage.geometry.Bounds;
import visage.scene.Node;
import visage.scene.Scene;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.netbeans.modules.visage.fxd.composer.misc.FXDComposerUtils;
import org.netbeans.modules.visage.fxd.composer.model.actions.ActionController;
import org.netbeans.modules.visage.fxd.composer.model.actions.HighlightActionFactory;
import org.netbeans.modules.visage.fxd.composer.model.actions.SelectActionFactory;
import org.openide.awt.MouseUtils;
import org.openide.util.actions.Presenter;
import org.openide.windows.TopComponent;

/**
 *
 * @author Pavel Benes
 */
final class PreviewImagePanel extends JPanel implements ActionLookup {
    static final String      MSG_CANNOT_SHOW     = "MSG_CANNOT_SHOW"; // NOI18N
    static final String      MSG_CANNOT_SHOW_OOM = "MSG_CANNOT_SHOW_OOM"; // NOI18N
    private static final String      MSG_FAILED_TO_RENDER = "MSG_FAILED_TO_RENDER"; // NOI18N
    private static final String      LBL_PARSING         = "LBL_PARSING"; // NOI18N
    private static final String      LBL_RENDERING       = "LBL_RENDERING"; // NOI18N

    private static final float       ZOOM_STEP = (float) 1.1;

    private final FXZDataObject m_dObj;
    private final Action []     m_actions;
    private final Color         m_defaultBackground;
    private       Scene         m_fxScene = null;
    private       int           m_changeTickerCopy = -1;
    private       Profile m_previewProfileCopy = null;
    private       String        m_selectedEntryCopy = null;
        
    PreviewImagePanel(final FXZDataObject dObj) {
        m_dObj = dObj;
    
        m_actions = new Action[] {
            new ZoomToFitAction(),
            new ZoomInAction(),
            new ZoomOutAction()
        };
        
        setLayout(new BorderLayout());
        m_defaultBackground = getBackground();                
        setBackground( Color.WHITE);
    }

    public Scene getScene(){
        return m_fxScene;
    }

    public Node getSceneRoot() {
        return getScene().impl_getRoot();
    }

    public JComponent getScenePanel() {
        if (getScene() != null){
            return getScenePanel(getScene());
        } else {
            return null;
        }
    }

    private JComponent getScenePanel(Scene scene) {
        return PreviewLoaderUtilities.getScenePanel(scene);
    }
    
    protected JLabel createWaitPanel() {
        URL url = PreviewImagePanel.class.getClassLoader().getResource("org/netbeans/modules/visage/fxd/composer/resources/clock.gif"); //NOI18N
        ImageIcon icon = new ImageIcon( url);
        JLabel label = new JLabel( icon);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition( JLabel.BOTTOM);
        return label;        
    }
    
    synchronized void refresh() {
        FXZArchive fxzArchive = m_dObj.getDataModel().getFXDContainer(); 
        if (  fxzArchive != null) {
            final int tickerCopy = fxzArchive.getChangeTicker();
            final Profile profileCopy = m_dObj.getDataModel().getPreviewProfile();
            final String  selectedEntryCopy = m_dObj.getDataModel().getSelectedEntry();
            if ( tickerCopy != m_changeTickerCopy || 
                 profileCopy != m_previewProfileCopy ||
                 !FXDComposerUtils.safeEquals( selectedEntryCopy,m_selectedEntryCopy)) {
                removeAll();
                setBackground( Color.WHITE);

                final JLabel label = createWaitPanel();
                label.setText( NbBundle.getMessage( PreviewImagePanel.class, LBL_PARSING));
                add( label, BorderLayout.CENTER);
                
                m_fxScene = null;
                Thread th = new Thread() {
                    @Override
                    public void run() {
                        final FXZArchive fxz = m_dObj.getDataModel().getFXDContainer();
                        final FXDFileModel fModel = fxz.getFileModel(selectedEntryCopy);
                        fModel.updateModel();

                        m_changeTickerCopy = tickerCopy;
                        m_previewProfileCopy = profileCopy;
                        m_selectedEntryCopy = selectedEntryCopy;

                        if (fModel.isError()) {
                            showError(MSG_CANNOT_SHOW, fModel.getErrorMsg());
                            return;
                        } else {
                            updateLabelMessage(label, LBL_RENDERING, null);
                        }
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                try {
                                    fModel.readLock();

                                    PreviewStatistics statistics = new PreviewStatistics();
                                    ProgressHandler progress = new ProgressHandler();
                                    final PreviewLoader loader = PreviewLoader.createLoader(profileCopy, statistics, progress);
                                    progress.setCallback(new ProgressHandler.Callback() {

                                        public void onProgress(float percentage, int phase, int phasePercentage, int eventNum) {
                                            //update progress
                                        }

                                        public void onDone(Throwable error) {
                                            //in case error == null than load was completed successfully
                                            //otherwise it failed
                                            assert SwingUtilities.isEventDispatchThread();
                                            if (error == null) {
                                                showImagePanel(loader);
                                            } else {
                                                showError(MSG_CANNOT_SHOW, error.getLocalizedMessage());
                                                m_fxScene = null;
                                            }
                                        }
                                    });

                                    PreviewLoader.loadOnBackground(ContainerEntry.create(fxz, selectedEntryCopy), loader);

                                } finally {
                                    fModel.readUnlock();
                                }
                            }
                        });
                    }
                };
                th.setName("ModelUpdate-Thread");  //NOI18N
                th.start();
            } else {
                try {
                    updateZoom();
                } catch (Exception ex) {
                    showError(MSG_CANNOT_SHOW, ex.getLocalizedMessage());
                }
            }
        } else {
            Exception error = m_dObj.getDataModel().getFXDContainerLoadError();
            showError(MSG_CANNOT_SHOW, error.getLocalizedMessage());
        }
    }

    private void updateLabelMessage(final JLabel label, final String bundleKey, final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                label.setText(NbBundle.getMessage(PreviewImagePanel.class, bundleKey, msg));
            }
        });
    }

    private void showImagePanel(final PreviewLoader loader) {
        assert SwingUtilities.isEventDispatchThread();
        try {
            m_fxScene = loader.createScene();
            
            JComponent scenePanel = getScenePanel(m_fxScene);

            removeAll();
            add(new ImageHolder(scenePanel, m_dObj), BorderLayout.CENTER);

            // TODO: see RT-9343. should stop here or at least disable events (and then warn user)
            MouseEventCollector mec = new MouseEventCollector();
            scenePanel.addMouseListener(mec);
            scenePanel.addMouseMotionListener(mec);
            //zooming
            scenePanel.addMouseWheelListener(mec);
            addMouseWheelListener(mec);
            // popup
            PopupListener popupL = new PopupListener();
            scenePanel.addMouseListener(popupL);
            addMouseListener(popupL);

            revalidate();
            updateZoom();
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            showError(MSG_CANNOT_SHOW_OOM, oom.getLocalizedMessage());
        } catch (Exception e) {
            showError(MSG_CANNOT_SHOW, e.getLocalizedMessage());
        } finally {
            System.gc();
        }
    }

    private void showError(final String bundleKey, final Object msg) {
        if (SwingUtilities.isEventDispatchThread()) {
            doShowError(bundleKey, msg);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    doShowError(bundleKey, msg);
                }
            });
        }
    }

    private void doShowError(final String bundleKey, final Object msg) {
        removeAll();
        setBackground(m_defaultBackground);

        JLabel label = new JLabel(
                NbBundle.getMessage(PreviewImagePanel.class, bundleKey, msg),
                JLabel.CENTER);
        add(label, BorderLayout.CENTER);
    }
    
    private void updateZoom() throws Exception {
        JComponent scenePanel = getScenePanel();
        if (scenePanel != null) {
            float zoom = m_dObj.getDataModel().getZoomRatio();
            Node node = getSceneRoot();

            Bounds2D bounds = getNodeLocalBounds(node);
            if (bounds != null) {
                node.set$scaleX(zoom);
                node.set$scaleY(zoom);
                float cx = (bounds.x1 + bounds.x2) / 2;
                float cy = (bounds.y1 + bounds.y2) / 2;
                node.set$translateX(cx * zoom - cx);
                node.set$translateY(cy * zoom - cy);

                scenePanel.invalidate();
                if (scenePanel.getParent() != null) {
                    scenePanel.getParent().validate();
                }
            } else {
                throw new Exception(NbBundle.getMessage( PreviewImagePanel.class,
                        MSG_FAILED_TO_RENDER));

            }
        }
    }

    private Bounds2D getNodeLocalBounds(Node node) {
        Bounds2D bounds = null;
        //node.getLocalBounds(bounds, BaseTransform.IDENTITY_TRANSFORM);
        Bounds localBounds = node.get$boundsInLocal();
        if (localBounds != null){
            bounds = new Bounds2D();
            bounds.x1 = localBounds.$minX;
            bounds.x2 = localBounds.$maxX;
            bounds.y1 = localBounds.$minY;
            bounds.y2 = localBounds.$maxY;
        }
        return bounds;
    }

    private Action[] getPopupActions() {
        ActionLookup lookup = ActionLookupUtils.merge(new ActionLookup[]{
                    PreviewImagePanel.this,
                    m_dObj.getController().getActionController()
                });
        Action[] actions = new Action[]{
            lookup.get(SelectActionFactory.PreviousSelectionAction.class),
            lookup.get(SelectActionFactory.NextSelectionAction.class),
            lookup.get(SelectActionFactory.ParentSelectionAction.class),
            null,
            lookup.get(PreviewImagePanel.ZoomToFitAction.class),
            lookup.get(PreviewImagePanel.ZoomInAction.class),
            lookup.get(PreviewImagePanel.ZoomOutAction.class),
            null,
            lookup.get(HighlightActionFactory.ToggleTooltipAction.class),
            lookup.get(HighlightActionFactory.ToggleHighlightAction.class),
            null,
            lookup.get(ActionController.GenerateUIStubAction.class)
        };
        return actions;
    }

    final class ZoomToFitAction extends AbstractFXDAction {
        private static final long serialVersionUID = 2L;

        ZoomToFitAction() {
            super("zoom_fit"); //NOI18N
        }

        public void actionPerformed(ActionEvent e) {
            float zoom = m_dObj.getDataModel().getZoomRatio();

            JComponent scenePanel = getScenePanel();
            if (scenePanel == null) {
                return;
            }

            Dimension panelSize = getParent().getSize();
            Dimension sceneSize = scenePanel.getSize();

            double xRatio = (panelSize.getWidth() - 2 * ImageHolder.CROSS_SIZE) / 
                    (sceneSize.getWidth() / zoom);
            double yRatio = (panelSize.getHeight() - 2 * ImageHolder.CROSS_SIZE) / 
                    (sceneSize.getHeight() / zoom);
            
            m_dObj.getController().setZoomRatio((float) Math.min( xRatio, yRatio));
        }
    }
    
    final class ZoomInAction extends AbstractFXDAction {
        private static final long serialVersionUID = 2L;

        ZoomInAction() {
            super("zoom_in"); //NOI18N
        }

        public void actionPerformed(ActionEvent e) {
            float zoom = m_dObj.getDataModel().getZoomRatio() * ZOOM_STEP;
            m_dObj.getController().setZoomRatio(zoom);
        }
    }

    final class ZoomOutAction extends AbstractFXDAction {
        private static final long serialVersionUID = 2L;

        ZoomOutAction() {
            super("zoom_out"); //NOI18N
        }

        public void actionPerformed(ActionEvent e) {
            float zoom = m_dObj.getDataModel().getZoomRatio() / ZOOM_STEP;
            m_dObj.getController().setZoomRatio(zoom);
        }
    }
    
    private final class MouseEventCollector implements MouseListener, MouseMotionListener, MouseWheelListener {
        public void mouseClicked(MouseEvent e) {
            processEvent(e);
        }

        public void mousePressed(MouseEvent e) {
            if (!e.isPopupTrigger()){
                processEvent(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (!e.isPopupTrigger()){
                processEvent(e);
            }
        }

        public void mouseEntered(MouseEvent e) {
            processEvent(e);
        }

        public void mouseExited(MouseEvent e) {
            processEvent(e);
            getStatusBar().setText(PreviewStatusBar.CELL_POSITION, "[-,-]");  //NOI18N
        }

        public void mouseDragged(MouseEvent e) {
            processEvent(e);
        }

        public void mouseMoved(MouseEvent e) {
            processEvent(e);
            float zoom = m_dObj.getDataModel().getZoomRatio();
            
            getStatusBar().setText( PreviewStatusBar.CELL_POSITION, String.format("[%d,%d]", Math.round(e.getX()/zoom), Math.round(e.getY()/zoom))); //NOI18N
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0){
                PreviewImagePanel.this.get(ZoomInAction.class).actionPerformed(null);
            } else {
                PreviewImagePanel.this.get(ZoomOutAction.class).actionPerformed(null);
            }
            processEvent(e);
        }

        
        
        protected void processEvent( AWTEvent event) {
            m_dObj.getController().getActionController().processEvent(event);
        }
    }

    private final class PopupListener extends MouseUtils.PopupMouseAdapter {

        private JPopupMenu m_popup;

        public PopupListener() {
            TopComponent tc = m_dObj.getController().getPreviewComponent();

            Action[] actions = getPopupActions();

            m_popup = new JPopupMenu();
            for (int i = 0; i < actions.length; i++) {
                if (actions[i] instanceof Presenter.Popup) {
                    m_popup.add(((Presenter.Popup) actions[i]).getPopupPresenter());
                } else if (actions[i] == null){
                    m_popup.addSeparator();
                } else {
                    m_popup.add(actions[i]);
                }
                if (actions[i] instanceof AbstractFXDAction) {
                    ((AbstractFXDAction) actions[i]).registerAction(tc);
                }
            }
        }

        @Override
        protected void showPopup(MouseEvent e) {
            m_popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public Action get(final Class clazz) {
        return ActionLookupUtils.get(m_actions, clazz);
    }

    protected PreviewStatusBar getStatusBar() {
        return m_dObj.getController().getPreviewComponent().getStatusBar();
    }
}
