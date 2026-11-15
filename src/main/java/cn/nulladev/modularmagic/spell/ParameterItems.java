package cn.nulladev.modularmagic.spell;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Optional;

/** Reference slots sample the cursor stack; they never own or consume an item. */
public final class ParameterItems {
    public static String kind(NodeTemplate template, String key) {
        template.field(key);
        if (key.equals("icon") || key.equals("item")) return "item";
        if (key.equals("block") || key.equals("state")) return "block";
        if (key.equals("effect")) return "effect";
        return "";
    }

    public static String value(NodeTemplate template, String key, ItemStack stack) {
        if (stack.isEmpty()) throw new IllegalArgumentException("Hold a reference item first");
        return switch (kind(template, key)) {
            case "item" -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            case "block" -> {
                if (!(stack.getItem() instanceof BlockItem block)) throw new IllegalArgumentException("This field needs a block item");
                yield BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
            }
            case "effect" -> {
                var contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                var effects = contents.getAllEffects().iterator();
                if (!effects.hasNext()) throw new IllegalArgumentException("This item has no potion effect");
                yield effects.next().getEffect().unwrapKey().orElseThrow().location().toString();
            }
            default -> throw new IllegalArgumentException("This parameter does not accept item references");
        };
    }

    public static ItemStack preview(NodeTemplate template, String key, String text, HolderLookup.Provider registries) {
        try {
            var id = ResourceLocation.tryParse(text);
            if (id == null) return ItemStack.EMPTY;
            return switch (kind(template, key)) {
                case "item" -> new ItemStack(BuiltInRegistries.ITEM.get(id));
                case "block" -> new ItemStack(BuiltInRegistries.BLOCK.get(id).asItem());
                case "effect" -> {
                    var effect = BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow();
                    var potion = new ItemStack(Items.POTION);
                    potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(effect, 200))));
                    yield potion;
                }
                default -> ItemStack.EMPTY;
            };
        } catch (RuntimeException ignored) { return ItemStack.EMPTY; }
    }
    private ParameterItems() {}
}
