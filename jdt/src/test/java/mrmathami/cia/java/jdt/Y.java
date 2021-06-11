package mrmathami.cia.java.jdt;

import mrmathami.cia.java.JavaCiaException;
import mrmathami.cia.java.jdt.project.builder.parameter.BuildInputSources;
import mrmathami.cia.java.jdt.project.builder.parameter.JavaBuildParameter;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.dependency.JavaDependency;
import mrmathami.cia.java.tree.dependency.JavaDependencyWeightTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		final Path corePath = Path.of("core", "src", "main", "java");
		final Path jdtPath = Path.of("jdt", "src", "main", "java");
		final BuildInputSources inputSources = new BuildInputSources(Path.of("."));
		Utils.getFileList(inputSources.createModule("core", corePath), corePath);
		Utils.getFileList(inputSources.createModule("jdt", jdtPath), jdtPath);

		final List<Path> classPaths = List.of(
				Path.of("/home/meo/.m2/repository/org/eclipse/jdt/org.eclipse.jdt.core/3.25.0/org.eclipse.jdt.core-3.25.0.jar"),
				Path.of("/home/meo/.m2/repository/org/eclipse/platform/org.eclipse.text/3.11.0/org.eclipse.text-3.11.0.jar"),
				Path.of("/home/meo/.m2/repository/mrmathami/mrmathami.utils/1.0.6/mrmathami.utils-1.0.6.jar")
		);

		long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshot = ProjectBuilder.createProjectSnapshot("before",
				DEPENDENCY_WEIGHT_TABLE, inputSources, Set.of(new JavaBuildParameter(classPaths, true)));
		long timeParseA = System.nanoTime();

		final String jsonA = projectSnapshot.getRootNode().toJson();

		Files.write(Path.of("output.json"), jsonA.getBytes(StandardCharsets.UTF_8));

		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);
	}
}
