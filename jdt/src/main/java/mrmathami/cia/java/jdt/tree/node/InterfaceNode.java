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
import mrmathami.cia.java.tree.node.JavaInterfaceNode;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractParameterizedModifiedAnnotatedNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class InterfaceNode extends AbstractParameterizedModifiedAnnotatedNode implements JavaInterfaceNode {

	private static final long serialVersionUID = -1L;

	@Nullable private final String binaryName;

	@Nonnull private transient List<AbstractType> extendsInterfaces = List.of();


	public InterfaceNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nullable String binaryName) {
		super(sourceFile, parent, simpleName);
		assert parent instanceof AnnotationNode || parent instanceof ClassNode || parent instanceof EnumNode
				|| parent instanceof InterfaceNode || parent instanceof PackageNode || parent instanceof RootNode
				: "Invalid parent type!";

		this.binaryName = binaryName;
	}


	//region Getter & Setter

	@Nullable
	@Override
	public String getBinaryName() {
		return binaryName;
	}

	@Nonnull
	@Override
	public List<AbstractType> getExtendsInterfaces() {
		return isFrozen() ? extendsInterfaces : Collections.unmodifiableList(extendsInterfaces);
	}

	public void setExtendsInterfaces(@Nonnull List<AbstractType> extendsInterfaces) {
		assertNonFrozen();
		this.extendsInterfaces = extendsInterfaces;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.extendsInterfaces = List.copyOf(extendsInterfaces);
		for (final AbstractType type : extendsInterfaces) type.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(extendsInterfaces);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.extendsInterfaces = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"binaryName\": \"").append(binaryName).append('"');
	}

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!extendsInterfaces.isEmpty()) {
			builder.append(", \"extendsInterfaces\": [");
			internalArrayToReferenceJson(builder, indentation, extendsInterfaces);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
