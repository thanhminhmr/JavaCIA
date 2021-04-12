package mrmathami.cia.java.jdt.gephi;

import mrmathami.cia.java.jdt.gephi.model.EdgeGephi;
import mrmathami.cia.java.jdt.gephi.model.NodeGephi;
import mrmathami.cia.java.project.JavaProjectSnapshot;
import mrmathami.cia.java.tree.node.JavaNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Printer {
	public String writeGephi(JavaProjectSnapshot projectSnapshot) {
		List<? extends JavaNode> nodeList = projectSnapshot.getRootNode().getAllNodes();
		HashMap<JavaNode, Set<? extends JavaNode>> dependencyToMap = new HashMap<>();
		for (JavaNode node : nodeList) {
			Set<? extends JavaNode> dependencyToNodes = node.getDependencyToNodes();
			dependencyToMap.put(node, dependencyToNodes);
		}
		List<NodeGephi> nodeGephiList = new ArrayList<>();
		List<EdgeGephi> edgeGephiList = new ArrayList<>();
		int count = 0;
		for (Map.Entry<JavaNode, Set<? extends JavaNode>> entry : dependencyToMap.entrySet()) {
			NodeGephi nodeGephi = new NodeGephi(entry.getKey().getId(), entry.getKey().getNodeName(), NodeGephi.Type.UNCHANGE);
			nodeGephiList.add(nodeGephi);
			Set<? extends JavaNode> value = entry.getValue();
			for (JavaNode javaNode : value) {
				EdgeGephi edgeGephi = new EdgeGephi(count, entry.getKey().getId(), javaNode.getId());
				count++;
				edgeGephiList.add(edgeGephi);
			}
		}

		final String open = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<gexf xmlns=\"http://www.gexf.net/1.3\" version=\"1.3\" xmlns:viz=\"http://www.gexf.net/1.3/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.gexf.net/1.3 http://www.gexf.net/1.3/gexf.xsd\">" +
				"\n<graph defaultedgetype=\"directed\" mode=\"static\">\n";

		String listNode = writeListNodes(nodeGephiList);
		String listEdge = writeListEdges(edgeGephiList);

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

}
