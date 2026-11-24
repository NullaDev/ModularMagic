package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.energy.EnergyContent;
import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import cn.nulladev.modularmagic.spell.SpellCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KnowledgeCommandGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"KnowledgeTester"));p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);p.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());return p;}
    @GameTest(template="empty",batch="knowledge_commands") public static void creativePlayersCanRelockAndPrepareResearch(GameTestHelper h)throws Exception{
        var p=player(h);var manager=h.getLevel().getServer().getCommands();var source=p.createCommandSourceStack().withPermission(2);
        manager.performPrefixedCommand(source,"mm knowledge grant @s all");h.assertTrue(NodeCatalog.all().stream().allMatch(n->ResearchKnowledge.knows(p,n.id())),"Grant all must store real knowledge even in creative mode");
        manager.performPrefixedCommand(source,"mm knowledge revoke @s K01");h.assertTrue(!ResearchKnowledge.knows(p,"effect/push")&&!ResearchKnowledge.completed(p,ResearchCatalog.get("K01")),"Creative mode must not bypass a relock");
        ResearchKnowledge.save(p,"paid:effect/push","yes");ResearchKnowledge.save(p,"draft:effect/push","bad");manager.performPrefixedCommand(source,"mm knowledge prepare @s K01");h.assertTrue(ResearchKnowledge.ready(p,ResearchCatalog.get("K01"))&&!ResearchKnowledge.completed(p,ResearchCatalog.get("K01")),"Prepare must learn prerequisites and lock the target");h.assertTrue(ResearchKnowledge.state(p).getString("paid:effect/push").isEmpty()&&ResearchKnowledge.state(p).getString("draft:effect/push").isEmpty(),"Prepare clears old target progress and ink payment");
        h.assertTrue(p.getInventory().items.stream().anyMatch(s->s.is(ResearchContent.NOTE.get())),"Prepare gives a physical note for the table");
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,ResearchContent.TABLE.get().defaultBlockState());var menu=new ResearchMenu(11,p.getInventory(),pos,false);menu.supplies.setItem(0,ResearchNoteItem.create("K01",false));h.assertTrue(menu.document!=null&&!menu.reading,"Prepared creative players must actually reach the assembly puzzle");menu.removed(p);
        manager.performPrefixedCommand(source,"modularmagic knowledge reset @s");h.assertTrue(ResearchKnowledge.state(p).isEmpty(),"Reset must clear knowledge, drafts and payments");h.succeed();
    }
    @GameTest(template="empty",batch="knowledge_commands") public static void commandPermissionAndUnknownIdsAreChecked(GameTestHelper h)throws Exception{
        var p=player(h);var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var low=p.createCommandSourceStack().withPermission(0);var parsed=dispatcher.parse("mm knowledge grant @s all",low);h.assertTrue(parsed.getReader().canRead(),"Non-operators must not receive the admin command branch");var source=p.createCommandSourceStack().withPermission(2);boolean rejected=false;try{dispatcher.execute("mm knowledge grant @s no_such_knowledge",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}h.assertTrue(rejected&&ResearchKnowledge.state(p).isEmpty(),"An invalid id must not partially grant knowledge");dispatcher.execute("mm knowledge grant @s effect/push",source);h.assertTrue(ResearchKnowledge.knows(p,"effect/push"),"Node IDs with a slash must work without quoting");h.succeed();
    }

    @GameTest(template="empty",batch="knowledge_commands")
    public static void knowledgeFragmentsUnlockOnlyUnknownTopicsAndStayCraftable(GameTestHelper h) {
        var p = player(h);
        p.setGameMode(GameType.SURVIVAL);
        var stack = ResearchContent.KNOWLEDGE_FRAGMENT.toStack(2);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var input = CraftingInput.of(1, 1, List.of(stack));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.id().equals(ModularMagic.id("knowledge_fragment_recycling")),
                "A fragment should be craftable even before all topics are known");

        var topic = ResearchCatalog.get("K01");
        for (var known : ResearchCatalog.all()) if (!known.equals(topic)) ResearchKnowledge.learn(p, known);
        h.assertTrue(!ResearchKnowledge.completed(p, topic), "Exactly one topic should remain unknown");
        var result = stack.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult().consumesAction() && stack.getCount() == 1 && ResearchKnowledge.completed(p, topic),
                "A fragment should learn the remaining unknown topic and consume one item");

        var before = ResearchKnowledge.state(p).copy();
        result = stack.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult() == InteractionResult.FAIL && stack.getCount() == 1
                        && before.equals(ResearchKnowledge.state(p)),
                "Using a fragment after learning all topics should keep the item and knowledge intact");
        var crafted = recipe.value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(recipe.value().matches(input, h.getLevel())
                        && crafted.is(EnergyContent.CRYSTAL.get()) && crafted.getCount() == 1,
                "The retained fragment should still craft into exactly one crystalline aether fragment");
        h.succeed();
    }

    @GameTest(template="empty",batch="knowledge_commands")
    public static void explorationLootAndCreativeTabOfferFragments(GameTestHelper h) {
        var level = h.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, h.absolutePos(BlockPos.ZERO).getCenter())
                .create(LootContextParamSets.CHEST);
        var random = RandomSource.create(5239L);
        boolean found = false;
        for (int i = 0; i < 40; i++) {
            var loot = table.getRandomItems(params, random.nextLong());
            h.assertTrue(loot.stream().noneMatch(stack -> stack.is(ResearchContent.DIRECT_NOTE.get())),
                    "Exploration loot should no longer drop completed notes");
            found |= loot.stream().anyMatch(stack -> stack.is(ResearchContent.KNOWLEDGE_FRAGMENT.get()));
        }
        h.assertTrue(found, "Dungeon loot should contain knowledge fragments");

        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(ModularMagic.id("main"));
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        h.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(ResearchContent.KNOWLEDGE_FRAGMENT.get()))
                        && tab.getSearchTabDisplayItems().stream().noneMatch(stack -> stack.is(ResearchContent.DIRECT_NOTE.get())),
                "The creative tab should expose fragments without listing completed notes");
        h.succeed();
    }

    @GameTest(template="empty",batch="knowledge_commands")
    public static void treasureTiersDropEditableSpellCores(GameTestHelper h) {
        var level = h.getLevel();
        var params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, h.absolutePos(BlockPos.ZERO).getCenter())
                .create(LootContextParamSets.CHEST);
        var tiers = Map.of(
                BuiltInLootTables.SIMPLE_DUNGEON, Set.of("glimmering.flower", "glimmering.snow", "glimmering.sponge", "glimmering.vine", "magic_arrows"),
                BuiltInLootTables.DESERT_PYRAMID, Set.of("glimmering.amethyst", "glimmering.stone_bridge", "glimmering.flame_dash", "glimmering.icy_flash", "frost_nova"),
                BuiltInLootTables.END_CITY_TREASURE, Set.of("glimmering.meteor", "glimmering.thunderstorm", "glimmering.ocean_shelter", "glimmering.amethyst_penetration", "starfall"));
        for (var tier : tiers.entrySet()) {
            var table = level.getServer().reloadableRegistries().getLootTable(tier.getKey());
            var random = RandomSource.create(43100L);
            boolean found = false;
            for (int seed = 0; seed < 96; seed++) {
                var cores = table.getRandomItems(params, random.nextLong()).stream()
                        .filter(stack -> stack.is(ModContent.FORMED_CORE.get())).toList();
                h.assertTrue(cores.size() <= 1, "A treasure tier should roll at most one spell core");
                for (var core : cores) {
                    found = true;
                    var name = (TranslatableContents) core.getHoverName().getContents();
                    h.assertTrue(tier.getValue().contains(name.getKey().substring("spell.modularmagic.".length())),
                            "Spell belongs in a different treasure tier: " + name.getKey());
                    h.assertTrue(SpellCompiler.compile(SpellCoreItem.read(core), level.registryAccess()).complete(),
                            "A treasure spell must be an editable formed core");
                }
            }
            h.assertTrue(found, "The loot table should yield example spells: " + tier.getKey().location());
        }
        h.succeed();
    }
}
