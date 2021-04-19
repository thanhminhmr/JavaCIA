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

package mrmathami.cia.java.jdt.project.builder;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.Module;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.node.AnnotationNode;
import mrmathami.cia.java.jdt.tree.node.ClassNode;
import mrmathami.cia.java.jdt.tree.node.EnumNode;
import mrmathami.cia.java.jdt.tree.node.FieldNode;
import mrmathami.cia.java.jdt.tree.node.InitializerNode;
import mrmathami.cia.java.jdt.tree.node.InterfaceNode;
import mrmathami.cia.java.jdt.tree.node.MethodNode;
import mrmathami.cia.java.jdt.tree.node.PackageNode;
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.jdt.tree.node.XMLNode;
import mrmathami.cia.java.jdt.tree.type.AbstractType;
import mrmathami.cia.java.tree.JavaModifier;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.node.container.JavaAnnotationContainer;
import mrmathami.cia.java.tree.node.container.JavaClassContainer;
import mrmathami.cia.java.tree.node.container.JavaEnumContainer;
import mrmathami.cia.java.tree.node.container.JavaFieldContainer;
import mrmathami.cia.java.tree.node.container.JavaInitializerContainer;
import mrmathami.cia.java.tree.node.container.JavaInterfaceContainer;
import mrmathami.cia.java.tree.node.container.JavaMethodContainer;
import mrmathami.cia.java.utils.RelativePath;
import mrmathami.utils.Pair;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

final class JavaNodes {

	@Nonnull private final CodeFormatter codeFormatter;
	private final boolean enableRecovery;

	@Nonnull private final JavaDependencies dependencies = new JavaDependencies();
	@Nonnull private final JavaAnnotates annotates = new JavaAnnotates(dependencies);
	@Nonnull private final JavaTypes types = new JavaTypes(dependencies, annotates);


	@Nonnull private final RootNode rootNode = new RootNode();
	@Nonnull private final Map<String, Pair<PackageNode, IPackageBinding>> packageNodeMap = new HashMap<>();

	@Nonnull private final Map<IBinding, AbstractNode> bindingNodeMap = new HashMap<>();

	@Nonnull private final Map<AbstractNode, Pair<
			Pair<InitializerNode, List<InitializerNode.InitializerImpl>>,
			Pair<InitializerNode, List<InitializerNode.InitializerImpl>>>>
			classInitializerMap = new IdentityHashMap<>();

	@Nonnull private final List<Pair<ASTNode, AbstractNode>> delayedDependencyWalkers = new LinkedList<>();

	@Nullable private SourceFile sourceFile;
	private Map<String, List<XMLNode>> mapXMlDependency;


	JavaNodes(@Nonnull CodeFormatter formatter, boolean enableRecovery) {
		this.codeFormatter = formatter;
		this.enableRecovery = enableRecovery;
	}

	void build(@Nonnull SourceFile sourceFile, @Nonnull CompilationUnit compilationUnit, Map<String, List<XMLNode>> mapXMlDependency)
			throws JavaCiaException {
		this.sourceFile = sourceFile;
		this.mapXMlDependency = mapXMlDependency;

		final PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		if (packageDeclaration != null) {
			final PackageNode packageNode = createPackageNodeFromPackageDeclaration(packageDeclaration);
			for (Object type : compilationUnit.types()) {
				if (type instanceof AbstractTypeDeclaration) {
					parseAbstractTypeDeclaration(packageNode, (AbstractTypeDeclaration) type);
				}
			}
		} else {
			// the parent is root
			for (Object type : compilationUnit.types()) {
				if (type instanceof AbstractTypeDeclaration) {
					parseAbstractTypeDeclaration(rootNode, (AbstractTypeDeclaration) type);
				}
			}
		}

		this.sourceFile = null;
	}

	@Nonnull
	private PackageNode createPackageNodeFromPath(@Nonnull RelativePath relativePath) {
		List<String> components = relativePath.getComponents();
		String[] nameComponents = components.toArray(new String[0]);

		final Pair<PackageNode, IPackageBinding> oldPair = packageNodeMap.get(nameComponents[nameComponents.length - 1]);
		final Pair<PackageNode, IPackageBinding> pair = oldPair != null
				? oldPair
				: internalCreatePackagePairFromNameComponents(nameComponents);
		PackageNode packageNode = pair.getA();
		System.out.println("packageNode " + packageNode);
		return packageNode;
	}

	//for xml file in src
	void build(@Nonnull SourceFile sourceFile, @Nonnull org.w3c.dom.Document document, String pathFile) {
		this.sourceFile = sourceFile;
		final Module module = sourceFile.getModule();
		DocumentTraversal traversal = (DocumentTraversal) document;
		PackageNode packageNode = createPackageNodeFromPath(sourceFile.getRelativePath());

		TreeWalker walker = traversal.createTreeWalker(document.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT, null, true);

		XMLNode rootNode = packageNode.createChildXMlNode(sourceFile, document.getDocumentElement().getNodeName(), document.getDocumentElement().getTextContent(), document.getDocumentElement().getChildNodes(),
				document.getDocumentElement().getAttributes());

		traverseLevel(walker, rootNode, false);

		dependencies.createDependencyToNode(packageNode, rootNode, JavaDependency.MEMBER);

		this.sourceFile = null;
	}

