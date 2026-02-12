package cn.nulladev.modularmagic.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class MagicCompilerBlock extends Block {

    private static final Component CONTAINER_TITLE = Component.translatable("test");

    public MagicCompilerBlock(Properties p) {
        super(p);
    }

    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState p_52233_, Level p_52234_, @NotNull BlockPos p_52235_, @NotNull Player p_52236_, @NotNull BlockHitResult p_52238_) {
        if (p_52234_.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            p_52236_.openMenu(p_52233_.getMenuProvider(p_52234_, p_52235_));
            return InteractionResult.CONSUME;
        }
    }

    protected MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
        return new SimpleMenuProvider((containerID, playerInv, p_52231_) -> {
            return new MagicCompilerMenu(containerID, playerInv, ContainerLevelAccess.create(level, blockPos));
        }, CONTAINER_TITLE);
    }
}
