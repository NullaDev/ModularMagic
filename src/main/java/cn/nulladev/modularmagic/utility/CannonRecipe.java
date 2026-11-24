package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public record CannonRecipe(ShapelessRecipe delegate) implements CraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModularMagic.MOD_ID);
    private static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CannonRecipe>> SERIALIZER =
            SERIALIZERS.register("cannon_upgrade", () -> new RecipeSerializer<>() {
                @Override public MapCodec<CannonRecipe> codec() {
                    return RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(CannonRecipe::new, CannonRecipe::delegate);
                }
                @Override public StreamCodec<RegistryFriendlyByteBuf, CannonRecipe> streamCodec() {
                    return RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(CannonRecipe::new, CannonRecipe::delegate);
                }
            });

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }
    @Override public boolean matches(CraftingInput input, Level level) { return delegate.matches(input, level); }
    @Override public boolean canCraftInDimensions(int width, int height) { return delegate.canCraftInDimensions(width, height); }
    @Override public CraftingBookCategory category() { return delegate.category(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return delegate.getResultItem(registries); }
    @Override public NonNullList<Ingredient> getIngredients() { return delegate.getIngredients(); }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var output = delegate.assemble(input, registries);
        long balance = 0;
        ItemStack sample = ItemStack.EMPTY;
        for (var stack : input.items()) {
            if (stack.is(UtilityContent.LAUNCH.get())) balance = UtilityContent.LAUNCH.get().stored(stack);
            else if (PlaceholderItem.target(stack) != null) sample = stack;
        }
        AetherUtilityItem.setSample(output, sample);
        UtilityContent.CANNON.get().receive(output, balance, false);
        return output;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = CraftingRecipe.super.getRemainingItems(input);
        for (int index = 0; index < input.size(); index++) {
            var stack = input.items().get(index);
            if (stack.is(UtilityContent.LAUNCH.get())) remaining.set(index, AetherUtilityItem.sample(stack).copy());
        }
        return remaining;
    }
}
