package mrmathami.cia.java.tree.node;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.node.container.JavaXMLContainer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

public interface JavaXMLNode extends JavaNode, JavaXMLContainer {
	@Nonnull
	String OBJECT_CLASS = "JavaXMLNode";

	//region Basic Getter
	@Nonnull
	@Override
	default String getEntityClass() {
		return OBJECT_CLASS;
	}

	@Nonnull
	@Override
	default JavaXMLNode asXMLNode() {
		return this;
	}


	@Nonnull
	String getTextContent();

	@Nonnull
	NodeList getChildNodes();

	@Nonnull
	NamedNodeMap getAttributes();
	//end region Basic Getter
}
