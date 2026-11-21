package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.crafting.*;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EnchantingScreen extends AbstractContainerScreen<EnchantingMenu> {
    private Button apply;
    public EnchantingScreen(EnchantingMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=194;imageHeight=232;}
    private Component tr(String key,Object...args){return Component.translatable("enchanter.modularmagic."+key,args);}
    @Override protected void init(){super.init();apply=addRenderableWidget(Button.builder(tr("apply"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,100)).bounds(leftPos+106,topPos+23,70,20).build());}
    @Override protected void containerTick(){super.containerTick();apply.active=menu.possible&&menu.energy>=menu.cost&&!menu.paused;apply.setTooltip(menu.paused?Tooltip.create(tr("paused")):menu.possible&&menu.energy<menu.cost?Tooltip.create(tr("waiting")):null);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff172332);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xff6d94a0);for(var s:menu.slots){g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xff0b1520);g.renderOutline(leftPos+s.x-1,topPos+s.y-1,18,18,s.index==menu.selected?0xffffdf8f:0xff506c7a);}g.drawString(font,"→",leftPos+42,topPos+29,0xffd3eaf2,false);g.renderOutline(leftPos+62,topPos+24,18,18,0xffcfbc83);var q=menu.quote();if(!q.result().isEmpty())g.renderItem(q.result(),leftPos+63,topPos+25);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,8,7,0xffd6e5ed,false);g.drawString(font,tr("preview"),62,14,0xff9bacb7,false);var message=menu.slots.get(0).getItem().isEmpty()?tr("insert_equipment"):menu.selected<0?tr("select_sample"):!menu.possible?tr("no_change"):tr("fee",AetherNumbers.format(menu.cost));if(menu.slots.get(0).hasItem()&&menu.selected>=0)g.drawString(font,message,16,51,0xffc5d4e0,false);g.drawString(font,tr("stored",AetherNumbers.format(menu.energy)),16,132,0xff71d4e8,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);var q=menu.quote();if(!q.result().isEmpty()&&mx>=leftPos+62&&mx<leftPos+80&&my>=topPos+24&&my<topPos+42)g.renderTooltip(font,q.result(),mx,my);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&!hasShiftDown()&&menu.getCarried().isEmpty())for(int i=1;i<28;i++){var s=menu.slots.get(i);if(s.hasItem()&&x>=leftPos+s.x&&x<leftPos+s.x+16&&y>=topPos+s.y&&y<topPos+s.y+16){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i);return true;}}return super.mouseClicked(x,y,button);}
}
