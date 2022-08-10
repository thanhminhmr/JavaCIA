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

package io.github.thanhminhmr.javacia.project;

import io.github.thanhminhmr.javacia.tree.dependency.JavaDependencyWeightTable;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import io.github.thanhminhmr.javacia.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface JavaProjectSnapshotComparison {

	@NotNull String getName();

	@NotNull JavaProjectSnapshot getPreviousSnapshot();

	@NotNull JavaProjectSnapshot getCurrentSnapshot();

	@NotNull Set<@NotNull JavaNode> getRemovedNodes();

	@NotNull Set<@NotNull JavaNode> getAddedNodes();

	@NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> getChangedNodes();

	@NotNull Set<@NotNull Pair<@NotNull JavaNode, @NotNull JavaNode>> getUnchangedNodes();

	@NotNull JavaDependencyWeightTable getDependencyImpactTable();

	@NotNull JavaNodeWeightTable getNodeImpactTable();

}
