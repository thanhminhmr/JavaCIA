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

package mrmathami.cia.java.tree.annotate;

import mrmathami.cia.java.tree.JavaEntity;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;
import mrmathami.cia.java.tree.node.JavaNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface JavaAnnotate extends JavaIdentifiedEntity {

	@NotNull String OBJECT_CLASS = "JavaAnnotate";
	@NotNull String ID_CLASS = "JavaAnnotate";


	//region Basic Getter

	@Override
	default @NotNull String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Override
	default @NotNull String getIdClass() {
		return ID_CLASS;
	}

	//endregion Basic Getter

	//region Getter & Setter

	@NotNull String getName();

	@Nullable JavaNode getNode();

	@NotNull List<? extends Parameter> getParameters();

	//endregion Getter & Setter

	interface Parameter extends JavaEntity {

		//region Basic Getter

		@Override
		default @NotNull String getEntityClass() {
			return "JavaAnnotate.Parameter";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@NotNull String getName();

		@Nullable JavaNode getNode();

		@Nullable Value getValue();

		//endregion Getter & Setter

	}

	interface Value extends JavaEntity {
	}

	interface ArrayValue extends Value {

		//region Basic Getter

		@Override
		default @NotNull String getEntityClass() {
			return "JavaAnnotate.ArrayValue";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@NotNull List<? extends NonArrayValue> getValues();

		//endregion Getter & Setter

	}

	interface NonArrayValue extends Value {
	}

	interface SimpleValue extends NonArrayValue {

		//region Basic Getter

		@Override
		default @NotNull String getEntityClass() {
			return "JavaAnnotate.SimpleValue";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@NotNull String getValueType();

		@NotNull Object getValue();

		//endregion Getter & Setter

		//region Helper

		static boolean isValidValueType(@NotNull Object value) {
			return value instanceof String || value instanceof Boolean || value instanceof Byte
					|| value instanceof Short || value instanceof Integer || value instanceof Long
					|| value instanceof Character || value instanceof Float || value instanceof Double;
		}

		//endregion Helper

	}

	interface NodeValue extends NonArrayValue {

		//region Basic Getter

		@Override
		default @NotNull String getEntityClass() {
			return "JavaAnnotate.NodeValue";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@NotNull String getDescribe();

		@Nullable JavaNode getNode();

		//endregion Getter & Setter

	}

	interface AnnotateValue extends NonArrayValue {

		//region Basic Getter

		@Override
		default @NotNull String getEntityClass() {
			return "JavaAnnotate.AnnotateValue";
		}

		//endregion Basic Getter

		//region Getter & Setter

		@Nullable JavaAnnotate getAnnotate();

		//endregion Getter & Setter

	}

}
