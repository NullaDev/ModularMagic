package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.generator.*;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Research keeps its finite virtual puzzle separate from physical notes, ink and the player's pack. */
public final class ResearchMenu extends AbstractContainerMenu {
    public final Player owner;
    public final BlockPos pos;
    public final boolean carving;
    public SpellDocument document,reference;
    public String topic="",target="",notice="",selected="";
    public long revision;
    private long sent=-1;
    private final RuneTableBlockEntity table;
    public final net.minecraft.world.SimpleContainer supplies;
    private boolean changingSupplies;
    public boolean reading;
    private final net.minecraft.world.ContainerListener suppliesListener=c->suppliesChanged();
    public boolean paid;
    public int inkCost,inkAvailable;
    public final Map<String,Integer> palette=new LinkedHashMap<>();
    public final Set<String> known=new HashSet<>();
    public final List<String> courses=new ArrayList<>();

    public ResearchMenu(int id,Inventory inv,BlockPos pos,boolean carving){super(ResearchContent.MENU.get(),id);owner=inv.player;this.pos=pos;this.carving=carving;
        table=owner.level().isClientSide?null:(RuneTableBlockEntity)owner.level().getBlockEntity(pos);
        if(!owner.level().isClientSide&&table==null)throw new IllegalStateException("Missing rune table");
        supplies=table==null?new net.minecraft.world.SimpleContainer(RuneTableBlockEntity.SIZE):table.inventory;
        if(table!=null)table.acquire(this);
        for(int i=0;i<RuneTableBlockEntity.SIZE;i++)addSlot(supplySlot(i,-1000,-1000));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(inventorySlot(inv,9+row*9+col,-1000,-1000));
        for(int col=0;col<9;col++)addSlot(inventorySlot(inv,col,-1000,-1000));
        supplies.addListener(suppliesListener);
        if(owner.level().isClientSide)revision=-1;else load();
    }
    private Slot supplySlot(int index,int x,int y){return new Slot(supplies,index,x,y){
        @Override public boolean mayPlace(ItemStack s){if(!isActive()||index==RuneTableBlockEntity.OUTPUT)return false;if(index==0)return carving&&(s.is(ResearchContent.BLANK.get())||s.is(cn.nulladev.modularmagic.crafting.ArtificeContent.BLANK_CORE.get())||s.is(Items.CLAY_BALL));return !carving&&s.is(ResearchContent.INK.get());}
        @Override public boolean isActive(){return carving?(index==0||index==9):(index==0||index==1);}
        @Override public boolean mayPickup(Player p){return isActive()&&(carving||index!=0);}
    };}
    private Slot inventorySlot(Inventory inv,int index,int x,int y){return new Slot(inv,index,x,y){
        @Override public boolean isActive(){return true;}
        @Override public boolean mayPlace(ItemStack s){return true;}
        @Override public boolean mayPickup(Player p){return true;}
    };}
    private void position(int index,Slot slot){slot.index=index;slots.set(index,slot);}
    public void layout(int width,int height){
        if(!carving){position(0,supplySlot(0,width-178,29));position(1,supplySlot(1,width-142,29));for(int i=10;i<46;i++){var old=slots.get(i);int local=i-10;position(i,inventorySlot(owner.getInventory(),old.getContainerSlot(),width-178+(local%9)*18,local<27?height-104+(local/9)*18:height-42));}return;}
        position(0,supplySlot(0,190,29));position(9,supplySlot(9,218,29));
        for(int i=10;i<46;i++){var old=slots.get(i);int local=i-10;position(i,inventorySlot(owner.getInventory(),old.getContainerSlot(),41+(local%9)*18,local<27?150+(local/9)*18:208));}
    }
    private ResearchCatalog.Topic note(){try{if(!(supplies.getItem(0).getItem() instanceof ResearchNoteItem))return null;return ResearchCatalog.get(cn.nulladev.modularmagic.energy.EnergyLinkItem.data(supplies.getItem(0)).getString("Research"));}catch(RuntimeException e){return null;}}
    private void suppliesChanged(){if(owner.level().isClientSide||changingSupplies)return;save();load();revision++;broadcastChanges();}
    private int availableInk(){int have=0;for(int i=1;i<9;i++){var s=supplies.getItem(i);if(s.is(ResearchContent.INK.get()))have+=s.getMaxDamage()-s.getDamageValue();}for(var s:owner.getInventory().items)if(s.is(ResearchContent.INK.get()))have+=s.getMaxDamage()-s.getDamageValue();return have;}
    private boolean useInk(int amount,boolean simulate){if(owner.isCreative())return true;if(availableInk()<amount)return false;if(simulate)return true;
        for(int i=1;i<9&&amount>0;i++){var s=supplies.getItem(i);if(s.is(ResearchContent.INK.get())){int n=Math.min(amount,s.getMaxDamage()-s.getDamageValue());s.setDamageValue(s.getDamageValue()+n);amount-=n;if(s.getDamageValue()>=s.getMaxDamage())s.shrink(1);}}if(amount>0)ResearchContent.ink(owner,amount,false);owner.getInventory().setChanged();supplies.setChanged();return true;
    }
    private void load(){
        inkAvailable=availableInk();
        known.clear();for(var n:NodeCatalog.all())if(ResearchKnowledge.knows(owner,n.id()))known.add(n.id());
        courses.clear();for(var t:ResearchCatalog.all())if(ResearchKnowledge.ready(owner,t)&&!ResearchKnowledge.completed(owner,t)&&ResearchKnowledge.state(owner).getString("observed:"+t.id()).equals("yes"))courses.add(t.id());
        if(carving){palette.clear();for(var recipe:cn.nulladev.modularmagic.crafting.CarvingRecipe.all(owner.level()))if(recipe.ingredient().test(supplies.getItem(0))&&recipe.unlocked(owner))palette.put(recipe.key(),1);return;}
        var note=note();topic=note==null?"":note.id();target="";document=null;reference=null;palette.clear();reading=false;paid=false;inkCost=0;
        if(note==null)return;if(!ResearchKnowledge.ready(owner,note)){notice="prerequisite";return;}if(ResearchKnowledge.completed(owner,note)){notice="complete";return;}
        reading=((ResearchNoteItem)supplies.getItem(0).getItem()).direct||note.nodes().isEmpty()||note.id().equals("K00");if(reading)return;
        target=ResearchCatalog.next(owner,ResearchCatalog.get(topic));if(target.isEmpty())return;
        reference=ResearchCatalog.reference(target);
        for(var n:reference.nodes())palette.merge(n.template,1,Integer::sum);
        // Same puzzle always has the same distractors; reopening cannot reroll its price or pieces.
        var random=new Random((owner.getUUID()+":"+target).hashCode());var candidates=NodeCatalog.all().stream().filter(n->!palette.containsKey(n.id())&&!n.category().equals("root")).toList();
        if(reference.nodes().size()>4)for(int i=0;i<Math.min(2,candidates.size());i++)palette.put(candidates.get(random.nextInt(candidates.size())).id(),1);
        long unknown=reference.nodes().stream().filter(n->!known.contains(n.template)).count();
        inkCost=(int)Math.min(100000,Math.ceil((ResearchContent.INK_BASE.get()+ResearchContent.INK_PER_NODE.get()*reference.nodes().size())*(1+ResearchContent.UNKNOWN_LINEAR.get()*unknown+ResearchContent.UNKNOWN_QUADRATIC.get()*unknown*unknown)));
        paid=ResearchKnowledge.state(owner).getString("paid:"+target).equals("yes");
        try{document=SpellDocument.fromJson(ResearchKnowledge.state(owner).getString("draft:"+target));}catch(RuntimeException e){document=SpellDocument.create(reference.root.template);}
        // A draft can only use this lesson's finite pieces, even if catalogues change during development.
        if(!validPieces(document))document=SpellDocument.create(reference.root.template);
        ResearchKnowledge.save(owner,"active",topic);
    }
    public int remaining(String id){return palette.getOrDefault(id,0)-(document==null?0:(int)document.nodes().stream().filter(n->n.template.equals(id)).count());}
    private boolean validPieces(SpellDocument d){var counts=new HashMap<String,Integer>();for(var n:d.nodes())counts.merge(n.template,1,Integer::sum);return counts.entrySet().stream().allMatch(e->e.getValue()<=palette.getOrDefault(e.getKey(),0));}
    private void save(){if(document!=null&&!target.isEmpty())ResearchKnowledge.save(owner,"draft:"+target,document.toJson());}
    public void apply(long expected,String command){
        if(owner.level().isClientSide||!stillValid(owner)||expected!=revision)return;
        changingSupplies=true;
        try{if(command.length()>32768)return;var j=JsonParser.parseString(command).getAsJsonObject();String op=j.get("op").getAsString();notice="";
            if(op.equals("start")&&!carving&&reading){var t=note();if(t!=null&&ResearchKnowledge.ready(owner,t)){t.nodes().forEach(n->ResearchKnowledge.learn(owner,n));ResearchKnowledge.learn(owner,t.id());if(!owner.isCreative())supplies.removeItem(0,1);notice="success";load();}}
            else if(op.equals("carve")&&carving){carve(j.get("id").getAsString());}
            else if(!carving&&document!=null){
                if(op.equals("start")){if(!paid){if(!useInk(inkCost,false)){notice="ink";}else{paid=true;ResearchKnowledge.save(owner,"paid:"+target,"yes");}}}
                else if(op.equals("check")&&paid){if(ResearchCatalog.equivalent(document.root,reference.root)){
                    for(var n:reference.nodes())ResearchKnowledge.learn(owner,n.template);ResearchKnowledge.save(owner,"draft:"+target,"");ResearchKnowledge.save(owner,"paid:"+target,"");if(ResearchKnowledge.completed(owner,ResearchCatalog.get(topic))&&!owner.isCreative())supplies.removeItem(0,1);notice="success";load();
                }else notice="mismatch";}
                else if(paid){var copy=SpellDocument.fromJson(document.toJson());
                    if(op.equals("insert")){String template=j.get("template").getAsString();if(remaining(template)<=0)throw new IllegalArgumentException();copy.insert(j.get("parent").getAsString(),j.get("slot").getAsString(),j.get("index").getAsInt(),template);}
                    else if(op.equals("remove"))copy.remove(j.get("node").getAsString());
                    else if(op.equals("parameters")){var map=new LinkedHashMap<String,String>();j.getAsJsonObject("values").entrySet().forEach(e->map.put(e.getKey(),e.getValue().getAsString()));copy.setParameters(j.get("node").getAsString(),map);}
                    if(!validPieces(copy))throw new IllegalArgumentException();document=copy;save();
                }
            }
        }catch(RuntimeException e){notice="invalid";}finally{changingSupplies=false;}
        inkAvailable=availableInk();revision++;broadcastChanges();
    }
    public static Map<Item,Integer> carvingCost(String id){
        var costs=new LinkedHashMap<Item,Integer>();costs.put(ResearchContent.BLANK.get(),1);
        return costs;
    }
    private void carve(String id){
        if(!palette.containsKey(id))throw new IllegalArgumentException();var costs=carvingCost(id);
        var recipe=cn.nulladev.modularmagic.crafting.CarvingRecipe.all(owner.level()).stream().filter(r->r.key().equals(id)&&r.ingredient().test(supplies.getItem(0))&&r.unlocked(owner)).findFirst().orElseThrow();
        ItemStack result=recipe.result().copy();
        var output=supplies.getItem(9);if(!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()+result.getCount()>output.getMaxStackSize())){notice="output";return;}
        if(!owner.isCreative()){if(!recipe.ingredient().test(supplies.getItem(0))){notice="materials";return;}supplies.removeItem(0,1);}
        if(output.isEmpty())supplies.setItem(9,result);else{output.grow(result.getCount());supplies.setChanged();}load();notice="carved";
    }
    private String metadata(){var j=new JsonObject();j.addProperty("topic",topic);j.addProperty("target",target);j.addProperty("paid",paid);j.addProperty("reading",reading);j.addProperty("ink",inkCost);j.addProperty("inkAvailable",inkAvailable);j.addProperty("notice",notice);var a=new JsonObject();palette.forEach(a::addProperty);j.add("palette",a);var k=new JsonArray();known.forEach(k::add);j.add("known",k);var c=new JsonArray();courses.forEach(c::add);j.add("courses",c);return j.toString();}
    @Override public void broadcastChanges(){super.broadcastChanges();if(owner instanceof ServerPlayer p&&sent!=revision){sent=revision;PacketDistributor.sendToPlayer(p,new EditorNetworking.Snapshot(containerId,revision,document==null?"":document.toJson(),paid,metadata()));}}
    public void receive(EditorNetworking.Snapshot s){revision=s.revision();document=s.document().isEmpty()?null:SpellDocument.fromJson(s.document());var j=JsonParser.parseString(s.status()).getAsJsonObject();topic=j.get("topic").getAsString();target=j.get("target").getAsString();reference=target.isEmpty()?null:ResearchCatalog.reference(target);paid=j.get("paid").getAsBoolean();reading=j.get("reading").getAsBoolean();inkCost=j.get("ink").getAsInt();inkAvailable=j.get("inkAvailable").getAsInt();notice=j.get("notice").getAsString();palette.clear();j.getAsJsonObject("palette").entrySet().forEach(e->palette.put(e.getKey(),e.getValue().getAsInt()));known.clear();j.getAsJsonArray("known").forEach(e->known.add(e.getAsString()));courses.clear();j.getAsJsonArray("courses").forEach(e->courses.add(e.getAsString()));}
    @Override public boolean stillValid(Player p){return p==owner&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64&&p.level().getBlockState(pos).is(carving?ResearchContent.CARVER.get():ResearchContent.TABLE.get());}
    public void refreshKnowledge(){notice="";load();revision++;broadcastChanges();}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(p)||!slot.isActive())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();boolean moved;
        if(index<10)moved=moveItemStackTo(stack,10,46,true);else if(stack.is(ResearchContent.INK.get())){moved=moveItemStackTo(stack,1,2,false);if(!moved)moved=moveItemStackTo(stack,2,9,false);}else if(carving&&(stack.is(ResearchContent.BLANK.get())||stack.is(cn.nulladev.modularmagic.crafting.ArtificeContent.BLANK_CORE.get())||stack.is(Items.CLAY_BALL)))moved=moveItemStackTo(stack,0,1,false);else moved=false;
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
    @Override public void removed(Player p){
        supplies.removeListener(suppliesListener);
        if(!p.level().isClientSide&&!carving){
            var current=note();if(current!=null){var state=ResearchKnowledge.state(p).copy();for(String id:current.nodes()){state.remove("draft:"+id);state.remove("paid:"+id);}state.remove("active");p.setData(ResearchKnowledge.DATA,state);}
            var returned=supplies.removeItemNoUpdate(0);if(!returned.isEmpty()&&!p.getInventory().add(returned))p.drop(returned,false);
        }
        if(table!=null)table.release(this);super.removed(p);
    }
}
