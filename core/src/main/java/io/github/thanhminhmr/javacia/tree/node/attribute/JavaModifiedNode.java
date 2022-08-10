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

package io.github.thanhminhmr.javacia.tree.node.attribute;

import io.github.thanhminhmr.javacia.tree.JavaModifier;
import io.github.thanhminhmr.javacia.tree.node.JavaNode;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public interface JavaModifiedNode extends JavaNode {

	//region Getter & Setter

	int getModifiers();

	default @NotNull Set<JavaModifier> getModifierSet() {
		final int modifiers = getModifiers();
		final EnumSet<JavaModifier> modifierSet = EnumSet.noneOf(JavaModifier.class);
		for (final JavaModifier modifier : JavaModifier.VALUE_LIST) {
			if (isContainModifier(modifiers, modifier)) modifierSet.add(modifier);
		}
		return modifierSet;
	}

	default boolean isContainModifier(@NotNull JavaModifier modifier) {
		return isContainModifier(getModifiers(), modifier);
	}

	default boolean isPublic() {
		return isContainModifier(getModifiers(), JavaModifier.PUBLIC);
	}

	default boolean isProtected() {
		return isContainModifier(getModifiers(), JavaModifier.PROTECTED);
	}

	default boolean isPrivate() {
		return isContainModifier(getModifiers(), JavaModifier.PRIVATE);
	}

	default boolean isStatic() {
		return isContainModifier(getModifiers(), JavaModifier.STATIC);
	}

	default boolean isAbstract() {
		return isContainModifier(getModifiers(), JavaModifier.ABSTRACT);
	}

	default boolean isFinal() {
		return isContainModifier(getModifiers(), JavaModifier.FINAL);
	}

	default boolean isNative() {
		return isContainModifier(getModifiers(), JavaModifier.NATIVE);
	}

	default boolean isSynchronized() {
		return isContainModifier(getModifiers(), JavaModifier.SYNCHRONIZED);
	}

	default boolean isTransient() {
		return isContainModifier(getModifiers(), JavaModifier.TRANSIENT);
	}

	default boolean isVolatile() {
		return isContainModifier(getModifiers(), JavaModifier.VOLATILE);
	}

	default boolean isStrictfp() {
		return isContainModifier(getModifiers(), JavaModifier.STRICTFP);
	}

	//endregion Getter & Setter

	private static boolean isContainModifier(int modifiers, @NotNull JavaModifier modifier) {
		return (modifiers & (1 << modifier.ordinal())) != 0;
	}

}
