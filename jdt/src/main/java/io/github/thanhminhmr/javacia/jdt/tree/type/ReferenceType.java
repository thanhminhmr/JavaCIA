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

package io.github.thanhminhmr.javacia.jdt.tree.type;

import io.github.thanhminhmr.javacia.jdt.tree.AbstractIdentifiedEntity;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.tree.type.JavaReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReferenceType extends AbstractType implements JavaReferenceType {

	private static final long serialVersionUID = -1L;

	private @Nullable AbstractNode node;

	private transient @NotNull List<@NotNull AbstractType> arguments = List.of();


	public ReferenceType(@NotNull String description) {
		super(description);
	}


	//region Getter & Setter

	@Override
	public @Nullable AbstractNode getNode() {
		return node;
	}

	public void setNode(@NotNull AbstractNode node) {
		assertNonFrozen();
		this.node = node;
	}

	@Override
	public @NotNull List<@NotNull AbstractType> getArguments() {
		return isFrozen() ? arguments : Collections.unmodifiableList(arguments);
	}

	public void setArguments(@NotNull List<@NotNull AbstractType> arguments) {
		assertNonFrozen();
		this.arguments = arguments;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.arguments = List.copyOf(arguments);
		for (final AbstractType argument : arguments) argument.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(arguments);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.arguments = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (node != null) {
			builder.append(", \"node\": { ");
			node.internalToReferenceJson(builder);
			builder.append(" }");
		}
		if (!arguments.isEmpty()) {
			builder.append(", \"arguments\": [");
			internalArrayToReferenceJson(builder, indentation, arguments);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
