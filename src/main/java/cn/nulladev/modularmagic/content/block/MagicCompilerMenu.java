package cn.nulladev.modularmagic.content.block;

import cn.nulladev.modularmagic.content.init.registry.ModItems;
import cn.nulladev.modularmagic.content.init.registry.ModRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MagicCompilerMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public static final int RESULT_SLOT = 0;


    public MagicCompilerMenu(int containerId, Inventory playerInv, ContainerLevelAccess access) {
        super(ModRegistry.MAGIC_COMPILER_MENU.get(), containerId);
        this.access = access;
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
}
