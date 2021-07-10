package quynh.cia.java.cui;

import mrmathami.annotations.Nonnull;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
		name = "cia-core-cli",
		description = "Change Impact Analysis Tool",
		version = "1.0-SNAPSHOT",
		mixinStandardHelpOptions = true,
		exitCodeOnExecutionException = -1,
		exitCodeOnInvalidInput = 1,
		sortOptions = false
)
public final class Main {
	private Main() {

	}

	public static void main(@Nonnull String[] args) {
		System.exit(new CommandLine(Main.class)
				.addSubcommand(AnalysisCommand.class)
				.addSubcommand(ComparisonCommand.class)
				.execute(args));
	}

	/*
	for analyzing a version of source code.
	 */
	@Command(
			name = "analysis",
			description = "Analysis tool",
			exitCodeOnExecutionException = -1,
			exitCodeOnInvalidInput = 1,
			sortOptions = false
	)
	private static final class AnalysisCommand implements Callable<Void> {
		@Option(
				names = {"-p", "--configuration-path"},
				paramLabel = "CONFIGURATION-PATH",
				description = "Path of configuration file."
		)
		private Path configurationPath;

		@Option(
				names = {"-s", "--source-path"},
				paramLabel = "NAME=SOURCE-PATH",
				required = true,
				split = ",",
				description = "Path of source files."

		)
		private Map<String, Path> sources;

		@Option(
				names = {"-c", "--class-path"},
				paramLabel = "PATHS",
				description = "The class path(s) of the input executable file (same as JVM -cp switch)."
		)
		private List<Path> classPaths;

		@Option(
				names = {"-o", "--output"},
				paramLabel = "OUTPUT-FILE",
				required = true,
				description = "The result file path (list file and json file)."
		)
		private Path outputFile;

		@Override
		public Void call() throws Exception {
			AnalysisBuilder.build(configurationPath != null ? configurationPath : Path.of(""), sources, classPaths != null ? classPaths : List.of(), outputFile);
			return null;
		}
	}

	/*
	for compare versions
	 */
	@Command(
			name = "comparison",
			description = "Comparison tool",
			exitCodeOnExecutionException = -1,
			exitCodeOnInvalidInput = 1,
			sortOptions = false
	)
	public static final class ComparisonCommand implements Callable<Void> {

		@Option(
				names = {"-pa", "--configuration-pathA"},
				paramLabel = "CONFIGURATION-PATH",
				description = "Path of configuration file."
		)
		private Path configurationPathA;

		@Option(
				names = {"-sa", "--source-pathA"},
				paramLabel = "NAME=SOURCE-PATH",
				required = true,
				split = ",",
				description = "Path of source files."

		)
		private Map<String, Path> sourcesA;

		@Option(
				names = {"-ca", "--class-pathA"},
				paramLabel = "PATHS",
				description = "The class path(s) of the input executable file (same as JVM -cp switch)."
		)
		private List<Path> classPathsA;

		@Option(
				names = {"-pb", "--configuration-pathB"},
				paramLabel = "CONFIGURATION-PATH",
				description = "Path of configuration file."
		)
		private Path configurationPathB;

		@Option(
				names = {"-sb", "--source-pathB"},
				paramLabel = "NAME=SOURCE-PATH",
				required = true,
				split = ",",
				description = "Path of source files."

		)
		private Map<String, Path> sourcesB;

		@Option(
				names = {"-cb", "--class-pathB"},
				paramLabel = "PATHS",
				description = "The class path(s) of the input executable file (same as JVM -cp switch)."
		)
		private List<Path> classPathsB;

		@Option(
				names = {"-o", "--output"},
				paramLabel = "OUTPUT-FILE",
				required = true,
				description = "The result file path (list file and json file)."
		)
		private Path outputFile;

		@Override
		public Void call() throws Exception {
			VersionComparison.compare(configurationPathA != null ? configurationPathA : Path.of(""), sourcesA, classPathsA != null ? classPathsA : List.of(),
					configurationPathB != null ? configurationPathB : Path.of(""), sourcesB, classPathsB != null ? classPathsB : List.of(), outputFile);
			return null;
		}
	}

}
