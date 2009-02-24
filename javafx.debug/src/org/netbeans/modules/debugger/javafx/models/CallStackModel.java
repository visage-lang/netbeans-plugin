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

package org.netbeans.modules.debugger.javafx.models;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Action;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.NodeActionsProvider;
import org.netbeans.spi.viewmodel.UnknownTypeException;


public class CallStackModel implements TreeModel, NodeModel,
        NodeActionsProvider, TableModel {
    
    public static final String CALL_STACK =
            "org/netbeans/modules/debugger/resources/callStackView/NonCurrentFrame"; // NOI18N
    public static final String CURRENT_CALL_STACK =
            "org/netbeans/modules/debugger/resources/callStackView/CurrentFrame"; // NOI18N

  private final List<ModelListener> listeners;
    public CallStackModel() {
        listeners = new CopyOnWriteArrayList<ModelListener>();
    }
  
  public Object getRoot() {
        return ROOT;
    }

    public Object[] getChildren(Object parent, int from, int to) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public boolean isLeaf(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public int getChildrenCount(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public void addModelListener(ModelListener l) {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public void removeModelListener(ModelListener l) {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public String getDisplayName(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public String getIconBase(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public String getShortDescription(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public void performDefaultAction(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public Action[] getActions(Object node) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public Object getValueAt(Object node, String columnID) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public boolean isReadOnly(Object node, String columnID) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    public void setValueAt(Object node, String columnID, Object value) throws UnknownTypeException {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }
     public void fireChanges() {
        for (ModelListener listener : listeners) {
            listener.modelChanged(new ModelEvent.TreeChanged(this));
        }
    }
   
    
}