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

package io.github.thanhminhmr.javacia.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

final class ImmutableCollections {

	private ImmutableCollections() {
	}

	static @NotNull UnsupportedOperationException up() {
		return new UnsupportedOperationException();
	}

	static @NotNull NoSuchElementException nothing() {
		return new NoSuchElementException();
	}

	static @NotNull IllegalArgumentException off() {
		return new IllegalArgumentException();
	}

	static int @NotNull [] createPointers(@Nullable Object @NotNull [] objects, int length) {
		final int[] pointers = new int[length];
		for (int pointer = 0; pointer < length; pointer++) {
			final Object object = Objects.requireNonNull(objects[pointer]);
			final int hashMod = object.hashCode() % length;
			int index = hashMod >= 0 ? hashMod : length + hashMod;
			int ptr;
			while ((ptr = pointers[index]) < 0) {
				if (objects[~ptr].equals(object)) throw off();
				if (++index == length) index = 0;
			}
			pointers[index] = ~pointer;
		}
		return pointers;
	}

	static int probePointers(@NotNull Object @NotNull [] objects, int @NotNull [] pointers, int length,
			@NotNull Object object, int hashCode) {
		if (length == 0) return -1;
		final int hashMod = hashCode % length;
		final int start = hashMod >= 0 ? hashMod : length + hashMod;
		int index = start;
		do {
			final int pointer = ~pointers[index];
			if (objects[pointer].equals(object)) return pointer;
			if (++index == length) index = 0;
		} while (index != start);
		return -1;
	}

	@SuppressWarnings("unchecked")
	static <E> @NotNull E elementAt(@NotNull Object @NotNull [] objects, int index) {
		return (E) objects[index];
	}

	@SuppressWarnings("unchecked")
	static <T> T @NotNull [] createArray(T @NotNull [] array, @NotNull Object @NotNull [] objects, int startAt,
			int length) {
		final int arrayLength = array.length; // implicit null check
		if (arrayLength < length) {
			return (T[]) Arrays.copyOfRange(objects, startAt, startAt + length, array.getClass());
		}
		//noinspection SuspiciousSystemArraycopy
		System.arraycopy(objects, startAt, array, 0, length);
		if (arrayLength > length) array[length] = null;
		return array;
	}

	@SuppressWarnings("unchecked")
	static <T> T @NotNull [] createArray(T @NotNull [] array, @NotNull Object @NotNull [] objects) {
		final int arrayLength = array.length; // implicit null check
		final int length = objects.length;
		if (arrayLength < length) {
			return (T[]) Arrays.copyOf(objects, length, array.getClass());
		}
		//noinspection SuspiciousSystemArraycopy
		System.arraycopy(objects, 0, array, 0, length);
		if (arrayLength > length) array[length] = null;
		return array;
	}

}
