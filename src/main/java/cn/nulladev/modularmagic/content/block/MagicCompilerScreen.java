package cn.nulladev.modularmagic.content.block;

import cn.nulladev.modularmagic.content.init.ModularMagic;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class MagicCompilerScreen extends AbstractContainerScreen {
    private static net.minecraft.resources.ResourceLocation ResourceLocation;
    private static final ResourceLocation BACKGROUND_LOCATION = ResourceLocation.fromNamespaceAndPath(ModularMagic.MODID, "textures/gui/container/my_container_screen.png");

    protected MagicCompilerScreen(MagicCompilerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // Do something
        this.titleLabelX = 10;
    }

    @Override
    protected void init() {
        super.init();

//        this.addRenderableWidget(new EditBox(/* ... */));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // This method is added by the container screen to submit
        // the tooltip of the hovered slot in the tooltip stratum.
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND_LOCATION,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                256, 256
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        // Do something
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        // Tick things here
    }

    @Override
    public void onClose() {
        // Stop any handlers here

        // Call last in case it interferes with the override
        super.onClose();
    }

    @Override
    public void removed() {
        // Reset initial states here

        // Call last in case it interferes with the override
        super.removed()
        ;}
}
