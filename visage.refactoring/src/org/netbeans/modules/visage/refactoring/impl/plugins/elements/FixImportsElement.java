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

package org.netbeans.modules.visage.refactoring.impl.plugins.elements;

import java.util.Set;
import org.netbeans.modules.visage.refactoring.repository.ClassModel;
import org.netbeans.modules.visage.refactoring.repository.ElementDef;
import org.netbeans.modules.visage.refactoring.repository.ImportEntry;
import org.netbeans.modules.visage.refactoring.repository.ImportSet;
import org.netbeans.modules.visage.refactoring.transformations.InsertTextTransformation;
import org.netbeans.modules.visage.refactoring.transformations.RemoveTextTransformation;
import org.netbeans.modules.visage.refactoring.transformations.Transformation;
import org.netbeans.modules.refactoring.api.RefactoringSession;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik <yardus@netbeans.org>
 */
abstract public class FixImportsElement extends BaseRefactoringElementImplementation {

    public FixImportsElement(FileObject srcFO, RefactoringSession session, boolean shouldBackup) {
        super(srcFO, session, shouldBackup);
    }

    public FixImportsElement(FileObject srcFO, RefactoringSession session) {
        super(srcFO, session);
    }

    @Override
    final protected String getRefactoringText() {
        return NbBundle.getMessage(FixImportsElement.class, "LBL_FixImports"); // NOI18N
    }

    final protected void fixImports(Set<ElementDef> movingDefs, ImportSet is, ClassModel cm, boolean isMoving, Set<Transformation> transformations) {
        int lastRemovePos = -1;
        for(ImportEntry ie : is.getUnused()) {
            transformations.add(new RemoveTextTransformation(ie.getStartPos(), ie.getEndPos() - ie.getStartPos()));
            if (ie.getStartPos() > lastRemovePos) {
                lastRemovePos = ie.getStartPos();
            }
        }

        int insertionPos = lastRemovePos > cm.getImportPos() ? lastRemovePos : cm.getImportPos();

        for(ImportSet.Touple<ElementDef, ImportEntry> missing : is.getMissing()) {
            if (isMoving ^ movingDefs.contains(missing.getT1())) {
                transformations.add(new InsertTextTransformation(insertionPos, missing.getT2().toString() + ";\n")); // NOI18N
            }
        }
    }
}
