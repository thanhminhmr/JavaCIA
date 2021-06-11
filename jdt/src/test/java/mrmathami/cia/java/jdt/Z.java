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

public class Z {
	public static final JavaDependencyWeightTable DEPENDENCY_WEIGHT_TABLE = JavaDependencyWeightTable.of(Map.of(
			JavaDependency.USE, 1.0,
			JavaDependency.MEMBER, 1.0,
			JavaDependency.INHERITANCE, 4.0,
			JavaDependency.INVOCATION, 4.0,
			JavaDependency.OVERRIDE, 1.0
	));

	public static void main(String[] args) throws JavaCiaException, IOException {
//		System.in.read();

		final Path inputPath = Path.of("test", "test_recovery");
		final BuildInputSources inputSources = new BuildInputSources(inputPath);
		Utils.getFileList(inputSources.createModule("core", inputPath), inputPath);

		long timeStart = System.nanoTime();
		final JavaProjectSnapshot projectSnapshot = ProjectBuilder.createProjectSnapshot("before",
				DEPENDENCY_WEIGHT_TABLE, inputSources, Set.of(new JavaBuildParameter(List.of(), true)));
		long timeParseA = System.nanoTime();

		final String jsonA = projectSnapshot.getRootNode().toJson();

		Files.write(inputPath.resolve("output.json"), jsonA.getBytes(StandardCharsets.UTF_8));

		System.out.printf("Parse A time: %s\n", (timeParseA - timeStart) / 1000000.0);
	}
}
