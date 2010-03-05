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

package org.netbeans.modules.javafx.fxd.composer.editor.completion.providers;

import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.structure.api.DocumentElement;
import org.netbeans.modules.javafx.fxd.composer.lexer.FXDTokenId;
import org.netbeans.modules.javafx.fxd.composer.lexer.TokenUtils;
import org.netbeans.spi.editor.completion.CompletionResultSet;

/**
 *
 * @author Andrey Korostelev
 */
public class ArrayElemCompletionProvider extends AbstractCompletionProvider {

    @Override
    protected void fillCompletionItems(CompletionResultSet resultSet, DocumentElement el, int caretOffset, TokenSequence<FXDTokenId> ts) {
        FXDTokenId prev = getPrevNonWhiteID(el, caretOffset, ts);
        FXDTokenId next = getNextNonWhiteID(el, caretOffset, ts);
        if (prev == null && next == FXDTokenId.IDENTIFIER){
            if (caretOffset <= ts.offset() ) {
                // right before an id
                processAttrValue(resultSet, el.getParentElement(), caretOffset);
            } else {
                // inside id
                processArrElemId(resultSet, el, caretOffset);
            }
        } else if (prev == FXDTokenId.IDENTIFIER) {
            // move ts to previous non-white token
            Token<FXDTokenId> prevT = TokenUtils.getNextNonWhiteBwd(ts, caretOffset);
            if (ts.offset() + prevT.length() == caretOffset) {
                // at the end of id
                processArrElemId(resultSet, el, caretOffset);
            }
            // after id. nothing to suggest?
        }

    }

    private void processArrElemId(final CompletionResultSet resultSet,
            DocumentElement el, int caretOffset) {
        String nameStart = el.getName().substring(0, caretOffset - el.getStartOffset());

        // get array attr containgng this elem
        DocumentElement parent = el.getParentElement();
        if (parent == null) {
            return;
        }
        // get it's parent node
        parent = parent.getParentElement();
        if (parent == null) {
            return;
        }
        // get parent properties with nameStart pattern
        fillItemsWithNodeAttrs(resultSet, parent, caretOffset, nameStart);
    }

}
