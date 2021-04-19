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

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.project.JavaModule;
import mrmathami.cia.java.project.JavaSourceFile;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.node.container.JavaAnnotationContainer;
import mrmathami.cia.java.tree.node.container.JavaClassContainer;
import mrmathami.cia.java.tree.node.container.JavaEnumContainer;
import mrmathami.cia.java.tree.node.container.JavaInterfaceContainer;
import mrmathami.cia.java.tree.node.container.JavaPackageContainer;
import mrmathami.cia.java.tree.type.JavaType;
import mrmathami.cia.java.xml.JavaXmlContainer;

import java.util.List;
import java.util.NoSuchElementException;

public interface JavaRootNode extends JavaNode,
		JavaPackageContainer, JavaAnnotationContainer, JavaClassContainer,
		JavaEnumContainer, JavaInterfaceContainer, JavaXmlContainer {

	@Nonnull String OBJECT_CLASS = "JavaRootNode";


	//region Basic Getter

	@Nonnull
	@Override
	default String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Override
	default boolean isRoot() {
		return true;
	}

	@Nonnull
	@Override
	default JavaRootNode getRoot() {
		return this;
	}

	@Nonnull
	@Override
	default JavaNode getParent() {
		throw new NoSuchElementException("JavaRootNode does not have a parent.");
	}

	@Nonnull
	@Override
	default String getSimpleName() {
		return "{ROOT}";
	}

	@Nonnull
	@Override
	default String getQualifiedName() {
		return "{ROOT}";
	}

	@Nonnull
	@Override
	default String getUniqueName() {
		return "{ROOT}";
	}

	@Nullable
	@Override
	default JavaSourceFile getSourceFile() {
		return null;
	}

	@Nullable
	@Override
	default JavaModule getModule() {
		return null;
	}

	//endregion Basic Getter

	//region Node Type

	@Nonnull
	@Override
	default JavaRootNode asRootNode() {
		return this;
	}

	//endregion Node Type

	//region Getter & Setter

	@Nonnull
	List<? extends JavaNode> getAllNodes();

	@Nonnull
	List<? extends JavaType> getAllTypes();

	@Nonnull
	List<? extends JavaAnnotate> getAllAnnotates();

	//endregion Getter & Setter

}
