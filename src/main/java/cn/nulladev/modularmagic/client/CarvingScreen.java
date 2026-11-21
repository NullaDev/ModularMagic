package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.crafting.CarvingRecipe;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.network.EditorNetworking;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class CarvingScreen extends AbstractContainerScreen<ResearchMenu> {
    private String selected="";private int scroll;private Button carve;private long revision=-1;
    public CarvingScreen(ResearchMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=244;imageHeight=232;}
    private Component tr(String key){return Component.translatable("carving.modularmagic."+key);}
    private List<String> choices(){return new ArrayList<>(menu.palette.keySet());}
    private ItemStack result(String key){return CarvingRecipe.all(menu.owner.level()).stream().filter(r->r.key().equals(key)).map(r->r.result().copy()).findFirst().orElse(ItemStack.EMPTY);}
    @Override protected void init(){super.init();menu.layout(imageWidth,imageHeight);carve=addRenderableWidget(Button.builder(tr("carve"),b->{var j=new JsonObject();j.addProperty("op","carve");j.addProperty("id",selected);PacketDistributor.sendToServer(new EditorNetworking.Edit(menu.containerId,menu.revision,j.toString()));}).bounds(leftPos+187,topPos+125,49,20).build());}
    @Override protected void containerTick(){super.containerTick();if(revision!=menu.revision){revision=menu.revision;if(!menu.palette.containsKey(selected))selected="";scroll=Math.clamp(scroll,0,Math.max(0,(choices().size()+6)/7-5));}carve.active=!selected.isEmpty()&&menu.palette.containsKey(selected);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){g.fill(leftPos,topPos,leftPos+244,topPos+232,0xff172332);g.renderOutline(leftPos,topPos,244,232,0xff718d99);g.fill(leftPos+6,topPos+24,leftPos+179,topPos+146,0xff101824);
        for(var s:menu.slots)if(s.isActive()){g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xff0a1520);g.renderOutline(leftPos+s.x-1,topPos+s.y-1,18,18,s.index==9?0xffd6bf83:0xff4d6878);}
        var list=choices();for(int i=scroll*7;i<Math.min(list.size(),(scroll+5)*7);i++){int x=leftPos+9+i%7*24,y=topPos+27+(i/7-scroll)*24;String id=list.get(i);g.fill(x,y,x+22,y+22,id.equals(selected)?0xff486477:0xff263747);g.renderItem(result(id),x+3,y+3);}
        if(!selected.isEmpty()){g.pose().pushPose();g.pose().translate(leftPos+194,topPos+73,0);g.pose().scale(2,2,1);g.renderItem(result(selected),0,0);g.pose().popPose();}
        g.drawString(font,"→",leftPos+208,topPos+34,0xffd2e7ef,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,title,8,8,0xffd8e7ec,false);if(!menu.notice.isEmpty())g.drawString(font,menu.notice.equals("carved")?"✓":"!",224,112,0xffd4be83,false);}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);int x=mx-leftPos-9,y=my-topPos-27;if(x>=0&&x<168&&y>=0&&y<120){int i=(scroll+y/24)*7+x/24;var list=choices();if(i<list.size())g.renderTooltip(font,result(list.get(i)),mx,my);}}
    @Override public boolean mouseClicked(double mx,double my,int button){int x=(int)mx-leftPos-9,y=(int)my-topPos-27;if(button==0&&x>=0&&x<168&&y>=0&&y<120){int i=(scroll+y/24)*7+x/24;var list=choices();if(i<list.size())selected=list.get(i);return true;}return super.mouseClicked(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){if(mx>=leftPos+6&&mx<leftPos+179&&my>=topPos+24&&my<topPos+146){scroll=Math.clamp(scroll-(int)dy,0,Math.max(0,(choices().size()+6)/7-5));return true;}return super.mouseScrolled(mx,my,dx,dy);}
}
