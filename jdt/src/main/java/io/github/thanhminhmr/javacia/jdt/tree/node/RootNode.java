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
import io.github.thanhminhmr.javacia.jdt.tree.annotate.Annotate;
import io.github.thanhminhmr.javacia.jdt.tree.type.AbstractType;
import io.github.thanhminhmr.javacia.tree.annotate.JavaAnnotate;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import io.github.thanhminhmr.javacia.tree.node.JavaRootNode;
import io.github.thanhminhmr.javacia.tree.type.JavaType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class RootNode extends AbstractNode implements JavaRootNode {

	private static final long serialVersionUID = -1L;

	private transient @NotNull List<@NotNull AbstractNode> allNodes = List.of();
	private transient @NotNull List<@NotNull AbstractType> allTypes = List.of();
	private transient @NotNull List<@NotNull Annotate> allAnnotates = List.of();


	public RootNode() {
	}


	//region Root Helper

	@NotNull
	public static <A, B extends A, C extends Collection<B>> C collectionFilter(
			@NotNull C destCollection, @NotNull Collection<A> collection, @NotNull Class<B> filterClass) {
		for (final A element : collection) {
			if (filterClass.isInstance(element)) destCollection.add(filterClass.cast(element));
		}
		return destCollection;
	}

	public void freeze() {
		final Map<String, List<AbstractIdentifiedEntity>> map = new LinkedHashMap<>();
		internalFreeze(map);

		final List<AbstractIdentifiedEntity> allNodes = map.get(JavaNode.ID_CLASS);
		if (allNodes != null) {
			this.allNodes = List.copyOf(collectionFilter(new ArrayList<>(), allNodes, AbstractNode.class));
		}

		final List<AbstractIdentifiedEntity> allTypes = map.get(JavaType.ID_CLASS);
		if (allTypes != null) {
			this.allTypes = List.copyOf(collectionFilter(new ArrayList<>(), allTypes, AbstractType.class));
		}

		final List<AbstractIdentifiedEntity> allAnnotates = map.get(JavaAnnotate.ID_CLASS);
		if (allAnnotates != null) {
			this.allAnnotates = List.copyOf(collectionFilter(new ArrayList<>(), allAnnotates, Annotate.class));
		}
	}

	//endregion Root Helper

	//region Basic Getter

	@Override
	public @NotNull RootNode getRoot() {
		return this;
	}

	@Override
	public @NotNull AbstractNode getParent() {
		throw new NoSuchElementException("JavaRootNode");
	}

	//endregion Basic Getter

	//region Getter & Setter

	@Override
	public @NotNull List<@NotNull AbstractNode> getAllNodes() {
		return isFrozen() ? allNodes : Collections.unmodifiableList(allNodes);
	}

//	void setAllNodes(@NotNull List<AbstractNode> allNodes) {
//		checkFrozen();
//		this.allNodes = allNodes;
//	}

	@Override
	public @NotNull List<@NotNull AbstractType> getAllTypes() {
		return isFrozen() ? allTypes : Collections.unmodifiableList(allTypes);
	}

//	void setAllTypes(@NotNull List<AbstractType> allTypes) {
//		checkFrozen();
//		this.allTypes = allTypes;
//	}

	@Override
	public @NotNull List<@NotNull Annotate> getAllAnnotates() {
		return isFrozen() ? allAnnotates : Collections.unmodifiableList(allAnnotates);
	}

//	void setAllAnnotates(@NotNull List<Annotate> allAnnotates) {
//		checkFrozen();
//		this.allAnnotates = allAnnotates;
//	}

	//endregion Getter & Setter

	//region Serialization Helper

//	@Override
//	void internalFreeze(@NotNull List<AbstractType> allTypes, @NotNull List<Annotate> allAnnotates) {
//		super.internalFreeze(allTypes, allAnnotates);
//		this.allNodes = List.copyOf(allNodes);
//		this.allTypes = List.copyOf(allTypes);
//		this.allAnnotates = List.copyOf(allAnnotates);
//	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(allNodes);
		outputStream.writeObject(allTypes);
		outputStream.writeObject(allAnnotates);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.allNodes = (List<AbstractNode>) inputStream.readObject();
		this.allTypes = (List<AbstractType>) inputStream.readObject();
		this.allAnnotates = (List<Annotate>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonEnd(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonEnd(builder, indentation);
		if (!allNodes.isEmpty()) {
			builder.append(", \"allNodes\": [");
			internalArrayToReferenceJson(builder, indentation, allNodes);
			builder.append('\n').append(indentation).append(']');
		}
		if (!allTypes.isEmpty()) {
			builder.append(", \"allTypes\": [");
			internalArrayToJson(builder, indentation, true, allTypes);
			builder.append('\n').append(indentation).append(']');
		}
		if (!allAnnotates.isEmpty()) {
			builder.append(", \"allAnnotates\": [");
			internalArrayToJson(builder, indentation, true, allAnnotates);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
