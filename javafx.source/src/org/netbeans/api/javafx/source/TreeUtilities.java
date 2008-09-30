/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.api.javafx.source;

import com.sun.javafx.api.tree.ExpressionTree;
import com.sun.javafx.api.tree.JavaFXTreePath;
import com.sun.javafx.api.tree.SyntheticTree.SynthType;
import com.sun.javafx.api.tree.Tree;
import com.sun.javafx.api.tree.JavaFXTreePathScanner;
import com.sun.javafx.api.tree.Scope;
import com.sun.javafx.api.tree.SourcePositions;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javafx.api.JavafxcScope;
import com.sun.tools.javafx.comp.JavafxAttrContext;
import com.sun.tools.javafx.comp.JavafxEnv;
import com.sun.tools.javafx.comp.JavafxResolve;
import com.sun.tools.javafx.tree.JFXBreak;
import com.sun.tools.javafx.tree.JFXContinue;
import com.sun.tools.javafx.tree.JFXTree;
import com.sun.tools.javafx.tree.JavafxPretty;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Random;
import org.netbeans.api.javafx.lexer.JFXTokenId;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.swing.text.Document;
import org.netbeans.api.javafx.source.JavaFXSource.Phase;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Jan Lahoda, Dusan Balek, Tomas Zezula
 */
public final class TreeUtilities {

    private static final Logger logger = Logger.getLogger(TreeUtilities.class.getName());
    private static final boolean LOGGABLE = logger.isLoggable(Level.FINE);
    
    private final CompilationInfo info;
//    private final CommentHandlerService handler;
    
    /** Creates a new instance of CommentUtilities */
    public TreeUtilities(final CompilationInfo info) {
        assert info != null;
        this.info = info;
//        this.handler = CommentHandlerService.instance(info.impl.getJavacTask().getContext());
    }
    
    /**
     * Returns whether or not the given tree is synthetic - generated by the parser.
     * Please note that this method does not check trees transitively - a child of a syntetic tree
     * may be considered non-syntetic.
     * @return true if the given tree is synthetic, false otherwise
     */
    public boolean isSynthetic(JavaFXTreePath path) {
        if (path == null) {
            if (LOGGABLE) log("isSynthetic invoked with null argument");
            return false;
        }
        final Tree leaf = path.getLeaf();
        if (leaf instanceof JFXTree) {
            JFXTree fxLeaf = (JFXTree)leaf;
            SynthType type = fxLeaf.getGenType();
            return SynthType.SYNTHETIC.equals(type);
        }
        if (LOGGABLE) log("isSynthetic returning false because the leaf is not JFXTree.");
        return false;
    }
    
    /**Returns list of comments attached to a given tree. Can return either
     * preceding or trailing comments.
     *
     * @param tree for which comments should be returned
     * @param preceding true if preceding comments should be returned, false if trailing comments should be returned.
     * @return list of preceding/trailing comments attached to the given tree
     */
//    public List<Comment> getComments(Tree tree, boolean preceding) {
//        CommentSetImpl set = handler.getComments(tree);
//        
//        if (!set.areCommentsMapped()) {
//            boolean assertsEnabled = false;
//            boolean automap = true;
//            
//            assert assertsEnabled = true;
//            
//            if (assertsEnabled) {
//                TreePath tp = TreePath.getPath(info.getCompilationUnit(), tree);
//                
//                if (tp == null) {
//                    Logger.getLogger(TreeUtilities.class.getName()).log(Level.WARNING, "Comment automap requested for Tree not from the root compilation info. Please, make sure to call GeneratorUtilities.importComments before Treeutilities.getComments. Tree: {0}", tree);
//                    Logger.getLogger(TreeUtilities.class.getName()).log(Level.WARNING, "Caller", new Exception());
//                    automap = false;
//                }
//            }
//            
//            if (automap) {
//                try {
//                    TokenSequence<JFXTokenId> seq = ((SourceFileObject) info.getCompilationUnit().getSourceFile()).getTokenHierarchy().tokenSequence(JFXTokenId.language());
//                    new TranslateIdentifier(info, true, false, seq).translate(tree);
//                } catch (IOException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//            }
//        }
//        
//        List<Comment> comments = preceding ? set.getPrecedingComments() : set.getTrailingComments();
//        
//        return Collections.unmodifiableList(comments);
//    }
    
