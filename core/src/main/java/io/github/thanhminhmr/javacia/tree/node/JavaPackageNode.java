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

package io.github.thanhminhmr.javacia.tree.node;

import io.github.thanhminhmr.javacia.project.JavaModule;
import io.github.thanhminhmr.javacia.project.JavaSourceFile;
import io.github.thanhminhmr.javacia.tree.node.attribute.JavaAnnotatedNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JavaPackageNode extends JavaNode,
		JavaAnnotatedNode {

	@NotNull String OBJECT_CLASS = "JavaPackageNode";


	//region Basic Getter

	@Override
	default @NotNull String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Override
	default @Nullable JavaSourceFile getSourceFile() {
		return null;
	}

	@Override
	default @Nullable JavaModule getModule() {
		return null;
	}

	//endregion Basic Getter

}
