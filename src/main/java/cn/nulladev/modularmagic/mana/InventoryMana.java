package cn.nulladev.modularmagic.mana;

import net.minecraft.world.entity.player.Player;

/** Main inventory, hotbar and offhand; excludes armor, nested bags and the cursor. */
public final class InventoryMana {
    public record Total(long units, long capacityUnits, boolean infinite) {
        public double stored(){return (double)units/ManaStorage.SCALE;}
        public double capacity(){return (double)capacityUnits/ManaStorage.SCALE;}
    }
    public static Total total(Player player) {
        long stored = 0, capacity = 0;
        for (var inventory : java.util.List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (var stack : inventory) if (!stack.isEmpty() && stack.getItem() instanceof ManaStorage mana) {
                if (mana.infinite(stack)) return new Total(0, 0, true);
                stored = add(stored, mana.storedUnits(stack)); capacity = add(capacity, mana.capacity(stack)*ManaStorage.SCALE);
            }
        }
        return new Total(stored, capacity, false);
    }
    private static long add(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
    private InventoryMana() {}
}
