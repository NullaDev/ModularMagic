package cn.nulladev.modularmagic.utility;

import net.minecraft.core.NonNullList;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

public final class AutoCrafterMenu extends AbstractContainerMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack tool;
    private final int locked;
    public final SimpleContainer pattern=new SimpleContainer(9),preview=new SimpleContainer(1);
    public AutoCrafterMenu(int id,Inventory inv,InteractionHand hand){
        super(UtilityContent.MENU.get(),id);owner=inv.player;this.hand=hand;tool=owner.getItemInHand(hand);locked=hand==InteractionHand.MAIN_HAND?inv.selected:40;
        if(!tool.is(UtilityContent.CRAFTER.get()))throw new IllegalStateException("Missing auto crafter");
        var data=tool.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY);for(int i=0;i<9;i++)pattern.setItem(i,data.samples().get(i).copy());
        for(int i=0;i<9;i++)addSlot(new Slot(pattern,i,26+i%3*18,22+i/3*18){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}public boolean isFake(){return true;}});
        addSlot(new Slot(preview,0,124,40){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}public boolean isFake(){return true;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)playerSlot(inv,9+row*9+col,8+col*18,116+row*18);
        for(int col=0;col<9;col++)playerSlot(inv,col,8+col*18,174);
        pattern.addListener(c->update(true));update(false);
    }
    private void playerSlot(Inventory inv,int index,int x,int y){addSlot(new Slot(inv,index,x,y){public boolean mayPickup(Player p){return index!=locked;}public boolean mayPlace(ItemStack stack){return index!=locked;}});}
    private void update(boolean changed){
        if(owner.level().isClientSide)return;
        var samples=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<9;i++)samples.set(i,pattern.getItem(i).copy());
        var input=CraftingInput.of(3,3,samples);var recipe=owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,owner.level()).orElse(null);
        preview.setItem(0,recipe==null?ItemStack.EMPTY:recipe.value().assemble(input,owner.registryAccess()));
        boolean active=!changed&&tool.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY).enabled();
        tool.set(UtilityContent.PATTERN.get(),new CrafterPattern(samples,active,recipe==null?"":recipe.id().toString()));owner.getInventory().setChanged();broadcastChanges();
    }
    public boolean enabled(){return owner.getItemInHand(hand).getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY).enabled();}
    @Override public boolean clickMenuButton(Player p,int button){if(!stillValid(p)||button!=0||preview.isEmpty())return false;var state=tool.getOrDefault(UtilityContent.PATTERN.get(),CrafterPattern.EMPTY);tool.set(UtilityContent.PATTERN.get(),state.active(!state.enabled()));owner.getInventory().setChanged();broadcastChanges();return true;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!stillValid(p))return;
        if(slot>=0&&slot<9){
            if(type==ClickType.PICKUP)pattern.setItem(slot,button==1?ItemStack.EMPTY:getCarried().copyWithCount(1));
            else if(type==ClickType.SWAP&&button>=0&&button<9)pattern.setItem(slot,owner.getInventory().getItem(button).copyWithCount(1));
            else if(type==ClickType.QUICK_MOVE)pattern.setItem(slot,ItemStack.EMPTY);
            return;
        }
        if(slot==9||type==ClickType.SWAP&&button==locked||slot>=10&&slot<slots.size()&&slots.get(slot).getContainerSlot()==locked)return;
        super.clicked(slot,button,type,p);
    }
    @Override public boolean canDragTo(Slot slot){return slot.index>=10&&slot.getContainerSlot()!=locked;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<10||index>=slots.size()||!slots.get(index).mayPickup(p))return ItemStack.EMPTY;
        var stack=slots.get(index).getItem();if(stack.isEmpty())return ItemStack.EMPTY;
        for(int i=0;i<9;i++)if(pattern.getItem(i).isEmpty()){pattern.setItem(i,stack.copyWithCount(1));break;}return ItemStack.EMPTY;
    }
    @Override public boolean stillValid(Player p){return p==owner&&owner.getItemInHand(hand)==tool;}
}
