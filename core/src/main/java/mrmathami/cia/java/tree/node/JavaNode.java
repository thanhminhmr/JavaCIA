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

package mrmathami.cia.java.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;
import mrmathami.cia.java.tree.dependency.JavaDependencyCountTable;
import mrmathami.cia.java.tree.node.container.*;

import java.util.ArrayList;
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
	String getNodeName();

	@Nonnull
	String getQualifiedName();

	@Nonnull
	String getUniqueName();

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

	//region Node Type

	@Nonnull
	default JavaAnnotationNode asAnnotationNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaAnnotationNode!");
	}

	@Nonnull
	default JavaClassNode asClassNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaClassNode!");
	}

	@Nonnull
	default JavaEnumNode asEnumNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaEnumNode!");
	}

	@Nonnull
	default JavaFieldNode asFieldNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaFieldNode!");
	}

	@Nonnull
	default JavaInitializerNode asInitializerNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaInitializerNode!");
	}

	@Nonnull
	default JavaInterfaceNode asInterfaceNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaInterfaceNode!");
	}

	@Nonnull
	default JavaMethodNode asMethodNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaMethodNode!");
	}

	@Nonnull
	default JavaPackageNode asPackageNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaPackageNode!");
	}
	@Nonnull
	default JavaXMLNode asXMLNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaXMLNode!");
	}

	@Nonnull
	default JavaRootNode asRootNode() throws ClassCastException {
		throw new ClassCastException("Not a JavaRootNode!");
	}

	//endregion Node Type

	//region Container Type

	@Nonnull
	default <E extends JavaNode> List<E> getChildren(@Nonnull Class<E> filterClass) {
		return getChildren(filterClass, new ArrayList<>());
	}

	@Nonnull
	default <E extends JavaNode> List<E> getChildren(@Nonnull Class<E> filterClass, @Nonnull List<E> nodeList) {
		for (final JavaNode childNode : getChildren()) {
			if (filterClass.isInstance(childNode)) nodeList.add(filterClass.cast(childNode));
		}
		return nodeList;
	}

	//region Annotation Container

	@Nonnull
	default List<? extends JavaAnnotationNode> getChildAnnotations(@Nonnull List<JavaAnnotationNode> annotationNodes) {
		return annotationNodes;
	}

	@Nonnull
	default List<? extends JavaAnnotationNode> getChildAnnotations() {
		return List.of();
	}

	@Nonnull
	default JavaAnnotationContainer asAnnotationContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaAnnotationContainer!");
	}

	//endregion Annotation Container

	//region Class Container

	@Nonnull
	default List<? extends JavaClassNode> getChildClasses(@Nonnull List<JavaClassNode> classNodes) {
		return classNodes;
	}

	@Nonnull
	default List<? extends JavaClassNode> getChildClasses() {
		return List.of();
	}

	@Nonnull
	default JavaClassContainer asClassContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaClassContainer!");
	}

	//endregion Class Container

	//region Enum Container

	@Nonnull
	default List<? extends JavaEnumNode> getChildEnums(@Nonnull List<JavaEnumNode> enumNodes) {
		return enumNodes;
	}

	@Nonnull
	default List<? extends JavaEnumNode> getChildEnums() {
		return List.of();
	}

	@Nonnull
	default JavaEnumContainer asEnumContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaEnumContainer!");
	}

	//endregion Enum Container

	//region Field Container

	@Nonnull
	default List<? extends JavaFieldNode> getChildFields(@Nonnull List<JavaFieldNode> fieldNodes) {
		return fieldNodes;
	}

	@Nonnull
	default List<? extends JavaFieldNode> getChildFields() {
		return List.of();
	}

	@Nonnull
	default JavaFieldContainer asFieldContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaFieldContainer!");
	}

	//endregion Field Container

	//region Initializer Container

	@Nonnull
	default List<? extends JavaInitializerNode> getChildInitializers(@Nonnull List<JavaInitializerNode> initializerNodes) {
		return initializerNodes;
	}

	@Nonnull
	default List<? extends JavaInitializerNode> getChildInitializers() {
		return List.of();
	}

	@Nonnull
	default JavaInitializerContainer asInitializerContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaInitializerContainer!");
	}

	//endregion Initializer Container

	//region Interface Container

	@Nonnull
	default List<? extends JavaInterfaceNode> getChildInterfaces(@Nonnull List<JavaInterfaceNode> interfaceNodes) {
		return interfaceNodes;
	}

	@Nonnull
	default List<? extends JavaInterfaceNode> getChildInterfaces() {
		return List.of();
	}

	@Nonnull
	default JavaInterfaceContainer asInterfaceContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaInterfaceContainer!");
	}

	//endregion Interface Container

	//region Method Container

	@Nonnull
	default List<? extends JavaMethodNode> getChildMethods(@Nonnull List<JavaMethodNode> methodNodes) {
		return methodNodes;
	}

	@Nonnull
	default List<? extends JavaMethodNode> getChildMethods() {
		return List.of();
	}

	@Nonnull
	default JavaMethodContainer asMethodContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaMethodContainer!");
	}

	//endregion Method Container

	//region XML Container

	@Nonnull
	default List<? extends JavaXMLNode> getChildXMLNodes(@Nonnull List<JavaXMLNode> xmlNodes) {
		return xmlNodes;
	}

	@Nonnull
	default List<? extends JavaXMLNode> getChildXMLNodes() {
		return List.of();
	}

	@Nonnull
	default JavaXMLContainer asXMLContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaXMLContainer");
	}

	//end region XML Container

	//region Package Container

	@Nonnull
	default List<? extends JavaPackageNode> getChildPackages(@Nonnull List<JavaPackageNode> packageNodes) {
		return packageNodes;
	}

	@Nonnull
	default List<? extends JavaPackageNode> getChildPackages() {
		return List.of();
	}

	@Nonnull
	default JavaPackageContainer asPackageContainer() throws ClassCastException {
		throw new ClassCastException("Not a JavaPackageContainer!");
	}

	//endregion Package Container

	//endregion Container Type

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
