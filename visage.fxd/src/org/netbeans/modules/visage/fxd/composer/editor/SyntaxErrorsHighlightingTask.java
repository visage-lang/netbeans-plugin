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
package org.netbeans.modules.visage.fxd.composer.editor;

import com.sun.visage.tools.fxd.container.scene.fxd.FXDSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.visage.fxd.composer.source.FXDDocumentModelProvider;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;


/**
 *
 * @author Andrey Korostelev
 */
public class SyntaxErrorsHighlightingTask extends ParserResultTask {

    private static final String MSG_ERROR = "MSG_ERROR"; // NOI18N

    public SyntaxErrorsHighlightingTask() {
    }

    @Override
    public void run(Result result, SchedulerEvent event) {
        try {
            Document document = result.getSnapshot().getSource().getDocument(false);
            if(document == null){
                return;
            }
            Object prop = document.getProperty(FXDDocumentModelProvider.PROP_PARSE_ERROR_LIST);
            List<FXDSyntaxErrorException> syntaxErrors = (List<FXDSyntaxErrorException>)prop;
            if (syntaxErrors == null){
                return;
            }

            List<ErrorDescription> errors = new ArrayList<ErrorDescription>();
            for (FXDSyntaxErrorException syntaxError : syntaxErrors) {
                int ErrRow = getRow(syntaxError, (BaseDocument) document);
                int ErrPosition = getPosition(syntaxError, (BaseDocument) document);

                // 174091
                ErrorDescription errorDescription = ErrorDescriptionFactory.createErrorDescription(
                        Severity.ERROR,
                        NbBundle.getMessage(SyntaxErrorsHighlightingTask.class,
                                MSG_ERROR, syntaxError.getLocalizedMessage(), ErrRow, ErrPosition),
                        document,
                        ErrRow);
                errors.add(errorDescription);
            }
            if (document != null) {
                HintsController.setErrors(document, "simple-java", errors); //NOI18N
            }
        } catch (BadLocationException ex1) {
            Exceptions.printStackTrace(ex1);
        }
    }

    public static int getRow(FXDSyntaxErrorException syntaxError, BaseDocument document)
            throws BadLocationException {
        return Utilities.getRowCount(document, 0, syntaxError.getOffset());
    }

    public static int getPosition(FXDSyntaxErrorException syntaxError, BaseDocument document)
            throws BadLocationException {
        int offset = syntaxError.getOffset();
        if (offset > -1 && offset < document.getLength()) {
            int rowStart = Utilities.getRowStart((BaseDocument) document, offset);
            int position = offset - rowStart;
            int tabs = syntaxError.getTabsInLastRow();
            if (tabs > 0) {
                // replace 1 tab char by number of spaces in tab
                position = position - tabs + (tabs * IndentUtils.tabSize(document));
            }
            return position;
        }
        return 0;
    }


    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }
}
