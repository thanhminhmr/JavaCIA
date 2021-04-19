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
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.ProjectSnapshot;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.utils.Triple;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class JavaSnapshotBuilder {

	private JavaSnapshotBuilder() {
	}


	@Nonnull
	public static ProjectSnapshot build(@Nonnull String snapshotName, @Nonnull Path projectRoot,
			@Nonnull List<Triple<String, Path, List<Path>>> javaSources, @Nonnull List<Path> classPaths,
			@Nonnull JavaDependencyWeightTable dependencyWeightMap, boolean enableRecovery,
			Path configurationPath) throws JavaCiaException, ParserConfigurationException, SAXException, IOException {

		final JavaRootNode rootNode = JavaSnapshotParser.build(projectRoot, javaSources, classPaths, enableRecovery, configurationPath);

		final double[] dependencyWeights = new double[JavaDependency.VALUE_LIST.size()];
		for (final JavaDependency type : JavaDependency.VALUE_LIST) {
			dependencyWeights[type.ordinal()] = dependencyWeightMap.getWeight(type);
		}

		return new ProjectSnapshot(snapshotName, rootNode, dependencyWeights,
				calculateWeights(dependencyWeights, rootNode.getAllNodes()));
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

}
