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

package mrmathami.cia.java.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.project.JavaModule;
import mrmathami.cia.java.project.JavaSourceFile;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public interface JavaNode extends JavaIdentifiedEntity {

	@Nonnull String ID_CLASS = "JavaNode";


	//region Basic Getter

	@Nonnull
	@Override
	default String getIdClass() {
		return ID_CLASS;
	}

	/**
	 * Return node id. Guarantee to be continuous, start from 0.
	 * Guarantee to satisfy <code>this == this.getRoot().getAllNodes().get(this.getId())</code>
	 *
	 * @return node id
	 */
	@Override
	int getId();

	boolean isRoot();

	@Nonnull
	JavaRootNode getRoot();

	@Nonnull
	JavaNode getParent();

	@Nonnull
	List<? extends JavaNode> getChildren();

	@Nonnull
	String getSimpleName();

	@Nonnull
	String getQualifiedName();

	@Nonnull
	String getUniqueName();

	@Nullable
	JavaSourceFile getSourceFile();

	@Nullable
	default JavaModule getModule() {
		final JavaSourceFile sourceFile = getSourceFile();
		return sourceFile != null ? sourceFile.getModule() : null;
	}

	//endregion Basic Getter

	//region Dependency

	@Nonnull
	Map<? extends JavaNode, ? extends JavaDependencyCountTable> getDependencyFrom();

	@Nonnull
	Map<? extends JavaNode, ? extends JavaDependencyCountTable> getDependencyTo();

	@Nonnull
	Set<? extends JavaNode> getDependencyFromNodes();

	@Nonnull
	Set<? extends JavaNode> getDependencyToNodes();

	//endregion Dependency

	//region Visit Iterator

	@Nonnull
	default Iterator<? extends JavaNode> getVisitIterator() {
		return new VisitIterator(this);
	}

	final class VisitIterator implements Iterator<JavaNode> {
		@Nonnull private final Stack<Iterator<? extends JavaNode>> stack = new Stack<>();
		@Nullable private JavaNode current;

		private VisitIterator(@Nullable JavaNode current) {
			this.current = current;
		}

		@Override
		public boolean hasNext() {
			if (current != null) stack.push(current.getChildren().iterator());
			this.current = null;
			do {
				if (stack.peek().hasNext()) return true;
				stack.pop();
			} while (!stack.isEmpty());
			return false;
		}

		@Override
		public JavaNode next() {
			if (current != null) stack.push(current.getChildren().iterator());
			return this.current = stack.peek().next();
		}
	}

	//endregion Visit Iterator

}
