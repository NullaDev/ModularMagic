package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** Exploration rewards can be spent on discoveries or recycled into aether. */
@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class ResearchLoot{
    @SubscribeEvent public static void loot(LootTableLoadEvent e){
        if(!e.getName().getNamespace().equals("minecraft"))return;
        ExampleSpellLoot.addTo(e);
        String path=e.getName().getPath();float chance;
        if(path.startsWith("chests/village/")||path.equals("chests/simple_dungeon")||path.startsWith("chests/abandoned_mineshaft"))chance=.15f;
        else if(path.contains("ancient_city")||path.contains("end_city")||path.contains("bastion")||path.contains("stronghold")
                ||path.contains("desert_pyramid")||path.contains("jungle_temple")||path.contains("nether_bridge")
                ||path.contains("woodland_mansion")||path.contains("trial_chambers"))chance=.3f;
        else return;
        var pool=LootPool.lootPool().name("modularmagic_research")
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(ResearchContent.KNOWLEDGE_FRAGMENT.get()));
        e.getTable().addPool(pool.build());
    }
    private ResearchLoot(){}
}
