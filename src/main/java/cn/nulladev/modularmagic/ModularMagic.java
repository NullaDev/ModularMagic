package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ModularMagic.MOD_ID)
public final class ModularMagic {
    public static final String MOD_ID = "modularmagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModularMagic(IEventBus modBus) {
        ModContent.register(modBus);
        modBus.addListener(EditorNetworking::register);
        modBus.addListener(ModularMagic::creativeContents);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            dev.xkmc.l2itemselector.select.item.IItemSelector.register(new cn.nulladev.modularmagic.item.CoreWandSelector());
            event.enqueueWork(() -> {
                if (dev.xkmc.l2serial.serialization.custom_handler.Handlers.getReg(dev.xkmc.l2magic.content.engine.spell.SpellAction.class) == null)
                    dev.xkmc.l2serial.serialization.custom_handler.Handlers.registerReg(dev.xkmc.l2magic.content.engine.spell.SpellAction.class,
                            dev.xkmc.l2magic.init.registrate.EngineRegistry.SPELL);
            });
        });
        LOGGER.info("ModularMagic by nulladev: {} node item templates", NodeCatalog.all().size());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void creativeContents(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        var items = dev.xkmc.l2magic.init.registrate.LMItems.CREATIVE_WAND;
        if (!event.getTabKey().equals(dev.xkmc.l2magic.init.registrate.LMItems.TAB.key())) return;
        // L2Magic creates one creative wand for every spell namespace. Our only entry is an internal execution anchor.
        for (var stack : java.util.stream.Stream.concat(event.getParentEntries().stream(), event.getSearchEntries().stream()).toList()) {
            if (stack.is(ModContent.CORE_WAND.get()) || stack.is(items.get()) && MOD_ID.equals(
                    dev.xkmc.l2magic.init.registrate.LMItems.MODID.getOrDefault(stack, "")))
                event.remove(stack, net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
