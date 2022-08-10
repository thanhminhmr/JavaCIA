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

package io.github.thanhminhmr.javacia.jdt.tree.annotate;

import io.github.thanhminhmr.javacia.jdt.tree.AbstractIdentifiedEntity;
import io.github.thanhminhmr.javacia.jdt.tree.AbstractNonIdentifiedEntity;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.tree.annotate.JavaAnnotate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Annotate extends AbstractIdentifiedEntity implements JavaAnnotate {

	private static final long serialVersionUID = -1L;

	private final @NotNull String name;
	private @Nullable AbstractNode node;
	private transient @NotNull List<@NotNull ParameterImpl> parameters = List.of();


	public Annotate(@NotNull String name) {
		this.name = name;
	}


	//region Getter & Setter

	@Override
	public @NotNull String getName() {
		return name;
	}

	@Override
	public @Nullable AbstractNode getNode() {
		return node;
	}

	public void setNode(@Nullable AbstractNode node) {
		assertNonFrozen();
		this.node = node;
	}

	@Override
	public @NotNull List<@NotNull ParameterImpl> getParameters() {
		return isFrozen() ? parameters : Collections.unmodifiableList(parameters);
	}

	public void setParameters(@NotNull List<@NotNull ParameterImpl> parameters) {
		assertNonFrozen();
		this.parameters = parameters;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	// must be called when @Override
	@Override
	public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.parameters = List.copyOf(parameters);
		for (final ParameterImpl parameter : parameters) parameter.internalFreeze(map);
		return false;
	}

	private void writeObject(@NotNull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(parameters);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@NotNull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.parameters = (List<ParameterImpl>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonEnd(@NotNull StringBuilder builder) {
		super.internalToReferenceJsonEnd(builder);
		builder.append(", \"name\": \"").append(name).append('"');
	}

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (node != null) {
			builder.append(", \"node\": { ");
			node.internalToReferenceJson(builder);
			builder.append(" }");
		}
		if (!parameters.isEmpty()) {
			builder.append(", \"parameters\": [");
			internalArrayToJson(builder, indentation, true, parameters);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

	@Override
	public @NotNull String toString() {
		return name;
	}


	public static final class ParameterImpl extends AbstractNonIdentifiedEntity implements Parameter {

		private static final long serialVersionUID = -1L;

		private final @NotNull String name;
		private @Nullable  ValueImpl value;
		private @Nullable  AbstractNode node;


		public ParameterImpl(@NotNull String name) {
			this.name = name;
		}


		//region Getter & Setter

		@Override
		public @NotNull String getName() {
			return name;
		}

		@Override
		public @Nullable ValueImpl getValue() {
			return value;
		}

		public void setValue(@NotNull ValueImpl value) {
			assertNonFrozen();
			this.value = value;
		}

		@Override
		public @Nullable AbstractNode getNode() {
			return node;
		}

		public void setNode(@NotNull AbstractNode node) {
			assertNonFrozen();
			this.node = node;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			if (value != null) value.internalFreeze(map);
			return false;
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
			super.internalToJsonStart(builder, indentation);
			builder.append(", \"name\": \"").append(name).append('"');
			if (node != null) {
				builder.append(", \"node\": { ");
				node.internalToReferenceJson(builder);
				builder.append(" }");
			}
			if (value != null) {
				builder.append(", \"value\": { ");
				value.internalToJson(builder, indentation);
				builder.append(" }");
			}
		}

		//endregion Jsonify

	}

	public static abstract class ValueImpl extends AbstractNonIdentifiedEntity implements Value {

		private static final long serialVersionUID = -1L;

	}

	public static final class ArrayValueImpl extends ValueImpl implements ArrayValue {

		private static final long serialVersionUID = -1L;

		private transient @NotNull List<NonArrayValueImpl> values = List.of();


		//region Getter & Setter

		@Override
		public @NotNull List<@NotNull NonArrayValueImpl> getValues() {
			return values;
		}

		public void setValues(@NotNull List<NonArrayValueImpl> values) {
			assertNonFrozen();
			this.values = values;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			this.values = List.copyOf(values);
			for (final NonArrayValueImpl value : values) value.internalFreeze(map);
			return false;
		}

		private void writeObject(@NotNull ObjectOutputStream outputStream)
				throws IOException, UnsupportedOperationException {
			assertFrozen();
			outputStream.defaultWriteObject();
			outputStream.writeObject(values);
		}

		@SuppressWarnings("unchecked")
		private void readObject(@NotNull ObjectInputStream inputStream)
				throws IOException, ClassNotFoundException, ClassCastException {
			inputStream.defaultReadObject();
			this.values = (List<NonArrayValueImpl>) inputStream.readObject();
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
			super.internalToJsonStart(builder, indentation);
			if (!values.isEmpty()) {
				builder.append(", \"values\": [");
				internalArrayToJson(builder, indentation, true, values);
				builder.append('\n').append(indentation).append(']');
			}
		}

		//endregion Jsonify

	}

	public static abstract class NonArrayValueImpl extends ValueImpl implements NonArrayValue {

		private static final long serialVersionUID = -1L;

	}

	public static final class SimpleValueImpl extends NonArrayValueImpl implements SimpleValue {

		private static final long serialVersionUID = -1L;

		private final @NotNull Object value;


		public SimpleValueImpl(@NotNull Object value) {
			if (!SimpleValue.isValidValueType(value)) throw new IllegalArgumentException("Invalid value type!");
			this.value = value;
		}


		//region Getter & Setter

		@Override
		public @NotNull String getValueType() {
			return value.getClass().getSimpleName();
		}

		@Override
		public @NotNull Object getValue() {
			return value;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
			super.internalToJsonStart(builder, indentation);
			builder.append(", \"type\": \"").append(getValueType())
					.append("\", \"value\": ");
			if (value instanceof String) {
				builder.append('"');
				internalEscapeString(builder, (String) value);
				builder.append('"');
			} else {
				builder.append(value);
			}
		}

		//endregion Jsonify

	}

	public static final class NodeValueImpl extends NonArrayValueImpl implements NodeValue {

		private static final long serialVersionUID = -1L;

		private final @NotNull String describe;

		private @Nullable  AbstractNode node;


		public NodeValueImpl(@NotNull String describe) {
			this.describe = describe;
		}


		//region Getter & Setter

		@Override
		public @NotNull String getDescribe() {
			return describe;
		}

		@Override
		public @Nullable AbstractNode getNode() {
			return node;
		}

		public void setNode(@NotNull AbstractNode node) {
			assertNonFrozen();
			this.node = node;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
			super.internalToJsonStart(builder, indentation);
			builder.append(", \"describe\": \"").append(describe).append('"');
			if (node != null) {
				builder.append(", \"node\": { ");
				node.internalToReferenceJson(builder);
				builder.append(" }");
			}
		}

		//endregion Jsonify

	}

	public static final class AnnotateValueImpl extends NonArrayValueImpl implements AnnotateValue {

		private static final long serialVersionUID = -1L;

		@Nullable
		private Annotate annotate;


		//region Getter & Setter

		@Override
		public @Nullable Annotate getAnnotate() {
			return annotate;
		}

		public void setAnnotate(@NotNull Annotate annotate) {
			assertNonFrozen();
			this.annotate = annotate;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@NotNull Map<@NotNull String, @NotNull List<@NotNull AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			if (annotate != null) annotate.internalFreeze(map);
			return false;
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
			super.internalToJsonStart(builder, indentation);
			if (annotate != null) {
				builder.append(", \"annotate\": { ");
				annotate.internalToReferenceJson(builder);
				builder.append(" }");
			}
		}

		//endregion Jsonify

	}

}
