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

package mrmathami.cia.java.jdt.tree;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.JavaEntity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public abstract class AbstractEntity implements JavaEntity, Serializable {

	private static final long serialVersionUID = -1L;

	private transient boolean frozen = false;


	//region Serialization Helper

	protected final boolean isFrozen() {
		return frozen;
	}

	protected final void assertNonFrozen() throws UnsupportedOperationException {
		if (frozen) throw new UnsupportedOperationException("Frozen Node!");
	}

	protected final void assertFrozen() throws UnsupportedOperationException {
		if (!frozen) throw new UnsupportedOperationException("Non Frozen Node!");
	}

	// must be called when @Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (frozen) return true;
		this.frozen = true;
		return false;
	}

	private void readObject(@Nonnull ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		inputStream.defaultReadObject();
		this.frozen = true;
	}

	//endregion Serialization Helper

	//region Jsonify

	@Nonnull
	@Override
	public final String toJson() {
		final StringBuilder builder = new StringBuilder("{ ");
		internalToJson(builder, "");
		return builder.append(" }").toString();
	}

	public abstract void internalToJson(@Nonnull StringBuilder builder, @Nonnull String indentation);

	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
	}

	protected void internalToJsonEnd(@Nonnull StringBuilder builder, @Nonnull String indentation) {
	}

	//endregion Jsonify

	//region Jsonify Helper

	protected static void internalArrayToReferenceJson(@Nonnull StringBuilder builder,
			@Nonnull String indentation, @Nonnull List<? extends AbstractIdentifiedEntity> entities) {
		boolean next = false;
		for (final AbstractIdentifiedEntity entity : entities) {
			builder.append(next ? ",\n\t" : "\n\t").append(indentation).append("{ ");
			entity.internalToReferenceJson(builder);
			builder.append(" }");
			next = true;
		}
	}

	protected static void internalArrayToJson(@Nonnull StringBuilder builder, @Nonnull String indentation,
			boolean increaseIndentation, @Nonnull List<? extends AbstractEntity> entities) {
		boolean next = false;
		for (final AbstractEntity jsonify : entities) {
			builder.append(next ? ",\n\t" : "\n\t").append(indentation).append("{ ");
			jsonify.internalToJson(builder, increaseIndentation ? indentation + '\t' : indentation);
			builder.append(" }");
			next = true;
		}
	}

	protected static void internalEscapeString(@Nonnull StringBuilder builder, @Nonnull String string) {
		for (final int codePoint : string.codePoints().toArray()) {
			if (codePoint == '\\' || codePoint == '/' || codePoint == '"') {
				builder.append('\\').appendCodePoint(codePoint);
			} else if (codePoint == '\b') {
				builder.append("\\b");
			} else if (codePoint == '\f') {
				builder.append("\\f");
			} else if (codePoint == '\n') {
				builder.append("\\n");
			} else if (codePoint == '\r') {
				builder.append("\\r");
			} else if (codePoint == '\t') {
				builder.append("\\t");
			} else if (codePoint < 32) {
				builder.append("\\u00").append(codePoint <= 0xF ? '0' : '1')
						.append((codePoint <= 0x9 ? '0' : 49) + (codePoint & 0xF));
			} else {
				builder.appendCodePoint(codePoint);
			}
		}
	}

	//endregion Jsonify Helper

}
