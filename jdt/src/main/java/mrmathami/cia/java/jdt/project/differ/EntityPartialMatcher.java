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

package mrmathami.cia.java.jdt.project.differ;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;
import mrmathami.cia.java.tree.node.JavaClassNode;
import mrmathami.cia.java.tree.node.JavaEnumNode;
import mrmathami.cia.java.tree.node.JavaFieldNode;
import mrmathami.cia.java.tree.node.JavaInitializerNode;
import mrmathami.cia.java.tree.node.JavaInterfaceNode;
import mrmathami.cia.java.tree.node.JavaMethodNode;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.attribute.JavaAnnotatedNode;
import mrmathami.cia.java.tree.node.attribute.JavaModifiedNode;
import mrmathami.cia.java.tree.node.attribute.JavaParameterizedNode;
import mrmathami.cia.java.tree.node.attribute.JavaTypeNode;
import mrmathami.cia.java.tree.type.JavaReferenceType;
import mrmathami.cia.java.tree.type.JavaSimpleType;
import mrmathami.cia.java.tree.type.JavaSyntheticType;
import mrmathami.cia.java.tree.type.JavaType;
import mrmathami.utils.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

enum EntityPartialMatcher {

	NODE(JavaNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaNode;
			final JavaNode node = (JavaNode) entity;
			int matchCode = node.getEntityClass().hashCode();
			matchCode = matchCode * 31 + node.getNodeName().hashCode();
			matchCode = matchCode * 31 + node.getQualifiedName().hashCode();
			matchCode = matchCode * 31 + node.getUniqueName().hashCode();
			return matchCode * 31 + (identicalMatch ? node.getDependencyToNodes().size() : -1);
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaNode && entityB instanceof JavaNode;
			final JavaNode nodeA = (JavaNode) entityA, nodeB = (JavaNode) entityB;

			if (!nodeA.getEntityClass().equals(nodeB.getEntityClass())
					|| !nodeA.getNodeName().equals(nodeB.getNodeName())
					|| !nodeA.getQualifiedName().equals(nodeB.getQualifiedName())
					|| !nodeA.getUniqueName().equals(nodeB.getUniqueName())
					|| !nodeA.isRoot() && !nodeB.isRoot()
					&& !matcher.match(nodeA.getParent(), nodeB.getParent(), false)) {
				return false;
			}
			if (!identicalMatch) return true;

			// dependency node only need to be similar
			final Map<? extends JavaNode, ? extends JavaDependencyCountTable> dependencyToA = nodeA.getDependencyTo();
			final Map<? extends JavaNode, ? extends JavaDependencyCountTable> dependencyToB = nodeB.getDependencyTo();
			if (dependencyToA.size() != dependencyToB.size()) return false;

			final Map<Pair<EntityWrapper, JavaDependencyCountTable>, int[]> map = new HashMap<>();
			for (final Map.Entry<? extends JavaNode, ? extends JavaDependencyCountTable> entry : dependencyToA.entrySet()) {
				final EntityWrapper wrapper = matcher.wrap(entry.getKey(), false);
				final Pair<EntityWrapper, JavaDependencyCountTable> pair = Pair.immutableOf(wrapper, entry.getValue());
				final int[] countWrapper = map.computeIfAbsent(pair, EntityPartialMatcher::createCountWrapper);
				countWrapper[0] += 1;
			}
			for (final Map.Entry<? extends JavaNode, ? extends JavaDependencyCountTable> entry : dependencyToB.entrySet()) {
				final EntityWrapper wrapper = matcher.wrap(entry.getKey(), false);
				final Pair<EntityWrapper, JavaDependencyCountTable> pair = Pair.immutableOf(wrapper, entry.getValue());
				final int[] countWrapper = map.get(pair);
				if (countWrapper == null || --countWrapper[0] < 0) return false;
			}
			return true;
		}
	},

	ANNOTATED_NODE(JavaAnnotatedNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaAnnotatedNode;
			return identicalMatch ? ((JavaAnnotatedNode) entity).getAnnotates().size() : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaAnnotatedNode && entityB instanceof JavaAnnotatedNode;
			return !identicalMatch || matcher.matchNonOrdered(((JavaAnnotatedNode) entityA).getAnnotates(),
					((JavaAnnotatedNode) entityB).getAnnotates(), true);
		}
	},

	MODIFIED_NODE(JavaModifiedNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaModifiedNode;
			return identicalMatch ? ((JavaModifiedNode) entity).getModifiers() : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaModifiedNode && entityB instanceof JavaModifiedNode;
			return !identicalMatch || ((JavaModifiedNode) entityA).getModifiers() == ((JavaModifiedNode) entityB).getModifiers();
		}
	},

	PARAMETERIZED_NODE(JavaParameterizedNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaParameterizedNode;
			return ((JavaParameterizedNode) entity).getTypeParameters().size();
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaParameterizedNode && entityB instanceof JavaParameterizedNode;
			return matcher.matchOrdered(((JavaParameterizedNode) entityA).getTypeParameters(),
					((JavaParameterizedNode) entityB).getTypeParameters(), identicalMatch);
		}
	},

	TYPE_NODE(JavaTypeNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaTypeNode;
			final String binaryName = ((JavaTypeNode) entity).getBinaryName();
			return binaryName != null ? binaryName.hashCode() : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaTypeNode && entityB instanceof JavaTypeNode;
			final String binaryNameA = ((JavaTypeNode) entityA).getBinaryName();
			final String binaryNameB = ((JavaTypeNode) entityB).getBinaryName();
			return binaryNameA != null && binaryNameA.equals(binaryNameB) || binaryNameB == null;
		}
	},

