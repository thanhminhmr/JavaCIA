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

package mrmathami.cia.java.jdt.project.builder;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.Module;
import mrmathami.cia.java.jdt.project.ProjectSnapshot;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.project.builder.parameter.BuildInputSources;
import mrmathami.cia.java.jdt.project.builder.parameter.JavaBuildParameter;
import mrmathami.cia.java.jdt.project.builder.parameter.SnapshotBuildParameter;
import mrmathami.cia.java.project.JavaSourceFileType;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.cia.java.utils.RelativePath;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class SnapshotBuilder {

	private SnapshotBuilder() {
	}


	@Nonnull
	private static double[] calculateWeights(@Nonnull double[] dependencyWeights,
			@Nonnull List<? extends JavaNode> allNodes) {
		final double[] nodeWeights = new double[allNodes.size()];
		for (final JavaNode node : allNodes) {
			double nodeWeight = 0.0;
			for (final JavaDependencyCountTable nodeDependency : node.getDependencyFrom().values()) {
				for (final JavaDependency dependency : JavaDependency.VALUE_LIST) {
					nodeWeight += dependencyWeights[dependency.ordinal()]
							* nodeDependency.getCount(dependency);
				}
			}
			nodeWeights[node.getId()] = nodeWeight;
		}
		return nodeWeights;
	}

	@Nonnull
	private static Map<String, SourceFile> createJavaSourceFileMap(@Nonnull BuildInputSources inputSources) {
		final Map<String, SourceFile> sourceFileMap = new TreeMap<>();
		final Path sourcesPath = inputSources.getPath();
		for (final BuildInputSources.InputModule inputModule : inputSources) {
			final Path modulePath = inputModule.getPath();
			final Module module = new Module(inputModule.getName(),
					RelativePath.fromPath(sourcesPath.relativize(modulePath)));
			for (final BuildInputSources.InputSourceFile inputSourceFile : inputModule) {
				if (inputSourceFile.getType() == JavaSourceFileType.JAVA) {
					final Path sourceFilePath = inputSourceFile.getPath();
					final SourceFile sourceFile = new SourceFile(module, inputSourceFile.getType(),
							RelativePath.fromPath(modulePath.relativize(sourceFilePath)));
					sourceFileMap.put(sourceFilePath.toString(), sourceFile);
				}
			}
		}
		return sourceFileMap;
	}

	@Nonnull
	public static ProjectSnapshot build(@Nonnull String snapshotName,
			@Nonnull JavaDependencyWeightTable dependencyWeightTable, @Nonnull BuildInputSources inputSources,
			@Nonnull Set<SnapshotBuildParameter> parameters) throws JavaCiaException {

		final Map<String, SourceFile> sourceFileMap = createJavaSourceFileMap(inputSources);

		final JavaBuildParameter javaParameter = getParameter(parameters, JavaBuildParameter.class);
		final List<Path> classPaths = javaParameter != null ? javaParameter.getClassPaths() : List.of();
		final boolean recoveryEnabled = javaParameter == null || javaParameter.isRecoveryEnabled();

		final String[] sourcePathArray = sourceFileMap.keySet().toArray(String[]::new);
		final String[] sourceEncodingArray = new String[sourcePathArray.length];
		Arrays.fill(sourceEncodingArray, StandardCharsets.UTF_8.name());
		final String[] classPathArray = Stream.concat(
				StreamSupport.stream(inputSources.spliterator(), false).map(BuildInputSources.InputModule::getPath),
				classPaths.stream()
		).map(Object::toString).toArray(String[]::new);

		final JavaRootNode rootNode = JavaParser.parse(sourcePathArray, sourceEncodingArray, classPathArray,
				sourceFileMap, recoveryEnabled);

		final double[] dependencyWeights = new double[JavaDependency.VALUE_LIST.size()];
		for (final JavaDependency type : JavaDependency.VALUE_LIST) {
			dependencyWeights[type.ordinal()] = dependencyWeightTable.getWeight(type);
		}

		return new ProjectSnapshot(snapshotName, rootNode, dependencyWeights,
				calculateWeights(dependencyWeights, rootNode.getAllNodes()));
	}

	@Nullable
	private static <E extends SnapshotBuildParameter> E getParameter(@Nonnull Set<SnapshotBuildParameter> parameters,
			@Nonnull Class<E> parameterClass) {
		return parameters.stream()
				.filter(parameter -> parameter.getClass() == parameterClass)
				.map(parameterClass::cast)
				.findFirst().orElse(null);
	}
}
