package cn.nulladev.modularmagic.item;

import cn.nulladev.modularmagic.spell.NodeCatalog;
import cn.nulladev.modularmagic.spell.NodeTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

public final class NodeItem extends Item {
    private final String template;
    public NodeItem(Properties properties, String template) { super(properties); this.template = template; }
    public NodeTemplate template() { return NodeCatalog.get(template); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var schema = template();
        if (!schema.category().equals("root")) tooltip.add(Component.translatable("category.modularmagic." + schema.category())
                .withColor(NodeCatalog.category(schema.category()).rgb()));
        if (flag.isAdvanced()) tooltip.add(Component.literal(schema.nativeType()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
