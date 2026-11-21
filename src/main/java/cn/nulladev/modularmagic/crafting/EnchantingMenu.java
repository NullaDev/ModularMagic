package cn.nulladev.modularmagic.crafting;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.network.EditorNetworking;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.*;

public final class EnchantingMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ModularMagic.MOD_ID);
    public static final DeferredHolder<MenuType<?>,MenuType<EnchantingMenu>> TYPE=MENUS.register("aether_enchanting",()->IMenuTypeExtension.create((id,inv,buf)->new EnchantingMenu(id,inv,buf.readBlockPos())));
    public static void register(IEventBus bus){MENUS.register(bus);}
    public final Player player;public final BlockPos pos;private final EnergyBlockEntity device;private final Container inventory;
    public int selected=-1;public long energy,cost,revision;public boolean possible,paused;public String notice="";private String sent="";
    public EnchantingMenu(int id,Inventory inv,BlockPos pos){
        super(TYPE.get(),id);player=inv.player;this.pos=pos;device=player.level().getBlockEntity(pos) instanceof EnergyBlockEntity be?be:null;
        inventory=player.level().isClientSide?new SimpleContainer(28):device.enchantInventory;
        addSlot(new Slot(inventory,0,16,25){@Override public boolean mayPlace(ItemStack s){return s.isDamageableItem()||s.isEnchantable();}@Override public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,1+row*9+col,16+col*18,70+row*18){@Override public boolean mayPlace(ItemStack s){return !AetherEnchanting.enchantments(s).isEmpty();}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,16+col*18,148+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,16+col*18,206));
    }
    public static void open(Player player,BlockPos pos){if(player instanceof ServerPlayer p&&p.level().getBlockEntity(pos) instanceof EnergyBlockEntity be&&be.kind()==EnergyContent.Kind.ENCHANTER&&be.allowed(p))p.openMenu(new SimpleMenuProvider((id,inv,user)->new EnchantingMenu(id,inv,pos),Component.translatable("block.modularmagic.aether_enchanter")),buf->buf.writeBlockPos(pos));}
    public AetherEnchanting.Quote quote(){return selected>=1&&selected<28?AetherEnchanting.quote(inventory.getItem(0),inventory.getItem(selected)):new AetherEnchanting.Quote(ItemStack.EMPTY,0,0);}
    @Override public boolean clickMenuButton(Player p,int id){
        if(p!=player||!stillValid(p)||p.level().isClientSide)return false;
        notice="";
        if(id>=1&&id<28){selected=id;broadcastChanges();return true;}
        if(id!=100)return false;
        var quote=quote();if(quote.result().isEmpty()){notice="invalid";broadcastChanges();return false;}
        long needed=Math.multiplyExact(quote.aether(),ManaStorage.SCALE);
        if(device.paused()||device.energy<needed){notice="energy";broadcastChanges();return false;}
        device.energy-=needed;inventory.setItem(0,quote.result());device.changed();notice="success";broadcastChanges();return true;
    }
    @Override public void broadcastChanges(){super.broadcastChanges();if(!(player instanceof ServerPlayer p)||device==null)return;
        var q=quote();energy=device.energy/ManaStorage.SCALE;cost=q.aether();possible=!q.result().isEmpty();paused=device.paused();
        var j=new JsonObject();j.addProperty("energy",energy);j.addProperty("cost",cost);j.addProperty("selected",selected);j.addProperty("possible",possible);j.addProperty("paused",paused);j.addProperty("notice",notice);
        String next=j.toString();if(!next.equals(sent)){sent=next;PacketDistributor.sendToPlayer(p,new EditorNetworking.Snapshot(containerId,++revision,"",true,next));}
    }
    public void receive(EditorNetworking.Snapshot snapshot){var j=JsonParser.parseString(snapshot.status()).getAsJsonObject();revision=snapshot.revision();energy=j.get("energy").getAsLong();cost=j.get("cost").getAsLong();selected=j.get("selected").getAsInt();possible=j.get("possible").getAsBoolean();paused=j.get("paused").getAsBoolean();notice=j.get("notice").getAsString();}
    @Override public boolean stillValid(Player p){return p==player&&p.distanceToSqr(pos.getCenter())<=64&&p.level().getBlockState(pos).is(EnergyContent.BLOCKS.get(EnergyContent.Kind.ENCHANTER).get());}
    @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();boolean moved;
        if(index<28)moved=moveItemStackTo(s,28,64,true);else if(slots.get(0).mayPlace(s)&&!slots.get(0).hasItem())moved=moveItemStackTo(s,0,1,false);else moved=moveItemStackTo(s,1,28,false);
        if(!moved)return ItemStack.EMPTY;if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return copy;
    }
}
