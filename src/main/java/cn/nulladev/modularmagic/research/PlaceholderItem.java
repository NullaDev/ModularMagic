package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.energy.EnergyLinkItem;
import cn.nulladev.modularmagic.utility.UnitSampleMenu;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class PlaceholderItem extends Item {
    private final MimicFamily family;
    private final boolean creative;

    public PlaceholderItem() {
        this(null, false);
    }

    public PlaceholderItem(MimicFamily family, boolean creative) {
        super(new Properties().stacksTo(1));
        this.family = family;
        this.creative = creative;
    }

    public static boolean blank(ItemStack stack) {
        return stack.is(PlaceholderContent.ITEM.get()) && target(stack) == null;
    }

    public static ResourceLocation target(ItemStack stack) {
        if (!(stack.getItem() instanceof PlaceholderItem)) return null;
        String value = EnergyLinkItem.data(stack).getString("MaterialTarget");
        return value.isEmpty() ? null : ResourceLocation.tryParse(value);
    }

    public static ItemStack create(ResourceLocation target) {
        var family = MimicFamily.fromTarget(target);
        if (family != null) return family.template();
        return record(PlaceholderContent.ITEM.toStack(), target, !BuiltInRegistries.BLOCK.containsKey(target), -1);
    }

    public static ItemStack derived(ItemStack material, long price) {
        return recordMaterial(PlaceholderContent.ITEM.toStack(), material, price);
    }

    public static ItemStack bind(MimicFamily family, ItemStack sample) {
        return bind(family.template(), sample);
    }

    public static ItemStack bind(ItemStack template, ItemStack sample) {
        return recordMaterial(template.copyWithCount(1), sample, -1);
    }

    public static ItemStack unbind(ItemStack bound) {
        var template = bound.copyWithCount(1);
        var tag = EnergyLinkItem.data(template);
        tag.remove("MaterialTarget");
        tag.remove("MaterialItem");
        tag.remove("MaterialPrice");
        if (tag.isEmpty()) template.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        else EnergyLinkItem.data(template, tag);
        return template;
    }

    private static ItemStack recordMaterial(ItemStack stack, ItemStack sample, long price) {
        if (!recordable(sample)) return ItemStack.EMPTY;
        if (sample.is(Items.WATER_BUCKET)) return record(stack, ResourceLocation.withDefaultNamespace("water"), false, price);
        if (sample.is(Items.LAVA_BUCKET)) return record(stack, ResourceLocation.withDefaultNamespace("lava"), false, price);
        if (sample.is(Items.POWDER_SNOW_BUCKET)) return record(stack, ResourceLocation.withDefaultNamespace("powder_snow"), false, price);
        var item = sample.getItem();
        if (item instanceof BlockItem block && !(item instanceof ItemNameBlockItem)) {
            return record(stack, BuiltInRegistries.BLOCK.getKey(block.getBlock()), false, price);
        }
        return record(stack, BuiltInRegistries.ITEM.getKey(item), true, price);
    }

    public static boolean fillCreative(ItemStack stack, ItemStack reference) {
        return stack.is(PlaceholderContent.CREATIVE.get()) && !recordMaterial(stack, reference, -1).isEmpty();
    }

    public static boolean recordable(ItemStack material) {
        return !material.isEmpty() && !(material.getItem() instanceof PlaceholderItem)
                && material.getComponentsPatch().isEmpty();
    }

    private static ItemStack record(ItemStack stack, ResourceLocation target, boolean item, long price) {
        var tag = EnergyLinkItem.data(stack);
        tag.putString("MaterialTarget", target.toString());
        tag.putBoolean("MaterialItem", item);
        if (price >= 0) tag.putLong("MaterialPrice", price);
        EnergyLinkItem.data(stack, tag);
        return stack;
    }

    public static Block block(ItemStack stack) {
        var id = target(stack);
        if (id == null) return Blocks.AIR;
        if (!EnergyLinkItem.data(stack).getBoolean("MaterialItem")) return BuiltInRegistries.BLOCK.get(id);
        return BuiltInRegistries.ITEM.get(id) instanceof BlockItem item ? item.getBlock() : Blocks.AIR;
    }

    public static ItemStack material(ItemStack stack) {
        var id = target(stack);
        if (id == null) return ItemStack.EMPTY;
        if (EnergyLinkItem.data(stack).getBoolean("MaterialItem")) return new ItemStack(BuiltInRegistries.ITEM.get(id));
        return switch (id.toString()) {
            case "minecraft:water" -> new ItemStack(Items.WATER_BUCKET);
            case "minecraft:lava" -> new ItemStack(Items.LAVA_BUCKET);
            case "minecraft:powder_snow" -> new ItemStack(Items.POWDER_SNOW_BUCKET);
            default -> new ItemStack(BuiltInRegistries.BLOCK.get(id));
        };
    }

    public static long price(ItemStack stack) {
        long base = basePrice(stack);
        if (base < 0 || !stack.is(PlaceholderContent.ADVANCED_PLANTS.get())) return base;
        long boneMeal = basePrice(PlantTemplateRecipe.boneMeal(stack));
        return boneMeal < 0 || boneMeal > Long.MAX_VALUE - base ? -1 : base + boneMeal;
    }

    private static long basePrice(ItemStack stack) {
        var id = target(stack);
        if (id == null) return -1;
        var tag = EnergyLinkItem.data(stack);
        if (tag.contains("MaterialPrice")) return tag.getLong("MaterialPrice");
        if (stack.getItem() instanceof PlaceholderItem sample && sample.creative) return PlaceholderContent.CREATIVE_PRICE.get();
        var configured = PlaceholderContent.ITEM_COSTS.get(id.toString());
        if (tag.getBoolean("MaterialItem") && configured != null) return configured.get();
        if (block(stack) == Blocks.AIR && stack.getItem() instanceof PlaceholderItem sample && sample.family != null) {
            try {
                double cost = cn.nulladev.modularmagic.spell.billing.CostRules.evaluate(
                        PlaceholderContent.FAMILY_COSTS.get(sample.family).get(), java.util.Map.of("hardness", 0D));
                return Double.isFinite(cost) && cost >= 0 ? (long) Math.ceil(cost) : -1;
            } catch (RuntimeException error) {
                return -1;
            }
        }
        double cost = MaterialPlacement.fee(block(stack).defaultBlockState());
        return Double.isFinite(cost) && cost >= 0 ? (long) Math.ceil(cost) : -1;
    }

    private static final List<String> APPEARANCES = appearances();

    private static List<String> appearances() {
        try (var in = PlaceholderItem.class.getResourceAsStream("/data/modularmagic/mimic_appearances.json")) {
            if (in == null) throw new IllegalStateException("Missing sample appearances");
            var appearances = new java.util.ArrayList<String>();
            com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonArray().forEach(value -> appearances.add(value.getAsString()));
            return List.copyOf(appearances);
        } catch (java.io.IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static float appearance(ItemStack stack) {
        var id = target(stack);
        if (id == null) return 0;
        int index = APPEARANCES.indexOf(id.toString());
        return index < 0 ? 1 : index + 2;
    }

    private static Component materialName(ItemStack stack) {
        if (EnergyLinkItem.data(stack).getBoolean("MaterialItem")) return material(stack).getHoverName();
        if (block(stack) == Blocks.SNOW) return Component.translatable("placeholder.modularmagic.snow_name");
        return block(stack).getName();
    }

    @Override
    public Component getName(ItemStack stack) {
        return family != null || creative || target(stack) == null
                ? super.getName(stack)
                : Component.translatable("placeholder.modularmagic.name", materialName(stack));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (creative && player.isCreative()) {
            if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider(
                    (id, inv, user) -> new UnitSampleMenu(id, inv, hand),
                    stack.getHoverName()), buffer -> buffer.writeEnum(hand));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!blank(stack)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inv, user) -> new PlaceholderMenu(id, inv, stack),
                    Component.translatable("placeholder.modularmagic.title")), buf -> {});
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack carried, Slot slot, ClickAction action,
                                           Player player, SlotAccess access) {
        if (!creative || !player.isCreative() || action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        var filled = recordMaterial(stack.copy(), carried, -1);
        if (filled.isEmpty()) return false;
        slot.setByPlayer(filled);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (family != null || creative) {
            if (target(stack) == null) lines.add(Component.translatable(creative
                    ? "placeholder.modularmagic.creative_fill" : "placeholder.modularmagic.family_fill"));
            else lines.add(Component.translatable("placeholder.modularmagic.filled", materialName(stack)));
        }
        if (target(stack) == null) return;
        lines.add(Component.translatable(EnergyLinkItem.data(stack).getBoolean("MaterialItem")
                ? "placeholder.modularmagic.type_item" : "placeholder.modularmagic.type_block"));
        long price = price(stack);
        if (price >= 0) lines.add(Component.translatable("placeholder.modularmagic.fee", AetherNumbers.format(price)));
    }
}
