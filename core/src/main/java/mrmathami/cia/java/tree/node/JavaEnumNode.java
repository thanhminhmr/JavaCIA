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

import mrmathami.cia.java.tree.node.attribute.JavaAnnotatedNode;
import mrmathami.cia.java.tree.node.attribute.JavaModifiedNode;
import mrmathami.cia.java.tree.node.attribute.JavaTypeNode;
import mrmathami.cia.java.tree.type.JavaType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JavaEnumNode extends JavaNode,
		JavaAnnotatedNode, JavaModifiedNode, JavaTypeNode {

	@NotNull String OBJECT_CLASS = "JavaEnumNode";


	//region Basic Getter

	@Override
	default @NotNull String getEntityClass() {
		return OBJECT_CLASS;
	}

	//endregion Basic Getter

	//region Getter & Setter

	@NotNull List<? extends JavaType> getImplementsInterfaces();

	//endregion Getter & Setter

}
