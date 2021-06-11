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
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.jdt.tree.type.ReferenceType;
import mrmathami.cia.java.jdt.tree.type.SimpleType;
import mrmathami.cia.java.jdt.tree.type.SyntheticType;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.utils.Pair;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaTypeBuilder {

	@Nonnull private final JavaNodeBuilder nodes;
	@Nonnull private final JavaAnnotateBuilder annotates;

	@Nonnull private final Map<ITypeBinding, Pair<AbstractType, Map<IBinding, int[]>>> delayedTypes = new HashMap<>();
	@Nonnull private final Map<ITypeBinding, List<ReferenceType>> delayedReferenceTypeNodes = new HashMap<>();


	JavaTypeBuilder(@Nonnull JavaNodeBuilder nodes, @Nonnull JavaAnnotateBuilder annotates) {
		this.nodes = nodes;
		this.annotates = annotates;
	}


	void postprocessing(@Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
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
			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding originTypeBinding = JavaNodeBuilder.getOriginTypeBinding(typeBinding);
		final String typeBindingQualifiedName = typeBinding.getQualifiedName();
		final Map<IBinding, int[]> newDependencyMap = new LinkedHashMap<>();
		if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
			final SyntheticType syntheticType = new SyntheticType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(syntheticType, newDependencyMap));
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
			delayedTypes.put(typeBinding, Pair.immutableOf(simpleType, newDependencyMap));
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
			delayedTypes.put(typeBinding, Pair.immutableOf(referenceType, newDependencyMap));
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

	@Nonnull
	AbstractType createUnprocessedTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull JavaDependency dependencyType) throws JavaCiaException {
		return internalCreateTypeFromTypeBinding(typeBinding, dependencyType, null);
	}

	void processUnprocessedType(@Nonnull ITypeBinding typeBinding, @Nonnull AbstractNode dependencySourceNode) {
		final Pair<AbstractType, Map<IBinding, int[]>> pair = delayedTypes.get(typeBinding);
		assert pair != null : "typeBinding are not create yet!";
		nodes.createDelayDependencyFromDependencyMap(dependencySourceNode, pair.getB());
	}

	@Nonnull
	List<AbstractType> createTypesFromTypeBindings(@Nonnull ITypeBinding[] typeBindings,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType)
			throws JavaCiaException {
		if (typeBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final ArrayList<AbstractType> arguments = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			arguments.add(createTypeFromTypeBinding(typeBinding, dependencySourceNode, dependencyType));
		}
		return arguments;
	}

	@Nonnull
	AbstractType createTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType)
			throws JavaCiaException {
		final AbstractType type = createUnprocessedTypeFromTypeBinding(typeBinding, dependencyType);
		processUnprocessedType(typeBinding, dependencySourceNode);
		return type;
	}

}
