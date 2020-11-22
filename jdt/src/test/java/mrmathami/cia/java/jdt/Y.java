package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Y {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0
	));

	public static void main(String[] args) throws JavaCiaException, IOException {
//		System.in.read();

//		CodeFormatter

		final Path corePath = Path.of("D:\\Research\\SourceCodeComparator\\javacia\\core\\src\\main\\java");
		final List<Path> coreFiles = getFileList(new ArrayList<>(), corePath);
		final Path jdtPath = Path.of("D:\\Research\\SourceCodeComparator\\javacia\\jdt\\src\\main\\java");
		final List<Path> jdtFiles = getFileList(new ArrayList<>(), jdtPath);
		final Map<String, Pair<Path, List<Path>>> javaSources = Map.of(
				"core", Pair.immutableOf(corePath, coreFiles),
				"jdt", Pair.immutableOf(jdtPath, jdtFiles)
		);

		final List<Path> classPaths = List.of(
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\org\\eclipse\\jdt\\org.eclipse.jdt.core\\3.22.0\\org.eclipse.jdt.core-3.22.0.jar"),
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\org\\eclipse\\platform\\org.eclipse.text\\3.10.200\\org.eclipse.text-3.10.200.jar"),
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\mrmathami\\utils\\1.0.0\\utils-1.0.0.jar")
		);

		long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshot = ProjectBuilders.createProjectSnapshot("before",
				javaSources, classPaths, DEPENDENCY_WEIGHT_TABLE, true);
		long timeParseA = System.nanoTime();

		final String jsonA = projectSnapshot.getRootNode().toJson();

		Files.write(corePath.resolve("output.txt"), jsonA.getBytes(StandardCharsets.UTF_8));

		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);
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
