package cn.nulladev.modularmagic.research;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.*;

/** Real ring ingredients and blank token, a non-consuming center reference and one checked output. */
public final class PlaceholderMenu extends AbstractContainerMenu{
    public final Player owner;
    public final SimpleContainer inputs=new SimpleContainer(9),ghost=new SimpleContainer(1),result=new SimpleContainer(1);
    private PlaceholderRecipe recipe;
    private boolean updating;
    public PlaceholderMenu(int id,Inventory inv,ItemStack held){super(PlaceholderContent.MENU.get(),id);owner=inv.player;
        addSlot(new Slot(inputs,0,18,98){@Override public boolean mayPlace(ItemStack s){return s.is(PlaceholderContent.ITEM.get())&&PlaceholderItem.target(s)==null;}});
        int[][] ring={{62,18},{80,18},{98,18},{62,36},{98,36},{62,54},{80,54},{98,54}};
        for(int i=0;i<8;i++)addSlot(new Slot(inputs,i+1,ring[i][0],ring[i][1]));
        addSlot(new Slot(ghost,0,80,36){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}public boolean isFake(){return true;}});
        addSlot(new Slot(result,0,140,36){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){update();return recipe!=null&&!result.isEmpty();}public void onTake(Player p,ItemStack s){finish();super.onTake(p,s);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+18*col,130+18*row));for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+18*col,188));
        inputs.addListener(c->update());if(held!=null&&!owner.level().isClientSide)inputs.setItem(0,held.split(1));
    }
    private ResourceLocation reference(){var s=ghost.getItem(0);if(s.getItem() instanceof BlockItem b)return BuiltInRegistries.BLOCK.getKey(b.getBlock());if(s.is(Items.WATER_BUCKET))return ResourceLocation.withDefaultNamespace("water");if(s.is(Items.LAVA_BUCKET))return ResourceLocation.withDefaultNamespace("lava");if(s.is(Items.POWDER_SNOW_BUCKET))return ResourceLocation.withDefaultNamespace("powder_snow");if(s.is(Items.FLINT_AND_STEEL))return ResourceLocation.withDefaultNamespace("fire");return null;}
    private CraftingInput ring(){var items=new ArrayList<ItemStack>();for(int i=1;i<=8;i++)items.add(inputs.getItem(i));return CraftingInput.of(4,2,items);}
    private void update(){if(updating||owner.level().isClientSide)return;updating=true;try{recipe=null;var target=reference();if(target!=null&&inputs.getItem(0).is(PlaceholderContent.ITEM.get())&&PlaceholderItem.target(inputs.getItem(0))==null&&ResearchKnowledge.completed(owner,ResearchCatalog.get("K31")))for(var h:owner.level().getRecipeManager().getAllRecipesFor(PlaceholderContent.TYPE.get()))if(h.value().matchesReference(target)&&h.value().matches(ring(),owner.level())){recipe=h.value();break;}result.setItem(0,recipe==null?ItemStack.EMPTY:recipe.assemble(ring(),owner.level().registryAccess()));}finally{updating=false;}broadcastChanges();}
    private void finish(){if(owner.level().isClientSide)return;updating=true;try{inputs.removeItem(0,1);for(int i=1;i<=8;i++){var s=inputs.getItem(i);if(!s.isEmpty()){var remainder=s.getCraftingRemainingItem();s.shrink(1);if(!remainder.isEmpty()){if(s.isEmpty())inputs.setItem(i,remainder);else if(!owner.addItem(remainder))owner.drop(remainder,false);}}}}finally{updating=false;}update();}
    @Override public void clicked(int slot,int button,ClickType type,Player p){if(slot==9){if(!p.level().isClientSide&&type==ClickType.PICKUP){ghost.setItem(0,getCarried().copyWithCount(1));update();}return;}super.clicked(slot,button,type,p);}
    @Override public boolean canDragTo(Slot slot){return slot.index!=9&&slot.index!=10;}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return p==owner&&p.isAlive();}
    @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide)clearContainer(p,inputs);}
}
