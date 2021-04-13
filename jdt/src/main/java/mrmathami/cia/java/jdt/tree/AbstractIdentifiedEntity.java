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
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractIdentifiedEntity extends AbstractEntity implements JavaIdentifiedEntity {

	private static final long serialVersionUID = -1L;

	private int id;


	//region Serialization Helper

	@Nonnull
	private static <A, R> List<A> createArrayList(@Nullable R any) {
		return new ArrayList<>();
	}

	// must be called when @Override
	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		final List<AbstractIdentifiedEntity> list
				= map.computeIfAbsent(getIdClass(), AbstractIdentifiedEntity::createArrayList);
		this.id = list.size();
		list.add(this);
		return false;
	}

	//endregion Serialization Helper

	//region Getter & Setter

	@Override
	public int getId() {
		assertFrozen();
		return id;
	}

	//endregion Getter & Setter

	//region Jsonify

	@Override
	public final void internalToJson(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		internalToReferenceJson(builder);
		internalToJsonStart(builder, indentation);
		internalToJsonEnd(builder, indentation);
	}

	public final void internalToReferenceJson(@Nonnull StringBuilder builder) {
		builder.append("\"entityClass\": \"").append(getEntityClass())
				.append("\", \"idClass\": \"").append(getIdClass())
				.append("\", \"id\": ").append(id);
		internalToReferenceJsonStart(builder);
		internalToReferenceJsonEnd(builder);
	}


	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
	}

	protected void internalToReferenceJsonEnd(@Nonnull StringBuilder builder) {
	}

	//endregion Jsonify

}
