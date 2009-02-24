/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.debugger.javafx.models;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

import org.netbeans.api.debugger.Watch;
import org.netbeans.modules.debugger.javafx.JavaFXDebuggerImpl;
import org.netbeans.api.debugger.javafx.InvalidExpressionException;
import org.netbeans.api.debugger.javafx.JavaFXWatch;
import org.netbeans.api.debugger.javafx.LocalVariable;
import org.netbeans.api.debugger.javafx.ObjectVariable;


/**
 * Represents watch in JavaFX debugger.
 *
 * @author   Jan Jancura
 */

class JavaFXObjectWatchImpl extends AbstractObjectVariable implements JavaFXWatch,
ObjectVariable {

    private JavaFXDebuggerImpl    debugger;
    private Watch               watch;
    private String              exceptionDescription;
    
    
    JavaFXObjectWatchImpl (JavaFXDebuggerImpl debugger, Watch watch, Value v) {
        super (
            debugger, 
            v, 
            "" + watch +
                (v instanceof ObjectReference ? "^" : "")	//NOI18N
        );
        this.debugger = debugger;
        this.watch = watch;
    }
    
    JavaFXObjectWatchImpl (JavaFXDebuggerImpl debugger, Watch watch, String exceptionDescription) {
        super (
            debugger, 
            null, 
            "" + watch
        );
        this.debugger = debugger;
        this.watch = watch;
        this.exceptionDescription = exceptionDescription;
    }
    
    /**
     * Watched expression.
     *
     * @return watched expression
     */
    public String getExpression () {
        return watch.getExpression ();
    }

    /**
     * Sets watched expression.
     *
     * @param expression a expression to be watched
     */
    public void setExpression (String expression) {
        watch.setExpression (expression);
    }
    
    /**
     * Remove the watch from the list of all watches in the system.
     */
    public void remove () {
        watch.remove ();
    }
    
    /**
     * Returns description of problem is this watch can not be evaluated
     * in current context.
     *
     * @return description of problem
     */
    public String getExceptionDescription () {
        return exceptionDescription;
    }

    /**
    * Sets string representation of value of this variable.
    *
    * @param value string representation of value of this variable.
    *
    public void setValue (String expression) throws InvalidExpressionException {
        // evaluate expression to Value
        Value value = model.getDebugger ().evaluateIn (expression);
        // set new value to remote veriable
        setValue (value);
        // set new value to this model
        setInnerValue (value);
        // refresh tree
        model.fireTableValueChangedChanged (this, null);
    }
     */
    
    protected void setValue (final Value value) 
    throws InvalidExpressionException {
        
        // 1) get frame
        CallStackFrameImpl frame = (CallStackFrameImpl) debugger.
            getCurrentCallStackFrame ();
        if (frame == null)
            throw new InvalidExpressionException ("No curent frame."); // NOI18N
        
        // 2) try to set as a local variable value
        try {
            LocalVariable local = frame.getLocalVariable("$"+getExpression ());	//NOI18N
            if (local != null) {
                if (local instanceof Local) {
                    ((Local) local).setValue(value);
                } else {
                    ((ObjectLocalVariable) local).setValue(value);
                }
                return;
            }
        } catch (AbsentInformationException ex) {
            // no local variable visible in this case
        }
        // 2,5) try to set as static field
        ReferenceType clazz = frame.getStackFrame().location().declaringType();
        Field field1 = clazz.fieldByName("$"+getExpression());	//NOI18N
        if (field1 == null) {
            throw new InvalidExpressionException (
                "Can not set value to expression."); // NOI18N
        }
        if (field1.isStatic()) {
            if (clazz instanceof ClassType) {
                try {
//In JavaFX All globals are static final
                    if (field1.isFinal()) {
                        Value v = clazz.getValue(field1);
                        if (v instanceof ObjectReference) {
                            ObjectReference ref = (ObjectReference)v;
                            ReferenceType rt = ref.referenceType();
                            if (rt!=null){
                                Field valueField = rt.fieldByName("$value");	//NOI18N
                                if (valueField!=null) {
                                    ref.setValue(valueField, value);
                                }
                            }
                        }
                    } else {
                        ((ClassType) clazz).setValue(field1, value);
                    }
                } catch (InvalidTypeException ex) {
                    throw new InvalidExpressionException (ex);
                } catch (ClassNotLoadedException ex) {
                    throw new InvalidExpressionException (ex);
                }
            } else {
                throw new InvalidExpressionException
                 ("Can not set value to expression."); // NOI18N
            }
        } else {
        // 3) try tu set as a field
        ObjectReference thisObject = frame.getStackFrame ().thisObject ();
        if (thisObject == null) {
                    throw new InvalidExpressionException
                     ("Can not set value to expression."); // NOI18N
        }
        Field field = thisObject.referenceType ().fieldByName
            ("$"+getExpression ());	//NOI18N
        if (field == null)
            throw new InvalidExpressionException 
                ("Can not set value to expression."); // NOI18N
        try {
            thisObject.setValue (field, value);
        } catch (InvalidTypeException ex) {
            throw new InvalidExpressionException (ex);
        } catch (ClassNotLoadedException ex) {
            throw new InvalidExpressionException (ex);
        }
        }
    }
    
    protected void setInnerValue (Value v) {
        super.setInnerValue (v);
        exceptionDescription = null;
    }
    
    void setException (String exceptionDescription) {
        super.setInnerValue (null);
        this.exceptionDescription = exceptionDescription;
    }
    
    boolean isPrimitive () {
        return !(getInnerValue () instanceof ObjectReference);
    }
    
    public JavaFXObjectWatchImpl clone() {
        JavaFXObjectWatchImpl clon;
        if (exceptionDescription == null) {
            clon = new JavaFXObjectWatchImpl(getDebugger(), watch, getJDIValue());
        } else {
            clon = new JavaFXObjectWatchImpl(getDebugger(), watch, exceptionDescription);
        }
        return clon;
    }
    
}
