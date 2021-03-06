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
package org.netbeans.api.visage.source;

import com.sun.javadoc.Doc;
import com.sun.tools.mjavac.code.Flags;
import com.sun.tools.mjavac.code.Source;
import com.sun.tools.mjavac.code.Symbol;
import com.sun.tools.mjavac.code.Symbol.ClassSymbol;
import com.sun.tools.mjavac.code.Symbol.MethodSymbol;
import com.sun.tools.mjavac.code.Symbol.PackageSymbol;
import com.sun.tools.mjavac.code.Symbol.VarSymbol;
import com.sun.tools.mjavac.code.Symtab;
import com.sun.tools.mjavac.code.Type;
import com.sun.tools.mjavac.code.Type.ClassType;
import com.sun.tools.mjavac.code.Types;
import com.sun.tools.mjavac.util.Context;
import com.sun.tools.mjavac.util.Name;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.netbeans.modules.visage.source.VisagedocEnv;
import org.netbeans.modules.visage.source.parsing.VisageParserResultImpl;
import org.openide.util.Exceptions;
import org.visage.api.tree.SourcePositions;
import org.visage.api.tree.Tree;
import org.visage.api.tree.VisageTreePathScanner;
import org.visage.tools.api.VisagecScope;
import org.visage.tools.code.VisageTypes;
import org.visage.tools.tree.VisageFunctionDefinition;
import org.visage.tools.tree.VisageOverrideClassVar;
import org.visage.tools.tree.VisageTree;
import org.visage.tools.tree.VisageTreeInfo;
import org.visage.tools.visagedoc.ClassDocImpl;
import org.visage.tools.visagedoc.DocEnv;

/**
 *
 * @author Jan Lahoda, Dusan Balek, Tomas Zezula
 */
public final class ElementUtilities {
    final private Map<Integer, SoftReference<Element>> elementCache = new HashMap<Integer, SoftReference<Element>>();

    private final Context ctx;
//    private final ElementsService delegate;
    private final VisageParserResultImpl parserResultImpl;

    ElementUtilities(final CompilationInfo info) {
        this(info.impl().parserResultImpl());
    }

    ElementUtilities(VisageParserResultImpl parserResultImpl) {
        this.parserResultImpl = parserResultImpl;
        this.ctx = parserResultImpl.getVisagecTaskImpl().getContext();
//        this.delegate = ElementsService.instance(ctx);
    }

    static public boolean alreadyDefinedIn(CharSequence name, ExecutableElement method, TypeElement enclClass) {
        ElementHandle origHandle = ElementHandle.create(method);
        String sigs[] = origHandle.getSignatures();
        origHandle = new ElementHandle(origHandle.getKind(), new String[]{sigs[0], name.toString(), sigs[2]});
        for(Element e : ElementFilter.methodsIn(enclClass.getEnclosedElements())) {
            if (origHandle.equals(ElementHandle.create(e))) {
                return true;
            }
        }
        return false;
     }

    /**
     * Returns the type element within which this member or constructor
     * is declared. Does not accept pakages
     * If this is the declaration of a top-level type (a non-nested class
     * or interface), returns null.
     *
     * @return the type declaration within which this member or constructor
     * is declared, or null if there is none
     * @throws IllegalArgumentException if the provided element is a package element
     */
    public static TypeElement enclosingTypeElement( Element element ) throws IllegalArgumentException {
	
	if( element.getKind() == ElementKind.PACKAGE ) {
	    throw new IllegalArgumentException();
	}
	
        if (element.getEnclosingElement().getKind() == ElementKind.PACKAGE) {
            //element is a top level class, returning null according to the contract:
            return null;
        }
        
	while( !(element.getEnclosingElement().getKind().isClass() || 
	       element.getEnclosingElement().getKind().isInterface()) ) {
	    element = element.getEnclosingElement();
	}
	
	return (TypeElement)element.getEnclosingElement(); // Wrong
    }

