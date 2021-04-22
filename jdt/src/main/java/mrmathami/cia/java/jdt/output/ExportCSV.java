package mrmathami.cia.java.jdt.output;

import mrmathami.cia.java.project.JavaNodeWeightTable;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.project.JavaProjectSnapshotComparison;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.utils.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class ExportCSV {
	private final String[] componentHeader = {"Type", "Name"};
	private final String[] impactHeader = {"Type", "Name", "Weight"};

	public void exportComponentsList(JavaProjectSnapshot projectSnapshot, Path outputPath) {
		List<? extends JavaNode> nodeList = projectSnapshot.getRootNode().getAllNodes();
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(String.valueOf(outputPath)));
				CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader(componentHeader))) {
			for (JavaNode node : nodeList) {
				if (!node.isRoot()) {
					csvPrinter.printRecord(node.getEntityClass(), node.getQualifiedName());
				}
			}
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exportImpactList(JavaProjectSnapshotComparison comparison, Path outputPath) {
		final Set<JavaNode> addedNodes = comparison.getAddedNodes();
		final Set<Pair<JavaNode, JavaNode>> changedNodes = comparison.getChangedNodes();
		final Set<Pair<JavaNode, JavaNode>> unchangedNodes = comparison.getUnchangedNodes();

		final JavaNodeWeightTable nodeImpactTable = comparison.getNodeImpactTable();

		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(String.valueOf(outputPath)));
				CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader(impactHeader))) {
			addedNodes.forEach(javaNode -> {
				try {
					if (!javaNode.isRoot()) {
						csvPrinter.printRecord(javaNode.getEntityClass(), javaNode.getQualifiedName(), nodeImpactTable.getWeight(javaNode));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			printSetPair(changedNodes, csvPrinter, nodeImpactTable);
			printSetPair(unchangedNodes, csvPrinter, nodeImpactTable);
			csvPrinter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printSetPair(Set<Pair<JavaNode, JavaNode>> set, CSVPrinter csvPrinter, JavaNodeWeightTable nodeImpactTable) {
		set.forEach(pair -> {
			try {
				csvPrinter.printRecord(pair.getB().getEntityClass(), pair.getB().getQualifiedName(), nodeImpactTable.getWeight(pair.getB()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
