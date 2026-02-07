package cn.nulladev.modularmagic.content.magictree;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeNode extends Node {
    public CompositeNode(NodeType type) {
        super(type, new ArrayList<>(), List.of());
    }

    public void addChild(Node child) {
        if (child == Nodes.EMPTY) {
            throw new IllegalArgumentException("Cannot add EMPTY node");
        }
        if (child.type != this.type) {
            throw new IllegalArgumentException("Child node type " + child.type + " does not match composite node type " + this.type);
        }
        this.children.add(child);
        this.childTypes.add(child.type);
    }

    @Override
    public boolean isLeafNode() {
        return false;
    }

    @Override
    public boolean isValid() {
        if (children.isEmpty()) return false;
        for (Node child : children) {
            if (child.type != this.type || !child.isValid()) {
                return false;
            }
        }
        return true;
    }
}
