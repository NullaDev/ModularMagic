package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid=ModularMagic.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EnergyOverlay {
    @SubscribeEvent public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ModularMagic.id("energy_supply"),(g,t)->{
            var mc=Minecraft.getInstance();
            if(mc.player==null||mc.options.hideGui||mc.player.isSpectator())return;
            var held=mc.player.getMainHandItem();
            if(!(held.getItem() instanceof AetherShardItem shard&&!shard.stable))held=mc.player.getOffhandItem();
            if(held.getItem() instanceof AetherShardItem shard&&!shard.stable) {
                var label=AetherShardItem.countdown(held,mc.level);
                g.drawCenteredString(mc.font,label,g.guiWidth()/2,g.guiHeight()-65,0xffa5e6ef);
            }
            if(!ResourceHud.visible())return;
            for(var list:java.util.List.of(mc.player.getInventory().items,mc.player.getInventory().offhand))for(var stack:list) {
                if(!stack.is(EnergyContent.RECEIVER.get()))continue;
                var data=EnergyLinkItem.data(stack);
                g.drawString(mc.font,Component.translatable(data.getBoolean("Connected")?"energy.modularmagic.supply":"energy.modularmagic.disconnected",AetherNumbers.format(data.getLong("DeliveredPerSecond"))),8,g.guiHeight()-61,0xffa5e6ef,false);return;
            }
        });
    }
}
