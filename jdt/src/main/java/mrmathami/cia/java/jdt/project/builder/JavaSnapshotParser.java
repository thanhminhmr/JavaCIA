/*
 * Copyright (C) 2020 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
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
import mrmathami.cia.java.tree.JavaModifier;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.cia.java.tree.node.container.JavaAnnotationContainer;
import mrmathami.cia.java.tree.node.container.JavaClassContainer;
import mrmathami.cia.java.tree.node.container.JavaEnumContainer;
import mrmathami.cia.java.tree.node.container.JavaFieldContainer;
import mrmathami.cia.java.tree.node.container.JavaInitializerContainer;
import mrmathami.cia.java.tree.node.container.JavaInterfaceContainer;
import mrmathami.cia.java.tree.node.container.JavaMethodContainer;
import mrmathami.cia.java.jdt.tree.annotate.Annotate;
import mrmathami.cia.java.jdt.tree.annotate.Annotate.NodeValueImpl;
import mrmathami.cia.java.jdt.tree.annotate.Annotate.ParameterImpl;
import mrmathami.cia.java.jdt.tree.dependency.DependencyCountTable;
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
import mrmathami.cia.java.jdt.tree.type.ReferenceType;
import mrmathami.cia.java.jdt.tree.type.SimpleType;
import mrmathami.cia.java.jdt.tree.type.SyntheticType;
import mrmathami.utils.Pair;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
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
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static mrmathami.cia.java.jdt.Utilities.DEPENDENCY_TYPES;

final class JavaSnapshotParser extends FileASTRequestor {

	@Nonnull private static final String[] EMPTY = new String[0];

	@Nonnull private final CodeFormatter codeFormatter;

	@Nonnull private final RootNode rootNode = new RootNode();
	@Nonnull private final Map<String, Pair<PackageNode, IPackageBinding>> packageNodeMap = new HashMap<>();

	@Nonnull private final Map<IBinding, AbstractNode> bindingNodeMap = new HashMap<>();


	@Nonnull private final Map<ITypeBinding, Pair<AbstractType, Map<IBinding, int[]>>> delayedTypes = new HashMap<>();
	@Nonnull private final Map<ITypeBinding, List<ReferenceType>> delayedReferenceTypeNodes = new HashMap<>();

	@Nonnull private final Map<IAnnotationBinding, Pair<Annotate, Map<IBinding, int[]>>> delayedAnnotations = new HashMap<>();
	@Nonnull private final Map<ITypeBinding, List<Annotate>> delayedAnnotationNodes = new HashMap<>();
	@Nonnull private final Map<IMethodBinding, List<ParameterImpl>> delayedAnnotationParameters = new HashMap<>();
	@Nonnull private final Map<IBinding, List<NodeValueImpl>> delayedAnnotationNodeValues = new HashMap<>();

	@Nonnull private final Map<AbstractNode, Map<IBinding, int[]>> delayedDependencies = new IdentityHashMap<>();
	@Nonnull private final List<Pair<ASTNode, AbstractNode>> delayedDependencyWalkers = new LinkedList<>();

	@Nonnull private final Map<MethodNode, IMethodBinding> methodBindingMap = new IdentityHashMap<>();
	@Nonnull private final Set<AbstractNode> delayedOverrideProcessedNode = new HashSet<>();
	@Nonnull private final Map<AbstractNode, List<MethodNode>> delayedOverrideChildMethodsMap = new IdentityHashMap<>();
	@Nonnull private final Map<MethodNode, List<MethodNode>> delayedMethodOverridesMap = new IdentityHashMap<>();

	@Nonnull private final Map<AbstractNode, Map<AbstractNode, int[]>> nodeDependencies = new IdentityHashMap<>();

	@Nonnull private final Map<AbstractNode, Pair<InitializerNode, InitializerNode>> classInitializerMap = new IdentityHashMap<>();
	@Nonnull private final Map<InitializerNode, List<InitializerNode.InitializerImpl>> initializerListMap = new IdentityHashMap<>();

	@Nullable private JavaCiaException exception;


	private JavaSnapshotParser(@Nonnull CodeFormatter codeFormatter) {
		this.codeFormatter = codeFormatter;
	}


	@Nonnull
	static JavaRootNode build(@Nonnull Map<Path, List<Path>> javaSources, @Nonnull List<Path> classPaths)
			throws JavaCiaException {
		final List<String> classPathList = new ArrayList<>(classPaths.size() + javaSources.size());
		final List<String> projectFileList = new LinkedList<>();
		for (final Map.Entry<Path, List<Path>> entry : javaSources.entrySet()) {
			classPathList.add(entry.getKey().toAbsolutePath().toString());
			for (final Path projectFilePath : entry.getValue()) {
				projectFileList.add(projectFilePath.toAbsolutePath().toString());
			}
		}
		for (final Path classPath : classPaths) {
			classPathList.add(classPath.toAbsolutePath().toString());
		}

		final String[] sourcePathArray = projectFileList.toArray(EMPTY);

		final String[] sourceEncodingArray = new String[sourcePathArray.length];
		Arrays.fill(sourceEncodingArray, StandardCharsets.UTF_8.name());

		final String[] classPathArray = classPathList.toArray(EMPTY);

		return parse(sourcePathArray, sourceEncodingArray, classPathArray);
	}

	//region JDT dependent

	@Nonnull
	private static JavaRootNode parse(@Nonnull String[] sourcePathArray, @Nonnull String[] sourceEncodingArray,
			@Nonnull String[] classPathArray) throws JavaCiaException {

		final ASTParser astParser = ASTParser.newParser(AST.JLS14);
		final Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_14, options);
		astParser.setCompilerOptions(options);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		astParser.setEnvironment(classPathArray, null, null, true);

		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "65536");

		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);
		final JavaSnapshotParser parser = new JavaSnapshotParser(codeFormatter);
		astParser.createASTs(sourcePathArray, sourceEncodingArray, EMPTY, parser, null);
		return parser.postProcessing();
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

	//endregion JDT dependent

	//region Misc

	@Nonnull
	private static <A, B, R> Pair<A, B> createMutablePair(@Nullable R any) {
		return Pair.mutableOf(null, null);
	}

	@Nonnull
	private static <A, R> List<A> createArrayList(@Nullable R any) {
		return new ArrayList<>();
	}

	@Nonnull
	private static <A, B, R> Map<A, B> createHashMap(@Nullable R any) {
		return new HashMap<>();
	}

	@Nonnull
	private static <A, B, R> Map<A, B> createIdentityHashMap(@Nullable R any) {
		return new IdentityHashMap<>();
	}

	//endregion Misc

	//region Post Processing

	//region Post Processing Overrides

	@Nonnull
	private static List<MethodNode> getChildMethodsFromNode(@Nonnull AbstractNode parentNode) {
		return parentNode.getChildren(MethodNode.class, new ArrayList<>());
	}

	private void internalPostProcessingOverrideInterface(@Nonnull InterfaceNode interfaceNode) {
		if (!delayedOverrideProcessedNode.add(interfaceNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= internalProcessOverrideInterfaceTypes(interfaceNode.getExtendsInterfaces());
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : delayedOverrideChildMethodsMap
					.computeIfAbsent(interfaceNode, JavaSnapshotParser::getChildMethodsFromNode)) {
				internalProcessOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	private void internalPostProcessingOverrideClass(@Nonnull ClassNode classNode) {
		if (!delayedOverrideProcessedNode.add(classNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= internalProcessOverrideInterfaceTypes(classNode.getImplementsInterfaces());
		final AbstractType extendsClass = classNode.getExtendsClass();
		if (extendsClass instanceof ReferenceType) {
			final AbstractNode extendsClassNode = ((ReferenceType) extendsClass).getNode();
			if (extendsClassNode instanceof ClassNode) {
				internalPostProcessingOverrideClass((ClassNode) extendsClassNode);
				final List<MethodNode> parentMethodNodes = delayedOverrideChildMethodsMap
						.computeIfAbsent(extendsClassNode, JavaSnapshotParser::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			} else if (extendsClassNode instanceof EnumNode) {
				internalPostProcessingOverrideEnum((EnumNode) extendsClassNode);
				final List<MethodNode> parentMethodNodes = delayedOverrideChildMethodsMap
						.computeIfAbsent(extendsClassNode, JavaSnapshotParser::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			}
		}
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : delayedOverrideChildMethodsMap
					.computeIfAbsent(classNode, JavaSnapshotParser::getChildMethodsFromNode)) {
				internalProcessOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	private void internalPostProcessingOverrideEnum(@Nonnull EnumNode enumNode) {
		if (!delayedOverrideProcessedNode.add(enumNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= internalProcessOverrideInterfaceTypes(enumNode.getImplementsInterfaces());
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : delayedOverrideChildMethodsMap
					.computeIfAbsent(enumNode, JavaSnapshotParser::getChildMethodsFromNode)) {
				internalProcessOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	@Nonnull
	private List<List<MethodNode>> internalProcessOverrideInterfaceTypes(@Nonnull List<AbstractType> interfaceTypes) {
		final List<List<MethodNode>> parentMethodsList = new ArrayList<>();
		for (final AbstractType interfaceType : interfaceTypes) {
			if (!(interfaceType instanceof ReferenceType)) continue;
			final AbstractNode interfaceNode = ((ReferenceType) interfaceType).getNode();
			if (!(interfaceNode instanceof InterfaceNode)) continue;
			internalPostProcessingOverrideInterface((InterfaceNode) interfaceNode);
			final List<MethodNode> methodNodes = delayedOverrideChildMethodsMap
					.computeIfAbsent(interfaceNode, JavaSnapshotParser::getChildMethodsFromNode);
			if (!methodNodes.isEmpty()) parentMethodsList.add(methodNodes);
		}
		return parentMethodsList;
	}

	private void internalProcessOverrideParentMethodsList(@Nonnull MethodNode childMethod,
			@Nonnull List<List<MethodNode>> parentMethodsList) {
		final List<MethodNode> childMethodOverrides = delayedMethodOverridesMap
				.computeIfAbsent(childMethod, JavaSnapshotParser::createArrayList);
		final int childMethodParameterSize = childMethod.getParameters().size();
		final IMethodBinding childMethodBinding = methodBindingMap.get(childMethod);
		assert childMethodBinding != null : "A method node should always have an associate method binding!";

		for (final List<MethodNode> parentMethods : parentMethodsList) {
			for (final MethodNode parentMethod : parentMethods) {
				if (childMethodParameterSize == parentMethod.getParameters().size()) {
					final IMethodBinding parentMethodBinding = methodBindingMap.get(parentMethod);
					assert parentMethodBinding != null
							: "A method node should always have an associate method binding!";

					if (childMethodBinding.overrides(parentMethodBinding)) {
						childMethodOverrides.add(parentMethod);
						childMethodOverrides.addAll(delayedMethodOverridesMap
								.computeIfAbsent(parentMethod, JavaSnapshotParser::createArrayList));
						break;
					}
				}
			}
		}
		for (final MethodNode childMethodOverride : childMethodOverrides) {
//			childMethod.addDependencyTo(childMethodOverride, JavaDependency.OVERRIDE);
			createDependencyToNode(childMethod, childMethodOverride, JavaDependency.OVERRIDE);
		}
	}

	//endregion Post Processing Overrides

	@Nonnull
	private RootNode postProcessing() throws JavaCiaException {
		if (exception != null) throw exception;

		// =====
		// delay dependency walkers
		for (final Pair<ASTNode, AbstractNode> pair : delayedDependencyWalkers) {
			walkDependency(pair.getA(), pair.getB());
		}
		delayedDependencyWalkers.clear();

		// =====
		// delay reference type node
		delayedTypes.clear();
		for (final Map.Entry<ITypeBinding, List<ReferenceType>> entry : delayedReferenceTypeNodes.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final ReferenceType referenceType : entry.getValue()) referenceType.setNode(node);
			}
		}
		delayedReferenceTypeNodes.clear();

		// =====
		// delay annotations
		delayedAnnotations.clear();

		// delay annotate annotation nodes
		for (final Map.Entry<ITypeBinding, List<Annotate>> entry : delayedAnnotationNodes.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final Annotate annotate : entry.getValue()) annotate.setNode(node);
			}
		}
		delayedAnnotationNodes.clear();

		// delay annotate parameters
		for (final Map.Entry<IMethodBinding, List<ParameterImpl>> entry : delayedAnnotationParameters.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final ParameterImpl parameter : entry.getValue()) parameter.setNode(node);
			}
		}
		delayedAnnotationParameters.clear();

		// delay annotate values
		for (final Map.Entry<IBinding, List<NodeValueImpl>> entry : delayedAnnotationNodeValues.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final NodeValueImpl value : entry.getValue()) value.setNode(node);
			}
		}
		delayedAnnotationNodeValues.clear();

		// =====
		// delay dependencies
		for (final Map.Entry<AbstractNode, Map<IBinding, int[]>> entry : delayedDependencies.entrySet()) {
			final AbstractNode sourceNode = entry.getKey();
			final Map<IBinding, int[]> dependencyMap = entry.getValue();
			for (final Map.Entry<IBinding, int[]> bindingEntry : dependencyMap.entrySet()) {
				final AbstractNode targetNode = bindingNodeMap.get(bindingEntry.getKey());
				if (targetNode != null && sourceNode != targetNode) {
					createDependenciesToNode(sourceNode, targetNode, bindingEntry.getValue());
				}
			}
		}
		delayedDependencies.clear();

		// delay override dependencies
		for (final AbstractNode node : bindingNodeMap.values()) {
			if (node instanceof InterfaceNode) {
				internalPostProcessingOverrideInterface((InterfaceNode) node);
			} else if (node instanceof ClassNode) {
				internalPostProcessingOverrideClass((ClassNode) node);
			} else if (node instanceof EnumNode) {
				internalPostProcessingOverrideEnum((EnumNode) node);
			}
		}
		methodBindingMap.clear();
		delayedOverrideProcessedNode.clear();
		delayedOverrideChildMethodsMap.clear();
		delayedMethodOverridesMap.clear();

		// set main dependency
		final Map<DependencyCountTable, DependencyCountTable> nodeDependencyMap = new HashMap<>();
		for (final Map.Entry<AbstractNode, Map<AbstractNode, int[]>> sourceEntry : nodeDependencies.entrySet()) {
			final AbstractNode sourceNode = sourceEntry.getKey();
			for (final Map.Entry<AbstractNode, int[]> destinationEntry : sourceEntry.getValue().entrySet()) {
				final AbstractNode destinationNode = destinationEntry.getKey();
				final int[] dependencyCounts = destinationEntry.getValue();
				final DependencyCountTable nodeDependency = nodeDependencyMap
						.computeIfAbsent(new DependencyCountTable(dependencyCounts), Objects::requireNonNull);
				sourceNode.createDependencyTo(destinationNode, nodeDependency);
			}
		}
		nodeDependencyMap.clear();
		nodeDependencies.clear();

		// =====
		// set class initializers
		for (final Map.Entry<InitializerNode, List<InitializerNode.InitializerImpl>> entry : initializerListMap.entrySet()) {
			entry.getKey().setInitializers(entry.getValue());
		}
		initializerListMap.clear();
		classInitializerMap.clear();

		// =====
		// final cleanup
		packageNodeMap.clear();
		bindingNodeMap.clear();

		// =====
		// freeze root node
		rootNode.freeze();
		return rootNode;
	}

	//endregion Post Processing

	//region Dependency

	@Nonnull
	private static <R> int[] internalCreateDependencyCounts(@Nullable R any) {
		return new int[DEPENDENCY_TYPES.length];
	}

	//region Main Dependency

	@Nonnull
	private int[] internalGetOrCreateDependencyCounts(@Nonnull AbstractNode sourceNode, @Nonnull AbstractNode targetNode) {
		return nodeDependencies
				.computeIfAbsent(sourceNode, JavaSnapshotParser::createIdentityHashMap)
				.computeIfAbsent(targetNode, JavaSnapshotParser::internalCreateDependencyCounts);
	}

	private void createDependenciesToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull int[] dependencyCounts) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		final int length = currentCounts.length;
		for (int i = 0; i < length; i++) currentCounts[i] += dependencyCounts[i];
	}

	private void createDependencyToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull JavaDependency dependencyType) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		currentCounts[dependencyType.ordinal()] += 1;
	}

	//endregion Main Dependency

	//region Delayed Dependency

	private static void internalCombineDelayedDependencyMap(@Nonnull Map<IBinding, int[]> targetMap,
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

	private static void internalAddDependencyToDelayedDependencyMap(@Nonnull Map<IBinding, int[]> targetMap,
			@Nonnull IBinding targetBinding, @Nonnull JavaDependency dependencyType) {
		final int[] counts = targetMap
				.computeIfAbsent(targetBinding, JavaSnapshotParser::internalCreateDependencyCounts);
		counts[dependencyType.ordinal()] += 1;
	}

	private void createDelayDependencyFromDependencyMap(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull Map<IBinding, int[]> dependencyMap) {
		final Map<IBinding, int[]> oldDependencyMap = delayedDependencies.get(dependencySourceNode);
		if (oldDependencyMap == null) {
			final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>(dependencyMap);
			for (final Map.Entry<IBinding, int[]> entry : newDependencyMap.entrySet()) {
				entry.setValue(entry.getValue().clone());
			}
			delayedDependencies.put(dependencySourceNode, newDependencyMap);
		} else {
			internalCombineDelayedDependencyMap(oldDependencyMap, dependencyMap);
		}
	}

	private void createDelayDependency(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull IBinding targetBinding, @Nonnull JavaDependency dependencyType) {
		internalAddDependencyToDelayedDependencyMap(
				delayedDependencies.computeIfAbsent(dependencySourceNode, JavaSnapshotParser::createHashMap),
				targetBinding, dependencyType);
	}

	@Nonnull
	private static ITypeBinding getOriginTypeBinding(@Nonnull ITypeBinding typeBinding) {
		while (true) {
			final ITypeBinding parentBinding = typeBinding.getTypeDeclaration();
			if (parentBinding == null || parentBinding == typeBinding) return typeBinding;
			typeBinding = parentBinding;
		}
	}

	@Nonnull
	private static IMethodBinding getOriginMethodBinding(@Nonnull IMethodBinding methodBinding) {
		while (true) {
			final IMethodBinding parentBinding = methodBinding.getMethodDeclaration();
			if (parentBinding == null || parentBinding == methodBinding) return methodBinding;
			methodBinding = parentBinding;
		}
	}

	@Nonnull
	private static IVariableBinding getOriginVariableBinding(@Nonnull IVariableBinding variableBinding) {
		while (true) {
			final IVariableBinding parentBinding = variableBinding.getVariableDeclaration();
			if (parentBinding == null || parentBinding == variableBinding) return variableBinding;
			variableBinding = parentBinding;
		}
	}

	//endregion Delayed Dependency

	//endregion Dependency

	//region JavaAnnotate

	@Nonnull
	private List<Annotate> internalCreateAnnotatesFromAnnotationBindings(
			@Nonnull IAnnotationBinding[] annotationBindings, @Nonnull JavaDependency dependencyType,
			@Nullable Map<IBinding, int[]> dependencyMap) throws JavaCiaException {
		if (annotationBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final List<Annotate> annotates = new ArrayList<>(annotationBindings.length);
		for (final IAnnotationBinding annotationBinding : annotationBindings) {
			annotates.add(
					internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap));
		}
		return annotates;
	}

	@Nonnull
	private Annotate.ValueImpl internalProcessAnnotateValue(@Nonnull Object value,
			@Nonnull JavaDependency dependencyType, @Nonnull Map<IBinding, int[]> dependencyMap)
			throws JavaCiaException {

		if (JavaAnnotate.SimpleValue.isValidValueType(value)) {
			return new Annotate.SimpleValueImpl(value);

		} else if (value instanceof ITypeBinding) {
			final ITypeBinding typeBinding = (ITypeBinding) value;
			final NodeValueImpl nodeValue =
					new NodeValueImpl("Class<" + typeBinding.getQualifiedName() + ">");
			internalAddDependencyToDelayedDependencyMap(dependencyMap,
					getOriginTypeBinding(typeBinding), dependencyType);
			delayedAnnotationNodeValues.computeIfAbsent(typeBinding, JavaSnapshotParser::createArrayList)
					.add(nodeValue);
			return nodeValue;

		} else if (value instanceof IVariableBinding) {
			final IVariableBinding variableBinding = (IVariableBinding) value;
			final NodeValueImpl nodeValue = new NodeValueImpl(
					variableBinding.getDeclaringClass().getQualifiedName() + '.' + variableBinding.getName());
			internalAddDependencyToDelayedDependencyMap(dependencyMap,
					getOriginVariableBinding(variableBinding), dependencyType);
			delayedAnnotationNodeValues.computeIfAbsent(variableBinding, JavaSnapshotParser::createArrayList)
					.add(nodeValue);
			return nodeValue;

		} else if (value instanceof IAnnotationBinding) {
			final IAnnotationBinding annotationBinding = (IAnnotationBinding) value;
			final Annotate annotate
					= internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap);
			final Annotate.AnnotateValueImpl annotateValue = new Annotate.AnnotateValueImpl();
			annotateValue.setAnnotate(annotate);
			return annotateValue;

		} else if (value instanceof Object[]) {
			final Object[] objects = (Object[]) value;
			final Annotate.ArrayValueImpl arrayValue = new Annotate.ArrayValueImpl();
			if (objects.length <= 0) return arrayValue;
			final List<Annotate.NonArrayValueImpl> values = new ArrayList<>(objects.length);
			for (final Object object : objects) {
				final Annotate.ValueImpl innerValue =
						internalProcessAnnotateValue(object, dependencyType, dependencyMap);
				if (!(innerValue instanceof Annotate.NonArrayValueImpl)) {
					throw new JavaCiaException("Multi-dimensional array in annotate is illegal!");
				}
				values.add((Annotate.NonArrayValueImpl) innerValue);
			}
			arrayValue.setValues(values);
			return arrayValue;
		}
		throw new JavaCiaException("Unknown annotate value!");
	}

	@Nonnull
	private Annotate internalCreateAnnotateFromAnnotationBinding(@Nonnull IAnnotationBinding annotationBinding,
			@Nonnull JavaDependency dependencyType, @Nullable Map<IBinding, int[]> dependencyMap)
			throws JavaCiaException {

		final Pair<Annotate, Map<IBinding, int[]>> pair = delayedAnnotations.get(annotationBinding);
		if (pair != null) {
			if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding annotationTypeBinding = annotationBinding.getAnnotationType();
		final Annotate annotate = new Annotate(annotationTypeBinding.getQualifiedName());
		delayedAnnotationNodes.computeIfAbsent(annotationTypeBinding, JavaSnapshotParser::createArrayList)
				.add(annotate);

		final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>();
		internalAddDependencyToDelayedDependencyMap(newDependencyMap,
				getOriginTypeBinding(annotationTypeBinding), dependencyType);
		delayedAnnotations.put(annotationBinding, Pair.immutableOf(annotate, newDependencyMap));

		final IMemberValuePairBinding[] pairBindings = annotationBinding.getDeclaredMemberValuePairs();
		final List<ParameterImpl> parameters = new ArrayList<>(pairBindings.length);
		for (final IMemberValuePairBinding pairBinding : pairBindings) {
			final IMethodBinding annotationMethodBinding = pairBinding.getMethodBinding();

			final ParameterImpl parameter = new ParameterImpl(pairBinding.getName());
			internalAddDependencyToDelayedDependencyMap(newDependencyMap,
					getOriginMethodBinding(annotationMethodBinding), dependencyType);
			delayedAnnotationParameters
					.computeIfAbsent(annotationMethodBinding, JavaSnapshotParser::createArrayList)
					.add(parameter);

			final Object value = pairBinding.getValue();
			if (value != null) {
				parameter.setValue(internalProcessAnnotateValue(value, dependencyType, newDependencyMap));
			}

			parameters.add(parameter);
		}
		annotate.setParameters(parameters);

		if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, newDependencyMap);
		return annotate;
	}

	@Nonnull
	private List<Annotate> createAnnotatesFromAnnotationBindings(@Nonnull IAnnotationBinding[] annotationBindings,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType)
			throws JavaCiaException {
		if (annotationBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final Map<IBinding, int[]> dependencyMap = new HashMap<>();
		final List<Annotate> annotates = new ArrayList<>(annotationBindings.length);
		for (final IAnnotationBinding annotationBinding : annotationBindings) {
			annotates.add(
					internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap));
		}
		createDelayDependencyFromDependencyMap(dependencySourceNode, dependencyMap);
		return annotates;
	}

	//endregion JavaAnnotate

	//region JavaType

	@Nonnull
	private List<AbstractType> internalCreateTypesFromTypeBindings(@Nonnull ITypeBinding[] typeBindings,
			@Nonnull JavaDependency dependencyType, @Nullable Map<IBinding, int[]> dependencyMap)
			throws JavaCiaException {
		final List<AbstractType> types = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			types.add(internalCreateTypeFromTypeBinding(typeBinding, dependencyType, dependencyMap));
		}
		return types;
	}

	@Nonnull
	private AbstractType internalCreateTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull JavaDependency dependencyType, @Nullable Map<IBinding, int[]> dependencyMap)
			throws JavaCiaException {
		final Pair<AbstractType, Map<IBinding, int[]>> pair = delayedTypes.get(typeBinding);
		if (pair != null) {
			if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding originTypeBinding = getOriginTypeBinding(typeBinding);
		final String typeBindingQualifiedName = typeBinding.getQualifiedName();
		final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>();
		if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
			final SyntheticType syntheticType = new SyntheticType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(syntheticType, newDependencyMap));
			internalAddDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			syntheticType.setAnnotates(internalCreateAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			if (typeBinding.isWildcardType()) {
				final ITypeBinding typeBindingBound = typeBinding.getBound();
				if (typeBindingBound != null) {
					syntheticType.setBounds(List.of(internalCreateTypeFromTypeBinding(typeBindingBound,
							dependencyType, newDependencyMap)));
				}
			} else {
				syntheticType.setBounds(internalCreateTypesFromTypeBindings(typeBinding.getTypeBounds(),
						dependencyType, newDependencyMap));
			}

			if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return syntheticType;

		} else if (typeBinding.isArray() || typeBinding.isPrimitive()) {
			final SimpleType simpleType = new SimpleType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(simpleType, newDependencyMap));
			internalAddDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			simpleType.setAnnotates(internalCreateAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			final ITypeBinding componentTypeBinding = typeBinding.getComponentType();
			if (componentTypeBinding != null) {
				simpleType.setInnerType(internalCreateTypeFromTypeBinding(componentTypeBinding,
						dependencyType, newDependencyMap));
			}

			if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return simpleType;

		} else {
			final ReferenceType referenceType = new ReferenceType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(referenceType, newDependencyMap));
			internalAddDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			referenceType.setAnnotates(internalCreateAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			referenceType.setArguments(internalCreateTypesFromTypeBindings(typeBinding.getTypeArguments(),
					dependencyType, newDependencyMap));

			delayedReferenceTypeNodes.computeIfAbsent(originTypeBinding, JavaSnapshotParser::createArrayList)
					.add(referenceType);

			if (dependencyMap != null) internalCombineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return referenceType;
		}
	}

	@Nonnull
	private AbstractType createUnprocessedTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull JavaDependency dependencyType) throws JavaCiaException {
		return internalCreateTypeFromTypeBinding(typeBinding, dependencyType, null);
	}

	private void processUnprocessedType(@Nonnull ITypeBinding typeBinding,
			@Nonnull AbstractNode dependencySourceNode) {
		final Pair<AbstractType, Map<IBinding, int[]>> pair = delayedTypes.get(typeBinding);
		assert pair != null : "typeBinding are not create yet!";
		createDelayDependencyFromDependencyMap(dependencySourceNode, pair.getB());
	}

	@Nonnull
	private List<AbstractType> createTypesFromTypeBindings(@Nonnull ITypeBinding[] typeBindings,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType) throws JavaCiaException {
		if (typeBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final ArrayList<AbstractType> arguments = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			arguments.add(createTypeFromTypeBinding(typeBinding, dependencySourceNode, dependencyType));
		}
		return arguments;
	}

	@Nonnull
	private AbstractType createTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType) throws JavaCiaException {
		final AbstractType type = createUnprocessedTypeFromTypeBinding(typeBinding, dependencyType);
		processUnprocessedType(typeBinding, dependencySourceNode);
		return type;
	}


	//endregion JavaType

	//region Package

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreateFirstLevelPackageFromName(
			@Nonnull String nameComponent) {
		final PackageNode packageNode = rootNode.createChildPackage(nameComponent);
		createDependencyToNode(rootNode, packageNode, JavaDependency.MEMBER);
		return Pair.mutableOf(packageNode, null);
	}

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreatePackagePairFromNameComponents(
			@Nonnull String[] nameComponents) {
		assert nameComponents.length > 0 : "nameComponents length should not be 0.";

		final String firstNameComponent = nameComponents[0];
		final StringBuilder qualifiedNameBuilder = new StringBuilder(firstNameComponent);
		Pair<PackageNode, IPackageBinding> pair = packageNodeMap.computeIfAbsent(firstNameComponent,
				this::internalCreateFirstLevelPackageFromName);

		for (int i = 1; i < nameComponents.length; i++) {
			final String nameComponent = nameComponents[i];
			qualifiedNameBuilder.append(".").append(nameComponent);
			final PackageNode parentNode = pair.getA();
			pair = packageNodeMap.computeIfAbsent(qualifiedNameBuilder.toString(),
					qualifiedName -> {
						final PackageNode packageNode = parentNode.createChildPackage(nameComponent);
						createDependencyToNode(parentNode, packageNode, JavaDependency.MEMBER);
						return Pair.mutableOf(packageNode, null);
					});
		}
		return pair;
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
			packageNode.setAnnotates(createAnnotatesFromAnnotationBindings(packageBinding.getAnnotations(),
					packageNode, JavaDependency.USE));
		}
		return packageNode;
	}

	//endregion Package

	//region Modifier

	private int processModifiersFromBindingModifiers(int bindingModifiers) {
		int modifiers = 0;
		if ((bindingModifiers & Modifier.PUBLIC) != 0) modifiers |= JavaModifier.PUBLIC_MASK;
		if ((bindingModifiers & Modifier.PRIVATE) != 0) modifiers |= JavaModifier.PRIVATE_MASK;
		if ((bindingModifiers & Modifier.PROTECTED) != 0) modifiers |= JavaModifier.PROTECTED_MASK;
		if ((bindingModifiers & Modifier.STATIC) != 0) modifiers |= JavaModifier.STATIC_MASK;
		if ((bindingModifiers & Modifier.SYNCHRONIZED) != 0) modifiers |= JavaModifier.SYNCHRONIZED_MASK;
		if ((bindingModifiers & Modifier.VOLATILE) != 0) modifiers |= JavaModifier.VOLATILE_MASK;
		if ((bindingModifiers & Modifier.TRANSIENT) != 0) modifiers |= JavaModifier.TRANSIENT_MASK;
		if ((bindingModifiers & Modifier.NATIVE) != 0) modifiers |= JavaModifier.NATIVE_MASK;
		if ((bindingModifiers & Modifier.STRICTFP) != 0) modifiers |= JavaModifier.STRICTFP_MASK;
		return modifiers;
	}

	//endregion Modifier

	//region Compilation Unit

	@Override
	public void acceptAST(@Nonnull String sourcePath, @Nonnull CompilationUnit compilationUnit) {
		if (exception != null) return;
		try {
			final PackageDeclaration packageDeclaration = compilationUnit.getPackage();
			if (packageDeclaration != null) {
				final PackageNode packageNode = createPackageNodeFromPackageDeclaration(packageDeclaration);
				for (Object type : compilationUnit.types()) {
					if (type instanceof AbstractTypeDeclaration) {
						parseAbstractTypeDeclaration(packageNode, (AbstractTypeDeclaration) type);
					}
				}
			} else {
				// the parent is root
				for (Object type : compilationUnit.types()) {
					if (type instanceof AbstractTypeDeclaration) {
						parseAbstractTypeDeclaration(rootNode, (AbstractTypeDeclaration) type);
					}
				}
			}
		} catch (JavaCiaException exception) {
			this.exception = exception;
		}
	}

	//endregion Compilation Unit

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
		assert parentNode instanceof JavaClassContainer : "Expected a JavaClassContainer parent.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final ClassNode classNode = parentNode.createChildClass(typeBinding.getName(), typeBinding.getBinaryName());
		createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		internalParseClassTypeBinding(classNode, typeBinding, typeDeclaration.bodyDeclarations());
	}

	private void internalParseClassTypeBinding(@Nonnull ClassNode classNode, @Nonnull ITypeBinding typeBinding,
			@Nonnull List<?> bodyDeclarations) throws JavaCiaException {
		// put binding map
		bindingNodeMap.put(typeBinding, classNode);

		// set annotate
		classNode.setAnnotates(createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				classNode, JavaDependency.USE));

		// set modifier
		classNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		classNode.setTypeParameters(createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				classNode, JavaDependency.USE));

		// set extends class
		final ITypeBinding superClassBinding = typeBinding.getSuperclass();
		if (superClassBinding != null) {
			classNode.setExtendsClass(createTypeFromTypeBinding(superClassBinding,
					classNode, JavaDependency.INHERITANCE));
		}

		// set implements interfaces
		classNode.setImplementsInterfaces(createTypesFromTypeBindings(typeBinding.getInterfaces(),
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
		assert parentNode instanceof JavaInterfaceContainer : "Expected a JavaInterfaceContainer parent.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on interface declaration!");

		final InterfaceNode interfaceNode = parentNode
				.createChildInterface(typeBinding.getName(), typeBinding.getBinaryName());
		createDependencyToNode(parentNode, interfaceNode, JavaDependency.MEMBER);

		// put binding map
		bindingNodeMap.put(typeBinding, interfaceNode);

		// set annotate
		interfaceNode.setAnnotates(createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				interfaceNode, JavaDependency.USE));

		// set modifier
		interfaceNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		interfaceNode.setTypeParameters(createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				interfaceNode, JavaDependency.USE));

		// set extends interfaces
		interfaceNode.setExtendsInterfaces(createTypesFromTypeBindings(typeBinding.getInterfaces(),
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
		assert parentNode instanceof JavaEnumContainer : "Expected a JavaEnumContainer parent.";

		final ITypeBinding typeBinding = enumDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on enum declaration!");

		final EnumNode enumNode = parentNode.createChildEnum(typeBinding.getName(), typeBinding.getBinaryName());
		createDependencyToNode(parentNode, enumNode, JavaDependency.MEMBER);

		// put binding map
		bindingNodeMap.put(typeBinding, enumNode);

		// set annotate
		enumNode.setAnnotates(createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				enumNode, JavaDependency.USE));

		// set modifier
		enumNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		enumNode.setTypeParameters(createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				enumNode, JavaDependency.USE));

		// set implements interfaces
		enumNode.setImplementsInterfaces(createTypesFromTypeBindings(typeBinding.getInterfaces(),
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
		assert parentNode instanceof JavaFieldContainer : "Expected a JavaFieldContainer parent.";

		final IVariableBinding variableBinding = enumConstantDeclaration.resolveVariable();
		if (variableBinding == null) throw new JavaCiaException("Cannot resolve binding on enum constant declaration!");

		final FieldNode fieldNode = parentNode.createChildField(variableBinding.getName());
		createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

		internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

		// put constructor invocation dependency
		final IMethodBinding constructorBinding = enumConstantDeclaration.resolveConstructorBinding();
		if (constructorBinding != null) {
			createDelayDependency(fieldNode, getOriginMethodBinding(constructorBinding), JavaDependency.INVOCATION);
		}

		// put delayed variable initializer
		for (final Object object : enumConstantDeclaration.arguments()) {
			if (object instanceof Expression) {
				final Expression expression = (Expression) object;
				delayedDependencyWalkers.add(Pair.immutableOf(expression, fieldNode));
				walkDeclaration(expression, fieldNode);
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
		fieldNode.setAnnotates(createAnnotatesFromAnnotationBindings(variableBinding.getAnnotations(),
				fieldNode, JavaDependency.USE));

		// set modifier
		fieldNode.setModifiers(processModifiersFromBindingModifiers(variableBinding.getModifiers()));

		// set type
		fieldNode.setType(createTypeFromTypeBinding(variableBinding.getType(), fieldNode, JavaDependency.USE));
	}

	private void parseAnonymousClassDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnonymousClassDeclaration anonymousClassDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaClassContainer : "Expected a JavaClassContainer parent.";

		final ITypeBinding typeBinding = anonymousClassDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final String binaryName = typeBinding.getBinaryName();
		final String className = binaryName.contains("$")
				? binaryName.substring(binaryName.lastIndexOf('$'))
				: binaryName.contains(".")
				? binaryName.substring(binaryName.lastIndexOf('.')).replace('.', '$')
				: binaryName;

		final ClassNode classNode = parentNode.createChildClass(className, binaryName);
		createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		internalParseClassTypeBinding(classNode, typeBinding, anonymousClassDeclaration.bodyDeclarations());
	}

	private void parseAnnotationTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeDeclaration annotationTypeDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaAnnotationContainer : "Expected a JavaAnnotationContainer parent.";

		final ITypeBinding annotationBinding = annotationTypeDeclaration.resolveBinding();
		if (annotationBinding == null) throw new JavaCiaException("Cannot resolve binding on annotation declaration!");

		final AnnotationNode annotationNode = parentNode
				.createChildAnnotation(annotationBinding.getName(), annotationBinding.getBinaryName());
		createDependencyToNode(parentNode, annotationNode, JavaDependency.MEMBER);

		// put binding map
		bindingNodeMap.put(annotationBinding, annotationNode);

		// set annotate
		annotationNode.setAnnotates(createAnnotatesFromAnnotationBindings(annotationBinding.getAnnotations(),
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
		assert parentNode instanceof JavaMethodContainer : "Expected a JavaMethodContainer parent.";

		// check null and resolve origin
		final IMethodBinding annotationMemberBinding = annotationMemberDeclaration.resolveBinding();
		if (annotationMemberBinding == null)
			throw new JavaCiaException("Cannot resolve binding on annotation type member declaration!");

		// create node and containment dependency
		final MethodNode methodNode = parentNode.createChildMethod(annotationMemberBinding.getName(), List.of());
		createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// put binding map
		bindingNodeMap.put(annotationMemberBinding, methodNode);

		// set annotate
		methodNode.setAnnotates(createAnnotatesFromAnnotationBindings(annotationMemberBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(annotationMemberBinding.getModifiers()));

		// set type
		final ITypeBinding typeBinding = annotationMemberBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// put delayed default value
		final Expression annotationMemberDeclarationDefault = annotationMemberDeclaration.getDefault();
		if (annotationMemberDeclarationDefault != null) {
			delayedDependencyWalkers.add(Pair.immutableOf(annotationMemberDeclarationDefault, methodNode));
			walkDeclaration(annotationMemberDeclarationDefault, methodNode);
		}
	}

	private void parseFieldDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull FieldDeclaration fieldDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaFieldContainer : "Expected a JavaFieldContainer parent.";

		for (Object fragment : fieldDeclaration.fragments()) {
			if (fragment instanceof VariableDeclaration) {
				final VariableDeclaration variableDeclaration = (VariableDeclaration) fragment;

				final IVariableBinding variableBinding = variableDeclaration.resolveBinding();
				if (variableBinding == null) {
					throw new JavaCiaException("Cannot resolve binding on variable declaration!");
				}

				final FieldNode fieldNode = parentNode.createChildField(variableBinding.getName());
				createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

				internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

				// put delayed variable initializer
				final Expression variableInitializer = variableDeclaration.getInitializer();
				if (variableInitializer != null) {
					final InitializerNode initializerNode
							= internalCreateOrGetInitializerNode(parentNode, fieldNode.isStatic());
					final List<InitializerNode.InitializerImpl> initializerList
							= initializerListMap.computeIfAbsent(initializerNode, JavaSnapshotParser::createArrayList);
					initializerList.add(new InitializerNode.FieldInitializerImpl(fieldNode,
							format(variableInitializer.toString(), CodeFormatter.K_EXPRESSION)));
					delayedDependencyWalkers.add(Pair.immutableOf(variableInitializer, initializerNode));
					walkDeclaration(variableInitializer, fieldNode);
				}
			}
		}
	}

	private void parseInitializer(@Nonnull AbstractNode parentNode, @Nonnull Initializer initializer)
			throws JavaCiaException {
		assert parentNode instanceof JavaInitializerContainer : "Expected a JavaInitializerContainer parent.";

		// create node and containment dependency
		final InitializerNode initializerNode = internalCreateOrGetInitializerNode(parentNode,
				(initializer.getModifiers() & Modifier.STATIC) != 0);
		createDependencyToNode(parentNode, initializerNode, JavaDependency.MEMBER);

		// put delayed initializer body
		final Block initializerBody = initializer.getBody();
		if (initializerBody != null) {
			final List<InitializerNode.InitializerImpl> initializerList
					= initializerListMap.computeIfAbsent(initializerNode, JavaSnapshotParser::createArrayList);
			initializerList.add(new InitializerNode.BlockInitializerImpl(
					format(initializerBody.toString(), CodeFormatter.K_STATEMENTS)));
			delayedDependencyWalkers.add(Pair.immutableOf(initializerBody, initializerNode));
			walkDeclaration(initializerBody, initializerNode);
		}
	}

	@Nonnull
	private InitializerNode internalCreateOrGetInitializerNode(@Nonnull AbstractNode parentNode, boolean isStatic) {
		final Pair<InitializerNode, InitializerNode> pair
				= classInitializerMap.computeIfAbsent(parentNode, JavaSnapshotParser::createMutablePair);
		return isStatic
				? pair.getA() != null ? pair.getA() : pair.setGetA(parentNode.createChildInitializer(true))
				: pair.getB() != null ? pair.getB() : pair.setGetB(parentNode.createChildInitializer(false));
	}

	private void parseMethodDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull MethodDeclaration methodDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaMethodContainer : "Expected a JavaMethodContainer parent.";

		// check null and resolve origin
		final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if (methodBinding == null) throw new JavaCiaException("Cannot resolve binding on method declaration!");

		// create parameter quick types
		final ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
		final List<AbstractType> parameterJavaTypes = new ArrayList<>(parameterTypeBindings.length);
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			parameterJavaTypes.add(createUnprocessedTypeFromTypeBinding(parameterTypeBinding, JavaDependency.USE));
		}

		// create node and containment dependency
		final MethodNode methodNode = parentNode.createChildMethod(methodBinding.getName(), parameterJavaTypes);
		createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// create parameter proper types
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			processUnprocessedType(parameterTypeBinding, methodNode);
		}

		// put binding map
		bindingNodeMap.put(methodBinding, methodNode);

		// put method binding map
		methodBindingMap.put(methodNode, methodBinding);

		// set annotate
		methodNode.setAnnotates(createAnnotatesFromAnnotationBindings(methodBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(methodBinding.getModifiers()));

		// set type parameter
		methodNode.setTypeParameters(createTypesFromTypeBindings(methodBinding.getTypeParameters(),
				methodNode, JavaDependency.USE));

		// set type
		final ITypeBinding typeBinding = methodBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// set exceptions
		methodNode.setExceptions(createTypesFromTypeBindings(methodBinding.getExceptionTypes(),
				methodNode, JavaDependency.USE));

		// put delayed method body
		final Block methodDeclarationBody = methodDeclaration.getBody();
		if (methodDeclarationBody != null) {
			methodNode.setBodyBlock(format(methodDeclarationBody.toString(), CodeFormatter.K_STATEMENTS));
			delayedDependencyWalkers.add(Pair.immutableOf(methodDeclarationBody, methodNode));
			walkDeclaration(methodDeclarationBody, methodNode);
		}
	}

	//endregion Parser

	//region Walker

	private void walkDeclaration(@Nonnull ASTNode astNode, @Nonnull AbstractNode javaNode) throws JavaCiaException {
		final JavaCiaException[] exceptionProxy = new JavaCiaException[]{null};
		astNode.accept(new ASTVisitor() {

			@Override
			public boolean preVisit2(@Nonnull ASTNode node) {
				return exceptionProxy[0] == null;
			}

			@Override
			public boolean visit(@Nonnull TypeDeclaration node) {
				try {
					parseTypeDeclaration(javaNode, node);
				} catch (JavaCiaException exception) {
					exceptionProxy[0] = exception;
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull AnonymousClassDeclaration node) {
				try {
					parseAnonymousClassDeclaration(javaNode, node);
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
				if (binding == null) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on constructor invocation!");
					return false;
				}
				return createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
			}

			@Override
			public boolean visit(@Nonnull ConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding == null) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on constructor invocation!");
					return false;
				}
				return createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
			}

			@Override
			public boolean visit(@Nonnull SuperMethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding == null) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on method invocation!");
					return false;
				}
				return createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
			}

			@Override
			public boolean visit(@Nonnull MethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding == null) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on method invocation!");
					return false;
				}
				return createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
			}

			private boolean createDependencyFromInvocation(@Nonnull IMethodBinding binding,
					@Nonnull List<?> typeArguments, @Nonnull List<?> arguments) {
				createDelayDependency(javaNode, getOriginMethodBinding(binding), JavaDependency.INVOCATION);
				for (final Object object : typeArguments) {
					if (object instanceof Type) ((Type) object).accept(this);
				}
				for (final Object object : arguments) {
					if (object instanceof Expression) ((Expression) object).accept(this);
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull SimpleName node) {
				final IBinding binding = node.resolveBinding();
				if (binding == null) {
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
