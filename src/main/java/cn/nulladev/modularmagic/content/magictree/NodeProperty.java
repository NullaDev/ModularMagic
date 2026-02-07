package cn.nulladev.modularmagic.content.magictree;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class NodeProperty {
    public final NodeType type;
    public final List<NodeType> childTypes;
    public final List<NodeVariableType> variableTypes;

    public NodeProperty(NodeType type, List<NodeType> childTypes, List<NodeVariableType> variableTypes) {
        this.type = type;
        this.childTypes = childTypes;
        this.variableTypes = variableTypes;
    }

    public abstract String toJSON(List<Node> children, List<String> variable);

    public static final Map<NodeProperty, ItemStack> itemRelateMap = new HashMap<>();
    public static final NodeProperty EMPTY = new NodeProperty(NodeType.EMPTY, List.of(), List.of()) {
        @Override
        public String toJSON(List<Node> children, List<String> variable) {
            return "{}";
        }
    };

    public static final NodeProperty SELECTOR_BALL = new NodeProperty(NodeType.SELECTOR, List.of(), List.of(NodeVariableType.DOUBLE)) {
        @Override
        public String toJSON(List<Node> children, List<String> variable) {
            return """
            {
              "selector": {
                "type": "l2magic:ball",
                "r": %s
              }
            }
            """.formatted(variable.get(0));
        }
    };

    public static final NodeProperty SELECTOR_CYLINDER = new NodeProperty(NodeType.SELECTOR, List.of(), List.of(NodeVariableType.DOUBLE, NodeVariableType.DOUBLE)) {
        @Override
        public String toJSON(List<Node> children, List<String> variable) {
            return """
            {
              "selector": {
                "type": "l2magic:cylinder",
                "r": %s,
                "y": %s
              }
            }
            """.formatted(variable.get(0), variable.get(1));
        }
    };
}
