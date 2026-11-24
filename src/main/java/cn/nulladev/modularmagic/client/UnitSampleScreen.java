package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.utility.UnitSampleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class UnitSampleScreen extends AbstractContainerScreen<UnitSampleMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    public UnitSampleScreen(UnitSampleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.fill(leftPos + 61, topPos + 16, leftPos + 115, topPos + 70, 0xffc6c6c6);
        graphics.fill(leftPos + 79, topPos + 34, leftPos + 97, topPos + 52, 0xff373737);
        graphics.fill(leftPos + 80, topPos + 35, leftPos + 97, topPos + 52, 0xff8b8b8b);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
