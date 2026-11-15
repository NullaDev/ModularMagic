package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.menu.WandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WandScreen extends AbstractContainerScreen<WandMenu> {
    public WandScreen(WandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 176; inventoryLabelY = 82;
    }
    @Override protected void init() {
        super.init();
        var help = addRenderableWidget(Button.builder(Component.literal("i"), button -> {})
                .bounds(leftPos + 154, topPos + 4, 16, 16).build());
        help.setTooltip(Tooltip.create(Component.translatable("tooltip.modularmagic.wand_controls")));
    }
    @Override protected void renderBg(GuiGraphics g, float tick, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff202735);
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xff8491a5);
        for (var slot : menu.slots) {
            g.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xff0c111b);
            g.renderOutline(leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18, 0xff59677b);
        }
        var selected = menu.getSlot(menu.selected());
        g.renderOutline(leftPos + selected.x - 2, topPos + selected.y - 2, 20, 20, 0xffb69aff);
    }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) {
        g.drawString(font, title, 8, 7, 0xffe9e3fc, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 0xffbbc7d6, false);
    }
    @Override public void render(GuiGraphics g, int x, int y, float tick) { super.render(g, x, y, tick); renderTooltip(g, x, y); }
}
