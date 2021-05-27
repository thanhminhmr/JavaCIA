package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;
import mrmathami.utils.Pair;
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

public class Z {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0
	));
	private static final Path configurationPath = Path.of("");

	public static void main(String[] args) throws JavaCiaException, IOException, ParserConfigurationException, SAXException {
//		System.in.read();

//		CodeFormatter

		final Path inputPath = Path.of("D:\\Research\\SourceCodeComparator\\javacia\\test\\test_recovery");
		final List<Path> inputFiles = getFileList(new ArrayList<>(), inputPath);
		final Map<String, Pair<Path, List<Path>>> javaSources = Map.of(
				"input", Pair.immutableOf(inputPath, inputFiles)
		);

		final List<Path> classPaths = List.of(
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\org\\eclipse\\jdt\\org.eclipse.jdt.core\\3.22.0\\org.eclipse.jdt.core-3.22.0.jar"),
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\org\\eclipse\\platform\\org.eclipse.text\\3.10.200\\org.eclipse.text-3.10.200.jar"),
//				Path.of("C:\\Users\\Meo\\.m2\\repository\\mrmathami\\utils\\1.0.0\\utils-1.0.0.jar")
		);

		long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshot = ProjectBuilders.createProjectSnapshot("before",
				javaSources, classPaths, DEPENDENCY_WEIGHT_TABLE, true, configurationPath);
		long timeParseA = System.nanoTime();

		final String jsonA = projectSnapshot.getRootNode().toJson();

		Files.write(inputPath.resolve("output.txt"), jsonA.getBytes(StandardCharsets.UTF_8));

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
