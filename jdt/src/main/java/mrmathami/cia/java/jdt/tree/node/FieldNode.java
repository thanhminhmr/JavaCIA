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

package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.node.JavaFieldNode;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractModifiedAnnotatedNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;

import java.util.List;
import java.util.Map;

public final class FieldNode extends AbstractModifiedAnnotatedNode implements JavaFieldNode {

	private static final long serialVersionUID = -1L;

	@Nullable private AbstractType type;


	public FieldNode(@Nonnull AbstractNode parent, @Nonnull String simpleName) {
		super(parent, simpleName);
		assert parent instanceof AnnotationNode || parent instanceof ClassNode || parent instanceof EnumNode
				|| parent instanceof InterfaceNode || parent instanceof MethodNode : "Invalid parent type!";
	}


	//region Getter & Setter

	@Nullable
	@Override
	public AbstractType getType() {
		return type;
	}

	public void setType(@Nullable AbstractType type) {
		assertNonFrozen();
		this.type = type;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		if (type != null) type.internalFreeze(map);
		return false;
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (type != null) {
			builder.append(", \"type\": { ");
			type.internalToReferenceJson(builder);
			builder.append(" }");
		}
	}

	//endregion Jsonify

}
