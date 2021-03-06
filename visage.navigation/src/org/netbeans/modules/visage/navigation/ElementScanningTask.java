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
package org.netbeans.modules.visage.navigation;

import com.sun.tools.mjavac.code.Symbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.visage.editor.VisageSourceUtils;
import org.netbeans.api.visage.source.CancellableTask;
import org.netbeans.api.visage.source.CompilationInfo;
import org.netbeans.api.visage.source.ElementHandle;
import org.netbeans.modules.visage.navigation.ElementNode.Description;
import org.visage.api.tree.ClassDeclarationTree;
import org.visage.api.tree.FunctionDefinitionTree;
import org.visage.api.tree.InitDefinitionTree;
import org.visage.api.tree.OverrideClassVarTree;
import org.visage.api.tree.SourcePositions;
import org.visage.api.tree.Tree;
import org.visage.api.tree.UnitTree;
import org.visage.api.tree.VariableTree;
import org.visage.api.tree.VisageTreePath;
import org.visage.api.tree.VisageTreePathScanner;
import org.visage.tools.api.VisagecScope;
import org.visage.tools.api.VisagecTrees;
import org.visage.tools.code.VisageTypes;

/** 
 *
 * @author phrebejk
 * @author Anton Chechel (space magic fixes only)
 */
public class ElementScanningTask implements CancellableTask<CompilationInfo> {

    private ClassMemberPanelUI ui;
    private final AtomicBoolean canceled = new AtomicBoolean();

    public ElementScanningTask(ClassMemberPanelUI ui) {
        this.ui = ui;
    }

    public void cancel() {
        canceled.set(true);
    }

    public void run(CompilationInfo info) throws Exception {
        canceled.set(false); // Task shared for one file needs reset first

        Description rootDescription = new Description(ui);
        rootDescription.fileObject = info.getFileObject();
        rootDescription.subs = new HashSet<Description>();

        // Get all outerclasses in the Compilation unit
        UnitTree cuTree = info.getCompilationUnit();
        List<? extends TypeElement> elements = info.getTopLevelElements();

        final Map<Element, Long> pos = new HashMap<Element, Long>();
        if (!canceled.get()) {
            VisagecTrees trees = info.getTrees();
            PositionVisitor posVis = new PositionVisitor(info, trees, canceled);
            posVis.scan(cuTree, pos);
        }

        if (!canceled.get()) {
            Element magicRunMethod = null;
            out:
            for (Element element : elements) {
                Symbol.ClassSymbol sym = (Symbol.ClassSymbol) element;
                for (Symbol symbol : sym.members().getElements()) {
                    if (SpaceMagicUtils.isSpiritualMethod(symbol)) {
                        magicRunMethod = symbol;
                        break out;
                    }
                }
            }

            // top level elements
            for (Element element : elements) {
                Description topLevel = element2description(element, null, false, info, pos);
                if (null != topLevel) {
                    rootDescription.subs.add(topLevel);
                    addMembers((TypeElement) element, topLevel, info, pos);
                }
            }

            // elements from magic visage$run$ method, should be top level as well
            if (magicRunMethod != null) {
                addMembers((ExecutableElement) magicRunMethod, rootDescription, info, pos);
            }
        }

        if (!canceled.get()) {
            ui.refresh(rootDescription, info);
        }
    }

    private static class PositionVisitor extends VisageTreePathScanner<Void, Map<Element, Long>> {

        private final CompilationInfo info;
        private final VisagecTrees trees;
        private final SourcePositions sourcePositions;
        private final AtomicBoolean canceled;
        private UnitTree cu;

        public PositionVisitor(final CompilationInfo info, final VisagecTrees trees, final AtomicBoolean canceled) {
            assert trees != null;
            assert canceled != null;
            this.info = info;
            this.trees = trees;
            this.sourcePositions = trees.getSourcePositions();
            this.canceled = canceled;
        }

        @Override
        public Void visitCompilationUnit(UnitTree node, Map<Element, Long> p) {
            this.cu = node;
            return super.visitCompilationUnit(node, p);
        }

