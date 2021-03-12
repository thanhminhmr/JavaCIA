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

package mrmathami.cia.java.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;
import mrmathami.cia.java.tree.node.container.*;
import mrmathami.cia.java.tree.type.JavaType;

import java.util.List;
import java.util.NoSuchElementException;

public interface JavaRootNode extends JavaNode,
		JavaPackageContainer, JavaAnnotationContainer, JavaClassContainer,
		JavaEnumContainer, JavaInterfaceContainer, JavaXMLContainer {

	@Nonnull String OBJECT_CLASS = "JavaRootNode";


	//region Basic Getter

	@Nonnull
	@Override
	default String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Nonnull
	@Override
	default JavaRootNode asRootNode() {
		return this;
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
	default String getNodeName() {
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

	//endregion Basic Getter

	//region Getter & Setter

	@Nonnull
	List<? extends JavaNode> getAllNodes();

	@Nonnull
	List<? extends JavaType> getAllTypes();

	@Nonnull
	List<? extends JavaAnnotate> getAllAnnotates();

	//endregion Getter & Setter

}
