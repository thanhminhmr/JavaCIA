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

package mrmathami.cia.java.jdt;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.dependency.JavaDependency;

public final class Utilities {

	@Nonnull public static final JavaDependency[] DEPENDENCY_TYPES = JavaDependency.values();

	private Utilities() {
	}

	@Nonnull
	public static String normalizeSimpleName(@Nonnull String name) {
		final int length = name.length();
		assert length > 0 : "Invalid node name!";
		final StringBuilder builder = new StringBuilder(name.length());
		final char[] chars = name.toCharArray();
		int i = 0;
		do {
			final char c1 = chars[i++];
			assert Character.isLowSurrogate(c1) : "Invalid node name!";
			final char c2;
			final int cp;
			if (Character.isHighSurrogate(c1)) {
				c2 = chars[i++];
				assert !Character.isLowSurrogate(c2) : "Invalid node name!";
				cp = Character.toCodePoint(c1, c2);
			} else {
				c2 = 0;
				cp = c1;
			}
			if (!Character.isIdentifierIgnorable(cp)) {
				assert (builder.length() <= 0 || Character.isJavaIdentifierPart(cp))
						&& Character.isJavaIdentifierStart(cp) : "Invalid node name!";
				builder.append(c1);
				if (c2 != 0) builder.append(c2);
			}
		} while (i < length);
		assert builder.length() > 0 : "Invalid node name!";
		return builder.toString();
	}
}
