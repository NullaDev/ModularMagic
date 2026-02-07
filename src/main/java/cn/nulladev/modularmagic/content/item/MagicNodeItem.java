package cn.nulladev.modularmagic.content.item;

import cn.nulladev.modularmagic.content.magictree.NodeProperty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MagicNodeItem extends Item {
    public final NodeProperty relateNodeProperty;
    public MagicNodeItem(Properties p, NodeProperty property) {
        super(p);
        this.relateNodeProperty = property;
        NodeProperty.itemRelateMap.put(property, new ItemStack(this));
    }
}
