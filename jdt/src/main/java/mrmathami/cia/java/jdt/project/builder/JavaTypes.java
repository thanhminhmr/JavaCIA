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
import java.util.List;
import java.util.Map;

final class JavaTypes {

	@Nonnull private final JavaDependencies dependencies;
	@Nonnull private final JavaAnnotates annotates;


	@Nonnull private final Map<ITypeBinding, Pair<AbstractType, Map<IBinding, int[]>>> delayedTypes = new HashMap<>();
	@Nonnull private final Map<ITypeBinding, List<ReferenceType>> delayedReferenceTypeNodes = new HashMap<>();


	JavaTypes(@Nonnull JavaDependencies dependencies, @Nonnull JavaAnnotates annotates) {
		this.dependencies = dependencies;
		this.annotates = annotates;
	}


	void postprocessing(@Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
		// =====
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
			if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, pair.getB());
			return pair.getA();
		}

		final ITypeBinding originTypeBinding = JavaDependencies.getOriginTypeBinding(typeBinding);
		final String typeBindingQualifiedName = typeBinding.getQualifiedName();
		final HashMap<IBinding, int[]> newDependencyMap = new HashMap<>();
		if (typeBinding.isTypeVariable() || typeBinding.isCapture() || typeBinding.isWildcardType()) {
			final SyntheticType syntheticType = new SyntheticType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(syntheticType, newDependencyMap));
			JavaDependencies.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

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

			if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return syntheticType;

		} else if (typeBinding.isArray() || typeBinding.isPrimitive()) {
			final SimpleType simpleType = new SimpleType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(simpleType, newDependencyMap));
			JavaDependencies.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			simpleType.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			final ITypeBinding componentTypeBinding = typeBinding.getComponentType();
			if (componentTypeBinding != null) {
				simpleType.setInnerType(internalCreateTypeFromTypeBinding(componentTypeBinding,
						dependencyType, newDependencyMap));
			}

			if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
			return simpleType;

		} else {
			final ReferenceType referenceType = new ReferenceType(typeBindingQualifiedName);
			delayedTypes.put(typeBinding, Pair.immutableOf(referenceType, newDependencyMap));
			JavaDependencies.addDependencyToDelayedDependencyMap(newDependencyMap, originTypeBinding, dependencyType);

			referenceType.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getTypeAnnotations(),
					dependencyType, newDependencyMap));

			referenceType.setArguments(internalCreateTypesFromTypeBindings(typeBinding.getTypeArguments(),
					dependencyType, newDependencyMap));

			delayedReferenceTypeNodes.computeIfAbsent(originTypeBinding, JavaSnapshotParser::createArrayList)
					.add(referenceType);

			if (dependencyMap != null) JavaDependencies.combineDelayedDependencyMap(dependencyMap, newDependencyMap);
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
		dependencies.createDelayDependencyFromDependencyMap(dependencySourceNode, pair.getB());
	}

	@Nonnull
	List<AbstractType> createTypesFromTypeBindings(@Nonnull ITypeBinding[] typeBindings,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType) throws JavaCiaException {
		if (typeBindings.length == 0) return List.of(); // unnecessary, but nice to have
		final ArrayList<AbstractType> arguments = new ArrayList<>(typeBindings.length);
		for (final ITypeBinding typeBinding : typeBindings) {
			arguments.add(createTypeFromTypeBinding(typeBinding, dependencySourceNode, dependencyType));
		}
		return arguments;
	}

	@Nonnull
	AbstractType createTypeFromTypeBinding(@Nonnull ITypeBinding typeBinding,
			@Nonnull AbstractNode dependencySourceNode, @Nonnull JavaDependency dependencyType) throws JavaCiaException {
		final AbstractType type = createUnprocessedTypeFromTypeBinding(typeBinding, dependencyType);
		processUnprocessedType(typeBinding, dependencySourceNode);
		return type;
	}

}
