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
package org.netbeans.modules.visage.editor.hints;

import org.netbeans.api.visage.source.CompilationInfo;
import org.netbeans.api.visage.source.VisageSource;
import com.sun.tools.mjavac.code.Symbol.ClassSymbol;
import com.sun.tools.mjavac.code.Symbol.MethodSymbol;
import com.sun.tools.mjavac.code.Symbol.VarSymbol;
import com.sun.tools.mjavac.code.Type;
import com.sun.tools.mjavac.util.JCDiagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import org.netbeans.api.visage.editor.VisageSourceUtils;
import org.netbeans.api.visage.source.CancellableTask;
import org.netbeans.spi.editor.hints.*;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.visage.api.tree.ClassDeclarationTree;
import org.visage.api.tree.VisageTreePath;
import org.visage.api.tree.VisageTreePathScanner;
import org.visage.tools.code.VisageClassSymbol;
import org.visage.tools.code.VisageTypes;

/**
 *
 * @author karol harezlak
 */
public final class OverrideAllTaskFactory extends VisageAbstractEditorHint {

    private static final String ERROR_CODE1 = "compiler.err.does.not.override.abstract"; //NOI18N
    private static final String ERROR_CODE2 = "compiler.err.abstract.cant.be.instantiated"; //NOI18N
    private static final String HINT_IDENT = "overridevisage"; //NOI18N
    private static final String NATIVE_STRING = "nativearray of "; //NOI18N
    private final AtomicBoolean cancel = new AtomicBoolean();
    private final Collection<ErrorDescription> errorDescriptions = new HashSet<ErrorDescription>();
    //private final static Logger LOG = Logger.getAnonymousLogger();

    public OverrideAllTaskFactory() {
        super(VisageSource.Phase.ANALYZED, VisageSource.Priority.NORMAL);
    }

    @Override
    public CancellableTask<CompilationInfo> createTask(final FileObject file) {
        return new CancellableTask<CompilationInfo>() {

            public void cancel() {
                cancel.set(true);
            }

            public void run(final CompilationInfo compilationInfo) throws Exception {
                errorDescriptions.clear();
                cancel.set(false);
                for (final Diagnostic diagnostic : compilationInfo.getDiagnostics()) {
                    if (HintsUtils.isInGuardedBlock(compilationInfo.getDocument(), (int) diagnostic.getPosition())
                            || !isValidError(diagnostic, compilationInfo)
                            || cancel.get()) {
                        continue;
                    }
                    int position = findPosition(compilationInfo, (int) diagnostic.getStartPosition(), (int) (diagnostic.getEndPosition() - diagnostic.getStartPosition()));
                    if (!(diagnostic instanceof JCDiagnostic) || position < 0) {
                        continue;
                    }
                    if (!isMixin(diagnostic, compilationInfo)) {
                        errorDescriptions.add(getErrorDescription(compilationInfo.getDocument(), file, compilationInfo, diagnostic));
                    }
                }
                HintsController.setErrors(compilationInfo.getDocument(), HINT_IDENT, errorDescriptions);
            }
        };
    }

    @Override
    Collection<ErrorDescription> getErrorDescriptions() {
        return Collections.unmodifiableCollection(errorDescriptions);
    }

    private static boolean isMixin(Diagnostic diagnostic, final CompilationInfo compilationInfo) {
        final ClassSymbol classSymbol = getClassSymbol(diagnostic);

        final boolean[] active = new boolean[1];
        new VisageTreePathScanner<Void, Void>() {

            @Override
            public Void visitClassDeclaration(ClassDeclarationTree node, Void v) {
                VisageTreePath path = getCurrentPath();
                Element element = compilationInfo.getTrees().getElement(path);
                if (element == classSymbol && !node.getMixins().isEmpty()) {
                    active[0] = true;
                }

                return super.visitClassDeclaration(node, v);
            }
        }.scan(compilationInfo.getCompilationUnit(), null);

        return active[0];
    }

