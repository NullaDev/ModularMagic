package cn.nulladev.modularmagic.research;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Physical supplies belong to the table; the player's virtual research draft stays personal. */
public final class RuneTableBlockEntity extends BlockEntity {
    public static final int SIZE=10, OUTPUT=9;
    public final SimpleContainer inventory=new SimpleContainer(SIZE);
    private ResearchMenu editor;
    public RuneTableBlockEntity(BlockPos pos,BlockState state){super(ResearchContent.TABLE_ENTITY.get(),pos,state);inventory.addListener(c->setChanged());}
    public boolean inUse(){return editor!=null&&editor.owner.containerMenu==editor&&editor.stillValid(editor.owner);}
    public void acquire(ResearchMenu menu){if(inUse())throw new IllegalStateException("Table already in use");editor=menu;}
    public void release(ResearchMenu menu){if(editor==menu)editor=null;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);var items=NonNullList.withSize(SIZE,net.minecraft.world.item.ItemStack.EMPTY);for(int i=0;i<SIZE;i++)items.set(i,inventory.getItem(i));net.minecraft.world.ContainerHelper.saveAllItems(t,items,r);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);var items=NonNullList.withSize(SIZE,net.minecraft.world.item.ItemStack.EMPTY);net.minecraft.world.ContainerHelper.loadAllItems(t,items,r);for(int i=0;i<SIZE;i++)inventory.setItem(i,items.get(i));}
}
