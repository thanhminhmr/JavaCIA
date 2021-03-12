package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.node.JavaXMLNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public final class XMLNode extends AbstractNode implements JavaXMLNode {

	private final String nodeName;
	private final AbstractNode parent;
	private String textContent;
	private NodeList children;
	private NamedNodeMap listAttributes;

	private static final long serialVersionUID = -1L;

	public XMLNode(@Nonnull String nodeName, @Nonnull AbstractNode parent, String textContent, NodeList children, NamedNodeMap listAttributes) {
		this.nodeName = nodeName;
		this.parent = parent;
		this.textContent = textContent;
		this.children = children;
		this.listAttributes = listAttributes;
	}

	@Override
	public boolean isRoot() {
		return (parent == null);
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	@Override
	public String getQualifiedName() {
		return nodeName;
	}

	@Override
	public String getUniqueName() {
		return nodeName;
	}

	@Override
	public RootNode getRoot() {
		return parent.getRoot();
	}

	@Override
	public AbstractNode getParent() {
		return parent;
	}

	@Override
	public String getTextContent() {
		return textContent;
	}

	@Override
	public NodeList getChildNodes() {
		return children;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return listAttributes;
	}

	public void setTextContent(String textContent) {
		this.textContent = textContent;
	}

	public void setChildren(NodeList children) {
		this.children = children;
	}

	public void setListAttributes(NamedNodeMap listAttributes) {
		this.listAttributes = listAttributes;
	}

	//region Jsonify

	protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		builder.append(", \"nodeName\": \"").append(nodeName)
				.append("\", \"textContent\": \"").append(textContent)
				.append("\", \"listAttributes\": ").append(internalToReferenceJson(listAttributes));
	}

	private static String internalToReferenceJson(NamedNodeMap namedNodeMap) {
		String listAttributes = "[ ";
		if (namedNodeMap != null) {
			for (int i = 0; i < namedNodeMap.getLength(); i++) {
				listAttributes += "{\n \"" + "name" + "\": " + "\"" + namedNodeMap.item(i).getNodeName() + "\",";
				listAttributes += "\"" + "textContent" + "\": " + "\"" + namedNodeMap.item(i).getTextContent() + "\" \n}";
				if (i < namedNodeMap.getLength() - 1) {
					listAttributes += ",";
				}
			}
		}
		listAttributes += " ]";
		return listAttributes;
	}

	//endregion Jsonify
}