    private static boolean isValidError(Diagnostic diagnostic, CompilationInfo compilationInfo) {
        if (diagnostic.getCode().equals(ERROR_CODE1) || diagnostic.getCode().equals(ERROR_CODE2)) {
            for (Diagnostic d : compilationInfo.getDiagnostics()) {
                if (d != diagnostic && d.getLineNumber() == diagnostic.getLineNumber()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static int findPosition(CompilationInfo compilationInfo, int start, int lenght) {
        Document document = compilationInfo.getDocument();
        try {
            String text = document.getText(start, lenght);
            StringTokenizer st = new StringTokenizer(text);
            boolean isExtends = false;
            boolean isClass = false;

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equals("extends")) { //NOI18N
                    isExtends = true;
                }
                if (token.equals("class")) { //NOI18N
                    isClass = true;
                }
            }
            if (isClass && !isExtends) {
                return -1;
            }
            int index = text.indexOf("{"); //NOI18N
            if (index > 0) {
                return start + index + 1;
            } else if (text.contains("(") || text.contains(")")) { //NOI18N
                return -1;
            } else {
                text = document.getText(start, document.getLength() - start);
                index = text.indexOf("{"); //NOI18N
                if (index < 0) {
                    return -1;
                }
                return start + index + 1;
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        return -1;
    }

    private static ClassSymbol getClassSymbol(Diagnostic diagnostic) {
        JCDiagnostic jcDiagnostic = (JCDiagnostic) diagnostic;
        ClassSymbol classSymbol = null;
        for (Object arg : jcDiagnostic.getArgs()) {
            if (arg instanceof VisageClassSymbol) {
                classSymbol = (VisageClassSymbol) arg;
                return classSymbol;
            }
        }

        return null;
    }

    private ErrorDescription getErrorDescription(final Document document,
            final FileObject file,
            final CompilationInfo compilationInfo,
            final Diagnostic diagnostic) {

        Fix fix = new Fix() {

            public String getText() {
                return NbBundle.getMessage(OverrideAllTaskFactory.class, "TITLE_IMPLEMENT_ABSTRACT"); //NOI18N
            }

            public ChangeInfo implement() throws Exception {
                ClassSymbol classSymbol = getClassSymbol(diagnostic);
                final Collection<MethodSymbol> abstractMethods = new HashSet<MethodSymbol>();
                if (classSymbol != null) {
                    Collection<? extends Element> elements = getAllMembers(compilationInfo, classSymbol);
                    for (Element e : elements) {
                        if (e instanceof MethodSymbol) {
                            MethodSymbol method = (MethodSymbol) e;
                            //LOG.info(method.name.toString());
                            for (Modifier modifier : method.getModifiers()) {
                                if (modifier == Modifier.ABSTRACT) {
                                    boolean methodExists = false;
                                    for (Element exisitngMethod : classSymbol.getEnclosedElements()) {
                                        if (!(exisitngMethod instanceof ExecutableElement) && !method.getSimpleName().toString().equals(exisitngMethod.getSimpleName().toString())) {
                                            continue;
                                        }
                                        if (compilationInfo.getElements().overrides((ExecutableElement) exisitngMethod, (ExecutableElement) method, classSymbol)) {
                                            methodExists = true;
                                        }
                                    }
                                    if (!methodExists) {
                                        abstractMethods.add(method);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (abstractMethods.isEmpty()) {
                        return null;
                    }
                }
                final StringBuilder methods = new StringBuilder();
                final String space = HintsUtils.calculateSpace((int) diagnostic.getStartPosition(), document);
                for (MethodSymbol methodSymbol : abstractMethods) {
                    methods.append(createMethod(methodSymbol, space));
                }
                if (methods.toString().length() > 0) {
                    methods.append(space);
                }
                int length = (int) (diagnostic.getEndPosition() - diagnostic.getStartPosition());
                final int positon = findPosition(compilationInfo, (int) diagnostic.getStartPosition(), length);
                if (positon < 0) {
                    return null;
                }
                Runnable runnable = new Runnable() {

                    public void run() {
                        try {
                            document.insertString(positon, methods.toString(), null);
                            HintsUtils.addImport(document, HintsUtils.EXCEPTION_UOE);
                            for (MethodSymbol method : abstractMethods) {
                                addImport(method.asType());
                                for (VarSymbol var : method.getParameters()) {
                                    scanImport(var.asType());
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                HintsUtils.runInAWT(runnable);

                return null;
            }

            private void scanImport(Type type) {
                if (type.getParameterTypes() != null && !type.getParameterTypes().isEmpty()) {
                    for (Type t : type.getParameterTypes()) {
                        scanImport(t);
                    }
                }
                addImport(type);
            }

            private void addImport(Type type) {
                if (type == null) {
                    return;
                }
                type = erasureType(type, compilationInfo.getVisageTypes());
                String importName = type.toString();
                if (!type.isPrimitive() && !importName.equalsIgnoreCase("void") && importName.contains(".")) { //NOI18N
                    importName = removeBetween("()", importName); //NOI18N
                    importName = removeBetween("<>", importName); //NOI18N
                    Matcher symbolMatcher = Pattern.compile("[\\[\\]!@#$%^&*(){}|:'/<>~`,]").matcher(importName); //NOI18N
                    if (symbolMatcher.find()) {
                        importName = symbolMatcher.replaceAll("").trim(); //NOI18N
                    }
                    final String processedImportName = importName;
                    if (importName.contains(".") && !importName.equals("java.lang.Object")) { //NOI18N
                        Runnable runnable = new Runnable() {

                            public void run() {
                                HintsUtils.addImport(document, processedImportName);
                            }
                        };
                        HintsUtils.runInAWT(runnable);
                    }
                }
            }

            private String createMethod(MethodSymbol methodSymbol, String space) {
                StringBuilder method = new StringBuilder();
                method.append("\n").append(space).append(HintsUtils.TAB).append("override "); //NOI18N
                for (Modifier modifier : methodSymbol.getModifiers()) {
                    switch (modifier) {
                        case PUBLIC:
                            method.append("public "); //NOI18N
                            break;
                        case PROTECTED:
                            method.append("protected "); //NOI18N
                            break;
                    }
                }
                method.append("function ").append(methodSymbol.getQualifiedName() + " ("); //NOI18N
                //TODO Work around for NPE which is thrown by methodSymbol.getParameters();
                try {
                    if (methodSymbol.getParameters() != null) {
                        Iterator<VarSymbol> iterator = methodSymbol.getParameters().iterator();
                        while (iterator.hasNext()) {
                            VarSymbol var = iterator.next();
                            String varType = getTypeString(var.asType(), compilationInfo.getVisageTypes());
                            method.append(var.getSimpleName()).append(" : ").append(varType); //NOI18N
                            if (iterator.hasNext()) {
                                method.append(", "); //NOI18N
                            }
                        }
                    }
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
                //TODO Work around for methodSymbol.getReturnType() which throws NPE!
                String returnType = null;
                try {
                    returnType = getTypeString(methodSymbol.getReturnType(), compilationInfo.getVisageTypes());
                } catch (NullPointerException npe) {
                    npe.printStackTrace();
                }
                if (returnType == null) {
                    returnType = "Void"; //NOI18N
                }
                method.append(")").append(" : ").append(returnType).append(" { \n"); //NOI18N
                method.append(space).append(HintsUtils.TAB).append(HintsUtils.TAB).append("throw new UnsupportedOperationException('Not implemented yet');\n"); //NOI18N
                method.append(space).append(HintsUtils.TAB).append("}\n"); //NOI18N

                return method.toString();
            }
        };
        if (diagnostic.getStartPosition() < 0) {
            return null;
        }
        
        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(Severity.HINT, NbBundle.getMessage(OverrideAllTaskFactory.class, "TITLE_IMPLEMENT_ABSTRACT"), Collections.singletonList(fix), file, (int) diagnostic.getStartPosition(), (int) diagnostic.getStartPosition());

        return ed;
    }

    private static String removeBetween(String symbols, String name) {
        int firstIndex = name.indexOf(symbols.substring(0, 1));
        int lastIndex = name.indexOf(symbols.substring(1, 2));
        if (firstIndex < 0 || lastIndex < 0) {
            return name;
        }
        name = name.replace(name.substring(firstIndex, lastIndex + 1), "");

        return name;
    }

    private static Type erasureType(Type type, VisageTypes types) {
        if (types.isSequence(type)) {
            return type;
        }

        return types.erasure(type);
    }

    private static String getTypeString(Type type, VisageTypes types) {
        type = erasureType(type, types);
        String typeString = types.toVisageString(type);
        if (types.isArray(type)) {
            typeString = getNativeArrayClassSimpleName(typeString);
        } else {
            typeString = HintsUtils.getClassSimpleName(typeString);
        }
        if (typeString == null) {
            typeString = ""; //NOI18N
        } else if (typeString != null && typeString.equals("void")) { //NOI18N
            typeString = "Void"; //NOI18N
        }

        return typeString;
    }

    private static Collection<? extends Element> getAllMembers(CompilationInfo compilationInfo, Element element) {
//        VisageTreePath path = compilationInfo.getTreeUtilities().pathFor(position);
//        Element element = compilationInfo.getTrees().getElement(path);
//        ClassSymbol classSymbol = null;
//        if (element instanceof ClassSymbol) {
//            classSymbol = (ClassSymbol) element;
//        }
//        if (classSymbol == null) {
//            return Collections.EMPTY_SET;
//        }
//        ClassIndex classIndex = compilationInfo.getClasspathInfo().getClassIndex();
//        Collection<ElementHandle<TypeElement>> options = classIndex.getDeclaredTypes(classSymbol.getSimpleName().toString(), ClassIndex.NameKind.SIMPLE_NAME, SCOPE);
//        Collection<? extends Element> elements = Collections.EMPTY_SET;
//        for (ElementHandle<TypeElement> eh : options) {
//            if (!eh.getQualifiedName().toString().equals(classSymbol.getQualifiedName().toString())) {
//                continue;
//            }
//            TypeElement el = eh.resolve(compilationInfo);
//            elements = compilationInfo.getElements().getAllMembers(el);
//        }

        return VisageSourceUtils.getAllMembers(compilationInfo.getElements(), (TypeElement) element);
    }

    private static String getNativeArrayClassSimpleName(String fqName) {
        int start = fqName.lastIndexOf(".") + 1; //NOI18N
        if (start > 0) {
            fqName = fqName.substring(start);
        }
        if (!fqName.contains(NATIVE_STRING)) {
            fqName = NATIVE_STRING + fqName;
        }

        return fqName.trim();
    }
}




