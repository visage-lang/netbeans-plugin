/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.javafx.editor.completion.environment;

import com.sun.javafx.api.tree.JavaFXTreePath;
import com.sun.tools.javafx.tree.JFXObjectLiteralPart;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.javafx.editor.completion.JavaFXCompletionEnvironment;

/**
 *
 * @author David Strupl
 */
public class ObjectLiteralPartEnvironment extends JavaFXCompletionEnvironment<JFXObjectLiteralPart> {
    
    private static final Logger logger = Logger.getLogger(ObjectLiteralPartEnvironment.class.getName());
    private static final boolean LOGGABLE = logger.isLoggable(Level.FINE);

    @Override
    protected void inside(JFXObjectLiteralPart t) throws IOException {
        if (LOGGABLE) log("inside JFXObjectLiteralPart " + t + "  offset == " + offset);
        addLocalAndImportedTypes(null, null, null, false, getSmartType(t));
        addLocalMembersAndVars(getSmartType(t));
        addValueKeywords();

    }
    
    private TypeMirror getSmartType(JFXObjectLiteralPart t) throws IOException {
        if (t.getExpression() == null) {
            return null;
        }
        // note: this is probably wrong
        TypeMirror type = controller.getTrees().getTypeMirror(new JavaFXTreePath(path, t.getExpression()));
        // it should instead extract the name of the attribute and extract
        //   the type from the named attribute
        if (LOGGABLE) log("  smart == " + type);
        return type;
    }
    
    private static void log(String s) {
        if (LOGGABLE) {
            logger.fine(s);
        }
    }
}
