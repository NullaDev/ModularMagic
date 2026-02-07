package cn.nulladev.modularmagic.content.magictree;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Nodes {

    public static final Map<Node, ItemStack> itemRelateMap = new HashMap<>();
    public static final Node EMPTY = new Node(NodeType.EMPTY, List.of(), List.of()) {
        @Override
        public String toJSON() {
            return "{}";
        }
    };

    public static final Node SELECTOR_BALL = new Node(NodeType.SELECTOR, List.of(), List.of(NodeVariableType.DOUBLE)) {
        @Override
        public String toJSON() {
            return """
            {
              "selector": {
                "type": "l2magic:ball",
                "r": %s
              }
            }
            """.formatted(this.variable.get(0));
        }
    };

    public static final Node SELECTOR_CYLINDER = new Node(NodeType.SELECTOR, List.of(), List.of(NodeVariableType.DOUBLE, NodeVariableType.DOUBLE)) {
        @Override
        public String toJSON() {
            return """
            {
              "selector": {
                "type": "l2magic:cylinder",
                "r": %s,
                "y": %s
              }
            }
            """.formatted(this.variable.get(0), this.variable.get(1));
        }
    };
}
