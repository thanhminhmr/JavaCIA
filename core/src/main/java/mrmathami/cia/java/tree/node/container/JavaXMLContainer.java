package mrmathami.cia.java.tree.node.container;

import mrmathami.annotations.Nonnull;
import mrmathami.cia.java.tree.node.JavaNode;
import mrmathami.cia.java.tree.node.JavaXMLNode;

import java.util.List;

public interface JavaXMLContainer extends JavaNode {
    @Nonnull
    @Override
    default List<? extends JavaXMLNode> getChildXMLNodes(@Nonnull List<JavaXMLNode> xmlNodes) {
        return getChildren(JavaXMLNode.class, xmlNodes);
    }

    @Nonnull
    @Override
    default List<? extends JavaXMLNode> getChildXMLNodes() {
        return getChildren(JavaXMLNode.class);
    }

    @Nonnull
    @Override
    default JavaXMLContainer asXMLContainer() {
        return this;
    }
}
