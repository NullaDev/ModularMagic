package cn.nulladev.modularmagic.spell;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public final class TemporarySpellBlock extends Block {
    private static final VoxelShape BAMBOO_SHAPE = box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape CLAY_SHAPE = box(0, 0, 0, 16, 1, 16);

    public enum Material implements StringRepresentable {
        STONE("stone", Blocks.AIR), GLASS("glass", Blocks.AIR), BAMBOO("bamboo", Blocks.AIR), CLAY("clay", Blocks.AIR),
        HOT_STONE("hot_stone", Blocks.STONE), HOT_DEEPSLATE("hot_deepslate", Blocks.DEEPSLATE),
        HOT_NETHERRACK("hot_netherrack", Blocks.NETHERRACK);

        public static final Codec<Material> CODEC = StringRepresentable.fromEnum(Material::values);
        private final String name;
        private final Block restore;

        Material(String name, Block restore) { this.name = name; this.restore = restore; }
        public Block restore() { return restore; }
        @Override public String getSerializedName() { return name; }
    }

    public static final EnumProperty<Material> MATERIAL = EnumProperty.create("material", Material.class);

    public TemporarySpellBlock() {
        super(Properties.of().strength(1.5F).noOcclusion().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(MATERIAL, Material.STONE));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(MATERIAL); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(MATERIAL)) {
            case BAMBOO -> BAMBOO_SHAPE;
            case CLAY -> CLAY_SHAPE;
            default -> Shapes.block();
        };
    }

    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlockAndUpdate(pos, state.getValue(MATERIAL).restore().defaultBlockState());
    }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (state.getValue(MATERIAL).restore() != Blocks.AIR) Blocks.MAGMA_BLOCK.stepOn(level, pos, state, entity);
        else super.stepOn(level, pos, state, entity);
    }
}
