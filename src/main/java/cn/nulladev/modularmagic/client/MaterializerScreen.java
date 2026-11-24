package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.utility.MaterializerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MaterializerScreen extends AbstractContainerScreen<MaterializerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    public MaterializerScreen(MaterializerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.fill(leftPos + 61, topPos + 16, leftPos + 115, topPos + 70, 0xffc6c6c6);
        for (int x : new int[]{53, 73, 116}) {
            graphics.fill(leftPos + x - 1, topPos + 34, leftPos + x + 17, topPos + 52, 0xff373737);
            graphics.fill(leftPos + x, topPos + 35, leftPos + x + 17, topPos + 52, 0xff8b8b8b);
        }
        graphics.drawString(font, "\u2192", leftPos + 99, topPos + 40, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
