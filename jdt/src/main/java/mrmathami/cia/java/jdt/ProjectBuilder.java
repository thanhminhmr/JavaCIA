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

package mrmathami.cia.java.jdt;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.Project;
import mrmathami.cia.java.jdt.project.builder.parameter.BuildInputSources;
import mrmathami.cia.java.jdt.project.differ.JavaSnapshotComparator;
import mrmathami.cia.java.jdt.project.builder.SnapshotBuilder;
import mrmathami.cia.java.jdt.project.builder.parameter.SnapshotBuildParameter;
import mrmathami.cia.java.project.JavaProject;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;

import java.util.Set;

public final class ProjectBuilder {

	private ProjectBuilder() {
	}

	@Nonnull
	public static JavaProject createProject(@Nonnull String name) {
		return new Project(name);
	}

	@Nonnull
	public static JavaProjectSnapshot createProjectSnapshot(@Nonnull String snapshotName,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable, @Nonnull BuildInputSources inputSources,
			@Nonnull Set<SnapshotBuildParameter> parameters) throws JavaCiaException {
		return SnapshotBuilder.build(snapshotName, dependencyWeightTable, inputSources, parameters);
	}

	@Nonnull
	public static JavaProjectSnapshotComparison createProjectSnapshotComparison(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException {
		return JavaSnapshotComparator.compare(comparisonName, previousSnapshot, currentSnapshot, impactWeightTable);
	}

}
