package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.output.ExportCSV;
import mrmathami.cia.java.jdt.output.gephi.Printer;
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

public class X {
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

	public static void main(String[] args) throws JavaCiaException, IOException, ParserConfigurationException, SAXException {
//		System.in.read();
//
//		for (int i = 0; i < 10; i++) {
//		final Path javaSourcePathA = Path.of("D:\\project\\MyBatis Collection\\mybatis-XML\\mybatis-example-1\\src");
//		final Path javaSourcePathB = Path.of("D:\\project\\MyBatis Collection\\mybatis-XML\\new\\mybatis-example-1\\src");
		final Path javaSourcePathA = Path.of("D:\\test-weight-1806\\test19\\CP_40");
		final Path javaSourcePathB = Path.of("D:\\test-weight-1806\\test19\\new\\CP_40");
		final List<Path> fileNamesA = getFileList(new ArrayList<>(), javaSourcePathA);
		final List<Path> fileNamesB = getFileList(new ArrayList<>(), javaSourcePathB);
		final List<Triple<String, Path, List<Path>>> javaSourcesA = List.of(
				Triple.immutableOf("", javaSourcePathA, fileNamesA)
		);
		final List<Triple<String, Path, List<Path>>> javaSourcesB = List.of(
				Triple.immutableOf("", javaSourcePathB, fileNamesB)
		);
		final Path configurationPath = Path.of("");

		final long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshotA = ProjectBuilders.createProjectSnapshot("JSON-java-before",
				javaSourcePathA, javaSourcesA, List.of(),
				DEPENDENCY_WEIGHT_TABLE, true, configurationPath);
		final long timeParseA = System.nanoTime();
		final JavaProjectSnapshot projectSnapshotB = ProjectBuilders.createProjectSnapshot("JSON-java-after",
				javaSourcePathB, javaSourcesB, List.of(),
				DEPENDENCY_WEIGHT_TABLE, true, configurationPath);
		final long timeParseB = System.nanoTime();

		final String jsonA = projectSnapshotA.getRootNode().toJson();
		final String jsonB = projectSnapshotB.getRootNode().toJson();

		Files.write(javaSourcePathA.resolve("output.txt"), jsonA.getBytes(StandardCharsets.UTF_8));
		Files.write(javaSourcePathB.resolve("output.txt"), jsonB.getBytes(StandardCharsets.UTF_8));

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

		/*try (final ObjectOutputStream objectOutputStream
				= new ObjectOutputStream(Files.newOutputStream(Path.of("jdt\\src\\test\\JSON-java.proj")))) {
			objectOutputStream.writeObject(javaProject);
		}*/

		System.out.printf("Compare time: %s\n", (timeCompareFinish - timeCompareStart) / 1000000.0);
		Printer printer = new Printer();
		Files.write(javaSourcePathB.resolve("gephi.gexf"), printer.writeGephiComparison(snapshotComparison).getBytes(StandardCharsets.UTF_8));

		ExportCSV exportCSV = new ExportCSV();
		exportCSV.exportImpactList(snapshotComparison, javaSourcePathB.resolve("weight.csv"));
		System.out.println("------------------------");
		System.out.println("Path gephi file: " + javaSourcePathB);
		System.out.println("Path weight csv: " + javaSourcePathB);
		System.out.println("-------------------------");

		System.out.println(snapshotComparison);
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
