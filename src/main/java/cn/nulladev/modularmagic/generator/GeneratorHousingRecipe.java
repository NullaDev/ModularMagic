package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.mana.ManaStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** The storage component is built into the casing and must be empty. */
public final class GeneratorHousingRecipe extends CustomRecipe {
    public GeneratorHousingRecipe(CraftingBookCategory category){super(category);}
    @Override public boolean matches(CraftingInput input,Level level){int copper=0,quartz=0,storage=0;for(int i=0;i<input.size();i++){var s=input.getItem(i);if(s.isEmpty())continue;if(s.is(Items.COPPER_INGOT))copper++;else if(s.is(Items.QUARTZ))quartz++;else if(s.is(ModContent.MANA_CONTAINERS.get("mana_container_basic").get())&&((ManaStorage)s.getItem()).storedUnits(s)==0&&cn.nulladev.modularmagic.item.ManaContainerItem.reserved(s)==0)storage++;else return false;}return copper==3&&quartz==1&&storage==1;}
    @Override public ItemStack assemble(CraftingInput i,HolderLookup.Provider p){return GeneratorItems.HOUSING.toStack();}
    @Override public ItemStack getResultItem(HolderLookup.Provider p){return GeneratorItems.HOUSING.toStack();}
    @Override public net.minecraft.core.NonNullList<Ingredient> getIngredients(){return net.minecraft.core.NonNullList.of(Ingredient.EMPTY,Ingredient.of(Items.COPPER_INGOT),Ingredient.of(Items.COPPER_INGOT),Ingredient.of(Items.COPPER_INGOT),Ingredient.of(Items.QUARTZ),Ingredient.of(ModContent.MANA_CONTAINERS.get("mana_container_basic").get()));}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=5;}
    @Override public RecipeSerializer<?> getSerializer(){return GeneratorItems.HOUSING_RECIPE.get();}
}
