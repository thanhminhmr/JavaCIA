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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class RelativePath implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] components;
	private transient @Nullable List<@NotNull String> componentList;


	public RelativePath(@NotNull String @NotNull [] components) {
		this.components = components;
	}


	public static @NotNull RelativePath fromPath(@NotNull Path path) {
		return new RelativePath(
				StreamSupport.stream(path.spliterator(), false)
						.map(Path::toString)
						.toArray(String[]::new)
		);
	}

	public static @NotNull RelativePath concat(@NotNull RelativePath pathA, @NotNull RelativePath pathB) {
		return pathA.length() == 0 ? pathB : pathB.length() == 0 ? pathA : new RelativePath(
				Stream.concat(Arrays.stream(pathA.components), Arrays.stream(pathB.components))
						.toArray(String[]::new)
		);
	}


	public int length() {
		return components.length;
	}

	public @NotNull String getComponent(int index) {
		final int length = components.length;
		if (index < -length || index >= length) throw new IndexOutOfBoundsException();
		return index >= 0 ? components[index] : components[length + index];
	}

	public @NotNull List<@NotNull String> getComponents() {
		return componentList != null ? componentList : (this.componentList = List.of(components));
	}

	public @NotNull String @NotNull [] toArray() {
		return components.clone();
	}

	public @NotNull Path appendToPath(@NotNull Path path) {
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
	public @NotNull String toString() {
		return String.join("/", components);
	}
}
