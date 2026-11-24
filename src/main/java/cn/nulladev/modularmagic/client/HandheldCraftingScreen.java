package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.utility.HandheldCraftingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

public final class HandheldCraftingScreen extends CraftingScreen {
    public HandheldCraftingScreen(CraftingMenu menu, Inventory player, Component title) {
        super(menu, player, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        graphics.fill(leftPos + 91, topPos + 16, leftPos + 109, topPos + 34, 0xff373737);
        graphics.fill(leftPos + 92, topPos + 17, leftPos + 109, topPos + 34, 0xff8b8b8b);
        var handheld = (HandheldCraftingMenu) menu;
        for (int index = 0; index < 9; index++) {
            var sample = handheld.reference(index);
            if (sample.isEmpty()) continue;
            var slot = menu.getSlot(index + 1);
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.renderItem(sample, x, y);
            graphics.fill(x, y, x + 16, y + 16, 0x598b8b8b);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int status = ((HandheldCraftingMenu) menu).status();
        if (status != 0 && isHovering(124, 35, 16, 16, mouseX, mouseY) && menu.getCarried().isEmpty()) {
            graphics.renderTooltip(font, Component.translatable(status == 1
                    ? "gui.modularmagic.derivation.blank" : "gui.modularmagic.derivation.aether"), mouseX, mouseY);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
