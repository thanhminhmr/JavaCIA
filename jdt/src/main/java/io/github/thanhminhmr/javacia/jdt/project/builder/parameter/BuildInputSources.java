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

package io.github.thanhminhmr.javacia.jdt.project.builder.parameter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.TreeMap;

public final class BuildInputSources implements Iterable<BuildInputSources.InputModule>, Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull Path path;
	private final @NotNull Map<String, InputModule> map = new TreeMap<>();


	public BuildInputSources(@NotNull Path path) throws IOException {
		this.path = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
	}


	public @NotNull Path getPath() {
		return path;
	}

	@Override
	public @NotNull Iterator<@NotNull InputModule> iterator() {
		return map.values().iterator();
	}

	@Override
	public @NotNull Spliterator<@NotNull InputModule> spliterator() {
		return map.values().spliterator();
	}

	public @NotNull InputModule createModule(@NotNull String name, @NotNull Path path) throws IOException {
		if (map.containsKey(name)) throw new IllegalArgumentException("Module \"" + name + "\" already exist!");
		final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
		if (!realPath.startsWith(this.path)) throw new IOException("Input path must start with project path!");
		final InputModule module = new InputModule(name, realPath);
		map.put(name, module);
		return module;
	}

	public @Nullable InputModule getModule(@NotNull String name) {
		return map.get(name);
	}

	public @NotNull InputModule removeModule(@NotNull String name) {
		return map.remove(name);
	}

	public int size() {
		return map.size();
	}

	public void clear() {
		map.clear();
	}


	public static final class InputModule implements Iterable<InputSourceFile>, Serializable {

		private static final long serialVersionUID = -1L;

		private final @NotNull String name;
		private final @NotNull Path path;
		private final @NotNull Map<@NotNull Path, @NotNull InputSourceFile> map = new TreeMap<>();


		private InputModule(@NotNull String name, @NotNull Path path) {
			this.name = name;
			this.path = path;
		}


		public @NotNull String getName() {
			return name;
		}

		public @NotNull Path getPath() {
			return path;
		}

		@Override
		public @NotNull Iterator<@NotNull InputSourceFile> iterator() {
			return map.values().iterator();
		}

		@Override
		public @NotNull Spliterator<@NotNull InputSourceFile> spliterator() {
			return map.values().spliterator();
		}

		public @NotNull InputSourceFile createFile(@NotNull Path path) throws IOException {
			final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
			if (map.containsKey(realPath)) throw new IllegalArgumentException("");
			if (!realPath.startsWith(this.path)) throw new IOException("Input path must start with module path!");
			final InputSourceFile file = new InputSourceFile(realPath);
			map.put(realPath, file);
			return file;
		}

		public @Nullable InputSourceFile getFile(@NotNull Path path) throws IOException {
			return map.get(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
		}

		public @NotNull InputSourceFile removeFile(@NotNull Path path) throws IOException {
			return map.remove(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
		}

		public int size() {
			return map.size();
		}

		public void clear() {
			map.clear();
		}

	}

	public static final class InputSourceFile implements Serializable {

		private static final long serialVersionUID = -1L;

		private final @NotNull Path path;


		private InputSourceFile(@NotNull Path path) {
			this.path = path;
		}


		public @NotNull Path getPath() {
			return path;
		}

	}

}
