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

import java.util.Collection;
import java.util.function.Predicate;

import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.up;

abstract class AbstractImmutableCollection<E> implements Collection<E> {

	@Override
	public final boolean add(@NotNull E element) {
		throw up();
	}

	@Override
	public final boolean remove(@NotNull Object object) {
		throw up();
	}

	@Override
	public final boolean addAll(@NotNull Collection<? extends E> collection) {
		throw up();
	}

	@Override
	public final boolean retainAll(@NotNull Collection<?> collection) {
		throw up();
	}

	@Override
	public final boolean removeAll(@NotNull Collection<?> collection) {
		throw up();
	}

	@Override
	public final void clear() {
		throw up();
	}

	@Override
	public final boolean removeIf(@NotNull Predicate<? super E> filter) {
		throw up();
	}

}