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

package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.annotate.Annotate;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.cia.java.tree.type.JavaType;

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

	@Nonnull private transient List<AbstractNode> allNodes = List.of();
	@Nonnull private transient List<AbstractType> allTypes = List.of();
	@Nonnull private transient List<Annotate> allAnnotates = List.of();


	public RootNode() {
	}


	//region Root Helper

	@Nonnull
	public static <A, B extends A, C extends Collection<B>> C collectionFilter(
			@Nonnull C destCollection, @Nonnull Collection<A> collection, @Nonnull Class<B> filterClass) {
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

	@Nonnull
	@Override
	public RootNode getRoot() {
		return this;
	}

	@Nonnull
	@Override
	public AbstractNode getParent() {
		throw new NoSuchElementException("JavaRootNode does not have a parent.");
	}

	//endregion Basic Getter

	//region Getter & Setter

	@Nonnull
	@Override
	public List<AbstractNode> getAllNodes() {
		return isFrozen() ? allNodes : Collections.unmodifiableList(allNodes);
	}

//	void setAllNodes(@Nonnull List<AbstractNode> allNodes) {
//		checkFrozen();
//		this.allNodes = allNodes;
//	}

	@Nonnull
	@Override
	public List<AbstractType> getAllTypes() {
		return isFrozen() ? allTypes : Collections.unmodifiableList(allTypes);
	}

//	void setAllTypes(@Nonnull List<AbstractType> allTypes) {
//		checkFrozen();
//		this.allTypes = allTypes;
//	}

	@Nonnull
	@Override
	public List<Annotate> getAllAnnotates() {
		return isFrozen() ? allAnnotates : Collections.unmodifiableList(allAnnotates);
	}

//	void setAllAnnotates(@Nonnull List<Annotate> allAnnotates) {
//		checkFrozen();
//		this.allAnnotates = allAnnotates;
//	}

	//endregion Getter & Setter

	//region Serialization Helper

//	@Override
//	void internalFreeze(@Nonnull List<AbstractType> allTypes, @Nonnull List<Annotate> allAnnotates) {
//		super.internalFreeze(allTypes, allAnnotates);
//		this.allNodes = List.copyOf(allNodes);
//		this.allTypes = List.copyOf(allTypes);
//		this.allAnnotates = List.copyOf(allAnnotates);
//	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(allNodes);
		outputStream.writeObject(allTypes);
		outputStream.writeObject(allAnnotates);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.allNodes = (List<AbstractNode>) inputStream.readObject();
		this.allTypes = (List<AbstractType>) inputStream.readObject();
		this.allAnnotates = (List<Annotate>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonEnd(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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
