package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.research.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid=ModularMagic.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ResearchLensHud{
    @SubscribeEvent public static void register(RegisterGuiLayersEvent e){e.registerAboveAll(ModularMagic.id("lens"),(g,delta)->{
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||mc.options.hideGui||!(p.getMainHandItem().is(ResearchContent.LENS.get())||p.getOffhandItem().is(ResearchContent.LENS.get())))return;
        var from=p.getEyePosition();var to=from.add(p.getViewVector(1).scale(p.blockInteractionRange()));var hit=p.level().clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,p));if(hit.getType()!=HitResult.Type.BLOCK)return;
        var topics=ResearchCatalog.source(BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(hit.getBlockPos()).getBlock()).toString());
        String state=topics.isEmpty()?"no_value":topics.stream().allMatch(t->ResearchKnowledge.completed(p,t))?"complete":topics.stream().anyMatch(t->!ResearchKnowledge.completed(p,t)&&ResearchKnowledge.ready(p,t))?"available":"prerequisite";
        var text=Component.translatable("research.modularmagic."+state);int x=(g.guiWidth()-mc.font.width(text))/2,y=g.guiHeight()/2+20;g.fill(x-5,y-3,x+mc.font.width(text)+5,y+12,0xb0192736);g.drawString(mc.font,text,x,y,0xffb4e6ec,false);
    });}
    private ResearchLensHud(){}
}
