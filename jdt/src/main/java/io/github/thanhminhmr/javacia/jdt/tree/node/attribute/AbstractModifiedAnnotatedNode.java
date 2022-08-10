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

package io.github.thanhminhmr.javacia.jdt.tree.node.attribute;

import io.github.thanhminhmr.javacia.jdt.project.SourceFile;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.tree.JavaModifier;
import io.github.thanhminhmr.javacia.tree.node.attribute.JavaModifiedNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractModifiedAnnotatedNode extends AbstractAnnotatedNode implements JavaModifiedNode {

	private static final long serialVersionUID = -1L;

	private int modifiers;


	public AbstractModifiedAnnotatedNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName) {
		super(sourceFile, parent, simpleName);
	}

	public AbstractModifiedAnnotatedNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName, @NotNull String uniqueNameSuffix) {
		super(sourceFile, parent, simpleName, uniqueNameSuffix);
	}


	//region Getter & Setter

	@Override
	public final int getModifiers() {
		return modifiers;
	}

	public final void setModifiers(int modifiers) {
		assertNonFrozen();
		this.modifiers = modifiers;
	}

	//endregion Getter & Setter

	//region Jsonify

	@Override
	protected void internalToReferenceJsonEnd(@NotNull StringBuilder builder) {
		super.internalToReferenceJsonEnd(builder);
		if (modifiers != 0) {
			builder.append(", \"modifiers\": [ ");
			boolean next = false;
			for (final JavaModifier modifier : JavaModifier.VALUE_LIST) {
				if (isContainModifier(modifier)) {
					builder.append(next ? ", \"" : "\"").append(modifier).append('"');
					next = true;
				}
			}
			builder.append(" ]");
		}
	}

	//endregion Jsonify

}
