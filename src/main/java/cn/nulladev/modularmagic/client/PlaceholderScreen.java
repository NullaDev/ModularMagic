package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.research.PlaceholderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PlaceholderScreen extends AbstractContainerScreen<PlaceholderMenu>{
    public PlaceholderScreen(PlaceholderMenu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=212;inventoryLabelY=118;}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff192332);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xff809eb5);for(var slot:menu.slots){g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,slot.index==9?0xff597184:0xff0c1420);g.renderOutline(leftPos+slot.x-1,topPos+slot.y-1,18,18,slot.index==9?0xff85dce9:0xff43576c);}g.drawString(font,"→",leftPos+120,topPos+41,0xffd8e8ed,false);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,8,6,0xffd5e7ee,false);g.drawString(font,playerInventoryTitle,8,118,0xffc3d2e0,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
