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
import mrmathami.cia.java.tree.JavaEntity;
import mrmathami.cia.java.tree.node.container.JavaClassContainer;

import java.util.List;

public interface JavaInitializerNode extends JavaNode, JavaClassContainer {

	@Nonnull String OBJECT_CLASS = "JavaInitializerNode";


	//region Basic Getter

	@Nonnull
	@Override
	default String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Nonnull
	@Override
	default JavaInitializerNode asInitializerNode() {
		return this;
	}

	//endregion Basic Getter

	//region Getter & Setter

	boolean isStatic();

	@Nonnull
	List<? extends Initializer> getInitializers();

	//endregion Getter & Setter

	interface Initializer extends JavaEntity {
	}

	interface BlockInitializer extends Initializer {

		//region Basic Getter

		@Nonnull
		@Override
		default String getEntityClass() {
			return "JavaClassNode.BlockInitializer";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@Nonnull
		String getBodyBlock();

		//endregion Getter & Setter

	}

	interface FieldInitializer extends Initializer {

		//region Basic Getter

		@Nonnull
		@Override
		default String getEntityClass() {
			return "JavaClassNode.FieldInitializer";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@Nonnull
		JavaFieldNode getFieldNode();

		@Nonnull
		String getInitialExpression();

		//endregion Getter & Setter

	}

}
