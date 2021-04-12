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

package mrmathami.cia.java.jdt.project.differ;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.ProjectSnapshotComparison;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class JavaSnapshotComparator {

	private JavaSnapshotComparator() {
	}


	@Nonnull
	public static ProjectSnapshotComparison compare(@Nonnull String comparisonName,
			@Nonnull JavaProjectSnapshot previousSnapshot, @Nonnull JavaProjectSnapshot currentSnapshot,
			@Nonnull JavaDependencyWeightTable impactWeightMap) throws JavaCiaException {
		final JavaRootNode previousRootNode = previousSnapshot.getRootNode();
		final JavaRootNode currentRootNode = currentSnapshot.getRootNode();

		final Set<JavaNode> addedNodes = new LinkedHashSet<>();
		final Set<JavaNode> removedNodes = new LinkedHashSet<>();
		final Set<Pair<JavaNode, JavaNode>> changedNodes = new LinkedHashSet<>();
		final Set<Pair<JavaNode, JavaNode>> unchangedNodes = new LinkedHashSet<>();

		compareRootNodes(previousRootNode, currentRootNode, addedNodes, removedNodes, changedNodes, unchangedNodes);

		final double[] dependencyImpacts = new double[JavaDependency.valueList.size()];
		for (final JavaDependency type : JavaDependency.valueList) {
			dependencyImpacts[type.ordinal()] = impactWeightMap.getWeight(type);
		}

		return new ProjectSnapshotComparison(comparisonName, previousSnapshot, currentSnapshot,
				addedNodes, removedNodes, changedNodes, unchangedNodes, dependencyImpacts,
				calculateNodeImpacts(dependencyImpacts, currentRootNode, addedNodes, changedNodes));
	}


	private static void compareRootNodes(@Nonnull JavaRootNode previousRootNode, @Nonnull JavaRootNode currentRootNode,
			@Nonnull Set<JavaNode> addedNodes, @Nonnull Set<JavaNode> removedNodes,
			@Nonnull Set<Pair<JavaNode, JavaNode>> changedNodes,
			@Nonnull Set<Pair<JavaNode, JavaNode>> unchangedNodes) {
		final EntityMatcher matcher = new EntityMatcher();

		final Map<EntityWrapper, JavaNode> previousNodeMap = new HashMap<>();
		final Map<EntityWrapper, JavaNode> currentNodeMap = new HashMap<>();
		for (final JavaNode node : previousRootNode.getAllNodes()) {
			previousNodeMap.put(matcher.wrap(node, false), node);
		}
		for (final JavaNode node : currentRootNode.getAllNodes()) {
			currentNodeMap.put(matcher.wrap(node, false), node);
		}

		for (final Map.Entry<EntityWrapper, JavaNode> previousEntry : previousNodeMap.entrySet()) {
			final EntityWrapper previousWrapper = previousEntry.getKey();
			final JavaNode previousNode = previousEntry.getValue();
			final JavaNode currentNode = currentNodeMap.get(previousWrapper);

			if (currentNode != null) {
				if (matcher.match(previousNode, currentNode, true)) {
					unchangedNodes.add(Pair.immutableOf(previousNode, currentNode));
				} else {
					changedNodes.add(Pair.immutableOf(previousNode, currentNode));
				}
			} else {
				removedNodes.add(previousNode);
			}
		}


		for (final Map.Entry<EntityWrapper, JavaNode> currentEntry : currentNodeMap.entrySet()) {
			final EntityWrapper currentWrapper = currentEntry.getKey();
			final JavaNode currentNode = currentEntry.getValue();
			final JavaNode previousNode = previousNodeMap.get(currentWrapper);
			if (previousNode == null) addedNodes.add(currentNode);
		}
	}

	@Nonnull
	private static double[] calculateNodeImpacts(@Nonnull double[] dependencyImpacts,
			@Nonnull JavaRootNode rootNode, @Nonnull Set<JavaNode> addedNodes,
			@Nonnull Set<Pair<JavaNode, JavaNode>> changedNodes) throws JavaCiaException {

		final int nodeCount = rootNode.getAllNodes().size();
		final List<Future<double[]>> taskFutures = new ArrayList<>(addedNodes.size() + changedNodes.size());

		final ExecutorService executorService = Executors.newWorkStealingPool();
		for (final JavaNode node : addedNodes) {
			taskFutures.add(executorService.submit(new ImpactCalculator(nodeCount, dependencyImpacts, node)));
		}
		for (final Pair<JavaNode, JavaNode> pair : changedNodes) {
			taskFutures.add(executorService.submit(new ImpactCalculator(nodeCount, dependencyImpacts, pair.getB())));
		}
		executorService.shutdown();

		final double[] weights = new double[nodeCount];
		Arrays.fill(weights, 1.0f);

		try {
			for (final Future<double[]> future : taskFutures) {
				final double[] singleWeights = future.get();

				//for (int i = 0; i < nodeCount; i++) weights[i] *= 1.0f - singleWeights[i]; // NOTE: change me both!!
				for (int i = 0; i < nodeCount; i++) weights[i] *= singleWeights[i];
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new JavaCiaException("Cannot calculate impact weights!", e);
		}

		for (int i = 0; i < nodeCount; i++) weights[i] = 1.0f - weights[i];
		return weights;
	}

}
