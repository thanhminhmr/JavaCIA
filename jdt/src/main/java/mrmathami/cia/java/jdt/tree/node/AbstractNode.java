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

package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.dependency.DependencyCountTable;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.collections.ImmutableOrderedMap;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractNode extends AbstractIdentifiedEntity implements JavaNode {

	private static final long serialVersionUID = -1L;

	@Nonnull private transient List<AbstractNode> children = new ArrayList<>();
	@Nonnull private transient Map<AbstractNode, DependencyCountTable> dependencyFrom = new LinkedHashMap<>();
	@Nonnull private transient Map<AbstractNode, DependencyCountTable> dependencyTo = new LinkedHashMap<>();


	public AbstractNode() {
	}


	//region Basic Getter

	@Nonnull
	@Override
	public abstract RootNode getRoot();

	@Nonnull
	@Override
	public abstract AbstractNode getParent();

	@Nonnull
	public final List<AbstractNode> getChildren() {
		return isFrozen() ? children : Collections.unmodifiableList(children);
	}

	//endregion Basic Getter

	//region Dependency

	@Nonnull
	@Override
	public final Map<AbstractNode, DependencyCountTable> getDependencyFrom() {
		return isFrozen() ? dependencyFrom : Collections.unmodifiableMap(dependencyFrom);
	}

	@Nonnull
	@Override
	public final Map<AbstractNode, DependencyCountTable> getDependencyTo() {
		return isFrozen() ? dependencyTo : Collections.unmodifiableMap(dependencyTo);
	}

	@Nonnull
	@Override
	public final Set<AbstractNode> getDependencyFromNodes() {
		return isFrozen() ? dependencyFrom.keySet() : Collections.unmodifiableSet(dependencyFrom.keySet());
	}

	@Nonnull
	@Override
	public final Set<AbstractNode> getDependencyToNodes() {
		return isFrozen() ? dependencyTo.keySet() : Collections.unmodifiableSet(dependencyTo.keySet());
	}

	public final void createDependencyTo(@Nonnull AbstractNode node, @Nonnull DependencyCountTable nodeDependency) {
		assertNonFrozen();
		assert getRoot() == node.getRoot() : "Node is not in the same tree!";
		assert node != this : "Self dependency is not allowed!";

		final boolean check = dependencyTo.put(node, nodeDependency) == null
				&& node.dependencyFrom.put(this, nodeDependency) == null;
		assert check : "Node dependency already exist!";
	}

	//endregion Dependency

	//region Tree Node

	@Nonnull
	private <E extends AbstractNode> E internalAddChild(@Nonnull E node) {
		children.add(node);
		return node;
	}

	//endregion Tree Node

	//region Node Container

	@Nonnull
	public final AnnotationNode createChildAnnotation(@Nonnull String simpleName, @Nullable String binaryName) {
		assertNonFrozen();
		asAnnotationContainer();
		return internalAddChild(new AnnotationNode(this, simpleName, binaryName));
	}

	@Nonnull
	public final ClassNode createChildClass(@Nonnull String simpleName, @Nullable String binaryName) {
		assertNonFrozen();
		asClassContainer();
		return internalAddChild(new ClassNode(this, simpleName, binaryName));
	}

	@Nonnull
	public final EnumNode createChildEnum(@Nonnull String simpleName, @Nullable String binaryName) {
		assertNonFrozen();
		asEnumContainer();
		return internalAddChild(new EnumNode(this, simpleName, binaryName));
	}

	@Nonnull
	public final FieldNode createChildField(@Nonnull String simpleName) {
		assertNonFrozen();
		asFieldContainer();
		return internalAddChild(new FieldNode(this, simpleName));
	}

	@Nonnull
	public final InitializerNode createChildInitializer(boolean isStatic) {
		assertNonFrozen();
		asInitializerContainer();
		return internalAddChild(new InitializerNode(this, isStatic));
	}

	@Nonnull
	public final InterfaceNode createChildInterface(@Nonnull String simpleName, @Nullable String binaryName) {
		assertNonFrozen();
		asInterfaceContainer();
		return internalAddChild(new InterfaceNode(this, simpleName, binaryName));
	}

	@Nonnull
	public final MethodNode createChildMethod(@Nonnull String simpleName, @Nonnull List<AbstractType> parameters) {
		assertNonFrozen();
		asMethodContainer();
		return internalAddChild(new MethodNode(this, simpleName, parameters));
	}


	@Nonnull
	public final PackageNode createChildPackage(@Nonnull String simpleName) {
		assertNonFrozen();
		asPackageContainer();
		return internalAddChild(new PackageNode(this, simpleName));
	}

	@Nonnull
	public final XMLNode createChildXMlNode(@Nonnull String simpleName, String textContent, NodeList children, NamedNodeMap listAttributes) {
		assertNonFrozen();
		asXMLContainer();
		return internalAddChild(new XMLNode(simpleName, this, textContent, children, listAttributes));
	}

	//endregion Node Container

	//region Serialization Helper

	// must be call when @Override
	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.children = List.copyOf(children);
		this.dependencyFrom = ImmutableOrderedMap.copyOf(dependencyFrom);
		this.dependencyTo = ImmutableOrderedMap.copyOf(dependencyTo);
		for (final AbstractNode child : children) child.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(children);
		outputStream.writeObject(dependencyFrom);
		outputStream.writeObject(dependencyTo);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.children = (List<AbstractNode>) inputStream.readObject();
		this.dependencyFrom = (Map<AbstractNode, DependencyCountTable>) inputStream.readObject();
		this.dependencyTo = (Map<AbstractNode, DependencyCountTable>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	private static void internalDependencyMapToJson(@Nonnull StringBuilder builder, @Nonnull String indentation,
			@Nonnull Map<AbstractNode, DependencyCountTable> dependencyMap) {
		boolean next = false;
		for (final Map.Entry<AbstractNode, DependencyCountTable> entry : dependencyMap.entrySet()) {
			builder.append(next ? ",\n\t" : "\n\t").append(indentation).append("[ { ");
			entry.getKey().internalToReferenceJson(builder);
			builder.append(" }, ");
			entry.getValue().toString(builder);
			builder.append(" ]");
			next = true;
		}
	}


	@Override
	protected void internalToJsonEnd(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		if (!dependencyTo.isEmpty()) {
			builder.append(", \"dependencyTo\": [");
			internalDependencyMapToJson(builder, indentation, dependencyTo);
			builder.append('\n').append(indentation).append(']');
		}
		if (!dependencyFrom.isEmpty()) {
			builder.append(", \"dependencyFrom\": [");
			internalDependencyMapToJson(builder, indentation, dependencyFrom);
			builder.append('\n').append(indentation).append(']');
		}
		if (!children.isEmpty()) {
			builder.append(", \"children\": [");
			internalArrayToJson(builder, indentation, true, children);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

	@Nonnull
	@Override
	public final String toString() {
		return getUniqueName();
	}

}
