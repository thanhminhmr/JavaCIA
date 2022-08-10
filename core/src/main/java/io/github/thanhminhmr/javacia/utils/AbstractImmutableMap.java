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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.up;

abstract class AbstractImmutableMap<K, V> implements Map<K, V> {

	@Override
	public final @Nullable V put(@NotNull K key, @NotNull V value) {
		throw up();
	}

	@Override
	public final @Nullable V remove(@NotNull Object key) {
		throw up();
	}

	@Override
	public final void putAll(@NotNull Map<? extends K, ? extends V> map) {
		throw up();
	}

	@Override
	public final void clear() {
		throw up();
	}

	@Override
	public final void replaceAll(@NotNull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Override
	public final @Nullable V putIfAbsent(@NotNull K key, @NotNull V value) {
		throw up();
	}

	@Override
	public final boolean remove(@NotNull Object key, @NotNull Object value) {
		throw up();
	}

	@Override
	public final boolean replace(@NotNull K key, @Nullable V oldValue, @Nullable V newValue) {
		throw up();
	}

	@Override
	public final @Nullable V replace(@NotNull K key, @Nullable V value) {
		throw up();
	}

	@Override
	public final @Nullable V computeIfAbsent(@NotNull K key, @NotNull Function<? super K, ? extends V> remapper) {
		throw up();
	}

	@Override
	public final @Nullable V computeIfPresent(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Override
	public final @Nullable V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Override
	public final @Nullable V merge(@NotNull K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remapper) {
		throw up();
	}

}
