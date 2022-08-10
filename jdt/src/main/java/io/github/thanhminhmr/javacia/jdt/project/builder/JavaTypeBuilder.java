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

import io.github.thanhminhmr.javacia.JavaCiaException;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.jdt.tree.type.AbstractType;
import io.github.thanhminhmr.javacia.jdt.tree.type.ReferenceType;
import io.github.thanhminhmr.javacia.jdt.tree.type.SimpleType;
import io.github.thanhminhmr.javacia.jdt.tree.type.SyntheticType;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import io.github.thanhminhmr.javacia.utils.Pair;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaTypeBuilder {

	private final @NotNull JavaNodeBuilder nodes;
	private final @NotNull JavaAnnotateBuilder annotates;

	private final @NotNull Map<@NotNull ITypeBinding, @NotNull Pair<@NotNull AbstractType, @NotNull Map<@NotNull IBinding, int @NotNull []>>> delayedTypes = new HashMap<>();
	private final @NotNull Map<@NotNull ITypeBinding, @NotNull List<@NotNull ReferenceType>> delayedReferenceTypeNodes = new HashMap<>();


	JavaTypeBuilder(@NotNull JavaNodeBuilder nodes, @NotNull JavaAnnotateBuilder annotates) {
		this.nodes = nodes;
		this.annotates = annotates;
	}


	void postprocessing(@NotNull Map<@NotNull IBinding, @NotNull AbstractNode> bindingNodeMap) {
		// delay reference type node
		delayedTypes.clear();

		for (final Map.Entry<ITypeBinding, List<ReferenceType>> entry : delayedReferenceTypeNodes.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final ReferenceType referenceType : entry.getValue()) {
					referenceType.setNode(node);
				}
			}
		}
		delayedReferenceTypeNodes.clear();
	}


	private @NotNull List<@NotNull AbstractType> internalCreateTypesFromTypeBindings(
			@NotNull ITypeBinding @NotNull [] typeBindings, @NotNull JavaDependency dependencyType,
			@Nullable Map<@NotNull IBinding, int @NotNull []> dependencyMap) throws JavaCiaException {
		final List<AbstractType> types = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			types.add(internalCreateTypeFromTypeBinding(typeBinding, dependencyType, dependencyMap));
		}
		return types;
	}

	private @NotNull AbstractType internalCreateTypeFromTypeBinding(@NotNull ITypeBinding typeBinding,
			@NotNull JavaDependency dependencyType, @Nullable Map<@NotNull IBinding, int @NotNull []> dependencyMap)
			throws JavaCiaException {
		final Pair<AbstractType, Map<IBinding, int[]>> pair = delayedTypes.get(typeBinding);
		if (pair != null) {
			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding originTypeBinding = JavaNodeBuilder.getOriginTypeBinding(typeBinding);
		final String typeBindingQualifiedName = typeBinding.getQualifiedName();
		final Map<IBinding, int[]> newDependencyMap = new LinkedHashMap<>();
		if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
			final SyntheticType syntheticType = new SyntheticType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, new Pair<>(syntheticType, newDependencyMap));
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			syntheticType.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
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

			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return syntheticType;

		} else if (typeBinding.isArray() || typeBinding.isPrimitive()) {
			final SimpleType simpleType = new SimpleType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, new Pair<>(simpleType, newDependencyMap));
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			simpleType.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			final ITypeBinding componentTypeBinding = typeBinding.getComponentType();
			if (componentTypeBinding != null) {
				simpleType.setInnerType(internalCreateTypeFromTypeBinding(componentTypeBinding,
						dependencyType, newDependencyMap));
			}

			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return simpleType;

		} else {
			final ReferenceType referenceType = new ReferenceType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, new Pair<>(referenceType, newDependencyMap));
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			referenceType.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			referenceType.setArguments(internalCreateTypesFromTypeBindings(typeBinding.getTypeArguments(),
					dependencyType, newDependencyMap));

			delayedReferenceTypeNodes.computeIfAbsent(originTypeBinding, JavaParser::createArrayList)
					.add(referenceType);

			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return referenceType;
		}
	}

	@NotNull AbstractType createUnprocessedTypeFromTypeBinding(@NotNull ITypeBinding typeBinding,
			@NotNull JavaDependency dependencyType) throws JavaCiaException {
		return internalCreateTypeFromTypeBinding(typeBinding, dependencyType, null);
	}

	void processUnprocessedType(@NotNull ITypeBinding typeBinding, @NotNull AbstractNode dependencySourceNode) {
		final Pair<AbstractType, Map<IBinding, int[]>> pair = delayedTypes.get(typeBinding);
		assert pair != null : "typeBinding are not create yet!";
		nodes.createDelayDependencyFromDependencyMap(dependencySourceNode, pair.getB());
	}

	@NotNull List<@NotNull AbstractType> createTypesFromTypeBindings(@NotNull ITypeBinding @NotNull [] typeBindings,
			@NotNull AbstractNode dependencySourceNode, @NotNull JavaDependency dependencyType)
			throws JavaCiaException {
		if (typeBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final ArrayList<AbstractType> arguments = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			arguments.add(createTypeFromTypeBinding(typeBinding, dependencySourceNode, dependencyType));
		}
		return arguments;
	}

	@NotNull AbstractType createTypeFromTypeBinding(@NotNull ITypeBinding typeBinding,
			@NotNull AbstractNode dependencySourceNode, @NotNull JavaDependency dependencyType)
			throws JavaCiaException {
		final AbstractType type = createUnprocessedTypeFromTypeBinding(typeBinding, dependencyType);
		processUnprocessedType(typeBinding, dependencySourceNode);
		return type;
	}

}
