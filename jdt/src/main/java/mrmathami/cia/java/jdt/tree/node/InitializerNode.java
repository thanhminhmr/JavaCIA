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

package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractModifiedAnnotatedNode;
import mrmathami.cia.java.tree.node.JavaInitializerNode;

public final class InitializerNode extends AbstractModifiedAnnotatedNode implements JavaInitializerNode {

	private static final long serialVersionUID = -1L;

	private final boolean isStatic;
	@Nullable private String bodyBlock;


	public InitializerNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent, boolean isStatic) {
		super(sourceFile, parent, isStatic ? "<clinit>" : "<init>");
		checkParent(parent, AnnotationNode.class, ClassNode.class, EnumNode.class, InterfaceNode.class);

		this.isStatic = isStatic;
	}


	//region Getter & Setter

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Nullable
	@Override
	public String getBodyBlock() {
		return bodyBlock;
	}

	public void setBodyBlock(@Nullable String bodyBlock) {
		assertNonFrozen();
		this.bodyBlock = bodyBlock;
	}

	//endregion Getter & Setter

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"isStatic\": ").append(isStatic);
	}

	@Override
	protected void internalToJsonEnd(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonEnd(builder, indentation);
		if (bodyBlock != null) {
			builder.append(", \"bodyBlock\": \"");
			internalEscapeString(builder, bodyBlock);
			builder.append('"');
		}
	}

	//endregion Jsonify

}
