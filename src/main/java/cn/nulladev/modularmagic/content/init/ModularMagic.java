package cn.nulladev.modularmagic.content.init;

import cn.nulladev.modularmagic.content.init.registry.*;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@Mod(ModularMagic.MODID)
public class ModularMagic {
    public static final String MODID = "modularmagic";
    public static final L2Registrate REGISTRATE = new L2Registrate(MODID);

    public ModularMagic(IEventBus bus) {
        ModRegistry.register();
        ModItems.register();
    }

}
