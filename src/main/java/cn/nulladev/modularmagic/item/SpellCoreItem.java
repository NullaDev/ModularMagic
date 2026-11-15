package cn.nulladev.modularmagic.item;

import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.SpellCompiler;
import cn.nulladev.modularmagic.spell.SpellDocument;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

public final class SpellCoreItem extends Item {
    public static final String DOCUMENT = "modularmagic_document";
    private final boolean formed;

    public SpellCoreItem(Properties properties, boolean formed) { super(properties); this.formed = formed; }

    public static SpellDocument read(ItemStack stack) {
        if (stack.getItem() instanceof NodeItem node && node.template().category().equals("root")) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.contains(DOCUMENT)) return SpellDocument.create(node.template().id());
            var document = SpellDocument.fromJson(tag.getString(DOCUMENT));
            if (!document.root.template.equals(node.template().id()) || document.root.hasChildren())
                throw new IllegalArgumentException("Invalid empty core document");
            return document;
        }
        if (!(stack.getItem() instanceof SpellCoreItem)) throw new IllegalArgumentException("Insert a spell root or a core");
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(DOCUMENT)) throw new IllegalArgumentException("This core has no assembly document");
        return SpellDocument.fromJson(tag.getString(DOCUMENT));
    }

    public static ItemStack write(SpellDocument document, SpellCompiler.Result result) {
        var stack = new ItemStack(!document.root.hasChildren() ? ModContent.NODES.get(document.root.template).get()
                : result.complete() ? ModContent.FORMED_CORE.get() : ModContent.UNFORMED_CORE.get());
        var tag = new CompoundTag();
        tag.putString(DOCUMENT, document.toJson());
        if (result.complete()) {
            tag.putString("l2magic_spell", result.spellJson());
            tag.putString("l2magic_projectiles", result.projectilesJson());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ItemStack write(SpellDocument document, SpellCompiler.Result result, ItemStack previous) {
        var stack = write(document, result);
        if (document.root.hasChildren() && previous.has(DataComponents.CUSTOM_NAME))
            stack.set(DataComponents.CUSTOM_NAME, previous.get(DataComponents.CUSTOM_NAME));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        try {
            var document = read(stack);
            tooltip.add(Component.translatable(document.root.schema().translationKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable("tooltip.modularmagic.nodes", document.nodes().size()));
        } catch (RuntimeException e) {
            tooltip.add(Component.translatable("tooltip.modularmagic.invalid").withStyle(ChatFormatting.RED));
        }
    }
}
