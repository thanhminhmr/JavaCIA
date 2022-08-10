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

package io.github.thanhminhmr.javacia.jdt.tree.node;

import io.github.thanhminhmr.javacia.jdt.project.SourceFile;
import io.github.thanhminhmr.javacia.jdt.tree.AbstractIdentifiedEntity;
import io.github.thanhminhmr.javacia.jdt.tree.node.attribute.AbstractModifiedAnnotatedNode;
import io.github.thanhminhmr.javacia.jdt.tree.type.AbstractType;
import io.github.thanhminhmr.javacia.tree.node.JavaFieldNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class FieldNode extends AbstractModifiedAnnotatedNode implements JavaFieldNode {

	private static final long serialVersionUID = -1L;

	private @Nullable AbstractType type;
	private @Nullable String value;


	public FieldNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent, @NotNull String simpleName) {
		super(sourceFile, parent, simpleName);
		checkParent(parent, AnnotationNode.class, ClassNode.class, EnumNode.class,
				InterfaceNode.class, MethodNode.class);
	}


	//region Getter & Setter

	@Override
	public @Nullable AbstractType getType() {
		return type;
	}

	public void setType(@Nullable AbstractType type) {
		assertNonFrozen();
		this.type = type;
	}

	@Override
	public @Nullable String getValue() {
		return value;
	}

	public void setValue(@Nullable String value) {
		assertNonFrozen();
		this.value = value;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		if (type != null) type.internalFreeze(map);
		return false;
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (type != null) {
			builder.append(", \"type\": { ");
			type.internalToReferenceJson(builder);
			builder.append(" }");
		}
	}

	//endregion Jsonify

}
