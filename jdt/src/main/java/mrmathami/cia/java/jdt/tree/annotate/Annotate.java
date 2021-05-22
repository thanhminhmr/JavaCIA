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

package mrmathami.cia.java.jdt.tree.annotate;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.AbstractNonIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.tree.annotate.JavaAnnotate;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Annotate extends AbstractIdentifiedEntity implements JavaAnnotate {

	private static final long serialVersionUID = -1L;

	@Nonnull private final String name;
	@Nullable private AbstractNode node;
	@Nonnull private transient List<ParameterImpl> parameters = List.of();


	public Annotate(@Nonnull String name) {
		this.name = name;
	}


	//region Getter & Setter

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nullable
	@Override
	public AbstractNode getNode() {
		return node;
	}

	public void setNode(@Nullable AbstractNode node) {
		assertNonFrozen();
		this.node = node;
	}

	@Nonnull
	@Override
	public List<ParameterImpl> getParameters() {
		return isFrozen() ? parameters : Collections.unmodifiableList(parameters);
	}

	public void setParameters(@Nonnull List<ParameterImpl> parameters) {
		assertNonFrozen();
		this.parameters = parameters;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	// must be called when @Override
	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.parameters = List.copyOf(parameters);
		for (final ParameterImpl parameter : parameters) parameter.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(parameters);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.parameters = (List<ParameterImpl>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToReferenceJsonEnd(@Nonnull StringBuilder builder) {
		super.internalToReferenceJsonEnd(builder);
		builder.append(", \"name\": \"").append(name).append('"');
	}

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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

	@Nonnull
	@Override
	public final String toString() {
		return name;
	}


	public static final class ParameterImpl extends AbstractNonIdentifiedEntity implements Parameter {

		private static final long serialVersionUID = -1L;

		@Nonnull private final String name;
		@Nullable private ValueImpl value;
		@Nullable private AbstractNode node;


		public ParameterImpl(@Nonnull String name) {
			this.name = name;
		}


		//region Getter & Setter

		@Nonnull
		@Override
		public String getName() {
			return name;
		}

		@Nullable
		@Override
		public ValueImpl getValue() {
			return value;
		}

		public void setValue(@Nonnull ValueImpl value) {
			assertNonFrozen();
			this.value = value;
		}

		@Nullable
		@Override
		public AbstractNode getNode() {
			return node;
		}

		public void setNode(@Nonnull AbstractNode node) {
			assertNonFrozen();
			this.node = node;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			if (value != null) value.internalFreeze(map);
			return false;
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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

		@Nonnull private transient List<NonArrayValueImpl> values = List.of();


		//region Getter & Setter

		@Nonnull
		@Override
		public List<NonArrayValueImpl> getValues() {
			return values;
		}

		public void setValues(@Nonnull List<NonArrayValueImpl> values) {
			assertNonFrozen();
			this.values = values;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			this.values = List.copyOf(values);
			for (final NonArrayValueImpl value : values) value.internalFreeze(map);
			return false;
		}

		private void writeObject(@Nonnull ObjectOutputStream outputStream)
				throws IOException, UnsupportedOperationException {
			assertFrozen();
			outputStream.defaultWriteObject();
			outputStream.writeObject(values);
		}

		@SuppressWarnings("unchecked")
		private void readObject(@Nonnull ObjectInputStream inputStream)
				throws IOException, ClassNotFoundException, ClassCastException {
			inputStream.defaultReadObject();
			this.values = (List<NonArrayValueImpl>) inputStream.readObject();
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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

		@Nonnull private final Object value;


		public SimpleValueImpl(@Nonnull Object value) {
			if (!SimpleValue.isValidValueType(value)) throw new IllegalArgumentException("Invalid value type!");
			this.value = value;
		}


		//region Getter & Setter

		@Nonnull
		@Override
		public String getValueType() {
			return value.getClass().getSimpleName();
		}

		@Nonnull
		@Override
		public Object getValue() {
			return value;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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

		@Nonnull private final String describe;

		@Nullable private AbstractNode node;


		public NodeValueImpl(@Nonnull String describe) {
			this.describe = describe;
		}


		//region Getter & Setter

		@Nonnull
		@Override
		public String getDescribe() {
			return describe;
		}

		@Nullable
		@Override
		public AbstractNode getNode() {
			return node;
		}

		public void setNode(@Nonnull AbstractNode node) {
			assertNonFrozen();
			this.node = node;
		}

		//endregion Getter & Setter

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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

		@Nullable
		@Override
		public Annotate getAnnotate() {
			return annotate;
		}

		public void setAnnotate(@Nonnull Annotate annotate) {
			assertNonFrozen();
			this.annotate = annotate;
		}

		//endregion Getter & Setter

		//region Serialization Helper

		@Override
		public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
			if (super.internalFreeze(map)) return true;
			if (annotate != null) annotate.internalFreeze(map);
			return false;
		}

		//endregion Serialization Helper

		//region Jsonify

		@Override
		protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
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