	private void traverseLevel(TreeWalker walker, XMLNode parent, boolean isFirstChild) {
		Node currentNode = walker.getCurrentNode();
		if (!isFirstChild) {
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, parent, true);
			}
		} else {
			XMLNode xmlNode = parent.createChildXMlNode(sourceFile, currentNode.getNodeName(), currentNode.getTextContent(), currentNode.getChildNodes(), currentNode.getAttributes());
			dependencies.createDependencyToNode(parent, xmlNode, JavaDependency.MEMBER);
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, xmlNode, true);
			}
		}
		walker.setCurrentNode(currentNode);
	}

	//for mapper file
	void build(@Nonnull SourceFile sourceFile, @Nonnull org.w3c.dom.Document document, String pathFile, Map<String, List<XMLNode>> mapXMlDependency) {
		this.sourceFile = sourceFile;
		DocumentTraversal traversal = (DocumentTraversal) document;
		PackageNode packageNode = createPackageNodeFromPath(sourceFile.getRelativePath());

		TreeWalker walker = traversal.createTreeWalker(document.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT, null, true);

		XMLNode rootNode = packageNode.createChildXMlNode(sourceFile, document.getDocumentElement().getNodeName(), document.getDocumentElement().getTextContent(), document.getDocumentElement().getChildNodes(),
				document.getDocumentElement().getAttributes());

		String namespace = document.getDocumentElement().getAttribute("namespace");
		if (namespace.contains(".")) {
			putInMap(mapXMlDependency, namespace, rootNode);
			traverseLevel(walker, rootNode, false, mapXMlDependency);
		} else {
			traverseLevel(walker, rootNode, false, mapXMlDependency, namespace);
		}

		dependencies.createDependencyToNode(packageNode, rootNode, JavaDependency.MEMBER);

		if (mapXMlDependency.containsKey(pathFile)) {
			for (int i = 0; i < mapXMlDependency.get(pathFile).size(); i++) {
				dependencies.createDependencyToNode(mapXMlDependency.get(pathFile).get(i), rootNode, JavaDependency.USE);
			}
		}
		this.sourceFile = null;
	}

	private void traverseLevel(TreeWalker walker, XMLNode parent, boolean isFirstChild, Map<String, List<XMLNode>> mapXMlDependency) {
		Node currentNode = walker.getCurrentNode();
		if (!isFirstChild) {
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, parent, true, mapXMlDependency);
			}
		} else {
			XMLNode xmlNode = parent.createChildXMlNode(sourceFile, currentNode.getNodeName(), currentNode.getTextContent(), currentNode.getChildNodes(), currentNode.getAttributes());
			dependencies.createDependencyToNode(parent, xmlNode, JavaDependency.MEMBER);
			NamedNodeMap listAttributes = xmlNode.getAttributes();
			if (xmlNode.getSimpleName().equals("select")) {
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("resultMap")) {
						putInMap(mapXMlDependency, listAttributes.item(i).getNodeValue(), xmlNode);
					}
				}
			}
			if (xmlNode.getSimpleName().equals("resultMap")) {
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("id")) {
						String key = listAttributes.item(i).getNodeValue();
						if (mapXMlDependency.containsKey(key)) {
							for (int j = 0; j < mapXMlDependency.get(key).size(); j++) {
								dependencies.createDependencyToNode(mapXMlDependency.get(key).get(j), xmlNode, JavaDependency.USE);
							}
						}
					}
				}
			}
			for (int i = 0; i < listAttributes.getLength(); i++) {
				if (listAttributes.item(i).getNodeName().equals("parameterType") || listAttributes.item(i).getNodeName().equals("resultType")) {
					String key = listAttributes.item(i).getNodeValue();
					if (mapXMlDependency.containsKey(key)) {
						for (int j = 0; j < mapXMlDependency.get(key).size(); j++) {
							dependencies.createDependencyToNode(xmlNode, mapXMlDependency.get(key).get(j), JavaDependency.USE);
						}
					}
				}
			}
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, xmlNode, true, mapXMlDependency);
			}
		}
		walker.setCurrentNode(currentNode);
	}

	private void traverseLevel(TreeWalker walker, XMLNode parent, boolean isFirstChild, Map<String, List<XMLNode>> mapXMlDependency, String namespace) {
		Node currentNode = walker.getCurrentNode();
		if (!isFirstChild) {
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, parent, true, mapXMlDependency, namespace);
			}
		} else {
			XMLNode xmlNode = parent.createChildXMlNode(sourceFile, currentNode.getNodeName(), currentNode.getTextContent(), currentNode.getChildNodes(), currentNode.getAttributes());
			dependencies.createDependencyToNode(parent, xmlNode, JavaDependency.MEMBER);
			NamedNodeMap listAttributes = xmlNode.getAttributes();
			if (xmlNode.getSimpleName().equals("select")) {
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("resultMap")) {
						putInMap(mapXMlDependency, listAttributes.item(i).getNodeValue(), xmlNode);
					}
				}
			}
			if (xmlNode.getSimpleName().equals("resultMap")) {
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("id")) {
						String key = listAttributes.item(i).getNodeValue();
						if (mapXMlDependency.containsKey(key)) {
							for (int j = 0; j < mapXMlDependency.get(key).size(); j++) {
								dependencies.createDependencyToNode(mapXMlDependency.get(key).get(j), xmlNode, JavaDependency.USE);
							}
						}
					}
				}
			}
			for (int i = 0; i < listAttributes.getLength(); i++) {
				if (listAttributes.item(i).getNodeName().equals("parameterType") || listAttributes.item(i).getNodeName().equals("resultType") || listAttributes.item(i).getNodeName().equals("type")) {
					String value = listAttributes.item(i).getNodeValue();
					if (value.contains(".")) {
						putInMap(mapXMlDependency, value, xmlNode);
					}
					if (mapXMlDependency.containsKey(value) && !value.contains(".")) {
						for (int j = 0; j < mapXMlDependency.get(value).size(); j++) {
							dependencies.createDependencyToNode(xmlNode, mapXMlDependency.get(value).get(j), JavaDependency.USE);
						}
					}
				} else if (listAttributes.item(i).getNodeName().equals("id") && !xmlNode.getSimpleName().equals("resultMap")) {
					String value = listAttributes.item(i).getNodeValue();
					putInMap(mapXMlDependency, namespace + "." + value, xmlNode);
				}
			}
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, xmlNode, true, mapXMlDependency, namespace);
			}
		}
		walker.setCurrentNode(currentNode);
	}


	//for configuration is not in src
	void build(org.w3c.dom.Document document, @Nonnull String[] sourcePathArray, Map<String, List<XMLNode>> mapXMLDependency, Path sourcePath) {
		System.out.println("xml node " + document.getDocumentElement().getNodeName());
		XMLNode rootXMLNode = rootNode.createChildXMlNode(sourceFile, document.getDocumentElement().getNodeName(), document.getDocumentElement().getTextContent(),
				document.getDocumentElement().getChildNodes(), document.getDocumentElement().getAttributes());
		String key = String.valueOf(sourcePath.getFileName());
		putInMap(mapXMLDependency, key, rootXMLNode);

		DocumentTraversal traversal = (DocumentTraversal) document;
		TreeWalker walker = traversal.createTreeWalker(document.getDocumentElement(),
				NodeFilter.SHOW_ELEMENT, null, true);
		traverseLevel(walker, rootXMLNode, false, sourcePathArray, mapXMLDependency);
		dependencies.createDependencyToNode(rootNode, rootXMLNode, JavaDependency.MEMBER);
	}

	private void traverseLevel(TreeWalker walker, XMLNode parent, boolean isFirstChild, String[] sourcePathArray, Map<String, List<XMLNode>> mapXMLDependency) {
		Node currentNode = walker.getCurrentNode();
		if (!isFirstChild) {
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, parent, true, sourcePathArray, mapXMLDependency);
			}
		} else {
			XMLNode xmlNode = parent.createChildXMlNode(sourceFile, currentNode.getNodeName(), currentNode.getTextContent(), currentNode.getChildNodes(), currentNode.getAttributes());
			dependencies.createDependencyToNode(parent, xmlNode, JavaDependency.MEMBER);
			if (xmlNode.getSimpleName().equals("typeAlias")) {
				NamedNodeMap listAttributes = xmlNode.getAttributes();
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("alias")) {
						putInMap(mapXMLDependency, listAttributes.item(i).getNodeValue(), xmlNode);
					} else if (listAttributes.item(i).getNodeName().equals("type")) {
						putInMap(mapXMLDependency, listAttributes.item(i).getNodeValue(), xmlNode);
					}
				}
			} else if (xmlNode.getSimpleName().equals("mapper")) {
				NamedNodeMap listAttributes = xmlNode.getAttributes();
				for (int i = 0; i < listAttributes.getLength(); i++) {
					if (listAttributes.item(i).getNodeName().equals("resource")) {
						String value = listAttributes.item(i).getNodeValue();
						String path = convertResourceToPath(sourcePathArray, value);
						putInMap(mapXMLDependency, path, xmlNode);
					}
				}
			}
			for (Node n = walker.firstChild(); n != null; n = walker.nextSibling()) {
				traverseLevel(walker, xmlNode, true, sourcePathArray, mapXMLDependency);
			}
		}
		walker.setCurrentNode(currentNode);
	}

	public static void putInMap(Map<String, List<XMLNode>> mapXMLDependency, String key, XMLNode xmlNode) {
		if (mapXMLDependency.containsKey(key)) {
			if (!mapXMLDependency.get(key).contains(xmlNode)) {
				mapXMLDependency.get(key).add(xmlNode);
			}
		} else {
			mapXMLDependency.put(key, new ArrayList<>(Arrays.asList(xmlNode)));
		}
	}

	public static String convertResourceToPath(@Nonnull String[] sourcePathArray, String resource) {
		String fullPath = "";
		for (String sourcePath : sourcePathArray) {
			String[] temp = resource.split("/");
			boolean sign = true;
			for (String s : temp) {
				if (!sourcePath.contains(s)) {
					sign = false;
					break;
				}
			}
			if (sign) {
				fullPath = sourcePath;
			}
		}
		return fullPath;
	}

	@Nonnull
	RootNode postprocessing() throws JavaCiaException {
		// delay dependency walkers
		for (final Pair<ASTNode, AbstractNode> pair : delayedDependencyWalkers) {
			walkDependency(pair.getA(), pair.getB());
		}
		delayedDependencyWalkers.clear();

		// delay reference type node
		types.postprocessing(bindingNodeMap);

		// delay annotations
		annotates.postprocessing(bindingNodeMap);

		// delay dependencies
		dependencies.postProcessing(bindingNodeMap);

		// set class initializers
		classInitializerMap.clear();

		// final cleanup
		packageNodeMap.clear();
		bindingNodeMap.clear();

		// freeze root node
		rootNode.freeze();
		mapXMlDependency.clear();
		return rootNode;
	}


	@Nonnull
	private String format(@Nonnull String code, int type) throws JavaCiaException {
		final TextEdit textEdit = codeFormatter.format(type, code, 0, code.length(), 0, "\n");
		final IDocument doc = new Document(code);
		try {
			textEdit.apply(doc, TextEdit.NONE);
			return doc.get();
		} catch (MalformedTreeException | BadLocationException e) {
			throw new JavaCiaException("Cannot format source code!", e);
		}
	}


	//region Modifier

	private static int getModifierMask(@Nonnull JavaModifier modifier) {
		return 1 << modifier.ordinal();
	}

	private int processModifiersFromBindingModifiers(int bindingModifiers) {
		int modifiers = 0;
		if ((bindingModifiers & Modifier.PUBLIC) != 0) modifiers |= getModifierMask(JavaModifier.PUBLIC);
		if ((bindingModifiers & Modifier.PRIVATE) != 0) modifiers |= getModifierMask(JavaModifier.PRIVATE);
		if ((bindingModifiers & Modifier.PROTECTED) != 0) modifiers |= getModifierMask(JavaModifier.PROTECTED);
		if ((bindingModifiers & Modifier.STATIC) != 0) modifiers |= getModifierMask(JavaModifier.STATIC);
		if ((bindingModifiers & Modifier.SYNCHRONIZED) != 0) modifiers |= getModifierMask(JavaModifier.SYNCHRONIZED);
		if ((bindingModifiers & Modifier.VOLATILE) != 0) modifiers |= getModifierMask(JavaModifier.VOLATILE);
		if ((bindingModifiers & Modifier.TRANSIENT) != 0) modifiers |= getModifierMask(JavaModifier.TRANSIENT);
		if ((bindingModifiers & Modifier.NATIVE) != 0) modifiers |= getModifierMask(JavaModifier.NATIVE);
		if ((bindingModifiers & Modifier.STRICTFP) != 0) modifiers |= getModifierMask(JavaModifier.STRICTFP);
		return modifiers;
	}

	//endregion Modifier

	//region Package

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreateFirstLevelPackagePairFromName(
			@Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(simpleName, qualifiedName -> {
			final PackageNode packageNode = rootNode.createChildPackage(simpleName);
			dependencies.createDependencyToNode(rootNode, packageNode, JavaDependency.MEMBER);
			return Pair.mutableOf(packageNode, null);
		});
	}

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreatePackagePairFromParentAndName(
			@Nonnull PackageNode parentNode, @Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(parentNode.getQualifiedName() + '.' + simpleName,
				qualifiedName -> {
					final PackageNode packageNode = parentNode.createChildPackage(simpleName);
					dependencies.createDependencyToNode(parentNode, packageNode, JavaDependency.MEMBER);
					return Pair.mutableOf(packageNode, null);
				});
	}

	@Nonnull
	private Pair<PackageNode, IPackageBinding> internalCreatePackagePairFromNameComponents(
			@Nonnull String[] nameComponents) {
		assert nameComponents.length > 0 : "nameComponents length should not be 0.";
		Pair<PackageNode, IPackageBinding> pair = internalCreateFirstLevelPackagePairFromName(nameComponents[0]);
		for (int i = 1; i < nameComponents.length; i++) {
			pair = internalCreatePackagePairFromParentAndName(pair.getA(), nameComponents[i]);
		}
		return pair;
	}

	@Nonnull
	private PackageNode createFirstLevelPackageFromName(@Nonnull String simpleName) {
		return internalCreateFirstLevelPackagePairFromName(simpleName).getA();
	}

	@Nonnull
	private PackageNode createPackageFromParentAndName(@Nonnull PackageNode parentNode, @Nonnull String simpleName) {
		return internalCreatePackagePairFromParentAndName(parentNode, simpleName).getA();
	}

	@Nonnull
	private PackageNode createPackageFromNameComponents(@Nonnull String[] nameComponents) {
		return internalCreatePackagePairFromNameComponents(nameComponents).getA();
	}

	@Nonnull
	private PackageNode createPackageNodeFromPackageDeclaration(@Nonnull PackageDeclaration packageDeclaration)
			throws JavaCiaException {
		final IPackageBinding packageBinding = packageDeclaration.resolveBinding();
		if (packageBinding == null) throw new JavaCiaException("Cannot resolve binding on package declaration!");

		final Pair<PackageNode, IPackageBinding> oldPair = packageNodeMap.get(packageBinding.getName());
		final Pair<PackageNode, IPackageBinding> pair = oldPair != null
				? oldPair
				: internalCreatePackagePairFromNameComponents(packageBinding.getNameComponents());
		final PackageNode packageNode = pair.getA();
		if (pair.getB() == null) {
			pair.setB(packageBinding);
			packageNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(packageBinding.getAnnotations(),
					packageNode, JavaDependency.USE));
		}

		// TODO: add to node set
