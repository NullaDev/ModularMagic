package cn.nulladev.modularmagic.content.magictree;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class Node {

    public final NodeType type;
    public final List<NodeType> childTypes;
    public final List<Node> children;
    public final List<NodeVariableType> variableTypes;
    public final List<String> variable;

    public Node(NodeType type, List<NodeType> childTypes, List<NodeVariableType> variableTypes) {
        this.type = type;
        this.childTypes = childTypes;
        this.children = new ArrayList<Node>(childTypes.size());
        for (int i = 0; i < childTypes.size(); i++) {
            this.children.add(Nodes.EMPTY);
        }
        this.variableTypes = variableTypes;
        this.variable = new ArrayList<String>(variableTypes.size());
        for (int i = 0; i < variableTypes.size(); i++) {
            this.variable.add("");
        }
    }
    public abstract String toJSON();

    public boolean isLeafNode() {
        return childTypes.size() == 0;
    }

    public boolean canBeRemoved() {
        return isLeafNode() || children.stream().allMatch(child -> child == Nodes.EMPTY);
    }

    public boolean isValid() {
        if (children.size() != childTypes.size()) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            NodeType expectedType = childTypes.get(i);
            if (child == Nodes.EMPTY) {
                return false;
            }
            if (child.type != expectedType) {
                return false;
            }
            if (!child.isValid()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack toStack() {
        return Nodes.itemRelateMap.getOrDefault(this, ItemStack.EMPTY);
    }

}

