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
import mrmathami.cia.java.tree.node.JavaFieldNode;
import mrmathami.cia.java.tree.node.JavaInitializerNode;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.AbstractNonIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractNonRootNode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class InitializerNode extends AbstractNonRootNode implements JavaInitializerNode {

	private static final long serialVersionUID = -1L;

	private final boolean isStatic;

	@Nonnull private transient List<InitializerImpl> initializers = List.of();


	public InitializerNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent, boolean isStatic) {
		super(sourceFile, parent, isStatic ? "$_clinit_$" : "$_init_$");
		this.isStatic = isStatic;
	}


	//region Getter & Setter

	@Override
	public boolean isStatic() {
		return isStatic;
	}

	@Nonnull
	@Override
	public List<InitializerImpl> getInitializers() {
		return isFrozen() ? initializers : Collections.unmodifiableList(initializers);
	}

	public void setInitializers(@Nonnull List<InitializerImpl> initializers) {
		assertNonFrozen();
		this.initializers = initializers;
	}

	//region Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.initializers = List.copyOf(initializers);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(initializers);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.initializers = (List<InitializerImpl>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonStart(builder);
		builder.append(", \"isStatic\": ").append(isStatic);
	}

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!initializers.isEmpty()) {
			builder.append(", \"initializers\": [");
			internalArrayToJson(builder, indentation, true, initializers);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

	public static abstract class InitializerImpl extends AbstractNonIdentifiedEntity implements Initializer {

		private static final long serialVersionUID = -1L;

	}

	public static final class BlockInitializerImpl extends InitializerImpl implements BlockInitializer {

		private static final long serialVersionUID = -1L;

		@Nonnull private final String bodyBlock;


		public BlockInitializerImpl(@Nonnull String bodyBlock) {
			this.bodyBlock = bodyBlock;
		}


		//region Getter & Setter

		@Nonnull
		@Override
		public String getBodyBlock() {
			return bodyBlock;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonEnd(@Nonnull StringBuilder builder, @Nonnull String indentation) {
			super.internalToJsonEnd(builder, indentation);
			builder.append(", \"bodyBlock\": \"");
			internalEscapeString(builder, bodyBlock);
			builder.append('"');
		}

		//endregion Jsonify
	}

	public static final class FieldInitializerImpl extends InitializerImpl implements FieldInitializer {

		private static final long serialVersionUID = -1L;

		@Nonnull private final FieldNode fieldNode;
		@Nonnull private final String initialExpression;


		public FieldInitializerImpl(@Nonnull FieldNode fieldNode, @Nonnull String initialExpression) {
			this.fieldNode = fieldNode;
			this.initialExpression = initialExpression;
		}


		//region Getter & Setter

		@Nonnull
		@Override
		public JavaFieldNode getFieldNode() {
			return fieldNode;
		}

		@Nonnull
		@Override
		public String getInitialExpression() {
			return initialExpression;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
			super.internalToJsonStart(builder, indentation);
			builder.append(", \"fieldNode\": { ");
			fieldNode.internalToReferenceJson(builder);
			builder.append(" }");
		}

		//endregion Jsonify
	}

}
