package cn.nulladev.modularmagic.menu;

import cn.nulladev.modularmagic.item.*;
import cn.nulladev.modularmagic.registry.ModContent;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The nine physical cores live in the held wand's vanilla container component. */
public final class WandMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack wand;
    private final SimpleContainer cores = new SimpleContainer(WandCores.CAPACITY);
    private final int lockedInventorySlot;

    public WandMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModContent.WAND_MENU.get(), id);
        owner = inventory.player; this.hand = hand; wand = owner.getItemInHand(hand);
        if (!(wand.getItem() instanceof CoreWandItem)) throw new IllegalStateException("Missing wand");
        lockedInventorySlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : 40;
        var stored = WandCores.read(wand);
        for (int i = 0; i < WandCores.CAPACITY; i++) cores.setItem(i, stored.get(i));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            final int index = row * 3 + col;
            addSlot(new Slot(cores, index, 62 + col * 18, 22 + row * 18) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack core) {
                    var proposed = WandCores.read(wand);
                    proposed.set(index, core.copyWithCount(1));
                    return WandCores.accepts(core.copyWithCount(1)) && WandCores.fits(proposed);
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(inventory, 9 + row * 9 + col, 8 + col * 18, 94 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(inventory, col, 8 + col * 18, 152);
        cores.addListener(container -> {
            if (owner.level().isClientSide()) return;
            var items = net.minecraft.core.NonNullList.withSize(WandCores.CAPACITY, ItemStack.EMPTY);
            for (int i = 0; i < items.size(); i++) items.set(i, cores.getItem(i));
            WandCores.write(wand, items);
            inventory.setChanged();
        });
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != lockedInventorySlot; }
            @Override public boolean mayPlace(ItemStack stack) { return index != lockedInventorySlot; }
        });
    }
    public int selected() { return WandCores.selected(wand); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (type == ClickType.SWAP && button == lockedInventorySlot) return;
        if (slot >= 9 && slot < slots.size() && slots.get(slot).getContainerSlot() == lockedInventorySlot) return;
        super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 9) {
            if (!moveItemStackTo(stack, 9, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
    @Override public boolean stillValid(Player player) { return owner == player && owner.getItemInHand(hand) == wand; }
}
