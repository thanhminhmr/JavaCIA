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
import mrmathami.cia.java.tree.node.JavaAnnotationNode;

public final class AnnotationNode extends AbstractModifiedAnnotatedNode implements JavaAnnotationNode {

	private static final long serialVersionUID = -1L;

	@Nullable private final String binaryName;

	public AnnotationNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nullable String binaryName) {
		super(sourceFile, parent, simpleName);
		checkParent(parent, AbstractNode.class, ClassNode.class, EnumNode.class,
				InterfaceNode.class, PackageNode.class, RootNode.class);

		this.binaryName = binaryName;
	}

	//region Getter & Setter

	@Nullable
	@Override
	public String getBinaryName() {
		return binaryName;
	}

	//endregion Getter & Setter

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"binaryName\": \"").append(binaryName).append('"');
	}

	//endregion Jsonify

}
