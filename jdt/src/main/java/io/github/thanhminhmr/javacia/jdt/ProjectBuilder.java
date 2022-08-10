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

package io.github.thanhminhmr.javacia.jdt;

import io.github.thanhminhmr.javacia.JavaCiaException;
import io.github.thanhminhmr.javacia.jdt.project.Project;
import io.github.thanhminhmr.javacia.jdt.project.builder.SnapshotBuilder;
import io.github.thanhminhmr.javacia.jdt.project.builder.parameter.BuildInputSources;
import io.github.thanhminhmr.javacia.jdt.project.builder.parameter.SnapshotBuildParameter;
import io.github.thanhminhmr.javacia.jdt.project.differ.JavaSnapshotComparator;
import io.github.thanhminhmr.javacia.project.JavaProject;
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshot;
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshotComparison;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependencyWeightTable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ProjectBuilder {

	private ProjectBuilder() {
	}

	public static @NotNull JavaProject createProject(@NotNull String name) {
		return new Project(name);
	}

	public static @NotNull JavaProjectSnapshot createProjectSnapshot(@NotNull String snapshotName,
			@NotNull JavaDependencyWeightTable dependencyWeightTable, @NotNull BuildInputSources inputSources,
			@NotNull Set<@NotNull SnapshotBuildParameter> parameters) throws JavaCiaException {
		return SnapshotBuilder.build(snapshotName, dependencyWeightTable, inputSources, parameters);
	}

	public static @NotNull JavaProjectSnapshotComparison createProjectSnapshotComparison(@NotNull String comparisonName,
			@NotNull JavaProjectSnapshot previousSnapshot, @NotNull JavaProjectSnapshot currentSnapshot,
			@NotNull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException {
		return JavaSnapshotComparator.compare(comparisonName, previousSnapshot, currentSnapshot, impactWeightTable);
	}

}
