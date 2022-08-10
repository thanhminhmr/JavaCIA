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
import io.github.thanhminhmr.javacia.jdt.tree.node.attribute.AbstractParameterizedModifiedAnnotatedNode;
import io.github.thanhminhmr.javacia.jdt.tree.type.AbstractType;
import io.github.thanhminhmr.javacia.tree.node.JavaInterfaceNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class InterfaceNode extends AbstractParameterizedModifiedAnnotatedNode implements JavaInterfaceNode {

	private static final long serialVersionUID = -1L;

	private final @Nullable String binaryName;

	private transient @NotNull List<@NotNull AbstractType> extendsInterfaces = List.of();


	public InterfaceNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName, @Nullable String binaryName) {
		super(sourceFile, parent, simpleName);
		checkParent(parent, AnnotationNode.class, ClassNode.class, EnumNode.class,
				InterfaceNode.class, PackageNode.class, RootNode.class);

		this.binaryName = binaryName;
	}


	//region Getter & Setter

	@Override
	public @Nullable String getBinaryName() {
		return binaryName;
	}

	@Override
	public @NotNull List<@NotNull AbstractType> getExtendsInterfaces() {
		return isFrozen() ? extendsInterfaces : Collections.unmodifiableList(extendsInterfaces);
	}

	public void setExtendsInterfaces(@NotNull List<@NotNull AbstractType> extendsInterfaces) {
		assertNonFrozen();
		this.extendsInterfaces = extendsInterfaces;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.extendsInterfaces = List.copyOf(extendsInterfaces);
		for (final AbstractType type : extendsInterfaces) type.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(extendsInterfaces);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.extendsInterfaces = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@NotNull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"binaryName\": \"").append(binaryName).append('"');
	}

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!extendsInterfaces.isEmpty()) {
			builder.append(", \"extendsInterfaces\": [");
			internalArrayToReferenceJson(builder, indentation, extendsInterfaces);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
