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

package io.github.thanhminhmr.javacia.jdt.tree.type;

import io.github.thanhminhmr.javacia.tree.type.JavaSimpleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SimpleType extends AbstractType implements JavaSimpleType {

	private static final long serialVersionUID = -1L;

	private @Nullable AbstractType innerType;


	public SimpleType(@NotNull String description) {
		super(description);
	}


	//region Getter & Setter

	@Override
	public @Nullable AbstractType getInnerType() {
		return innerType;
	}

	public void setInnerType(@Nullable AbstractType innerType) {
		assertNonFrozen();
		this.innerType = innerType;
	}

	//endregion Getter & Setter

	//region Jsonify

	@Override
	protected void internalToJsonStart(@NotNull StringBuilder builder, @NotNull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (innerType != null) {
			builder.append(", \"innerType\": { ");
			innerType.internalToReferenceJson(builder);
			builder.append(" }");
		}
	}

	//endregion Jsonify

}
