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
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;

import java.io.Serializable;
import java.util.Arrays;

import static mrmathami.cia.java.jdt.Utilities.DEPENDENCY_TYPES;

public final class DependencyCountTable implements JavaDependencyCountTable, Serializable {

	private static final long serialVersionUID = -1L;

	@Nonnull private static final int[] COUNTS_ZEROES = new int[DEPENDENCY_TYPES.length];

	@Nonnull private final int[] counts;


	public DependencyCountTable(@Nonnull int[] counts) {
		assert counts.length == DEPENDENCY_TYPES.length : "Invalid length for counts.";
		this.counts = counts;
	}


	@Override
	public int getCount(@Nonnull JavaDependency dependencyType) {
		return counts[dependencyType.ordinal()];
	}


	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object instanceof DependencyCountTable)
			return Arrays.equals(counts, ((DependencyCountTable) object).counts);
		if (object instanceof JavaDependencyCountTable) {
			final JavaDependencyCountTable dependency = (JavaDependencyCountTable) object;
			for (int i = 0; i < counts.length; i++) {
				if (dependency.getCount(DEPENDENCY_TYPES[i]) != counts[i]) return false;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(counts);
	}


	@Nonnull
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		toString(builder);
		return builder.toString();
	}

	public void toString(@Nonnull StringBuilder builder) {
		final int[] counts = this.counts;
		if (Arrays.equals(counts, COUNTS_ZEROES)) {
			builder.append("{}");
		} else {
			builder.append("{ ");
			boolean innerNext = false;
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] != 0) {
					builder.append(innerNext ? ", \"" : "\"")
							.append(DEPENDENCY_TYPES[i]).append("\": ").append(counts[i]);
					innerNext = true;
				}
			}
			builder.append(" }");
		}
	}

}
