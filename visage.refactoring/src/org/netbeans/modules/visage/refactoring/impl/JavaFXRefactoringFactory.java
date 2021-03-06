/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 *  Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 *  Other names may be trademarks of their respective owners.
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
 *  nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the GPL Version 2 section of the License file that
 *  accompanied this code. If applicable, add the following below the
 *  License Header, with the fields enclosed by brackets [] replaced by
 *  your own identifying information:
 *  "Portions Copyrighted [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 * 
 *  Portions Copyrighted 1997-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.visage.refactoring.impl;

import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.visage.refactoring.impl.visagec.SourceUtils;
import org.netbeans.modules.visage.refactoring.impl.plugins.CopyRefactoringPlugin;
import org.netbeans.modules.visage.refactoring.impl.plugins.MoveRefactoringPlugin;
import org.netbeans.modules.visage.refactoring.impl.plugins.RenamePackagePlugin;
import org.netbeans.modules.visage.refactoring.impl.plugins.RenameRefactoringPlugin;
import org.netbeans.modules.visage.refactoring.impl.plugins.SafeDeleteRefactoringPlugin;
import org.netbeans.modules.visage.refactoring.impl.plugins.WhereUsedQueryPlugin;
import org.netbeans.modules.visage.refactoring.repository.ElementDef;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.MoveRefactoring;
import org.netbeans.modules.refactoring.api.MultipleCopyRefactoring;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.api.SafeDeleteRefactoring;
import org.netbeans.modules.refactoring.api.SingleCopyRefactoring;
import org.netbeans.modules.refactoring.api.WhereUsedQuery;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringPluginFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jaroslav Bachorik
 */
@ServiceProvider(service=RefactoringPluginFactory.class)
public class VisageRefactoringFactory implements RefactoringPluginFactory {

    public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
        // disable visage refactoring for NB6.8 Beta
//        if (!Boolean.getBoolean("visage.refactoring")) return null;

        Lookup look = refactoring.getRefactoringSource();
        FileObject file = look.lookup(FileObject.class);
        NonRecursiveFolder folder = look.lookup(NonRecursiveFolder.class);
        ElementDef elDef = look.lookup(ElementDef.class);

        TreePathHandle javaHandle = look.lookup(TreePathHandle.class);

        if (refactoring instanceof WhereUsedQuery) {
            if (elDef == null && javaHandle == null) return null;
            return new WhereUsedQueryPlugin((WhereUsedQuery)refactoring);
        }

        if (refactoring instanceof RenameRefactoring) {
            if (elDef != null) {
                return new RenameRefactoringPlugin(((RenameRefactoring)refactoring));
            }
            if ((elDef !=null && elDef.getStartPos() > -1) || (elDef == null && ((file!=null) && SourceUtils.isVisageFile(file)))) {
                //rename visage file, class, method etc..
                return new RenameRefactoringPlugin((RenameRefactoring)refactoring);
            } else if (file!=null && SourceUtils.isOnSourceClasspath(file) && file.isFolder()) {
                //rename folder
                return new RenamePackagePlugin((RenameRefactoring)refactoring);
            } else if (folder!=null && SourceUtils.isOnSourceClasspath(folder.getFolder())) {
                //rename package
                return new RenamePackagePlugin((RenameRefactoring)refactoring);
            }
            if (javaHandle != null) {
                return new RenameRefactoringPlugin((RenameRefactoring)refactoring);
            }
        }

        if (refactoring instanceof MoveRefactoring) {
            if (checkMove(refactoring.getRefactoringSource())) {
                return new MoveRefactoringPlugin((MoveRefactoring) refactoring);
            }
        }

        if (refactoring instanceof SingleCopyRefactoring  || refactoring instanceof MultipleCopyRefactoring) {
            if (checkCopy(refactoring.getRefactoringSource())) {
                return new CopyRefactoringPlugin(refactoring);
            }
        }

        if (refactoring instanceof SafeDeleteRefactoring) {
            return new SafeDeleteRefactoringPlugin((SafeDeleteRefactoring)refactoring);
        }

        return null;
    }

    private boolean checkMove(Lookup refactoringSource) {
        for (FileObject f:refactoringSource.lookupAll(FileObject.class)) {
            if (SourceUtils.isVisageFile(f) || f.getExt().toLowerCase().equals("java")) { // NOI18N
                return true;
            }
            if (f.isFolder()) {
                return true;
            }
        }
        return false;
    }

    private boolean checkCopy(Lookup object) {
        FileObject f=object.lookup(FileObject.class);
        if (f!=null && SourceUtils.isVisageFile(f))
            return true;
        return false;
    }

}
