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
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.dependency.DependencyCountTable;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.node.PackageNode;
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.utils.Triple;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class JavaParser {

	@Nonnull private static final String[] EMPTY = new String[0];

	@Nonnull private final RootNode rootNode;
	@Nonnull private final Map<IBinding, AbstractNode> bindingNodeMap;

	@Nonnull private final Map<String, PackageNode> packageNodeMap = new HashMap<>();
	@Nonnull private final Map<AbstractNode, Map<AbstractNode, int[]>> nodeDependencies = new LinkedHashMap<>();;


	private JavaParser(@Nonnull RootNode rootNode, @Nonnull Map<IBinding, AbstractNode> bindingNodeMap) {
		this.rootNode = rootNode;
		this.bindingNodeMap = bindingNodeMap;
	}


	@Nonnull
	static JavaRootNode parse(@Nonnull String[] sourcePathArray, @Nonnull String[] sourceEncodingArray,
			@Nonnull String[] classPathArray, @Nonnull Map<String, SourceFile> sourceNameMap, boolean recoveryEnabled)
			throws JavaCiaException {

		final ASTParser astParser = ASTParser.newParser(AST.JLS15);
		final Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_15, options);
		astParser.setCompilerOptions(options);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(recoveryEnabled);
		astParser.setEnvironment(classPathArray, null, null, true);

		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "65536");

		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);

		final RootNode rootNode = new RootNode();
		final Map<IBinding, AbstractNode> bindingNodeMap = new HashMap<>();

		final JavaParser parser = new JavaParser(rootNode, bindingNodeMap);

		final JavaNodeBuilder nodes = new JavaNodeBuilder(sourceNameMap, parser, rootNode, bindingNodeMap,
				codeFormatter, recoveryEnabled);

		astParser.createASTs(sourcePathArray, sourceEncodingArray, EMPTY, nodes, null);

		nodes.postprocessing();

		return parser.postProcessing();
	}

	@Nonnull
	private RootNode postProcessing() {
		// set main dependency
		{
			final Map<DependencyCountTable, DependencyCountTable> nodeDependencyMap = new HashMap<>();
			for (final Map.Entry<AbstractNode, Map<AbstractNode, int[]>> sourceEntry : nodeDependencies.entrySet()) {
				final AbstractNode sourceNode = sourceEntry.getKey();
				for (final Map.Entry<AbstractNode, int[]> destinationEntry : sourceEntry.getValue().entrySet()) {
					final AbstractNode destinationNode = destinationEntry.getKey();
					final int[] dependencyCounts = destinationEntry.getValue();
					final DependencyCountTable nodeDependency = nodeDependencyMap
							.computeIfAbsent(new DependencyCountTable(dependencyCounts), Objects::requireNonNull);
					sourceNode.createDependencyTo(destinationNode, nodeDependency);
				}
			}
			nodeDependencyMap.clear();
			nodeDependencies.clear();
		}

		// clean up
		bindingNodeMap.clear();
		packageNodeMap.clear();

		// freeze root
		rootNode.freeze();
		return rootNode;
	}

	//region Package

	@Nonnull
	public PackageNode createFirstLevelPackageFromName(@Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(simpleName, qualifiedName -> {
			final PackageNode packageNode = rootNode.addChild(new PackageNode(rootNode, simpleName));
			createDependencyToNode(rootNode, packageNode, JavaDependency.MEMBER);
			return packageNode;
		});
	}

	@Nonnull
	public PackageNode createPackageFromParentAndName(@Nonnull PackageNode parentNode,
			@Nonnull String simpleName) {
		return packageNodeMap.computeIfAbsent(parentNode.getQualifiedName() + '.' + simpleName,
				qualifiedName -> {
					final PackageNode packageNode = parentNode.addChild(new PackageNode(parentNode, simpleName));
					createDependencyToNode(parentNode, packageNode, JavaDependency.MEMBER);
					return packageNode;
				});
	}

	@Nonnull
	public PackageNode createPackageFromNameComponents(@Nonnull String[] nameComponents) {
		assert nameComponents.length > 0 : "nameComponents length should not be 0.";
		PackageNode packageNode = createFirstLevelPackageFromName(nameComponents[0]);
		for (int i = 1; i < nameComponents.length; i++) {
			packageNode = createPackageFromParentAndName(packageNode, nameComponents[i]);
		}
		return packageNode;
	}

	//endregion Package

	//region Main Dependency

	@Nonnull
	private int[] internalGetOrCreateDependencyCounts(@Nonnull AbstractNode sourceNode,
			@Nonnull AbstractNode targetNode) {
		return nodeDependencies
				.computeIfAbsent(sourceNode, JavaParser::createLinkedHashMap)
				.computeIfAbsent(targetNode, JavaParser::createDependencyCounts);
	}

	void createDependenciesToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull int[] dependencyCounts) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		final int length = currentCounts.length;
		for (int i = 0; i < length; i++) currentCounts[i] += dependencyCounts[i];
	}

	void createDependencyToNode(@Nonnull AbstractNode dependencySourceNode,
			@Nonnull AbstractNode dependencyTargetNode, @Nonnull JavaDependency dependencyType) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		currentCounts[dependencyType.ordinal()] += 1;
	}

	//endregion Main Dependency

	//region Misc

	@Nonnull
	static <R> int[] createDependencyCounts(@Nullable R any) {
		return new int[JavaDependency.VALUE_LIST.size()];
	}

	@Nonnull
	static <A, B, C, R> Triple<A, B, C> createMutableTriple(@Nullable R any) {
		return Triple.mutableOf(null, null, null);
	}

	@Nonnull
	static <A, R> List<A> createArrayList(@Nullable R any) {
		return new ArrayList<>();
	}

	@Nonnull
	static <A, B, R> Map<A, B> createLinkedHashMap(@Nullable R any) {
		return new LinkedHashMap<>();
	}

	//endregion Misc

}
