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

package mrmathami.cia.java.jdt.project.builder;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.tree.node.AbstractNode;
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.utils.Pair;
import mrmathami.utils.Triple;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class JavaSnapshotParser extends FileASTRequestor {

	@Nonnull private static final String[] EMPTY = new String[0];

	@Nonnull private final Map<String, String> sourceNameMap;
	@Nonnull private final JavaNodes nodes;

	@Nonnull private final Map<String, Set<AbstractNode>> sourceNodeMap = new HashMap<>();

	@Nullable private JavaCiaException exception;


	private JavaSnapshotParser(@Nonnull Map<String, String> sourceNameMap, @Nonnull CodeFormatter codeFormatter,
			boolean enableRecovery) {
		this.sourceNameMap = sourceNameMap;
		this.nodes = new JavaNodes(codeFormatter, enableRecovery);
	}


	@Nonnull
	static JavaRootNode build(@Nonnull List<Triple<String, Path, List<Path>>> javaSources,
			@Nonnull List<Path> classPaths, boolean enableRecovery) throws JavaCiaException {

		final List<String> classPathList = new ArrayList<>(classPaths.size() + javaSources.size());
		final List<String> projectFileList = new ArrayList<>();
		final Map<String, String> sourceNameMap = new HashMap<>();
		try {
			for (final Triple<String, Path, List<Path>> triple : javaSources) {
				final String sourceName = triple.getA();
				classPathList.add(triple.getB().toRealPath(LinkOption.NOFOLLOW_LINKS).toString());
				for (final Path projectFilePath : triple.getC()) {
					final String projectFileString = projectFilePath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
					projectFileList.add(projectFileString);
					sourceNameMap.put(projectFileString, sourceName);
				}
			}
			for (final Path classPath : classPaths) {
				classPathList.add(classPath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString());
			}
		} catch (IOException exception) {
			throw new JavaCiaException("Cannot access source files or class paths!", exception);
		}
		final String[] sourcePathArray = projectFileList.toArray(EMPTY);

		final String[] sourceEncodingArray = new String[sourcePathArray.length];
		Arrays.fill(sourceEncodingArray, StandardCharsets.UTF_8.name());

		final String[] classPathArray = classPathList.toArray(EMPTY);

		return parse(sourcePathArray, sourceEncodingArray, classPathArray, sourceNameMap, enableRecovery);
	}

	@Nonnull
	private static JavaRootNode parse(@Nonnull String[] sourcePathArray, @Nonnull String[] sourceEncodingArray,
			@Nonnull String[] classPathArray, @Nonnull Map<String, String> sourceNameMap, boolean enableRecovery)
			throws JavaCiaException {

		final ASTParser astParser = ASTParser.newParser(AST.JLS14);
		final Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_14, options);
		astParser.setCompilerOptions(options);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(enableRecovery);
		astParser.setEnvironment(classPathArray, null, null, true);

		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "65536");

		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);
		final JavaSnapshotParser parser = new JavaSnapshotParser(sourceNameMap, codeFormatter, enableRecovery);
		astParser.createASTs(sourcePathArray, sourceEncodingArray, EMPTY, parser, null);

		// TODO: add source name info to tree
		return parser.postProcessing();
	}

	@Override
	public void acceptAST(@Nonnull String sourcePath, @Nonnull CompilationUnit compilationUnit) {
		if (exception != null) return;
		try {
			final String sourceName = sourceNameMap.get(sourcePath);
			if (sourceName == null) throw new JavaCiaException("Unknown source path!");
			final Set<AbstractNode> perFileNodeSet
					= sourceNodeMap.computeIfAbsent(sourceName, JavaSnapshotParser::createLinkedHashSet);
			nodes.build(perFileNodeSet, compilationUnit);
		} catch (JavaCiaException exception) {
			this.exception = exception;
		}
	}

	@Nonnull
	private RootNode postProcessing() throws JavaCiaException {
		if (exception != null) throw exception;
		return nodes.postprocessing();
	}

	//region Misc

	@Nonnull
	static <A, B, R> Pair<A, B> createMutablePair(@Nullable R any) {
		return Pair.mutableOf(null, null);
	}

	@Nonnull
	static <A, R> List<A> createArrayList(@Nullable R any) {
		return new ArrayList<>();
	}

	@Nonnull
	static <A, B, R> Map<A, B> createHashMap(@Nullable R any) {
		return new HashMap<>();
	}

	@Nonnull
	static <A, B, R> Map<A, B> createIdentityHashMap(@Nullable R any) {
		return new IdentityHashMap<>();
	}

	@Nonnull
	static <A, R> Set<A> createLinkedHashSet(@Nullable R any) {
		return new LinkedHashSet<>();
	}

	//endregion Misc

}
