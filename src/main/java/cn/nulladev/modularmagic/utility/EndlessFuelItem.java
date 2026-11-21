package cn.nulladev.modularmagic.utility;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;

public final class EndlessFuelItem extends Item {
    public EndlessFuelItem(){super(new Properties().stacksTo(1));}
    @Override public int getBurnTime(ItemStack stack,RecipeType<?> type){return new ItemStack(Items.WHITE_CARPET).getBurnTime(type);}
    @Override public boolean hasCraftingRemainingItem(ItemStack stack){return true;}
    @Override public ItemStack getCraftingRemainingItem(ItemStack stack){var result=new ItemStack(this);result.applyComponents(stack.getComponentsPatch());return result;}
}
