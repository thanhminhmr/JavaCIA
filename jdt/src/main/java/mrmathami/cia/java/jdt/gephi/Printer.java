package mrmathami.cia.java.jdt.gephi;

import mrmathami.cia.java.jdt.gephi.model.EdgeGephi;
import mrmathami.cia.java.jdt.gephi.model.NodeGephi;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaRootNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Printer {
	public String writeGephi(JavaProjectSnapshot projectSnapshot) {
		JavaRootNode rootNode = projectSnapshot.getRootNode();
		List<? extends JavaNode> nodeList = rootNode.getAllNodes();
		List<NodeGephi> nodeGephiList = new ArrayList<>();
		for (JavaNode node : nodeList) {
			NodeGephi nodeGephi = new NodeGephi(node.getId(), node.getNodeName(), NodeGephi.Type.UNCHANGE);
			nodeGephiList.add(nodeGephi);
		}
		final String open = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<gexf xmlns=\"http://www.gexf.net/1.3\" version=\"1.3\" xmlns:viz=\"http://www.gexf.net/1.3/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.gexf.net/1.3 http://www.gexf.net/1.3/gexf.xsd\">" +
				"\n<graph defaultedgetype=\"directed\" mode=\"static\">\n";
		NodeGephi nodeGephi1 = new NodeGephi(0, "classA", NodeGephi.Type.UNCHANGE);
		NodeGephi nodeGephi2 = new NodeGephi(1, "classB", NodeGephi.Type.UNCHANGE);
		NodeGephi nodeGephi3 = new NodeGephi(2, "classC", NodeGephi.Type.UNCHANGE);

		EdgeGephi edgeGephi1 = new EdgeGephi("0", "0", "1");
		EdgeGephi edgeGephi2 = new EdgeGephi("1", "1", "2");

		String listNode = writeListNodes(Arrays.asList(nodeGephi1, nodeGephi2, nodeGephi3));
		String listEdge = writeListEdges(Arrays.asList(edgeGephi1, edgeGephi2));

		return open + listNode + listEdge + "\n</graph>\n" + "</gexf>";
	}
	public String writeListNodes(List<NodeGephi> nodeGephiList) {
		StringBuilder result = new StringBuilder("\t<nodes>");
		for (NodeGephi node : nodeGephiList) {
			result.append("\n\t\t").append(node.toString());
		}
		result.append("\n\t</nodes>");
		return result.toString();
	}

	public String writeListEdges(List<EdgeGephi> edgeGephiList) {
		StringBuilder result = new StringBuilder("\n\t<edges>");
		for (EdgeGephi node : edgeGephiList) {
			result.append("\n\t\t").append(node.toString());
		}
		result.append("\n\t</edges>");
		return result.toString();
	}

	public static void main(String[] args) {
		Printer printer = new Printer();
		//System.out.println(printer.writeGephi());
	}
}
