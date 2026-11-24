package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.stream.Stream;

public record SampleIngredient(ResourceLocation target) implements ICustomIngredient {
    private static final DeferredRegister<IngredientType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ModularMagic.MOD_ID);
    private static final java.util.function.Supplier<IngredientType<SampleIngredient>> TYPE =
            TYPES.register("sample", () -> new IngredientType<>(
                    ResourceLocation.CODEC.xmap(SampleIngredient::new, SampleIngredient::target).fieldOf("target")));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override public boolean test(ItemStack stack) {
        return stack != null && !stack.is(PlaceholderContent.CREATIVE.get()) && target.equals(PlaceholderItem.target(stack));
    }

    @Override public Stream<ItemStack> getItems() {
        return Stream.of(PlaceholderItem.create(target));
    }

    @Override public boolean isSimple() {
        return false;
    }

    @Override public IngredientType<?> getType() {
        return TYPE.get();
    }
}
