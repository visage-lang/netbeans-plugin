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
package org.netbeans.modules.visage.editor.fold;

import com.sun.visage.api.tree.*;
import com.sun.visage.api.tree.Tree.VisageKind;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.api.visage.lexer.VSGTokenId;
import org.netbeans.api.visage.source.CompilationInfo;
import org.netbeans.api.visage.source.VisageSource;
import org.netbeans.api.visage.source.support.CancellableTreePathScanner;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.editor.ext.java.JavaFoldManager;
import org.netbeans.modules.visage.editor.VisageEditorKit;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.visage.editor.semantic.ScanningCancellableTask;
import org.netbeans.spi.editor.fold.FoldHierarchyTransaction;
import org.netbeans.spi.editor.fold.FoldOperation;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author Jan Lahoda
 */
public class VisageElementFoldManager extends JavaFoldManager {
    
    private static final Logger logger = Logger.getLogger(VisageElementFoldManager.class.getName());
    private static final boolean LOGGABLE = logger.isLoggable(Level.FINE);

    private FoldOperation operation;
    private FileObject    file;
    private VisageElementFoldTask task;
    
    // Folding presets
    private boolean foldImportsPreset;
    private boolean foldInnerClassesPreset;
    private boolean foldJavadocsPreset;
    private boolean foldCodeBlocksPreset;
    private boolean foldInitialCommentsPreset;
    
    /** Creates a new instance of VisageElementFoldManager */
    public VisageElementFoldManager() {
    }

    public void init(FoldOperation operation) {
        this.operation = operation;
        Preferences prefs = MimeLookup.getLookup(VisageEditorKit.FX_MIME_TYPE).lookup(Preferences.class);
        foldInitialCommentsPreset = prefs.getBoolean(SimpleValueNames.CODE_FOLDING_COLLAPSE_INITIAL_COMMENT, foldInitialCommentsPreset);
        foldImportsPreset = prefs.getBoolean(SimpleValueNames.CODE_FOLDING_COLLAPSE_IMPORT, foldImportsPreset);
        foldCodeBlocksPreset = prefs.getBoolean(SimpleValueNames.CODE_FOLDING_COLLAPSE_METHOD, foldCodeBlocksPreset);
        foldInnerClassesPreset = prefs.getBoolean(SimpleValueNames.CODE_FOLDING_COLLAPSE_INNERCLASS, foldInnerClassesPreset);
        foldJavadocsPreset = prefs.getBoolean(SimpleValueNames.CODE_FOLDING_COLLAPSE_JAVADOC, foldJavadocsPreset);
    }

    public synchronized void initFolds(FoldHierarchyTransaction transaction) {
        Document doc = operation.getHierarchy().getComponent().getDocument();
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        
        if (od != null) {
            currentFolds = new HashMap<FoldInfo, Fold>();
            task = VisageElementFoldTask.getTask(od.getPrimaryFile());
            task.setJavaElementFoldManager(VisageElementFoldManager.this);
        }
    }
    
