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

package io.github.thanhminhmr.javacia.jdt.project.differ;

import io.github.thanhminhmr.javacia.tree.JavaIdentifiedEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class EntityWrapper {

	private final @NotNull EntityMatcher matcher;
	private final @NotNull JavaIdentifiedEntity entity;
	private final int matchCode;
	private final boolean identicalMatch;

	EntityWrapper(@NotNull EntityMatcher matcher, @NotNull JavaIdentifiedEntity entity, int matchCode,
			boolean identicalMatch) {
		this.matcher = matcher;
		this.entity = entity;
		this.matchCode = matchCode;
		this.identicalMatch = identicalMatch;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof EntityWrapper)) return false;
		final EntityWrapper wrapper = (EntityWrapper) object;
		return matchCode == wrapper.matchCode && matcher.match(entity, wrapper.entity, identicalMatch);
	}

	@Override
	public int hashCode() {
		return matchCode;
	}

}
