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

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import org.netbeans.modules.debugger.javafx.JavaFXDebuggerImpl;
import org.netbeans.api.debugger.javafx.InvalidExpressionException;
import org.netbeans.api.debugger.javafx.JavaFXClassType;



/**
 * @author   Jan Jancura
 */
class ObjectFieldVariable extends AbstractObjectVariable
implements org.netbeans.api.debugger.javafx.Field {

    protected Field field;
    private ObjectReference objectReference;
    private String genericSignature;
    
    ObjectFieldVariable (
        JavaFXDebuggerImpl debugger, 
        ObjectReference value, 
        //String className,
        Field field,
        String parentID,
        ObjectReference objectReference
    ) {
        super (
            debugger, 
            value, 
            parentID + '.' + field.name () + "^"
        );
        this.field = field;
        //this.className = className;
        this.objectReference = objectReference;
    }

    ObjectFieldVariable (
        JavaFXDebuggerImpl debugger, 
        ObjectReference value, 
        //String className,
        Field field,
        String parentID,
        String genericSignature,
        ObjectReference objectReference
    ) {
        this (
            debugger,
            value,
            field,
            parentID,
            objectReference
        );
        this.genericSignature = genericSignature;
    }

    /**
    * Returns string representation of type of this variable.
    *
    * @return string representation of type of this variable.
    */
    public String getName () {
        String name = field.name();
//If variable started from $ need to remove $
        if (name.charAt(0)=='$' && name.length() > 1) {
            name = name.substring(1);
        }
        return name;
//        return field.name ();
    }

    /**
     * Returns name of enclosing class.
     *
     * @return name of enclosing class
     */
    public String getClassName () {
        return field.declaringType ().name (); //className;
    }

    public JavaFXClassType getDeclaringClass() {
        return new JavaFXClassTypeImpl(getDebugger(), (ReferenceType) objectReference.type());
    }

    /**
    * Returns string representation of type of this variable.
    *
    * @return string representation of type of this variable.
    */
    public String getDeclaredType () {
        return field.typeName ();
    }

    public JavaFXClassType getClassType() {
        Value value = getInnerValue();
        if (value != null) {
            return super.getClassType();
        }
        try {
            com.sun.jdi.Type type = field.type();
            if (type instanceof ReferenceType) {
                return new JavaFXClassTypeImpl(getDebugger(), (ReferenceType) type);
            } else {
                return null;
            }
        } catch (ClassNotLoadedException cnlex) {
            return null;
        }
    }
    
    /**
     * Returns <code>true</code> for static fields.
     *
     * @return <code>true</code> for static fields
     */
    public boolean isStatic () {
        return field.isStatic ();
    }
    
    protected void setValue (Value value) throws InvalidExpressionException {
        try {
            boolean set = false;
            if (objectReference != null) {
                if (field.isFinal()) {
                    Value v = objectReference.getValue(field);
                    if (v instanceof ObjectReference){
                        ObjectReference ref = (ObjectReference)v;
                        ReferenceType rt = ref.referenceType();
                        if (rt!=null) {
                            Field valueField = rt.fieldByName("$value");
                            if (valueField!=null) {
                                ref.setValue(valueField, value);
                                set=true;
                            }
                        }
                    }
                } else {
                    objectReference.setValue (field, value);
                    set = true;
                }
            } else {
                ReferenceType rt = field.declaringType();
                if (rt instanceof ClassType) {
//In JavaFX all globals are static final
                    if (field.isFinal()) {
                        Value v = rt.getValue(field);
                        if (v instanceof ObjectReference) {
                            ObjectReference ref = (ObjectReference)v;
                            ReferenceType lrt = ref.referenceType();
                            if (lrt!=null){
                                Field valueField = lrt.fieldByName("$value");
                                if (valueField!=null) {
                                    ref.setValue(valueField, value);
                                    set=true;
                                }
                            }
                        }
                    } else {
                        ClassType ct = (ClassType) rt;
                        ct.setValue(field, value);
                        set = true;
                    }
                }
            }
            if (!set) {
                throw new InvalidExpressionException(field.toString());
            }
        } catch (InvalidTypeException ex) {
            throw new InvalidExpressionException (ex);
        } catch (ClassNotLoadedException ex) {
            throw new InvalidExpressionException (ex);
        }
    }

    public ObjectFieldVariable clone() {
        return new ObjectFieldVariable(getDebugger(), (ObjectReference) getJDIValue(), field,
                getID().substring(0, getID().length() - ("." + field.name() + (getJDIValue() instanceof ObjectReference ? "^" : "")).length()),
                genericSignature, objectReference);
    }

    
    // other methods ...........................................................

    public String toString () {
        return "ObjectFieldVariable " + field.name ();
    }
}
