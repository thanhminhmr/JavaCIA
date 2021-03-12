package mrmathami.cia.java.jdt.project.builder;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.tree.annotate.Annotate;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.utils.Pair;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JavaAnnotates {

	@Nonnull private final JavaDependencies dependencies;

	@Nonnull private final Map<IAnnotationBinding, Pair<Annotate, Map<IBinding, int[]>>> delayedAnnotations = new HashMap<>();
	@Nonnull private final Map<ITypeBinding, List<Annotate>> delayedAnnotationNodes = new HashMap<>();
	@Nonnull private final Map<IMethodBinding, List<Annotate.ParameterImpl>> delayedAnnotationParameters = new HashMap<>();
	@Nonnull private final Map<IBinding, List<Annotate.NodeValueImpl>> delayedAnnotationNodeValues = new HashMap<>();


	JavaAnnotates(@Nonnull JavaDependencies dependencies) {
		this.dependencies = dependencies;
	}


	void postprocessing(@Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
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

	@Nonnull
	List<Annotate> createAnnotatesFromAnnotationBindings(
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
			final Annotate.NodeValueImpl nodeValue =
					new Annotate.NodeValueImpl("Class<" + typeBinding.getQualifiedName() + ">");
			JavaDependencies.addDependencyToDelayedDependencyMap(dependencyMap,
					JavaDependencies.getOriginTypeBinding(typeBinding), dependencyType);
			delayedAnnotationNodeValues.computeIfAbsent(typeBinding, JavaSnapshotParser::createArrayList)
					.add(nodeValue);
			return nodeValue;

		} else if (value instanceof IVariableBinding) {
			final IVariableBinding variableBinding = (IVariableBinding) value;
			final Annotate.NodeValueImpl nodeValue = new Annotate.NodeValueImpl(
					variableBinding.getDeclaringClass().getQualifiedName() + '.' + variableBinding.getName());
			JavaDependencies.addDependencyToDelayedDependencyMap(dependencyMap,
					JavaDependencies.getOriginVariableBinding(variableBinding), dependencyType);
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
			if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding annotationTypeBinding = annotationBinding.getAnnotationType();
		final Annotate annotate = new Annotate(annotationTypeBinding.getQualifiedName());
		delayedAnnotationNodes.computeIfAbsent(annotationTypeBinding, JavaSnapshotParser::createArrayList)
				.add(annotate);

		final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>();
		JavaDependencies.addDependencyToDelayedDependencyMap(newDependencyMap,
				JavaDependencies.getOriginTypeBinding(annotationTypeBinding), dependencyType);
		delayedAnnotations.put(annotationBinding, Pair.immutableOf(annotate, newDependencyMap));

		final IMemberValuePairBinding[] pairBindings = annotationBinding.getDeclaredMemberValuePairs();
		final List<Annotate.ParameterImpl> parameters = new ArrayList<>(pairBindings.length);
		for (final IMemberValuePairBinding pairBinding : pairBindings) {
			final IMethodBinding annotationMethodBinding = pairBinding.getMethodBinding();

			final Annotate.ParameterImpl parameter = new Annotate.ParameterImpl(pairBinding.getName());
			JavaDependencies.addDependencyToDelayedDependencyMap(newDependencyMap,
					JavaDependencies.getOriginMethodBinding(annotationMethodBinding), dependencyType);
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

		if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
		return annotate;
	}

	@Nonnull
	List<Annotate>  createAnnotatesFromAnnotationBindings(@Nonnull IAnnotationBinding[] annotationBindings,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType)
			throws JavaCiaException {
		if (annotationBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final Map<IBinding, int[]> dependencyMap = new HashMap<>();
		final List<Annotate> annotates = new ArrayList<>(annotationBindings.length);
		for (final IAnnotationBinding annotationBinding : annotationBindings) {
			annotates.add(
					internalCreateAnnotateFromAnnotationBinding(annotationBinding, dependencyType, dependencyMap));
		}
		dependencies.createDelayDependencyFromDependencyMap(dependencySourceNode, dependencyMap);
		return annotates;
	}

	//endregion JavaAnnotate

}
