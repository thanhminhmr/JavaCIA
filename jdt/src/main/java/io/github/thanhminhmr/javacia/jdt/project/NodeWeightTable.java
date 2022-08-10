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

package io.github.thanhminhmr.javacia.jdt.project;

import io.github.thanhminhmr.javacia.project.JavaNodeWeightTable;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import io.github.thanhminhmr.javacia.tree.node.JavaRootNode;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public final class NodeWeightTable implements JavaNodeWeightTable, Serializable {

	private static final long serialVersionUID = -1L;

	private final double @NotNull [] weights;
	private final @NotNull JavaRootNode rootNode;

	public NodeWeightTable(double @NotNull [] weights, @NotNull JavaRootNode rootNode) {
		assert weights.length == rootNode.getAllNodes().size();

		this.weights = weights;
		this.rootNode = rootNode;
	}

	@Override
	public double getWeight(@NotNull JavaNode javaNode) {
		if (javaNode.getRoot() == rootNode) return weights[javaNode.getId()];
		throw new IllegalArgumentException("Input JavaNode doesn't exist in this tree!");
	}

}
