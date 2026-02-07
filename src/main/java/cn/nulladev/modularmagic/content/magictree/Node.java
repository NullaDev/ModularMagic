package cn.nulladev.modularmagic.content.magictree;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Node {
    public final NodeProperty property;
    public final List<Node> children;
    public final List<String> variable;

    public Node(NodeProperty property) {
        this.property = property;
        this.children = new ArrayList<Node>(property.childTypes.size());
        for (int i = 0; i < property.childTypes.size(); i++) {
            this.children.add(new Node(NodeProperty.EMPTY));
        }
        this.variable = new ArrayList<String>(property.variableTypes.size());
        for (int i = 0; i < property.variableTypes.size(); i++) {
            this.variable.add("");
        }
    }

    public String toJSON() {
        return this.property.toJSON(this.children, this.variable);
    }

    public boolean isLeafNode() {
        return this.property.childTypes.size() == 0;
    }

    public boolean canBeRemoved() {
        return isLeafNode() || children.stream().allMatch(child -> child.property == NodeProperty.EMPTY);
    }

    public boolean isValid() {
        if (children.size() != this.property.childTypes.size()) {
            return false;
        }
        for (int i = 0; i < children.size(); i++) {
            Node child = children.get(i);
            NodeType expectedType = this.property.childTypes.get(i);
            if (child.property == NodeProperty.EMPTY) {
                return false;
            }
            if (child.property.type != expectedType) {
                return false;
            }
            if (!child.isValid()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack toStack() {
        return NodeProperty.itemRelateMap.getOrDefault(this.property, ItemStack.EMPTY);
    }

}

