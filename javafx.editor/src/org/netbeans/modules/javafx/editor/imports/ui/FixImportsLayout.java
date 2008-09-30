/*
 * Copyright (c) 2008, Your Corporation. All Rights Reserved.
 */

package org.netbeans.modules.javafx.editor.imports.ui;

import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.editor.GuardedDocument;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.openide.util.NbBundle;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Copied from Code Completion and shortened.
 *
 * @author Rastislav Komara (<a href="mailto:moonko@netbeans.orgm">RKo</a>)
 * @version 0.9
 */
public final class FixImportsLayout<T extends CompletionItem> {

    public static final int COMPLETION_ITEM_HEIGHT = 16;

    /**
     * Visual shift of the completion window to the left
     * so that the text in the rendered completion items.aligns horizontally
     * with the text in the document.
     */
    private static final int COMPLETION_ANCHOR_HORIZONTAL_SHIFT = 22;

    /**
     * Gap between caret and the displayed popup.
     */
    static final int POPUP_VERTICAL_GAP = 1;

//    private Reference<JTextComponent> editorComponentRef;

    private final FixPopup<T> fixPopup;

    private Stack<PopupWindow<T>> visiblePopups;    

    public static <T extends CompletionItem> FixImportsLayout<T> create() {
        return new FixImportsLayout<T>();
    }

    FixImportsLayout() {
        fixPopup = new FixPopup<T>();
        fixPopup.setLayout(this);
        fixPopup.setPreferDisplayAboveCaret(false);
        visiblePopups = new Stack<PopupWindow<T>>();
    }

    /*public*/ JTextComponent getEditorComponent() {
//        return (editorComponentRef != null)
//                ? editorComponentRef.get()
//                : null;
        return EditorRegistry.lastFocusedComponent();
    }

//    public void setEditorComponent(JTextComponent editorComponent) {
//        hideAll();
//        this.editorComponentRef = new WeakReference<JTextComponent>(editorComponent);
//    }

    private void hideAll() {
        fixPopup.hide();
        visiblePopups.clear();
    }

    @SuppressWarnings({"MethodWithTooManyParameters"})
    public void show(List<T> data, String title, int anchorOffset, ListSelectionListener lsl, 
                     String additionalItemsText, String shortcutHint, int selectedIndex) {
        fixPopup.show(data, title, anchorOffset, lsl, additionalItemsText, shortcutHint, selectedIndex);
        if (!visiblePopups.contains(fixPopup))
            visiblePopups.push(fixPopup);
    }

    public boolean hide() {
        if (fixPopup.isVisible()) {
            fixPopup.hide();
            fixPopup.completionScrollPane = null;
            visiblePopups.remove(fixPopup);
            return true;
        } else { // not visible
            return false;
        }
    }

    public boolean isVisible() {
        return fixPopup.isVisible();
    }

    public T getSelectedItem() {
        return fixPopup.getSelectedCompletionItem();
    }

    public int getSelectedIndex() {
        return fixPopup.getSelectedIndex();
    }

    public void processKeyEvent(KeyEvent evt) {
        for (int i = visiblePopups.size() - 1; i >= 0; i--) {
            PopupWindow<T> popup = visiblePopups.get(i);
            popup.processKeyEvent(evt);
            if (evt.isConsumed())
                return;
        }
    }

    void updateLayout(PopupWindow<T> popup) {
        // Make sure the popup returns its natural preferred size
        popup.resetPreferredSize();

        if (popup == fixPopup) { // completion popup
            popup.showAlongAnchorBounds();
        }
    }

    PopupWindow<T> testGetCompletionPopup() {
        return fixPopup;
    }

    private static final class FixPopup<T extends CompletionItem> extends PopupWindow<T> {

        private InnerScrollPane completionScrollPane;