    public JavaFXTreePath pathFor(int pos) {
        return pathFor(new JavaFXTreePath(info.getCompilationUnit()), pos);
    }

    /*XXX: dbalek
     */
    public JavaFXTreePath pathFor(JavaFXTreePath path, int pos) {
        return pathFor(path, pos, info.getTrees().getSourcePositions());
    }

    /*XXX: dbalek
     */
    public JavaFXTreePath pathFor(JavaFXTreePath path, int pos, SourcePositions sourcePositions) {
        if (info == null || path == null || sourcePositions == null)
            throw new IllegalArgumentException();
        
        class Result extends Error {
            JavaFXTreePath path;
            Result(JavaFXTreePath path) {
                this.path = path;
            }
        }
        
        class PathFinder extends JavaFXTreePathScanner<Void,Void> {
            private int pos;
            private SourcePositions sourcePositions;
            
            private PathFinder(int pos, SourcePositions sourcePositions) {
                this.pos = pos;
                this.sourcePositions = sourcePositions;
            }
            
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree != null) {
                    super.scan(tree, p);
                    long start = sourcePositions.getStartPosition(getCurrentPath().getCompilationUnit(), tree);
                    long end = sourcePositions.getEndPosition(getCurrentPath().getCompilationUnit(), tree);
                    if (start != -1 && start < pos && end >= pos) {
                        if (tree.getJavaFXKind() == Tree.JavaFXKind.ERRONEOUS) {
                            tree.accept(this, p);
                            throw new Result(getCurrentPath());
                        }
                        throw new Result(new JavaFXTreePath(getCurrentPath(), tree));
                    } else {
                        if ((start == -1) || (end == -1)) {
                            if (!isSynthetic(getCurrentPath())) {
                                // here we might have a problem
                                if (LOGGABLE) {
                                    logger.finest("SCAN: Cannot determine start and end for: " + treeToString(info, tree));
                                }
                            }
                        }
                    }
                }
                return null;
            }
        }
        
        try {
            new PathFinder(pos, sourcePositions).scan(path, null);
        } catch (Result result) {
            path = result.path;
        }
        
        if (path.getLeaf() == path.getCompilationUnit()) {
            log("pathFor returning compilation unit for position: " + pos);
            return path;
        }
        int start = (int)sourcePositions.getStartPosition(info.getCompilationUnit(), path.getLeaf());
        int end   = (int)sourcePositions.getEndPosition(info.getCompilationUnit(), path.getLeaf());
        while (start == -1 || pos < start || pos > end) {
            if (LOGGABLE) {
                logger.finer("pathFor moving to parent: " + treeToString(info, path.getLeaf()));
            }
            path = path.getParentPath();
            if (LOGGABLE) {
                logger.finer("pathFor moved to parent: " + treeToString(info, path.getLeaf()));
            }
            if (path.getLeaf() == path.getCompilationUnit()) {
                break;
            }
            start = (int)sourcePositions.getStartPosition(info.getCompilationUnit(), path.getLeaf());
            end   = (int)sourcePositions.getEndPosition(info.getCompilationUnit(), path.getLeaf());
        }
        if (LOGGABLE) {
            log("pathFor(pos: " + pos + ") returning: " + treeToString(info, path.getLeaf()));
        }
        return path;
    }
    
    /**Computes {@link Scope} for the given position.
     */
    public Scope scopeFor(int pos) {
        JavaFXTreePath path = pathFor(pos);
        Scope scope = info.getTrees().getScope(path);
        return scope;
    }
    
    /**Returns tokens for a given tree.
     */
    public TokenSequence<JFXTokenId> tokensFor(Tree tree) {
        return tokensFor(tree, info.getTrees().getSourcePositions());
    }
    
    /**Returns tokens for a given tree. Uses specified {@link SourcePositions}.
     */
    public TokenSequence<JFXTokenId> tokensFor(Tree tree, SourcePositions sourcePositions) {
        int start = (int)sourcePositions.getStartPosition(info.getCompilationUnit(), tree);
        int end   = (int)sourcePositions.getEndPosition(info.getCompilationUnit(), tree);
        if ((start == -1) || (end == -1)) {
            throw new RuntimeException("RE Cannot determine start and end for: " + treeToString(info, tree));
        }
        TokenSequence<JFXTokenId> t = ((TokenHierarchy<?>)info.getTokenHierarchy()).tokenSequence(JFXTokenId.language());
        if (t == null) {
            throw new RuntimeException("RE SDid not get a token sequence.");
        }
        return t.subSequence(start, end);
    }
    
    private static String treeToString(CompilationInfo info, Tree t) {
        Tree.JavaFXKind k = null;
        StringWriter s = new StringWriter();
        try {
            new JavafxPretty(s, false).printExpr((JFXTree)t);
        } catch (Exception e) {
            if (LOGGABLE) logger.log(Level.FINE, "Unable to pretty print " + t.getJavaFXKind(), e);
        }
        k = t.getJavaFXKind();
        String res = k.toString();
        SourcePositions pos = info.getTrees().getSourcePositions();
        res = res + '[' + pos.getStartPosition(info.getCompilationUnit(), t) + ',' + 
                pos.getEndPosition(info.getCompilationUnit(), t) + "]:" + s.toString();
        return res;
    }

    public ExpressionTree getBreakContinueTarget(JavaFXTreePath breakOrContinue) throws IllegalArgumentException {
        if (info.getPhase().lessThan(Phase.ANALYZED))
            throw new IllegalArgumentException("Not in correct Phase. Required: Phase.RESOLVED, got: Phase." + info.getPhase().toString());
        
        Tree leaf = breakOrContinue.getLeaf();
        
        switch (leaf.getJavaFXKind()) {
            case BREAK:
                return (ExpressionTree) ((JFXBreak) leaf).target;
            case CONTINUE:
                ExpressionTree target = (ExpressionTree) ((JFXContinue) leaf).target;
                
                if (target == null)
                    return null;
                
                // always true with current grammar
                //if (((JFXContinue) leaf).label == null)
                    return target;
                
            default:
                throw new IllegalArgumentException("Unsupported kind: " + leaf.getJavaFXKind());
        }
    }

    /**
     * Parses and analyzes given expression.
     * @param expr String expression to be parsed and analyzed
     * @param pos position in the source where the expression would occur
     * @return parsed expression tree or <code>null</code> if it was not
     *         successfull
     */
    public ExpressionTree parseExpression(String expr, int pos) {
        if (LOGGABLE) log("parseExpression pos= " + pos + " : " + expr);
        try {
            Document d = info.getJavaFXSource().getDocument();
            String start = d.getText(0, pos);
            if (LOGGABLE) log("  start = " + start);
            String end = d.getText(pos, d.getLength()-pos);
            if (LOGGABLE) log("  end = " + end);
            FileSystem fs = FileUtil.createMemoryFileSystem();
            final FileObject fo = fs.getRoot().createData("tmp" + (new Random().nextLong()) + ".fx");
            Writer w = new OutputStreamWriter(fo.getOutputStream());
            w.write(start);
            w.write("\n" + expr+"\n");
            w.write(end);
            w.close();
            if (LOGGABLE) log("  source written to " + fo);
            ClasspathInfo cp = ClasspathInfo.create(info.getFileObject());
            JavaFXSource s = JavaFXSource.create(cp, Collections.singleton(fo));
            if (LOGGABLE) log("  jfxsource obtained " + s);
            CompilationInfoImpl ci = new CompilationInfoImpl(s);
            s.moveToPhase(Phase.ANALYZED, ci, false);
            CompilationController cc = new CompilationController(ci);
            JavaFXTreePath p = cc.getTreeUtilities().pathFor(pos+2);
            if (p == null) {
                if (LOGGABLE) log("  path for returned null");
                return null;
            }
            SourcePositions sp = cc.getTrees().getSourcePositions();
            if (LOGGABLE) log(p.getLeaf().getClass().getName() + "   p = " + p.getLeaf());
            // first loop will try to find our expression
            while ((p != null) && (! (p.getLeaf() instanceof ExpressionTree))) {
                if (LOGGABLE) log(p.getLeaf().getClass().getName() + "   p (2) = " + p.getLeaf());
                p = p.getParentPath();
            }
            if (p == null) {
                if (LOGGABLE) log("  ExpressionTree not found! Returning null");
                return null;
            }
            // the second while loop will try to find as big expression as possible
            JavaFXTreePath pp = p.getParentPath();
            if (LOGGABLE && pp != null) {
                log(pp.getLeaf().getClass().getName() + "   pp = " + pp.getLeaf());
                log("   start == " + sp.getStartPosition(cc.getCompilationUnit(),pp.getLeaf()));
                log("   end == " + sp.getEndPosition(cc.getCompilationUnit(),pp.getLeaf()));
                log("   pos == " + pos);
                log("   pos+length == " + (pos+expr.length()));
                log("   (pp.getLeaf() instanceof ExpressionTree)" + (pp.getLeaf() instanceof ExpressionTree));
            }
            while ((pp != null) && ((pp.getLeaf() instanceof ExpressionTree)) &&
                    (sp.getStartPosition(cc.getCompilationUnit(),pp.getLeaf())>=pos) &&
                    (sp.getEndPosition(cc.getCompilationUnit(),pp.getLeaf())<=(pos+expr.length()))) {
                if (LOGGABLE) log(pp.getLeaf().getClass().getName() + "   p (3) = " + pp.getLeaf());
                p = pp;
                pp = pp.getParentPath();
                if (LOGGABLE) {
                    log(pp.getLeaf().getClass().getName() + "   pp = " + pp.getLeaf());
                    log("   start == " + sp.getStartPosition(cc.getCompilationUnit(),pp.getLeaf()));
                    log("   end == " + sp.getEndPosition(cc.getCompilationUnit(),pp.getLeaf()));
                    log("   (pp.getLeaf() instanceof ExpressionTree)" + (pp.getLeaf() instanceof ExpressionTree));
                }
            }
            if (LOGGABLE) log(p.getLeaf().getClass().getName() + "   p (4) = " + p.getLeaf());
            return (ExpressionTree)p.getLeaf();
        } catch (Exception x) {
            logger.log(Level.FINE, "Exception during parseExpression", x);
        }
        return null;
    }

    /**
     * @param scope
     * @param member
     * @param type
     * @return true if the given member of the given type is accessible in the
     *   given scope
     */
    public boolean isAccessible(Scope 
            scope, Element member, TypeMirror type) {
        if (LOGGABLE) {
            log("isAccessible scope == " + scope);
            log("   member == " + member);
            log("   type == " + type);
        }
        if (scope instanceof JavafxcScope && member instanceof Symbol && type instanceof Type) {
            JavafxResolve resolve = JavafxResolve.instance(info.impl.getContext());
            if (LOGGABLE) log("     resolve == " + resolve);
            Object env = ((JavafxcScope) scope).getEnv();
            JavafxEnv<JavafxAttrContext> fxEnv = (JavafxEnv<JavafxAttrContext>) env;
            if (LOGGABLE) log("     fxEnv == " + fxEnv);
            boolean res = resolve.isAccessible(fxEnv, (Type) type, (Symbol) member);
            if (LOGGABLE) log("     returning " + res);
            // temporary:
            return true;
        } else {
            if (LOGGABLE) log("     returning FALSE from the else branch");
            return false;
        }
    }

    /**
     *
     * @param scope
     * @param type
     * @return true if the class denoted by the type element is accessible
     *   in the given scope
     */
    public boolean isAccessible(Scope
            scope, Element type) {
        if (LOGGABLE) {
            log("isAccessible scope == " + scope);
            log("   type == " + type);
        }
        if (scope instanceof JavafxcScope &&  type instanceof Symbol.TypeSymbol) {
            JavafxResolve resolve = JavafxResolve.instance(info.impl.getContext());
            if (LOGGABLE) log("     resolve == " + resolve);
            Object env = ((JavafxcScope) scope).getEnv();
            JavafxEnv<JavafxAttrContext> fxEnv = (JavafxEnv<JavafxAttrContext>) env;
            if (LOGGABLE) log("     fxEnv == " + fxEnv);
            boolean res = resolve.isAccessible(fxEnv, (Symbol.TypeSymbol) type);
            if (LOGGABLE) log("     returning " + res);
            return res;
        } else {
            if (LOGGABLE) log("     returning FALSE from the else branch");
            return false;
        }
    }

    /**
     *
     * @param scope
     * @return
     */
    public boolean isStaticContext(Scope scope) {
        Object env = ((JavafxcScope) scope).getEnv();
        JavafxEnv<JavafxAttrContext> fxEnv = (JavafxEnv<JavafxAttrContext>) env;
        return JavafxResolve.isStatic(fxEnv);
//        return Resolve.isStatic(((JavafxcScope) scope).getEnv());
    }

    private static void log(String s) {
        if (LOGGABLE) {
            logger.fine(s);
        }
    }
}
