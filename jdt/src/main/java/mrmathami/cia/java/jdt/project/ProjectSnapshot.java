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

package mrmathami.cia.java.jdt.project;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.cia.java.jdt.tree.dependency.DependencyWeightTable;
import mrmathami.cia.java.project.JavaProjectSnapshot;

import java.io.Serializable;

public final class ProjectSnapshot implements JavaProjectSnapshot, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String name;
	@Nonnull private final JavaRootNode rootNode;
	@Nonnull private final double[] dependencyWeights;
	@Nonnull private final double[] nodeWeights;

	@Nullable private transient DependencyWeightTable dependencyWeightMap;
	@Nullable private transient NodeWeightTable nodeWeightTable;


	public ProjectSnapshot(@Nonnull String name, @Nonnull JavaRootNode rootNode,
			@Nonnull double[] dependencyWeights, @Nonnull double[] nodeWeights) {
		this.name = name;
		this.rootNode = rootNode;
		this.dependencyWeights = dependencyWeights;
		this.nodeWeights = nodeWeights;
	}


	//region Getter

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public JavaRootNode getRootNode() {
		return rootNode;
	}

	@Nonnull
	@Override
	public DependencyWeightTable getDependencyWeightTable() {
		return dependencyWeightMap != null ? dependencyWeightMap
				: (this.dependencyWeightMap = new DependencyWeightTable(dependencyWeights));
	}

	@Nonnull
	@Override
	public NodeWeightTable getNodeWeightTable() {
		return nodeWeightTable != null ? nodeWeightTable
				: (this.nodeWeightTable = new NodeWeightTable(nodeWeights, rootNode));
	}

	//endregion Getter

}
