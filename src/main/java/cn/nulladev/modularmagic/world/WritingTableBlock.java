package cn.nulladev.modularmagic.world;

import cn.nulladev.modularmagic.menu.WritingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.Containers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.*;

public final class WritingTableBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(0, 12, 0, 16, 16, 16), box(1, 0, 1, 4, 12, 4),
            box(12, 0, 1, 15, 12, 4), box(1, 0, 12, 4, 12, 15), box(12, 0, 12, 15, 12, 15));
    public WritingTableBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WritingTableBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WritingTableBlockEntity table)) return InteractionResult.PASS;
        if (table.inUse()) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("gui.modularmagic.table_busy"), true);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(
                new SimpleMenuProvider((id, inventory, user) -> new WritingMenu(id, inventory, pos),
                        Component.translatable("block.modularmagic.spell_writing_table")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof WritingTableBlockEntity table) {
            Containers.dropContents(level, pos, table.inventory());
            table.inventory().clearContent();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
