package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import cn.nulladev.modularmagic.energy.EnergyContent;
import cn.nulladev.modularmagic.research.MimicFamily;
import cn.nulladev.modularmagic.research.PlaceholderContent;
import cn.nulladev.modularmagic.research.PlaceholderItem;

@EventBusSubscriber(modid = ModularMagic.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModularMagicClient {
    @SubscribeEvent
    public static void energyRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(EnergyContent.ENTITY.get(), EnergyRenderer::new);
    }
    @SubscribeEvent
    public static void energyParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(EnergyContent.RESONANCE_GLOW.get(), GlowParticle.Provider::new);
    }
    private static net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<cn.nulladev.modularmagic.research.ResearchMenu> runeScreen(cn.nulladev.modularmagic.research.ResearchMenu m,net.minecraft.world.entity.player.Inventory inv,net.minecraft.network.chat.Component title){return m.carving?new CarvingScreen(m,inv,title):new ResearchScreen(m,inv,title);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(cn.nulladev.modularmagic.research.PlaceholderContent.ITEM.get(),cn.nulladev.modularmagic.ModularMagic.id("material"),(stack,level,entity,seed)->cn.nulladev.modularmagic.research.PlaceholderItem.appearance(stack)));}
    @SubscribeEvent
    public static void sampleIcons(RegisterItemDecorationsEvent event) {
        net.neoforged.neoforge.client.IItemDecorator icon = (graphics, font, stack, x, y) -> {
            if (stack.is(PlaceholderContent.ITEM.get()) && PlaceholderItem.appearance(stack) > 1) return false;
            var material = PlaceholderItem.material(stack);
            if (material.isEmpty()) return false;
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(x + 4, y + 4, 210);
                graphics.pose().scale(.5F, .5F, 1);
                graphics.renderItem(material, 0, 0);
            } finally {
                graphics.pose().popPose();
            }
            return true;
        };
        event.register(PlaceholderContent.ITEM.get(), icon);
        event.register(PlaceholderContent.CREATIVE.get(), icon);
        for (var family : MimicFamily.values()) event.register(family.template().getItem(), icon);
    }
    private ModularMagicClient() {}
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(cn.nulladev.modularmagic.utility.UtilityContent.MENU.get(),AutoCrafterScreen::new);
        event.register(cn.nulladev.modularmagic.utility.UtilityContent.HANDHELD_MENU.get(),HandheldCraftingScreen::new);
        event.register(cn.nulladev.modularmagic.utility.UtilityContent.SAMPLE_MENU.get(),UnitSampleScreen::new);
        event.register(cn.nulladev.modularmagic.utility.UtilityContent.MATERIALIZER_MENU.get(),MaterializerScreen::new);
        event.register(cn.nulladev.modularmagic.crafting.EnchantingMenu.TYPE.get(),EnchantingScreen::new);
        event.register(ModContent.WRITING_MENU.get(), WritingScreen::new);
        event.register(ModContent.WAND_MENU.get(), WandScreen::new);
        event.register(cn.nulladev.modularmagic.research.ResearchContent.MENU.get(), ModularMagicClient::runeScreen);
        event.register(cn.nulladev.modularmagic.research.PlaceholderContent.MENU.get(), PlaceholderScreen::new);
    }
}
