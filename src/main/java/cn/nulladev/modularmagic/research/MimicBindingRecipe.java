package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public record MimicBindingRecipe(MimicFamily family, boolean separate) implements CraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ModularMagic.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MimicBindingRecipe>> SERIALIZER =
            SERIALIZERS.register("mimic_binding", () -> new RecipeSerializer<>() {
                public MapCodec<MimicBindingRecipe> codec() {
                    return RecordCodecBuilder.mapCodec(builder -> builder.group(
                            com.mojang.serialization.Codec.STRING.xmap(MimicFamily::parse, family -> family.id)
                                    .fieldOf("family").forGetter(MimicBindingRecipe::family),
                            com.mojang.serialization.Codec.BOOL.fieldOf("separate").forGetter(MimicBindingRecipe::separate)
                    ).apply(builder, MimicBindingRecipe::new));
                }

                public StreamCodec<RegistryFriendlyByteBuf, MimicBindingRecipe> streamCodec() {
                    return new StreamCodec<>() {
                        public MimicBindingRecipe decode(RegistryFriendlyByteBuf buffer) {
                            return new MimicBindingRecipe(buffer.readEnum(MimicFamily.class), buffer.readBoolean());
                        }

                        public void encode(RegistryFriendlyByteBuf buffer, MimicBindingRecipe recipe) {
                            buffer.writeEnum(recipe.family);
                            buffer.writeBoolean(recipe.separate);
                        }
                    };
                }
            });

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    public static boolean material(ItemStack stack) {
        for (var family : MimicFamily.values()) if (sample(family, stack)) return true;
        return false;
    }

    public static boolean sample(MimicFamily family, ItemStack stack) {
        if (!PlaceholderItem.recordable(stack)) return false;
        return (stack.getItem() instanceof BlockItem block && family.matches(block.getBlock()))
                || (family == MimicFamily.PLANTS && (stack.is(Items.WHEAT) || stack.is(Items.BEETROOT)));
    }

    public static boolean bound(ItemStack stack) {
        if (PlaceholderItem.target(stack) == null) return false;
        for (var family : MimicFamily.values()) if (stack.is(family.template().getItem())) return true;
        return false;
    }

    public boolean matches(CraftingInput input, Level level) {
        var template = templateInput(input);
        if (template.isEmpty()) return false;
        if (separate) return input.ingredientCount() == 1 && PlaceholderItem.target(template) != null;
        return input.ingredientCount() == 2 && PlaceholderItem.target(template) == null
                && (family != MimicFamily.ADVANCED_PLANTS || !PlantTemplateRecipe.boneMeal(template).isEmpty())
                && !reference(input).isEmpty();
    }

    private ItemStack templateInput(CraftingInput input) {
        Item template = family.template().getItem();
        return input.items().stream().filter(stack -> stack.is(template)).findFirst().orElse(ItemStack.EMPTY);
    }

    private ItemStack reference(CraftingInput input) {
        return input.items().stream().filter(stack -> sample(family, stack)).findFirst().orElse(ItemStack.EMPTY);
    }

    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return separate ? PlaceholderItem.unbind(templateInput(input)) : PlaceholderItem.bind(templateInput(input), reference(input));
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remaining = CraftingRecipe.super.getRemainingItems(input);
        if (separate) for (int i = 0; i < input.size(); i++) {
            if (bound(input.items().get(i))) remaining.set(i, PlaceholderItem.material(input.items().get(i)));
        }
        return remaining;
    }

    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return separate ? family.template() : PlaceholderItem.bind(family, new ItemStack(family.example));
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= (separate ? 1 : 2);
    }

    public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    public boolean isSpecial() { return true; }
    public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
}
