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

package io.github.thanhminhmr.javacia.jdt.project.builder;

import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.ClassNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.EnumNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.InterfaceNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.MethodNode;
import io.github.thanhminhmr.javacia.jdt.tree.type.AbstractType;
import io.github.thanhminhmr.javacia.jdt.tree.type.ReferenceType;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JavaOverrideBuilder {
	private final @NotNull JavaParser parser;

	// contains a map between method and its binding
	private final @NotNull Map<@NotNull MethodNode, @NotNull IMethodBinding> methodBindingMap = new IdentityHashMap<>();
	// contains a set of node that already been processed
	private final @NotNull Set<@NotNull AbstractNode> overrideProcessedNodes = new HashSet<>();
	// contains all child method nodes of a node
	private final @NotNull Map<@NotNull AbstractNode, @NotNull List<@NotNull MethodNode>> childMethodsMap = new IdentityHashMap<>();
	// contains all methods that current method override
	private final @NotNull Map<@NotNull MethodNode, @NotNull List<@NotNull MethodNode>> methodOverridesMap = new IdentityHashMap<>();


	JavaOverrideBuilder(@NotNull JavaParser parser) {
		this.parser = parser;
	}


	void processOverrides(@NotNull Map<@NotNull IBinding, @NotNull AbstractNode> bindingNodeMap) {
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

	private void processOverrideInInterface(@NotNull InterfaceNode interfaceNode) {
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

	private void processOverrideInClass(@NotNull ClassNode classNode) {
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

	private void processOverrideInEnum(@NotNull EnumNode enumNode) {
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

	private @NotNull List<@NotNull List<@NotNull MethodNode>> processOverrideInterfaceTypes(
			@NotNull List<@NotNull AbstractType> interfaceTypes) {
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
	private void processOverrideParentMethodsList(@NotNull MethodNode childMethod,
			@NotNull List<@NotNull List<@NotNull MethodNode>> parentMethodsList) {
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

	private static @NotNull List<@NotNull MethodNode> getChildMethodsFromNode(@NotNull AbstractNode parentNode) {
		final List<AbstractNode> children = parentNode.getChildren();
		final List<MethodNode> methods = new ArrayList<>(children.size());
		for (final AbstractNode childNode : children) {
			if (childNode instanceof MethodNode) methods.add((MethodNode) childNode);
		}
		return methods;
	}

}
