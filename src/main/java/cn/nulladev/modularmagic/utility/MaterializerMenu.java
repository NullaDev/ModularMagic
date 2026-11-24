package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.energy.EnergyBlockEntity;
import cn.nulladev.modularmagic.research.PlaceholderContent;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MaterializerMenu extends AbstractContainerMenu {
    private final Container device;

    public MaterializerMenu(int id, Inventory inventory, Container device) {
        super(UtilityContent.MATERIALIZER_MENU.get(), id);
        this.device = device;
        addSlot(new Slot(device, 0, 53, 35) {
            @Override public boolean mayPlace(ItemStack stack) {
                return PlaceholderItem.price(stack) >= 0 && !PlaceholderItem.material(stack).isEmpty()
                        && (inventory.player.isCreative() || !stack.is(PlaceholderContent.CREATIVE.get()));
            }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(device, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(device, 2, 73, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.BUCKET); }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    public MaterializerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(3));
    }

    public static void open(Player player, BlockPos pos) {
        if (player instanceof ServerPlayer server && player.level().getBlockEntity(pos) instanceof EnergyBlockEntity device
                && device.allowed(player)) {
            server.openMenu(new SimpleMenuProvider((id, inventory, user) -> new MaterializerMenu(id, inventory, device),
                    Component.translatable("block.modularmagic.aether_materializer")), buffer -> {});
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return device.stillValid(player) && (!(device instanceof EnergyBlockEntity energy) || energy.allowed(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copied = stack.copy();
        boolean moved = index < 3 ? moveItemStackTo(stack, 3, slots.size(), true)
                : stack.is(Items.BUCKET) ? moveItemStackTo(stack, 2, 3, false) : moveItemStackTo(stack, 0, 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copied;
    }
}
