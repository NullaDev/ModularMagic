package cn.nulladev.modularmagic.energy;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.List;

public final class EnergyBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static final VoxelShape TANK_SHAPE = Shapes.or(box(0,0,0,16,2,16), box(0,2,0,2,16,16),
            box(14,2,0,16,16,16), box(2,2,0,14,16,2), box(2,2,14,14,16,16));
    public final EnergyContent.Kind kind;
    public EnergyBlock(EnergyContent.Kind kind, Properties properties) {
        super(properties); this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind.tank() ? TANK_SHAPE : Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EnergyBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyContent.ENTITY.get() ? (l,p,s,be) -> ((EnergyBlockEntity)be).tick() : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof EnergyBlockEntity be) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains("AetherDevice")) be.restoreItem(tag.getCompound("AetherDevice"));
            if (placer != null) be.owner = placer.getUUID();
            if(kind==EnergyContent.Kind.GENERATOR)be.generator.placed();
            be.changed();
        }
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(kind==EnergyContent.Kind.ENCHANTER&&!(stack.getItem() instanceof EnergyLinkItem)){cn.nulladev.modularmagic.crafting.EnchantingMenu.open(player,pos);return ItemInteractionResult.SUCCESS;}
        if (stack.getItem() instanceof EnergyLinkItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if(kind==EnergyContent.Kind.MATERIALIZER){cn.nulladev.modularmagic.utility.MaterializerMenu.open(player,pos);return ItemInteractionResult.SUCCESS;}
        if (!(level.getBlockEntity(pos) instanceof EnergyBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && be.interact(player, hand)) return ItemInteractionResult.SUCCESS;
        if (level.isClientSide && (stack.getItem() instanceof cn.nulladev.modularmagic.mana.ManaStorage || stack.getItem() instanceof AetherShardItem || stack.is(EnergyContent.CROSS_LENS.get()) || stack.is(Items.AMETHYST_SHARD)&&kind==EnergyContent.Kind.RESONATOR)) return ItemInteractionResult.SUCCESS;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if(kind==EnergyContent.Kind.ENCHANTER){cn.nulladev.modularmagic.crafting.EnchantingMenu.open(player,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
        if(kind==EnergyContent.Kind.MATERIALIZER){cn.nulladev.modularmagic.utility.MaterializerMenu.open(player,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyBlockEntity be) be.interact(player, InteractionHand.MAIN_HAND);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EnergyBlockEntity be ? (kind==EnergyContent.Kind.GENERATOR?be.generator.comparator():(int)Math.min(15, be.energy * 15 / Math.max(1, be.capacity()))) : 0;
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EnergyBlockEntity be) return List.of(be.asItem());
        return List.of(new ItemStack(this));
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof EnergyBlockEntity be) {
            if(!level.isClientSide&&kind==EnergyContent.Kind.GENERATOR)cn.nulladev.modularmagic.generator.CuringGenerator.release(be.generator);
            if(kind==EnergyContent.Kind.ENCHANTER)Containers.dropContents(level,pos,be.enchantInventory);
            be.cancelPulse(); EnergyWorld.release(be);
            if (!be.battery.isEmpty()) Containers.dropItemStack(level, pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,be.battery);
            be.battery = ItemStack.EMPTY;
            if(!be.product.isEmpty())Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,be.product);
            be.product=ItemStack.EMPTY;
            if(!be.emptyBuckets.isEmpty())Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,be.emptyBuckets);
            be.emptyBuckets=ItemStack.EMPTY;
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
