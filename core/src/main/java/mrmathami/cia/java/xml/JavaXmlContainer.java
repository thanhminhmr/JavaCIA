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

package mrmathami.cia.java.xml;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.node.JavaNode;

import java.util.List;

public interface JavaXmlContainer extends JavaNode {

	@Nonnull
	@Override
	default List<? extends JavaXmlNode> getChildXmls(@Nonnull List<JavaXmlNode> xmlNodes) {
		return getChildren(JavaXmlNode.class, xmlNodes);
	}

	@Nonnull
	@Override
	default List<? extends JavaXmlNode> getChildXmls() {
		return getChildren(JavaXmlNode.class);
	}

	@Nonnull
	@Override
	default JavaXmlContainer asXmlContainer() {
		return this;
	}

}
