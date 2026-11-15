package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.menu.WritingMenu;
import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class WritingScreen extends AbstractContainerScreen<WritingMenu> {
    private static final int CARD_W = 96, CARD_H = 38, ROW_H = 70;
    private static final float DEFAULT_ZOOM = 0.65F;
    private static final Set<String> DETAILS = Set.of("order", "params", "ambient", "visible", "indirect", "positioned", "damage_type", "renderType", "geom", "total", "age", "fullBright", "maxInclusive");
    private final List<Card> cards = new ArrayList<>();
    private final Map<String, Map<String, String>> edits = new LinkedHashMap<>();
    private final List<EditBox> fields = new ArrayList<>();
    private final List<NodeTemplate.Field> visibleFields = new ArrayList<>();
    private final Map<Integer, String> referenceFields = new HashMap<>();
    private String selected, rootIdentity;
    private long seenSnapshots = -1, sentRevision;
    private boolean awaiting, panning, advanced;
    private int fieldOffset;
    private float panX, panY = 12, zoom = DEFAULT_ZOOM;

    private static final class Card {
        SpellDocument.Node node;
        String parent, field, category;
        boolean list;
        int index, x, y, width;
        final List<Card> children = new ArrayList<>();
        boolean empty() { return node == null; }
    }

    public WritingScreen(WritingMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    private int canvasWidth() { return imageWidth - 200; }
    private int canvasHeight() { return imageHeight - 32; }
    private int fieldCount() { return Math.max(1, (imageHeight - 162) / 34); }
    private List<NodeTemplate.Field> inspectorFields(SpellDocument.Node node) {
        return node.schema().fields().stream().filter(f -> advanced || !DETAILS.contains(f.key()))
                .sorted(Comparator.comparingInt(f -> switch (f.key()) {
                    case "icon", "state", "block", "effect", "damage", "step", "value" -> 0;
                    default -> 1;
                })).toList();
    }
    private void centerTree() { panX = canvasWidth() * (1 - zoom) / 2; panY = 12; }

    @Override protected void init() {
        imageWidth = Math.max(300, width - 12);
        imageHeight = Math.max(220, height - 12);
        super.init();
        menu.layout(imageWidth, imageHeight);
        rebuildTree();
        rebuildEditorWidgets();
    }

    @Override protected void containerTick() {
        super.containerTick();
        synchronizeDocument();
    }

    private void synchronizeDocument() {
        if (seenSnapshots != menu.snapshots()) {
            seenSnapshots = menu.snapshots();
            if (awaiting && menu.revision() > sentRevision) edits.clear();
            awaiting = false;
            var document = menu.document();
            if (document == null) { selected = null; rootIdentity = null; edits.clear(); }
            else {
                if (!Objects.equals(rootIdentity, document.root.id)) {
                    rootIdentity = document.root.id;
                    selected = document.root.id;
                    fieldOffset = 0;
                    edits.clear();
                    zoom = DEFAULT_ZOOM; centerTree();
                }
                if (selected == null || document.nodes().stream().noneMatch(n -> n.id.equals(selected))) selected = document.root.id;
            }
            rebuildTree();
            rebuildEditorWidgets();
        }
    }

    private void rebuildEditorWidgets() {
        clearWidgets();
        fields.clear(); visibleFields.clear();
        referenceFields.clear();
        for (int i = 0; i < WritingMenu.REFERENCE_COUNT; i++) menu.reference(i, -1000, -1000, ItemStack.EMPTY);
        var help = addRenderableWidget(Button.builder(Component.literal("i"), b -> {})
                .bounds(leftPos + imageWidth - 24, topPos + 5, 16, 16).build());
        help.setTooltip(Tooltip.create(Component.translatable("gui.modularmagic.help")));
        addRenderableWidget(Button.builder(Component.translatable("gui.modularmagic.apply"), b -> send("apply", null))
                .bounds(leftPos + imageWidth - 164, topPos + 5, 68, 16).build()).active = menu.document() != null && !awaiting;
        addRenderableWidget(Button.builder(Component.translatable("gui.modularmagic.center"), b -> {
            zoom = DEFAULT_ZOOM; centerTree();
        }).bounds(leftPos + imageWidth - 92, topPos + 5, 64, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.modularmagic.take"), b -> send("take", null))
                .bounds(leftPos + imageWidth - 154, topPos + 27, 110, 20).build()).active = menu.document() != null && !awaiting;
        boolean emptyCore = menu.document() != null && !menu.document().root.hasChildren();
        var state = addRenderableWidget(Button.builder(Component.literal(menu.document() == null ? "\u2014" : emptyCore ? "\u25cb" : menu.complete() ? "\u2713" : "!"), b -> {})
                .bounds(leftPos + imageWidth - 40, topPos + 27, 24, 20).build());
        state.setTooltip(Tooltip.create(emptyCore ? Component.translatable("gui.modularmagic.empty_core") : Component.literal(menu.status().isEmpty()
                ? Component.translatable(menu.complete() ? "gui.modularmagic.formed" : "gui.modularmagic.empty_canvas").getString() : menu.status())));
        var node = selectedNode();
        if (node == null) return;
        var details = addRenderableWidget(Button.builder(Component.literal(advanced ? "\u2212" : "\u22ef"), b -> {
            advanced = !advanced; fieldOffset = 0; rebuildEditorWidgets();
        }).bounds(leftPos + imageWidth - 34, topPos + 52, 18, 16).build());
        details.setTooltip(Tooltip.create(Component.translatable(advanced ? "gui.modularmagic.basic" : "gui.modularmagic.advanced")));
        int count = fieldCount();
        var schemaFields = inspectorFields(node);
        fieldOffset = Math.max(0, Math.min(fieldOffset, Math.max(0, schemaFields.size() - count)));
        for (int i = fieldOffset; i < Math.min(schemaFields.size(), fieldOffset + count); i++) {
            var field = schemaFields.get(i);
            String value = edits.getOrDefault(node.id, Map.of()).getOrDefault(field.key(), node.parameters.getOrDefault(field.key(), ""));
            int row = i - fieldOffset;
            boolean reference = !ParameterItems.kind(node.schema(), field.key()).isEmpty() && row < WritingMenu.REFERENCE_COUNT;
            if (reference) {
                menu.reference(row, imageWidth - 178, 85 + row * 34, ParameterItems.preview(node.schema(), field.key(), value, minecraft.level.registryAccess()));
                referenceFields.put(WritingMenu.REFERENCE_START + row, field.key());
            }
            var box = new EditBox(font, leftPos + imageWidth - 178 + (reference ? 24 : 0),
                    topPos + 85 + row * 34, reference ? 138 : 162, 18, Component.literal(field.key()));
            box.setMaxLength(SpellDocument.MAX_FIELD);
            box.setValue(value);
            box.setHint(field.optional() ? Component.translatable("gui.modularmagic.optional") : Component.literal(field.kind()));
            box.setResponder(edited -> edits.computeIfAbsent(node.id, k -> new LinkedHashMap<>()).put(field.key(), edited));
            box.setEditable(!awaiting);
            fields.add(addRenderableWidget(box));
            visibleFields.add(field);
        }
    }

    private SpellDocument.Node selectedNode() {
        if (menu.document() == null || selected == null) return null;
        try { return menu.document().find(selected); } catch (IllegalArgumentException e) { return null; }
    }

    private void rebuildTree() {
        cards.clear();
        if (menu.document() == null) return;
        var tree = build(menu.document().root);
        measure(tree);
        place(tree, (canvasWidth() - tree.width) / 2, 0);
    }

    private Card build(SpellDocument.Node node) {
        var card = new Card();
        card.node = node; card.category = node.schema().category();
        for (var slot : node.schema().slots()) {
            var list = node.children.get(slot.key());
            for (int i = 0; i < list.size(); i++) {
                var child = build(list.get(i));
                child.parent = node.id; child.field = slot.key(); child.index = i;
                child.list = slot.list();
                card.children.add(child);
            }
            if ((slot.list() && list.size() < SpellDocument.MAX_LIST) || (!slot.list() && list.isEmpty())) {
                var empty = new Card();
                empty.parent = node.id; empty.field = slot.key(); empty.index = list.size(); empty.category = slot.category();
                empty.list = slot.list();
                card.children.add(empty);
            }
        }
        return card;
    }

    private int measure(Card card) {
        card.width = Math.max(CARD_W, card.children.stream().mapToInt(this::measure).sum() + Math.max(0, card.children.size() - 1) * 12);
        return card.width;
    }

    private void place(Card card, int x, int y) {
        card.x = x + (card.width - CARD_W) / 2; card.y = y;
        cards.add(card);
        int offset = x;
        for (var child : card.children) { place(child, offset, y + ROW_H); offset += child.width + 12; }
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff151b28);
        g.renderOutline(x, y, imageWidth, imageHeight, 0xff7383a2);
        g.fill(x + 8, y + 24, x + 8 + canvasWidth(), y + 24 + canvasHeight(), 0xff0c101a);
        g.fill(x + imageWidth - 188, y + 24, x + imageWidth - 8, y + imageHeight - 8, 0xff202a3b);
        g.hLine(x + imageWidth - 180, x + imageWidth - 16, y + imageHeight - 109, 0xff43516a);
        g.enableScissor(x + 8, y + 24, x + 8 + canvasWidth(), y + 24 + canvasHeight());
        if (menu.document() == null) g.drawCenteredString(font, Component.translatable("gui.modularmagic.empty_canvas"), x + 8 + canvasWidth() / 2, y + 24 + canvasHeight() / 2, 0xff788aa4);
        g.pose().pushPose();
        g.pose().translate(x + 8 + panX, y + 24 + panY, 0);
        g.pose().scale(zoom, zoom, 1);
        for (var card : cards) for (var child : card.children) {
            int color = 0xff000000 | NodeCatalog.category(child.category).rgb();
            int startX = card.x + CARD_W / 2, endX = child.x + CARD_W / 2, midY = card.y + CARD_H + 14;
            g.vLine(startX, card.y + CARD_H, midY, color);
            g.hLine(Math.min(startX, endX), Math.max(startX, endX), midY, color);
            g.vLine(endX, midY, child.y, color);
        }
        for (var card : cards) renderCard(g, card);
        g.pose().popPose();
        g.disableScissor();
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xff080d16);
            g.renderOutline(x + slot.x - 1, y + slot.y - 1, 18, 18, 0xff60708a);
        }
        var node = selectedNode();
        if (node != null) {
            g.drawString(font, font.plainSubstrByWidth(Component.translatable("node.modularmagic." + node.schema().item()).getString(), 134), x + imageWidth - 178, y + 56, 0xffe4dcff, false);
            for (int i = 0; i < visibleFields.size(); i++) {
                var field = visibleFields.get(i);
                String label = Component.translatable("parameter.modularmagic." + field.key()).getString();
                g.drawString(font, font.plainSubstrByWidth(label, 162),
                        x + imageWidth - 178, y + 75 + i * 34, 0xffbec9db, false);
            }
            if (inspectorFields(node).size() > visibleFields.size())
                g.drawString(font, Component.literal("\u2195"), x + imageWidth - 24, y + imageHeight - 120, 0xff93a5bc, false);
        }
    }

    private void renderCard(GuiGraphics g, Card card) {
        int color = 0xff000000 | NodeCatalog.category(card.category).rgb();
        g.fill(card.x, card.y, card.x + CARD_W, card.y + CARD_H, card.empty() ? 0xff151b27 : 0xff263248);
        g.renderOutline(card.x, card.y, CARD_W, CARD_H, color);
        if (card.empty()) {
            g.drawString(font, font.plainSubstrByWidth("+ " + slotLabel(card), CARD_W - 10), card.x + 5, card.y + 7, color, false);
            g.drawString(font, Component.translatable("gui.modularmagic.insert_node"), card.x + 5, card.y + 22, 0xff8290a8, false);
        } else {
            if (card.node.id.equals(selected)) g.renderOutline(card.x - 2, card.y - 2, CARD_W + 4, CARD_H + 4, 0xfff5edff);
            g.renderItem(new ItemStack(ModContent.NODES.get(card.node.template).get()), card.x + 5, card.y + 10);
            String label = Component.translatable("node.modularmagic." + card.node.schema().item()).getString();
            g.drawString(font, font.plainSubstrByWidth(label, 68), card.x + 25, card.y + 8, 0xfff0f3fa, false);
            String key = slotLabel(card);
            g.drawString(font, font.plainSubstrByWidth(key, 68), card.x + 25, card.y + 23, color, false);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0xffe9e0ff, false);
        g.drawString(font, playerInventoryTitle, imageWidth - 178, imageHeight - 100, 0xffc4cfe2, false);
    }

    private String slotLabel(Card card) {
        if (card.field == null) return Component.translatable("slot.modularmagic.root").getString();
        String label = Component.translatable("slot.modularmagic." + card.field).getString();
        if (card.list) label += " " + (card.index + 1);
        return label;
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        synchronizeDocument();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) return;
        var card = hitCard(mouseX, mouseY);
        if (card != null) {
            var text = new ArrayList<Component>();
            if (card.empty()) {
                text.add(Component.literal(slotLabel(card)));
                text.add(Component.translatable("category.modularmagic." + card.category).withColor(NodeCatalog.category(card.category).rgb()));
            } else {
                text.add(Component.translatable(card.node.schema().translationKey()));
                if (minecraft.options.advancedItemTooltips) text.add(Component.literal(card.node.schema().nativeType()));
                if (card.node.hasChildren()) text.add(Component.translatable("gui.modularmagic.locked"));
            }
            g.renderComponentTooltip(font, text, mouseX, mouseY);
        }
    }

    private boolean inCanvas(double x, double y) {
        return x >= leftPos + 8 && x < leftPos + 8 + canvasWidth() && y >= topPos + 24 && y < topPos + 24 + canvasHeight();
    }

    private Card hitCard(double x, double y) {
        if (!inCanvas(x, y)) return null;
        double localX = (x - leftPos - 8 - panX) / zoom, localY = (y - topPos - 24 - panY) / zoom;
        for (var card : cards) if (localX >= card.x && localX < card.x + CARD_W && localY >= card.y && localY < card.y + CARD_H) return card;
        return null;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        synchronizeDocument();
        if (inCanvas(x, y)) {
            var card = hitCard(x, y);
            if (button == 2 || card == null) { panning = true; return true; }
            if (!awaiting && card.empty() && !menu.getCarried().isEmpty() && button == 0) send("insert", card);
            else if (!card.empty()) {
                selected = card.node.id; fieldOffset = 0; rebuildEditorWidgets();
                if (!awaiting && button == 1 && !card.node.id.equals(menu.document().root.id)) send("remove", card);
            }
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (panning) { panX += (float)dx; panY += (float)dy; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        if (panning) { panning = false; return true; }
        return super.mouseReleased(x, y, button);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        synchronizeDocument();
        if (inCanvas(x, y)) {
            float newZoom = Math.clamp(zoom + (float)vertical * 0.05F, 0.2F, 1.75F);
            panX = (float)(x - leftPos - 8 - (x - leftPos - 8 - panX) * newZoom / zoom);
            panY = (float)(y - topPos - 24 - (y - topPos - 24 - panY) * newZoom / zoom);
            zoom = newZoom; return true;
        }
        if (x > leftPos + imageWidth - 188 && y >= topPos + 70 && y < topPos + imageHeight - 109) {
            fieldOffset -= (int)vertical; rebuildEditorWidgets(); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    private void send(String op, Card card) {
        sendCommand(op, card, null, false);
    }
    private void sendCommand(String op, Card card, String referenceField, boolean clear) {
        if (awaiting || menu.document() == null) return;
        var command = new JsonObject();
        command.addProperty("op", op);
        if (referenceField != null) {
            command.addProperty("node", selected);
            command.addProperty("field", referenceField);
            command.addProperty("clear", clear);
        }
        if (card != null) {
            if (op.equals("insert")) {
                command.addProperty("parent", card.parent); command.addProperty("field", card.field); command.addProperty("index", card.index);
            } else command.addProperty("node", card.node.id);
        }
        var changes = new JsonObject();
        edits.forEach((node, values) -> {
            var json = new JsonObject(); values.forEach(json::addProperty); changes.add(node, json);
        });
        command.add("edits", changes);
        if (command.toString().length() > 32768) return;
        sentRevision = menu.revision();
        awaiting = true;
        PacketDistributor.sendToServer(new EditorNetworking.Edit(menu.containerId, sentRevision, command.toString()));
        rebuildEditorWidgets();
    }

    @Override protected void slotClicked(Slot slot, int id, int button, ClickType type) {
        synchronizeDocument();
        if (awaiting) return;
        if (id >= WritingMenu.REFERENCE_START) {
            String field = referenceFields.get(id);
            if (field != null && type == ClickType.PICKUP && (button == 0 || button == 1))
                sendCommand("reference", null, field, button == 1);
            return;
        }
        if (id == 0 && !edits.isEmpty()) { send("apply", null); return; }
        super.slotClicked(slot, id, button, type);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        synchronizeDocument();
        if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (getFocused() instanceof EditBox box) { box.keyPressed(key, scanCode, modifiers); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() {
        synchronizeDocument();
        if (!edits.isEmpty() && !awaiting) send("apply", null);
        super.onClose();
    }

    @Override public boolean charTyped(char character, int modifiers) {
        synchronizeDocument();
        return super.charTyped(character, modifiers);
    }
}
