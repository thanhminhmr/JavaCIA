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

package io.github.thanhminhmr.javacia.jdt.tree.dependency;

import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependencyWeightTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

public final class DependencyWeightTable implements JavaDependencyWeightTable, Serializable {

	private static final long serialVersionUID = -1L;

	private final double @NotNull [] weights;


	public DependencyWeightTable(double @NotNull [] weights) {
		assert weights.length == JavaDependency.VALUE_LIST.size() : "Invalid length for weights.";
		this.weights = weights;
	}


	@Override
	public double getWeight(@NotNull JavaDependency dependencyType) {
		return weights[dependencyType.ordinal()];
	}


	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object instanceof DependencyWeightTable)
			return Arrays.equals(weights, ((DependencyWeightTable) object).weights);
		if (object instanceof JavaDependencyWeightTable) {
			final JavaDependencyWeightTable dependency = (JavaDependencyWeightTable) object;
			for (int i = 0; i < weights.length; i++) {
				if (dependency.getWeight(JavaDependency.VALUE_LIST.get(i)) != weights[i]) return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(weights);
	}


	@Override
	public @NotNull String toString() {
		final StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}

	public void toString(@NotNull StringBuilder builder) {
		final double[] weights = this.weights;
		builder.append('{');
		boolean innerNext = false;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] != 0) {
				builder.append(innerNext ? ", \"" : " \"")
						.append(JavaDependency.VALUE_LIST.get(i)).append("\": ").append(weights[i]);
				innerNext = true;
			}
		}
		builder.append(innerNext ? " }" : "}");
	}

}
