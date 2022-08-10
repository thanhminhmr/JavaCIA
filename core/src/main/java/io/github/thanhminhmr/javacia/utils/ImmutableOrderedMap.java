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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.createArray;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.createPointers;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.elementAt;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.nothing;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.probePointers;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.up;

/**
 * ImmutableOrderedMap is a read-only memory-efficient thread-safe {@link Map}
 * with linear probing hash table and predictable iteration order.
 */
public final class ImmutableOrderedMap<K, V> extends AbstractImmutableMap<K, V> implements Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull Object @NotNull [] objects;

	private transient int @NotNull [] pointers;


	private ImmutableOrderedMap(@NotNull Iterable<? extends Entry<? extends K, ? extends V>> entries, int size) {
		this.objects = new Object[size + size];
		int index = 0;
		for (final Entry<? extends K, ? extends V> entry : entries) {
			objects[index] = entry.getKey();
			objects[index + size] = Objects.requireNonNull(entry.getValue());
			index += 1;
		}
		this.pointers = createPointers(objects, size);
	}

	private ImmutableOrderedMap(@NotNull Object @NotNull ... objects) {
		assert (objects.length & 1) == 0;
		this.objects = objects;
		this.pointers = createPointers(objects, objects.length >> 1);
	}

	public static <K, V> @NotNull Map<K, V> of() {
		return Map.of();
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1) {
		return Map.of(k1, v1);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2) {
		return new ImmutableOrderedMap<>(k1, k2, v1, v2);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3) {
		return new ImmutableOrderedMap<>(k1, k2, k3, v1, v2, v3);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, v1, v2, v3, v4);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, k5, v1, v2, v3, v4, v5);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5,
			@NotNull K k6, @NotNull V v6) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, k5, k6, v1, v2, v3, v4, v5, v6);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5,
			@NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, k5, k6, k7, v1, v2, v3, v4, v5, v6, v7);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5,
			@NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, k5, k6, k7, k8, v1, v2, v3, v4, v5, v6, v7, v8);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5,
			@NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8,
			@NotNull K k9, @NotNull V v9) {
		return new ImmutableOrderedMap<>(k1, k2, k3, k4, k5, k6, k7, k8, k9, v1, v2, v3, v4, v5, v6, v7, v8, v9);
	}

	public static <K, V> @NotNull Map<K, V> of(@NotNull K k1, @NotNull V v1, @NotNull K k2, @NotNull V v2,
			@NotNull K k3, @NotNull V v3, @NotNull K k4, @NotNull V v4, @NotNull K k5, @NotNull V v5,
			@NotNull K k6, @NotNull V v6, @NotNull K k7, @NotNull V v7, @NotNull K k8, @NotNull V v8,
			@NotNull K k9, @NotNull V v9, @NotNull K k10, @NotNull V v10) {
		return new ImmutableOrderedMap<>(
				k1, k2, k3, k4, k5, k6, k7, k8, k9, k10,
				v1, v2, v3, v4, v5, v6, v7, v8, v9, v10
		);
	}

	@SuppressWarnings("unchecked")
	public static <K, V> @NotNull Map<K, V> copyOf(@NotNull Map<? extends K, ? extends V> map) {
		if (map instanceof ImmutableOrderedMap) return (Map<K, V>) map;
		final int size = map.size();
		if (size == 0) return Map.of();
		if (size == 1) return Map.copyOf(map);
		return new ImmutableOrderedMap<>(map.entrySet(), size);
	}

	@SafeVarargs
	public static <K, V> @NotNull Map<K, V> ofEntries(@NotNull Entry<? extends K, ? extends V> @NotNull ... entries) {
		final int size = entries.length;
		if (size == 0) return Map.of();
		if (size == 1) return Map.ofEntries(entries);
		return new ImmutableOrderedMap<>(Arrays.asList(entries), size);
	}


	private @NotNull K keyAt(int index) {
		assert index >= 0 && index < pointers.length;
		return elementAt(objects, index);
	}

	private @NotNull V valueAt(int index) {
		assert index >= 0 && index < pointers.length;
		return elementAt(objects, index + pointers.length);
	}

	private @NotNull Entry<@NotNull K, @NotNull V> entryAt(int index) {
		assert index >= 0 && index < pointers.length;
		return Map.entry(keyAt(index), valueAt(index));
	}


	@Override
	public int size() {
		return pointers.length;
	}

	@Override
	public boolean isEmpty() {
		return pointers.length == 0;
	}

	@Override
	public boolean containsKey(@NotNull Object key) {
		return probePointers(objects, pointers, pointers.length, key, key.hashCode()) >= 0;
	}

	@Override
	public boolean containsValue(@NotNull Object value) {
		for (int i = pointers.length; i < objects.length; i++) {
			if (value.equals(objects[i])) return true;
		}
		return false;
	}

	@Override
	public @Nullable V get(@NotNull Object key) {
		final int index = probePointers(objects, pointers, pointers.length, key, key.hashCode());
		return index >= 0 ? valueAt(index) : null;
	}

	@Override
	public @NotNull Set<@NotNull K> keySet() {
		return new KeySet();
	}

	@Override
	public @NotNull Collection<@NotNull V> values() {
		return new ValueCollection();
	}

	@Override
	public @NotNull Set<@NotNull Entry<@NotNull K, @NotNull V>> entrySet() {
		return new EntrySet();
	}

	@Override
	public @Nullable V getOrDefault(@NotNull Object key, @Nullable V defaultValue) {
		final V value = get(key);
		return value != null ? value : defaultValue;
	}

	@Override
	public void forEach(@NotNull BiConsumer<? super K, ? super V> action) {
		for (int i = 0; i < pointers.length; i++) {
			action.accept(keyAt(i), valueAt(i));
		}
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (int i = 0; i < pointers.length; i++) {
			hashCode += keyAt(i).hashCode() ^ valueAt(i).hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (object == this) return true;
		if (!(object instanceof Map)) return false;
		final Map<?, ?> map = (Map<?, ?>) object;
		return map.size() == pointers.length && entrySet().equals(map.entrySet());
	}

	@Override
	public @NotNull String toString() {
		if (pointers.length == 0) return "{}";
		final StringBuilder builder = new StringBuilder();
		for (int i = 0; i < pointers.length; i++) {
			builder.append(i > 0 ? ", " : "{").append(objects[i].toString())
					.append('=').append(objects[i + pointers.length].toString());
		}
		return builder.append('}').toString();
	}


	private @NotNull Object writeReplace() throws ObjectStreamException {
		switch (pointers.length) {
			case 0:
				return Map.of();
			case 1:
				return Map.of(keyAt(0), valueAt(0));
			default:
				return this;
		}
	}

	@SuppressWarnings("unchecked")
	private @NotNull Object readResolve() throws ObjectStreamException {
		if ((objects.length & 1) != 0) throw new InvalidObjectException("Invalid objects array length.");
		switch (objects.length) {
			case 0:
				return Map.of();
			case 2:
				return Map.of((K) objects[0], (V) objects[1]);
			default:
				this.pointers = createPointers(objects, objects.length >> 1);
				return this;
		}
	}


	private final class KeySet extends AbstractImmutableCollection<K> implements Set<K> {

		@Override
		public int size() {
			return pointers.length;
		}

		@Override
		public boolean isEmpty() {
			return pointers.length == 0;
		}

		@Override
		public boolean contains(@NotNull Object object) {
			return containsKey(object);
		}

		@Override
		public @NotNull Iterator<@NotNull K> iterator() {
			return new Iterator<>() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < pointers.length;
				}

				@Override
				public @NotNull K next() {
					if (index < pointers.length) return keyAt(index++);
					throw nothing();
				}

				@Override
				public void remove() {
					throw up();
				}

				@Override
				public void forEachRemaining(@NotNull Consumer<? super K> action) {
					Objects.requireNonNull(action);
					while (index < pointers.length) {
						action.accept(keyAt(index++));
					}
				}

			};
		}

		@Override
		public @NotNull Object @NotNull [] toArray() {
			return Arrays.copyOf(objects, pointers.length);
		}

		@Override
		public <T> T @NotNull [] toArray(T @NotNull [] array) {
			return createArray(array, objects, 0, pointers.length);
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			if (collection == this || collection.isEmpty()) return true;
			if (pointers.length == 0) return false;
			for (final Object object : collection) {
				if (!containsKey(object)) return false;
			}
			return true;
		}

		@Override
		public <T> T @NotNull [] toArray(@NotNull IntFunction<T @NotNull []> generator) {
			return toArray(generator.apply(pointers.length));
		}

		@Override
		public void forEach(@NotNull Consumer<? super K> action) {
			for (int i = 0; i < pointers.length; i++) {
				action.accept(keyAt(i));
			}
		}

		@Override
		public @NotNull Spliterator<@NotNull K> spliterator() {
			return Spliterators.spliterator(this,
					Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT);
		}

		@Override
		public int hashCode() {
			int hashCode = 0;
			for (int i = 0; i < pointers.length; i++) {
				hashCode += objects[i].hashCode();
			}
			return hashCode;
		}

		@Override
		public boolean equals(@Nullable Object object) {
			if (object == this) return true;
			if (!(object instanceof Set)) return false;
			final Set<?> set = (Set<?>) object;
			if (set.size() != pointers.length) return false;
			//noinspection SuspiciousMethodCalls
			return set.containsAll(this);
		}

		@Override
		public @NotNull String toString() {
			if (pointers.length == 0) return "[]";
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < pointers.length; i++) {
				builder.append(i > 0 ? ", " : "[").append(objects[i].toString());
			}
			return builder.append(']').toString();
		}

	}

	private final class ValueCollection extends AbstractImmutableCollection<V> {

		@Override
		public int size() {
			return pointers.length;
		}

		@Override
		public boolean isEmpty() {
			return pointers.length == 0;
		}

		@Override
		public boolean contains(@NotNull Object object) {
			return containsValue(object);
		}

		@Override
		public @NotNull Iterator<@NotNull V> iterator() {
			return new Iterator<>() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < pointers.length;
				}

				@NotNull
				@Override
				public V next() {
					if (index < pointers.length) return valueAt(index++);
					throw nothing();
				}

				@Override
				public void remove() {
					throw up();
				}

				@Override
				public void forEachRemaining(@NotNull Consumer<? super V> action) {
					Objects.requireNonNull(action);
					while (index < pointers.length) {
						action.accept(valueAt(index++));
					}
				}

			};
		}

		@Override
		public @NotNull Object @NotNull [] toArray() {
			return Arrays.copyOfRange(objects, pointers.length, objects.length);
		}

		@Override
		public <T> T @NotNull [] toArray(T @NotNull [] array) {
			return createArray(array, objects, pointers.length, pointers.length);
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			if (collection == this || collection.isEmpty()) return true;
			if (pointers.length == 0) return false;
			for (final Object object : collection) {
				if (!containsValue(object)) return false;
			}
			return true;
		}

		@Override
		public <T> T @NotNull [] toArray(@NotNull IntFunction<T @NotNull []> generator) {
			return toArray(generator.apply(pointers.length));
		}

		@Override
		public void forEach(@NotNull Consumer<? super V> action) {
			for (int i = 0; i < pointers.length; i++) {
				action.accept(valueAt(i));
			}
		}

		@Override
		public @NotNull Spliterator<@NotNull V> spliterator() {
			return Spliterators.spliterator(this, Spliterator.SIZED | Spliterator.ORDERED);
		}

		@Override
		public @NotNull String toString() {
			if (pointers.length == 0) return "[]";
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < pointers.length; i++) {
				builder.append(i > 0 ? ", " : "[").append(valueAt(i));
			}
			return builder.append(']').toString();
		}

	}

	private final class EntrySet extends AbstractImmutableCollection<Entry<K, V>> implements Set<Entry<K, V>> {

		@Override
		public int size() {
			return pointers.length;
		}

		@Override
		public boolean isEmpty() {
			return pointers.length == 0;
		}

		@Override
		public boolean contains(@NotNull Object object) {
			Objects.requireNonNull(object);
			if (!(object instanceof Entry)) return false;
			final Entry<?, ?> entry = (Entry<?, ?>) object;
			final V value = get(entry.getKey());
			return entry.getValue().equals(value);
		}

		@Override
		public @NotNull Iterator<@NotNull Entry<@NotNull K, @NotNull V>> iterator() {
			return new Iterator<>() {

				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < pointers.length;
				}

				@NotNull
				@Override
				public Entry<K, V> next() {
					if (index < pointers.length) return entryAt(index++);
					throw nothing();
				}

				@Override
				public void remove() {
					throw up();
				}

				@Override
				public void forEachRemaining(@NotNull Consumer<? super Entry<K, V>> action) {
					Objects.requireNonNull(action);
					while (index < pointers.length) {
						action.accept(entryAt(index++));
					}
				}

			};
		}

		@Override
		public @NotNull Object @NotNull [] toArray() {
			final Object[] array = new Object[pointers.length];
			for (int i = 0; i < pointers.length; i++) {
				array[i] = entryAt(i);
			}
			return array;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T @NotNull [] toArray(T @NotNull [] array) {
			if (array.length < pointers.length) {
				array = (T[]) Array.newInstance(array.getClass().getComponentType(), pointers.length);
			}
			for (int i = 0; i < pointers.length; i++) {
				array[i] = (T) entryAt(i);
			}
			if (array.length > pointers.length) array[pointers.length] = null;
			return array;
		}

		@Override
		public boolean containsAll(@NotNull Collection<?> collection) {
			if (collection == this || collection.isEmpty()) return true;
			if (pointers.length == 0) return false;
			for (final Object object : collection) {
				if (!contains(object)) return false;
			}
			return true;
		}

		@Override
		public <T> T @NotNull [] toArray(@NotNull IntFunction<T @NotNull []> generator) {
			return toArray(generator.apply(pointers.length));
		}

		@Override
		public void forEach(@NotNull Consumer<? super Entry<@NotNull K, @NotNull V>> action) {
			for (int i = 0; i < pointers.length; i++) {
				action.accept(entryAt(i));
			}
		}

		@Override
		public @NotNull Spliterator<Map.@NotNull Entry<@NotNull K, @NotNull V>> spliterator() {
			return Spliterators.spliterator(this,
					Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT);
		}

		@Override
		public int hashCode() {
			return ImmutableOrderedMap.this.hashCode();
		}

		@Override
		public boolean equals(@Nullable Object object) {
			if (object == this) return true;
			if (!(object instanceof Set)) return false;
			final Set<?> set = (Set<?>) object;
			if (set.size() != pointers.length) return false;
			for (final Object element : set) {
				if (!(element instanceof Entry)) return false;
				final Entry<?, ?> entry = (Entry<?, ?>) element;
				final Object key, value;
				if ((key = entry.getKey()) == null || (value = get(key)) == null || !value.equals(entry.getValue())) {
					return false;
				}
			}
			return true;
		}

		@Override
		public @NotNull String toString() {
			if (pointers.length == 0) return "[]";
			final StringBuilder builder = new StringBuilder();
			for (int i = 0; i < pointers.length; i++) {
				builder.append(i > 0 ? ", " : "[").append(objects[i].toString())
						.append('=').append(objects[i + pointers.length].toString());
			}
			return builder.append(']').toString();
		}

	}

}
