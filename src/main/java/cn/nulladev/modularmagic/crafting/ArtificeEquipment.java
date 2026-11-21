package cn.nulladev.modularmagic.crafting;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.spell.billing.ManaEscrow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class ArtificeEquipment {
    public static boolean spend(ServerPlayer p,long aether){
        if(p.isCreative())return true;long units=Math.multiplyExact(aether,ManaStorage.SCALE),available=0;
        var stacks=new ArrayList<ItemStack>();stacks.addAll(p.getInventory().items);stacks.addAll(p.getInventory().offhand);
        for(var s:stacks)if(s.getItem() instanceof ManaStorage m){if(m.infinite(s))return true;ManaEscrow.get(p.getServer()).claim(s);available=Math.min(Long.MAX_VALUE/2,available+m.extractUnits(s,Long.MAX_VALUE,true));}
        if(available<units)return false;
        for(var s:stacks)if(units>0&&s.getItem() instanceof ManaStorage m)units-=m.extractUnits(s,units,false);
        p.getInventory().setChanged();return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.level().getGameTime()%ArtificeConfig.REPAIR_INTERVAL.get()!=0)return;
        repair(p);
    }
    public static void repair(ServerPlayer p){
        var gear=new ArrayList<ItemStack>();gear.addAll(p.getInventory().items);gear.addAll(p.getInventory().armor);gear.addAll(p.getInventory().offhand);
        for(var stack:gear)if(ArtificeContent.gear(stack)&&stack.isDamaged()){
            int points=Math.min(stack.getDamageValue(),ArtificeConfig.REPAIR_POINTS.get());
            if(spend(p,points*ArtificeConfig.REPAIR_PER_POINT.get())){stack.setDamageValue(stack.getDamageValue()-points);p.getInventory().setChanged();}
        }
    }
    private ArtificeEquipment(){}
}
