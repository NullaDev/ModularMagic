package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public final class PlaceholderContent{
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,ModularMagic.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,ModularMagic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ModularMagic.MOD_ID);
    public static final net.minecraft.resources.ResourceLocation GRAVITY_TARGET=cn.nulladev.modularmagic.ModularMagic.id("gravity");
    public static final DeferredItem<PlaceholderItem> GRAVITY=ITEMS.register("gravity_placeholder",()->new PlaceholderItem(MimicFamily.GRAVITY,false));
    public static final DeferredItem<PlaceholderItem> RAILS=ITEMS.register("rail_placeholder",()->new PlaceholderItem(MimicFamily.RAILS,false));
    public static final DeferredItem<PlaceholderItem> PLANTS=ITEMS.register("plant_placeholder",()->new PlaceholderItem(MimicFamily.PLANTS,false));
    public static final DeferredItem<PlaceholderItem> ADVANCED_PLANTS=ITEMS.register("advanced_plant_placeholder",()->new PlaceholderItem(MimicFamily.ADVANCED_PLANTS,false));
    public static final DeferredItem<PlaceholderItem> CARPETS=ITEMS.register("carpet_placeholder",()->new PlaceholderItem(MimicFamily.CARPETS,false));
    public static final DeferredItem<PlaceholderItem> CREATIVE=ITEMS.register("creative_aether_sample",()->new PlaceholderItem(null,true));
    public static final DeferredItem<PlaceholderItem> ITEM=ITEMS.register("item_placeholder",PlaceholderItem::new);
    public static final DeferredHolder<RecipeType<?>,RecipeType<PlaceholderRecipe>> TYPE=TYPES.register("placeholder",()->new RecipeType<>(){public String toString(){return "modularmagic:placeholder";}});
    public static final DeferredHolder<RecipeSerializer<?>,PlaceholderRecipe.Serializer> SERIALIZER=SERIALIZERS.register("placeholder",PlaceholderRecipe.Serializer::new);
    public static final DeferredHolder<MenuType<?>,MenuType<PlaceholderMenu>> MENU=MENUS.register("placeholder",()->IMenuTypeExtension.create((id,inv,buf)->new PlaceholderMenu(id,inv,null)));
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COSTS;
    public static final Map<MimicFamily,ModConfigSpec.ConfigValue<String>> FAMILY_COSTS=new EnumMap<>(MimicFamily.class);
    public static final Map<String,ModConfigSpec.ConfigValue<String>> EXTRA_COSTS=new LinkedHashMap<>();
    public static final Map<String,ModConfigSpec.LongValue> ITEM_COSTS=new LinkedHashMap<>();
    public static final ModConfigSpec.LongValue MIN_DERIVATION,CREATIVE_PRICE;
    static {var b=new ModConfigSpec.Builder();COSTS=b.comment("Generation-only fees in displayed aether. The ordinary placement fee is additional.","Each entry is block_id=arithmetic formula. Per-block overrides, with family defaults for grouped mimics. All rails share railGenerationCost and all carpets share carpetGenerationCost. Variables: hardness.")
            .defineListAllowEmpty("generationCosts",List.of("minecraft:water=500","minecraft:lava=15000","minecraft:cobblestone=320","minecraft:stone=480","minecraft:basalt=640","minecraft:snow=400","minecraft:ice=3000","minecraft:fire=160","minecraft:tnt=40000","minecraft:sand=2560","minecraft:gravel=2560"),()->"minecraft:water=500",v->v instanceof String s&&s.contains("="));for(var f:MimicFamily.values())if(f!=MimicFamily.ADVANCED_PLANTS)FAMILY_COSTS.put(f,b.define(f.configKey,f.defaultCost));
        FAMILY_COSTS.put(MimicFamily.ADVANCED_PLANTS,FAMILY_COSTS.get(MimicFamily.PLANTS));
        for(var entry:Map.of("powder_snow","3000","obsidian","16000","amethyst_cluster","10000","pointed_dripstone","1200","sculk","6000","chorus_flower","8000").entrySet())EXTRA_COSTS.put("minecraft:"+entry.getKey(),b.define(entry.getKey()+"GenerationCost",entry.getValue()));
        ITEM_COSTS.put("minecraft:egg",b.defineInRange("eggGenerationCost",300L,0,1_000_000_000L));
        ITEM_COSTS.put("minecraft:iron_ingot",b.defineInRange("ironIngotGenerationCost",2400L,0,1_000_000_000L));
        ITEM_COSTS.put("minecraft:bone_meal",b.defineInRange("boneMealGenerationCost",1000L,0,1_000_000_000L));
        MIN_DERIVATION=b.defineInRange("minimumDerivationCost",16L,0,1_000_000_000L);
        CREATIVE_PRICE=b.defineInRange("creativeSampleGenerationCost",16L,0,1_000_000_000L);
        SPEC=b.build();}
    public static boolean gravity(net.minecraft.world.level.block.Block block){return MimicFamily.GRAVITY.matches(block);}
    public static List<ItemStack> physicalSamples() {
        var samples = new ArrayList<ItemStack>();
        for (String material : List.of("water", "lava", "cobblestone", "stone", "basalt", "snow", "ice",
                "fire", "tnt", "powder_snow", "obsidian", "amethyst_cluster", "pointed_dripstone", "sculk",
                "egg", "iron_ingot", "bone_meal")) {
            samples.add(PlaceholderItem.create(net.minecraft.resources.ResourceLocation.withDefaultNamespace(material)));
        }
        return List.copyOf(samples);
    }
    public static void register(IEventBus b){ITEMS.register(b);SERIALIZERS.register(b);TYPES.register(b);MENUS.register(b);MimicBindingRecipe.register(b);PlantTemplateRecipe.register(b);SampleIngredient.register(b);}
    private PlaceholderContent(){}
}
