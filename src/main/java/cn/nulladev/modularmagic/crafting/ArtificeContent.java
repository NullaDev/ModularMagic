package cn.nulladev.modularmagic.crafting;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public final class ArtificeContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ModularMagic.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR=DeferredRegister.create(Registries.ARMOR_MATERIAL,ModularMagic.MOD_ID);
    public static final DeferredItem<Item> BLANK_CORE=ITEMS.registerSimpleItem("blank_spell_core");
    public static final DeferredItem<Item> GEM=ITEMS.registerSimpleItem("aether_gem");
    public static final DeferredItem<Item> MATTER=ITEMS.registerSimpleItem("unformed_matter",new Item.Properties().rarity(Rarity.RARE).fireResistant());
    public static final DeferredBlock<Block> GLASS=BLOCKS.register("aether_glass",()->new TransparentBlock(BlockBehaviour.Properties.of().strength(.6f).sound(SoundType.GLASS).noOcclusion().lightLevel(s->3)));
    public static final DeferredItem<BlockItem> GLASS_ITEM=ITEMS.registerSimpleBlockItem(GLASS);
    public static final Map<String,DeferredItem<Item>> PLATES=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<? extends Item>> GEAR=new LinkedHashMap<>();
    public static final Tier TIER=new Tier(){
        public int getUses(){return 2560;}public float getSpeed(){return 10f;}public float getAttackDamageBonus(){return 5f;}
        public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops(){return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;}
        public int getEnchantmentValue(){return 20;}public Ingredient getRepairIngredient(){return Ingredient.of(MATTER.get());}
    };
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> MATERIAL=ARMOR.register("unformed",()->new ArmorMaterial(
        Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,8,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3,ArmorItem.Type.BODY,11),
        20,SoundEvents.ARMOR_EQUIP_NETHERITE,()->Ingredient.of(MATTER.get()),List.of(new ArmorMaterial.Layer(ModularMagic.id("unformed"))),4f,.1f));
    static {
        for(var id:plateIds())PLATES.put(id,ITEMS.registerSimpleItem("rune_plate_"+id.toLowerCase(Locale.ROOT)));
        GEAR.put("sword",ITEMS.register("unformed_sword",()->new SwordItem(TIER,gearProperties().attributes(SwordItem.createAttributes(TIER,3,-2.4f)))));
        GEAR.put("pickaxe",ITEMS.register("unformed_pickaxe",()->new PickaxeItem(TIER,gearProperties().attributes(DiggerItem.createAttributes(TIER,1,-2.8f)))));
        GEAR.put("axe",ITEMS.register("unformed_axe",()->new AxeItem(TIER,gearProperties().attributes(AxeItem.createAttributes(TIER,5,-3f)))));
        GEAR.put("shovel",ITEMS.register("unformed_shovel",()->new ShovelItem(TIER,gearProperties().attributes(ShovelItem.createAttributes(TIER,1.5f,-3f)))));
        GEAR.put("hoe",ITEMS.register("unformed_hoe",()->new HoeItem(TIER,gearProperties().attributes(HoeItem.createAttributes(TIER,-5,0f)))));
        for(var type:List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS))GEAR.put(type.getName(),ITEMS.register("unformed_"+type.getName(),()->new ArmorItem(MATERIAL,type,gearProperties().durability(type.getDurability(45)))));
    }
    private static List<String> plateIds(){try(var in=ArtificeContent.class.getResourceAsStream("/data/modularmagic/rune_plates.json")){if(in==null)throw new IllegalStateException("Missing rune plate catalog");var list=new ArrayList<String>();com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray().forEach(e->list.add(e.getAsString()));return List.copyOf(list);}catch(java.io.IOException ex){throw new ExceptionInInitializerError(ex);}}
    private static Item.Properties gearProperties(){return new Item.Properties().fireResistant().rarity(Rarity.RARE);}
    public static boolean gear(ItemStack stack){return GEAR.values().stream().anyMatch(i->stack.is(i.get()));}
    public static boolean fullArmor(net.minecraft.world.entity.player.Player p){int count=0;for(var s:p.getArmorSlots())if(s.getItem() instanceof ArmorItem&&gear(s))count++;return count==4;}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ARMOR.register(bus);CarvingRecipe.register(bus);ContainerUpgradeRecipe.register(bus);EnchantingMenu.register(bus);}
    private ArtificeContent(){}
}
