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

package mrmathami.cia.java.jdt.project;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.project.JavaModule;
import mrmathami.cia.java.utils.RelativePath;

import java.io.Serializable;

public final class Module implements JavaModule, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String name;
	@Nonnull private final RelativePath relativePath;


	public Module(@Nonnull String name, @Nonnull RelativePath relativePath) {
		this.name = name;
		this.relativePath = relativePath;
	}


	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Override
	@Nonnull
	public RelativePath getRelativePath() {
		return relativePath;
	}

}
