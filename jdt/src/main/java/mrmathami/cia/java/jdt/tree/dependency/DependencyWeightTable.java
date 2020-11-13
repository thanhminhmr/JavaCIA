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

package mrmathami.cia.java.jdt.tree.dependency;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;

import java.io.Serializable;
import java.util.Arrays;

public final class DependencyWeightTable implements JavaDependencyWeightTable, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private final double[] weights;


	public DependencyWeightTable(@Nonnull double[] weights) {
		assert weights.length == JavaDependency.valueList.size() : "Invalid length for weights.";
		this.weights = weights;
	}


	@Override
	public double getWeight(@Nonnull JavaDependency dependencyType) {
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
				if (dependency.getWeight(JavaDependency.valueList.get(i)) != weights[i]) return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(weights);
	}


	@Nonnull
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}

	public void toString(@Nonnull StringBuilder builder) {
		final double[] weights = this.weights;
		builder.append('{');
		boolean innerNext = false;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] != 0) {
				builder.append(innerNext ? ", \"" : " \"")
						.append(JavaDependency.valueList.get(i)).append("\": ").append(weights[i]);
				innerNext = true;
			}
		}
		builder.append(innerNext ? " }" : "}");
	}

}
