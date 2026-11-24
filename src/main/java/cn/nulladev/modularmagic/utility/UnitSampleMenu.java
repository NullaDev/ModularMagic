package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.research.PlaceholderContent;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class UnitSampleMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack device;
    private final SimpleContainer sample;
    private final boolean creative;

    public UnitSampleMenu(int id, Inventory inventory, InteractionHand hand) {
        super(UtilityContent.SAMPLE_MENU.get(), id);
        owner = inventory.player;
        this.hand = hand;
        device = owner.getItemInHand(hand);
        creative = device.is(PlaceholderContent.CREATIVE.get());
        sample = new SimpleContainer(1);
        sample.setItem(0, creative ? PlaceholderItem.material(device) : AetherUtilityItem.sample(device).copy());
        sample.addListener(container -> {
            if (!creative && !owner.level().isClientSide && stillValid(owner)) {
                AetherUtilityItem.setSample(device, sample.getItem(0));
                owner.getInventory().setChanged();
            }
        });
        addSlot(new Slot(sample, 0, 80, 35) {
            @Override public boolean mayPlace(ItemStack stack) {
                return !creative && device.getItem() instanceof AetherUtilityItem item && item.accepts(stack, owner);
            }
            @Override public boolean mayPickup(Player player) { return !creative; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addInventory(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18);
        }
        for (int column = 0; column < 9; column++) addInventory(inventory, column, 8 + column * 18, 142);
    }

    private void addInventory(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return getItem() != device; }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getItemInHand(hand) == device && (!creative || player.isCreative());
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!player.level().isClientSide && !stillValid(player)) return;
        if (slot == 0 && creative && type == ClickType.PICKUP) {
            if (!player.level().isClientSide && PlaceholderItem.fillCreative(device, getCarried())) {
                sample.setItem(0, getCarried().copyWithCount(1));
                owner.getInventory().setChanged();
                broadcastChanges();
            }
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (creative || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
