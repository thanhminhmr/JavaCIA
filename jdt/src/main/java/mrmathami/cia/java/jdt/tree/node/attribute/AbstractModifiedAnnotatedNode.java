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

package mrmathami.cia.java.jdt.tree.node.attribute;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.tree.JavaModifier;
import mrmathami.cia.java.tree.node.attribute.JavaModifiedNode;

public abstract class AbstractModifiedAnnotatedNode extends AbstractAnnotatedNode implements JavaModifiedNode {

	private static final long serialVersionUID = -1L;

	private int modifiers;


	public AbstractModifiedAnnotatedNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName) {
		super(sourceFile, parent, simpleName);
	}

	public AbstractModifiedAnnotatedNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nonnull String uniqueNameSuffix) {
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
	protected void internalToReferenceJsonEnd(@Nonnull StringBuilder builder) {
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
