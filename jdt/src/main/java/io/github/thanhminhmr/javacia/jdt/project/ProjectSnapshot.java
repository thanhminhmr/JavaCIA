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

import io.github.thanhminhmr.javacia.jdt.tree.dependency.DependencyWeightTable;
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshot;
import io.github.thanhminhmr.javacia.tree.node.JavaRootNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public final class ProjectSnapshot implements JavaProjectSnapshot, Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull String name;
	private final @NotNull JavaRootNode rootNode;
	private final double @NotNull [] dependencyWeights;
	private final double @NotNull [] nodeWeights;

	private transient @Nullable DependencyWeightTable dependencyWeightTable;
	private transient @Nullable NodeWeightTable nodeWeightTable;


	public ProjectSnapshot(@NotNull String name, @NotNull JavaRootNode rootNode,
			double @NotNull [] dependencyWeights, double @NotNull [] nodeWeights) {
		this.name = name;
		this.rootNode = rootNode;
		this.dependencyWeights = dependencyWeights;
		this.nodeWeights = nodeWeights;
	}


	//region Getter

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @NotNull JavaRootNode getRootNode() {
		return rootNode;
	}

	@Override
	public @NotNull DependencyWeightTable getDependencyWeightTable() {
		return dependencyWeightTable != null ? dependencyWeightTable
				: (this.dependencyWeightTable = new DependencyWeightTable(dependencyWeights));
	}

	@Override
	public @NotNull NodeWeightTable getNodeWeightTable() {
		return nodeWeightTable != null ? nodeWeightTable
				: (this.nodeWeightTable = new NodeWeightTable(nodeWeights, rootNode));
	}

	//endregion Getter

}
