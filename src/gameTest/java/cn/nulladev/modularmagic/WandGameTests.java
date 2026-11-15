package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.item.*;
import cn.nulladev.modularmagic.menu.*;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WandGameTests {
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "WandTester"));
        var pos = helper.absolutePos(new BlockPos(1, 1, 0));
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return player;
    }
    private static void reference(WritingMenu menu, String node, String field) {
        var command = new JsonObject(); command.addProperty("op", "reference"); command.addProperty("node", node); command.addProperty("field", field);
        menu.apply(menu.revision(), command.toString());
    }
    @GameTest(template = "empty")
    public static void referenceSlotsSampleWithoutConsumingItems(GameTestHelper helper) {
        var player = player(helper); var pos = player.blockPosition();
        helper.getLevel().setBlockAndUpdate(pos, ModContent.WRITING_TABLE.get().defaultBlockState());
        var menu = new WritingMenu(61, player.getInventory(), pos);
        var doc = SpellDocument.create("root/instant");
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        var list = doc.insert(doc.root.id, "action", 0, "action/list");
        var block = doc.insert(list.id, "children", 0, "action/set_block");
        var process = doc.insert(list.id, "children", 1, "action/processor");
        doc.insert(process.id, "selector", 0, "selector/self");
        var effect = doc.insert(process.id, "processors", 0, "effect/effect");
        menu.getSlot(0).set(SpellCoreItem.write(doc, SpellCompiler.compile(doc, helper.getLevel().registryAccess())));
        menu.setCarried(new ItemStack(Items.DIAMOND, 12));
        reference(menu, doc.root.id, "icon");
        helper.assertTrue(menu.document().root.parameters.get("icon").equals("minecraft:diamond") && menu.getCarried().getCount() == 12, "Icon reference must not consume diamonds");
        menu.setCarried(new ItemStack(Items.OAK_PLANKS, 17));
        reference(menu, block.id, "state");
        helper.assertTrue(menu.document().find(block.id).parameters.get("state").equals("minecraft:oak_planks") && menu.getCarried().getCount() == 17, "Block reference must not consume blocks");
        var potion = new ItemStack(Items.POTION);
        potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        menu.setCarried(potion); reference(menu, effect.id, "effect");
        helper.assertTrue(menu.document().find(effect.id).parameters.get("effect").equals("minecraft:speed") && menu.getCarried().getCount() == 1, "Potion must select its actual effect without being consumed");
        menu.setCarried(new ItemStack(Items.DIAMOND, 3));
        reference(menu, block.id, "state");
        helper.assertTrue(menu.document().find(block.id).parameters.get("state").equals("minecraft:oak_planks") && menu.getCarried().getCount() == 3, "Wrong reference types must be rejected atomically");
        menu.clicked(WritingMenu.REFERENCE_START, 0, ClickType.CLONE, player);
        helper.assertTrue(menu.getCarried().getCount() == 3 && menu.quickMoveStack(player, WritingMenu.REFERENCE_START).isEmpty(), "Ghost slots must never create or move physical items");
        menu.setCarried(ItemStack.EMPTY); menu.removed(player); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wandCoresPersistAndSelectorDoesNotReplaceStoredItems(GameTestHelper helper) {
        var player = player(helper); var wand = ModContent.CORE_WAND.toStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var menu = new WandMenu(62, player.getInventory(), InteractionHand.MAIN_HAND); player.containerMenu = menu;
        for (int i = 0; i < WandCores.CAPACITY; i++) menu.getSlot(i).set(BuiltinSpells.create("magic_arrows", helper.getLevel().registryAccess()));
        helper.assertTrue(WandCores.read(wand).stream().filter(c -> !c.isEmpty()).count() == 9, "All nine real cores must be stored in the wand");
        menu.clicked(36, 0, ClickType.THROW, player);
        helper.assertTrue(player.getMainHandItem() == wand, "The open wand must not be movable from its inventory slot");
        menu.removed(player); player.containerMenu = player.inventoryMenu;
        var selected = dev.xkmc.l2itemselector.select.item.IItemSelector.getSelection(player);
        helper.assertTrue(selected != null && selected.selector() instanceof CoreWandSelector, "Wand must use the prerequisite's real selection system");
        var selector = new CoreWandSelector(); selector.swap(player, 5, wand);
        helper.assertTrue(WandCores.selected(wand) == 1 && WandCores.read(wand).stream().filter(c -> !c.isEmpty()).count() == 9, "Switching must change only the selected core, never replace the wand contents");
        var restored = ItemStack.parse(helper.getLevel().registryAccess(), wand.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(WandCores.selected(restored) == 1 && WandCores.read(restored).stream().filter(c -> !c.isEmpty()).count() == 9, "Core storage and selection must survive NBT serialization");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wandExecutesSpellAndCostHookCanDenyIt(GameTestHelper helper) {
        var player = player(helper); var level = helper.getLevel();
        var doc = SpellDocument.create("root/instant");
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        var block = doc.insert(doc.root.id, "action", 0, "action/set_block");
        doc.setParameters(block.id, java.util.Map.of("state", "minecraft:stone"));
        var wand = ModContent.CORE_WAND.toStack(); var cores = WandCores.read(wand);
        cores.set(0, SpellCoreItem.write(doc, SpellCompiler.compile(doc, level.registryAccess()))); WandCores.write(wand, cores);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        var item = (CoreWandItem)wand.getItem(); var pos = player.blockPosition();
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.assertTrue(item.cast(wand, player, 0) && level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.STONE), "Wand must execute the L2Magic block effect on the server");
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        try {
            CastCosts.setHandler((p, w, c, context) -> false);
            helper.assertTrue(!item.cast(wand, player, 0) && level.getBlockState(pos).isAir(), "Resource hook rejection must occur before any effect");
        } finally { CastCosts.setHandler((p, w, c, context) -> true); }
        helper.assertTrue(WandCores.read(wand).get(0).getCount() == 1, "Casting must retain the core");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void customProjectileProgramSurvivesNbtAndSpawnPackets(GameTestHelper helper) {
        var player = player(helper); var level = helper.getLevel();
        var wand = ModContent.CORE_WAND.toStack(); var cores = WandCores.read(wand);
        var large = SpellCoreItem.read(TestSpells.projectile(level.registryAccess()));
        var shot = large.root.children.get("action").getFirst();
        large.root.children.get("action").clear();
        var sequence = large.insert(large.root.id, "action", 0, "action/list");
        sequence.children.get("children").add(shot);
        for (int i = 0; i < 20; i++) {
            var loop = large.insert(sequence.id, "children", i + 1, "action/iterate");
            large.setParameters(loop.id, java.util.Map.of("step", "0", "index", "v" + i + "x".repeat(1900)));
            large.insert(loop.id, "child", 0, "action/sound");
        }
        helper.assertTrue(large.toJson().length() > 32767, "Test must exercise a multi-chunk projectile program");
        cores.set(0, SpellCoreItem.write(large, SpellCompiler.compile(large, level.registryAccess()))); WandCores.write(wand, cores);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        helper.assertTrue(((CoreWandItem)wand.getItem()).cast(wand, player, 0), "Custom projectile cast must succeed");
        var projectiles = level.getEntitiesOfClass(dev.xkmc.l2magic.content.entity.core.LMProjectile.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(8));
        helper.assertTrue(projectiles.size() == 1, "The actual L2Magic projectile must spawn");
        var projectile = projectiles.getFirst();
        helper.assertTrue(projectile.getRenderer() != null && projectile.lifetime() == 60, "Local projectile config must work without global registry mutation");
        var tag = new net.minecraft.nbt.CompoundTag(); projectile.addAdditionalSaveData(tag);
        var copy = new dev.xkmc.l2magic.content.entity.core.LMProjectile(dev.xkmc.l2magic.init.registrate.LMItems.GENERIC_PROJECTILE.get(), level);
        copy.readAdditionalSaveData(tag); copy.setOwner(player);
        helper.assertTrue(copy.getRenderer() != null && copy.lifetime() == 60, "NBT reload must retain the local projectile program");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            projectile.writeSpawnData(buffer);
            var clientCopy = new dev.xkmc.l2magic.content.entity.core.LMProjectile(dev.xkmc.l2magic.init.registrate.LMItems.GENERIC_PROJECTILE.get(), level);
            clientCopy.readSpawnData(buffer); clientCopy.setOwner(player);
            helper.assertTrue(clientCopy.getRenderer() != null, "Spawn packet roundtrip must carry the custom projectile program");
        } finally { buffer.release(); projectile.discard(); }
        helper.succeed();
    }
}
