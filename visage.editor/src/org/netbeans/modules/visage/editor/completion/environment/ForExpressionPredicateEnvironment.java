/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.visage.editor.completion.environment;

import org.netbeans.api.visage.lexer.VisageTokenId;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.visage.editor.completion.VisageCompletionEnvironment;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.NbBundle;
import org.visage.tools.tree.VisageForExpression;

/**
 * seq[x | condition]
 * @author David Strupl
 */
public class ForExpressionPredicateEnvironment extends VisageCompletionEnvironment<VisageForExpression> {
    
    private static final Logger logger = Logger.getLogger(ForExpressionPredicateEnvironment.class.getName());
    private static final boolean LOGGABLE = logger.isLoggable(Level.FINE);

    @Override
    protected void inside(VisageForExpression foe) throws IOException {
        if (LOGGABLE) log("inside ForExpressionPredicateEnvironment " + foe); // NOI18N
        if (LOGGABLE) log("  prefix: " + prefix); // NOI18N
        int start = (int)sourcePositions.getStartPosition(root, foe);
        if (LOGGABLE) log("  offset: " + offset); // NOI18N
        if (LOGGABLE) log("  start: " + start); // NOI18N
        TokenSequence<VisageTokenId> ts = ((TokenHierarchy<?>)controller.getTokenHierarchy()).tokenSequence(VisageTokenId.language());
        ts.move(start);
        boolean afterIdentifier = false;
        boolean afterPipe = false;
        boolean afterLBracket = false;
        WHILE: while (ts.moveNext()) {
            if (ts.offset() >= offset) {
                break;
            }
            switch (ts.token().id()) {
                case PIPE:
                    afterPipe = true;
                    break;
                case LBRACKET:
                    if (afterLBracket) {
                        if (LOGGABLE) log("  second L bracket --> bad luck "); // NOI18N
                        return;
                    } else {
                        afterLBracket = true;
                    }
                    break;
                case WS:
                case LINE_COMMENT:
                case COMMENT:
                case DOC_COMMENT:
                    continue;
                case IDENTIFIER:
                    afterIdentifier = true;
                    break;
                default:
                    if (LOGGABLE) log("  default: " + ts.token().id()); // NOI18N
                    if (afterPipe) {
                        break WHILE;
                    } else {
                        if (LOGGABLE) log("   there is too much, return nothing"); // NOI18N
                        return;
                    }
            }
        }
        if (LOGGABLE) log("  afterPipe: " + afterPipe); // NOI18N
        if (afterPipe) {
            if (LOGGABLE) log(NbBundle.getBundle("org/netbeans/modules/visage/editor/completion/environment/Bundle").getString("__NOT_IMPLEMENTED:_boolean_condition_")); // NOI18N
            return;
        }
        if (LOGGABLE) log("  afterIdentifier: " + afterIdentifier); // NOI18N
        if (afterIdentifier) {
            if (LOGGABLE) log(NbBundle.getBundle("org/netbeans/modules/visage/editor/completion/environment/Bundle").getString("__NOT_IMPLEMENTED:_suggest_ending_the_variable_name_and_|_after")); // NOI18N
            return;
        } 
    }

    private static void log(String s) {
        if (LOGGABLE) {
            logger.fine(s);
        }
    }
}
