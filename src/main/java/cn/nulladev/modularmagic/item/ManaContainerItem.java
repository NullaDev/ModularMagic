package cn.nulladev.modularmagic.item;

import cn.nulladev.modularmagic.mana.ManaStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.function.LongSupplier;

public final class ManaContainerItem extends Item implements ManaStorage {
    private static final String MANA = "modularmagic_mana";
    private static final String UNITS = "modularmagic_mana_units", RESERVED = "modularmagic_reserved_units";
    private final LongSupplier limit;
    private final boolean infinite;
    private java.util.function.Supplier<ManaContainerItem> replacement;
    private boolean legacyFull;
    public static ManaContainerItem legacy(Properties properties, LongSupplier capacity, boolean full, java.util.function.Supplier<ManaContainerItem> replacement) {
        var item = new ManaContainerItem(properties, capacity, false);
        item.replacement = replacement; item.legacyFull = full; return item;
    }
    public ManaContainerItem(Properties properties, LongSupplier capacity, boolean infinite) {
        super(properties.stacksTo(1)); this.limit = capacity; this.infinite = infinite;
    }
    @Override public long capacity(ItemStack stack) { return infinite ? Long.MAX_VALUE : limit.getAsLong(); }
    @Override public long stored(ItemStack stack) {
        return infinite ? Long.MAX_VALUE : storedUnits(stack) / SCALE;
    }
    @Override public long storedUnits(ItemStack stack) {
        if (infinite) return Long.MAX_VALUE;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.clamp(tag.contains(UNITS) ? tag.getLong(UNITS) : tag.contains(MANA) ? tag.getLong(MANA)*LEGACY_SCALE : legacyFull ? capacity(stack)*SCALE : 0, 0L, capacity(stack)*SCALE);
    }
    public ItemStack filledStack() {
        var stack = new ItemStack(this);
        if (!infinite) writeUnits(stack, capacity(stack)*SCALE);
        return stack;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !infinite; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * storedUnits(stack) / (capacity(stack)*SCALE)); }
    @Override public int getBarColor(ItemStack stack) { return 0x47c7df; }
    @Override public long receive(ItemStack stack, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long units = amount > Long.MAX_VALUE / SCALE ? Long.MAX_VALUE : amount*SCALE;
        // Whole-unit callers never report an unreturned fractional transfer.
        long room = Math.max(0, capacity(stack)*SCALE-storedUnits(stack)-reserved(stack));
        return receiveUnits(stack, Math.min(units, room/SCALE*SCALE), simulate)/SCALE;
    }
    @Override public long receiveUnits(ItemStack stack, long amount, boolean simulate) {
        if (amount <= 0 || infinite || stack.getItem() != this || stack.getCount() != 1) return 0;
        long current = storedUnits(stack), moved = Math.min(amount, Math.max(0, capacity(stack)*SCALE - current - reserved(stack)));
        if (!simulate && moved > 0) writeUnits(stack, current + moved);
        return moved;
    }
    @Override public long extract(ItemStack stack, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        if (infinite) return amount;
        long units = amount > Long.MAX_VALUE / SCALE ? Long.MAX_VALUE : amount*SCALE;
        return extractUnits(stack, Math.min(units, storedUnits(stack)/SCALE*SCALE), simulate)/SCALE;
    }
    @Override public long extractUnits(ItemStack stack, long amount, boolean simulate) {
        if (amount <= 0 || stack.getItem() != this || stack.getCount() != 1) return 0;
        if (infinite) return amount;
        long current = storedUnits(stack), moved = Math.min(amount, current);
        if (!simulate && moved > 0) writeUnits(stack, current - moved);
        return moved;
    }
    public static void writeUnits(ItemStack stack, long amount) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.remove(MANA); tag.putLong(UNITS, amount); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    public static long reserved(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong(RESERVED); }
    public static void reserved(ItemStack stack, long amount) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putLong(RESERVED,amount); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    @Override public boolean infinite(ItemStack stack) { return infinite; }
    @Override public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof net.minecraft.world.entity.player.Player player)) return;
        cn.nulladev.modularmagic.spell.billing.ManaEscrow.get(player.getServer()).claim(stack);
        if (replacement == null) return;
        if (player.getInventory().getItem(slot) != stack) return;
        var converted = new ItemStack(replacement.get());
        converted.applyComponents(stack.getComponentsPatch());
        writeUnits(converted, storedUnits(stack));
        player.getInventory().setItem(slot, converted);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(infinite ? Component.translatable("tooltip.modularmagic.mana_infinite")
                : Component.translatable("tooltip.modularmagic.mana_stored", stored(stack), capacity(stack)));
    }
}
