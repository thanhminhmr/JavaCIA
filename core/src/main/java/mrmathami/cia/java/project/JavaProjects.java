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

package mrmathami.cia.java.project;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class JavaProjects {

	@Nullable private static final JavaProjectBuilder INSTANCE = loadInstance();


	@Nullable
	private static JavaProjectBuilder loadInstance() {
		for (final JavaProjectBuilder builder : ServiceLoader.load(JavaProjectBuilder.class)) {
			return builder;
		}
		return null;
	}

	@Nonnull
	private static JavaProjectBuilder getInstance() throws JavaCiaException {
		if (INSTANCE != null) return INSTANCE;
		throw new JavaCiaException("Cannot find an implementation for JavaProjectBuilder!");
	}


	@Nonnull
	public static JavaProjectSnapshot createProjectSnapshot(@Nonnull String snapshotName,
			@Nonnull Map<Path, List<Path>> javaSources, @Nonnull List<Path> classPaths,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable) throws JavaCiaException {
		return getInstance().createProjectSnapshot(snapshotName, javaSources, classPaths, dependencyWeightTable);
	}

	@Nonnull
	public static JavaProjectSnapshotModification createSnapshotModifications(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightTable) throws JavaCiaException {
		return getInstance()
				.createSnapshotModifications(comparisonName, previousSnapshot, currentSnapshot, impactWeightTable);
	}

	@Nonnull
	public static JavaProject createProject(@Nonnull String projectName,
			@Nonnull List<? extends JavaProjectSnapshot> snapshots,
			@Nonnull List<? extends JavaProjectSnapshotModification> modifications) throws JavaCiaException {
		return getInstance().createProject(projectName, snapshots, modifications);
	}


	private JavaProjects() {
	}

}
