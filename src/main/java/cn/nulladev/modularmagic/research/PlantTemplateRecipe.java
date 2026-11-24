package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public record PlantTemplateRecipe(boolean separate) implements CraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModularMagic.MOD_ID);
    private static final Supplier<RecipeSerializer<PlantTemplateRecipe>> SERIALIZER =
            SERIALIZERS.register("plant_template", () -> new RecipeSerializer<>() {
                public MapCodec<PlantTemplateRecipe> codec() {
                    return com.mojang.serialization.Codec.BOOL.xmap(PlantTemplateRecipe::new, PlantTemplateRecipe::separate)
                            .fieldOf("separate");
                }

                public StreamCodec<RegistryFriendlyByteBuf, PlantTemplateRecipe> streamCodec() {
                    return StreamCodec.of((buffer, recipe) -> buffer.writeBoolean(recipe.separate),
                            buffer -> new PlantTemplateRecipe(buffer.readBoolean()));
                }
            });
    private static final ResourceLocation BONE_MEAL = ResourceLocation.withDefaultNamespace("bone_meal");

    public static void register(IEventBus bus) { SERIALIZERS.register(bus); }

    public static ItemStack withBoneMeal(ItemStack bone) {
        var advanced = PlaceholderContent.ADVANCED_PLANTS.toStack();
        advanced.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(bone.copyWithCount(1))));
        return advanced;
    }

    public static ItemStack boneMeal(ItemStack advanced) {
        return advanced.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    private static boolean isBoneMealSample(ItemStack stack) {
        return stack.is(PlaceholderContent.ITEM.get()) && BONE_MEAL.equals(PlaceholderItem.target(stack));
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (separate) {
            return input.ingredientCount() == 1 && input.items().stream().anyMatch(stack ->
                    stack.is(PlaceholderContent.ADVANCED_PLANTS.get()) && PlaceholderItem.target(stack) == null
                            && isBoneMealSample(boneMeal(stack)));
        }
        return input.ingredientCount() == 2
                && input.items().stream().anyMatch(stack -> stack.is(PlaceholderContent.PLANTS.get()) && PlaceholderItem.target(stack) == null)
                && input.items().stream().anyMatch(PlantTemplateRecipe::isBoneMealSample);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        if (separate) return PlaceholderContent.PLANTS.toStack();
        return input.items().stream().filter(PlantTemplateRecipe::isBoneMealSample).findFirst()
                .map(PlantTemplateRecipe::withBoneMeal).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var result = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        if (separate) for (int i = 0; i < input.size(); i++) {
            if (input.items().get(i).is(PlaceholderContent.ADVANCED_PLANTS.get())) result.set(i, boneMeal(input.items().get(i)));
        }
        return result;
    }

    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {
        return separate ? PlaceholderContent.PLANTS.toStack() : withBoneMeal(PlaceholderItem.create(BONE_MEAL));
    }
    @Override public NonNullList<Ingredient> getIngredients() {
        return separate ? NonNullList.of(Ingredient.EMPTY, Ingredient.of(PlaceholderContent.ADVANCED_PLANTS.get()))
                : NonNullList.of(Ingredient.EMPTY, Ingredient.of(PlaceholderContent.PLANTS.get()), new SampleIngredient(BONE_MEAL).toVanilla());
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= (separate ? 1 : 2); }
    @Override public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
}
