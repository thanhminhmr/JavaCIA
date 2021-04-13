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

package mrmathami.cia.java.jdt.project;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.builder.JavaSnapshotBuilder;
import mrmathami.cia.java.jdt.project.differ.JavaSnapshotComparator;
import mrmathami.cia.java.project.JavaProject;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Triple;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class Project implements JavaProject, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String name;
	@Nonnull private final List<ProjectSnapshot> snapshots = new LinkedList<>();
	@Nonnull private final List<ProjectSnapshotComparison> snapshotComparisons = new LinkedList<>();


	public Project(@Nonnull String name) {
		this.name = name;
	}


	private static void checkSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot) {
		if (projectSnapshot instanceof ProjectSnapshot) return;
		throw new IllegalArgumentException("Input project snapshot is not JDT based.");
	}

	private static void checkComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison) {
		if (snapshotComparison instanceof ProjectSnapshotComparison) return;
		throw new IllegalArgumentException("Input project snapshot comparison is not JDT based.");
	}


	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public List<ProjectSnapshot> getSnapshots() {
		return Collections.unmodifiableList(snapshots);
	}

	@Nonnull
	@Override
	public List<ProjectSnapshotComparison> getSnapshotComparisons() {
		return Collections.unmodifiableList(snapshotComparisons);
	}

	@Nonnull
	@Override
	public ProjectSnapshot createSnapshot(@Nonnull String snapshotName, @Nonnull Path projectRoot,
			@Nonnull List<Triple<String, Path, List<Path>>> javaSources, @Nonnull List<Path> classPaths,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable, boolean enableRecovery) throws JavaCiaException {
		final ProjectSnapshot snapshot = JavaSnapshotBuilder.build(
				snapshotName, projectRoot, javaSources, classPaths, dependencyWeightTable, enableRecovery);
		snapshots.add(snapshot);
		return snapshot;
	}

	@Nonnull
	@Override
	public ProjectSnapshotComparison createSnapshotComparison(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException {
		checkSnapshot(previousSnapshot);
		checkSnapshot(currentSnapshot);
		final ProjectSnapshotComparison comparison = JavaSnapshotComparator.compare(
				comparisonName, previousSnapshot, currentSnapshot, impactWeightTable);
		snapshotComparisons.add(comparison);
		return comparison;
	}

	public boolean containsSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot) {
		checkSnapshot(projectSnapshot);
		//noinspection SuspiciousMethodCalls
		return snapshots.contains(projectSnapshot);
	}

	@Override
	public boolean containsSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison) {
		checkComparison(snapshotComparison);
		//noinspection SuspiciousMethodCalls
		return snapshotComparisons.contains(snapshotComparison);
	}

	@Override
	public boolean addSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot) {
		if (containsSnapshot(projectSnapshot)) return false;
		snapshots.add((ProjectSnapshot) projectSnapshot);
		return true;
	}

	@Override
	public boolean addSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison) {
		if (containsSnapshotComparison(snapshotComparison)) return false;
		final JavaProjectSnapshot previousSnapshot = snapshotComparison.getPreviousSnapshot();
		final JavaProjectSnapshot currentSnapshot = snapshotComparison.getCurrentSnapshot();
		final boolean containsPreviousSnapshot = containsSnapshot(previousSnapshot);
		final boolean containsCurrentSnapshot = containsSnapshot(currentSnapshot);
		if (!containsPreviousSnapshot) snapshots.add((ProjectSnapshot) previousSnapshot);
		if (!containsCurrentSnapshot) snapshots.add((ProjectSnapshot) currentSnapshot);
		snapshotComparisons.add((ProjectSnapshotComparison) snapshotComparison);
		return false;
	}

	@Override
	public boolean removeSnapshot(@Nonnull JavaProjectSnapshot projectSnapshot) {
		if (!containsSnapshot(projectSnapshot)) return false;
		for (final ProjectSnapshotComparison snapshotComparison : snapshotComparisons) {
			if (snapshotComparison.getPreviousSnapshot().equals(projectSnapshot)
					|| snapshotComparison.getCurrentSnapshot().equals(projectSnapshot)) {
				return false;
			}
		}
		//noinspection SuspiciousMethodCalls
		return snapshots.remove(projectSnapshot);
	}

	@Override
	public boolean removeSnapshotComparison(@Nonnull JavaProjectSnapshotComparison snapshotComparison) {
		checkComparison(snapshotComparison);
		//noinspection SuspiciousMethodCalls
		return snapshotComparisons.remove(snapshotComparison);
	}

}
