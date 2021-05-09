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
import mrmathami.cia.java.jdt.project.Module;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.node.RootNode;
import mrmathami.cia.java.jdt.tree.node.XMLNode;
import mrmathami.cia.java.project.JavaSourceFileType;
import mrmathami.cia.java.tree.node.JavaRootNode;
import mrmathami.cia.java.utils.RelativePath;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

	@Nonnull private final Map<String, SourceFile> sourceFileMap;
	@Nonnull private final JavaNodes nodes;

	// TODO: this make the whole parser cannot run continuously if not clean correctly
	// TODO: memory leaking here
	// TODO: this make the code impossible to run in parallel
	private final Map<String, List<XMLNode>> mapXMlDependency = new HashMap<>();

	@Nullable private JavaCiaException exception;


	private JavaSnapshotParser(@Nonnull Map<String, SourceFile> sourceFileMap, @Nonnull CodeFormatter codeFormatter,
			boolean enableRecovery) {
		this.sourceFileMap = sourceFileMap;
		this.nodes = new JavaNodes(codeFormatter, enableRecovery);
	}


	@Nonnull
	static JavaRootNode build(@Nonnull Path projectRoot, @Nonnull List<Triple<String, Path, List<Path>>> javaSources,
			@Nonnull List<Path> classPaths, boolean enableRecovery, Path configurationPath) throws JavaCiaException, IOException, SAXException, ParserConfigurationException {

		final Path projectPath = toRealPathOrThrow(projectRoot);
		final Map<String, Module> modules = new HashMap<>();
		final List<String> classPathList = new ArrayList<>(classPaths.size() + javaSources.size());
		final List<String> projectFileList = new ArrayList<>();
		final Map<String, SourceFile> sourceFileMap = new HashMap<>();
		for (final Triple<String, Path, List<Path>> triple : javaSources) {
			final Path modulePath = toRealPathOrThrow(triple.getB());
			final Module module = modules.computeIfAbsent(triple.getA(),
					name -> new Module(name, RelativePath.fromPath(projectPath.relativize(modulePath))));
			classPathList.add(modulePath.toString());
			for (final Path path : triple.getC()) {
				final Path sourcePath = toRealPathOrThrow(path);
				final String sourcePathString = path.toString();
				projectFileList.add(sourcePathString);
				sourceFileMap.put(sourcePathString, new SourceFile(module, JavaSourceFileType.JAVA,
						RelativePath.fromPath(modulePath.relativize(sourcePath))));
			}
		}
		for (final Path classPath : classPaths) {
			classPathList.add(toRealPathOrThrow(classPath).toString());
		}
		final String[] sourcePathArray = projectFileList.toArray(EMPTY);

		final String[] sourceEncodingArray = new String[sourcePathArray.length];
		Arrays.fill(sourceEncodingArray, StandardCharsets.UTF_8.name());

		final String[] classPathArray = classPathList.toArray(EMPTY);

		return parse(sourcePathArray, sourceEncodingArray, classPathArray, sourceFileMap, enableRecovery, configurationPath);
	}

	@Nonnull
	private static Path toRealPathOrThrow(@Nonnull Path path) throws JavaCiaException {
		try {
			return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
		} catch (IOException exception) {
			throw new JavaCiaException("Cannot access file!", exception);
		}
	}

	@Nonnull
	private static JavaRootNode parse(@Nonnull String[] sourcePathArray, @Nonnull String[] sourceEncodingArray,
			@Nonnull String[] classPathArray, @Nonnull Map<String, SourceFile> sourceNameMap, boolean enableRecovery, Path configuration)
			throws JavaCiaException, ParserConfigurationException, SAXException, IOException {

		final ASTParser astParser = ASTParser.newParser(AST.JLS15);
		final Map<String, String> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_15, options);
		astParser.setCompilerOptions(options);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(enableRecovery);
		astParser.setEnvironment(classPathArray, null, null, true);

		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "65536");

		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options, ToolFactory.M_FORMAT_EXISTING);
		final JavaSnapshotParser parser = new JavaSnapshotParser(sourceNameMap, codeFormatter, enableRecovery);

		if (configuration.toString().equals("")) {
			List<String> listMapperPaths = parser.filterMapFiles(sourcePathArray);
			parser.acceptXMLMapper(listMapperPaths, parser.mapXMlDependency);
		} else {
			parser.acceptXMlConfig(configuration, sourcePathArray, parser.mapXMlDependency);
			parser.acceptXMlMapper(parser.mapXMlDependency);
		}

		astParser.createASTs(sourcePathArray, sourceEncodingArray, EMPTY, parser, null);

		// TODO: add source name info to tree
		return parser.postProcessing();
	}

	public List<String> filterMapFiles(@Nonnull String[] sourcePathArray) throws IOException, SAXException, ParserConfigurationException {
		List<String> list = new ArrayList<>();
		for (String sourcePath : sourcePathArray) {
			if (sourcePath.endsWith(".xml")) {
				Document document = parseXML(Path.of(sourcePath));
				String type = document.getDocumentElement().getNodeName();
				if (type.equals("mapper")) {
					list.add(sourcePath);
				}
			}
		}
		return list;
	}

	public void acceptXMlConfig(@Nonnull Path sourcePath, @Nonnull String[] sourcePathArray, Map<String, List<XMLNode>> mapXMlDependency) {
		if (exception != null) return;
		try {
			Document doc = parseXML(sourcePath);
			nodes.build(doc, sourcePathArray, mapXMlDependency, sourcePath);
		} catch (ParserConfigurationException | IOException | SAXException e) {
			e.printStackTrace();
		}
	}

	public void acceptXMLMapper(List<String> listMapperPaths, Map<String, List<XMLNode>> mapXMlDependency) {
		for (String path : listMapperPaths) {
			if (exception != null) return;
			try {
				final SourceFile sourceFile = sourceFileMap.get(path);
				if (sourceFile == null) throw new JavaCiaException("Unknown source path!");
				Document doc = parseXML(Path.of(path));
				nodes.build(sourceFile, doc, path, mapXMlDependency);
			} catch (JavaCiaException javaCiaException) {
				exception = javaCiaException;
			} catch (ParserConfigurationException | IOException | SAXException e) {
				e.printStackTrace();
			}
		}
	}

	public void acceptXMlMapper(Map<String, List<XMLNode>> mapXMlDependency) {
		List<String> listMapperPaths = new ArrayList<>();
		for (String path : mapXMlDependency.keySet()) {
			if (path.endsWith(".xml") && path.contains("\\")) {
				System.out.println("mapper file path: " + path);
				listMapperPaths.add(path);
			}
		}
		acceptXMLMapper(listMapperPaths, mapXMlDependency);
	}


	@Override
	public void acceptAST(@Nonnull String sourcePath, @Nonnull CompilationUnit compilationUnit) {
		if (exception != null) return;
		try {
			final SourceFile sourceFile = sourceFileMap.get(sourcePath);
			if (sourceFile == null) throw new JavaCiaException("Unknown source path!");
			nodes.build(sourceFile, compilationUnit, mapXMlDependency);
		} catch (JavaCiaException exception) {
			this.exception = exception;
		}
	}

	private static Document parseXML(Path sourcePath) throws ParserConfigurationException, IOException, SAXException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);
		dbf.setCoalescing(true);
		dbf.setIgnoringElementContentWhitespace(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(sourcePath.toFile());
		doc.normalizeDocument();
		return doc;
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
