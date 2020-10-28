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

package mrmathami.cia.java.tree.dependency;

import mrmathami.annotations.Nonnull;

import java.util.EnumMap;
import java.util.Map;

public interface JavaDependencyCountTable {

	@Nonnull
	static JavaDependencyCountTable of(@Nonnull Map<JavaDependency, Integer> map) {
		final EnumMap<JavaDependency, Integer> enumMap = new EnumMap<>(map);
		return key -> {
			final Integer value = enumMap.get(key);
			return value != null ? value : 0;
		};
	}

	int getCount(@Nonnull JavaDependency dependencyType);

}
