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

package org.netbeans.modules.visage.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.netbeans.modules.java.api.common.SourceRoots;
import org.openide.filesystems.FileObject;
import org.netbeans.api.queries.FileBuiltQuery;
import org.netbeans.spi.queries.FileBuiltQueryImplementation;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.modules.visage.project.ui.customizer.VisageProjectProperties;





public class JavaFxFileBuiltQuery implements FileBuiltQueryImplementation, PropertyChangeListener {

    private FileBuiltQueryImplementation delegate;
    private final AntProjectHelper helper;
    private final PropertyEvaluator evaluator;
    private final SourceRoots sourceRoots;
    private final SourceRoots testRoots;

    JavaFxFileBuiltQuery (AntProjectHelper helper, PropertyEvaluator evaluator,
                        SourceRoots sourceRoots, SourceRoots testRoots) {
        assert helper != null && evaluator != null && sourceRoots != null && testRoots != null;
        this.helper = helper;
        this.evaluator = evaluator;
        this.sourceRoots = sourceRoots;
        this.testRoots = testRoots;
        this.sourceRoots.addPropertyChangeListener (this);
        this.testRoots.addPropertyChangeListener (this);
    }

    public synchronized FileBuiltQuery.Status getStatus(FileObject file) {
        if (this.delegate == null) {
            this.delegate = createDelegate ();
        }
        return this.delegate.getStatus (file);
    }


    private FileBuiltQueryImplementation createDelegate () {
        String[] srcRoots = this.sourceRoots.getRootProperties();
        String[] tstRoots = this.testRoots.getRootProperties();
        String[] from = new String [srcRoots.length + tstRoots.length];
        String[] to = new String [srcRoots.length + tstRoots.length];
        for (int i=0; i< srcRoots.length; i++) {
            from[i] = "${" + srcRoots[i] + "}/*.java"; // NOI18N
            to[i] = "${" + VisageProjectProperties.BUILD_CLASSES_DIR + "}/*.class"; // NOI18N
        }
        return helper.createGlobFileBuiltQuery(evaluator, from, to);    //Safe to pass APH
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (SourceRoots.PROP_ROOT_PROPERTIES.equals (evt.getPropertyName())) {
            synchronized(this) {
                this.delegate = null;
                ///XXX: What to do with already returned Statuses
            }
        }
    }
}