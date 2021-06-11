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

package mrmathami.cia.java.jdt.project.differ;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;
import mrmathami.cia.java.tree.node.JavaNode;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.Callable;

final class ImpactCalculator implements Callable<double[]> {

	private static final double THRESHOLD = 1.0e-5;

	@Nonnull private final BitSet pathSet;
	@Nonnull private final double[] calculatingWeights;
	@Nonnull private final double[] dependencyImpacts;
	@Nonnull private final JavaNode changedNode;


	ImpactCalculator(int nodeCount, @Nonnull double[] dependencyImpacts, @Nonnull JavaNode changedNode) {
		this.pathSet = new BitSet(nodeCount);
		this.calculatingWeights = new double[nodeCount];
		this.dependencyImpacts = dependencyImpacts;
		this.changedNode = changedNode;
	}


	private void recursiveCalculate(@Nonnull JavaNode currentNode, double currentWeight) {
		for (final Map.Entry<? extends JavaNode, ? extends JavaDependencyCountTable> entry
				: currentNode.getDependencyFrom().entrySet()) {
			final JavaNode nextNode = entry.getKey();
			final int nextId = nextNode.getId();
			if (pathSet.get(nextId)) continue;
			pathSet.set(nextId);

			double linkWeight = 1.0;
			final JavaDependencyCountTable nodeDependency = entry.getValue();
			for (final JavaDependency dependency : JavaDependency.VALUE_LIST) {
				final int count = nodeDependency.getCount(dependency);
				if (count > 0) {
					linkWeight *= Math.pow(1.0 - dependencyImpacts[dependency.ordinal()], count);
				}
			}

			final double nextWeight = currentWeight * (1.0 - linkWeight);
			if (nextWeight >= THRESHOLD) {
				calculatingWeights[nextId] *= 1.0 - nextWeight;
				recursiveCalculate(nextNode, nextWeight);
			}

			pathSet.clear(nextId);
		}
	}

	@Override
	public double[] call() {
		Arrays.fill(calculatingWeights, 1.0);

		final int changedId = changedNode.getId();
		calculatingWeights[changedId] = 0.0;
		pathSet.set(changedId);

		recursiveCalculate(changedNode, 1.0);

		//for (int i = 0; i < nodeCount; i++) weights[i] = 1.0f - weights[i]; // NOTE: change me both!!
		return calculatingWeights;
	}

}
