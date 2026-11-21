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
    private static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<cn.nulladev.modularmagic.research.ResearchMenu> runeScreen(cn.nulladev.modularmagic.research.ResearchMenu m,net.minecraft.world.entity.player.Inventory inv,net.minecraft.network.chat.Component title){return m.carving?new CarvingScreen(m,inv,title):new ResearchScreen(m,inv,title);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(cn.nulladev.modularmagic.research.PlaceholderContent.ITEM.get(),cn.nulladev.modularmagic.ModularMagic.id("material"),(stack,level,entity,seed)->cn.nulladev.modularmagic.research.PlaceholderItem.appearance(stack)));}
    private ModularMagicClient() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(cn.nulladev.modularmagic.utility.UtilityContent.MENU.get(),AutoCrafterScreen::new);
        event.register(cn.nulladev.modularmagic.crafting.EnchantingMenu.TYPE.get(),EnchantingScreen::new);
        event.register(ModContent.WRITING_MENU.get(), WritingScreen::new);
        event.register(ModContent.WAND_MENU.get(), WandScreen::new);
        event.register(cn.nulladev.modularmagic.research.ResearchContent.MENU.get(), ModularMagicClient::runeScreen);
        event.register(cn.nulladev.modularmagic.research.PlaceholderContent.MENU.get(), PlaceholderScreen::new);
    }
}
