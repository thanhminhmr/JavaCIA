package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.AbstractEntity;
import mrmathami.cia.java.project.JavaSourceFile;
import mrmathami.cia.java.xml.JavaXmlNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class XMLNode extends AbstractNode implements JavaXmlNode {
	private final String nodeName;
	private final AbstractNode parent;
	private String textContent;
	private NodeList children;
	private NamedNodeMap listAttributes;
	private final SourceFile sourceFile;

	private static final long serialVersionUID = -1L;

	public XMLNode(@Nullable SourceFile sourceFile, @Nonnull String nodeName, @Nonnull AbstractNode parent, String textContent, NodeList children, NamedNodeMap listAttributes) {
		this.sourceFile = sourceFile;
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
	public String getQualifiedName() {
		return nodeName;
	}

	@Override
	public String getUniqueName() {
		return nodeName;
	}

	@Override
	public JavaSourceFile getSourceFile() {
		return sourceFile;
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
	public String getSimpleName() {
		return nodeName;
	}

	public String getTextContent() {
		return textContent;
	}

	public NodeList getChildNodes() {
		return children;
	}

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
				.append("\", \"textContent\": \"");
		AbstractEntity.internalEscapeString(builder, textContent);
		builder.append("\", \"listAttributes\": ").append(internalToReferenceJson(listAttributes));
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
