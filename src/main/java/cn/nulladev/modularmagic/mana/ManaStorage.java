package cn.nulladev.modularmagic.mana;

import net.minecraft.world.item.ItemStack;

/** Implemented by items. Amounts are integer mana units; mutations belong on the logical server thread.
 * Simulation must not mutate the stack. Implementations return the amount actually moved, never more than requested.
 * This API neither regenerates mana nor treats the player as its owner. */
public interface ManaStorage {
    // Saved fixed-point balances survive the currency conversion unchanged.
    long LEGACY_SCALE = 10000;
    long SCALE = LEGACY_SCALE / cn.nulladev.modularmagic.spell.ResourceBalance.MANA_SCALE;
    long capacity(ItemStack stack);
    long stored(ItemStack stack);
    long receive(ItemStack stack, long amount, boolean simulate);
    long extract(ItemStack stack, long amount, boolean simulate);
    default boolean infinite(ItemStack stack) { return false; }
    default long storedUnits(ItemStack stack) { return stored(stack) > Long.MAX_VALUE / SCALE ? Long.MAX_VALUE : stored(stack) * SCALE; }
    default long receiveUnits(ItemStack stack, long amount, boolean simulate) { return amount % SCALE == 0 ? receive(stack, amount / SCALE, simulate) * SCALE : 0; }
    default long extractUnits(ItemStack stack, long amount, boolean simulate) { return amount % SCALE == 0 ? extract(stack, amount / SCALE, simulate) * SCALE : 0; }
}
