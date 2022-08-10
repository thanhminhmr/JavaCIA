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
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshotComparison;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import io.github.thanhminhmr.javacia.utils.ImmutableOrderedSet;
import io.github.thanhminhmr.javacia.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Set;

public final class ProjectSnapshotComparison implements JavaProjectSnapshotComparison, Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull String name;
	private final @NotNull JavaProjectSnapshot previousSnapshot;
	private final @NotNull JavaProjectSnapshot currentSnapshot;
	private final @NotNull Set<@NotNull JavaNode> removedNodes;
	private final @NotNull Set<@NotNull JavaNode> addedNodes;
	private final @NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> changedNodes;
	private final @NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> unchangedNodes;
	private final double @NotNull [] dependencyImpacts;
	private final double @NotNull [] nodeImpacts;

	private @Nullable DependencyWeightTable dependencyImpactMap;
	private @Nullable  NodeWeightTable nodeImpactTable;


	public ProjectSnapshotComparison(@NotNull String name,
			@NotNull JavaProjectSnapshot previousSnapshot, @NotNull JavaProjectSnapshot currentSnapshot,
			@NotNull Set<@NotNull JavaNode> removedNodes, @NotNull Set<@NotNull JavaNode> addedNodes,
			@NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> changedNodes,
			@NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> unchangedNodes,
			double @NotNull [] dependencyImpacts, double @NotNull [] nodeImpacts) {
		this.name = name;
		this.previousSnapshot = previousSnapshot;
		this.currentSnapshot = currentSnapshot;
		this.removedNodes = ImmutableOrderedSet.copyOf(removedNodes);
		this.addedNodes = ImmutableOrderedSet.copyOf(addedNodes);
		this.changedNodes = ImmutableOrderedSet.copyOf(changedNodes);
		this.unchangedNodes = ImmutableOrderedSet.copyOf(unchangedNodes);
		this.dependencyImpacts = dependencyImpacts;
		this.nodeImpacts = nodeImpacts;
	}


	//region Getter

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @NotNull JavaProjectSnapshot getPreviousSnapshot() {
		return previousSnapshot;
	}

	@Override
	public @NotNull JavaProjectSnapshot getCurrentSnapshot() {
		return currentSnapshot;
	}

	@Override
	public @NotNull Set<@NotNull JavaNode> getRemovedNodes() {
		return removedNodes;
	}

	@Override
	public @NotNull Set<@NotNull JavaNode> getAddedNodes() {
		return addedNodes;
	}

	@Override
	public @NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> getChangedNodes() {
		return changedNodes;
	}

	@Override
	public @NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> getUnchangedNodes() {
		return unchangedNodes;
	}

	@Override
	public @NotNull DependencyWeightTable getDependencyImpactTable() {
		return dependencyImpactMap != null ? dependencyImpactMap
				: (this.dependencyImpactMap = new DependencyWeightTable(dependencyImpacts));
	}

	@Override
	public @NotNull NodeWeightTable getNodeImpactTable() {
		return nodeImpactTable != null ? nodeImpactTable
				: (this.nodeImpactTable = new NodeWeightTable(nodeImpacts, currentSnapshot.getRootNode()));
	}

	//endregion Getter

}
