package quynh.cia.java.cui;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.ProjectBuilders;
import mrmathami.cia.java.jdt.output.ExportCSV;
import mrmathami.cia.java.project.JavaProject;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
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

public class VersionComparison {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0,
			JavaDependency.ACCESS, 1.0
	));
	public static final JavaDependencyWeightTable DEPENDENCY_IMPACT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 0.4,
			JavaDependency.MEMBER, 0.2,
			JavaDependency.INHERITANCE, 0.3,
			JavaDependency.INVOCATION, 0.3,
			JavaDependency.OVERRIDE, 0.3,
			JavaDependency.ACCESS, 0.4
	));

	public static void compare(@Nullable Path configurationPathA, @Nonnull Map<String, Path> sourcesA, @Nullable List<Path> classPathsA,
			@Nullable Path configurationPathB, @Nonnull Map<String, Path> sourcesB, @Nullable List<Path> classPathsB,
			@Nonnull Path outputPath) throws IOException, ParserConfigurationException, SAXException, JavaCiaException {
		final List<Triple<String, Path, List<Path>>> javaSourcesA = new ArrayList<>();
		for (Map.Entry<String, Path> entry : sourcesA.entrySet()) {
			String nameModule = entry.getKey();
			Path path = entry.getValue();
			List<Path> files = getFileList(new ArrayList<>(), path);
			javaSourcesA.add(Triple.immutableOf(nameModule, path, files));
		}
		final List<Triple<String, Path, List<Path>>> javaSourcesB = new ArrayList<>();
		for (Map.Entry<String, Path> entry : sourcesB.entrySet()) {
			String nameModule = entry.getKey();
			Path path = entry.getValue();
			List<Path> files = getFileList(new ArrayList<>(), path);
			javaSourcesB.add(Triple.immutableOf(nameModule, path, files));
		}
		long timeStart = System.nanoTime();
		//version A
		assert classPathsA != null;
		final JavaProjectSnapshot projectSnapshotA = ProjectBuilders.createProjectSnapshot("before", javaSourcesA.get(0).getB(),
				javaSourcesA, classPathsA, DEPENDENCY_WEIGHT_TABLE, true, configurationPathA);
		long timeParseA = System.nanoTime();
		final String jsonA = projectSnapshotA.getRootNode().toJson();
		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);

		//version B
		assert classPathsB != null;
		final JavaProjectSnapshot projectSnapshotB = ProjectBuilders.createProjectSnapshot("before", javaSourcesB.get(0).getB(),
				javaSourcesB, classPathsB, DEPENDENCY_WEIGHT_TABLE, true, configurationPathB);
		long timeParseB = System.nanoTime();
		final String jsonB = projectSnapshotB.getRootNode().toJson();
		System.out.printf("Parse B time: %s\n", (timeParseB - timeStart) / 1000000.0);

		Files.write(outputPath.resolve("outputA.json"), jsonA.getBytes(StandardCharsets.UTF_8));
		Files.write(outputPath.resolve("outputB.json"), jsonB.getBytes(StandardCharsets.UTF_8));

		final long timeCompareStart = System.nanoTime();
		final JavaProjectSnapshotComparison snapshotComparison = ProjectBuilders.createProjectSnapshotComparison(
				"compare", projectSnapshotA, projectSnapshotB, DEPENDENCY_IMPACT_TABLE);
		final long timeCompareFinish = System.nanoTime();

		final JavaProject javaProject = ProjectBuilders.createProject("JSON-java");
		javaProject.addSnapshot(projectSnapshotA);
		javaProject.addSnapshot(projectSnapshotB);
		javaProject.addSnapshotComparison(snapshotComparison);
		System.out.printf("Compare time: %s\n", (timeCompareFinish - timeCompareStart) / 1000000.0);

		ExportCSV exportCSV = new ExportCSV();
		exportCSV.exportImpactList(snapshotComparison, outputPath.resolve("weight.csv"));

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
