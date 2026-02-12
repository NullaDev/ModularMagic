package cn.nulladev.modularmagic.content.block;

import cn.nulladev.modularmagic.content.init.registry.ModItems;
import cn.nulladev.modularmagic.content.init.registry.ModRegistry;
import cn.nulladev.modularmagic.content.item.CastableItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MagicCompilerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    private final Container spellItemContainer;
    private final Container spellItemContentContainer;

    public static final int SPELL_ITEM_SLOT = 0;

    public MagicCompilerMenu(int containerId, Inventory playerInv, ContainerLevelAccess access) {
        super(ModRegistry.MAGIC_COMPILER_MENU.get(), containerId);
        this.access = access;

        this.spellItemContainer = new SimpleContainer();
        this.spellItemContentContainer = new SimpleContainer();

        this.addSlot(new SpellItemSlot(spellItemContainer, SPELL_ITEM_SLOT, 0, 0));

        for(int l = 0; l < 3; ++l) {
            for(int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(playerInv, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
            }
        }

        for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(playerInv, l, 8 + l * 18, 142));
        }
    }

    public MagicCompilerMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv, ContainerLevelAccess.NULL);
    }


    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModItems.MAGIC_COMPILER_BLOCK.get());
    }

    private class SpellItemSlot extends Slot {

        public SpellItemSlot(Container inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return stack.getItem() instanceof CastableItem;
        }
    }
}