        @Override
        public Void visitClassDeclaration(ClassDeclarationTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);
            }
            return super.visitClassDeclaration(node, p);
        }

        @Override
        public Void visitFunctionDefinition(FunctionDefinitionTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);

                // guesss what? space magic! weeeee! >:-((
                if (SpaceMagicUtils.isSpiritualMethod(e)) {
                    List<Element> spiritualMembers = SpaceMagicUtils.getSpiritualMembers(info);
                    for (Element sm : spiritualMembers) {
                        VisageTreePath smPath = info.getPath(sm);
                        Tree smTree = smPath != null ? smPath.getLeaf() : null;

                        if (smTree != null) {
                            long smPos = this.sourcePositions.getStartPosition(cu, smTree);
                            p.put(sm, smPos);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);
            }
            return null;
        }

        @Override
        public Void visitOverrideClassVar(OverrideClassVarTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);
            }
            return null;
        }

        @Override
        public Void visitInitDefinition(InitDefinitionTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);
            }
            return null;
        }

        @Override
        public Void visitPostInitDefinition(InitDefinitionTree node, Map<Element, Long> p) {
            Element e = this.trees.getElement(this.getCurrentPath());
            if (e != null) {
                long pos = this.sourcePositions.getStartPosition(cu, node);
                p.put(e, pos);
            }
            return null;
        }

        @Override
        public Void scan(Tree tree, Map<Element, Long> p) {
            if (!canceled.get()) {
                return super.scan(tree, p);
            } else {
                return null;
            }
        }
    }

    private void addMembers(final TypeElement e, final Description parentDescription, final CompilationInfo info, final Map<Element, Long> pos) {
        if (e == null || e.asType().getKind() == TypeKind.ERROR) {
            return;
        }
        final List<? extends Element> allMembers = VisageSourceUtils.getAllMembers(info.getElements(), e);
        for (Element m : allMembers) {
            if (canceled.get()) {
                return;
            }
            Description d = element2description(m, e, parentDescription.isInherited, info, pos);
            if (null != d) {
                parentDescription.subs.add(d);
                if (m instanceof TypeElement && !d.isInherited) {
                    addMembers((TypeElement) m, d, info, pos);
                }
            }
        }
    }

    private void addMembers(final ExecutableElement e, final Description parentDescription, final CompilationInfo info, final Map<Element, Long> pos) {
        if (e == null) {
            return;
        }

        List<Element> spiritualMembers = SpaceMagicUtils.getSpiritualMembers(info);
        for (Element el : spiritualMembers) {
            Description d = element2description(el, e, parentDescription.isInherited, info, pos);
            if (null != d) {
                parentDescription.subs.add(d);
                if (el instanceof TypeElement && !d.isInherited) {
                    addMembers((TypeElement) el, d, info, pos);
                }
            }
        }
    }

    private Description element2description(final Element e, final Element parent,
            final boolean isParentInherited, final CompilationInfo info,
            final Map<Element, Long> pos) {

        final Name simpleName = e.getSimpleName();
        final String name = simpleName != null ? simpleName.toString() : "<null>"; // NOI18N
        final boolean spaceMagic = SpaceMagicUtils.isSpiritualMethod(e.getEnclosingElement());
        if (!spaceMagic && info.getElementUtilities().isSynthetic(e)) {
            return null;
        }

        final boolean inherited = isParentInherited || (null != parent && !parent.equals(e.getEnclosingElement()));
        final ElementKind kind = e.getKind();
        final ElementKind spaceMagicKind = kind == ElementKind.LOCAL_VARIABLE ? ElementKind.FIELD : kind;

        ElementHandle<Element> eh = null;
        try {
            eh = ElementHandle.create(e);
        } catch (Exception ex) {
            // can't convert to element handler (incomplete element)
        }
        if (eh == null) {
            return null;
        }

        Description d = new Description(ui, name, eh, spaceMagicKind, inherited);
        final VisageTypes visageTypes = info.getVisageTypes();
        final boolean isDeprecated = info.getElements().isDeprecated(e);

        if (e instanceof TypeElement) {
            if (null != parent) {
                final VisagecTrees trees = info.getTrees();
                final VisagecScope scope = trees.getScope(info.getPath(parent));
                if (!trees.isAccessible(scope, (TypeElement) e)) {
                    return null;
                }
            }
            d.subs = new HashSet<Description>();
            d.htmlHeader = VisageSourceUtils.typeElementToString(visageTypes, (TypeElement) e, isDeprecated, d.isInherited);
        } else if (e instanceof ExecutableElement) {
            if (!spaceMagic && name.contains("$") && !name.contains("init")) { // NOI18N
                return null;
            }
            d.htmlHeader = VisageSourceUtils.executableElementToString(visageTypes, (ExecutableElement) e, isDeprecated, d.isInherited);
        } else if (e instanceof VariableElement) {
            if (!spaceMagic && kind != ElementKind.FIELD && kind != ElementKind.ENUM_CONSTANT) {
                return null;
            }
            d.htmlHeader = VisageSourceUtils.variableElementToString(visageTypes, (VariableElement) e, isDeprecated, d.isInherited);
        }

        d.modifiers = VisageSourceUtils.getModifiers(e);
        d.pos = getPosition(e, pos);

        return d;
    }

    private static long getPosition(final Element e, final Map<Element, Long> pos) {
        Long res = pos.get(e);
        if (res == null) {
            return -1;
        }
        return res.longValue();
    }
}
