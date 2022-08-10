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
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class JavaNodeBuilder extends FileASTRequestor {

	@Nonnull private final Map<String, SourceFile> sourceFileMap;

	@Nonnull private final JavaParser parser;
	@Nonnull private final RootNode rootNode;
	@Nonnull private final Map<IBinding, AbstractNode> bindingNodeMap;

	@Nonnull private final CodeFormatter codeFormatter;
	private final boolean recoveryEnabled;


	@Nonnull private final JavaAnnotateBuilder annotates = new JavaAnnotateBuilder(this);
	@Nonnull private final JavaTypeBuilder types = new JavaTypeBuilder(this, annotates);

	@Nonnull private final Map<AbstractNode, Map<IBinding, int[]>> delayedDependencies = new LinkedHashMap<>();
	@Nonnull private final List<Pair<ASTNode, AbstractNode>> delayedDependencyWalkers = new LinkedList<>();

	@Nullable private JavaCiaException exception;
	@Nullable private SourceFile sourceFile;


	JavaNodeBuilder(@Nonnull Map<String, SourceFile> sourceFileMap, @Nonnull JavaParser parser,
			@Nonnull RootNode rootNode, @Nonnull Map<IBinding, AbstractNode> bindingNodeMap,
			@Nonnull CodeFormatter formatter, boolean recoveryEnabled) {
		this.sourceFileMap = sourceFileMap;
		this.parser = parser;
		this.rootNode = rootNode;
		this.bindingNodeMap = bindingNodeMap;
		this.codeFormatter = formatter;
		this.recoveryEnabled = recoveryEnabled;
	}


	@Override
	public void acceptAST(@Nonnull String sourcePath, @Nonnull CompilationUnit compilationUnit) {
		if (exception != null) return;
		try {
			this.sourceFile = sourceFileMap.get(sourcePath);
			if (sourceFile == null) throw new JavaCiaException("Unknown source path!");

			final PackageDeclaration packageDeclaration = compilationUnit.getPackage();
			final AbstractNode parentNode = packageDeclaration != null
					? createPackageNodeFromPackageDeclaration(packageDeclaration)
					: rootNode;
			for (final Object type : compilationUnit.types()) {
				if (type instanceof AbstractTypeDeclaration) {
					parseAbstractTypeDeclaration(parentNode, (AbstractTypeDeclaration) type);
				}
			}

			this.sourceFile = null;
		} catch (JavaCiaException exception) {
			this.exception = exception;
		}
	}

	void postprocessing() throws JavaCiaException {
		if (exception != null) throw exception;

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
		for (final Map.Entry<AbstractNode, Map<IBinding, int[]>> entry : delayedDependencies.entrySet()) {
			final AbstractNode sourceNode = entry.getKey();
			for (final Map.Entry<IBinding, int[]> bindingEntry : entry.getValue().entrySet()) {
				final AbstractNode targetNode = bindingNodeMap.get(bindingEntry.getKey());
				if (targetNode != null && sourceNode != targetNode) {
					parser.createDependenciesToNode(sourceNode, targetNode, bindingEntry.getValue());
				}
			}
		}
		delayedDependencies.clear();

		// delay method overrides
		{
			final JavaOverrideBuilder overrides = new JavaOverrideBuilder(parser);
			overrides.processOverrides(bindingNodeMap);
		}

		// set class initializers
		for (final AbstractNode node : bindingNodeMap.values()) {
			if (node instanceof ClassNode) {
				final ClassNode classNode = (ClassNode) node;
				final List<AbstractNode> children = classNode.getChildren();
				final List<InitializerNode> initializerNodes = new ArrayList<>();
				final List<MethodNode> constructorNodes = new ArrayList<>();
				for (final AbstractNode childNode : children) {
					if (childNode instanceof InitializerNode) {
						initializerNodes.add((InitializerNode) childNode);
					} else if (childNode instanceof MethodNode) {
						final MethodNode methodNode = (MethodNode) childNode;
						if (methodNode.isConstructor()) constructorNodes.add(methodNode);
					}
				}
				if (initializerNodes.size() > 0 && constructorNodes.size() > 0) {
					for (final MethodNode constructorNode : constructorNodes) {
						for (final InitializerNode initializerNode : initializerNodes) {
							parser.createDependencyToNode(constructorNode, initializerNode, JavaDependency.INVOCATION);
						}
					}
				}
			}
		}
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


	//region Delayed Dependency

	static void combineDelayedDependencyMap(@Nonnull Map<IBinding, int[]> targetMap,
			@Nonnull Map<IBinding, int[]> sourceMap) {
		for (final Map.Entry<IBinding, int[]> entry : sourceMap.entrySet()) {
			final IBinding binding = entry.getKey();
			final int[] sourceCounts = entry.getValue();
			final int[] targetCounts = targetMap.get(binding);
			if (targetCounts != null) {
				for (int i = 0; i < targetCounts.length; i++) targetCounts[i] += sourceCounts[i];
			} else {
				targetMap.put(binding, sourceCounts.clone());
			}
		}
	}

	static void addDependencyToDelayedDependencyMap(@Nonnull Map<IBinding, int[]> targetMap,
			@Nonnull IBinding targetBinding, @Nonnull JavaDependency dependencyType) {
		final int[] counts = targetMap
				.computeIfAbsent(targetBinding, JavaParser::createDependencyCounts);
		counts[dependencyType.ordinal()] += 1;
	}

	void createDelayDependencyFromDependencyMap(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull Map<IBinding, int[]> dependencyMap) {
		final Map<IBinding, int[]> oldDependencyMap = delayedDependencies.get(dependencySourceNode);
		if (oldDependencyMap == null) {
			final Map<IBinding, int[]> newDependencyMap = new LinkedHashMap<>(dependencyMap);
			for (final Map.Entry<IBinding, int[]> entry : newDependencyMap.entrySet()) {
				entry.setValue(entry.getValue().clone());
			}
			delayedDependencies.put(dependencySourceNode, newDependencyMap);
		} else {
			combineDelayedDependencyMap(oldDependencyMap, dependencyMap);
		}
	}

	void createDelayDependency(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull IBinding targetBinding, @Nonnull JavaDependency dependencyType) {
		addDependencyToDelayedDependencyMap(
				delayedDependencies.computeIfAbsent(dependencySourceNode, JavaParser::createLinkedHashMap),
				targetBinding, dependencyType);
	}

	@Nonnull
	static ITypeBinding getOriginTypeBinding(@Nonnull ITypeBinding typeBinding) {
		while (true) {
			final ITypeBinding parentBinding = typeBinding.getTypeDeclaration();
			if (parentBinding == null || parentBinding == typeBinding) return typeBinding;
			typeBinding = parentBinding;
		}
	}

	@Nonnull
	static IMethodBinding getOriginMethodBinding(@Nonnull IMethodBinding methodBinding) {
		while (true) {
			final IMethodBinding parentBinding = methodBinding.getMethodDeclaration();
			if (parentBinding == null || parentBinding == methodBinding) return methodBinding;
			methodBinding = parentBinding;
		}
	}

	@Nonnull
	static IVariableBinding getOriginVariableBinding(@Nonnull IVariableBinding variableBinding) {
		while (true) {
			final IVariableBinding parentBinding = variableBinding.getVariableDeclaration();
			if (parentBinding == null || parentBinding == variableBinding) return variableBinding;
			variableBinding = parentBinding;
		}
	}

	//endregion Delayed Dependency

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

	//region Parser

	@Nonnull
	private PackageNode createPackageNodeFromPackageDeclaration(@Nonnull PackageDeclaration packageDeclaration)
			throws JavaCiaException {
		final IPackageBinding packageBinding = packageDeclaration.resolveBinding();
		if (packageBinding == null) throw new JavaCiaException("Cannot resolve binding on package declaration!");

		final PackageNode packageNode = parser.createPackageFromNameComponents(packageBinding.getNameComponents());
		final IAnnotationBinding[] annotations = packageBinding.getAnnotations();
		if (annotations.length > 0) {
			packageNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(
					annotations, packageNode, JavaDependency.USE));
		}
		return packageNode;
	}

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
		parser.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

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
		parser.createDependencyToNode(parentNode, interfaceNode, JavaDependency.MEMBER);

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
		parser.createDependencyToNode(parentNode, enumNode, JavaDependency.MEMBER);

		// put binding map
		bindingNodeMap.put(typeBinding, enumNode);

		// set annotate
		enumNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				enumNode, JavaDependency.USE));

		// set modifier
		enumNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

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
		parser.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

		internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

		// put constructor invocation dependency
		final IMethodBinding constructorBinding = enumConstantDeclaration.resolveConstructorBinding();
		if (constructorBinding != null) {
			createDelayDependency(fieldNode, getOriginMethodBinding(constructorBinding), JavaDependency.INVOCATION);
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
		parser.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		internalParseClassTypeBinding(classNode, typeBinding, anonymousClassDeclaration.bodyDeclarations());
	}

	private void parseAnnotationTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeDeclaration annotationTypeDeclaration) throws JavaCiaException {
		final ITypeBinding annotationBinding = annotationTypeDeclaration.resolveBinding();
		if (annotationBinding == null) throw new JavaCiaException("Cannot resolve binding on annotation declaration!");

		final AnnotationNode annotationNode = parentNode.addChild(new AnnotationNode(sourceFile, parentNode,
				annotationBinding.getName(), annotationBinding.getBinaryName()));
		parser.createDependencyToNode(parentNode, annotationNode, JavaDependency.MEMBER);

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
				annotationMemberBinding.getName(), false, List.of()));
		parser.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

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
				parser.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

				internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

				// put delayed variable initializer
				final Expression variableInitializer = variableDeclaration.getInitializer();
				if (variableInitializer != null) {
					fieldNode.setValue(format(variableInitializer.toString(), CodeFormatter.K_EXPRESSION));
					walkDeclaration(variableInitializer, fieldNode, fieldNode);
				}
			}
		}
	}

	private void parseInitializer(@Nonnull AbstractNode parentNode, @Nonnull Initializer initializer)
			throws JavaCiaException {
		// create node and containment dependency
		final InitializerNode initializerNode = parentNode.addChild(new InitializerNode(sourceFile, parentNode,
				(initializer.getModifiers() & Modifier.STATIC) != 0));
		parser.createDependencyToNode(parentNode, initializerNode, JavaDependency.MEMBER);

		// put delayed initializer body
		final Block initializerBody = initializer.getBody();
		if (initializerBody != null) {
			initializerNode.setBodyBlock(format(initializerBody.toString(), CodeFormatter.K_STATEMENTS));
			walkDeclaration(initializerBody, initializerNode, initializerNode);
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
				methodBinding.getName(), methodBinding.isConstructor(), parameterJavaTypes));
		parser.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

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
			methodNode.setBodyBlock(format(methodDeclarationBody.toString(), CodeFormatter.K_STATEMENTS));
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
				} else if (!recoveryEnabled) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull ConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!recoveryEnabled) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull SuperMethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!recoveryEnabled) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super method invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull MethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!recoveryEnabled) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on method invocation!");
				}
				return false;
			}

			private void createDependencyFromInvocation(@Nonnull IMethodBinding binding,
					@Nonnull List<?> typeArguments, @Nonnull List<?> arguments) {
				createDelayDependency(javaNode, getOriginMethodBinding(binding), JavaDependency.INVOCATION);
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
				if (binding == null && !recoveryEnabled) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on simple name!");
					return false;
				}
				final IBinding originalBinding = binding instanceof ITypeBinding
						? getOriginTypeBinding((ITypeBinding) binding)
						: binding instanceof IMethodBinding
						? getOriginMethodBinding((IMethodBinding) binding)
						: binding instanceof IVariableBinding
						? getOriginVariableBinding((IVariableBinding) binding)
						: null;
				if (originalBinding != null) {
					createDelayDependency(javaNode, originalBinding, JavaDependency.USE);
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
