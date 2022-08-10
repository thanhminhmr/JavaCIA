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
import io.github.thanhminhmr.javacia.jdt.tree.node.RootNode;
import io.github.thanhminhmr.javacia.project.JavaSourceFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractNonRootNode extends AbstractNode {

	private static final long serialVersionUID = -1L;

	private final @Nullable SourceFile sourceFile;
	private final @NotNull AbstractNode parent;
	private final @NotNull String simpleName;
	private final @NotNull String qualifiedName;
	private final @NotNull String uniqueName;


	private transient @Nullable RootNode root; // only null when deserialize


	public AbstractNonRootNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName) {
		this.sourceFile = sourceFile;
		this.parent = parent;
		this.simpleName = normalizeSimpleName(simpleName);
		this.uniqueName = this.qualifiedName = parent.isRoot()
				? this.simpleName
				: parent.getQualifiedName() + '.' + this.simpleName;
		this.root = parent.getRoot();
	}

	public AbstractNonRootNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName, @NotNull String uniqueNameSuffix) {
		this.sourceFile = sourceFile;
		this.parent = parent;
		this.simpleName = normalizeSimpleName(simpleName);
		this.uniqueName = (
				this.qualifiedName = parent.isRoot()
						? this.simpleName
						: parent.getQualifiedName() + '.' + this.simpleName
		) + uniqueNameSuffix;
		this.root = parent.getRoot();
	}


	@NotNull
	private static String normalizeSimpleName(@NotNull String name) {
		final int length = name.length();
		assert length > 0 : "Invalid node name!";
		final StringBuilder builder = new StringBuilder(name.length());
		final char[] chars = name.toCharArray();
		int i = 0;
		do {
			final char c1 = chars[i++];
			assert Character.isLowSurrogate(c1) : "Invalid node name!";
			final char c2;
			final int cp;
			if (Character.isHighSurrogate(c1)) {
				c2 = chars[i++];
				assert !Character.isLowSurrogate(c2) : "Invalid node name!";
				cp = Character.toCodePoint(c1, c2);
			} else {
				c2 = 0;
				cp = c1;
			}
			if (!Character.isIdentifierIgnorable(cp)) {
				assert (builder.length() <= 0 || Character.isJavaIdentifierPart(cp))
						&& Character.isJavaIdentifierStart(cp) : "Invalid node name!";
				builder.append(c1);
				if (c2 != 0) builder.append(c2);
			}
		} while (i < length);
		assert builder.length() > 0 : "Invalid node name!";
		return builder.toString();
	}


	//region Basic Getter

	@Override
	public final boolean isRoot() {
		return false;
	}

	@Override
	public final @NotNull RootNode getRoot() {
		return root != null ? root : (this.root = parent.getRoot());
	}

	@Override
	public final @NotNull AbstractNode getParent() {
		return parent;
	}

	@Override
	public final @NotNull String getSimpleName() {
		return simpleName;
	}

	@Override
	public final @NotNull String getQualifiedName() {
		return qualifiedName;
	}

	@Override
	public final @NotNull String getUniqueName() {
		return uniqueName;
	}

	@Override
	public @Nullable JavaSourceFile getSourceFile() {
		return sourceFile;
	}

	//endregion Basic Getter

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@NotNull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"simpleName\": \"").append(simpleName)
				.append("\", \"qualifiedName\": \"").append(qualifiedName)
				.append("\", \"uniqueName\": \"").append(uniqueName).append('"');
	}

	//endregion Jsonify

}
