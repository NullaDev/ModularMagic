package cn.nulladev.modularmagic.content.item;

import cn.nulladev.modularmagic.content.magictree.Node;
import cn.nulladev.modularmagic.content.magictree.Nodes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MagicNodeItem extends Item {
    public final Node relateNode;
    public MagicNodeItem(Properties p, Node node) {
        super(p);
        this.relateNode = node;
        Nodes.itemRelateMap.put(node, new ItemStack(this));
    }
}
