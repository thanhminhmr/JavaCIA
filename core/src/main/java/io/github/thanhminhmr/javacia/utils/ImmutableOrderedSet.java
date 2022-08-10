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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.createArray;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.createPointers;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.elementAt;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.nothing;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.probePointers;
import static io.github.thanhminhmr.javacia.utils.ImmutableCollections.up;

/**
 * ImmutableOrderedSet is a read-only memory-efficient thread-safe {@link Set}
 * with linear probing hash table and predictable iteration order.
 */
public final class ImmutableOrderedSet<E> extends AbstractImmutableCollection<E> implements Set<E>, Serializable {

	private static final long serialVersionUID = -1L;

	private final @NotNull Object @NotNull [] objects;

	private transient int @NotNull [] pointers;


	public ImmutableOrderedSet(@NotNull Object @NotNull [] objects) {
		assert objects.length > 0;
		this.objects = objects;
		this.pointers = createPointers(objects, objects.length);
	}

	public static <E> @NotNull Set<@NotNull E> of() {
		return Set.of();
	}

	public static <E> @NotNull Set<@NotNull E> of(@NotNull E e) {
		return Set.of(e);
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <E> @NotNull Set<@NotNull E> of(@NotNull E @NotNull ... elements) {
		switch (elements.length) {
			case 0:
				return Set.of();
			case 1:
				return Set.of(elements[0]);
			default:
				return new ImmutableOrderedSet<>(Arrays.copyOf(elements, elements.length, Object[].class));
		}
	}

	@SuppressWarnings("unchecked")
	public static <E> @NotNull Set<@NotNull E> copyOf(@NotNull Collection<? extends E> collection) {
		if (collection instanceof ImmutableOrderedSet) {
			return (Set<E>) collection;
		}
		switch (collection.size()) {
			case 0:
				return Set.of();
			case 1:
				return Set.copyOf(collection);
			default:
				return new ImmutableOrderedSet<>(collection.toArray());
		}
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
	public boolean contains(@NotNull Object object) {
		return probePointers(objects, pointers, pointers.length, object, object.hashCode()) >= 0;
	}

	@Override
	public @NotNull Iterator<@NotNull E> iterator() {
		return new Iterator<>() {

			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < pointers.length;
			}

			@NotNull
			@Override
			public E next() {
				if (index < pointers.length) return elementAt(objects, index++);
				throw nothing();
			}

			@Override
			public void remove() {
				throw up();
			}

			@Override
			public void forEachRemaining(@NotNull Consumer<? super E> action) {
				Objects.requireNonNull(action);
				while (index < pointers.length) {
					action.accept(elementAt(objects, index++));
				}
			}

		};
	}

	@Override
	public @NotNull Object @NotNull [] toArray() {
		return objects.clone();
	}

	@Override
	public <T> T @NotNull [] toArray(T @NotNull [] array) {
		return createArray(array, objects);
	}

	@Override
	public boolean containsAll(@NotNull Collection<?> collection) {
		if (collection == this || collection.isEmpty()) return true;
		if (pointers.length == 0) return false;
		for (final Object object : collection) {
			if (probePointers(objects, pointers, pointers.length, object, object.hashCode()) < 0) return false;
		}
		return true;
	}

	@Override
	public @NotNull Spliterator<@NotNull E> spliterator() {
		return Spliterators.spliterator(this,
				Spliterator.SIZED | Spliterator.ORDERED | Spliterator.DISTINCT);
	}

	@Override
	public <T> T @NotNull [] toArray(@NotNull IntFunction<T @NotNull []> generator) {
		return toArray(generator.apply(pointers.length));
	}

	@Override
	public void forEach(@NotNull Consumer<? super E> action) {
		for (int i = 0; i < pointers.length; i++) {
			action.accept(elementAt(objects, i));
		}
	}

	@Override
	public int hashCode() {
		int hashCode = 0;
		for (final Object object : objects) {
			hashCode += object.hashCode();
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


	private @NotNull Object writeReplace() throws ObjectStreamException {
		switch (pointers.length) {
			case 0:
				return Set.of();
			case 1:
				return Set.of(objects[0]);
			default:
				return this;
		}
	}

	private @NotNull Object readResolve() throws ObjectStreamException {
		switch (objects.length) {
			case 0:
				return Set.of();
			case 1:
				return Set.of(objects[0]);
			default:
				this.pointers = createPointers(objects, objects.length);
				return this;
		}
	}

}
