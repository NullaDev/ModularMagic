package cn.nulladev.modularmagic.world;

import cn.nulladev.modularmagic.menu.WritingMenu;
import cn.nulladev.modularmagic.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The table owns its core across menu closes, chunk unloads and world saves. */
public final class WritingTableBlockEntity extends BlockEntity {
    private final SimpleContainer inventory = new SimpleContainer(1) {
        @Override public int getMaxStackSize() { return 1; }
    };
    private WritingMenu editor;

    public WritingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.WRITING_TABLE_ENTITY.get(), pos, state);
        inventory.addListener(container -> setChanged());
    }

    public SimpleContainer inventory() { return inventory; }
    public boolean inUse() { return editor != null && editor.isOpenForOwner(); }
    public void acquire(WritingMenu menu) {
        if (inUse()) throw new IllegalStateException("Writing table is already in use");
        editor = menu;
    }
    public void release(WritingMenu menu) { if (editor == menu) editor = null; }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.createTag(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.clearContent();
        inventory.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registries);
    }
}
