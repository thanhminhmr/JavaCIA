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
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.jdt.tree.dependency.DependencyWeightTable;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotModification;
import mrmathami.collections.ImmutableOrderedSet;
import mrmathami.utils.Pair;

import java.io.Serializable;
import java.util.Set;

public final class ProjectSnapshotModification implements JavaProjectSnapshotModification, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String name;
	@Nonnull private final JavaProjectSnapshot previousSnapshot;
	@Nonnull private final JavaProjectSnapshot currentSnapshot;
	@Nonnull private final Set<JavaNode> removedNodes;
	@Nonnull private final Set<JavaNode> addedNodes;
	@Nonnull private final Set<Pair<JavaNode, JavaNode>> changedNodes;
	@Nonnull private final Set<Pair<JavaNode, JavaNode>> unchangedNodes;
	@Nonnull private final double[] dependencyImpacts;
	@Nonnull private final double[] nodeImpacts;

	@Nullable private DependencyWeightTable dependencyImpactMap;
	@Nullable private NodeWeightTable nodeImpactTable;


	public ProjectSnapshotModification(@Nonnull String name,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull Set<JavaNode> removedNodes, @Nonnull Set<JavaNode> addedNodes,
			@Nonnull Set<Pair<JavaNode, JavaNode>> changedNodes,
			@Nonnull Set<Pair<JavaNode, JavaNode>> unchangedNodes,
			@Nonnull double[] dependencyImpacts, @Nonnull double[] nodeImpacts) {
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

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public JavaProjectSnapshot getPreviousSnapshot() {
		return previousSnapshot;
	}

	@Nonnull
	@Override
	public JavaProjectSnapshot getCurrentSnapshot() {
		return currentSnapshot;
	}

	@Nonnull
	@Override
	public Set<JavaNode> getRemovedNodes() {
		return removedNodes;
	}

	@Nonnull
	@Override
	public Set<JavaNode> getAddedNodes() {
		return addedNodes;
	}

	@Nonnull
	@Override
	public Set<Pair<JavaNode, JavaNode>> getChangedNodes() {
		return changedNodes;
	}

	@Nonnull
	@Override
	public Set<Pair<JavaNode, JavaNode>> getUnchangedNodes() {
		return unchangedNodes;
	}

	@Nonnull
	@Override
	public DependencyWeightTable getDependencyImpactTable() {
		return dependencyImpactMap != null ? dependencyImpactMap
				: (this.dependencyImpactMap = new DependencyWeightTable(dependencyImpacts));
	}

	@Nonnull
	@Override
	public NodeWeightTable getNodeImpactTable() {
		return nodeImpactTable != null ? nodeImpactTable
				: (this.nodeImpactTable = new NodeWeightTable(nodeImpacts, currentSnapshot.getRootNode()));
	}

	//endregion Getter

}
