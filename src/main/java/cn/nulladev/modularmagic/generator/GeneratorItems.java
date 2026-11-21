package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public final class GeneratorItems {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,ModularMagic.MOD_ID);
    public static final DeferredItem<Item> HOUSING=ITEMS.registerSimpleItem("generator_housing");
    public static final DeferredHolder<RecipeSerializer<?>,SimpleCraftingRecipeSerializer<GeneratorHousingRecipe>> HOUSING_RECIPE=SERIALIZERS.register("generator_housing",()->new SimpleCraftingRecipeSerializer<>(GeneratorHousingRecipe::new));
    public static void register(IEventBus bus){ITEMS.register(bus);SERIALIZERS.register(bus);}
    private GeneratorItems(){}
}
