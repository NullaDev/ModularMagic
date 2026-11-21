package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.crafting.*;
import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.generator.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArtificeGameTests {
    @GameTest(template="empty",batch="artifice") public static void enchantingGrindstoneExperienceLoopCannotCreateAether(GameTestHelper h){
        long yield=GeneratorCatalog.get("experience").yield(0);double worst=0;String name="";
        for(var enchantment:h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().toList()){
            if(enchantment.is(net.minecraft.tags.EnchantmentTags.CURSE))continue;
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->s.isDamageableItem()&&s.supportsEnchantment(enchantment)).findFirst().orElse(ItemStack.EMPTY);if(item.isEmpty())continue;
            for(int level=1;level<=enchantment.value().getMaxLevel();level++){
                var book=enchanted(Items.ENCHANTED_BOOK,enchantment,level);var q=AetherEnchanting.quote(item,book);if(q.result().isEmpty())continue;
                long value=enchantment.value().getMinCost(level);long maximumXp=Math.max(0,value);
                double ratio=maximumXp*yield/(double)q.aether();if(ratio>worst){worst=ratio;name=enchantment.key().location()+" "+level;}
                h.assertTrue(maximumXp*yield<q.aether(),"Enchanting, grinding and reclaiming must lose aether: "+enchantment.key().location()+" "+level);
            }
        }
        ModularMagic.LOGGER.info("AETHER_LOOP_CHECK maxReturnRatio={} enchantment={}",worst,name);h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void containerUpgradesKeepStoredAetherAndReservationIdentity(GameTestHelper h){
        var recipe=(ContainerUpgradeRecipe)h.getLevel().getRecipeManager().byKey(ModularMagic.id("mana_container_expanded")).orElseThrow().value();
        var inputs=new ArrayList<ItemStack>();ItemStack source=ItemStack.EMPTY;
        for(var ingredient:recipe.getIngredients()){var items=ingredient.getItems();var stack=items.length==0?ItemStack.EMPTY:items[0].copy();if(stack.getItem() instanceof cn.nulladev.modularmagic.item.ManaContainerItem){source=stack;((ManaStorage)stack.getItem()).receive(stack,12345,false);stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My Aether Jar"));}inputs.add(stack);}
        var identity=cn.nulladev.modularmagic.spell.billing.ManaEscrow.identity(source);cn.nulladev.modularmagic.item.ManaContainerItem.reserved(source,5000);
        var input=CraftingInput.of(recipe.delegate().getWidth(),recipe.delegate().getHeight(),inputs);h.assertTrue(recipe.matches(input,h.getLevel()),"The upgrade must match its actual shaped inputs");var result=recipe.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(ModContent.MANA_CONTAINERS.get("mana_container_expanded").get())&&((ManaStorage)result.getItem()).stored(result)==12345,"Expansion cannot destroy stored aether");
        h.assertTrue(cn.nulladev.modularmagic.spell.billing.ManaEscrow.identity(result).equals(identity)&&cn.nulladev.modularmagic.item.ManaContainerItem.reserved(result)==5000&&result.getHoverName().getString().equals("My Aether Jar"),"Names and the address for delayed spell refunds survive expansion");
        h.assertTrue(result.getRarity()==Rarity.UNCOMMON,"The result must gain its new tier's rarity");h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArtificeTester"));p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());return p;}
    private static EnergyBlockEntity device(GameTestHelper h,EnergyContent.Kind kind){var pos=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(pos,EnergyContent.BLOCKS.get(kind).get().defaultBlockState());return (EnergyBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static Holder<Enchantment> enchantment(GameTestHelper h,net.minecraft.resources.ResourceKey<Enchantment> key){return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
    private static ItemStack enchanted(Item item,Holder<Enchantment> enchantment,int level){var s=new ItemStack(item);var value=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);value.set(enchantment,level);s.set(item==Items.ENCHANTED_BOOK?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS,value.toImmutable());return s;}

    @GameTest(template="empty",batch="artifice") public static void carvingFiltersByPhysicalBlankAndRequiresKnowledge(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ResearchContent.CARVER.get().defaultBlockState());var m=new ResearchMenu(10,p.getInventory(),pos,true);
        h.assertTrue(m.palette.isEmpty(),"An empty carving desk must not list any recipe");m.supplies.setItem(0,ResearchContent.BLANK.toStack());h.assertTrue(m.palette.isEmpty(),"Unknown patterns cannot be carved");
        NodeCatalog.all().forEach(n->ResearchKnowledge.learn(p,n.id()));ResearchCatalog.all().forEach(t->ResearchKnowledge.learn(p,t.id()));m.refreshKnowledge();
        h.assertTrue(m.palette.size()==97&&m.palette.keySet().stream().noneMatch(s->s.startsWith("root/")),"A blank node lists the 97 non-root patterns");
        m.supplies.setItem(0,ArtificeContent.BLANK_CORE.toStack());h.assertTrue(m.palette.size()==3&&m.palette.keySet().stream().allMatch(s->s.startsWith("root/")),"A blank core lists only the three roots");
        m.supplies.setItem(0,new ItemStack(Items.CLAY_BALL,2));h.assertTrue(m.palette.size()==ArtificeContent.PLATES.size()&&m.palette.keySet().stream().allMatch(s->s.startsWith("plate:")),"Clay lists knowledge rune plates");
        m.apply(m.revision,"{\"op\":\"carve\",\"id\":\"plate:K32\"}");h.assertTrue(m.supplies.getItem(9).is(ArtificeContent.PLATES.get("K32").get())&&m.supplies.getItem(0).getCount()==1,"One clay ball makes one experience plate without auxiliary materials");m.removed(p);h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void materialsAndEveryGeneratorHaveVisibleRecipes(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager();for(String id:List.of("aether_glass","aether_gem","unformed_matter","aether_enchanter","blank_spell_core"))h.assertTrue(recipes.byKey(ModularMagic.id(id)).isPresent(),"Missing standard recipe "+id);
        var glass=(CraftingRecipe)recipes.byKey(ModularMagic.id("aether_glass")).orElseThrow().value();
        for(var fragment:List.of(EnergyContent.SHARD.toStack(),EnergyContent.CRYSTAL.toStack())){var input=CraftingInput.of(3,2,List.of(fragment,new ItemStack(Items.GLASS),new ItemStack(Items.GLASS),new ItemStack(Items.GLASS),new ItemStack(Items.GLASS),ItemStack.EMPTY));h.assertTrue(glass.matches(input,h.getLevel())&&glass.assemble(input,h.getLevel().registryAccess()).getCount()==4,"Either aether fragment makes four resilient glass blocks");}
        var gem=(CraftingRecipe)recipes.byKey(ModularMagic.id("aether_gem")).orElseThrow().value();for(var mineral:List.of(Items.DIAMOND,Items.EMERALD))h.assertTrue(gem.matches(CraftingInput.of(2,1,List.of(EnergyContent.SHARD.toStack(),new ItemStack(mineral))),h.getLevel()),"Either gemstone can hold aether");
        h.assertTrue(CarvingRecipe.all(h.getLevel()).size()==100+ArtificeContent.PLATES.size(),"Every node and knowledge plate needs a data-driven carving recipe");
        for(var r:GeneratorCatalog.RULES.values()){var recipe=recipes.byKey(ModularMagic.id("generator_"+r.id())).orElseThrow().value();h.assertTrue(recipe.getIngredients().stream().anyMatch(i->i.test(ArtificeContent.PLATES.get("K24").toStack()))&&recipe.getIngredients().stream().filter(i->ArtificeContent.PLATES.values().stream().anyMatch(p->i.test(p.toStack()))).count()>=2,"Every generator combines the reversal rule with basic rune plates");}
        h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void enchantmentLevelsAddWithoutConsumingTheLibrary(GameTestHelper h){
        var p=player(h);var be=device(h,EnergyContent.Kind.ENCHANTER);var sharp=enchantment(h,Enchantments.SHARPNESS);
        be.enchantInventory.setItem(0,enchanted(Items.DIAMOND_SWORD,sharp,1));var sample=enchanted(Items.ENCHANTED_BOOK,sharp,2);be.enchantInventory.setItem(1,sample);var before=sample.copy();
        var menu=new EnchantingMenu(5,p.getInventory(),be.getBlockPos());menu.clickMenuButton(p,1);var q=menu.quote();h.assertTrue(q.changed()==1&&AetherEnchanting.enchantments(q.result()).getLevel(sharp)==3,"One plus two must become three");
        be.energy=q.aether()*ManaStorage.SCALE-1;h.assertTrue(!menu.clickMenuButton(p,100)&&AetherEnchanting.enchantments(be.enchantInventory.getItem(0)).getLevel(sharp)==1,"Insufficient aether leaves equipment unchanged");
        be.energy=q.aether()*ManaStorage.SCALE;h.assertTrue(menu.clickMenuButton(p,100)&&be.energy==0&&AetherEnchanting.enchantments(be.enchantInventory.getItem(0)).getLevel(sharp)==3,"Only the quoted amount is consumed");
        h.assertTrue(ItemStack.isSameItemSameComponents(before,be.enchantInventory.getItem(1)),"The exact book, including components, must remain");
        var capped=AetherEnchanting.quote(enchanted(Items.DIAMOND_SWORD,sharp,4),sample);h.assertTrue(AetherEnchanting.enchantments(capped.result()).getLevel(sharp)==5,"Addition respects the enchantment's normal maximum");h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void enchantmentsRespectConflictsAndTheFortyLevelBarrierIsAbsent(GameTestHelper h){
        var sharp=enchantment(h,Enchantments.SHARPNESS);var smite=enchantment(h,Enchantments.SMITE);
        var source=enchanted(Items.DIAMOND_AXE,smite,2);h.assertTrue(AetherEnchanting.quote(enchanted(Items.DIAMOND_SWORD,sharp,1),source).result().isEmpty(),"Incompatible enchantments cannot erase existing patterns");
        h.assertTrue(AetherEnchanting.quote(new ItemStack(Items.DIAMOND_BOOTS),enchanted(Items.ENCHANTED_BOOK,sharp,1)).result().isEmpty(),"Unsupported equipment cannot receive an enchantment");
        var target=new ItemStack(Items.DIAMOND_SWORD);target.set(DataComponents.REPAIR_COST,63);var q=AetherEnchanting.quote(target,enchanted(Items.ENCHANTED_BOOK,sharp,1));h.assertTrue(!q.result().isEmpty()&&q.aether()>40*ArtificeConfig.AETHER_PER_ANVIL_LEVEL.get(),"A quote above forty levels remains valid");h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void enchanterPersistsEveryLibrarySlotAndAcceptsNetworkInput(GameTestHelper h){
        var be=device(h,EnergyContent.Kind.ENCHANTER);var sample=enchanted(Items.ENCHANTED_BOOK,enchantment(h,Enchantments.UNBREAKING),2);be.enchantInventory.setItem(27,sample);be.enchantInventory.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(be.receive(1000*ManaStorage.SCALE,false)==1000*ManaStorage.SCALE,"The table receives energy through the shared network interface");var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());be.enchantInventory.clearContent();be.energy=0;be.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(be.enchantInventory.getItem(27).is(Items.ENCHANTED_BOOK)&&be.enchantInventory.getItem(1).isEmpty()&&be.energy==1000*ManaStorage.SCALE,"Save and load must preserve sparse chest indices and aether");h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void unformedGearRepairsOnlyBySpendingItemAether(GameTestHelper h){
        var p=player(h);var sword=ArtificeContent.GEAR.get("sword").toStack();sword.setDamageValue(10);p.getInventory().setItem(0,sword);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(1,battery);
        ArtificeEquipment.repair(p);h.assertTrue(sword.getDamageValue()==10,"An empty container cannot repair gear");((ManaStorage)battery.getItem()).receive(battery,1000,false);ArtificeEquipment.repair(p);
        h.assertTrue(sword.getDamageValue()==8&&((ManaStorage)battery.getItem()).stored(battery)==800,"Repair pays configured aether per durability point");
        h.assertTrue(ArtificeContent.TIER.getUses()>Tiers.NETHERITE.getUses()&&ArtificeContent.TIER.getSpeed()>Tiers.NETHERITE.getSpeed()&&ArtificeContent.MATERIAL.value().toughness()>ArmorMaterials.NETHERITE.value().toughness(),"Stats improve modestly over netherite");
        for(var pair:Map.of(EquipmentSlot.HEAD,"helmet",EquipmentSlot.CHEST,"chestplate",EquipmentSlot.LEGS,"leggings",EquipmentSlot.FEET,"boots").entrySet())p.setItemSlot(pair.getKey(),ArtificeContent.GEAR.get(pair.getValue()).toStack());h.assertTrue(ArtificeContent.fullArmor(p),"Only the full set grants the mana discount");p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);h.assertTrue(!ArtificeContent.fullArmor(p),"A partial suit must lose the discount");h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void researchOpensOnlyWithUnresolvedNotes(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(pos,ResearchContent.TABLE.get().defaultBlockState());var previous=p.containerMenu;
        ResearchContent.open(p,pos,false);h.assertTrue(p.containerMenu==previous,"Empty hands cannot open the research desk");ResearchCatalog.get("K00").nodes().forEach(n->ResearchKnowledge.learn(p,n));
        p.setItemInHand(InteractionHand.MAIN_HAND,ResearchNoteItem.create("K01",true));ResearchContent.open(p,pos,false);h.assertTrue(p.containerMenu==previous,"Decoded notes cannot open a research puzzle");
        p.setItemInHand(InteractionHand.MAIN_HAND,ResearchNoteItem.create("K01",false));ResearchContent.open(p,pos,false);h.assertTrue(p.containerMenu==previous&&p.getMainHandItem().is(ResearchContent.NOTE.get())&&((RuneTableBlockEntity)h.getLevel().getBlockEntity(pos)).inventory.getItem(0).isEmpty(),"Fake players decline GUI opening; that must return the unresolved note without loss");h.succeed();
    }
    @GameTest(template="empty",batch="artifice") public static void experienceGeneratorConsumesOnlyAvailableExperience(GameTestHelper h){
        var p=player(h);h.getLevel().addNewPlayer(p);p.giveExperiencePoints(100);var be=device(h,EnergyContent.Kind.GENERATOR);be.generator.ruleId="experience";be.generator.cooldown=0;
        be.generator.tick();h.assertTrue(p.totalExperience==36&&be.energy==64*20*ManaStorage.SCALE,"Sixty-four real experience points become configured aether");
        int before=p.totalExperience;be.energy=be.capacity();be.generator.cooldown=0;be.generator.tick();h.assertTrue(p.totalExperience==before,"A full buffer leaves the player's experience alone");p.discard();h.succeed();
    }
}
