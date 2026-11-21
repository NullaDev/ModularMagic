package cn.nulladev.modularmagic.crafting;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.item.ManaContainerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** A shaped upgrade preserves the battery's energy, identity and outstanding spell refunds. */
public record ContainerUpgradeRecipe(ShapedRecipe delegate) implements CraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,ModularMagic.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<ContainerUpgradeRecipe>> SERIALIZER=SERIALIZERS.register("container_upgrade",()->new RecipeSerializer<>(){
        public MapCodec<ContainerUpgradeRecipe> codec(){return RecipeSerializer.SHAPED_RECIPE.codec().xmap(ContainerUpgradeRecipe::new,ContainerUpgradeRecipe::delegate);}
        public StreamCodec<RegistryFriendlyByteBuf,ContainerUpgradeRecipe> streamCodec(){return RecipeSerializer.SHAPED_RECIPE.streamCodec().map(ContainerUpgradeRecipe::new,ContainerUpgradeRecipe::delegate);}
    });
    public static void register(IEventBus bus){SERIALIZERS.register(bus);}
    public boolean matches(CraftingInput input,Level level){return delegate.matches(input,level);}
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){var result=delegate.assemble(input,registries);for(var stack:input.items())if(stack.getItem() instanceof ManaContainerItem){result.applyComponents(stack.getComponentsPatch());break;}return result;}
    public ItemStack getResultItem(HolderLookup.Provider r){return delegate.getResultItem(r);}
    public NonNullList<Ingredient> getIngredients(){return delegate.getIngredients();}
    public boolean canCraftInDimensions(int w,int h){return delegate.canCraftInDimensions(w,h);}
    public CraftingBookCategory category(){return delegate.category();}
    public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
}
