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
import io.github.thanhminhmr.javacia.tree.node.JavaMethodNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MethodNode extends AbstractParameterizedModifiedAnnotatedNode implements JavaMethodNode {

	private static final long serialVersionUID = -1L;

	private final boolean isConstructor;
	private final @NotNull List<@NotNull AbstractType> parameters;
	private @Nullable  AbstractType returnType;
	private @Nullable  String bodyBlock;

	private transient @NotNull List<@NotNull AbstractType> exceptions = List.of();


	@NotNull
	private static String createUniqueNameSuffixFromParameters(@NotNull List<@NotNull AbstractType> parameters) {
		if (parameters.isEmpty()) return "()";
		final StringBuilder builder = new StringBuilder("(");
		boolean next = false;
		for (final AbstractType parameter : parameters) {
			if (next) builder.append(',');
			builder.append(parameter.getDescription());
			next = true;
		}
		return builder.append(')').toString();
	}


	public MethodNode(@Nullable SourceFile sourceFile, @NotNull AbstractNode parent,
			@NotNull String simpleName, boolean isConstructor, @NotNull List<@NotNull AbstractType> parameters) {
		super(sourceFile, parent, simpleName, createUniqueNameSuffixFromParameters(parameters));
		checkParent(parent, AbstractNode.class, ClassNode.class, EnumNode.class, InterfaceNode.class);

		this.isConstructor = isConstructor;
		this.parameters = List.copyOf(parameters);
	}


	//region Getter & Setter

	@Override
	public boolean isConstructor() {
		return isConstructor;
	}

	@Override
	public @NotNull List<@NotNull AbstractType> getParameters() {
		return parameters;
	}

	@Override
	public @NotNull List<@NotNull AbstractType> getExceptions() {
		return isFrozen() ? exceptions : Collections.unmodifiableList(exceptions);
	}

	public void setExceptions(@NotNull List<@NotNull AbstractType> exceptions) {
		assertNonFrozen();
		this.exceptions = exceptions;
	}

	@Override
	public @Nullable AbstractType getReturnType() {
		return returnType;
	}

	public void setReturnType(@Nullable AbstractType returnType) {
		assertNonFrozen();
		this.returnType = returnType;
	}

	@Override
	public @Nullable String getBodyBlock() {
		return bodyBlock;
	}

	public void setBodyBlock(@Nullable String bodyBlock) {
		assertNonFrozen();
		this.bodyBlock = bodyBlock;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.exceptions = List.copyOf(exceptions);
		for (final AbstractType parameter : parameters) parameter.internalFreeze(map);
		if (returnType != null) returnType.internalFreeze(map);
		for (final AbstractType exception : exceptions) exception.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(exceptions);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.exceptions = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (returnType != null) {
			builder.append(", \"type\": { ");
			returnType.internalToReferenceJson(builder);
			builder.append(" }");
		}
		if (!parameters.isEmpty()) {
			builder.append(", \"parameters\": [");
			internalArrayToReferenceJson(builder, indentation, parameters);
			builder.append('\n').append(indentation).append(']');
		}
		if (!exceptions.isEmpty()) {
			builder.append(", \"exceptions\": [");
			internalArrayToReferenceJson(builder, indentation, exceptions);
			builder.append('\n').append(indentation).append(']');
		}
	}

	@Override
	protected void internalToJsonEnd(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonEnd(builder, indentation);
		if (bodyBlock != null) {
			builder.append(", \"bodyBlock\": \"");
			internalEscapeString(builder, bodyBlock);
			builder.append('"');
		}
	}

	//endregion Jsonify

}
