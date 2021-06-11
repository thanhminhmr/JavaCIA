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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JavaOverrideBuilder {
	@Nonnull private final JavaParser parser;

	// contains a map between method and its binding
	@Nonnull private final Map<MethodNode, IMethodBinding> methodBindingMap = new IdentityHashMap<>();
	// contains a set of node that already been processed
	@Nonnull private final Set<AbstractNode> overrideProcessedNodes = new HashSet<>();
	// contains all child method nodes of a node
	@Nonnull private final Map<AbstractNode, List<MethodNode>> childMethodsMap = new IdentityHashMap<>();
	// contains all methods that current method override
	@Nonnull private final Map<MethodNode, List<MethodNode>> methodOverridesMap = new IdentityHashMap<>();


	JavaOverrideBuilder(@Nonnull JavaParser parser) {
		this.parser = parser;
	}


	void processOverrides(@Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
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
				processOverrideInInterface((InterfaceNode) node);
			} else if (node instanceof ClassNode) {
				processOverrideInClass((ClassNode) node);
			} else if (node instanceof EnumNode) {
				processOverrideInEnum((EnumNode) node);
			}
		}

		methodBindingMap.clear();
		overrideProcessedNodes.clear();
		childMethodsMap.clear();
		methodOverridesMap.clear();
	}

	private void processOverrideInInterface(@Nonnull InterfaceNode interfaceNode) {
		if (!overrideProcessedNodes.add(interfaceNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= processOverrideInterfaceTypes(interfaceNode.getExtendsInterfaces());
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : childMethodsMap
					.computeIfAbsent(interfaceNode, JavaOverrideBuilder::getChildMethodsFromNode)) {
				processOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	private void processOverrideInClass(@Nonnull ClassNode classNode) {
		if (!overrideProcessedNodes.add(classNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= processOverrideInterfaceTypes(classNode.getImplementsInterfaces());
		final AbstractType extendsClass = classNode.getExtendsClass();
		if (extendsClass instanceof ReferenceType) {
			final AbstractNode extendsClassNode = ((ReferenceType) extendsClass).getNode();
			if (extendsClassNode instanceof ClassNode) {
				processOverrideInClass((ClassNode) extendsClassNode);
				final List<MethodNode> parentMethodNodes = childMethodsMap
						.computeIfAbsent(extendsClassNode, JavaOverrideBuilder::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			} else if (extendsClassNode instanceof EnumNode) {
				processOverrideInEnum((EnumNode) extendsClassNode);
				final List<MethodNode> parentMethodNodes = childMethodsMap
						.computeIfAbsent(extendsClassNode, JavaOverrideBuilder::getChildMethodsFromNode);
				if (!parentMethodNodes.isEmpty()) parentMethodsList.add(parentMethodNodes);
			}
		}
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : childMethodsMap
					.computeIfAbsent(classNode, JavaOverrideBuilder::getChildMethodsFromNode)) {
				processOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	private void processOverrideInEnum(@Nonnull EnumNode enumNode) {
		if (!overrideProcessedNodes.add(enumNode)) return;
		final List<List<MethodNode>> parentMethodsList
				= processOverrideInterfaceTypes(enumNode.getImplementsInterfaces());
		if (!parentMethodsList.isEmpty()) {
			for (final MethodNode childMethod : childMethodsMap
					.computeIfAbsent(enumNode, JavaOverrideBuilder::getChildMethodsFromNode)) {
				processOverrideParentMethodsList(childMethod, parentMethodsList);
			}
		}
	}

	@Nonnull
	private List<List<MethodNode>> processOverrideInterfaceTypes(@Nonnull List<AbstractType> interfaceTypes) {
		final List<List<MethodNode>> parentMethodsList = new ArrayList<>();
		for (final AbstractType interfaceType : interfaceTypes) {
			if (!(interfaceType instanceof ReferenceType)) continue;
			final AbstractNode interfaceNode = ((ReferenceType) interfaceType).getNode();
			if (!(interfaceNode instanceof InterfaceNode)) continue;
			processOverrideInInterface((InterfaceNode) interfaceNode);
			final List<MethodNode> methodNodes = childMethodsMap
					.computeIfAbsent(interfaceNode, JavaOverrideBuilder::getChildMethodsFromNode);
			if (!methodNodes.isEmpty()) parentMethodsList.add(methodNodes);
		}
		return parentMethodsList;
	}

	// Check if child method override any methods in the list of all methods that current method container
	private void processOverrideParentMethodsList(@Nonnull MethodNode childMethod,
			@Nonnull List<List<MethodNode>> parentMethodsList) {
		final List<MethodNode> childMethodOverrides = methodOverridesMap
				.computeIfAbsent(childMethod, JavaParser::createArrayList);
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
						childMethodOverrides.addAll(methodOverridesMap
								.computeIfAbsent(parentMethod, JavaParser::createArrayList));
						break;
					}
				}
			}
		}
		for (final MethodNode childMethodOverride : childMethodOverrides) {
			parser.createDependencyToNode(childMethod, childMethodOverride, JavaDependency.OVERRIDE);
		}
	}

	@Nonnull
	private static List<MethodNode> getChildMethodsFromNode(@Nonnull AbstractNode parentNode) {
		final List<AbstractNode> children = parentNode.getChildren();
		final List<MethodNode> methods = new ArrayList<>(children.size());
		for (final AbstractNode childNode : children) {
			if (childNode instanceof MethodNode) methods.add((MethodNode) childNode);
		}
		return methods;
	}

}