//	ANNOTATION_NODE(JavaAnnotationNode.class) {
//		@Override
//		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
//			return 0;
//		}
//
//		@Override
//		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
//				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
//			return false;
//		}
//	},

	CLASS_NODE(JavaClassNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaClassNode;
			final JavaClassNode node = (JavaClassNode) entity;
			int matchCode = identicalMatch ? node.getExtendsClass() != null ? 1 : 0 : -1;
			return matchCode * 31 + (identicalMatch ? node.getImplementsInterfaces().size() : -1);
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaClassNode && entityB instanceof JavaClassNode;
			final JavaClassNode nodeA = (JavaClassNode) entityA, nodeB = (JavaClassNode) entityB;
			return !identicalMatch || (matcher.match(nodeA.getExtendsClass(), nodeB.getExtendsClass(), false)
					&& matcher.matchNonOrdered(nodeA.getImplementsInterfaces(), nodeB.getImplementsInterfaces(), false));
		}
	},

	ENUM_NODE(JavaEnumNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaEnumNode;
			return identicalMatch ? ((JavaEnumNode) entity).getImplementsInterfaces().size() : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaEnumNode && entityB instanceof JavaEnumNode;
			return !identicalMatch || matcher.matchNonOrdered(((JavaEnumNode) entityA).getImplementsInterfaces(),
					((JavaEnumNode) entityB).getImplementsInterfaces(), false);
		}
	},

	FIELD_NODE(JavaFieldNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaFieldNode;
			final JavaFieldNode node = (JavaFieldNode) entity;
			return identicalMatch ? node.getType() != null ? 1 : 0 : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaFieldNode && entityB instanceof JavaFieldNode;
			final JavaFieldNode nodeA = (JavaFieldNode) entityA, nodeB = (JavaFieldNode) entityB;
			return !identicalMatch || matcher.match(nodeA.getType(), nodeB.getType(), true);
		}
	},

	INITIALIZER_NODE(JavaInitializerNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaInitializerNode;
			final JavaInitializerNode node = (JavaInitializerNode) entity;
			int matchCode = node.isStatic() ? 1 : 0;
			return matchCode * 31 + (identicalMatch ? node.getInitializers().size() : -1);
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaInitializerNode && entityB instanceof JavaInitializerNode;
			final JavaInitializerNode nodeA = (JavaInitializerNode) entityA, nodeB = (JavaInitializerNode) entityB;
			return nodeA.isStatic() == nodeB.isStatic() && (!identicalMatch ||
					internalMatchInitializer(nodeA.getInitializers(), nodeB.getInitializers(), matcher));
		}

		private boolean internalMatchInitializer(
				@Nonnull List<? extends JavaInitializerNode.Initializer> initializersA,
				@Nonnull List<? extends JavaInitializerNode.Initializer> initializersB,
				@Nonnull EntityMatcher matcher) {
			if (initializersA.size() != initializersB.size()) return false;

			final Iterator<? extends JavaInitializerNode.Initializer> iteratorA = initializersA.iterator();
			final Iterator<? extends JavaInitializerNode.Initializer> iteratorB = initializersB.iterator();
			while (iteratorA.hasNext()/* && iteratorB.hasNext()*/) {
				final JavaInitializerNode.Initializer initializerA = iteratorA.next();
				final JavaInitializerNode.Initializer initializerB = iteratorB.next();
				if (initializerA instanceof JavaInitializerNode.BlockInitializer) {
					return initializerB instanceof JavaInitializerNode.BlockInitializer
							&& ((JavaInitializerNode.BlockInitializer) initializerA).getBodyBlock()
							.equals(((JavaInitializerNode.BlockInitializer) initializerB).getBodyBlock());
				} else if (initializerA instanceof JavaInitializerNode.FieldInitializer) {
					if (!(initializerB instanceof JavaInitializerNode.FieldInitializer)) return false;
					final JavaInitializerNode.FieldInitializer
							fieldInitA = (JavaInitializerNode.FieldInitializer) initializerA,
							fieldInitB = (JavaInitializerNode.FieldInitializer) initializerB;
					return matcher.match(fieldInitA.getFieldNode(), fieldInitB.getFieldNode(), true)
							&& Objects.equals(fieldInitA.getInitialExpression(), fieldInitB.getInitialExpression());
				}
			}
			return false;
		}
	},

	INTERFACE_NODE(JavaInterfaceNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaInterfaceNode;
			return identicalMatch ? ((JavaInterfaceNode) entity).getExtendsInterfaces().size() : -1;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaInterfaceNode && entityB instanceof JavaInterfaceNode;
			final JavaInterfaceNode nodeA = (JavaInterfaceNode) entityA, nodeB = (JavaInterfaceNode) entityB;
			return !identicalMatch || matcher.matchNonOrdered(nodeA.getExtendsInterfaces(),
					nodeB.getExtendsInterfaces(), false);
		}
	},

	METHOD_NODE(JavaMethodNode.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaMethodNode;
			final JavaMethodNode node = (JavaMethodNode) entity;
			int matchCode = identicalMatch ? node.getReturnType() != null ? 1 : 0 : -1;
			matchCode = matchCode * 31 + node.getParameters().size();
			matchCode = matchCode * 31 + (identicalMatch ? node.getExceptions().size() : -1);
			return matchCode * 31 + (identicalMatch ? node.getBodyBlock() != null ? 1 : 0 : -1);
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaMethodNode && entityB instanceof JavaMethodNode;
			final JavaMethodNode nodeA = (JavaMethodNode) entityA, nodeB = (JavaMethodNode) entityB;
			return matcher.matchOrdered(nodeA.getParameters(), nodeB.getParameters(), identicalMatch)
					&& (!identicalMatch || (matcher.match(nodeA.getReturnType(), nodeB.getReturnType(), true)
					&& matcher.matchNonOrdered(nodeA.getExceptions(), nodeB.getExceptions(), true)
					&& Objects.equals(nodeA.getBodyBlock(), nodeB.getBodyBlock())));
		}
	},