    public void insertUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
    }

    public void removeUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
    }

    public void changedUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
    }

    public void removeEmptyNotify(Fold emptyFold) {
        removeDamagedNotify(emptyFold);
    }

    public void removeDamagedNotify(Fold damagedFold) {
        currentFolds.remove(operation.getExtraInfo(damagedFold));
        if (importsFold == damagedFold) {
            importsFold = null;//not sure if this is correct...
        }
        if (initialCommentFold == damagedFold) {
            initialCommentFold = null;//not sure if this is correct...
        }
    }

    public void expandNotify(Fold expandedFold) {
    }

    public synchronized void release() {
        if (task != null)
            task.setJavaElementFoldManager(null);
        
        task         = null;
        file         = null;
        currentFolds = null;
        importsFold  = null;
        initialCommentFold = null;
    }
    
    private static void dumpPositions(Tree tree, int start, int end) {
        if (!logger.isLoggable(Level.FINER)) {
            return;
        }
        logger.finer("decl = " + tree); // NOI18N
        logger.finer("startOffset = " + start); // NOI18N
        logger.finer("endOffset = " + end); // NOI18N
        
        if (start == (-1) || end == (-1)) {
            logger.finer("ERROR: the positions are outside document."); // NOI18N
        }

    }
    
    static final class VisageElementFoldTask extends ScanningCancellableTask<CompilationInfo> {
        //XXX: this will hold VisageElementFoldTask as long as the FileObject exists:
        private static Map<FileObject, VisageElementFoldTask> file2Task = new WeakHashMap<FileObject, VisageElementFoldTask>();
        
        static VisageElementFoldTask getTask(FileObject file) {
            VisageSource.forFileObject(file); // make sure the VisageSource is loaded ...
            VisageElementFoldTask task = file2Task.get(file);
            
            if (task == null) {
                file2Task.put(file, task = new VisageElementFoldTask());
            }
            
            return task;
        }
        
        private Reference<VisageElementFoldManager> manager;
        
        synchronized void setJavaElementFoldManager(VisageElementFoldManager manager) {
            this.manager = new WeakReference<VisageElementFoldManager>(manager);
        }
        
        public void run(final CompilationInfo info) {
            resume();
            
            VisageElementFoldManager manager;
            
            //the synchronized section should be as limited as possible here
            //in particular, "scan" should not be called in the synchronized section
            //or a deadlock could appear: sy(this)+document read lock against
            //document write lock and this.cancel/sy(this)
            synchronized (this) {
                manager = this.manager != null ? this.manager.get() : null;
            }
            
            if (manager == null)
                return ;
            
            long startTime = System.currentTimeMillis();

            final UnitTree cu = info.getCompilationUnit();
            final VisageElementFoldVisitor v = manager.new VisageElementFoldVisitor(info, cu, info.getTrees().getSourcePositions());
            
            scan(v, cu, null);
            if (LOGGABLE) log("No of folds after scan: " + v.folds.size()); // NOI18N
            if (v.folds.size() == 0) {
                // this is a hack to somehow fool the effects of #133144
                // this should be removed when the error recovery is implemented
                return;
            }
            
            if (v.stopped || isCancelled()) {
                return ;
            }
            
            //check for comments folds:
            v.addCommentsFolds();
            
            if (v.stopped || isCancelled()) {
                return ;
            }
            
            if (LOGGABLE) log("will commit folds: " + v.folds.size()); // NOI18N
            
            SwingUtilities.invokeLater(manager.new CommitFolds(v.folds));
            
            long endTime = System.currentTimeMillis();
            
            Logger.getLogger("TIMER").log(Level.FINE, "Folds - 1", // NOI18N
                    new Object[] {info, endTime - startTime});
        }
        
    }
    
    private class CommitFolds implements Runnable {
        
        private boolean insideRender;
        private List<FoldInfo> infos;
        private long startTime;
        
        public CommitFolds(List<FoldInfo> infos) {
            this.infos = infos;
        }
        
        public void run() {
            if (!insideRender) {
                startTime = System.currentTimeMillis();
                insideRender = true;
                operation.getHierarchy().getComponent().getDocument().render(this);
                
                return;
            }
            
            operation.getHierarchy().lock();
            
            try {
                FoldHierarchyTransaction tr = operation.openTransaction();
                
                try {
                    if (currentFolds == null)
                        return ;
                    
                    Map<FoldInfo, Fold> added   = new TreeMap<FoldInfo, Fold>();
                    List<FoldInfo>      removed = new ArrayList<FoldInfo>(currentFolds.keySet());
                    
                    for (FoldInfo i : infos) {
                        if (removed.remove(i)) {
                            continue ;
                        }
                        
                        int start = i.start.getOffset();
                        int end   = i.end.getOffset();
                        
                        if (end > start && (end - start) > (i.template.getStartGuardedLength() + i.template.getEndGuardedLength())) {
                            Fold f    = operation.addToHierarchy(i.template.getType(),
                                                                 i.template.getDescription(),
                                                                 i.collapseByDefault,
                                                                 start,
                                                                 end,
                                                                 i.template.getStartGuardedLength(),
                                                                 i.template.getEndGuardedLength(),
                                                                 i,
                                                                 tr);
                            
                            added.put(i, f);
                            
                            if (i.template == IMPORTS_FOLD_TEMPLATE) {
                                importsFold = f;
                            }
                            if (i.template == INITIAL_COMMENT_FOLD_TEMPLATE) {
                                initialCommentFold = f;
                            }
                        }
                    }
                    
                    for (FoldInfo i : removed) {
                        Fold f = currentFolds.remove(i);
                        
                        operation.removeFromHierarchy(f, tr);
                        
                        if (importsFold == f ) {
                            importsFold = null;
                        }
                        
                        if (initialCommentFold == f) {
                            initialCommentFold = f;
                        }
                    }
                    
                    currentFolds.putAll(added);
                } catch (BadLocationException e) {
                    ErrorManager.getDefault().notify(e);
                } finally {
                    tr.commit();
                }
            } finally {
                operation.getHierarchy().unlock();
            }
            
            long endTime = System.currentTimeMillis();
            
            Logger.getLogger("TIMER").log(Level.FINE, "Folds - 2", // NOI18N
                    new Object[] {file, endTime - startTime});
        }
    }
    
    private Map<FoldInfo, Fold> currentFolds;
    private Fold initialCommentFold;
    private Fold importsFold;
    
    private final class VisageElementFoldVisitor extends CancellableTreePathScanner<Object, Object> {

        private List<FoldInfo> folds = new ArrayList<VisageElementFoldManager.FoldInfo>();
        private CompilationInfo info;
        private UnitTree cu;
        private SourcePositions sp;
        private boolean stopped;
        
        public VisageElementFoldVisitor(CompilationInfo info, UnitTree cu, SourcePositions sp) {
            this.info = info;
            this.cu = cu;
            this.sp = sp;
        }
        
        private void addCommentsFolds() {
            // A safe TokenHierarchy, based on a snapshot, never throws CME
            TokenHierarchy<?> th = info.getTokenHierarchy();
            if (th == null) {
                if (LOGGABLE) log("addCommentsFolds returning because of null token hierarchy."); // NOI18N
                return;
            }

            Document doc = operation.getHierarchy().getComponent().getDocument();
            if (doc == null) {
                return;
            }

            TokenSequence<VSGTokenId> ts = th.tokenSequence(VSGTokenId.language());
            boolean firstNormalFold = true;
            while (ts.moveNext()) {
                Token<VSGTokenId> token = ts.token();
                try {
                    if (token.id() == VSGTokenId.DOC_COMMENT) {
                        int startOffset = ts.offset();
                        if (LOGGABLE) log("addCommentsFolds (DOC_COMMENT) adding fold [" + startOffset + ":" + (startOffset + token.length())+"] preset == " + foldJavadocsPreset); // NOI18N
                        folds.add(new FoldInfo(doc, startOffset, startOffset + token.length(), JAVADOC_FOLD_TEMPLATE, foldJavadocsPreset));
                    }
                    if (token.id() == VSGTokenId.COMMENT) {
                        int startOffset = ts.offset();
                        if (LOGGABLE) log("addCommentsFolds (COMMENT) adding fold [" + startOffset + ":" + (startOffset + token.length())+"]"); // NOI18N
                        if (firstNormalFold) {
                            if (LOGGABLE) log("foldInitialCommentsPreset == " + foldInitialCommentsPreset + " on " + token.text()); // NOI18N
                        }
                        folds.add(new FoldInfo(doc, startOffset, startOffset + token.length(), INITIAL_COMMENT_FOLD_TEMPLATE, firstNormalFold ? foldInitialCommentsPreset : false));
                        firstNormalFold = false;
                    }
                } catch (BadLocationException ble) {
                    if (LOGGABLE) {
                        logger.log(Level.FINE, "addDocComments continuing", ble); // NOI18N
                    }
                }
            }
        }
        
        private void handleTree(Tree node, Tree javadocTree, boolean handleOnlyJavadoc) {
            try {
                if (!handleOnlyJavadoc) {
                    Document doc = operation.getHierarchy().getComponent().getDocument();
                    int start = (int)sp.getStartPosition(cu, node);
                    int end   = (int)sp.getEndPosition(cu, node);
                    if (node.getVisageKind() == VisageKind.BLOCK_EXPRESSION) {
                        end = findBodyEnd(node, cu, sp, doc);
                    }
                    VisageTreePath pa = getCurrentPath(); //VisageTreePath.getPath(cu, node);
                    if (start != (-1) && end != (-1) &&
                            !info.getTreeUtilities().isSynthetic(pa)) {
                        
                        if (LOGGABLE) log("handleTree adding fold [" + start + ":" + end + "]"); // NOI18N
                        if (LOGGABLE) log("  for tree: " + node); // NOI18N
                        folds.add(new FoldInfo(doc, start, end, CODE_BLOCK_FOLD_TEMPLATE, foldCodeBlocksPreset));
                    } else {
                        // debug:
                        dumpPositions(node, start, end);
                    }
                }
            } catch (BadLocationException e) {
                //the document probably changed, stop
                stopped = true;
            }
        }

        @Override
        public Object visitInstantiate(InstantiateTree node, Object p) {
            super.visitInstantiate(node, p);
            try {
                Document doc = operation.getHierarchy().getComponent().getDocument();
                int start = findBodyStart(node, cu, sp, doc);
                int end   = (int)sp.getEndPosition(cu, node);

                if (start != (-1) && end != (-1)) {
                    if (LOGGABLE) log("visitInstantiate adding fold [" + start + ":" + end + "] for tree: " + node); // NOI18N
                    folds.add(new FoldInfo(doc, start, end, CODE_BLOCK_FOLD_TEMPLATE, foldInnerClassesPreset));
                } else {
                    dumpPositions(node, start, end);
                }
            } catch (BadLocationException e) {
                //the document probably changed, stop
                stopped = true;
            }
            return null;
        }
        
        @Override
        public Object visitObjectLiteralPart(ObjectLiteralPartTree node, Object p) {
            super.visitObjectLiteralPart(node, p);
            handleTree(node.getExpression(), null, true);
            return null;
        }

        @Override
        public Object visitClassDeclaration(ClassDeclarationTree node, Object p) {
            super.visitClassDeclaration(node, p);
            try {
                Document doc = operation.getHierarchy().getComponent().getDocument();
                int start = findBodyStart(node, cu, sp, doc);
                int end   = findBodyEnd(node, cu, sp, doc);
                VisageTreePath pa = getCurrentPath();
                if (start != (-1) && end != (-1) &&
                        !info.getTreeUtilities().isSynthetic(pa)) {
                    if (LOGGABLE) log("visitClassDeclaration adding fold [" + start + ":" + end + "] for tree: " + node); // NOI18N
                    folds.add(new FoldInfo(doc, start, end, CODE_BLOCK_FOLD_TEMPLATE, foldInnerClassesPreset));
                } else {
                    dumpPositions(node, start, end);
                }
            } catch (BadLocationException e) {
                //the document probably changed, stop
                stopped = true;
            }
            return null;
        }
        
        @Override
        public Object visitBlockExpression(BlockExpressionTree node, Object p) {
            super.visitBlockExpression(node, p);
            handleTree(node, node, false);
            return null;
        }

        @Override
        public Object visitVariable(VariableTree node,Object p) {
            super.visitVariable(node, p);
            handleTree(node, null, true);
            return null;
        }
        
        @Override
        public Object visitFunctionDefinition(FunctionDefinitionTree node, Object p) {
            super.visitFunctionDefinition(node, p);
            handleTree(node, null, true);
            return null;
        }

        @Override
        public Object visitSequenceExplicit(SequenceExplicitTree node, Object p) {
            super.visitSequenceExplicit(node, p);
            handleTree(node, null, false);
            return null;
        }
        
        @Override
        public Object visitCompilationUnit(UnitTree node, Object p) {
            int importsStart = Integer.MAX_VALUE;
            int importsEnd   = -1;
            
            for (ImportTree imp : node.getImports()) {
                int start = (int) sp.getStartPosition(cu, imp);
                int end   = (int) sp.getEndPosition(cu, imp);
                
                if (importsStart > start)
                    importsStart = start;
                
                if (end > importsEnd) {
                    importsEnd = end;
                }
            }
            
            if (importsEnd != (-1) && importsStart != (-1)) {
                try {
                    Document doc   = operation.getHierarchy().getComponent().getDocument();
                    boolean collapsed = foldImportsPreset;
                    
                    if (importsFold != null) {
                        collapsed = importsFold.isCollapsed();
                    }
                    
                    importsStart += 7/*"import ".length()*/; // NOI18Nitor
                    
                    if (importsStart < importsEnd) {
                        if (LOGGABLE) log("visitCompilationUnit adding fold [" + importsStart + ":" + importsEnd + "]"); // NOI18N
                        folds.add(new FoldInfo(doc, importsStart , importsEnd, IMPORTS_FOLD_TEMPLATE, collapsed));
                    }
                } catch (BadLocationException e) {
                    //the document probably changed, stop
                    stopped = true;
                }
            }
            return super.visitCompilationUnit(node, p);
        }
    }
    
    protected static final class FoldInfo implements Comparable {
        
        private Position start;
        private Position end;
        private FoldTemplate template;
        private boolean collapseByDefault;
        
        public FoldInfo(Document doc, int start, int end, FoldTemplate template, boolean collapseByDefault) throws BadLocationException {
            this.start = doc.createPosition(start);
            this.end   = doc.createPosition(end);
            this.template = template;
            this.collapseByDefault = collapseByDefault;
        }
        
        @Override
        public int hashCode() {
            return 1;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FoldInfo))
                return false;
            
            return compareTo(o) == 0;
        }
        
        public int compareTo(Object o) {
            FoldInfo remote = (FoldInfo) o;
            
            if (start.getOffset() < remote.start.getOffset()) {
                return -1;
            }
            
            if (start.getOffset() > remote.start.getOffset()) {
                return 1;
            }
            
            if (end.getOffset() < remote.end.getOffset()) {
                return -1;
            }
            
            if (end.getOffset() > remote.end.getOffset()) {
                return 1;
            }
            
            return 0;
        }
        
    }

    public static int findBodyStart(final Tree cltree, final UnitTree cu, final SourcePositions positions, final Document doc) {
        final int[] result = new int[1];
        doc.render(new Runnable() {
            public void run() {
                result[0] = findBodyStartImpl(cltree, cu, positions, doc);
            }
        });
        return result[0];
    }
    
    private static int findBodyStartImpl(Tree cltree, UnitTree cu, SourcePositions positions, Document doc) {
        int start = (int)positions.getStartPosition(cu, cltree);
        int end   = (int)positions.getEndPosition(cu, cltree);
        if (start == (-1) || end == (-1)) {
            dumpPositions(cltree, start, end);
            return -1;
        }
        if (start > doc.getLength() || end > doc.getLength()) {
            dumpPositions(cltree, start, end);
            return -1;
        }
        try {
            String text = doc.getText(start, end - start);
            int index = text.indexOf('{'); // NOI18N
            if (index == (-1)) {
                return -1;
            }
            return start + index;
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        }
        return -1;
    }
    
    public static int findBodyEnd(final Tree cltree, final UnitTree cu, final SourcePositions positions, final Document doc) {
        final int[] result = new int[1];
        doc.render(new Runnable() {
            public void run() {
                result[0] = findBodyEndImpl(cltree, cu, positions, doc);
            }
        });
        return result[0];
    }

    private static int findBodyEndImpl(Tree cltree, UnitTree cu, SourcePositions positions, Document doc) {
        if (LOGGABLE) log("findBodyEndImpl for " + cltree); // NOI18N
        int end   = (int)positions.getEndPosition(cu, cltree);
        if (end <= 0) {
            return -1;
        }
        if (end > doc.getLength()) {
            return -1;
        }
        try {
            String text = doc.getText(end-1, doc.getLength() - end + 1);
            if (LOGGABLE) log("      text == " + text); // NOI18N
            int index = text.indexOf('}');
            if (LOGGABLE) log("      index == " + index); // NOI18N
            if (index == -1) {
                if (LOGGABLE) log("findBodyEndImpl returning original end (index==-1)" + end); // NOI18N
                return end;
            }
            int ind2 = text.indexOf('{');
            if (ind2 != -1 && ind2 < index) {
                if (LOGGABLE) log("findBodyEndImpl returning original end " + end + " ind2 == " + ind2); // NOI18N
                return end;
            }
            if (LOGGABLE) log("findBodyEndImpl returning " + (end + index) + " instead of " + end); // NOI18N
            return end + index;
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        }
        return -1;
    }

    private static void log(String s) {
        if (LOGGABLE) {
            logger.fine(s);
        }
    }
}
