/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common
 *  Development and Distribution License("CDDL") (collectively, the
 *  "License"). You may not use this file except in compliance with the
 *  License. You can obtain a copy of the License at
 *  http://www.netbeans.org/cddl-gplv2.html
 *  or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 *  specific language governing permissions and limitations under the
 *  License.  When distributing the software, include this License Header
 *  Notice in each file and include the License file at
 *  nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the GPL Version 2 section of the License file that
 *  accompanied this code. If applicable, add the following below the
 *  License Header, with the fields enclosed by brackets [] replaced by
 *  your own identifying information:
 *  "Portions Copyrighted [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 * 
 *  Portions Copyrighted 1997-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javafx.refactoring.impl.scanners;

import com.sun.javafx.api.tree.FunctionDefinitionTree;
import com.sun.javafx.api.tree.JavaFXTreePathScanner;
import java.util.Collection;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.netbeans.api.javafx.source.CompilationController;
import org.netbeans.api.javafx.source.ElementHandle;
import org.netbeans.modules.javafx.refactoring.impl.WhereUsedElement;
import org.netbeans.modules.javafx.refactoring.impl.javafxc.SourceUtils;
import org.netbeans.modules.javafx.refactoring.impl.javafxc.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.WhereUsedQuery;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
public class FindOverridersScanner extends BaseRefactoringScanner<Void, RefactoringElementsBag> {
    private ExecutableElement methodElement;
    private AbstractRefactoring refactoring;

    public FindOverridersScanner(WhereUsedQuery refactoring, TreePathHandle searchHandle, CompilationController cc) {
        super(searchHandle, cc);
        this.methodElement = (ExecutableElement)searchHandle.resolveElement(cc);
        this.refactoring = refactoring;
    }

    public FindOverridersScanner(WhereUsedQuery refactoring, TreePathHandle searchHandle, ElementHandle handle, CompilationController cc) {
        super(searchHandle, handle, cc);
        this.methodElement = (ExecutableElement)handle.resolve(cc);
        this.refactoring = refactoring;
    }

    @Override
    public Void visitFunctionDefinition(FunctionDefinitionTree node, RefactoringElementsBag elements) {
        if (getElementKind() == ElementKind.METHOD) {
            ExecutableElement element = (ExecutableElement)getCompilationController().getTrees().getElement(getCurrentPath());

            Collection<ExecutableElement> methods = SourceUtils.getOverridenMethods(element, getCompilationController());
            if (methods.contains(methodElement)) {
                elements.add(refactoring, WhereUsedElement.create(TreePathHandle.create(getCurrentPath(), getCompilationController()), Lookups.singleton(getTreePathHandle())));
            }
        }
        return super.visitFunctionDefinition(node, elements);
    }
}