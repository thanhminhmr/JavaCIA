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

package mrmathami.cia.java.jdt.project.differ;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;

final class EntityWrapper {

	@Nonnull private final EntityMatcher matcher;
	@Nonnull private final JavaIdentifiedEntity entity;
	private final int matchCode;
	private final boolean identicalMatch;

	EntityWrapper(@Nonnull EntityMatcher matcher, @Nonnull JavaIdentifiedEntity entity,
			int matchCode, boolean identicalMatch) {
		this.matcher = matcher;
		this.entity = entity;
		this.matchCode = matchCode;
		this.identicalMatch = identicalMatch;
	}

	@Override
	public boolean equals(Object object) {
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
