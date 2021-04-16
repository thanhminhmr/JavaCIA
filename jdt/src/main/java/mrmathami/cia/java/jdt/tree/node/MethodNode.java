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
import mrmathami.cia.java.tree.node.JavaMethodNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MethodNode extends AbstractParameterizedModifiedAnnotatedNode implements JavaMethodNode {

	private static final long serialVersionUID = -1L;

	@Nonnull private final List<AbstractType> parameters;
	@Nullable private AbstractType returnType;
	@Nullable private String bodyBlock;

	@Nonnull private transient List<AbstractType> exceptions = List.of();


	@Nonnull
	private static String createUniqueNameSuffixFromParameters(@Nonnull List<AbstractType> parameters) {
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


	public MethodNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent,
			@Nonnull String simpleName, @Nonnull List<AbstractType> parameters) {
		super(sourceFile, parent, simpleName, createUniqueNameSuffixFromParameters(parameters));
		assert parent instanceof AnnotationNode || parent instanceof ClassNode || parent instanceof EnumNode
				|| parent instanceof InterfaceNode : "Invalid parent type!";
		this.parameters = List.copyOf(parameters);
	}


	//region Getter & Setter

	@Nonnull
	@Override
	public List<AbstractType> getParameters() {
		return parameters;
	}

	@Nonnull
	@Override
	public List<AbstractType> getExceptions() {
		return isFrozen() ? exceptions : Collections.unmodifiableList(exceptions);
	}

	public void setExceptions(@Nonnull List<AbstractType> exceptions) {
		assertNonFrozen();
		this.exceptions = exceptions;
	}

	@Nullable
	@Override
	public AbstractType getReturnType() {
		return returnType;
	}

	public void setReturnType(@Nullable AbstractType returnType) {
		assertNonFrozen();
		this.returnType = returnType;
	}

	@Nullable(
	)
	@Override
	public String getBodyBlock() {
		return bodyBlock;
	}

	public void setBodyBlock(@Nullable String bodyBlock) {
		assertNonFrozen();
		this.bodyBlock = bodyBlock;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.exceptions = List.copyOf(exceptions);
		for (final AbstractType parameter : parameters) parameter.internalFreeze(map);
		if (returnType != null) returnType.internalFreeze(map);
		for (final AbstractType exception : exceptions) exception.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(exceptions);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.exceptions = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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
