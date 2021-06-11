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

package mrmathami.cia.java.jdt.project.builder.parameter;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.project.JavaSourceFileType;

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

	@Nonnull private final Path path;
	@Nonnull private final Map<String, InputModule> map = new TreeMap<>();


	public BuildInputSources(@Nonnull Path path) throws IOException {
		this.path = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
	}


	@Nonnull
	public Path getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Iterator<InputModule> iterator() {
		return map.values().iterator();
	}

	@Nonnull
	@Override
	public Spliterator<InputModule> spliterator() {
		return map.values().spliterator();
	}

	@Nonnull
	public InputModule createModule(@Nonnull String name, @Nonnull Path path) throws IOException {
		if (map.containsKey(name)) throw new IllegalArgumentException("Module \"" + name + "\" already exist!");
		final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
		if (!realPath.startsWith(this.path)) throw new IOException("Input path must start with project path!");
		final InputModule module = new InputModule(name, realPath);
		map.put(name, module);
		return module;
	}

	@Nullable
	public InputModule getModule(@Nonnull String name) {
		return map.get(name);
	}

	@Nonnull
	public InputModule removeModule(@Nonnull String name) {
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

		@Nonnull private final String name;
		@Nonnull private final Path path;
		@Nonnull private final Map<Path, InputSourceFile> map = new TreeMap<>();


		private InputModule(@Nonnull String name, @Nonnull Path path) {
			this.name = name;
			this.path = path;
		}


		@Nonnull
		public String getName() {
			return name;
		}

		@Nonnull
		public Path getPath() {
			return path;
		}

		@Nonnull
		@Override
		public Iterator<InputSourceFile> iterator() {
			return map.values().iterator();
		}

		@Nonnull
		@Override
		public Spliterator<InputSourceFile> spliterator() {
			return map.values().spliterator();
		}

		@Nonnull
		public InputSourceFile createFile(@Nonnull Path path, @Nonnull JavaSourceFileType type) throws IOException {
			final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
			if (map.containsKey(realPath)) throw new IllegalArgumentException("");
			if (!realPath.startsWith(this.path)) throw new IOException("Input path must start with module path!");
			final InputSourceFile file = new InputSourceFile(realPath, type);
			map.put(realPath, file);
			return file;
		}

		@Nullable
		public InputSourceFile getFile(@Nonnull Path path) throws IOException {
			return map.get(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
		}

		@Nonnull
		public InputSourceFile removeFile(@Nonnull Path path) throws IOException {
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

		@Nonnull private final Path path;
		@Nonnull private final JavaSourceFileType type;


		private InputSourceFile(@Nonnull Path path, @Nonnull JavaSourceFileType type) {
			this.path = path;
			this.type = type;
		}

		@Nonnull
		public Path getPath() {
			return path;
		}

		@Nonnull
		public JavaSourceFileType getType() {
			return type;
		}

	}

}
