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

package io.github.thanhminhmr.javacia.jdt.project.differ;

import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependencyCountTable;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.Callable;

final class ImpactCalculator implements Callable<double[]> {

	private static final double THRESHOLD = 1.0e-5;

	private final @NotNull BitSet pathSet;
	private final double @NotNull [] calculatingWeights;
	private final double @NotNull [] dependencyImpacts;
	private final @NotNull JavaNode changedNode;


	ImpactCalculator(int nodeCount, double @NotNull [] dependencyImpacts, @NotNull JavaNode changedNode) {
		this.pathSet = new BitSet(nodeCount);
		this.calculatingWeights = new double[nodeCount];
		this.dependencyImpacts = dependencyImpacts;
		this.changedNode = changedNode;
	}


	private void recursiveCalculate(@NotNull JavaNode currentNode, double currentWeight) {
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
	public double @NotNull [] call() {
		Arrays.fill(calculatingWeights, 1.0);

		final int changedId = changedNode.getId();
		calculatingWeights[changedId] = 0.0;
		pathSet.set(changedId);

		recursiveCalculate(changedNode, 1.0);

		//for (int i = 0; i < nodeCount; i++) weights[i] = 1.0f - weights[i]; // NOTE: change me both!!
		return calculatingWeights;
	}

}
