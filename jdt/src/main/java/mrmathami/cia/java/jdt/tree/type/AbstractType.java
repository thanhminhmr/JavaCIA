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

package mrmathami.cia.java.jdt.tree.type;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.annotate.Annotate;
import mrmathami.cia.java.tree.type.JavaType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractType extends AbstractIdentifiedEntity implements JavaType {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String description;
	@Nonnull private transient List<Annotate> annotates = List.of();


	public AbstractType(@Nonnull String description) {
		this.description = description;
	}


	//region Getter & Setter

	@Nonnull
	@Override
	public final String getDescription() {
		return description;
	}

	@Nonnull
	@Override
	public final List<Annotate> getAnnotates() {
		return isFrozen() ? annotates : Collections.unmodifiableList(annotates);
	}

	public final void setAnnotates(@Nonnull List<Annotate> annotates) {
		assertNonFrozen();
		this.annotates = annotates;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.annotates = List.copyOf(annotates);
		for (final Annotate annotate : annotates) annotate.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(annotates);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.annotates = (List<Annotate>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonEnd(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonEnd(builder);
		builder.append(", \"describe\": \"").append(description).append('"');
	}

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!annotates.isEmpty()) {
			builder.append(", \"annotates\": [");
			internalArrayToJson(builder, indentation, false, annotates);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

	@Nonnull
	@Override
	public final String toString() {
		return description;
	}

}
