package cn.nulladev.modularmagic.content.magictree;

import net.minecraft.world.item.ItemStack;
import org.openjdk.nashorn.internal.runtime.regexp.joni.constants.NodeType;

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
            NodeProperty.NodeType expectedType = this.property.childTypes.get(i);
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

    public NodeRecord toRecord() {
        List<NodeRecord> childrenRecords = children.stream()
                .map(Node::toRecord)
                .toList();
        return new NodeRecord(property, childrenRecords, List.copyOf(variable));
    }

    public static Node fromRecord(NodeRecord record) {
        Node node = new Node(record.property());
        for (int i = 0; i < record.children().size(); i++) {
            node.children.set(i, fromRecord(record.children().get(i)));
        }
        for (int i = 0; i < record.variable().size(); i++) {
            node.variable.set(i, record.variable().get(i));
        }
        return node;
    }

    public record NodeRecord(
            NodeProperty property,
            List<NodeRecord> children,
            List<String> variable
    ) {}

}
