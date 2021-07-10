package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
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

public class Y {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0,
			JavaDependency.ACCESS, 1.0
	));
	//final static Path configurationPath = Path.of("D:\\project\\MyBatis Collection\\mybatis-spring-example\\mybatis-spring-example\\resources\\SqlMapConfig.xml");
	final static Path configurationPath = Path.of("");
	public static void main(String[] args) throws JavaCiaException, IOException, ParserConfigurationException, SAXException {
//		System.in.read();

//		CodeFormatter

		/*final Path corePath = Path.of("core", "src", "main", "java");
		final List<Path> coreFiles = getFileList(new ArrayList<>(), corePath);
		final Path jdtPath = Path.of("jdt", "src", "main", "java");
		final List<Path> jdtFiles = getFileList(new ArrayList<>(), jdtPath);
		final List<Triple<String, Path, List<Path>>> javaSources = List.of(
				Triple.immutableOf("core", corePath, coreFiles),
				Triple.immutableOf("jdt", jdtPath, jdtFiles)
		);*/

		final Path corePath = Path.of("D:\\project\\MyBatisCollection\\Github\\MyBatisExample1\\MyBatisExample\\src\\main\\java");
		final List<Path> coreFiles = getFileList(new ArrayList<>(), corePath);
		final List<Triple<String, Path, List<Path>>> javaSources = List.of(
				Triple.immutableOf("", corePath, coreFiles)
		);
		final List<Path> classPaths = List.of(
				/*Path.of("/home/meo/.m2/repository/org/eclipse/jdt/org.eclipse.jdt.core/3.25.0/org.eclipse.jdt.core-3.25.0.jar"),
				Path.of("/home/meo/.m2/repository/org/eclipse/platform/org.eclipse.text/3.11.0/org.eclipse.text-3.11.0.jar"),
				Path.of("/home/meo/.m2/repository/mrmathami/mrmathami.utils/1.0.4/mrmathami.utils-1.0.4.jar")*/
		);

		long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshot = ProjectBuilders.createProjectSnapshot("before",
				corePath, javaSources, classPaths, DEPENDENCY_WEIGHT_TABLE, true, configurationPath);
		long timeParseA = System.nanoTime();

		final String jsonA = projectSnapshot.getRootNode().toJson();

		Files.write(corePath.resolve("output.txt"), jsonA.getBytes(StandardCharsets.UTF_8));
		Printer printer = new Printer();
		Files.write(corePath.resolve("gephi.gexf"), printer.writeGephi(projectSnapshot).getBytes(StandardCharsets.UTF_8));


		ExportCSV exportCSV = new ExportCSV();
		exportCSV.exportComponentsList(projectSnapshot, corePath.resolve("list.csv"));

		System.out.println("------------------------");
		System.out.println("Path gephi file: " + corePath);
		System.out.println("Path list csv: " + corePath);
		System.out.println("-------------------------");

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
