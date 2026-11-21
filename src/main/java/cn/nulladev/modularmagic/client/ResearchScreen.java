package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Virtual research pieces and physical supplies share a screen without sharing slots. */
public final class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {
    private record Row(SpellDocument.Node node, SpellDocument.Node parent, NodeTemplate.ChildSlot slot, int depth) {}
    private final List<Row> rows=new ArrayList<>();
    private final Map<String,EditBox> fields=new LinkedHashMap<>();
    private String selectedNode="",selectedPiece="",shownTarget="",feedback="";
    private int treeScroll,paletteScroll,fieldPage;
    private long shownRevision=-2;
    private boolean referenceView,editing;
    public ResearchScreen(ResearchMenu m,Inventory inv,Component title){super(m,inv,title);}
    private int side(){return imageWidth-194;}
    private int columns(){return 7;}
    private int treeBottom(){return imageHeight-(menu.carving?62:52);}
    private int paletteRows(){return Math.max(1,menu.carving?Math.min(4,(imageHeight-155)/24):(imageHeight-184)/24);}
    private Component tr(String key,Object...args){return Component.translatable("research.modularmagic."+key,args);}
    private ItemStack piece(String id){return ModContent.NODES.get(id).toStack();}
    private void command(JsonObject j){PacketDistributor.sendToServer(new EditorNetworking.Edit(menu.containerId,menu.revision,j.toString()));}
    private void op(String op){var j=new JsonObject();j.addProperty("op",op);command(j);}
    private Component label(String id){if(menu.carving||menu.known.contains(id))return Component.translatable(NodeCatalog.get(id).translationKey());var unknown=menu.palette.keySet().stream().filter(s->!menu.known.contains(s)).toList();return tr("unknown",unknown.indexOf(id)+1);}
    @Override protected void init(){imageWidth=Math.min(width-16,620);imageHeight=Math.min(height-16,400);super.init();menu.layout(imageWidth,imageHeight);rebuild();}
    private void gather(SpellDocument.Node n,int depth){rows.add(new Row(n,null,null,depth));for(var slot:n.schema().slots()){var children=n.children.get(slot.key());for(var child:children)gather(child,depth+1);if(!referenceView&&(children.isEmpty()||slot.list()))rows.add(new Row(null,n,slot,depth+1));}}
    private Button button(Component text,int x,int y,int w,Runnable run){return addRenderableWidget(Button.builder(text,b->run.run()).bounds(leftPos+x,topPos+y,w,20).build());}
    private void rebuild(){
        clearWidgets();fields.clear();shownRevision=menu.revision;rows.clear();
        if(!shownTarget.equals(menu.target)){shownTarget=menu.target;selectedNode="";selectedPiece="";editing=false;referenceView=false;treeScroll=0;fieldPage=0;}
        var displayed=referenceView?menu.reference:menu.document;if(displayed!=null)gather(displayed.root,0);
        if(menu.carving&&selectedPiece.isEmpty()&&!menu.palette.isEmpty())selectedPiece=menu.palette.keySet().iterator().next();
        var action=button(tr(menu.carving?"carve":menu.reading?"read":menu.paid?"check":"begin"),8,imageHeight-26,Math.max(70,side()-36),()->{feedback="";if(menu.carving){var j=new JsonObject();j.addProperty("op","carve");j.addProperty("id",selectedPiece);command(j);}else{editing=false;referenceView=false;op(menu.paid?"check":"start");rebuild();}});
        action.active=menu.revision>=0&&(menu.carving?!selectedPiece.isEmpty():menu.reading||menu.document!=null);
        if(!menu.carving){
            if(menu.document!=null&&!editing){button(tr("reference"),side()-174,29,82,()->{referenceView=true;treeScroll=0;rebuild();}).active=!referenceView;button(tr("draft"),side()-88,29,80,()->{referenceView=false;treeScroll=0;rebuild();}).active=referenceView;}
            if(!menu.paid&&menu.document!=null)action.setTooltip(Tooltip.create(tr("ink_required",menu.inkCost)));
        }
        if(editing&&menu.document!=null&&!selectedNode.isEmpty()){
            SpellDocument.Node n;try{n=menu.document.find(selectedNode);}catch(RuntimeException e){editing=false;rebuild();return;}
            var fs=n.schema().fields();int perPage=Math.max(1,(treeBottom()-105)/35),pages=Math.max(1,(fs.size()+perPage-1)/perPage);fieldPage=Math.min(fieldPage,pages-1);int start=fieldPage*perPage;
            for(int i=start;i<Math.min(start+perPage,fs.size());i++){var f=fs.get(i);var edit=new EditBox(font,leftPos+14,topPos+83+(i-start)*35,side()-30,18,Component.translatable("parameter.modularmagic."+f.key()));edit.setMaxLength(SpellDocument.MAX_FIELD);edit.setValue(n.parameters.getOrDefault(f.key(),""));fields.put(f.key(),edit);addRenderableWidget(edit);}
            button(tr("back"),8,29,54,()->{editing=false;rebuild();});
            button(tr("save_values"),side()-92,29,80,()->{var j=new JsonObject();j.addProperty("op","parameters");j.addProperty("node",selectedNode);var values=new JsonObject();fields.forEach((key,e)->values.addProperty(key,e.getValue()));j.add("values",values);command(j);editing=false;feedback="";rebuild();});
            if(pages>1)button(Component.literal((fieldPage+1)+"/"+pages+" >"),side()-72,treeBottom()-23,60,()->{fieldPage=(fieldPage+1)%pages;rebuild();});
        }
    }
    @Override protected void containerTick(){super.containerTick();if(shownRevision!=menu.revision){feedback="";rebuild();}}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff171e2b);g.renderOutline(leftPos,topPos,imageWidth,imageHeight,0xff718da1);int edge=leftPos+side();
        g.fill(leftPos+5,topPos+50,edge-5,topPos+treeBottom(),0xff0d131e);g.fill(edge+4,topPos+53,leftPos+imageWidth-5,topPos+(menu.carving?imageHeight-100:imageHeight-122),0xff101824);
        for(var s:menu.slots)if(s.isActive()){g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xff09121c);g.renderOutline(leftPos+s.x-1,topPos+s.y-1,18,18,s.index==9?0xffd1bb7a:0xff4d6578);}
        if(menu.carving){var blank=menu.slots.get(0);if(!blank.hasItem()){g.setColor(1,1,1,.3f);g.renderItem(ResearchContent.BLANK.toStack(),leftPos+blank.x,topPos+blank.y);g.setColor(1,1,1,1);}g.drawString(font,"→",leftPos+imageWidth-63,topPos+33,0xffb4ccda,false);if(!selectedPiece.isEmpty()){g.pose().pushPose();g.pose().translate(leftPos+side()/2-24,topPos+75,0);g.pose().scale(3,3,1);g.renderItem(piece(selectedPiece),0,0);g.pose().popPose();}return;}
        if(!menu.slots.get(1).hasItem()){var ink=menu.slots.get(1);g.setColor(1,1,1,.3f);g.renderItem(ResearchContent.INK.toStack(),leftPos+ink.x,topPos+ink.y);g.setColor(1,1,1,1);}
        if(menu.document==null){g.renderItem(ResearchContent.NOTE.toStack(),leftPos+side()/2-8,topPos+imageHeight/2-32);var message=tr(menu.reading?"reading_prompt":menu.topic.isEmpty()?"no_note":"status_"+(menu.notice.isEmpty()?"complete":menu.notice));g.drawWordWrap(font,message,leftPos+28,topPos+imageHeight/2,side()-56,0xffc5d3e2);return;}
        if(editing)return;int visible=Math.max(1,(treeBottom()-54)/21);treeScroll=Math.clamp(treeScroll,0,Math.max(0,rows.size()-visible));
        for(int i=treeScroll;i<Math.min(rows.size(),treeScroll+visible);i++){var r=rows.get(i);int y=topPos+53+(i-treeScroll)*21,x=leftPos+9+Math.min(8,r.depth)*12;String category=r.node!=null?r.node.schema().category():r.slot.category();int color=0xff000000|NodeCatalog.category(category).rgb();g.fill(x,y,edge-8,y+19,r.node!=null&&r.node.id.equals(selectedNode)?0xff344c64:0xff212b3b);g.fill(x,y,x+3,y+19,color);Component text;if(r.node!=null){g.renderItem(piece(r.node.template),x+4,y+1);text=label(r.node.template);}else text=Component.literal("+ ").append(Component.translatable("slot.modularmagic."+r.slot.key()));g.drawString(font,font.plainSubstrByWidth(text.getString(),Math.max(20,edge-x-31)),x+23,y+6,menu.paid||referenceView?0xffe1eaf2:0xff8897a8,false);}
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,title,8,9,0xffd6e5ef,false);if(!menu.topic.isEmpty()&&!editing){int limit=menu.carving||menu.document==null?side()-16:side()-190;g.drawString(font,font.plainSubstrByWidth(Component.translatable(ResearchCatalog.get(menu.topic).key()).getString(),Math.max(40,limit)),8,34,0xffbbd2e8,false);}
        var list=new ArrayList<>(menu.palette.keySet());int count=columns()*paletteRows();paletteScroll=Math.clamp(paletteScroll,0,Math.max(0,(list.size()+columns()-1)/columns()-paletteRows()));
        for(int i=paletteScroll*columns();i<Math.min(list.size(),paletteScroll*columns()+count);i++){String id=list.get(i);int local=i-paletteScroll*columns(),x=imageWidth-178+local%columns()*24,y=57+local/columns()*24;int color=0xff000000|NodeCatalog.category(NodeCatalog.get(id).category()).rgb();g.fill(x,y,x+22,y+22,id.equals(selectedPiece)?0xff506679:0xff263547);g.renderOutline(x,y,22,22,color);g.renderItem(piece(id),x+3,y+2);if(!menu.carving)g.drawString(font,Integer.toString(menu.remaining(id)),x+15,y+14,menu.remaining(id)>0?0xffffffff:0xff7c8792,false);}
        for(var e:fields.entrySet())g.drawString(font,Component.translatable("parameter.modularmagic."+e.getKey()),14,e.getValue().getY()-topPos-11,0xffc2cfe0,false);
        if(!menu.carving)g.drawString(font,tr(menu.owner.isCreative()?"creative_ink":"ink_balance",menu.inkAvailable,menu.inkCost),imageWidth-178,imageHeight-22,0xffb4c7da,false);
        String status=feedback.isEmpty()?menu.notice:feedback;if(!status.isEmpty()){int y=imageHeight-(menu.carving?46:43);g.fill(7,y-2,side()-6,y+11,0xff171e2b);g.drawString(font,font.plainSubstrByWidth(tr("status_"+status).getString(),side()-20),10,y,0xffddc591,false);}
    }
    private int paletteIndex(double mx,double my){double x=mx-leftPos-(imageWidth-178),y=my-topPos-57;if(x<0||x>=columns()*24||y<0||y>=paletteRows()*24)return -1;return (paletteScroll+(int)y/24)*columns()+(int)x/24;}
    private Row rowAt(double mx,double my){if(editing||mx<leftPos+7||mx>=leftPos+side()-5||my<topPos+53||my>=topPos+treeBottom())return null;int index=treeScroll+((int)my-topPos-53)/21;return index<rows.size()?rows.get(index):null;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        super.render(g,mx,my,partial);renderTooltip(g,mx,my);int index=paletteIndex(mx,my);var list=new ArrayList<>(menu.palette.keySet());
        if(index>=0&&index<list.size()){String id=list.get(index);var lines=new ArrayList<Component>();lines.add(label(id));if(!menu.carving){lines.add(tr("pieces_remaining",menu.remaining(id)));}g.renderComponentTooltip(font,lines,mx,my);}
        var r=rowAt(mx,my);if(r!=null){var lines=new ArrayList<Component>();if(r.node==null)lines.add(Component.translatable("slot.modularmagic."+r.slot.key()));else{lines.add(label(r.node.template));if(referenceView)r.node.parameters.forEach((key,value)->{if(!value.isBlank())lines.add(Component.translatable("parameter.modularmagic."+key).append(": ").append(font.plainSubstrByWidth(value,Math.max(120,width-160))));});}g.renderComponentTooltip(font,lines,mx,my);}
    }
    @Override public boolean mouseClicked(double mx,double my,int mouseButton){
        if(mouseButton>1)return super.mouseClicked(mx,my,mouseButton);int index=paletteIndex(mx,my);var list=new ArrayList<>(menu.palette.keySet());
        if(index>=0&&index<list.size()){String id=list.get(index);if(mouseButton==1)selectedPiece="";else if(!menu.carving&&menu.remaining(id)<=0)feedback="piece_spent";else{selectedPiece=id;feedback="";if(referenceView){referenceView=false;treeScroll=0;}}rebuild();return true;}
        if(editing&&mouseButton==1&&mx<leftPos+side()&&my>=topPos+50&&my<topPos+treeBottom()){editing=false;rebuild();return true;}
        var r=rowAt(mx,my);if(!menu.carving&&r!=null){
            if(referenceView){feedback="reference_only";return true;}if(!menu.paid){feedback="start_first";return true;}
            if(r.node!=null){selectedNode=r.node.id;fieldPage=0;if(mouseButton==1){if(r.node==menu.document.root)feedback="root_fixed";else if(r.node.hasChildren())feedback="children_first";else{var j=new JsonObject();j.addProperty("op","remove");j.addProperty("node",r.node.id);command(j);feedback="";}}else{editing=!r.node.schema().fields().isEmpty();feedback="";}}
            else if(mouseButton==0){if(selectedPiece.isEmpty())feedback="choose_piece";else if(!NodeCatalog.get(selectedPiece).category().equals(r.slot.category()))feedback="wrong_type";else if(menu.remaining(selectedPiece)<=0)feedback="piece_spent";else{var j=new JsonObject();j.addProperty("op","insert");j.addProperty("parent",r.parent.id);j.addProperty("slot",r.slot.key());j.addProperty("index",r.parent.children.get(r.slot.key()).size());j.addProperty("template",selectedPiece);command(j);feedback="";}}
            rebuild();return true;
        }
        return super.mouseClicked(mx,my,mouseButton);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){if(x>leftPos+side()&&y>=topPos+57&&y<topPos+(menu.carving?imageHeight-100:imageHeight-52))paletteScroll=Math.max(0,paletteScroll-(int)dy);else if(x<leftPos+side()&&!editing&&y>=topPos+53&&y<topPos+treeBottom())treeScroll=Math.max(0,treeScroll-(int)dy);else return super.mouseScrolled(x,y,dx,dy);return true;}
}