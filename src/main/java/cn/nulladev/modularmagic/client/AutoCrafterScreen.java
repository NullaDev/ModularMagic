package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.utility.AutoCrafterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {
    private Button toggle;
    public AutoCrafterScreen(AutoCrafterMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=198;inventoryLabelY=104;}
    @Override protected void init(){super.init();toggle=addRenderableWidget(Button.builder(Component.empty(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+110,topPos+72,58,20).build());updateButton();}
    private void updateButton(){toggle.setMessage(Component.translatable(menu.enabled()?"gui.modularmagic.autocrafter_stop":"gui.modularmagic.autocrafter_start"));toggle.active=!menu.preview.isEmpty();}
    @Override protected void containerTick(){super.containerTick();updateButton();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff172332);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xff7193a0);
        for(var slot:menu.slots){int x=leftPos+slot.x,y=topPos+slot.y;g.fill(x-1,y-1,x+17,y+17,0xff0b1520);g.renderOutline(x-1,y-1,18,18,slot.index==9?0xffcfbc83:0xff506c7a);if(slot.index<9&&!slot.hasItem()){g.fill(x+3,y+7,x+13,y+9,0xff42505c);}}
        g.drawString(font,"\u2192",leftPos+96,topPos+44,0xffcfe6ed,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,titleLabelX,titleLabelY,0xffcddfe8,false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xff9ab2bf,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);}
}
