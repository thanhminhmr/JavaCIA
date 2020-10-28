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

package mrmathami.cia.java.tree.node.attribute;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.JavaModifier;
import mrmathami.cia.java.tree.node.JavaNode;

import java.util.EnumSet;
import java.util.Set;

public interface JavaModifiedNode extends JavaNode {

	//region Getter & Setter

	int getModifiers();

	@Nonnull
	default Set<JavaModifier> getModifierSet() {
		final int modifiers = getModifiers();
		final EnumSet<JavaModifier> modifierSet = EnumSet.noneOf(JavaModifier.class);
		for (final JavaModifier modifierType : JavaModifier.values()) {
			if ((modifiers & modifierType.getMask()) != 0) modifierSet.add(modifierType);
		}
		return modifierSet;
	}

	default boolean isContainModifier(@Nonnull JavaModifier modifierType) {
		return (getModifiers() & modifierType.getMask()) != 0;
	}

	default boolean isPublic() {
		return (getModifiers() & JavaModifier.PUBLIC_MASK) != 0;
	}

	default boolean isProtected() {
		return (getModifiers() & JavaModifier.PROTECTED_MASK) != 0;
	}

	default boolean isPrivate() {
		return (getModifiers() & JavaModifier.PRIVATE_MASK) != 0;
	}

	default boolean isStatic() {
		return (getModifiers() & JavaModifier.STATIC_MASK) != 0;
	}

	default boolean isAbstract() {
		return (getModifiers() & JavaModifier.ABSTRACT_MASK) != 0;
	}

	default boolean isFinal() {
		return (getModifiers() & JavaModifier.FINAL_MASK) != 0;
	}

	default boolean isNative() {
		return (getModifiers() & JavaModifier.NATIVE_MASK) != 0;
	}

	default boolean isSynchronized() {
		return (getModifiers() & JavaModifier.SYNCHRONIZED_MASK) != 0;
	}

	default boolean isTransient() {
		return (getModifiers() & JavaModifier.TRANSIENT_MASK) != 0;
	}

	default boolean isVolatile() {
		return (getModifiers() & JavaModifier.VOLATILE_MASK) != 0;
	}

	default boolean isStrictfp() {
		return (getModifiers() & JavaModifier.STRICTFP_MASK) != 0;
	}

	//endregion Getter & Setter

}
