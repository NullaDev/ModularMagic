package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetCustomDataFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** Different exploration pools keep village chests from granting the entire tree. */
@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class ResearchLoot{
    @SubscribeEvent public static void loot(LootTableLoadEvent e){
        if(!e.getName().getNamespace().equals("minecraft"))return;
        String path=e.getName().getPath();int tier;
        if(path.startsWith("chests/village/")||path.equals("chests/simple_dungeon")||path.startsWith("chests/abandoned_mineshaft"))tier=1;
        else if(path.contains("ancient_city")||path.contains("end_city")||path.contains("bastion")||path.contains("stronghold"))tier=3;
        else if(path.contains("desert_pyramid")||path.contains("jungle_temple")||path.contains("nether_bridge")||path.contains("woodland_mansion")||path.contains("trial_chambers"))tier=2;
        else return;
        var pool=LootPool.lootPool().name("modularmagic_research").setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(tier==1?.15f:.3f));
        for(var t:ResearchCatalog.all()){
            int number=Integer.parseInt(t.id().substring(1));boolean early=t.id().startsWith("K")&&number<=8;
            boolean advanced=t.id().startsWith("P")?number>=12:java.util.Set.of(13,14,16,17,21,22,26,27,28,29,30,31).contains(number);
            if(tier==1&&!early||tier==2&&advanced||tier==3&&early)continue;
            var data=new CompoundTag();data.putString("Research",t.id());pool.add(LootItem.lootTableItem(ResearchContent.DIRECT_NOTE.get()).apply(SetCustomDataFunction.setCustomData(data)));
        }
        e.getTable().addPool(pool.build());
    }
    private ResearchLoot(){}
}
