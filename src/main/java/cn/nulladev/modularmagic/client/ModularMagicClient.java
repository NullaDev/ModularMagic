package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ModularMagic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModularMagicClient {
    @SubscribeEvent
    public static void energyRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(cn.nulladev.modularmagic.energy.EnergyContent.ENTITY.get(), EnergyRenderer::new);
    }
    private ModularMagicClient() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContent.WRITING_MENU.get(), WritingScreen::new);
        event.register(ModContent.WAND_MENU.get(), WandScreen::new);
    }
}
