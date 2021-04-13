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

package mrmathami.cia.java.project;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.utils.RelativePath;

public interface JavaSourceFile {

	@Nonnull
	JavaModule getModule();

	@Nonnull
	JavaSourceFileType getType();

	@Nonnull
	RelativePath getRelativePath();

	@Nonnull
	default String getName() {
		// note: if this throw IndexOutOfBound, something seriously wrong happened in JavaNodes
		return getRelativePath().getComponent(-1);
	}

}
