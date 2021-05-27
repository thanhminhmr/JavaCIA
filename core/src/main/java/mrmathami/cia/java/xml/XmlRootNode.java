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
import mrmathami.cia.java.xml.attribute.XmlAttributedNode;

import java.util.NoSuchElementException;

public interface XmlRootNode extends XmlNode, XmlAttributedNode {

	@Nonnull String OBJECT_CLASS = "XmlRootNode";


	//region Basic Getter

	@Nonnull
	@Override
	default String getEntityClass() {
		return OBJECT_CLASS;
	}

	//endregion Basic Getter

	//region Getter & Setter

	@Override
	default boolean isXmlRoot() {
		return true;
	}

	@Nonnull
	@Override
	default XmlNode getXmlParent() {
		throw new NoSuchElementException("XmlRootNode does not have a XML parent.");
	}

	@Nonnull
	@Override
	default XmlRootNode getXmlRoot() {
		return this;
	}

	//endregion Getter & Setter

}