    /**
     * Returns the type element within which this member or constructor
     * is declared. Does not accept pakages
     * If this is the declaration of a top-level type (a non-nested class
     * or interface), returns null.
     *
     * @return the type declaration within which this member or constructor
     * is declared, or null if there is none
     * @throws IllegalArgumentException if the provided element is a package element
     */
    public static PackageElement enclosingPackageElement( Element element ) throws IllegalArgumentException {

	if( element.getKind() == ElementKind.PACKAGE ) {
            return null;
        }

	while( !(element.getEnclosingElement().getKind() == ElementKind.PACKAGE) ) {
	    element = element.getEnclosingElement();
	}

	return (PackageElement)element.getEnclosingElement(); // Wrong
    }

    /**
     * 
     * The outermost TypeElement which indirectly encloses this element.
     */
//    public TypeElement outermostTypeElement(Element element) {
//        return delegate.outermostTypeElement(element);
//    }

    /**
     * Returns the implementation of a method in class origin; null if none exists.
     */
//    public Element getImplementationOf(ExecutableElement method, TypeElement origin) {
//        return delegate.getImplementationOf(method, origin);
//    }

    /**
     * Returns true if the given element is syntetic.
     * 
     *  @param element to check
     *  @return true if and only if the given element is syntetic, false otherwise
     */
    public boolean isSynthetic(Element element) {
        return (((Symbol) element).flags() & Flags.SYNTHETIC) != 0 || (((Symbol) element).flags() & Flags.GENERATEDCONSTR) != 0;
    }

    /**
     * Returns true if the given element is deprecated.
     *
     *  @param element to check
     *  @return true if and only if the given element is deprecated, false otherwise
     */
    public boolean isDeprecated(Element element) {
        return (((Symbol) element).flags() & Flags.DEPRECATED) != 0;
    }

    /**
     * Returns true if this element represents a method which overrides a
     * method in one of its superclasses.
     */
//    public boolean overridesMethod(ExecutableElement element) {
//        return delegate.overridesMethod(element);
//    }

    /**
     * Returns a binary name of a type.
     * @param element for which the binary name should be returned
     * @return the binary name, see Java Language Specification 13.1
     * @throws IllegalArgumentException when the element is not a javac element
     */
//    public static String getBinaryName (TypeElement element) throws IllegalArgumentException {
//        if (element instanceof Symbol.TypeSymbol) {
//            return ((Symbol.TypeSymbol)element).flatName().toString();
//        }
//        else {
//            throw new IllegalArgumentException ();
//        } 
//    }

