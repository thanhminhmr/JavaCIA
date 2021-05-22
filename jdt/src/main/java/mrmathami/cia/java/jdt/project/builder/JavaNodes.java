/*
 * Copyright (C) 2020-2021 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package mrmathami.cia.java.jdt.project.builder;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.node.AnnotationNode;
import mrmathami.cia.java.jdt.tree.node.ClassNode;
import mrmathami.cia.java.jdt.tree.node.EnumNode;
import mrmathami.cia.java.jdt.tree.node.FieldNode;
import mrmathami.cia.java.jdt.tree.node.InitializerNode;
import mrmathami.cia.java.jdt.tree.node.InterfaceNode;
import mrmathami.cia.java.jdt.tree.node.MethodNode;
import mrmathami.cia.java.jdt.tree.node.PackageNode;
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.tree.JavaModifier;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.utils.Pair;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class JavaNodes {

	@Nonnull private final CodeFormatter codeFormatter;
	private final boolean enableRecovery;

	@Nonnull private final JavaDependencies dependencies = new JavaDependencies();
	@Nonnull private final JavaAnnotates annotates = new JavaAnnotates(dependencies);
	@Nonnull private final JavaTypes types = new JavaTypes(dependencies, annotates);


	@Nonnull private final RootNode rootNode = new RootNode();
	@Nonnull private final Map<String, Pair<PackageNode, IPackageBinding>> packageNodeMap = new HashMap<>();

	@Nonnull private final Map<IBinding, AbstractNode> bindingNodeMap = new HashMap<>();

	@Nonnull private final Map<AbstractNode, Pair<
			Pair<InitializerNode, List<InitializerNode.InitializerImpl>>,
			Pair<InitializerNode, List<InitializerNode.InitializerImpl>>>>
			classInitializerMap = new IdentityHashMap<>();

	@Nonnull private final List<Pair<ASTNode, AbstractNode>> delayedDependencyWalkers = new LinkedList<>();

	@Nullable private SourceFile sourceFile;


	JavaNodes(@Nonnull CodeFormatter formatter, boolean enableRecovery) {
		this.codeFormatter = formatter;
		this.enableRecovery = enableRecovery;
	}

	void build(@Nonnull SourceFile sourceFile, @Nonnull CompilationUnit compilationUnit)
			throws JavaCiaException {
		this.sourceFile = sourceFile;

		final PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		if (packageDeclaration != null) {
			final PackageNode packageNode = createPackageNodeFromPackageDeclaration(packageDeclaration);
			for (final Object type : compilationUnit.types()) {
				if (type instanceof AbstractTypeDeclaration) {
					parseAbstractTypeDeclaration(packageNode, (AbstractTypeDeclaration) type);
				}
			}
		} else {
			// the parent is root
			for (final Object type : compilationUnit.types()) {
				if (type instanceof AbstractTypeDeclaration) {
					parseAbstractTypeDeclaration(rootNode, (AbstractTypeDeclaration) type);
				}
			}
		}

		this.sourceFile = null;
	}

	@Nonnull
	RootNode postprocessing() throws JavaCiaException {
		// delay dependency walkers
		for (final Pair<ASTNode, AbstractNode> pair : delayedDependencyWalkers) {
			walkDependency(pair.getA(), pair.getB());
		}
		delayedDependencyWalkers.clear();

		// delay reference type node
		types.postprocessing(bindingNodeMap);

		// delay annotations
		annotates.postprocessing(bindingNodeMap);

		// delay dependencies
		dependencies.postProcessing(bindingNodeMap);

		// set class initializers
		classInitializerMap.clear();

		// final cleanup
		packageNodeMap.clear();
		bindingNodeMap.clear();

		// freeze root node
		rootNode.freeze();
		return rootNode;
	}


	@Nonnull
	private String format(@Nonnull String code, int type) throws JavaCiaException {
		final TextEdit textEdit = codeFormatter.format(type, code, 0, code.length(), 0, "\n");
		final IDocument doc = new Document(code);
		try {
			textEdit.apply(doc, TextEdit.NONE);
			return doc.get();
		} catch (MalformedTreeException | BadLocationException e) {
			throw new JavaCiaException("Cannot format source code!", e);
		}
	}


	//region Modifier

	private static int getModifierMask(@Nonnull JavaModifier modifier) {
		return 1 << modifier.ordinal();
	}

	private int processModifiersFromBindingModifiers(int bindingModifiers) {
		int modifiers = 0;
		if ((bindingModifiers & Modifier.PUBLIC) != 0) modifiers |= getModifierMask(JavaModifier.PUBLIC);
		if ((bindingModifiers & Modifier.PRIVATE) != 0) modifiers |= getModifierMask(JavaModifier.PRIVATE);
		if ((bindingModifiers & Modifier.PROTECTED) != 0) modifiers |= getModifierMask(JavaModifier.PROTECTED);
		if ((bindingModifiers & Modifier.STATIC) != 0) modifiers |= getModifierMask(JavaModifier.STATIC);
		if ((bindingModifiers & Modifier.SYNCHRONIZED) != 0) modifiers |= getModifierMask(JavaModifier.SYNCHRONIZED);
		if ((bindingModifiers & Modifier.VOLATILE) != 0) modifiers |= getModifierMask(JavaModifier.VOLATILE);
		if ((bindingModifiers & Modifier.TRANSIENT) != 0) modifiers |= getModifierMask(JavaModifier.TRANSIENT);
		if ((bindingModifiers & Modifier.NATIVE) != 0) modifiers |= getModifierMask(JavaModifier.NATIVE);
		if ((bindingModifiers & Modifier.STRICTFP) != 0) modifiers |= getModifierMask(JavaModifier.STRICTFP);
		return modifiers;
	}

	//endregion Modifier

	//region Package

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreateFirstLevelPackagePairFromName(
			@Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(simpleName, qualifiedName -> {
			final PackageNode packageNode = rootNode.addChild(new PackageNode(rootNode, simpleName));
			dependencies.createDependencyToNode(rootNode, packageNode, JavaDependency.MEMBER);
			return Pair.mutableOf(packageNode, null);
		});
	}

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreatePackagePairFromParentAndName(
			@Nonnull PackageNode parentNode, @Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(parentNode.getQualifiedName() + '.' + simpleName,
				qualifiedName -> {
					final PackageNode packageNode = parentNode.addChild(new PackageNode(parentNode, simpleName));
					dependencies.createDependencyToNode(parentNode, packageNode, JavaDependency.MEMBER);
					return Pair.mutableOf(packageNode, null);
				});
	}

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreatePackagePairFromNameComponents(
			@Nonnull String[] nameComponents) {
		assert nameComponents.length > 0 : "nameComponents length should not be 0.";
		Pair<PackageNode, IPackageBinding> pair = internalCreateFirstLevelPackagePairFromName(nameComponents[0]);
		for (int i = 1; i < nameComponents.length; i++) {
			pair = internalCreatePackagePairFromParentAndName(pair.getA(), nameComponents[i]);
		}
		return pair;
	}

	@Nonnull
	private PackageNode createFirstLevelPackageFromName(@Nonnull String simpleName) {
		return internalCreateFirstLevelPackagePairFromName(simpleName).getA();
	}

	@Nonnull
	private PackageNode createPackageFromParentAndName(@Nonnull PackageNode parentNode, @Nonnull String simpleName) {
		return internalCreatePackagePairFromParentAndName(parentNode, simpleName).getA();
	}

	@Nonnull
	private PackageNode createPackageFromNameComponents(@Nonnull String[] nameComponents) {
		return internalCreatePackagePairFromNameComponents(nameComponents).getA();
	}

	@Nonnull
	private PackageNode createPackageNodeFromPackageDeclaration(@Nonnull PackageDeclaration packageDeclaration)
			throws JavaCiaException {
		final IPackageBinding packageBinding = packageDeclaration.resolveBinding();
		if (packageBinding == null) throw new JavaCiaException("Cannot resolve binding on package declaration!");

		final Pair<PackageNode, IPackageBinding> oldPair = packageNodeMap.get(packageBinding.getName());
		final Pair<PackageNode, IPackageBinding> pair = oldPair != null
				? oldPair
				: internalCreatePackagePairFromNameComponents(packageBinding.getNameComponents());
		final PackageNode packageNode = pair.getA();
		if (pair.getB() == null) {
			pair.setB(packageBinding);
			packageNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(packageBinding.getAnnotations(),
					packageNode, JavaDependency.USE));
		}

		// TODO: add to node set
//		assert sourceFile != null;
//		for (AbstractNode node = packageNode; !node.isRoot(); node = node.getParent()) {
//			sourceFile.add(node);
//		}

		return packageNode;
	}

	//endregion Package

	//region Parser

	private void parseBodyDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull BodyDeclaration bodyDeclaration) throws JavaCiaException {
		if (bodyDeclaration instanceof AbstractTypeDeclaration) {
			parseAbstractTypeDeclaration(parentNode, (AbstractTypeDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
			parseAnnotationTypeMemberDeclaration(parentNode, (AnnotationTypeMemberDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof EnumConstantDeclaration) {
			parseEnumConstantDeclaration(parentNode, (EnumConstantDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof FieldDeclaration) {
			parseFieldDeclaration(parentNode, (FieldDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof Initializer) {
			parseInitializer(parentNode, (Initializer) bodyDeclaration);
		} else if (bodyDeclaration instanceof MethodDeclaration) {
			parseMethodDeclaration(parentNode, (MethodDeclaration) bodyDeclaration);
		}
	}

	private void parseAbstractTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AbstractTypeDeclaration abstractTypeDeclaration) throws JavaCiaException {
		if (abstractTypeDeclaration instanceof TypeDeclaration) {
			parseTypeDeclaration(parentNode, (TypeDeclaration) abstractTypeDeclaration);
		} else if (abstractTypeDeclaration instanceof EnumDeclaration) {
			parseEnumDeclaration(parentNode, (EnumDeclaration) abstractTypeDeclaration);
		} else if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
			parseAnnotationTypeDeclaration(parentNode, (AnnotationTypeDeclaration) abstractTypeDeclaration);
		}
	}

	private void parseTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull TypeDeclaration typeDeclaration) throws JavaCiaException {
		if (typeDeclaration.isInterface()) {
			parseInterfaceTypeDeclaration(parentNode, typeDeclaration);
		} else {
			parseClassTypeDeclaration(parentNode, typeDeclaration);
		}
	}

	private void parseClassTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull TypeDeclaration typeDeclaration) throws JavaCiaException {
		assert !typeDeclaration.isInterface() : "Expected a class TypeDeclaration.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final ClassNode classNode = parentNode.addChild(new ClassNode(sourceFile, parentNode,
				typeBinding.getName(), typeBinding.getBinaryName()));
		dependencies.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(classNode);

		internalParseClassTypeBinding(classNode, typeBinding, typeDeclaration.bodyDeclarations());
	}

	private void internalParseClassTypeBinding(@Nonnull ClassNode classNode, @Nonnull ITypeBinding typeBinding,
			@Nonnull List<?> bodyDeclarations) throws JavaCiaException {
		// put binding map
		bindingNodeMap.put(typeBinding, classNode);

		// set annotate
		classNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				classNode, JavaDependency.USE));

		// set modifier
		classNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		classNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				classNode, JavaDependency.USE));

		// set extends class
		final ITypeBinding superClassBinding = typeBinding.getSuperclass();
		if (superClassBinding != null) {
			classNode.setExtendsClass(types.createTypeFromTypeBinding(superClassBinding,
					classNode, JavaDependency.INHERITANCE));
		}

		// set implements interfaces
		classNode.setImplementsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				classNode, JavaDependency.INHERITANCE));

		// process children
		for (final Object object : bodyDeclarations) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(classNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseInterfaceTypeDeclaration(@Nonnull AbstractNode parentNode, @Nonnull TypeDeclaration typeDeclaration)
			throws JavaCiaException {
		assert !typeDeclaration.isInterface() : "Expected a interface TypeDeclaration.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on interface declaration!");

		final InterfaceNode interfaceNode = parentNode.addChild(new InterfaceNode(sourceFile, parentNode,
				typeBinding.getName(), typeBinding.getBinaryName()));
		dependencies.createDependencyToNode(parentNode, interfaceNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(interfaceNode);

		// put binding map
		bindingNodeMap.put(typeBinding, interfaceNode);

		// set annotate
		interfaceNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				interfaceNode, JavaDependency.USE));

		// set modifier
		interfaceNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		interfaceNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				interfaceNode, JavaDependency.USE));

		// set extends interfaces
		interfaceNode.setExtendsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				interfaceNode, JavaDependency.INHERITANCE));

		// process children
		for (final Object object : typeDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(interfaceNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseEnumDeclaration(@Nonnull AbstractNode parentNode, @Nonnull EnumDeclaration enumDeclaration)
			throws JavaCiaException {
		final ITypeBinding typeBinding = enumDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on enum declaration!");

		final EnumNode enumNode = parentNode.addChild(new EnumNode(sourceFile, parentNode,
				typeBinding.getName(), typeBinding.getBinaryName()));
		dependencies.createDependencyToNode(parentNode, enumNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(enumNode);

		// put binding map
		bindingNodeMap.put(typeBinding, enumNode);

		// set annotate
		enumNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				enumNode, JavaDependency.USE));

		// set modifier
		enumNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		enumNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				enumNode, JavaDependency.USE));

		// set implements interfaces
		enumNode.setImplementsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				enumNode, JavaDependency.INHERITANCE));

		// process enum constant children
		for (final Object object : enumDeclaration.enumConstants()) {
			if (object instanceof EnumConstantDeclaration) {
				parseBodyDeclaration(enumNode, (EnumConstantDeclaration) object);
			}
		}

		// process children
		for (final Object object : enumDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(enumNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseEnumConstantDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull EnumConstantDeclaration enumConstantDeclaration) throws JavaCiaException {
		final IVariableBinding variableBinding = enumConstantDeclaration.resolveVariable();
		if (variableBinding == null) throw new JavaCiaException("Cannot resolve binding on enum constant declaration!");

		final FieldNode fieldNode = parentNode.addChild(new FieldNode(sourceFile, parentNode,
				variableBinding.getName()));
		dependencies.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(fieldNode);

		internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

		// put constructor invocation dependency
		final IMethodBinding constructorBinding = enumConstantDeclaration.resolveConstructorBinding();
		if (constructorBinding != null) {
			dependencies.createDelayDependency(fieldNode,
					JavaDependencies.getOriginMethodBinding(constructorBinding), JavaDependency.INVOCATION);
		}

		// put delayed variable initializer
		for (final Object object : enumConstantDeclaration.arguments()) {
			if (object instanceof Expression) {
				walkDeclaration((Expression) object, fieldNode, fieldNode);
			}
		}

		// parse anonymous class declaration
		final AnonymousClassDeclaration anonymousClassDeclaration
				= enumConstantDeclaration.getAnonymousClassDeclaration();
		if (anonymousClassDeclaration != null) {
			parseAnonymousClassDeclaration(fieldNode, anonymousClassDeclaration);
		}
	}

	private void internalEnumConstantOrFieldVariableBinding(@Nonnull FieldNode fieldNode,
			@Nonnull IVariableBinding variableBinding) throws JavaCiaException {
		// put binding map
		bindingNodeMap.put(variableBinding, fieldNode);

		// set annotate
		fieldNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(variableBinding.getAnnotations(),
				fieldNode, JavaDependency.USE));

		// set modifier
		fieldNode.setModifiers(processModifiersFromBindingModifiers(variableBinding.getModifiers()));

		// set type
		fieldNode.setType(types.createTypeFromTypeBinding(variableBinding.getType(), fieldNode, JavaDependency.USE));
	}

	private void parseAnonymousClassDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnonymousClassDeclaration anonymousClassDeclaration) throws JavaCiaException {
		final ITypeBinding typeBinding = anonymousClassDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final String binaryName = typeBinding.getBinaryName();
		final String className = binaryName.contains("$")
				? binaryName.substring(binaryName.lastIndexOf('$'))
				: binaryName.contains(".")
				? binaryName.substring(binaryName.lastIndexOf('.')).replace('.', '$')
				: binaryName;

		final ClassNode classNode = parentNode.addChild(new ClassNode(sourceFile, parentNode, className, binaryName));
		dependencies.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(classNode);

		internalParseClassTypeBinding(classNode, typeBinding, anonymousClassDeclaration.bodyDeclarations());
	}

	private void parseAnnotationTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeDeclaration annotationTypeDeclaration) throws JavaCiaException {
		final ITypeBinding annotationBinding = annotationTypeDeclaration.resolveBinding();
		if (annotationBinding == null) throw new JavaCiaException("Cannot resolve binding on annotation declaration!");

		final AnnotationNode annotationNode = parentNode.addChild(new AnnotationNode(sourceFile, parentNode,
				annotationBinding.getName(), annotationBinding.getBinaryName()));
		dependencies.createDependencyToNode(parentNode, annotationNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(annotationNode);

		// put binding map
		bindingNodeMap.put(annotationBinding, annotationNode);

		// set annotate
		annotationNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(annotationBinding.getAnnotations(),
				annotationNode, JavaDependency.USE));

		// set modifier
		annotationNode.setModifiers(processModifiersFromBindingModifiers(annotationBinding.getModifiers()));

		// process children
		for (final Object object : annotationTypeDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(annotationNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseAnnotationTypeMemberDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeMemberDeclaration annotationMemberDeclaration) throws JavaCiaException {
		// check null and resolve origin
		final IMethodBinding annotationMemberBinding = annotationMemberDeclaration.resolveBinding();
		if (annotationMemberBinding == null)
			throw new JavaCiaException("Cannot resolve binding on annotation type member declaration!");

		// create node and containment dependency
		final MethodNode methodNode = parentNode.addChild(new MethodNode(sourceFile, parentNode,
				annotationMemberBinding.getName(), List.of()));
		dependencies.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(methodNode);

		// put binding map
		bindingNodeMap.put(annotationMemberBinding, methodNode);

		// set annotate
		methodNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(annotationMemberBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(annotationMemberBinding.getModifiers()));

		// set type
		final ITypeBinding typeBinding = annotationMemberBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(types.createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// put delayed default value
		final Expression annotationMemberDeclarationDefault = annotationMemberDeclaration.getDefault();
		if (annotationMemberDeclarationDefault != null) {
			walkDeclaration(annotationMemberDeclarationDefault, methodNode, methodNode);
		}
	}

	private void parseFieldDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull FieldDeclaration fieldDeclaration) throws JavaCiaException {
		for (final Object fragment : fieldDeclaration.fragments()) {
			if (fragment instanceof VariableDeclaration) {
				final VariableDeclaration variableDeclaration = (VariableDeclaration) fragment;

				final IVariableBinding variableBinding = variableDeclaration.resolveBinding();
				if (variableBinding == null) {
					throw new JavaCiaException("Cannot resolve binding on variable declaration!");
				}

				final FieldNode fieldNode = parentNode.addChild(new FieldNode(sourceFile, parentNode,
						variableBinding.getName()));
				dependencies.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

				// TODO: add to node set
//				assert sourceFile != null;
//				sourceFile.add(fieldNode);

				internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

				// put delayed variable initializer
				final Expression variableInitializer = variableDeclaration.getInitializer();
				if (variableInitializer != null) {
					final Pair<InitializerNode, List<InitializerNode.InitializerImpl>> pair
							= internalCreateOrGetInitializerNode(parentNode, fieldNode.isStatic());
					final InitializerNode initializerNode = pair.getA();

					// TODO: add to node set
//					assert sourceFile != null;
//					sourceFile.add(initializerNode);

					final List<InitializerNode.InitializerImpl> initializerList = pair.getB();
					initializerList.add(new InitializerNode.FieldInitializerImpl(fieldNode,
							format(variableInitializer.toString(), CodeFormatter.K_EXPRESSION)));
					walkDeclaration(variableInitializer, fieldNode, initializerNode);
				}
			}
		}
	}

	private void parseInitializer(@Nonnull AbstractNode parentNode, @Nonnull Initializer initializer)
			throws JavaCiaException {
		// create node and containment dependency
		final Pair<InitializerNode, List<InitializerNode.InitializerImpl>> pair = internalCreateOrGetInitializerNode(
				parentNode, (initializer.getModifiers() & Modifier.STATIC) != 0);
		final InitializerNode initializerNode = pair.getA();
		dependencies.createDependencyToNode(parentNode, initializerNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(initializerNode);

		// put delayed initializer body
		final Block initializerBody = initializer.getBody();
		if (initializerBody != null) {
			final List<InitializerNode.InitializerImpl> initializerList = pair.getB();
			initializerList.add(new InitializerNode.BlockInitializerImpl(
					format(initializerBody.toString(), CodeFormatter.K_STATEMENTS)));
			walkDeclaration(initializerBody, initializerNode, initializerNode);
		}
	}

	@Nonnull
	private Pair<InitializerNode, List<InitializerNode.InitializerImpl>> internalCreateOrGetInitializerNode(
			@Nonnull AbstractNode parentNode, boolean isStatic) {
		final Pair<Pair<InitializerNode, List<InitializerNode.InitializerImpl>>,
				Pair<InitializerNode, List<InitializerNode.InitializerImpl>>> pair
				= classInitializerMap.computeIfAbsent(parentNode, JavaSnapshotParser::createMutablePair);
		if (isStatic) {
			if (pair.getA() != null) return pair.getA();
			final InitializerNode initializer = parentNode.addChild(new InitializerNode(sourceFile, parentNode, true));
			final List<InitializerNode.InitializerImpl> initializerList = new ArrayList<>();
			initializer.setInitializers(initializerList);
			return pair.setGetA(Pair.immutableOf(initializer, initializerList));
		} else {
			if (pair.getB() != null) return pair.getB();
			final InitializerNode initializer = parentNode.addChild(new InitializerNode(sourceFile, parentNode, false));
			final List<InitializerNode.InitializerImpl> initializerList = new ArrayList<>();
			initializer.setInitializers(initializerList);
			return pair.setGetB(Pair.immutableOf(initializer, initializerList));
		}
	}

	private void parseMethodDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull MethodDeclaration methodDeclaration) throws JavaCiaException {
		// check null and resolve origin
		final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if (methodBinding == null) throw new JavaCiaException("Cannot resolve binding on method declaration!");

		// create parameter quick types
		final ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
		final List<AbstractType> parameterJavaTypes = new ArrayList<>(parameterTypeBindings.length);
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			parameterJavaTypes.add(types.createUnprocessedTypeFromTypeBinding(parameterTypeBinding, JavaDependency.USE));
		}

		// create node and containment dependency
		final MethodNode methodNode = parentNode.addChild(new MethodNode(sourceFile, parentNode,
				methodBinding.getName(), parameterJavaTypes));
		dependencies.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(methodNode);

		// create parameter proper types
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			types.processUnprocessedType(parameterTypeBinding, methodNode);
		}

		// put binding map
		bindingNodeMap.put(methodBinding, methodNode);

		// set annotate
		methodNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(methodBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(methodBinding.getModifiers()));

		// set type parameter
		methodNode.setTypeParameters(types.createTypesFromTypeBindings(methodBinding.getTypeParameters(),
				methodNode, JavaDependency.USE));

		// set type
		final ITypeBinding typeBinding = methodBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(types.createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// set exceptions
		methodNode.setExceptions(types.createTypesFromTypeBindings(methodBinding.getExceptionTypes(),
				methodNode, JavaDependency.USE));

		// put delayed method body
		final Block methodDeclarationBody = methodDeclaration.getBody();
		if (methodDeclarationBody != null) {
			methodNode.setBodyBlock(
					format(methodDeclarationBody.toString(), CodeFormatter.K_STATEMENTS));
			walkDeclaration(methodDeclarationBody, methodNode, methodNode);
		}
	}

	//endregion Parser

	//region Walker

	private void walkDeclaration(@Nonnull ASTNode astNode, @Nonnull AbstractNode parentNode,
			@Nonnull AbstractNode dependencyNode) throws JavaCiaException {
		delayedDependencyWalkers.add(Pair.immutableOf(astNode, dependencyNode));

		final JavaCiaException[] exceptionProxy = new JavaCiaException[]{null};
		astNode.accept(new ASTVisitor() {

			@Override
			public boolean preVisit2(@Nonnull ASTNode node) {
				return exceptionProxy[0] == null;
			}

			@Override
			public boolean visit(@Nonnull TypeDeclaration node) {
				try {
					parseTypeDeclaration(parentNode, node);
				} catch (JavaCiaException exception) {
					exceptionProxy[0] = exception;
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull AnonymousClassDeclaration node) {
				try {
					parseAnonymousClassDeclaration(parentNode, node);
				} catch (JavaCiaException exception) {
					exceptionProxy[0] = exception;
				}
				return false;
			}

		});
		if (exceptionProxy[0] != null) throw exceptionProxy[0];
	}

	private void walkDependency(@Nonnull ASTNode astNode, @Nonnull AbstractNode javaNode) throws JavaCiaException {
		final JavaCiaException[] exceptionProxy = new JavaCiaException[]{null};
		astNode.accept(new ASTVisitor() {

			@Override
			public boolean preVisit2(@Nonnull ASTNode node) {
				return exceptionProxy[0] == null;
			}

			@Override
			public boolean visit(@Nonnull SuperConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull ConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull SuperMethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super method invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull MethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on method invocation!");
				}
				return false;
			}

			private void createDependencyFromInvocation(@Nonnull IMethodBinding binding,
					@Nonnull List<?> typeArguments, @Nonnull List<?> arguments) {
				dependencies.createDelayDependency(javaNode,
						JavaDependencies.getOriginMethodBinding(binding), JavaDependency.INVOCATION);
				for (final Object object : typeArguments) {
					if (object instanceof Type) ((Type) object).accept(this);
				}
				for (final Object object : arguments) {
					if (object instanceof Expression) ((Expression) object).accept(this);
				}
			}

			@Override
			public boolean visit(@Nonnull SimpleName node) {
				final IBinding binding = node.resolveBinding();
				if (binding == null && !enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on simple name!");
					return false;
				}
				final IBinding originalBinding = binding instanceof ITypeBinding
						? JavaDependencies.getOriginTypeBinding((ITypeBinding) binding)
						: binding instanceof IMethodBinding
						? JavaDependencies.getOriginMethodBinding((IMethodBinding) binding)
						: binding instanceof IVariableBinding
						? JavaDependencies.getOriginVariableBinding((IVariableBinding) binding)
						: null;
				if (originalBinding != null) {
					dependencies.createDelayDependency(javaNode, originalBinding, JavaDependency.USE);
				}
				return true;
			}

			@Override
			public boolean visit(@Nonnull TypeDeclaration node) {
				return false;
			}

			@Override
			public boolean visit(@Nonnull AnonymousClassDeclaration node) {
				return false;
			}

		});
		if (exceptionProxy[0] != null) throw exceptionProxy[0];
	}

	//endregion Walker

}
