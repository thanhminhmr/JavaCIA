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

import mrmathami.cia.java.project.JavaProjectBuilder;

module mrmathami.cia.java.core {
	requires transitive mrmathami.utils;

	exports mrmathami.cia.java;
	exports mrmathami.cia.java.tree;
	exports mrmathami.cia.java.tree.annotate;
	exports mrmathami.cia.java.tree.helper;
	exports mrmathami.cia.java.tree.node;
	exports mrmathami.cia.java.tree.node.attribute;
	exports mrmathami.cia.java.tree.node.container;
	exports mrmathami.cia.java.tree.dependency;
	exports mrmathami.cia.java.tree.type;
	exports mrmathami.cia.java.project;

	uses JavaProjectBuilder;
}