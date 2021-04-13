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

package mrmathami.cia.java.utils;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.utils.ArrayUtils;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

public final class RelativePath implements Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String[] components;

	@Nullable private transient List<String> componentList;


	public RelativePath(@Nonnull String[] components) {
		this.components = components;
	}


	@Nonnull
	public static RelativePath fromPath(@Nonnull Path path) {
		return new RelativePath(
				StreamSupport.stream(path.spliterator(), false)
						.map(Path::toString)
						.toArray(String[]::new)
		);
	}

	@Nonnull
	public static RelativePath concat(@Nonnull RelativePath relativePathA, @Nonnull RelativePath relativePathB) {
		final String[] componentsA = relativePathA.components, componentsB = relativePathB.components;
		return componentsA.length == 0 ? relativePathB : componentsB.length == 0 ? relativePathA
				: new RelativePath(ArrayUtils.concat(componentsA, componentsB));
	}


	public int length() {
		return components.length;
	}

	@Nonnull
	public String getComponent(int index) {
		final int length = components.length;
		if (index < -length || index >= length) throw new IndexOutOfBoundsException();
		return index >= 0 ? components[index] : components[length + index];
	}

	@Nonnull
	public List<String> getComponents() {
		return componentList != null ? componentList : (this.componentList = List.of(components));
	}

	@Nonnull
	public String[] toArray() {
		return components.clone();
	}

	@Nonnull
	public Path appendToPath(@Nonnull Path path) {
		Path result = path;
		for (final String component : components) {
			result = result.resolve(component);
		}
		return result;
	}


	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof RelativePath)) return false;
		return Arrays.equals(components, ((RelativePath) object).components);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(components);
	}

	@Override
	public String toString() {
		return String.join("/", components);
	}

}
