package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.client.WritingScreen;
import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.SpellDocument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Field;
import java.util.List;

/** Drives real screen clicks and real client/server packets in a disposable copied save. */
@EventBusSubscriber(modid = ModularMagic.MOD_ID, value = Dist.CLIENT)
public final class EditorClientSmoke {
    private static int ticks, stage, changedAt;
    private static volatile boolean prepared;
    private static BlockPos table;
    private static String loopId, listId;
    private static String blockId, effectId;
    private static boolean iconReferenceSent;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("modularmagic.clientSmoke")) return;
        var mc = Minecraft.getInstance();
        if (++ticks > 6000) { fail(mc, new IllegalStateException("Client smoke timed out at stage " + stage)); return; }
        if (ticks - changedAt < 12) return;
        try {
            if (stage == 0 && mc.player != null && mc.getSingleplayerServer() != null) {
                mc.options.guiScale().set(2); mc.resizeDisplay();
                var uuid = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                    player.setGameMode(GameType.CREATIVE);
                    player.getInventory().clearContent();
                    table = player.blockPosition().offset(2, 0, 0);
                    player.serverLevel().setBlockAndUpdate(table, ModContent.WRITING_TABLE.get().defaultBlockState());
                    ((cn.nulladev.modularmagic.world.WritingTableBlockEntity) player.serverLevel().getBlockEntity(table)).inventory().clearContent();
                    String[] items = {"root/instant", "trigger/self_pos", "action/list", "action/iterate", "action/sound"};
                    for (int i = 0; i < items.length; i++) player.getInventory().setItem(i, new ItemStack(ModContent.NODES.get(items[i]).get()));
                    player.getInventory().setItem(5, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                });
                next(); return;
            }
            if (stage == 1 && prepared) {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
                next(); return;
            }
            if (mc.screen instanceof cn.nulladev.modularmagic.client.WandScreen wandScreen) {
                var wandMenu = wandScreen.getMenu();
                if (stage == 19 && wandMenu.getSlot(9).hasItem()) { clickSlot(wandScreen, 9); clickSlot(wandScreen, 0); next(); }
                else if (stage == 20 && wandMenu.getSlot(0).hasItem()) { clickSlot(wandScreen, 10); clickSlot(wandScreen, 1); next(); }
                else if (stage == 21 && wandMenu.getSlot(1).hasItem()) { clickSlot(wandScreen, 11); clickSlot(wandScreen, 2); next(); }
                else if (stage == 22 && wandMenu.getSlot(2).hasItem()) { screenshot(mc, "modularmagic-wand-cores.png"); wandScreen.onClose(); next(); }
                else if (stage == 27) { clickSlot(wandScreen, 2); clickSlot(wandScreen, 9); next(); }
                else if (stage == 28 && !wandMenu.getSlot(2).hasItem()) { wandScreen.onClose(); next(); }
                return;
            }
            if (!(mc.screen instanceof WritingScreen screen)) {
                if (stage == 34) {
                    screenshot(mc, "modularmagic-node-guide.png");
                    ModularMagic.LOGGER.info("MODULARMAGIC_NODE_GUIDE_SUCCESS: Patchouli loaded all 100 node entries and rendered the loop entry");
                    stage = 99; mc.stop();
                } else if (stage == 15 && mc.player != null) {
                    require(mc.player.getInventory().items.stream().noneMatch(item -> item.getItem() instanceof SpellCoreItem), "Closing must not return the table's core to the player");
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(table), Direction.UP, table, false));
                    stage = 16; changedAt = ticks;
                } else if (stage == 18) { prepareWand(mc); next(); }
                else if (stage == 23) {
                    require(cn.nulladev.modularmagic.item.WandCores.read(mc.player.getMainHandItem()).stream().filter(s -> !s.isEmpty()).count() == 3, "Real core transfers must persist inside the wand");
                    switchCore(1); next();
                } else if (stage == 24 && cn.nulladev.modularmagic.item.WandCores.selected(mc.player.getMainHandItem()) == 1) {
                    mc.player.setXRot(-20);
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); next();
                } else if (stage == 25) {
                    boolean found = false;
                    for (var entity : mc.level.entitiesForRendering()) if (entity instanceof dev.xkmc.l2magic.content.entity.core.LMProjectile projectile) {
                        require(projectile.getRenderer() != null, "Real client spawn data must resolve the local projectile renderer"); found = true;
                    }
                    if (found) { screenshot(mc, "modularmagic-wand-cast.png"); openWand(mc); stage = 27; changedAt = ticks; }
                } else if (stage == 29) {
                    mc.getSingleplayerServer().execute(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        player.serverLevel().setBlockAndUpdate(table, ModContent.WRITING_TABLE.get().defaultBlockState());
                        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, user) -> new cn.nulladev.modularmagic.menu.WritingMenu(id, inv, table),
                                Component.translatable("block.modularmagic.spell_writing_table")), table);
                    }); next();
                }
                return;
            }
            var menu = screen.getMenu();
            if (stage == 2 && menu.snapshots() > 0) {
                screenshot(mc, "modularmagic-empty.png");
                clickSlot(screen, 28); clickSlot(screen, 0); next();
            } else if (stage == 3 && menu.document() != null) {
                if (!iconReferenceSent) {
                    checkSnapshotBeforeRender(mc, screen);
                    clickSlot(screen, 33); clickSlot(screen, cn.nulladev.modularmagic.menu.WritingMenu.REFERENCE_START);
                    iconReferenceSent = true; changedAt = ticks; return;
                }
                if (!menu.document().root.parameters.get("icon").equals("minecraft:diamond")) return;
                require(menu.getCarried().getCount() == 5, "Real icon reference clicks must not consume diamonds");
                clickSlot(screen, 33);
                clickSlot(screen, 29); clickCard(screen, null, menu.document().root.id, "trigger_type"); next();
            } else if (stage == 4 && !menu.document().root.children.get("trigger_type").isEmpty()) {
                clickSlot(screen, 30); clickCard(screen, null, menu.document().root.id, "action"); next();
            } else if (stage == 5 && !menu.document().root.children.get("action").isEmpty()) {
                listId = menu.document().root.children.get("action").getFirst().id;
                clickSlot(screen, 31); clickCard(screen, null, listId, "children"); next();
            } else if (stage == 6 && !menu.document().find(listId).children.get("children").isEmpty()) {
                loopId = menu.document().find(listId).children.get("children").getFirst().id;
                clickSlot(screen, 32); clickCard(screen, null, loopId, "child"); next();
            } else if (stage == 7 && menu.complete()) {
                require(menu.document().nodes().size() == 5, "Completed assembly must own exactly five nodes");
                require(cardExists(screen, null, listId, "children"), "List must retain an empty append slot");
                clickCard(screen, loopId, null, null);
                screenshot(mc, "modularmagic-formed.png");
                editStep(screen, "3+"); press(screen, "gui.modularmagic.apply"); next();
            } else if (stage == 8 && !menu.complete() && menu.document().find(loopId).parameters.get("step").equals("3+")) {
                screenshot(mc, "modularmagic-draft.png"); press(screen, "gui.modularmagic.take"); next();
            } else if (stage == 9 && menu.document() == null && !menu.getCarried().isEmpty()) {
                require(menu.getCarried().is(ModContent.UNFORMED_CORE.get()), "Invalid expression must produce unformed item");
                require(SpellCoreItem.read(menu.getCarried()).nodes().size() == 5, "Draft must retain every node item");
                clickSlot(screen, 0); next();
            } else if (stage == 10 && menu.document() != null) {
                clickCard(screen, loopId, null, null); next();
            } else if (stage == 11) {
                require(stepBox(screen).getValue().equals("3+"), "Parameter widget must restore the saved invalid text");
                editStep(screen, "3"); press(screen, "gui.modularmagic.apply"); next();
            } else if (stage == 12 && menu.complete()) {
                screenshot(mc, "modularmagic-reopened.png");
                // A pending parameter is intentionally saved only by closing the screen.
                editStep(screen, "7"); screen.onClose(); stage = 15; changedAt = ticks;
            } else if (stage == 16 && menu.document() != null) {
                require(menu.document().nodes().size() == 5 && menu.document().find(loopId).parameters.get("step").equals("7"), "Reopening the table must restore all nodes and pending text saved on close");
                screenshot(mc, "modularmagic-table-persisted.png");
                press(screen, "gui.modularmagic.take"); stage = 17; changedAt = ticks;
            } else if (stage == 17 && menu.document() == null && !menu.getCarried().isEmpty()) {
                require(SpellCoreItem.read(menu.getCarried()).nodes().size() == 5, "Only explicit Take returns the stored core");
                clickSlot(screen, 28); screen.onClose(); next();
            } else if (stage == 30 && menu.getSlot(1).hasItem()) {
                clickSlot(screen, 1); clickSlot(screen, 0); next();
            } else if (stage == 31 && menu.document() != null) {
                clickCard(screen, blockId, null, null); clickSlot(screen, 29); clickSlot(screen, cn.nulladev.modularmagic.menu.WritingMenu.REFERENCE_START); next();
            } else if (stage == 32 && menu.document().find(blockId).parameters.get("state").equals("minecraft:oak_planks")) {
                require(menu.getCarried().getCount() == 8, "Block sample slot must preserve the held stack");
                clickSlot(screen, 29); clickCard(screen, effectId, null, null); clickSlot(screen, 30); clickSlot(screen, cn.nulladev.modularmagic.menu.WritingMenu.REFERENCE_START); next();
            } else if (stage == 33 && menu.document().find(effectId).parameters.get("effect").equals("minecraft:speed")) {
                require(menu.getCarried().getCount() == 1, "Potion sample slot must preserve the potion");
                clickSlot(screen, 30); screenshot(mc, "modularmagic-reference-slots.png");
                ModularMagic.LOGGER.info("MODULARMAGIC_CLIENT_SMOKE_SUCCESS: real reference slots, non-consumption, original selector packets, wand core inventory, server cast and client projectile rendering");
                openNodeGuide(); stage = 34; changedAt = ticks;
            }
        } catch (Throwable e) { fail(mc, e); }
    }

    /** Exercise a packet/frame boundary that previously crashed while drawing stale cards. */
    private static void checkSnapshotBeforeRender(Minecraft mc, WritingScreen screen) throws ReflectiveOperationException {
        var menu = screen.getMenu();
        var original = new cn.nulladev.modularmagic.network.EditorNetworking.Snapshot(menu.containerId, menu.revision(),
                menu.document().toJson(), menu.complete(), menu.status());
        var graphics = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
        try {
            // Simulate packets arriving after the screen tick and before this frame's draw/input.
            var replacement = SpellDocument.create("root/charge");
            menu.receive(new cn.nulladev.modularmagic.network.EditorNetworking.Snapshot(menu.containerId, menu.revision() + 1,
                    replacement.toJson(), false, ""));
            screen.render(graphics, -100, -100, 0);
            require(replacement.root.id.equals(read(screen, "rootIdentity")), "Rendering must synchronize the new snapshot immediately");
            menu.receive(new cn.nulladev.modularmagic.network.EditorNetworking.Snapshot(menu.containerId, menu.revision() + 1, "", false, ""));
            screen.render(graphics, -100, -100, 0);
            require(((java.util.List<?>) read(screen, "cards")).isEmpty(), "Taking a core must clear cards before the next frame");
        } finally { menu.receive(original); }
        screen.render(graphics, -100, -100, 0);
        graphics.flush();
        clickCard(screen, menu.document().root.id, null, null);
        double x = screen.getGuiLeft() + 8 + (screen.width - 12 - 200) / 2.0;
        double y = screen.getGuiTop() + 24 + (screen.height - 12 - 32) / 2.0;
        screen.mouseClicked(x, y, 1); screen.mouseReleased(x, y, 1);
        require(!(boolean) read(screen, "awaiting"), "Right-clicking a root must not send a removal request");
    }

    /** Patchouli is runtime-only; keep the optional smoke driver independent of its compile API. */
    private static void openNodeGuide() throws ReflectiveOperationException {
        var registryType = Class.forName("vazkii.patchouli.client.book.ClientBookRegistry");
        registryType.getMethod("displayBookGui", net.minecraft.resources.ResourceLocation.class,
                net.minecraft.resources.ResourceLocation.class, int.class).invoke(registryType.getField("INSTANCE").get(null),
                ModularMagic.id("assembly_guide"), ModularMagic.id("nodes/action/iterate"), 0);
        var common = Class.forName("vazkii.patchouli.common.book.BookRegistry");
        var books = (java.util.Map<?, ?>) common.getField("books").get(common.getField("INSTANCE").get(null));
        var book = books.get(ModularMagic.id("assembly_guide"));
        var contents = book.getClass().getMethod("getContents").invoke(book);
        require(!(boolean) contents.getClass().getMethod("isErrored").invoke(contents), "Patchouli book must load without errors");
        var entries = (java.util.Map<?, ?>) contents.getClass().getField("entries").get(contents);
        var categories = (java.util.Map<?, ?>) contents.getClass().getField("categories").get(contents);
        require(categories.size() == 3 && categories.containsKey(ModularMagic.id("nodes")), "Guide must have only three categories with a shared node index");
        var lookup = contents.getClass().getMethod("getEntryForStack", ItemStack.class);
        for (var template : cn.nulladev.modularmagic.spell.NodeCatalog.all()) {
            var entry = entries.get(ModularMagic.id("nodes/" + template.id()));
            require(entry != null, "Missing Patchouli node entry: " + template.id());
            var pages = (java.util.List<?>) entry.getClass().getMethod("getPages").invoke(entry);
            require(pages.size() >= 3, "Every node needs purpose, child slots and parameter pages");
            var mapping = (com.mojang.datafixers.util.Pair<?, ?>) lookup.invoke(contents, new ItemStack(ModContent.NODES.get(template.id()).get()));
            require(mapping != null && mapping.getFirst() == entry, "Patchouli Ctrl lookup must map node to its own entry: " + template.id());
        }
        String[] items = {"spell_writing_table", "core_wand", "formed_spell_core", "unformed_spell_core"};
        String[] articles = {"writing_table", "wand", "formed_core", "unformed_core"};
        for (int i = 0; i < items.length; i++) {
            var stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ModularMagic.id(items[i])));
            var mapping = (com.mojang.datafixers.util.Pair<?, ?>) lookup.invoke(contents, stack);
            require(mapping != null && mapping.getFirst() == entries.get(ModularMagic.id("equipment/" + articles[i])), "Equipment must link to its dedicated page: " + items[i]);
        }
        var tutorial = entries.get(ModularMagic.id("tutorial"));
        require(tutorial != null && ((java.util.List<?>) tutorial.getClass().getMethod("getPages").invoke(tutorial)).size() >= 10,
                "A complete walkthrough must cover assembly, validation, storage and casting");
    }

    private static void next() { stage++; changedAt = ticks; }
    private static void require(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static void fail(Minecraft mc, Throwable e) {
        if (stage == 99) return;
        ModularMagic.LOGGER.error("MODULARMAGIC_CLIENT_SMOKE_FAILED at stage " + stage, e); stage = 99; mc.stop();
    }
    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> ModularMagic.LOGGER.info(message.getString()));
    }
    private static void clickSlot(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, int index) {
        var slot = screen.getMenu().getSlot(index);
        click(screen, screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8);
    }
    private static void click(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, double x, double y) { screen.mouseClicked(x, y, 0); screen.mouseReleased(x, y, 0); }

    private static void switchCore(int index) {
        dev.xkmc.l2itemselector.init.L2ItemSelector.PACKET_HANDLER.toServer(new dev.xkmc.l2itemselector.select.SetSelectedToServer(
                index, dev.xkmc.l2itemselector.init.L2ItemSelector.loc("item"), false, false, false));
    }
    private static void openWand(Minecraft mc) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.setShiftKeyDown(true);
            ModContent.CORE_WAND.get().use(player.level(), player, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
        });
    }
    private static void prepareWand(Minecraft mc) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.getInventory().clearContent();
            for (var pos : BlockPos.betweenClosed(player.blockPosition().offset(-4, 0, -4), player.blockPosition().offset(4, 5, 8)))
                player.serverLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, ModContent.CORE_WAND.toStack());
            player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 8));
            var potion = new ItemStack(net.minecraft.world.item.Items.POTION);
            potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
            player.getInventory().setItem(2, potion);
            player.getInventory().setItem(9, cn.nulladev.modularmagic.spell.BuiltinSpells.create("magic_arrows", player.level().registryAccess()));
            player.getInventory().setItem(10, TestSpells.projectile(player.level().registryAccess()));
            var doc = cn.nulladev.modularmagic.spell.SpellDocument.create("root/instant");
            doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
            var list = doc.insert(doc.root.id, "action", 0, "action/list");
            var block = doc.insert(list.id, "children", 0, "action/set_block"); blockId = block.id;
            var process = doc.insert(list.id, "children", 1, "action/processor");
            doc.insert(process.id, "selector", 0, "selector/self");
            effectId = doc.insert(process.id, "processors", 0, "effect/effect").id;
            player.getInventory().setItem(11, SpellCoreItem.write(doc, cn.nulladev.modularmagic.spell.SpellCompiler.compile(doc, player.level().registryAccess())));
            player.inventoryMenu.broadcastChanges();
            player.setShiftKeyDown(true); ModContent.CORE_WAND.get().use(player.level(), player, InteractionHand.MAIN_HAND); player.setShiftKeyDown(false);
        });
    }
    private static EditBox stepBox(WritingScreen screen) {
        return screen.children().stream().filter(c -> c instanceof EditBox box && box.getMessage().getString().equals("step"))
                .map(c -> (EditBox)c).findFirst().orElseThrow();
    }
    private static void editStep(WritingScreen screen, String text) {
        var box = stepBox(screen);
        box.setValue("");
        screen.setFocused(box); box.setFocused(true);
        for (char character : text.toCharArray()) screen.charTyped(character, 0);
        require(box.getValue().equals(text), "Actual text entry must reach the focused parameter widget");
    }
    private static void press(WritingScreen screen, String key) {
        var button = screen.children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(Component.translatable(key).getString()))
                .map(c -> (Button)c).findFirst().orElseThrow();
        click(screen, button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0);
    }
    private static Object read(Object object, String field) throws ReflectiveOperationException {
        Field f = object.getClass().getDeclaredField(field); f.setAccessible(true); return f.get(object);
    }
    private static Object findCard(WritingScreen screen, String nodeId, String parent, String field) throws ReflectiveOperationException {
        for (var card : (List<?>)read(screen, "cards")) {
            var node = (SpellDocument.Node)read(card, "node");
            if (nodeId != null && node != null && node.id.equals(nodeId)) return card;
            if (nodeId == null && node == null && parent.equals(read(card, "parent")) && field.equals(read(card, "field"))) return card;
        }
        return null;
    }
    private static boolean cardExists(WritingScreen screen, String nodeId, String parent, String field) throws ReflectiveOperationException { return findCard(screen, nodeId, parent, field) != null; }
    private static void clickCard(WritingScreen screen, String nodeId, String parent, String field) throws ReflectiveOperationException {
        var card = findCard(screen, nodeId, parent, field);
        require(card != null, "Expected canvas card");
        double zoom = (float)read(screen, "zoom");
        double x = screen.getGuiLeft() + 8 + (float)read(screen, "panX") + ((int)read(card, "x") + 48) * zoom;
        double y = screen.getGuiTop() + 24 + (float)read(screen, "panY") + ((int)read(card, "y") + 19) * zoom;
        int guiWidth = screen.width - 12, guiHeight = screen.height - 12;
        double centerX = screen.getGuiLeft() + 8 + (guiWidth - 200) / 2.0;
        double centerY = screen.getGuiTop() + 24 + (guiHeight - 32) / 2.0;
        // Pan through the actual drag handler so every target is visible before clicking.
        screen.mouseClicked(centerX, centerY, 2);
        screen.mouseDragged(centerX, centerY, 2, centerX - x, centerY - y);
        screen.mouseReleased(centerX, centerY, 2);
        click(screen, centerX, centerY);
    }
}
