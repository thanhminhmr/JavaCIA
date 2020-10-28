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

package mrmathami.cia.java.jdt.tree;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.JavaNonIdentifiedEntity;

public abstract class AbstractNonIdentifiedEntity extends AbstractEntity implements JavaNonIdentifiedEntity {

	private static final long serialVersionUID = -1L;

	//region Jsonify

	@Override
	public final void internalToJson(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		builder.append("\"entityClass\": \"").append(getEntityClass()).append('"');
		internalToJsonStart(builder, indentation);
		internalToJsonEnd(builder, indentation);
	}

	//endregion Jsonify

}
