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

package mrmathami.cia.java.tree;

public enum JavaModifier {
	PUBLIC, PROTECTED, PRIVATE, STATIC, ABSTRACT, FINAL, NATIVE, SYNCHRONIZED, TRANSIENT, VOLATILE, STRICTFP;

	public static final int PUBLIC_MASK = 1;
	public static final int PROTECTED_MASK = 1 << 1;
	public static final int PRIVATE_MASK = 1 << 2;
	public static final int STATIC_MASK = 1 << 3;
	public static final int ABSTRACT_MASK = 1 << 4;
	public static final int FINAL_MASK = 1 << 5;
	public static final int NATIVE_MASK = 1 << 6;
	public static final int SYNCHRONIZED_MASK = 1 << 7;
	public static final int TRANSIENT_MASK = 1 << 8;
	public static final int VOLATILE_MASK = 1 << 9;
	public static final int STRICTFP_MASK = 1 << 10;

	public final int getMask() {
		return 1 << ordinal();
	}
}
