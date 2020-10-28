/*
 * Copyright (C) 2020 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
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

package mrmathami.cia.java.jdt.tree.node.attribute;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.node.attribute.JavaParameterizedNode;
import mrmathami.cia.java.jdt.tree.AbstractIdentifiedEntity;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractParameterizedModifiedAnnotatedNode extends AbstractModifiedAnnotatedNode
		implements JavaParameterizedNode {

	private static final long serialVersionUID = -1L;

	@Nonnull private transient List<AbstractType> typeParameters = List.of();


	public AbstractParameterizedModifiedAnnotatedNode(@Nonnull AbstractNode parent, @Nonnull String simpleName) {
		super(parent, simpleName);
	}

	public AbstractParameterizedModifiedAnnotatedNode(@Nonnull AbstractNode parent, @Nonnull String simpleName,
			@Nonnull String uniqueNameSuffix) {
		super(parent, simpleName, uniqueNameSuffix);
	}


	//region Getter & Setter

	@Nonnull
	@Override
	public final List<AbstractType> getTypeParameters() {
		return isFrozen() ? typeParameters : Collections.unmodifiableList(typeParameters);
	}

	public final void setTypeParameters(@Nonnull List<AbstractType> typeParameters) {
		assertNonFrozen();
		this.typeParameters = typeParameters;
	}

	//endregion Getter & Setter

	//region Serialization Helper

	@Override
	public boolean internalFreeze(@Nonnull Map<String, List<AbstractIdentifiedEntity>> map) {
		if (super.internalFreeze(map)) return true;
		this.typeParameters = List.copyOf(typeParameters);
		for (final AbstractType typeParameter : typeParameters) typeParameter.internalFreeze(map);
		return false;
	}

	private void writeObject(@Nonnull ObjectOutputStream outputStream)
			throws IOException, UnsupportedOperationException {
		assertFrozen();
		outputStream.defaultWriteObject();
		outputStream.writeObject(typeParameters);
	}

	@SuppressWarnings("unchecked")
	private void readObject(@Nonnull ObjectInputStream inputStream)
			throws IOException, ClassNotFoundException, ClassCastException {
		inputStream.defaultReadObject();
		this.typeParameters = (List<AbstractType>) inputStream.readObject();
	}

	//endregion Serialization Helper

	//region Jsonify

	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		if (!typeParameters.isEmpty()) {
			builder.append(", \"typeParameters\": [");
			internalArrayToReferenceJson(builder, indentation, typeParameters);
			builder.append('\n').append(indentation).append(']');
		}
	}

	//endregion Jsonify

}
