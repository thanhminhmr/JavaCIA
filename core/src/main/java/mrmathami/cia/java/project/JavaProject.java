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

package mrmathami.cia.java.project;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Triple;

import java.nio.file.Path;
import java.util.List;

public interface JavaProject {

	@Nonnull
	String getName();

	@Nonnull
	List<? extends JavaProjectSnapshot> getSnapshots();

	@Nonnull
	List<? extends JavaProjectSnapshotComparison> getSnapshotComparisons();

	/**
	 * Create a snapshot of the current project.
	 *
	 * @param snapshotName          snapshot name
	 * @param javaSources           a list of module name, its root folder and list of its source files
	 * @param classPaths            list of dependency libraries
	 * @param dependencyWeightTable dependency weight table
	 * @param enableRecovery        create unknown types and skip unknown method calls
	 * @return the snapshot
	 * @throws JavaCiaException some error occur during the creation of the snapshot
	 */
	@Nonnull
	JavaProjectSnapshot createSnapshot(@Nonnull String snapshotName, @Nonnull Path projectRoot,
			@Nonnull List<Triple<String, Path, List<Path>>> javaSources, @Nonnull List<Path> classPaths,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable, boolean enableRecovery) throws JavaCiaException;

	/**
	 * @param comparisonName    comparison name
	 * @param previousSnapshot  the old snapshot
	 * @param currentSnapshot   the new snapshot
	 * @param impactWeightTable impact weight table
	 * @return the comparison
	 * @throws JavaCiaException some error occur during the creation of the comparison
	 */
	@Nonnull
	JavaProjectSnapshotComparison createSnapshotComparison(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException;

	boolean containsSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot);

	boolean containsSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison);

	boolean addSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot);

	boolean addSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison);

	boolean removeSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot);

	boolean removeSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison);

}
