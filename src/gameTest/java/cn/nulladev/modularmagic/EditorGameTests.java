package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.menu.WritingMenu;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EditorGameTests {
    @GameTest(template = "empty")
    public static void tablePersistsDraftAndDropsItOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, ModContent.WRITING_TABLE.get().defaultBlockState());
        var table = (cn.nulladev.modularmagic.world.WritingTableBlockEntity) level.getBlockEntity(pos);
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "TableTester"));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        var menu = new WritingMenu(41, player.getInventory(), pos);
        player.containerMenu = menu;
        menu.getSlot(0).set(new ItemStack(ModContent.NODES.get("root/instant").get()));
        var draft = menu.document().toJson();
        helper.assertTrue(table.inUse(), "An opened table must lock out concurrent sessions");
        boolean blocked = false;
        try { new WritingMenu(42, player.getInventory(), pos); } catch (IllegalStateException expected) { blocked = true; }
        helper.assertTrue(blocked, "A second editor must not access the same physical core");
        menu.removed(player); player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!table.inUse(), "Closing must release the table");
        helper.assertTrue(player.getInventory().isEmpty(), "Closing must not return the core to the player's inventory");
        helper.assertTrue(SpellCoreItem.read(table.inventory().getItem(0)).toJson().equals(draft), "Draft must remain inside the table");
        var tag = table.saveWithFullMetadata(level.registryAccess());
        var restored = (cn.nulladev.modularmagic.world.WritingTableBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                pos, table.getBlockState(), tag, level.registryAccess());
        helper.assertTrue(SpellCoreItem.read(restored.inventory().getItem(0)).toJson().equals(draft), "World NBT must retain the incomplete draft");
        level.removeBlockEntity(pos); level.setBlockEntity(restored);
        var reopened = new WritingMenu(43, player.getInventory(), pos);
        helper.assertTrue(reopened.document().toJson().equals(draft), "Reopening after a block entity reload must restore the draft");
        reopened.removed(player);
        helper.assertTrue(!table.getBlockState().canOcclude(), "Table must not hide the ground as a full opaque cube");
        helper.assertTrue(net.minecraft.world.level.block.Block.shouldRenderFace(
                level.getBlockState(pos.below()), level, pos.below(), net.minecraft.core.Direction.UP, pos),
                "The ground's upward face must render below the table legs");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1));
        helper.assertTrue(drops.stream().filter(e -> e.getItem().is(ModContent.NODES.get("root/instant").get())).mapToInt(e -> e.getItem().getCount()).sum() == 1,
                "Breaking a table must drop its one stored core exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void emptyCoresKeepTheirTypeAndParameters(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, ModContent.WRITING_TABLE.get().defaultBlockState());
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "EmptyCoreTester"));
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        var menu = new WritingMenu(45, player.getInventory(), pos);
        player.containerMenu = menu;
        try {
            for (var type : java.util.List.of("instant", "charge", "continuous")) {
                var item = ModContent.NODES.get("root/" + type).get();
                menu.setCarried(ItemStack.EMPTY);
                menu.getSlot(0).set(new ItemStack(item));
                helper.assertTrue(menu.getSlot(0).getItem().is(item), "Inserting an empty core must retain its item type");
                var id = menu.document().root.id;
                var remove = command("remove"); remove.addProperty("node", id);
                long revision = menu.revision();
                menu.apply(revision, remove.toString());
                helper.assertTrue(menu.getCarried().isEmpty() && menu.revision() == revision, "Root removal must be rejected server-side");
                var apply = command("apply"); var edits = new JsonObject(); var params = new JsonObject();
                params.addProperty("icon", "minecraft:diamond"); params.addProperty("order", "42");
                edits.add(id, params); apply.add("edits", edits);
                menu.apply(menu.revision(), apply.toString());
                menu.apply(menu.revision(), command("take").toString());
                var empty = menu.getCarried().copy();
                helper.assertTrue(empty.is(item), "Taking an empty structure must return the original core: " + type);
                var document = SpellCoreItem.read(empty);
                helper.assertTrue(document.root.id.equals(id) && document.root.parameters.get("icon").equals("minecraft:diamond")
                        && document.root.parameters.get("order").equals("42"), "Empty core parameters and identity must survive extraction");
                menu.setCarried(ItemStack.EMPTY); menu.getSlot(0).set(empty);
                menu.setCarried(new ItemStack(ModContent.NODES.get("trigger/self_pos").get()));
                var insert = command("insert"); insert.addProperty("parent", id); insert.addProperty("field", "trigger_type"); insert.addProperty("index", 0);
                menu.apply(menu.revision(), insert.toString());
                helper.assertTrue(menu.getSlot(0).getItem().is(ModContent.UNFORMED_CORE.get()), "Nonempty incomplete assemblies must retain their gems in an unformed core");
                remove.addProperty("node", menu.document().root.children.get("trigger_type").getFirst().id);
                menu.apply(menu.revision(), remove.toString());
                helper.assertTrue(menu.getSlot(0).getItem().is(item), "Removing the last child must restore the typed empty core");
                menu.setCarried(ItemStack.EMPTY); menu.getSlot(0).set(ItemStack.EMPTY);
            }
        } finally { menu.removed(player); player.containerMenu = player.inventoryMenu; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void builtinSpellCoresAreCompleteAndIndependent(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registries);
        var configs = new JsonObject();
        registries.lookupOrThrow(dev.xkmc.l2magic.init.registrate.EngineRegistry.PROJECTILE).listElements().forEach(holder ->
                configs.add(holder.key().location().toString(), dev.xkmc.l2magic.content.entity.core.ProjectileConfig.CODEC.encodeStart(ops, holder.value()).getOrThrow()));
        long upstreamCount = helper.getLevel().registryAccess().lookupOrThrow(dev.xkmc.l2magic.init.registrate.EngineRegistry.SPELL)
                .listElementIds().filter(key -> key.location().getNamespace().equals("l2magic")).count();
        helper.assertTrue(BuiltinSpells.ids().size() == upstreamCount, "Every upstream example must have an editable preset");
        for (var id : BuiltinSpells.ids()) {
            var core = BuiltinSpells.create(id, helper.getLevel().registryAccess());
            helper.assertTrue(core.is(ModContent.FORMED_CORE.get()), "Builtin must be a formed core: " + id);
            var document = SpellCoreItem.read(core);
            var compiled = SpellCompiler.compile(document, registries);
            var source = registries.lookupOrThrow(dev.xkmc.l2magic.init.registrate.EngineRegistry.SPELL).getOrThrow(net.minecraft.resources.ResourceKey.create(
                    dev.xkmc.l2magic.init.registrate.EngineRegistry.SPELL, BuiltinSpells.sourceId(id))).value();
            var original = dev.xkmc.l2magic.content.engine.spell.SpellAction.CODEC.encodeStart(ops, source).getOrThrow();
            helper.assertTrue(canonicalSpell(original, configs).equals(canonicalSpell(com.google.gson.JsonParser.parseString(compiled.spellJson()),
                    com.google.gson.JsonParser.parseString(compiled.projectilesJson()).getAsJsonObject())), "Import must retain every original field and effect: " + id);
            helper.assertTrue(SpellCompiler.compile(document, helper.getLevel().registryAccess()).complete(), "Builtin must compile: " + id);
            document.setParameters(document.root.id, java.util.Map.of("order", "999"));
            var other = BuiltinSpells.create(id, helper.getLevel().registryAccess());
            helper.assertTrue(!SpellCoreItem.read(other).root.parameters.get("order").equals("999"), "Preset instances must not share mutable documents");
            var rewritten = SpellCoreItem.write(document, SpellCompiler.compile(document, helper.getLevel().registryAccess()), core);
            helper.assertTrue(rewritten.getHoverName().equals(core.getHoverName()), "Editing must preserve the builtin's name");
        }
        helper.succeed();
    }

    /** Expand projectile references and omit empty lists emitted for unoccupied optional slots. */
    private static com.google.gson.JsonElement canonicalSpell(com.google.gson.JsonElement json, JsonObject configs) {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString() && configs.has(json.getAsString()))
            return canonicalSpell(configs.get(json.getAsString()), configs);
        if (json.isJsonObject()) {
            var result = new JsonObject();
            json.getAsJsonObject().entrySet().forEach(entry -> {
                if (!entry.getValue().isJsonArray() || !entry.getValue().getAsJsonArray().isEmpty())
                    result.add(entry.getKey(), canonicalSpell(entry.getValue(), configs));
            });
            return result;
        }
        if (json.isJsonArray()) {
            var result = new com.google.gson.JsonArray();
            json.getAsJsonArray().forEach(value -> result.add(canonicalSpell(value, configs)));
            return result;
        }
        return json;
    }

    @GameTest(template = "empty")
    public static void creativeWandsStayInTheirOwnTabs(GameTestHelper helper) {
        var tab = dev.xkmc.l2magic.init.registrate.LMItems.TAB.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        for (var stack : java.util.stream.Stream.concat(tab.getDisplayItems().stream(), tab.getSearchTabDisplayItems().stream()).toList()) {
            helper.assertTrue(!stack.is(ModContent.CORE_WAND.get()), "MM wand must not appear in the L2Magic tab");
            helper.assertTrue(!stack.is(dev.xkmc.l2magic.init.registrate.LMItems.CREATIVE_WAND.get()) || !ModularMagic.MOD_ID.equals(
                    dev.xkmc.l2magic.init.registrate.LMItems.MODID.getOrDefault(stack, "")), "Internal anchor must not generate a visible creative wand");
        }
        helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(dev.xkmc.l2magic.init.registrate.LMItems.CREATIVE_WAND.get())),
                "Original L2Magic creative wand must remain available");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyNodeTemplateMatchesItsEngineCodec(GameTestHelper helper) {
        for (var template : NodeCatalog.all()) {
            var doc = SpellDocument.create(template.category().equals("root") ? template.id() : "root/instant");
            var root = doc.root;
            SpellDocument.Node subject;
            if (template.category().equals("root")) subject = root;
            else if (template.category().equals("trigger")) {
                subject = doc.insert(root.id, "trigger_type", 0, template.id());
                doc.insert(root.id, "action", 0, "action/sound");
            } else {
                doc.insert(root.id, "trigger_type", 0, "trigger/self_pos");
                subject = switch (template.category()) {
                    case "action" -> doc.insert(root.id, "action", 0, template.id());
                    case "predicate" -> {
                        doc.insert(root.id, "action", 0, "action/sound");
                        yield doc.insert(root.id, "condition", 0, template.id());
                    }
                    case "modifier" -> {
                        var move = doc.insert(root.id, "action", 0, "action/move");
                        doc.insert(move.id, "child", 0, "action/sound");
                        yield doc.insert(move.id, "modifiers", 0, template.id());
                    }
                    case "selector" -> {
                        var process = doc.insert(root.id, "action", 0, "action/processor");
                        yield doc.insert(process.id, "selector", 0, template.id());
                    }
                    case "particle_renderer" -> {
                        var particle = doc.insert(root.id, "action", 0, "action/custom_particle");
                        yield doc.insert(particle.id, "renderer", 0, template.id());
                    }
                    default -> {
                        var shoot = doc.insert(root.id, "action", 0, "action/custom_projectile");
                        if (template.category().equals("projectile")) yield doc.insert(shoot.id, "config", 0, template.id());
                        var config = doc.insert(shoot.id, "config", 0, "projectile/config");
                        if (template.category().equals("effect")) yield doc.insert(config.id, "hit", 0, template.id());
                        if (template.category().equals("filter")) {
                            var filtered = doc.insert(config.id, "hit", 0, "effect/filtered");
                            yield doc.insert(filtered.id, "filter", 0, template.id());
                        }
                        yield doc.insert(config.id, template.category().equals("motion") ? "motion" : "renderer", 0, template.id());
                    }
                };
            }
            fillRequired(doc, root);
            var compiled = SpellCompiler.compile(doc, helper.getLevel().registryAccess());
            helper.assertTrue(compiled.complete(), template.id() + " failed its actual engine codec: " + compiled.errors());
        }
        helper.succeed();
    }

    private static void fillRequired(SpellDocument doc, SpellDocument.Node node) {
        for (var slot : node.schema().slots()) {
            var children = node.children.get(slot.key());
            if (!slot.optional() && !slot.list() && children.isEmpty()) {
                String fallback = switch (slot.category()) {
                    case "action" -> "action/sound";
                    case "trigger" -> "trigger/self_pos";
                    case "selector" -> "selector/self";
                    case "predicate" -> "predicate/expression";
                    case "filter" -> "filter/effect";
                    case "effect" -> "effect/damage";
                    case "motion" -> "motion/simple";
                    case "particle_renderer" -> "particle_renderer/color";
                    case "projectile_renderer" -> "projectile_renderer/simple";
                    case "projectile" -> "projectile/config";
                    default -> throw new IllegalArgumentException(slot.category());
                };
                doc.insert(node.id, slot.key(), 0, fallback);
            }
            for (var child : children) fillRequired(doc, child);
        }
    }

    @GameTest(template = "empty")
    public static void coreRoundtripAndEngineValidation(GameTestHelper helper) {
        var doc = SpellDocument.create("root/instant");
        var partial = SpellCompiler.compile(doc, helper.getLevel().registryAccess());
        helper.assertTrue(!partial.complete(), "Empty root must remain unformed");
        var draft = SpellCoreItem.write(doc, partial);
        helper.assertTrue(draft.is(ModContent.NODES.get("root/instant").get()), "An empty draft must retain its typed core");
        doc = SpellCoreItem.read(draft);
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        doc.setParameters(loop.id, java.util.Map.of("index", "i", "step", "3"));
        var sound = doc.insert(loop.id, "child", 0, "action/sound");
        doc.setParameters(sound.id, java.util.Map.of("pitch", "i+1"));
        var valid = SpellCompiler.compile(doc, helper.getLevel().registryAccess());
        helper.assertTrue(valid.complete(), "Valid loop must pass L2Magic: " + valid.errors());
        var core = SpellCoreItem.write(doc, valid);
        helper.assertTrue(core.is(ModContent.FORMED_CORE.get()), "Valid tree must produce a formed core");
        helper.assertTrue(SpellCoreItem.read(core).toJson().equals(doc.toJson()), "Core must retain all node IDs and parameters");
        doc.setParameters(loop.id, java.util.Map.of("step", "3+"));
        var broken = SpellCompiler.compile(doc, helper.getLevel().registryAccess());
        helper.assertTrue(!broken.complete(), "Syntactically invalid expression must fail");
        var restored = SpellCoreItem.read(SpellCoreItem.write(doc, broken));
        helper.assertTrue(restored.find(loop.id).parameters.get("step").equals("3+"), "Invalid expression must survive draft save");
        for (String invalid : java.util.List.of("min(1)", "sin()", "NoSuchVariable")) {
            doc.setParameters(loop.id, java.util.Map.of("step", invalid));
            helper.assertTrue(!SpellCompiler.compile(doc, helper.getLevel().registryAccess()).complete(), "Invalid expression must fail: " + invalid);
        }
        doc.setParameters(loop.id, java.util.Map.of("step", "min(3, 4)"));
        helper.assertTrue(SpellCompiler.compile(doc, helper.getLevel().registryAccess()).complete(), "Custom engine functions must remain valid");
        var oversized = SpellDocument.create("root/instant");
        oversized.insert(oversized.root.id, "trigger_type", 0, "trigger/self_pos");
        var random = oversized.insert(oversized.root.id, "action", 0, "action/random");
        oversized.insert(random.id, "child", 0, "action/sound");
        oversized.setParameters(random.id, java.util.Map.of("count", "2147483647"));
        helper.assertTrue(!SpellCompiler.compile(oversized, helper.getLevel().registryAccess()).complete(), "Variable expansion must be bounded before invoking engine verification");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void physicalItemsAndRejectedTransactions(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModContent.WRITING_TABLE.get().defaultBlockState());
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        var menu = new WritingMenu(17, player.getInventory(), pos);
        menu.getSlot(0).set(new ItemStack(ModContent.NODES.get("root/instant").get()));
        var doc = menu.document();
        helper.assertTrue(doc != null, "Inserting a real root must create a document");
        menu.setCarried(new ItemStack(ModContent.NODES.get("action/iterate").get(), 2));
        var insert = command("insert"); insert.addProperty("parent", doc.root.id); insert.addProperty("field", "action"); insert.addProperty("index", 0);
        long staleRevision = menu.revision();
        menu.apply(menu.revision(), insert.toString());
        helper.assertTrue(menu.getCarried().getCount() == 1, "Exactly one physical gem must be consumed");
        menu.apply(staleRevision, insert.toString());
        helper.assertTrue(menu.getCarried().getCount() == 1 && menu.document().nodes().size() == 2, "Stale requests must not duplicate or consume items");
        var loop = menu.document().root.children.get("action").getFirst();
        menu.setCarried(new ItemStack(ModContent.NODES.get("action/sound").get()));
        insert.addProperty("parent", loop.id); insert.addProperty("field", "child");
        menu.apply(menu.revision(), insert.toString());
        helper.assertTrue(menu.getCarried().isEmpty(), "Child gem must be consumed");
        var remove = command("remove"); remove.addProperty("node", loop.id);
        menu.apply(menu.revision(), remove.toString());
        helper.assertTrue(menu.getCarried().isEmpty() && menu.document().nodes().size() == 3, "A parent with children must stay locked");
        var take = command("take"); menu.apply(menu.revision(), take.toString());
        helper.assertTrue(menu.document() == null && menu.getSlot(0).getItem().isEmpty(), "Taking a core must empty the table session");
        helper.assertTrue(SpellCoreItem.read(menu.getCarried()).nodes().size() == 3, "All three physical items must remain in the draft core");
        var saved = menu.getCarried(); menu.setCarried(ItemStack.EMPTY); menu.getSlot(0).set(saved);
        helper.assertTrue(menu.document().nodes().size() == 3, "A saved core must reopen without losing items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void localProjectileConfigurationCompilesWithoutChangingEngineRegistries(GameTestHelper helper) {
        var doc = SpellDocument.create("root/instant");
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        var shoot = doc.insert(doc.root.id, "action", 0, "action/custom_projectile");
        var config = doc.insert(shoot.id, "config", 0, "projectile/config");
        doc.insert(config.id, "motion", 0, "motion/simple");
        doc.insert(config.id, "renderer", 0, "projectile_renderer/simple");
        var result = SpellCompiler.compile(doc, helper.getLevel().registryAccess());
        helper.assertTrue(result.complete(), "Local projectile config must compile: " + result.errors());
        helper.assertTrue(result.projectilesJson().contains("modularmagic:assembled/"), "Export must retain local projectile registry entries");
        helper.succeed();
    }

    private static JsonObject command(String operation) {
        var command = new JsonObject(); command.addProperty("op", operation); return command;
    }
}
