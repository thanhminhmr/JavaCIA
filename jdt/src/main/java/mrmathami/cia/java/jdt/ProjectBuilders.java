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

package mrmathami.cia.java.jdt;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.Project;
import mrmathami.cia.java.jdt.project.builder.JavaSnapshotBuilder;
import mrmathami.cia.java.jdt.project.differ.JavaSnapshotComparator;
import mrmathami.cia.java.project.JavaProject;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Pair;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ProjectBuilders {

	private ProjectBuilders() {
	}

	@Nonnull
	public static JavaProject createProject(@Nonnull String name) {
		return new Project(name);
	}

	@Nonnull
	public static JavaProjectSnapshot createProjectSnapshot(@Nonnull String snapshotName,
			@Nonnull Map<String, Pair<Path, List<Path>>> javaSources, @Nonnull List<Path> classPaths,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable) throws JavaCiaException {
		return JavaSnapshotBuilder.build(snapshotName, javaSources, classPaths, dependencyWeightTable);
	}

	@Nonnull
	public static JavaProjectSnapshotComparison createProjectSnapshotComparison(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException {
		return JavaSnapshotComparator.compare(comparisonName, previousSnapshot, currentSnapshot, impactWeightTable);
	}


}