        @SuppressWarnings({"MethodWithTooManyParameters", "MethodWithMoreThanThreeNegations"})
        public void show(List<T> data, String title, int anchorOffset,
                         ListSelectionListener listSelectionListener, String additionalItemsText, String shortcutHint, int selectedIndex) {

            JTextComponent editorComponent = getEditorComponent();
            if (editorComponent == null) {
                return;
            }

            Dimension lastSize;
            int lastAnchorOffset = getAnchorOffset();

            if (isVisible() && ((getContentComponent() == completionScrollPane) ^ (shortcutHint != null))) {
                lastSize = getContentComponent().getSize();
                resetPreferredSize();

            } else { // not yet visible => create completion scrollpane
                lastSize = new Dimension(0, 0); // no last size => use (0,0)

                completionScrollPane = new InnerScrollPane(
                        editorComponent, listSelectionListener,
                        new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent evt) {
                                JTextComponent c = getEditorComponent();
                                if (SwingUtilities.isLeftMouseButton(evt)) {
                                    if (c != null && evt.getClickCount() == 2) {
                                        CompletionItem selectedItem
                                                = completionScrollPane.getSelectedCompletionItem();
                                        if (selectedItem != null) {
                                            if (c.getDocument() instanceof GuardedDocument && ((GuardedDocument) c.getDocument()).isPosGuarded(c.getSelectionEnd())) {
                                                Toolkit.getDefaultToolkit().beep();
                                            } else {
                                                LogRecord r = new LogRecord(Level.FINE, "COMPL_MOUSE_SELECT"); // NOI18N
                                                r.setParameters(new Object[]{null, completionScrollPane.getSelectedIndex(), selectedItem.getClass().getSimpleName()});
                                                selectedItem.defaultAction(c);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                );

                if (shortcutHint != null) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.add(completionScrollPane, BorderLayout.CENTER);
                    JLabel label = new JLabel();
                    label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.white),
                            BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.gray), BorderFactory.createEmptyBorder(2, 2, 2, 2))));
                    label.setFont(label.getFont().deriveFont((float) label.getFont().getSize() - 2));
                    label.setHorizontalAlignment(SwingConstants.RIGHT);
                    label.setText(NbBundle.getMessage(FixImportsLayout.class, "TXT_completion_shortcut_tips", additionalItemsText, shortcutHint)); //NOI18N
                    panel.add(label, BorderLayout.SOUTH);
                    setContentComponent(panel);
                } else {
                    setContentComponent(completionScrollPane);
                }
            }
            // Set the new data
            completionScrollPane.setData(data, title, selectedIndex);
            setAnchorOffset(anchorOffset);

            Dimension prefSize = getPreferredSize();

            boolean changePopupSize;
            changePopupSize = !isVisible() || (prefSize.height != lastSize.height)
                    || (prefSize.width != lastSize.width)
                    || anchorOffset != lastAnchorOffset;

            if (changePopupSize) {
                // Do not change the popup's above/below caret positioning
                // when the popup is already displayed
                //noinspection unchecked
                getLayout().updateLayout(this);

            } // otherwise present popup size will be retained
        }

        @SuppressWarnings({"unchecked"})
        public T getSelectedCompletionItem() {
            return (T) (isVisible() ? completionScrollPane.getSelectedCompletionItem() : null);
        }

        public int getSelectedIndex() {
            return isVisible() ? completionScrollPane.getSelectedIndex() : -1;
        }

        public void processKeyEvent(KeyEvent evt) {
            if (isVisible()) {
                Object actionMapKey = completionScrollPane.getInputMap().get(
                        KeyStroke.getKeyStrokeForEvent(evt));

                if (actionMapKey != null) {
                    Action action = completionScrollPane.getActionMap().get(actionMapKey);
                    if (action != null) {
                        action.actionPerformed(new ActionEvent(completionScrollPane, 0, null));
                        evt.consume();
                    }
                }
            }
        }

        protected int getAnchorHorizontalShift() {
            return COMPLETION_ANCHOR_HORIZONTAL_SHIFT;
        }

    }

}