    /**
     * Get javadoc for given element.
     */
    public Doc javaDocFor(Element element) {
        if (element != null) {
            DocEnv env = DocEnv.instance(ctx);
            switch (element.getKind()) {
                case CLASS:
                case ENUM:
                case INTERFACE:
                    ClassDocImpl classDoc = null;
                    try {
                        classDoc = env.getClassDoc((ClassSymbol) element);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return classDoc;
                case FIELD:
                    return env.getFieldDoc((VarSymbol) element);
                case METHOD:
//                    if (element.getEnclosingElement().getKind() == ElementKind.ANNOTATION_TYPE)
//                        return env.getAnnotationTypeElementDoc((MethodSymbol)element);
                    return env.getFunctionDoc((MethodSymbol) element);
                case CONSTRUCTOR:
                    return env.getConstructorDoc((MethodSymbol) element);
                case PACKAGE:
                    return env.getPackageDoc((PackageSymbol) element);
            }
        }
        return null;
    }

    /**
     * Find a {@link Element} corresponding to a given {@link Doc}.
     */
    public Element elementFor(Doc doc) {
        return (doc instanceof VisagedocEnv.ElementHolder) ? ((VisagedocEnv.ElementHolder) doc).getElement() : null;
    }

    public Element elementFor(final  int pos) {
        synchronized(elementCache) {
            SoftReference<Element> e = elementCache.get(pos);
            if (e != null && e.get() != null) {
                return e.get();
            }
        }
        final SourcePositions positions = parserResultImpl.getTrees().getSourcePositions();
        final Element[] e = new Element[1];
        VisageTreePathScanner<Void, Void> scanner = new VisageTreePathScanner<Void, Void>() {
            private long lastValidSpan = Long.MAX_VALUE;
            @Override
            public Void scan(Tree tree, Void p) {
                super.scan(tree, p);
                if (tree != null) {
                    long start = positions.getStartPosition(parserResultImpl.getCompilationUnit(), tree);
                    long end = positions.getEndPosition(parserResultImpl.getCompilationUnit(), tree);

                    if (tree.getVisageKind() != Tree.VisageKind.STRING_LITERAL || !(tree.toString().equals("\"\"") || tree.toString().equals(""))) {
                        if (tree.getVisageKind() != Tree.VisageKind.MODIFIERS && tree.getVisageKind() != Tree.VisageKind.FUNCTION_VALUE && start != -1 && start != end && start <= pos && end >=pos) {
                            // check for visage$run$ magic
                            if (!(tree.getVisageKind() == Tree.VisageKind.FUNCTION_DEFINITION && ((VisageFunctionDefinition)tree).getName().contentEquals("visage$run$"))) {
                                long span = end - start + 1;
                                if (span < lastValidSpan) {
                                    e[0] = VisageTreeInfo.symbolFor((VisageTree)tree);
                                    if (e[0] != null) {
                                        lastValidSpan = span;
                                    } else {
                                        if (tree.getVisageKind() == Tree.VisageKind.MEMBER_SELECT || tree.getVisageKind() == Tree.VisageKind.IDENTIFIER) {
                                            // a bug in visagec - not resolving package symbols in "package statement"
                                            e[0] = getPackageElement(tree.toString());
                                            lastValidSpan = span;
                                        } else if (tree.getVisageKind() == Tree.VisageKind.VARIABLE) {
                                            if (tree instanceof VisageOverrideClassVar) {
                                                e[0] = ((VisageOverrideClassVar)tree).sym;
                                                lastValidSpan = span;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
        };
        scanner.scan(parserResultImpl.getCompilationUnit(), null);
        if (e[0] != null) {
            synchronized(elementCache) {
                elementCache.put(pos, new SoftReference<Element>(e[0]));
            }
        }
        return e[0];
    }

    private Name nameFor(String aName) {
        return Name.fromString(Name.Table.instance(ctx), aName);
    }

    /**
     * Returns all members of a type, whether inherited or
     * declared directly.  For a class the result also includes its
     * constructors, but not local or anonymous classes.
     * 
     * @param type  the type being examined
     * @param acceptor to filter the members
     * @return all members in the type
     * @see Elements#getAllMembers
     */
    @SuppressWarnings("fallthrough")
    public Iterable<? extends Element> getMembers(TypeMirror type, ElementAcceptor acceptor) {
        ArrayList<Element> members = new ArrayList<Element>();
        if (type != null) {
//            Elements elements = JavacElements.instance(ctx);
            Elements elements = parserResultImpl.getElements();
            switch (type.getKind()) {
                case DECLARED:
                    HashMap<CharSequence, ArrayList<Element>> hiders = new HashMap<CharSequence, ArrayList<Element>>();
                    Types types = VisageTypes.instance(ctx);
                    TypeElement te = (TypeElement) ((DeclaredType) type).asElement();
                    for (Element member : getAllMembers(elements, te)) {
                        if (acceptor == null || acceptor.accept(member, type)) {
                            CharSequence name = member.getSimpleName();
                            ArrayList<Element> h = hiders.get(name);
                            if (!isHidden(member, h, types)) {
                                members.add(member);
                                if (h == null) {
                                    h = new ArrayList<Element>();
                                    hiders.put(name, h);
                                }
                                h.add(member);
                            }
                        }
                    }
                    if (te.getKind().isClass()) {
                        VarSymbol thisPseudoMember = new VarSymbol(Flags.FINAL | Flags.HASINIT, Name.Table.instance(ctx)._this, (ClassType) te.asType(), (ClassSymbol) te);
                        if (acceptor == null || acceptor.accept(thisPseudoMember, type)) {
                            members.add(thisPseudoMember);
                        }
                        if (te.getSuperclass().getKind() == TypeKind.DECLARED) {
                            VarSymbol superPseudoMember = new VarSymbol(Flags.FINAL | Flags.HASINIT, Name.Table.instance(ctx)._super, (ClassType) te.getSuperclass(), (ClassSymbol) te);
                            if (acceptor == null || acceptor.accept(superPseudoMember, type)) {
                                members.add(superPseudoMember);
                            }
                        }
                    }
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case DOUBLE:
                case FLOAT:
                case INT:
                case LONG:
                case SHORT:
                case VOID:
                    Type t = Symtab.instance(ctx).classType;
                    com.sun.tools.mjavac.util.List<Type> typeargs = Source.instance(ctx).allowGenerics() ? com.sun.tools.mjavac.util.List.of((Type) type) : com.sun.tools.mjavac.util.List.<Type>nil();
                    t = new ClassType(t.getEnclosingType(), typeargs, t.tsym);
                    Element classPseudoMember = new VarSymbol(Flags.STATIC | Flags.PUBLIC | Flags.FINAL, Name.Table.instance(ctx)._class, t, ((Type) type).tsym);
                    if (acceptor == null || acceptor.accept(classPseudoMember, type)) {
                        members.add(classPseudoMember);
                    }
                    break;
                case ARRAY:
                    for (Element member : getAllMembers(elements, (TypeElement) ((Type) type).tsym)) {
                        if (acceptor == null || acceptor.accept(member, type)) {
                            members.add(member);
                        }
                    }
                    break;
            }
        }
        return members;
    }

    /**
     * Return members declared in the given scope.
     */
    public Iterable<? extends Element> getLocalMembersAndVars(VisagecScope scope, ElementAcceptor acceptor) {
        ArrayList<Element> members = new ArrayList<Element>();
        HashMap<CharSequence, ArrayList<Element>> hiders = new HashMap<CharSequence, ArrayList<Element>>();
//        Elements elements = JavacElements.instance(ctx);
        Elements elements = parserResultImpl.getElements();
        Types types = VisageTypes.instance(ctx);
        TypeElement cls;
        while (scope != null) {
            final Iterable<? extends Element> localElements = scope.getLocalElements();
            if ((cls = scope.getEnclosingClass()) != null) {
                for (Element local : localElements) {
                    if (acceptor == null || acceptor.accept(local, null)) {
                        CharSequence name = local.getSimpleName();
                        ArrayList<Element> h = hiders.get(name);
                        if (!isHidden(local, h, types)) {
                            members.add(local);
                            if (h == null) {
                                h = new ArrayList<Element>();
                                hiders.put(name, h);
                            }
                            h.add(local);
                        }
                    }
                }
                TypeMirror type = cls.asType();
                for (Element member : getAllMembers(elements, cls)) {
                    if (acceptor == null || acceptor.accept(member, type)) {
                        CharSequence name = member.getSimpleName();
                        ArrayList<Element> h = hiders.get(name);
                        if (!isHidden(member, h, types)) {
                            members.add(member);
                            if (h == null) {
                                h = new ArrayList<Element>();
                                hiders.put(name, h);
                            }
                            h.add(member);
                        }
                    }
                }
            } else {
                for (Element local : localElements) {
                    if (!local.getKind().isClass() && !local.getKind().isInterface() &&
                            (acceptor == null || acceptor.accept(local, local.getEnclosingElement().asType()))) {
                        CharSequence name = local.getSimpleName();
                        ArrayList<Element> h = hiders.get(name);
                        if (!isHidden(local, h, types)) {
                            members.add(local);
                            if (h == null) {
                                h = new ArrayList<Element>();
                                hiders.put(name, h);
                            }
                            h.add(local);
                        }
                    }
                }
            }
            scope = scope.getEnclosingScope();
        }
        return members;
    }

    /**
     * Return variables declared in the given scope.
     */
//    public Iterable<? extends Element> getLocalVars(Scope scope, ElementAcceptor acceptor) {
//        ArrayList<Element> members = new ArrayList<Element>();
//        HashMap<CharSequence, ArrayList<Element>> hiders = new HashMap<CharSequence, ArrayList<Element>>();
//        Types types = JavacTypes.instance(ctx);
//        while(scope != null && scope.getEnclosingClass() != null) {
//            for (Element local : scope.getLocalElements())
//                if (acceptor == null || acceptor.accept(local, null)) {
//                    CharSequence name = local.getSimpleName();
//                    ArrayList<Element> h = hiders.get(name);
//                    if (!isHidden(local, h, types)) {
//                        members.add(local);
//                        if (h == null) {
//                            h = new ArrayList<Element>();
//                            hiders.put(name, h);
//                        }
//                        h.add(local);
//                    }
//                }
//            scope = scope.getEnclosingScope();
//        }
//        return members;
//    }

    /**
     * Return {@link TypeElement}s:
     * <ul>
     *    <li>which are imported</li>
     *    <li>which are in the same package as the current file</li>
     *    <li>which are in the java.lang package</li>
     * </ul>
     */
//    public Iterable<? extends TypeElement> getGlobalTypes(ElementAcceptor acceptor) {
//        HashSet<TypeElement> members = new HashSet<TypeElement>();
//        HashMap<CharSequence, ArrayList<Element>> hiders = new HashMap<CharSequence, ArrayList<Element>>();
//        Trees trees = JavacTrees.instance(ctx);
//        Types types = JavacTypes.instance(ctx);
//        for (CompilationUnitTree unit : Collections.singletonList(info.getCompilationUnit())) {
//            TreePath path = new TreePath(unit);
//            Scope scope = trees.getScope(path);
//            while (scope != null && scope instanceof JavacScope && !((JavacScope)scope).isStarImportScope()) {
//                for (Element local : scope.getLocalElements())
//                    if (local.getKind().isClass() || local.getKind().isInterface()) {
//                        CharSequence name = local.getSimpleName();
//                        ArrayList<Element> h = hiders.get(name);
//                        if (!isHidden(local, h, types)) {
//                            if (acceptor == null || acceptor.accept(local, null))
//                                members.add((TypeElement)local);
//                            if (h == null) {
//                                h = new ArrayList<Element>();
//                                hiders.put(name, h);
//                            }
//                            h.add(local);
//                        }
//                    }
//                scope = scope.getEnclosingScope();
//            }
//            Element element = trees.getElement(path);
//            if (element != null && element.getKind() == ElementKind.PACKAGE) {
//                for (Element member : element.getEnclosedElements()) {
//                    CharSequence name = member.getSimpleName();
//                    ArrayList<Element> h = hiders.get(name);
//                    if (!isHidden(member, h, types)) {
//                        if (acceptor == null || acceptor.accept(member, null))
//                            members.add((TypeElement) member);
//                        if (h == null) {
//                            h = new ArrayList<Element>();
//                            hiders.put(name, h);
//                        }
//                        h.add(member);
//                    }
//                }
//            }
//            while (scope != null) {
//                for (Element local : scope.getLocalElements())
//                    if (local.getKind().isClass() || local.getKind().isInterface()) {
//                        CharSequence name = local.getSimpleName();
//                        ArrayList<Element> h = hiders.get(name);
//                        if (!isHidden(local, h, types)) {
//                            if (acceptor == null || acceptor.accept(local, null))
//                                members.add((TypeElement)local);
//                            if (h == null) {
//                                h = new ArrayList<Element>();
//                                hiders.put(name, h);
//                            }
//                            h.add(local);
//                        }
//                    }
//                scope = scope.getEnclosingScope();
//            }
//        }
//        return members;
//    }

    /**
     * Filter {@link Element}s
     */
    public static interface ElementAcceptor {

        /**Is the given element accepted.
         * 
         * @param e element to test
         * @param type the type for which to check if the member is accepted
         * @return true if and only if given element should be accepted
         */
        boolean accept(Element e, TypeMirror type);
    }

    private boolean isHidden(Element member, Iterable<Element> hiders, Types types) {
        if (hiders != null) {
            for (Element hider : hiders) {
                if (hider == member || (hider.getClass() == member.getClass() && //TODO: getClass() should not be used here
                        hider.getSimpleName() == member.getSimpleName() &&
                        ((hider.getKind() != ElementKind.METHOD && hider.getKind() != ElementKind.CONSTRUCTOR)))) 
                //                if (hider == member || (hider.getClass() == member.getClass() && //TODO: getClass() should not be used here
                //                    hider.getSimpleName() == member.getSimpleName() &&
                //                    ((hider.getKind() != ElementKind.METHOD && hider.getKind() != ElementKind.CONSTRUCTOR)
                //                    || types.isSubsignature((ExecutableType)hider.asType(), (ExecutableType)member.asType()))))
                {
                    return true;
                }
            }
        }
        return false;
    }

    // VSGC-2154
    public static java.util.List<? extends Element> getAllMembers(Elements elements, TypeElement type) {
        java.util.List<? extends Element> allMembers = Collections.<Element>emptyList();
        if (elements == null || type == null) {
            return allMembers;
        }
        try {
            allMembers = elements.getAllMembers(type);
        } catch (Exception e) {
            Exceptions.printStackTrace(e);
        }
        return allMembers;
    }

    /**
     * Returns true if the element is declared (directly or indirectly) local
     * to a method or variable initializer.  Also true for fields of inner 
     * classes which are in turn local to a method or variable initializer.
     */
//    public boolean isLocal(Element element) {
//        return delegate.isLocal(element);
//    }

    /**
     * Returns true if a method specified by name and type is defined in a
     * class type.
     */
//    public boolean alreadyDefinedIn(CharSequence name, ExecutableType method, TypeElement enclClass) {
//        return delegate.alreadyDefinedIn(name, method, enclClass);
//    }

    /**
     * Returns true if a type element has the specified element as a member.
     */
//    public boolean isMemberOf(Element e, TypeElement type) {
//        return delegate.isMemberOf(e, type);
//    }

    /**
     * Returns the parent method which the specified method overrides, or null
     * if the method does not override a parent class method.
     */
//    public ExecutableElement getOverriddenMethod(ExecutableElement method) {
//        return delegate.getOverriddenMethod(method);
//    }

    /**
     * Returns true if this element represents a method which 
     * implements a method in an interface the parent class implements.
     */
//    public boolean implementsMethod(ExecutableElement element) {
//        return delegate.implementsMethod(element);
//    }

    /**
     * Find all methods in given type and its supertypes, which are not implemented.
     * 
     * @param type to inspect
     * @return list of all unimplemented methods
     * 
     * @since 0.20
     */
//    public List<? extends ExecutableElement> findUnimplementedMethods(TypeElement impl) {
//        return findUnimplementedMethods(impl, impl);
//    }

    // private implementation --------------------------------------------------
//    private List<? extends ExecutableElement> findUnimplementedMethods(TypeElement impl, TypeElement element) {
//        List<ExecutableElement> undef = new ArrayList<ExecutableElement>();
//        if (element.getModifiers().contains(Modifier.ABSTRACT)) {
//            for (Element e : element.getEnclosedElements()) {
//                if (e.getKind() == ElementKind.METHOD && e.getModifiers().contains(Modifier.ABSTRACT)) {
//                    ExecutableElement ee = (ExecutableElement)e;
//                    Element eeImpl = getImplementationOf(ee, impl);
//                    if (eeImpl == null || (eeImpl == ee && impl != element))
//                        undef.add(ee);
//                }
//            }
//        }
//        Types types = JavacTypes.instance(ctx);
//        DeclaredType implType = (DeclaredType)impl.asType();
//        for (TypeMirror t : types.directSupertypes(element.asType())) {
//            for (ExecutableElement ee : findUnimplementedMethods(impl, (TypeElement) ((DeclaredType) t).asElement())) {
//                //check if "the same" method has already been added:
//                boolean exists = false;
//                ExecutableType eeType = (ExecutableType)types.asMemberOf(implType, ee);
//                for (ExecutableElement existing : undef) {
//                    if (existing.getSimpleName().contentEquals(ee.getSimpleName())) {
//                        ExecutableType existingType = (ExecutableType)types.asMemberOf(implType, existing);
//                        if (types.isSubsignature(existingType, eeType)) {
//                            TypeMirror existingReturnType = existingType.getReturnType();
//                            TypeMirror eeReturnType = eeType.getReturnType();
//                            if (!types.isSubtype(existingReturnType, eeReturnType)) {
//                                if (types.isSubtype(eeReturnType, existingReturnType)) {
//                                    undef.remove(existing);
//                                    undef.add(ee);
//                                } else if (existingReturnType.getKind() == TypeKind.DECLARED && eeReturnType.getKind() == TypeKind.DECLARED) {
//                                    Env<AttrContext> env = Enter.instance(ctx).getClassEnv((TypeSymbol)impl);
//                                    DeclaredType subType = env != null ? findCommonSubtype((DeclaredType)existingReturnType, (DeclaredType)eeReturnType, env) : null;
//                                    if (subType != null) {
//                                        undef.remove(existing);
//                                        MethodSymbol ms = ((MethodSymbol)existing).clone((Symbol)impl);
//                                        MethodType mt = (MethodType)ms.type.clone();
//                                        mt.restype = (Type)subType;
//                                        ms.type = mt;
//                                        undef.add(ms);
//                                    }
//                                }
//                            }
//                            exists = true;
//                            break;
//                        }
//                    }
//                }
//                if (!exists) {
//                    undef.add(ee);
//                }
//            }
//        }
//        return undef;
//    }
//    private DeclaredType findCommonSubtype(DeclaredType type1, DeclaredType type2, Env<AttrContext> env) {
//        List<DeclaredType> subtypes1 = getSubtypes(type1, env);
//        List<DeclaredType> subtypes2 = getSubtypes(type2, env);
//        Types types = info.getTypes();
//        for (DeclaredType subtype1 : subtypes1) {
//            for (DeclaredType subtype2 : subtypes2) {
//                if (types.isSubtype(subtype1, subtype2))
//                    return subtype1;
//                if (types.isSubtype(subtype2, subtype1))
//                    return subtype2;
//            }
//        }
//        return null;
//    }
    // TODO classindex for FX will be in future
//    private List<DeclaredType> getSubtypes(DeclaredType baseType, Env<AttrContext> env) {
//        LinkedList<DeclaredType> subtypes = new LinkedList<DeclaredType>();
//        HashSet<TypeElement> elems = new HashSet<TypeElement>();
//        LinkedList<DeclaredType> bases = new LinkedList<DeclaredType>();
//        bases.add(baseType);
//        ClassIndex index = info.getClasspathInfo().getClassIndex();
//        Trees trees = info.getTrees();
//        Types types = info.getTypes();
//        Resolve resolve = Resolve.instance(ctx);
//        while(!bases.isEmpty()) {
//            DeclaredType head = bases.remove();
//            TypeElement elem = (TypeElement)head.asElement();
//            if (!elems.add(elem))
//                continue;
//            subtypes.add(head);
//            List<? extends TypeMirror> tas = head.getTypeArguments();
//            boolean isRaw = !tas.iterator().hasNext();
//            subtypes:
//            for (ElementHandle<TypeElement> eh : index.getElements(ElementHandle.create(elem), EnumSet.of(ClassIndex.SearchKind.IMPLEMENTORS), EnumSet.allOf(ClassIndex.SearchScope.class))) {
//                TypeElement e = eh.resolve(info);
//                if (e != null) {
//                    if (resolve.isAccessible(env, (TypeSymbol)e)) {
//                        if (isRaw) {
//                            DeclaredType dt = types.getDeclaredType(e);
//                            bases.add(dt);
//                        } else {
//                            HashMap<Element, TypeMirror> map = new HashMap<Element, TypeMirror>();
//                            TypeMirror sup = e.getSuperclass();
//                            if (sup.getKind() == TypeKind.DECLARED && ((DeclaredType)sup).asElement() == elem) {
//                                DeclaredType dt = (DeclaredType)sup;
//                                Iterator<? extends TypeMirror> ittas = tas.iterator();
//                                Iterator<? extends TypeMirror> it = dt.getTypeArguments().iterator();
//                                while(it.hasNext() && ittas.hasNext()) {
//                                    TypeMirror basetm = ittas.next();
//                                    TypeMirror stm = it.next();
//                                    if (basetm != stm) {
//                                        if (stm.getKind() == TypeKind.TYPEVAR) {
//                                            map.put(((TypeVariable)stm).asElement(), basetm);
//                                        } else {
//                                            continue subtypes;
//                                        }
//                                    }
//                                }
//                                if (it.hasNext() != ittas.hasNext()) {
//                                    continue subtypes;
//                                }
//                            } else {
//                                for (TypeMirror tm : e.getInterfaces()) {
//                                    if (((DeclaredType)tm).asElement() == elem) {
//                                        DeclaredType dt = (DeclaredType)tm;
//                                        Iterator<? extends TypeMirror> ittas = tas.iterator();
//                                        Iterator<? extends TypeMirror> it = dt.getTypeArguments().iterator();
//                                        while(it.hasNext() && ittas.hasNext()) {
//                                            TypeMirror basetm = ittas.next();
//                                            TypeMirror stm = it.next();
//                                            if (basetm != stm) {
//                                                if (stm.getKind() == TypeKind.TYPEVAR) {
//                                                    map.put(((TypeVariable)stm).asElement(), basetm);
//                                                } else {
//                                                    continue subtypes;
//                                                }
//                                            }
//                                        }
//                                        if (it.hasNext() != ittas.hasNext()) {
//                                            continue subtypes;
//                                        }
//                                        break;
//                                    }
//                                }
//                            }
//                            bases.add(getDeclaredType(e, map, types));
//                        }
//                    }
//                }
//            }
//        }
//        return subtypes;
//    }
//
//    private DeclaredType getDeclaredType(TypeElement e, HashMap<? extends Element, ? extends TypeMirror> map, Types types) {
//        List<? extends TypeParameterElement> tpes = e.getTypeParameters();
//        TypeMirror[] targs = new TypeMirror[tpes.size()];
//        int i = 0;
//        for (Iterator<? extends TypeParameterElement> it = tpes.iterator(); it.hasNext();) {
//            TypeParameterElement tpe = it.next();
//            TypeMirror t = map.get(tpe);
//            targs[i++] = t != null ? t : tpe.asType();
//        }
//        Element encl = e.getEnclosingElement();
//        if ((encl.getKind().isClass() || encl.getKind().isInterface()) && !((TypeElement)encl).getTypeParameters().isEmpty())
//                return types.getDeclaredType(getDeclaredType((TypeElement)encl, map, types), e, targs);
//        return types.getDeclaredType(e, targs);
//    }

    public Element getPackageElement(String fqn) {
        Name.Table table = Name.Table.instance(ctx);
        Symbol owner = null;

        StringTokenizer st = new StringTokenizer(fqn, ".");
        while(st.hasMoreTokens()) {
            owner = new PackageSymbol(Name.fromString(table, st.nextToken()), owner);
        }
        return owner;
    }
}
