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

import io.github.thanhminhmr.javacia.project.JavaProject;
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshot;
import io.github.thanhminhmr.javacia.project.JavaProjectSnapshotComparison;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class Project implements JavaProject, Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull String name;
	private final @NotNull List<@NotNull ProjectSnapshot> snapshots = new LinkedList<>();
	private final @NotNull List<@NotNull ProjectSnapshotComparison> snapshotComparisons = new LinkedList<>();


	public Project(@NotNull String name) {
		this.name = name;
	}


	private static void checkSnapshot(@NotNull JavaProjectSnapshot projectSnapshot) {
		if (projectSnapshot instanceof ProjectSnapshot) return;
		throw new IllegalArgumentException("Input project snapshot is not JDT based.");
	}

	private static void checkComparison(@NotNull JavaProjectSnapshotComparison snapshotComparison) {
		if (snapshotComparison instanceof ProjectSnapshotComparison) return;
		throw new IllegalArgumentException("Input project snapshot comparison is not JDT based.");
	}


	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @NotNull List<@NotNull ProjectSnapshot> getSnapshots() {
		return Collections.unmodifiableList(snapshots);
	}

	@Override
	public @NotNull List<@NotNull ProjectSnapshotComparison> getSnapshotComparisons() {
		return Collections.unmodifiableList(snapshotComparisons);
	}

	public boolean containsSnapshot(@NotNull JavaProjectSnapshot projectSnapshot) {
		checkSnapshot(projectSnapshot);
		//noinspection SuspiciousMethodCalls
		return snapshots.contains(projectSnapshot);
	}

	@Override
	public boolean containsSnapshotComparison(@NotNull JavaProjectSnapshotComparison snapshotComparison) {
		checkComparison(snapshotComparison);
		//noinspection SuspiciousMethodCalls
		return snapshotComparisons.contains(snapshotComparison);
	}

	@Override
	public boolean addSnapshot(@NotNull JavaProjectSnapshot projectSnapshot) {
		if (containsSnapshot(projectSnapshot)) return false;
		snapshots.add((ProjectSnapshot) projectSnapshot);
		return true;
	}

	@Override
	public boolean addSnapshotComparison(@NotNull JavaProjectSnapshotComparison snapshotComparison) {
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
	public boolean removeSnapshot(@NotNull JavaProjectSnapshot projectSnapshot) {
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
	public boolean removeSnapshotComparison(@NotNull JavaProjectSnapshotComparison snapshotComparison) {
		checkComparison(snapshotComparison);
		//noinspection SuspiciousMethodCalls
		return snapshotComparisons.remove(snapshotComparison);
	}

}
