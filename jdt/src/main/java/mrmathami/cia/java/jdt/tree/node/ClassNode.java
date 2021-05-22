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
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractParameterizedModifiedAnnotatedNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.tree.node.JavaClassNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ClassNode extends AbstractParameterizedModifiedAnnotatedNode implements JavaClassNode {

	private static final long serialVersionUID = -1L;

	@Nullable private final String binaryName;
	@Nullable private AbstractType extendsClass;

	@Nonnull private transient List<AbstractType> implementsInterfaces = List.of();


	public ClassNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent, @Nonnull String name,
			@Nullable String binaryName) {
		super(sourceFile, parent, name);
		checkParent(parent, AbstractNode.class, ClassNode.class, EnumNode.class, FieldNode.class,
				InterfaceNode.class, MethodNode.class, PackageNode.class, RootNode.class);

		this.binaryName = binaryName;
	}


	//region Getter & Setter

	@Nullable
	@Override
	public String getBinaryName() {
		return binaryName;
	}

	@Nullable
	@Override
	public AbstractType getExtendsClass() {
		return extendsClass;
	}

	public void setExtendsClass(@Nullable AbstractType extendsClass) {
		assertNonFrozen();
		this.extendsClass = extendsClass;
	}

	@Nonnull
	@Override
	public List<AbstractType> getImplementsInterfaces() {
		return isFrozen() ? implementsInterfaces : Collections.unmodifiableList(implementsInterfaces);
	}

	public void setImplementsInterfaces(@Nonnull List<AbstractType> implementsInterfaces) {
		assertNonFrozen();
		this.implementsInterfaces = implementsInterfaces;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.implementsInterfaces = List.copyOf(implementsInterfaces);
		if (extendsClass != null) extendsClass.internalFreeze(map);
		for (final AbstractType type : implementsInterfaces) type.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(implementsInterfaces);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.implementsInterfaces = (List<AbstractType>) inputStream.readObject();
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
		if (extendsClass != null) {
			builder.append(", \"extendsClass\": { ");
			extendsClass.internalToReferenceJson(builder);
			builder.append(" }");
		}
		if (!implementsInterfaces.isEmpty()) {
			builder.append(", \"implementsInterfaces\": [");
			internalArrayToReferenceJson(builder, indentation, implementsInterfaces);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
