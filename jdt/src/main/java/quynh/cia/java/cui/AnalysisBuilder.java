package quynh.cia.java.cui;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.ProjectBuilders;
import mrmathami.cia.java.jdt.output.ExportCSV;
import mrmathami.cia.java.jdt.output.gephi.Printer;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Triple;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalysisBuilder {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0,
			JavaDependency.ACCESS, 1.0
	));
	/*public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 1.0,
			JavaDependency.OVERRIDE, 4.0,
			JavaDependency.ACCESS, 1.0
	));*/

	public static void build(@Nullable Path configurationPath, @Nonnull Map<String, Path> sources, @Nullable List<Path> classPaths, @Nonnull Path outputPath) throws IOException, ParserConfigurationException, SAXException, JavaCiaException {
		final List<Triple<String, Path, List<Path>>> javaSources = new ArrayList<>();
		for (Map.Entry<String, Path> entry : sources.entrySet()) {
			String nameModule = entry.getKey();
			Path path = entry.getValue();
			List<Path> files = getFileList(new ArrayList<>(), path);
			javaSources.add(Triple.immutableOf(nameModule, path, files));
		}
		long timeStart = System.nanoTime();
		assert classPaths != null;
		final JavaProjectSnapshot projectSnapshot = ProjectBuilders.createProjectSnapshot("before", javaSources.get(0).getB(),
				javaSources, classPaths, DEPENDENCY_WEIGHT_TABLE, true, configurationPath);
		long timeParseA = System.nanoTime();
		final String jsonA = projectSnapshot.getRootNode().toJson();
		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);

		Files.write(outputPath.resolve("output.json"), jsonA.getBytes(StandardCharsets.UTF_8));

		ExportCSV exportCSV = new ExportCSV();
		exportCSV.exportComponentsList(projectSnapshot, outputPath.resolve("list.csv"));
		Printer printer = new Printer();
		Files.write(outputPath.resolve("gephi.gexf"), printer.writeGephi(projectSnapshot).getBytes(StandardCharsets.UTF_8));
		System.out.println("------------------------");
		System.out.println("Output path: " + outputPath);
		System.out.println("------------------------");

	}

	private static List<Path> getFileList(List<Path> fileList, Path dir) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path path : stream) {
				if (path.toFile().isDirectory()) {
					getFileList(fileList, path);
				} else {
					fileList.add(path);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileList;
	}
}
