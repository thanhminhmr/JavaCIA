package mrmathami.cia.java.jdt.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.jdt.project.SourceFile;
import mrmathami.cia.java.jdt.tree.AbstractEntity;
import mrmathami.cia.java.jdt.tree.node.attribute.AbstractNonRootNode;
import mrmathami.cia.java.xml.JavaXmlNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public class XMLNode extends AbstractNonRootNode implements JavaXmlNode {
	@Nullable private final String textContent;
	@Nullable private final NodeList children;
	@Nullable private final NamedNodeMap listAttributes;

	private static final long serialVersionUID = -1L;


	public XMLNode(@Nullable SourceFile sourceFile, @Nonnull AbstractNode parent, @Nonnull String nodeName, @Nullable String textContent, @Nullable NodeList children, @Nullable NamedNodeMap listAttributes, int order) {
		super(sourceFile, parent, nodeName, parent.getUniqueName() + "." + nodeName + "#", order);
		this.textContent = textContent;
		this.children = children;
		this.listAttributes = listAttributes;
	}

	@Nullable
	public String getTextContent() {
		return textContent;
	}

	@Nullable
	public NamedNodeMap getAttributes() {
		return listAttributes;
	}

	//region Jsonify


	@Override
	protected void internalToJsonStart(@Nonnull StringBuilder builder, @Nonnull String indentation) {
		super.internalToJsonStart(builder, indentation);
		builder.append(", \"textContent\": \"");
		AbstractEntity.internalEscapeString(builder, textContent);
		builder.append("\", \"listAttributes\": ").append(internalToReferenceJson(listAttributes));
	}

	/*protected void internalToReferenceJsonStart(@Nonnull StringBuilder builder) {
		builder.append(", \"nodeName\": \"").append(this.getSimpleName())
				.append("\", \"textContent\": \"");
		AbstractEntity.internalEscapeString(builder, textContent);
		builder.append("\", \"listAttributes\": ").append(internalToReferenceJson(listAttributes));
	}*/

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
