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

package io.github.thanhminhmr.javacia.jdt.project.builder;

import io.github.thanhminhmr.javacia.JavaCiaException;
import io.github.thanhminhmr.javacia.jdt.project.SourceFile;
import io.github.thanhminhmr.javacia.jdt.tree.dependency.DependencyCountTable;
import io.github.thanhminhmr.javacia.jdt.tree.node.AbstractNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.PackageNode;
import io.github.thanhminhmr.javacia.jdt.tree.node.RootNode;
import io.github.thanhminhmr.javacia.tree.dependency.JavaDependency;
import io.github.thanhminhmr.javacia.tree.node.JavaRootNode;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class JavaParser {

	private static final @NotNull String @NotNull [] EMPTY = new String[0];

	private final @NotNull RootNode rootNode;
	private final @NotNull Map<@NotNull IBinding, @NotNull AbstractNode> bindingNodeMap;

	private final @NotNull Map<@NotNull String, @NotNull PackageNode> packageNodeMap = new HashMap<>();
	private final @NotNull Map<@NotNull AbstractNode, @NotNull Map<@NotNull AbstractNode, int @NotNull []>> nodeDependencies = new LinkedHashMap<>();


	private JavaParser(@NotNull RootNode rootNode, @NotNull Map<@NotNull IBinding, @NotNull AbstractNode> bindingNodeMap) {
		this.rootNode = rootNode;
		this.bindingNodeMap = bindingNodeMap;
	}


	static @NotNull JavaRootNode parse(@NotNull String @NotNull [] sourcePathArray,
			@NotNull String @NotNull [] sourceEncodingArray,
			@NotNull String @NotNull [] classPathArray,
			@NotNull Map<@NotNull String, @NotNull SourceFile> sourceNameMap, boolean recoveryEnabled)
			throws JavaCiaException {

		final ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
		final Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.latestSupportedJavaVersion(), options);
		options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
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

	private @NotNull RootNode postProcessing() {
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

	public @NotNull PackageNode createFirstLevelPackageFromName(@NotNull String simpleName) {
		return packageNodeMap.computeIfAbsent(simpleName, qualifiedName -> {
			final PackageNode packageNode = rootNode.addChild(new PackageNode(rootNode, simpleName));
			createDependencyToNode(rootNode, packageNode, JavaDependency.MEMBER);
			return packageNode;
		});
	}

	public @NotNull PackageNode createPackageFromParentAndName(@NotNull PackageNode parentNode,
			@NotNull String simpleName) {
		return packageNodeMap.computeIfAbsent(parentNode.getQualifiedName() + '.' + simpleName,
				qualifiedName -> {
					final PackageNode packageNode = parentNode.addChild(new PackageNode(parentNode, simpleName));
					createDependencyToNode(parentNode, packageNode, JavaDependency.MEMBER);
					return packageNode;
				});
	}

	public @NotNull PackageNode createPackageFromNameComponents(@NotNull String[] nameComponents) {
		assert nameComponents.length > 0 : "nameComponents length should not be 0.";
		PackageNode packageNode = createFirstLevelPackageFromName(nameComponents[0]);
		for (int i = 1; i < nameComponents.length; i++) {
			packageNode = createPackageFromParentAndName(packageNode, nameComponents[i]);
		}
		return packageNode;
	}

	//endregion Package

	//region Main Dependency

	private int @NotNull [] internalGetOrCreateDependencyCounts(@NotNull AbstractNode sourceNode,
			@NotNull AbstractNode targetNode) {
		return nodeDependencies
				.computeIfAbsent(sourceNode, JavaParser::createLinkedHashMap)
				.computeIfAbsent(targetNode, JavaParser::createDependencyCounts);
	}

	void createDependenciesToNode(@NotNull AbstractNode dependencySourceNode,
			@NotNull AbstractNode dependencyTargetNode, int @NotNull [] dependencyCounts) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		final int length = currentCounts.length;
		for (int i = 0; i < length; i++) currentCounts[i] += dependencyCounts[i];
	}

	void createDependencyToNode(@NotNull AbstractNode dependencySourceNode,
			@NotNull AbstractNode dependencyTargetNode, @NotNull JavaDependency dependencyType) {
		final int[] currentCounts = internalGetOrCreateDependencyCounts(dependencySourceNode, dependencyTargetNode);
		currentCounts[dependencyType.ordinal()] += 1;
	}

	//endregion Main Dependency

	//region Misc

	static <R> int @NotNull [] createDependencyCounts(@Nullable R any) {
		return new int[JavaDependency.VALUE_LIST.size()];
	}

	@NotNull
	static <A, R> List<A> createArrayList(@Nullable R any) {
		return new ArrayList<>();
	}

	@NotNull
	static <A, B, R> Map<A, B> createLinkedHashMap(@Nullable R any) {
		return new LinkedHashMap<>();
	}

	//endregion Misc

}
