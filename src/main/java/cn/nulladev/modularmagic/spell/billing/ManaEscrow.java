package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.item.ManaContainerItem;
import cn.nulladev.modularmagic.mana.ManaStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Durable unspent reservations and refunds addressed to the source item, never a player mana pool. */
public final class ManaEscrow extends SavedData {
    private final Map<UUID, LinkedHashMap<UUID,Long>> holds=new LinkedHashMap<>();
    private final Map<UUID,Long> credits=new HashMap<>();
    private static final String ID="modularmagic_mana_id";
    public static ManaEscrow get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ManaEscrow::new,ManaEscrow::load,null),"modularmagic_mana_escrow");
    }
    public static UUID identity(ItemStack stack) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if (!tag.hasUUID(ID)) { tag.putUUID(ID,UUID.randomUUID()); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); }
        return tag.getUUID(ID);
    }
    public void open(UUID cast, LinkedHashMap<UUID,Long> sources) { holds.put(cast,new LinkedHashMap<>(sources)); setDirty(); }
    public void spend(UUID cast,long amount) {
        var sources=holds.get(cast); if (sources==null) throw new IllegalStateException("Missing reservation");
        for (var entry:sources.entrySet()) { long paid=Math.min(amount,entry.getValue()); entry.setValue(entry.getValue()-paid); amount-=paid; if(amount==0) break; }
        if(amount!=0) throw new IllegalStateException("Reservation exhausted"); setDirty();
    }
    public void release(UUID cast) {
        var sources=holds.remove(cast); if(sources==null)return;
        sources.forEach((id,n)->{if(n>0)credits.merge(id,n,Math::addExact);}); setDirty();
    }
    public void release(UUID cast,double refundRatio){
        var sources=holds.get(cast);if(sources==null)return;
        if(refundRatio<1){long unused=0;for(long amount:sources.values())unused=Math.addExact(unused,amount);
            long refund=cn.nulladev.modularmagic.spell.ResourceBalance.refund(unused/ManaStorage.SCALE,refundRatio)*ManaStorage.SCALE;
            if(unused>refund)spend(cast,unused-refund);
        }
        release(cast);
    }
    public long outstanding(UUID source) {
        long amount=credits.getOrDefault(source,0L);
        for(var sources:holds.values()) amount=Math.addExact(amount,sources.getOrDefault(source,0L));
        return amount;
    }
    public void claim(ItemStack stack) {
        if(!(stack.getItem() instanceof ManaContainerItem item)||item.infinite(stack))return;
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); if(!tag.hasUUID(ID))return;
        UUID id=tag.getUUID(ID); long credit=credits.getOrDefault(id,0L);
        long moved=Math.min(credit,Math.max(0,item.capacity(stack)*ManaStorage.SCALE-item.storedUnits(stack)));
        if(moved>0) { ManaContainerItem.writeUnits(stack,item.storedUnits(stack)+moved); credit-=moved; if(credit==0)credits.remove(id);else credits.put(id,credit);setDirty(); }
        ManaContainerItem.reserved(stack,outstanding(id));
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        var pending=new CompoundTag();credits.forEach((id,n)->pending.putLong(id.toString(),n));tag.put("credits",pending);
        var active=new CompoundTag();holds.forEach((cast,sources)->{var row=new CompoundTag();sources.forEach((id,n)->row.putLong(id.toString(),n));active.put(cast.toString(),row);});tag.put("holds",active);return tag;
    }
    private static ManaEscrow load(CompoundTag tag,HolderLookup.Provider registries) {
        var result=new ManaEscrow();var credits=tag.getCompound("credits");
        for(String id:credits.getAllKeys())result.credits.put(UUID.fromString(id),Math.max(0,credits.getLong(id)));
        // No runnable contracts survive a server restart. Return each orphaned reservation once.
        var active=tag.getCompound("holds");for(String cast:active.getAllKeys()){var row=active.getCompound(cast);for(String id:row.getAllKeys())result.credits.merge(UUID.fromString(id),Math.max(0,row.getLong(id)),Math::addExact);}
        result.setDirty();return result;
    }
}
