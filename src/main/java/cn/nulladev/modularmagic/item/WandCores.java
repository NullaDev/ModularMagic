package cn.nulladev.modularmagic.item;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class WandCores {
    public static final int CAPACITY = 9;
    public static final int MAX_DATA_BYTES = 524288;
    public static NonNullList<ItemStack> read(ItemStack wand) {
        var cores = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
        wand.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(cores);
        return cores;
    }
    public static boolean accepts(ItemStack core) {
        if (core.getCount() != 1) return false;
        try { SpellCoreItem.read(core); return true; } catch (RuntimeException ignored) { return false; }
    }
    public static boolean fits(List<ItemStack> cores) {
        long bytes = 0;
        for (var core : cores) {
            if (!core.isEmpty() && !accepts(core)) return false;
            bytes += core.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().toString().getBytes(StandardCharsets.UTF_8).length;
        }
        return bytes <= MAX_DATA_BYTES;
    }
    public static void write(ItemStack wand, List<ItemStack> cores) {
        if (cores.size() != CAPACITY || !fits(cores)) throw new IllegalArgumentException("Wand core storage is full or invalid");
        wand.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cores));
        select(wand, selected(wand));
    }
    public static int selected(ItemStack wand) {
        int index = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("modularmagic_selected");
        var cores = read(wand);
        if (index >= 0 && index < CAPACITY && !cores.get(index).isEmpty()) return index;
        for (int i = 0; i < CAPACITY; i++) if (!cores.get(i).isEmpty()) return i;
        return 0;
    }
    public static void select(ItemStack wand, int index) {
        if (index < 0 || index >= CAPACITY) return;
        var tag = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("modularmagic_selected", index);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    private WandCores() {}
}
