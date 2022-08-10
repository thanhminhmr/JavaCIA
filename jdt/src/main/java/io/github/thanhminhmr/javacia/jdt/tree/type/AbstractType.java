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
import io.github.thanhminhmr.javacia.jdt.tree.annotate.Annotate;
import io.github.thanhminhmr.javacia.tree.type.JavaType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractType extends AbstractIdentifiedEntity implements JavaType {

	private static final long serialVersionUID = -1L;

	private final @NotNull String description;
	private transient @NotNull List<@NotNull Annotate> annotates = List.of();


	public AbstractType(@NotNull String description) {
		this.description = description;
	}


	//region Getter & Setter

	@Override
	public final @NotNull String getDescription() {
		return description;
	}

	@Override
	public final @NotNull List<@NotNull Annotate> getAnnotates() {
		return isFrozen() ? annotates : Collections.unmodifiableList(annotates);
	}

	public final void setAnnotates(@NotNull List<@NotNull Annotate> annotates) {
		assertNonFrozen();
		this.annotates = annotates;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.annotates = List.copyOf(annotates);
		for (final Annotate annotate : annotates) annotate.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(annotates);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.annotates = (List<Annotate>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonEnd(@NotNull StringBuilder builder) {
		super.internalToReferenceJsonEnd(builder);
		builder.append(", \"describe\": \"").append(description).append('"');
	}

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!annotates.isEmpty()) {
			builder.append(", \"annotates\": [");
			internalArrayToJson(builder, indentation, false, annotates);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

	@Override
	public final @NotNull String toString() {
		return description;
	}

}
