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

package io.github.thanhminhmr.javacia.jdt.tree.node;

import io.github.thanhminhmr.javacia.jdt.tree.AbstractIdentifiedEntity;
import io.github.thanhminhmr.javacia.jdt.tree.dependency.DependencyCountTable;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import io.github.thanhminhmr.javacia.utils.ImmutableOrderedMap;
import org.jetbrains.annotations.NotNull;

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

	private transient @NotNull List<AbstractNode> children = new ArrayList<>();
	private transient @NotNull Map<AbstractNode, DependencyCountTable> dependencyFrom = new LinkedHashMap<>();
	private transient @NotNull Map<AbstractNode, DependencyCountTable> dependencyTo = new LinkedHashMap<>();


	protected static void checkParent(@NotNull AbstractNode parentNode, @NotNull Class<?>... nodeClasses) {
		for (final Class<?> nodeClass : nodeClasses) {
			if (nodeClass.isInstance(parentNode)) return;
		}
		throw new UnsupportedOperationException("Invalid parent type!");
	}


	public AbstractNode() {
	}


	//region Basic Getter

	@Override
	public abstract @NotNull RootNode getRoot();

	@Override
	public abstract @NotNull AbstractNode getParent();

	@Override
	public final @NotNull List<@NotNull AbstractNode> getChildren() {
		return isFrozen() ? children : Collections.unmodifiableList(children);
	}

	//endregion Basic Getter

	//region Dependency

	@Override
	public final @NotNull Map<@NotNull AbstractNode, @NotNull DependencyCountTable> getDependencyFrom() {
		return isFrozen() ? dependencyFrom : Collections.unmodifiableMap(dependencyFrom);
	}

	@Override
	public final @NotNull Map<@NotNull AbstractNode, @NotNull DependencyCountTable> getDependencyTo() {
		return isFrozen() ? dependencyTo : Collections.unmodifiableMap(dependencyTo);
	}

	@Override
	public final @NotNull Set<@NotNull AbstractNode> getDependencyFromNodes() {
		return isFrozen() ? dependencyFrom.keySet() : Collections.unmodifiableSet(dependencyFrom.keySet());
	}

	@Override
	public final @NotNull Set<@NotNull AbstractNode> getDependencyToNodes() {
		return isFrozen() ? dependencyTo.keySet() : Collections.unmodifiableSet(dependencyTo.keySet());
	}

	public final void createDependencyTo(@NotNull AbstractNode node, @NotNull DependencyCountTable nodeDependency) {
		assertNonFrozen();
		assert getRoot() == node.getRoot() : "Node is not in the same tree!";
		assert node != this : "Self dependency is not allowed!";

		final boolean check = dependencyTo.put(node, nodeDependency) == null
				&& node.dependencyFrom.put(this, nodeDependency) == null;
		assert check : "Node dependency already exist!";
	}

	//endregion Dependency

	//region Tree Node

	@NotNull
	public final <E extends AbstractNode> E addChild(@NotNull E node) {
		assertNonFrozen();
		assert !node.isRoot() && node.getParent() == this : "This node is not my child!";
		assert !children.contains(node) : "This node is already my child!";
		children.add(node);
		return node;
	}

	//endregion Tree Node

	//region Serialization Helper

	// must be call when @Override
	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.children = List.copyOf(children);
		this.dependencyFrom = ImmutableOrderedMap.copyOf(dependencyFrom);
		this.dependencyTo = ImmutableOrderedMap.copyOf(dependencyTo);
		for (final AbstractNode child : children) child.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(children);
		outputStream.writeObject(dependencyFrom);
		outputStream.writeObject(dependencyTo);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.children = (List<AbstractNode>) inputStream.readObject();
		this.dependencyFrom = (Map<AbstractNode, DependencyCountTable>) inputStream.readObject();
		this.dependencyTo = (Map<AbstractNode, DependencyCountTable>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	private static void internalDependencyMapToJson(@NotNull StringBuilder builder, @NotNull String indentation,
			@NotNull Map<AbstractNode, DependencyCountTable> dependencyMap) {
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
	protected void internalToJsonEnd(@NotNull StringBuilder builder, @NotNull String indentation) {
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

	@Override
	public final @NotNull String toString() {
		return getUniqueName();
	}

}
