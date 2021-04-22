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
import mrmathami.cia.java.jdt.tree.dependency.DependencyCountTable;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.node.ClassNode;
import mrmathami.cia.java.jdt.tree.node.EnumNode;
import mrmathami.cia.java.jdt.tree.node.InterfaceNode;
import mrmathami.cia.java.jdt.tree.node.MethodNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.jdt.tree.type.ReferenceType;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class JavaDependencies {

	@Nonnull private final Map<AbstractNode, Map<AbstractNode, int[]>> nodeDependencies = new IdentityHashMap<>();
	@Nonnull private final Map<AbstractNode, Map<IBinding, int[]>> delayedDependencies = new IdentityHashMap<>();

	@Nonnull private final Map<MethodNode, IMethodBinding> methodBindingMap = new IdentityHashMap<>();
	@Nonnull private final Set<AbstractNode> delayedOverrideProcessedNode = new HashSet<>();
	@Nonnull private final Map<AbstractNode, List<MethodNode>> delayedOverrideChildMethodsMap = new IdentityHashMap<>();
	@Nonnull private final Map<MethodNode, List<MethodNode>> delayedMethodOverridesMap = new IdentityHashMap<>();


	void postProcessing(@Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
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

		for (final Map.Entry<IBinding, AbstractNode> entry : bindingNodeMap.entrySet()) {
			final AbstractNode node = entry.getValue();
			if (node instanceof MethodNode) {
				final IBinding binding = entry.getKey();
				assert binding instanceof IMethodBinding
						: "A method node should always have an associate method binding!";
				methodBindingMap.put((MethodNode) node, (IMethodBinding) binding);
			}
		}

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

	}

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
					.computeIfAbsent(interfaceNode, JavaDependencies::getChildMethodsFromNode)) {
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
						.computeIfAbsent(extendsClassNode, JavaDependencies::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			} else if (extendsClassNode instanceof EnumNode) {
				internalPostProcessingOverrideEnum((EnumNode) extendsClassNode);
				final List<MethodNode> parentMethodNodes = delayedOverrideChildMethodsMap
						.computeIfAbsent(extendsClassNode, JavaDependencies::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			}
		}
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : delayedOverrideChildMethodsMap
					.computeIfAbsent(classNode, JavaDependencies::getChildMethodsFromNode)) {
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
					.computeIfAbsent(enumNode, JavaDependencies::getChildMethodsFromNode)) {
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
					.computeIfAbsent(interfaceNode, JavaDependencies::getChildMethodsFromNode);
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
			createDependencyToNode(childMethod, childMethodOverride, JavaDependency.OVERRIDE);
		}
	}

	//endregion Post Processing Overrides

	//region Dependency

	@Nonnull
	private static <R> int[] internalCreateDependencyCounts(@Nullable R any) {
		return new int[JavaDependency.VALUE_LIST.size()];
	}

	//region Main Dependency

	@Nonnull
	private int[] internalGetOrCreateDependencyCounts(@Nonnull AbstractNode sourceNode, @Nonnull AbstractNode targetNode) {
		return nodeDependencies
				.computeIfAbsent(sourceNode, JavaSnapshotParser::createIdentityHashMap)
				.computeIfAbsent(targetNode, JavaDependencies::internalCreateDependencyCounts);
	}

	void createDependenciesToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull int[] dependencyCounts) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		final int length = currentCounts.length;
		for (int i = 0; i < length; i++) currentCounts[i] += dependencyCounts[i];
	}

	void createDependencyToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull JavaDependency dependencyType) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		currentCounts[dependencyType.ordinal()] += 1;
	}

	//endregion Main Dependency

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
				.computeIfAbsent(targetBinding, JavaDependencies::internalCreateDependencyCounts);
		counts[dependencyType.ordinal()] += 1;
	}

	void createDelayDependencyFromDependencyMap(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull Map<IBinding, int[]> dependencyMap) {
		final Map<IBinding, int[]> oldDependencyMap = delayedDependencies.get(dependencySourceNode);
		if (oldDependencyMap == null) {
			final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>(dependencyMap);
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
				delayedDependencies.computeIfAbsent(dependencySourceNode, JavaSnapshotParser::createHashMap),
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

	//endregion Dependency

}
