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
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.project.JavaSourceFile;

public abstract class AbstractNonRootNode extends AbstractNode {

	private static final long serialVersionUID = -1L;

	@Nullable private final SourceFile sourceFile;
	@Nonnull private final AbstractNode parent;
	@Nonnull private final String simpleName;
	@Nonnull private final String qualifiedName;
	@Nonnull private final String uniqueName;


	@Nullable private transient RootNode root; // only null when deserialize


	public AbstractNonRootNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName) {
		this.sourceFile = sourceFile;
		this.parent = parent;
		this.simpleName = normalizeSimpleName(simpleName);
		this.uniqueName = this.qualifiedName = parent.isRoot()
				? this.simpleName
				: parent.getQualifiedName() + '.' + this.simpleName;
		this.root = parent.getRoot();
	}

	public AbstractNonRootNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nonnull String uniqueNameSuffix) {
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

	public AbstractNonRootNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nonnull String uniqueName, int order) {
		this.sourceFile = sourceFile;
		this.parent = parent;
		this.simpleName = normalizeSimpleName(simpleName);
		this.qualifiedName = parent.isRoot()
				? this.simpleName
				: parent.getQualifiedName() + '.' + this.simpleName;
		this.uniqueName = uniqueName + order;
		this.root = parent.getRoot();
	}


	@Nonnull
	private static String normalizeSimpleName(@Nonnull String name) {
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

	@Nonnull
	@Override
	public final RootNode getRoot() {
		return root != null ? root : (this.root = parent.getRoot());
	}

	@Nonnull
	@Override
	public final AbstractNode getParent() {
		return parent;
	}

	@Nonnull
	@Override
	public final String getSimpleName() {
		return simpleName;
	}

	@Nonnull
	@Override
	public final String getQualifiedName() {
		return qualifiedName;
	}

	@Nonnull
	@Override
	public final String getUniqueName() {
		return uniqueName;
	}

	@Nullable
	@Override
	public JavaSourceFile getSourceFile() {
		return sourceFile;
	}

	//endregion Basic Getter

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"simpleName\": \"").append(simpleName)
				.append("\", \"qualifiedName\": \"").append(qualifiedName)
				.append("\", \"uniqueName\": \"").append(uniqueName).append('"');
	}

	//endregion Jsonify

}
