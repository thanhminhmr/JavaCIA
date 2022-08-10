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
import io.github.thanhminhmr.javacia.jdt.tree.annotate.Annotate;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.tree.annotate.JavaAnnotate;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import io.github.thanhminhmr.javacia.utils.Pair;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class JavaAnnotateBuilder {

	private final @NotNull JavaNodeBuilder nodes;

	private final @NotNull Map<@NotNull IAnnotationBinding, @NotNull Pair<@NotNull Annotate, @NotNull Map<@NotNull IBinding, int @NotNull []>>> delayedAnnotations = new HashMap<>();
	private final @NotNull Map<@NotNull ITypeBinding, @NotNull List<@NotNull Annotate>> delayedAnnotationNodes = new HashMap<>();
	private final @NotNull Map<@NotNull IMethodBinding, @NotNull List<Annotate.@NotNull ParameterImpl>> delayedAnnotationParameters = new HashMap<>();
	private final @NotNull Map<@NotNull IBinding, @NotNull List<Annotate.@NotNull NodeValueImpl>> delayedAnnotationNodeValues = new HashMap<>();


	JavaAnnotateBuilder(@NotNull JavaNodeBuilder nodes) {
		this.nodes = nodes;
	}


	void postprocessing(@NotNull Map<@NotNull IBinding, @NotNull AbstractNode> bindingNodeMap) {
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
		for (final Map.Entry<IMethodBinding, List<Annotate.ParameterImpl>> entry : delayedAnnotationParameters.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final Annotate.ParameterImpl parameter : entry.getValue()) parameter.setNode(node);
			}
		}
		delayedAnnotationParameters.clear();

		// delay annotate values
		for (final Map.Entry<IBinding, List<Annotate.NodeValueImpl>> entry : delayedAnnotationNodeValues.entrySet()) {
			final AbstractNode node = bindingNodeMap.get(entry.getKey());
			if (node != null) {
				for (final Annotate.NodeValueImpl value : entry.getValue()) value.setNode(node);
			}
		}
		delayedAnnotationNodeValues.clear();
	}

	//region JavaAnnotate

	@NotNull List<@NotNull Annotate> createAnnotatesFromAnnotationBindings(
			@NotNull IAnnotationBinding @NotNull [] annotationBindings, @NotNull JavaDependency dependencyType,
			@Nullable Map<@NotNull IBinding, int @NotNull []> dependencyMap) throws JavaCiaException {
		if (annotationBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final List<Annotate> annotates = new ArrayList<>(annotationBindings.length);
		for (final IAnnotationBinding annotationBinding : annotationBindings) {
			annotates.add(
					internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap));
		}
		return annotates;
	}

	private Annotate.@NotNull ValueImpl internalProcessAnnotateValue(@NotNull Object value,
			@NotNull JavaDependency dependencyType, @NotNull Map<@NotNull IBinding, int @NotNull []> dependencyMap)
			throws JavaCiaException {

		if (JavaAnnotate.SimpleValue.isValidValueType(value)) {
			return new Annotate.SimpleValueImpl(value);

		} else if (value instanceof ITypeBinding) {
			final ITypeBinding typeBinding = (ITypeBinding) value;
			final Annotate.NodeValueImpl nodeValue =
					new Annotate.NodeValueImpl("Class<" + typeBinding.getQualifiedName() + ">");
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(dependencyMap,
					JavaNodeBuilder.getOriginTypeBinding(typeBinding), dependencyType);
			delayedAnnotationNodeValues.computeIfAbsent(typeBinding, JavaParser::createArrayList)
					.add(nodeValue);
			return nodeValue;

		} else if (value instanceof IVariableBinding) {
			final IVariableBinding variableBinding = (IVariableBinding) value;
			final Annotate.NodeValueImpl nodeValue = new Annotate.NodeValueImpl(
					variableBinding.getDeclaringClass().getQualifiedName() + '.' + variableBinding.getName());
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(dependencyMap,
					JavaNodeBuilder.getOriginVariableBinding(variableBinding), dependencyType);
			delayedAnnotationNodeValues.computeIfAbsent(variableBinding, JavaParser::createArrayList)
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

	private @NotNull Annotate internalCreateAnnotateFromAnnotationBinding(@NotNull IAnnotationBinding annotationBinding,
			@NotNull JavaDependency dependencyType, @Nullable Map<@NotNull IBinding, int @NotNull []> dependencyMap)
			throws JavaCiaException {

		final Pair<Annotate, Map<IBinding, int[]>> pair = delayedAnnotations.get(annotationBinding);
		if (pair != null) {
			if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding annotationTypeBinding = annotationBinding.getAnnotationType();
		final Annotate annotate = new Annotate(annotationTypeBinding.getQualifiedName());
		delayedAnnotationNodes.computeIfAbsent(annotationTypeBinding, JavaParser::createArrayList)
				.add(annotate);

		final Map<IBinding, int[]> newDependencyMap = new LinkedHashMap<>();
		JavaNodeBuilder.addDependencyToDelayedDependencyMap(newDependencyMap,
				JavaNodeBuilder.getOriginTypeBinding(annotationTypeBinding), dependencyType);
		delayedAnnotations.put(annotationBinding, new Pair<>(annotate, newDependencyMap));

		final IMemberValuePairBinding[] pairBindings = annotationBinding.getDeclaredMemberValuePairs();
		final List<Annotate.ParameterImpl> parameters = new ArrayList<>(pairBindings.length);
		for (final IMemberValuePairBinding pairBinding : pairBindings) {
			final IMethodBinding annotationMethodBinding = pairBinding.getMethodBinding();

			final Annotate.ParameterImpl parameter = new Annotate.ParameterImpl(pairBinding.getName());
			JavaNodeBuilder.addDependencyToDelayedDependencyMap(newDependencyMap,
					JavaNodeBuilder.getOriginMethodBinding(annotationMethodBinding), dependencyType);
			delayedAnnotationParameters
					.computeIfAbsent(annotationMethodBinding, JavaParser::createArrayList)
					.add(parameter);

			final Object value = pairBinding.getValue();
			if (value != null) {
				parameter.setValue(internalProcessAnnotateValue(value, dependencyType, newDependencyMap));
			}

			parameters.add(parameter);
		}
		annotate.setParameters(parameters);

		if (dependencyMap != null) JavaNodeBuilder.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
		return annotate;
	}

	@NotNull
	List<Annotate> createAnnotatesFromAnnotationBindings(@NotNull IAnnotationBinding @NotNull [] annotationBindings,
			@NotNull AbstractNode dependencySourceNode, @NotNull JavaDependency dependencyType)
			throws JavaCiaException {
		if (annotationBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final Map<IBinding, int[]> dependencyMap = new LinkedHashMap<>();
		final List<Annotate> annotates = new ArrayList<>(annotationBindings.length);
		for (final IAnnotationBinding annotationBinding : annotationBindings) {
			annotates.add(
					internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap));
		}
		nodes.createDelayDependencyFromDependencyMap(dependencySourceNode, dependencyMap);
		return annotates;
	}

	//endregion JavaAnnotate

}
