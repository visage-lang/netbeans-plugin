/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.javafx.editor.hints;


import com.sun.javafx.api.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


final class UncaughtExceptionsModel {

    private Collection<UncaughtExceptionsModel.Hint> unresovedHints;

    public void addThrowHint(List<Type> thrownExceptions, Tree tree) {
        if (unresovedHints == null) {
            unresovedHints = new HashSet<Hint>();
        }

        assert thrownExceptions != null;
        Hint hint = new Hint(thrownExceptions, tree);
        unresovedHints.add(hint);
    }

    public void addCatchTree(Hint hint, Tree catchTree) {
        assert unresovedHints != null;
        if (!unresovedHints.contains(hint)) {
            throw new IllegalArgumentException("Hint does not existst in Set of added hints"); //NOI18N
        }
        hint.setCatchTree(catchTree);
    }

    public Collection<Hint> getThrowHints() {
        if (unresovedHints == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableCollection(unresovedHints);
    }

    public void removeHint(Hint hint) {
        unresovedHints.remove(hint);
    }

    static final class Hint {

        private List<Type> exceptions;
        private Tree tree;
        private Tree catchTree;

        private Hint() {}

        private Hint(List<Type> thrownExceptions, Tree tree) {
            this.exceptions = new ArrayList<Type>(thrownExceptions);
            this.tree = tree;
        }

        private void setCatchTree(Tree catchTree) {
            this.catchTree = catchTree;
        }

        Tree getTree() {
            return tree;
        }

        Tree getCatchTree() {
            return catchTree;
        }

        Collection<Type> getExceptions() {
            return Collections.unmodifiableList(exceptions);
        }

        void removeException(Type type) {
            exceptions.remove(type);
        }

    }
}


