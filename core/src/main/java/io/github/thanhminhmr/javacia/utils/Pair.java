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

import java.io.Serializable;
import java.util.Objects;

public final class Pair<A, B> implements Serializable, Cloneable {
	private static final long serialVersionUID = -1L;

	private A a;
	private B b;

	public Pair() {
	}

	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}


	public A getA() {
		return a;
	}

	public A setA(A a) {
		final A oldA = this.a;
		this.a = a;
		return oldA;
	}

	public B getB() {
		return b;
	}

	public B setB(B b) {
		final B oldB = this.b;
		this.b = b;
		return oldB;
	}


	@SuppressWarnings("unchecked")
	public @NotNull Pair<A, B> clone() {
		try {
			return (Pair<A, B>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof Pair)) return false;
		final Pair<?, ?> pair = (Pair<?, ?>) object;
		return Objects.equals(getA(), pair.getA()) && Objects.equals(getB(), pair.getB());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getA(), getB());
	}

	@Override
	public @NotNull String toString() {
		return "{ " + getA() + ", " + getB() + " }";
	}
}