//		assert sourceFile != null;
//		for (AbstractNode node = packageNode; !node.isRoot(); node = node.getParent()) {
//			sourceFile.add(node);
//		}

		return packageNode;
	}

	//endregion Package

	//region Parser

	private void parseBodyDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull BodyDeclaration bodyDeclaration) throws JavaCiaException {
		if (bodyDeclaration instanceof AbstractTypeDeclaration) {
			parseAbstractTypeDeclaration(parentNode, (AbstractTypeDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof AnnotationTypeMemberDeclaration) {
			parseAnnotationTypeMemberDeclaration(parentNode, (AnnotationTypeMemberDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof EnumConstantDeclaration) {
			parseEnumConstantDeclaration(parentNode, (EnumConstantDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof FieldDeclaration) {
			parseFieldDeclaration(parentNode, (FieldDeclaration) bodyDeclaration);
		} else if (bodyDeclaration instanceof Initializer) {
			parseInitializer(parentNode, (Initializer) bodyDeclaration);
		} else if (bodyDeclaration instanceof MethodDeclaration) {
			parseMethodDeclaration(parentNode, (MethodDeclaration) bodyDeclaration);
		}
	}

	private void parseAbstractTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AbstractTypeDeclaration abstractTypeDeclaration) throws JavaCiaException {
		if (abstractTypeDeclaration instanceof TypeDeclaration) {
			parseTypeDeclaration(parentNode, (TypeDeclaration) abstractTypeDeclaration);
		} else if (abstractTypeDeclaration instanceof EnumDeclaration) {
			parseEnumDeclaration(parentNode, (EnumDeclaration) abstractTypeDeclaration);
		} else if (abstractTypeDeclaration instanceof AnnotationTypeDeclaration) {
			parseAnnotationTypeDeclaration(parentNode, (AnnotationTypeDeclaration) abstractTypeDeclaration);
		}
	}

	private void parseTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull TypeDeclaration typeDeclaration) throws JavaCiaException {
		if (typeDeclaration.isInterface()) {
			parseInterfaceTypeDeclaration(parentNode, typeDeclaration);
		} else {
			parseClassTypeDeclaration(parentNode, typeDeclaration);
		}
	}

	private void parseClassTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull TypeDeclaration typeDeclaration) throws JavaCiaException {
		assert !typeDeclaration.isInterface() : "Expected a class TypeDeclaration.";
		assert parentNode instanceof JavaClassContainer : "Expected a JavaClassContainer parent.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final ClassNode classNode
				= parentNode.createChildClass(sourceFile, typeBinding.getName(), typeBinding.getBinaryName());
		dependencies.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		if (mapXMlDependency.containsKey(classNode.getBinaryName())) {
			for (int i = 0; i < mapXMlDependency.get(classNode.getBinaryName()).size(); i++) {
				dependencies.createDependencyToNode(mapXMlDependency.get(classNode.getBinaryName()).get(i), classNode, JavaDependency.USE);
			}
		}

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(classNode);

		internalParseClassTypeBinding(classNode, typeBinding, typeDeclaration.bodyDeclarations());
	}

	private void internalParseClassTypeBinding(@Nonnull ClassNode classNode, @Nonnull ITypeBinding typeBinding,
			@Nonnull List<?> bodyDeclarations) throws JavaCiaException {
		// put binding map
		bindingNodeMap.put(typeBinding, classNode);

		// set annotate
		classNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				classNode, JavaDependency.USE));

		// set modifier
		classNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		classNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				classNode, JavaDependency.USE));

		// set extends class
		final ITypeBinding superClassBinding = typeBinding.getSuperclass();
		if (superClassBinding != null) {
			classNode.setExtendsClass(types.createTypeFromTypeBinding(superClassBinding,
					classNode, JavaDependency.INHERITANCE));
		}

		// set implements interfaces
		classNode.setImplementsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				classNode, JavaDependency.INHERITANCE));

		// process children
		for (final Object object : bodyDeclarations) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(classNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseInterfaceTypeDeclaration(@Nonnull AbstractNode parentNode, @Nonnull TypeDeclaration typeDeclaration)
			throws JavaCiaException {
		assert !typeDeclaration.isInterface() : "Expected a interface TypeDeclaration.";
		assert parentNode instanceof JavaInterfaceContainer : "Expected a JavaInterfaceContainer parent.";

		final ITypeBinding typeBinding = typeDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on interface declaration!");

		final InterfaceNode interfaceNode = parentNode
				.createChildInterface(sourceFile, typeBinding.getName(), typeBinding.getBinaryName());
		dependencies.createDependencyToNode(parentNode, interfaceNode, JavaDependency.MEMBER);

		if (mapXMlDependency.containsKey(interfaceNode.getBinaryName())) {
			for (int i = 0; i < mapXMlDependency.get(interfaceNode.getBinaryName()).size(); i++) {
				dependencies.createDependencyToNode(mapXMlDependency.get(interfaceNode.getBinaryName()).get(i), interfaceNode, JavaDependency.USE);
			}
		}

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(interfaceNode);

		// put binding map
		bindingNodeMap.put(typeBinding, interfaceNode);

		// set annotate
		interfaceNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				interfaceNode, JavaDependency.USE));

		// set modifier
		interfaceNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		interfaceNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				interfaceNode, JavaDependency.USE));

		// set extends interfaces
		interfaceNode.setExtendsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				interfaceNode, JavaDependency.INHERITANCE));

		// process children
		for (final Object object : typeDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(interfaceNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseEnumDeclaration(@Nonnull AbstractNode parentNode, @Nonnull EnumDeclaration enumDeclaration)
			throws JavaCiaException {
		assert parentNode instanceof JavaEnumContainer : "Expected a JavaEnumContainer parent.";

		final ITypeBinding typeBinding = enumDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on enum declaration!");

		final EnumNode enumNode
				= parentNode.createChildEnum(sourceFile, typeBinding.getName(), typeBinding.getBinaryName());
		dependencies.createDependencyToNode(parentNode, enumNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(enumNode);

		// put binding map
		bindingNodeMap.put(typeBinding, enumNode);

		// set annotate
		enumNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(typeBinding.getAnnotations(),
				enumNode, JavaDependency.USE));

		// set modifier
		enumNode.setModifiers(processModifiersFromBindingModifiers(typeBinding.getModifiers()));

		// set type parameter
		enumNode.setTypeParameters(types.createTypesFromTypeBindings(typeBinding.getTypeParameters(),
				enumNode, JavaDependency.USE));

		// set implements interfaces
		enumNode.setImplementsInterfaces(types.createTypesFromTypeBindings(typeBinding.getInterfaces(),
				enumNode, JavaDependency.INHERITANCE));

		// process enum constant children
		for (final Object object : enumDeclaration.enumConstants()) {
			if (object instanceof EnumConstantDeclaration) {
				parseBodyDeclaration(enumNode, (EnumConstantDeclaration) object);
			}
		}

		// process children
		for (final Object object : enumDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(enumNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseEnumConstantDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull EnumConstantDeclaration enumConstantDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaFieldContainer : "Expected a JavaFieldContainer parent.";

		final IVariableBinding variableBinding = enumConstantDeclaration.resolveVariable();
		if (variableBinding == null) throw new JavaCiaException("Cannot resolve binding on enum constant declaration!");

		final FieldNode fieldNode = parentNode.createChildField(sourceFile, variableBinding.getName());
		dependencies.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(fieldNode);

		internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

		// put constructor invocation dependency
		final IMethodBinding constructorBinding = enumConstantDeclaration.resolveConstructorBinding();
		if (constructorBinding != null) {
			dependencies.createDelayDependency(fieldNode,
					JavaDependencies.getOriginMethodBinding(constructorBinding), JavaDependency.INVOCATION);
		}

		// put delayed variable initializer
		for (final Object object : enumConstantDeclaration.arguments()) {
			if (object instanceof Expression) {
				walkDeclaration((Expression) object, fieldNode, fieldNode);
			}
		}

		// parse anonymous class declaration
		final AnonymousClassDeclaration anonymousClassDeclaration
				= enumConstantDeclaration.getAnonymousClassDeclaration();
		if (anonymousClassDeclaration != null) {
			parseAnonymousClassDeclaration(fieldNode, anonymousClassDeclaration);
		}
	}

	private void internalEnumConstantOrFieldVariableBinding(@Nonnull FieldNode fieldNode,
			@Nonnull IVariableBinding variableBinding) throws JavaCiaException {
		// put binding map
		bindingNodeMap.put(variableBinding, fieldNode);

		// set annotate
		fieldNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(variableBinding.getAnnotations(),
				fieldNode, JavaDependency.USE));

		// set modifier
		fieldNode.setModifiers(processModifiersFromBindingModifiers(variableBinding.getModifiers()));

		// set type
		fieldNode.setType(types.createTypeFromTypeBinding(variableBinding.getType(), fieldNode, JavaDependency.USE));
	}

	private void parseAnonymousClassDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnonymousClassDeclaration anonymousClassDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaClassContainer : "Expected a JavaClassContainer parent.";

		final ITypeBinding typeBinding = anonymousClassDeclaration.resolveBinding();
		if (typeBinding == null) throw new JavaCiaException("Cannot resolve binding on class declaration!");

		final String binaryName = typeBinding.getBinaryName();
		final String className = binaryName.contains("$")
				? binaryName.substring(binaryName.lastIndexOf('$'))
				: binaryName.contains(".")
				? binaryName.substring(binaryName.lastIndexOf('.')).replace('.', '$')
				: binaryName;

		final ClassNode classNode = parentNode.createChildClass(sourceFile, className, binaryName);
		dependencies.createDependencyToNode(parentNode, classNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(classNode);

		internalParseClassTypeBinding(classNode, typeBinding, anonymousClassDeclaration.bodyDeclarations());
	}

	private void parseAnnotationTypeDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeDeclaration annotationTypeDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaAnnotationContainer : "Expected a JavaAnnotationContainer parent.";

		final ITypeBinding annotationBinding = annotationTypeDeclaration.resolveBinding();
		if (annotationBinding == null) throw new JavaCiaException("Cannot resolve binding on annotation declaration!");

		final AnnotationNode annotationNode = parentNode
				.createChildAnnotation(sourceFile, annotationBinding.getName(), annotationBinding.getBinaryName());
		dependencies.createDependencyToNode(parentNode, annotationNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(annotationNode);

		// put binding map
		bindingNodeMap.put(annotationBinding, annotationNode);

		// set annotate
		annotationNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(annotationBinding.getAnnotations(),
				annotationNode, JavaDependency.USE));

		// set modifier
		annotationNode.setModifiers(processModifiersFromBindingModifiers(annotationBinding.getModifiers()));

		// process children
		for (final Object object : annotationTypeDeclaration.bodyDeclarations()) {
			if (object instanceof BodyDeclaration) {
				parseBodyDeclaration(annotationNode, (BodyDeclaration) object);
			}
		}
	}

	private void parseAnnotationTypeMemberDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull AnnotationTypeMemberDeclaration annotationMemberDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaMethodContainer : "Expected a JavaMethodContainer parent.";

		// check null and resolve origin
		final IMethodBinding annotationMemberBinding = annotationMemberDeclaration.resolveBinding();
		if (annotationMemberBinding == null)
			throw new JavaCiaException("Cannot resolve binding on annotation type member declaration!");

		// create node and containment dependency
		final MethodNode methodNode
				= parentNode.createChildMethod(sourceFile, annotationMemberBinding.getName(), List.of());
		dependencies.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(methodNode);

		// put binding map
		bindingNodeMap.put(annotationMemberBinding, methodNode);

		// set annotate
		methodNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(annotationMemberBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(annotationMemberBinding.getModifiers()));

		// set type
		final ITypeBinding typeBinding = annotationMemberBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(types.createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// put delayed default value
		final Expression annotationMemberDeclarationDefault = annotationMemberDeclaration.getDefault();
		if (annotationMemberDeclarationDefault != null) {
			walkDeclaration(annotationMemberDeclarationDefault, methodNode, methodNode);
		}
	}

	private void parseFieldDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull FieldDeclaration fieldDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaFieldContainer : "Expected a JavaFieldContainer parent.";

		for (Object fragment : fieldDeclaration.fragments()) {
			if (fragment instanceof VariableDeclaration) {
				final VariableDeclaration variableDeclaration = (VariableDeclaration) fragment;

				final IVariableBinding variableBinding = variableDeclaration.resolveBinding();
				if (variableBinding == null) {
					throw new JavaCiaException("Cannot resolve binding on variable declaration!");
				}

				final FieldNode fieldNode = parentNode.createChildField(sourceFile, variableBinding.getName());
				dependencies.createDependencyToNode(parentNode, fieldNode, JavaDependency.MEMBER);

				// TODO: add to node set
//				assert sourceFile != null;
//				sourceFile.add(fieldNode);

				internalEnumConstantOrFieldVariableBinding(fieldNode, variableBinding);

				// put delayed variable initializer
				final Expression variableInitializer = variableDeclaration.getInitializer();
				if (variableInitializer != null) {
					final Pair<InitializerNode, List<InitializerNode.InitializerImpl>> pair
							= internalCreateOrGetInitializerNode(parentNode, fieldNode.isStatic());
					final InitializerNode initializerNode = pair.getA();

					// TODO: add to node set
//					assert sourceFile != null;
//					sourceFile.add(initializerNode);

					final List<InitializerNode.InitializerImpl> initializerList = pair.getB();
					initializerList.add(new InitializerNode.FieldInitializerImpl(fieldNode,
							format(variableInitializer.toString(), CodeFormatter.K_EXPRESSION)));
					walkDeclaration(variableInitializer, fieldNode, initializerNode);
				}
			}
		}
	}

	private void parseInitializer(@Nonnull AbstractNode parentNode, @Nonnull Initializer initializer)
			throws JavaCiaException {
		assert parentNode instanceof JavaInitializerContainer : "Expected a JavaInitializerContainer parent.";

		// create node and containment dependency
		final Pair<InitializerNode, List<InitializerNode.InitializerImpl>> pair = internalCreateOrGetInitializerNode(
				parentNode, (initializer.getModifiers() & Modifier.STATIC) != 0);
		final InitializerNode initializerNode = pair.getA();
		dependencies.createDependencyToNode(parentNode, initializerNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(initializerNode);

		// put delayed initializer body
		final Block initializerBody = initializer.getBody();
		if (initializerBody != null) {
			final List<InitializerNode.InitializerImpl> initializerList = pair.getB();
			initializerList.add(new InitializerNode.BlockInitializerImpl(
					format(initializerBody.toString(), CodeFormatter.K_STATEMENTS)));
			walkDeclaration(initializerBody, initializerNode, initializerNode);
		}
	}

	@Nonnull
	private Pair<InitializerNode, List<InitializerNode.InitializerImpl>> internalCreateOrGetInitializerNode(
			@Nonnull AbstractNode parentNode, boolean isStatic) {
		final Pair<Pair<InitializerNode, List<InitializerNode.InitializerImpl>>,
				Pair<InitializerNode, List<InitializerNode.InitializerImpl>>> pair
				= classInitializerMap.computeIfAbsent(parentNode, JavaSnapshotParser::createMutablePair);
		if (isStatic) {
			if (pair.getA() != null) return pair.getA();
			final InitializerNode initializer = parentNode.createChildInitializer(sourceFile, true);
			final List<InitializerNode.InitializerImpl> initializerList = new ArrayList<>();
			initializer.setInitializers(initializerList);
			return pair.setGetA(Pair.immutableOf(initializer, initializerList));
		} else {
			if (pair.getB() != null) return pair.getB();
			final InitializerNode initializer = parentNode.createChildInitializer(sourceFile, false);
			final List<InitializerNode.InitializerImpl> initializerList = new ArrayList<>();
			initializer.setInitializers(initializerList);
			return pair.setGetB(Pair.immutableOf(initializer, initializerList));
		}
	}

	private void parseMethodDeclaration(@Nonnull AbstractNode parentNode,
			@Nonnull MethodDeclaration methodDeclaration) throws JavaCiaException {
		assert parentNode instanceof JavaMethodContainer : "Expected a JavaMethodContainer parent.";

		// check null and resolve origin
		final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if (methodBinding == null) throw new JavaCiaException("Cannot resolve binding on method declaration!");

		// create parameter quick types
		final ITypeBinding[] parameterTypeBindings = methodBinding.getParameterTypes();
		final List<AbstractType> parameterJavaTypes = new ArrayList<>(parameterTypeBindings.length);
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			parameterJavaTypes.add(types.createUnprocessedTypeFromTypeBinding(parameterTypeBinding, JavaDependency.USE));
		}

		// create node and containment dependency
		final MethodNode methodNode
				= parentNode.createChildMethod(sourceFile, methodBinding.getName(), parameterJavaTypes);
		dependencies.createDependencyToNode(parentNode, methodNode, JavaDependency.MEMBER);

		// TODO: add to node set
//		assert sourceFile != null;
//		sourceFile.add(methodNode);

		// create parameter proper types
		for (final ITypeBinding parameterTypeBinding : parameterTypeBindings) {
			types.processUnprocessedType(parameterTypeBinding, methodNode);
		}

		// put binding map
		bindingNodeMap.put(methodBinding, methodNode);

		// set annotate
		methodNode.setAnnotates(annotates.createAnnotatesFromAnnotationBindings(methodBinding.getAnnotations(),
				methodNode, JavaDependency.USE));

		// set modifier
		methodNode.setModifiers(processModifiersFromBindingModifiers(methodBinding.getModifiers()));

		// set type parameter
		methodNode.setTypeParameters(types.createTypesFromTypeBindings(methodBinding.getTypeParameters(),
				methodNode, JavaDependency.USE));

		// set type
		final ITypeBinding typeBinding = methodBinding.getReturnType();
		if (typeBinding != null) {
			methodNode.setReturnType(types.createTypeFromTypeBinding(typeBinding, methodNode, JavaDependency.USE));
		}

		// set exceptions
		methodNode.setExceptions(types.createTypesFromTypeBindings(methodBinding.getExceptionTypes(),
				methodNode, JavaDependency.USE));

		// put delayed method body
		final Block methodDeclarationBody = methodDeclaration.getBody();
		if (methodDeclarationBody != null) {
			methodNode.setBodyBlock(
					format(methodDeclarationBody.toString(), CodeFormatter.K_STATEMENTS));
			walkDeclaration(methodDeclarationBody, methodNode, methodNode);
		}
	}

	//endregion Parser

	//region Walker

	private void walkDeclaration(@Nonnull ASTNode astNode, @Nonnull AbstractNode parentNode,
			@Nonnull AbstractNode dependencyNode) throws JavaCiaException {
		delayedDependencyWalkers.add(Pair.immutableOf(astNode, dependencyNode));

		final JavaCiaException[] exceptionProxy = new JavaCiaException[]{null};
		astNode.accept(new ASTVisitor() {

			@Override
			public boolean preVisit2(@Nonnull ASTNode node) {
				return exceptionProxy[0] == null;
			}

			@Override
			public boolean visit(@Nonnull TypeDeclaration node) {
				try {
					parseTypeDeclaration(parentNode, node);
				} catch (JavaCiaException exception) {
					exceptionProxy[0] = exception;
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull AnonymousClassDeclaration node) {
				try {
					parseAnonymousClassDeclaration(parentNode, node);
				} catch (JavaCiaException exception) {
					exceptionProxy[0] = exception;
				}
				return false;
			}

		});
		if (exceptionProxy[0] != null) throw exceptionProxy[0];
	}

	private void walkDependency(@Nonnull ASTNode astNode, @Nonnull AbstractNode javaNode) throws JavaCiaException {
		final JavaCiaException[] exceptionProxy = new JavaCiaException[]{null};
		astNode.accept(new ASTVisitor() {

			@Override
			public boolean preVisit2(@Nonnull ASTNode node) {
				return exceptionProxy[0] == null;
			}

			@Override
			public boolean visit(@Nonnull SuperConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull ConstructorInvocation node) {
				final IMethodBinding binding = node.resolveConstructorBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on constructor invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull SuperMethodInvocation node) {
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on super method invocation!");
				}
				return false;
			}

			@Override
			public boolean visit(@Nonnull MethodInvocation node) {
				Expression expression = node.getExpression();
				if (expression instanceof SimpleName) {
					IBinding expressionBinding = visitFromSimpleName((SimpleName) expression);
					if (expressionBinding instanceof IVariableBinding) {
						IVariableBinding iVariableBinding = (IVariableBinding) visitFromSimpleName((SimpleName) expression);
						if (iVariableBinding != null) {
							ITypeBinding iTypeBinding = iVariableBinding.getType();
							String binaryName = iTypeBinding.getBinaryName();
							if (binaryName.equals("SqlSession")) {
								createDependencyFromInvocation(node);
							}
						}
					} else if (expressionBinding == null) {
						if (((SimpleName) expression).getIdentifier().equals("Resources")) {
							createDependencyFromInvocation(node);
						}
					}
				}
				final IMethodBinding binding = node.resolveMethodBinding();
				if (binding != null) {
					createDependencyFromInvocation(binding, node.typeArguments(), node.arguments());
				} else if (!enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on method invocation!");
				}
				return false;
			}

			private IBinding visitFromSimpleName(SimpleName simpleName) {
				final IBinding binding = simpleName.resolveBinding();
				if (binding == null && !enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on simple name!");
				}
				final IBinding originalBinding = binding instanceof ITypeBinding
						? JavaDependencies.getOriginTypeBinding((ITypeBinding) binding)
						: binding instanceof IMethodBinding
						? JavaDependencies.getOriginMethodBinding((IMethodBinding) binding)
						: binding instanceof IVariableBinding
						? JavaDependencies.getOriginVariableBinding((IVariableBinding) binding)
						: null;

				if (originalBinding != null) {
					dependencies.createDelayDependency(javaNode, originalBinding, JavaDependency.USE);
				}
				return originalBinding;
			}

			private void createDependencyFromInvocation(@Nonnull MethodInvocation node) {
				List<?> listArgument = node.arguments();
				String argumentValue = null;
				for (Object argument : listArgument) {
					if (argument instanceof StringLiteral) {
						argumentValue = ((StringLiteral) argument).getLiteralValue();
					} else if (argument instanceof QualifiedName) {
						IVariableBinding iBinding = (IVariableBinding) ((QualifiedName) argument).getName().resolveBinding();
						IVariableBinding variableBinding = iBinding.getVariableDeclaration();
						argumentValue = (String) variableBinding.getConstantValue();
					}
				}
				if (argumentValue != null && mapXMlDependency.containsKey(argumentValue)) {
					for (int i = 0; i < mapXMlDependency.get(argumentValue).size(); i++) {
						dependencies.createDependencyToNode(javaNode, mapXMlDependency.get(argumentValue).get(i), JavaDependency.USE);
					}
				}
			}

			private void createDependencyFromInvocation(@Nonnull IMethodBinding binding,
					@Nonnull List<?> typeArguments, @Nonnull List<?> arguments) {
				dependencies.createDelayDependency(javaNode,
						JavaDependencies.getOriginMethodBinding(binding), JavaDependency.INVOCATION);
				for (final Object object : typeArguments) {
					if (object instanceof Type) ((Type) object).accept(this);
				}
				for (final Object object : arguments) {
					if (object instanceof Expression) ((Expression) object).accept(this);
				}
			}

			@Override
			public boolean visit(@Nonnull SimpleName node) {
				final IBinding binding = node.resolveBinding();
				if (binding == null && !enableRecovery) {
					exceptionProxy[0] = new JavaCiaException("Cannot resolve binding on simple name!");
					return false;
				}
				final IBinding originalBinding = binding instanceof ITypeBinding
						? JavaDependencies.getOriginTypeBinding((ITypeBinding) binding)
						: binding instanceof IMethodBinding
						? JavaDependencies.getOriginMethodBinding((IMethodBinding) binding)
						: binding instanceof IVariableBinding
						? JavaDependencies.getOriginVariableBinding((IVariableBinding) binding)
						: null;
				if (originalBinding != null) {
					dependencies.createDelayDependency(javaNode, originalBinding, JavaDependency.USE);
				}
				return true;
			}

			@Override
			public boolean visit(@Nonnull TypeDeclaration node) {
				return false;
			}

			@Override
			public boolean visit(@Nonnull AnonymousClassDeclaration node) {
				return false;
			}

		});
		if (exceptionProxy[0] != null) throw exceptionProxy[0];
	}

	//endregion Walker

}