//	PACKAGE_NODE(JavaPackageNode.class) {
//		@Override
//		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
//			return 0;
//		}
//
//		@Override
//		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
//				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
//			return false;
//		}
//	},

//	ROOT_NODE(JavaRootNode.class) {
//		@Override
//		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
//			return 0;
//		}
//
//		@Override
//		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
//				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
//			return false;
//		}
//	},

	TYPE(JavaType.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaType;
			final JavaType type = (JavaType) entity;
			int matchCode = type.getEntityClass().hashCode();
			matchCode = matchCode * 31 + type.getDescription().hashCode();
			return matchCode * 31 + (identicalMatch ? type.getAnnotates().size() : -1);
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaType && entityB instanceof JavaType;
			final JavaType typeA = (JavaType) entityA, typeB = (JavaType) entityB;
			return typeA.getDescription().equals(typeB.getDescription()) && (!identicalMatch
					|| matcher.matchNonOrdered(typeA.getAnnotates(), typeB.getAnnotates(), true));
		}
	},

	REFERENCE_TYPE(JavaReferenceType.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaReferenceType;
			final JavaReferenceType type = (JavaReferenceType) entity;
			int matchCode = type.getNode() != null ? 1 : 0;
			return matchCode * 31 + type.getArguments().size();
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaReferenceType && entityB instanceof JavaReferenceType;
			final JavaReferenceType typeA = (JavaReferenceType) entityA, typeB = (JavaReferenceType) entityB;
			return matcher.match(typeA.getNode(), typeB.getNode(), false)
					&& matcher.matchOrdered(typeA.getArguments(), typeB.getArguments(), identicalMatch);
		}
	},

	SIMPLE_TYPE(JavaSimpleType.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaSimpleType;
			return ((JavaSimpleType) entity).getInnerType() != null ? 1 : 0;
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaSimpleType && entityB instanceof JavaSimpleType;
			return matcher.match(((JavaSimpleType) entityA).getInnerType(),
					((JavaSimpleType) entityB).getInnerType(), identicalMatch);
		}
	},

	SYNTHETIC_TYPE(JavaSyntheticType.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaSyntheticType;
			return ((JavaSyntheticType) entity).getBounds().size();
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaSyntheticType && entityB instanceof JavaSyntheticType;
			return matcher.matchNonOrdered(((JavaSyntheticType) entityA).getBounds(),
					((JavaSyntheticType) entityB).getBounds(), identicalMatch);
		}
	},

	ANNOTATE(JavaAnnotate.class) {
		@Override
		protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
			assert entity instanceof JavaAnnotate;
			final JavaAnnotate annotate = (JavaAnnotate) entity;
			int matchCode = annotate.getEntityClass().hashCode();
			matchCode = matchCode * 31 + annotate.getName().hashCode();
			matchCode = matchCode * 31 + (annotate.getNode() != null ? 1 : 0);
			return matchCode * 31 + annotate.getParameters().size();
		}

		@Override
		protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			assert entityA instanceof JavaAnnotate && entityB instanceof JavaAnnotate;
			final JavaAnnotate annotateA = (JavaAnnotate) entityA, annotateB = (JavaAnnotate) entityB;

			if (!annotateA.getName().equals(annotateB.getName())
					|| !matcher.match(annotateA.getNode(), annotateB.getNode(), identicalMatch)) {
				return false;
			}

			final List<? extends JavaAnnotate.Parameter> parametersA = annotateA.getParameters();
			final List<? extends JavaAnnotate.Parameter> parametersB = annotateB.getParameters();
			if (parametersA.size() != parametersB.size()) return false;

			final Map<String, JavaAnnotate.Parameter> map = new HashMap<>();
			for (final JavaAnnotate.Parameter parameterA : parametersA) map.put(parameterA.getName(), parameterA);
			for (final JavaAnnotate.Parameter parameterB : parametersB) {
				final JavaAnnotate.Parameter parameterA = map.get(parameterB.getName());
				if (parameterA == null || !parameterA.getName().equals(parameterB.getName())
						|| !matcher.match(parameterA.getNode(), parameterB.getNode(), identicalMatch)
						|| internalMismatchValue(parameterA.getValue(), parameterB.getValue(), matcher, identicalMatch))
					return false;
			}
			return true;
		}

		private boolean internalMismatchValue(@Nullable JavaAnnotate.Value valueA, @Nullable JavaAnnotate.Value valueB,
				@Nonnull EntityMatcher matcher, boolean identicalMatch) {
			if (valueA == null || valueB == null) return valueA != valueB;
			if (!valueA.getEntityClass().equals(valueB.getEntityClass())) return true;
			if (valueA instanceof JavaAnnotate.ArrayValue) {
				if (!(valueB instanceof JavaAnnotate.ArrayValue)) return true;

				final List<? extends JavaAnnotate.NonArrayValue> valuesA = ((JavaAnnotate.ArrayValue) valueA).getValues();
				final List<? extends JavaAnnotate.NonArrayValue> valuesB = ((JavaAnnotate.ArrayValue) valueB).getValues();
				if (valuesA.size() != valuesB.size()) return true;

				final Iterator<? extends JavaAnnotate.NonArrayValue> iteratorA = valuesA.iterator();
				final Iterator<? extends JavaAnnotate.NonArrayValue> iteratorB = valuesB.iterator();
				while (iteratorA.hasNext()/* && iteratorB.hasNext()*/) {
					if (internalMismatchValue(iteratorA.next(), iteratorB.next(), matcher, identicalMatch)) return true;
				}
				return false;

			} else if (valueA instanceof JavaAnnotate.SimpleValue) {
				if (!(valueB instanceof JavaAnnotate.SimpleValue)) return true;

				final JavaAnnotate.SimpleValue simpleValueA = (JavaAnnotate.SimpleValue) valueA;
				final JavaAnnotate.SimpleValue simpleValueB = (JavaAnnotate.SimpleValue) valueB;
				return !simpleValueA.getValueType().equals(simpleValueB.getValueType())
						|| !simpleValueA.getValue().equals(simpleValueB.getValue());

			} else if (valueA instanceof JavaAnnotate.NodeValue) {
				if (!(valueB instanceof JavaAnnotate.NodeValue)) return true;

				final JavaAnnotate.NodeValue nodeValueA = (JavaAnnotate.NodeValue) valueA;
				final JavaAnnotate.NodeValue nodeValueB = (JavaAnnotate.NodeValue) valueB;
				return !nodeValueA.getDescribe().equals(nodeValueB.getDescribe())
						|| !matcher.match(nodeValueA.getNode(), nodeValueB.getNode(), identicalMatch);
			} else if (valueA instanceof JavaAnnotate.AnnotateValue) {
				if (!(valueB instanceof JavaAnnotate.AnnotateValue)) return true;

				final JavaAnnotate.AnnotateValue annotateValueA = (JavaAnnotate.AnnotateValue) valueA;
				final JavaAnnotate.AnnotateValue annotateValueB = (JavaAnnotate.AnnotateValue) valueB;
				return !matcher.match(annotateValueA.getAnnotate(), annotateValueB.getAnnotate(), identicalMatch);
			}
			throw new IllegalStateException("Unknown value type!");
		}
	};


	@Nonnull private static final EntityPartialMatcher[] PARTIAL_MATCHERS = values();


	@Nonnull private final Class<? extends JavaIdentifiedEntity> entityClass;


	EntityPartialMatcher(@Nonnull Class<? extends JavaIdentifiedEntity> entityClass) {
		this.entityClass = entityClass;
	}


	@Nonnull
	private static <R> int[] createCountWrapper(@Nullable R any) {
		return new int[]{0};
	}


	static int internalMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
		int matchCode = 0;
		for (final EntityPartialMatcher partialMatcher : PARTIAL_MATCHERS) {
			if (partialMatcher.entityClass.isInstance(entity)) {
				matchCode = matchCode * 31 + partialMatcher.partialMatchCode(entity, identicalMatch);
			}
		}
		return matchCode;
	}

	static boolean internalMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
			@Nonnull EntityMatcher matcher, boolean identicalMatch) {
		for (final EntityPartialMatcher partialMatcher : PARTIAL_MATCHERS) {
			final boolean conditionA = partialMatcher.entityClass.isInstance(entityA);
			final boolean conditionB = partialMatcher.entityClass.isInstance(entityB);
			if (conditionA != conditionB
					|| conditionA && !partialMatcher.partialMatch(entityA, entityB, matcher, identicalMatch)) {
				return false;
			}
		}
		return true;
	}


	abstract protected int partialMatchCode(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch);

	abstract protected boolean partialMatch(@Nonnull JavaIdentifiedEntity entityA, @Nonnull JavaIdentifiedEntity entityB,
			@Nonnull EntityMatcher matcher, boolean identicalMatch);

}
