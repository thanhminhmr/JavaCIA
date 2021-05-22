package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.project.JavaProject;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Triple;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class X {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0
	));
	public static final JavaDependencyWeightTable DEPENDENCY_IMPACT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 0.4,
			JavaDependency.MEMBER, 0.2,
			JavaDependency.INHERITANCE, 0.3,
			JavaDependency.INVOCATION, 0.3,
			JavaDependency.OVERRIDE, 0.3
	));

	public static void main(String[] args) throws JavaCiaException, IOException {
//		System.in.read();
//
//		for (int i = 0; i < 10; i++) {
		final Path javaRootPathA = Path.of("test/JSON-java-before");
		final Path javaSourcePathA = javaRootPathA.resolve(Path.of("src", "main", "java"));
		final Path javaRootPathB = Path.of("test/JSON-java");
		final Path javaSourcePathB = javaRootPathB.resolve(Path.of("src", "main", "java"));
		final List<Path> fileNamesA = getFileList(new ArrayList<>(), javaSourcePathA);
		final List<Path> fileNamesB = getFileList(new ArrayList<>(), javaSourcePathB);

		Collections.sort(fileNamesA);
		Collections.sort(fileNamesB);

		final long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshotA = ProjectBuilders.createProjectSnapshot("JSON-java-before",
				javaRootPathA, List.of(Triple.immutableOf("main", javaSourcePathA, fileNamesA)), List.of(),
				DEPENDENCY_WEIGHT_TABLE, false);
		final long timeParseA = System.nanoTime();
		final JavaProjectSnapshot projectSnapshotB = ProjectBuilders.createProjectSnapshot("JSON-java-after",
				javaRootPathB, List.of(Triple.immutableOf("main", javaSourcePathB, fileNamesB)), List.of(),
				DEPENDENCY_WEIGHT_TABLE, false);
		final long timeParseB = System.nanoTime();

		final String jsonA = projectSnapshotA.getRootNode().toJson();
		final String jsonB = projectSnapshotB.getRootNode().toJson();

		Files.write(Path.of("test/outputA.json"), jsonA.getBytes(StandardCharsets.UTF_8));
		Files.write(Path.of("test/outputB.json"), jsonB.getBytes(StandardCharsets.UTF_8));

		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);
		System.out.printf("Parse B time: %s\n", (timeParseB - timeParseA) / 1000000.0);

		final long timeCompareStart = System.nanoTime();
		final JavaProjectSnapshotComparison snapshotComparison = ProjectBuilders.createProjectSnapshotComparison(
				"compare", projectSnapshotA, projectSnapshotB, DEPENDENCY_IMPACT_TABLE);
		final long timeCompareFinish = System.nanoTime();

		final JavaProject javaProject = ProjectBuilders.createProject("JSON-java");
		javaProject.addSnapshot(projectSnapshotA);
		javaProject.addSnapshot(projectSnapshotB);
		javaProject.addSnapshotComparison(snapshotComparison);

		try (final ObjectOutputStream objectOutputStream
				= new ObjectOutputStream(Files.newOutputStream(Path.of("test/JSON-java.proj")))) {
			objectOutputStream.writeObject(javaProject);
		}

		System.out.printf("Compare time: %s\n", (timeCompareFinish - timeCompareStart) / 1000000.0);

		System.out.println(snapshotComparison);
//		}
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
