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

package mrmathami.cia.java.tree.node;

import mrmathami.cia.java.project.JavaModule;
import mrmathami.cia.java.project.JavaSourceFile;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.type.JavaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

public interface JavaRootNode extends JavaNode {

	@NotNull String OBJECT_CLASS = "JavaRootNode";


	//region Basic Getter

	@Override
	default @NotNull String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Override
	default boolean isRoot() {
		return true;
	}

	@Override
	default @NotNull JavaRootNode getRoot() {
		return this;
	}

	@Override
	default @NotNull JavaNode getParent() {
		throw new NoSuchElementException("JavaRootNode does not have a parent.");
	}

	@Override
	default @NotNull String getSimpleName() {
		return "{ROOT}";
	}

	@Override
	default @NotNull String getQualifiedName() {
		return "{ROOT}";
	}

	@Override
	default @NotNull String getUniqueName() {
		return "{ROOT}";
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

	//region Getter & Setter

	@NotNull List<? extends JavaNode> getAllNodes();

	@NotNull List<? extends JavaType> getAllTypes();

	@NotNull List<? extends JavaAnnotate> getAllAnnotates();

	//endregion Getter & Setter

}
