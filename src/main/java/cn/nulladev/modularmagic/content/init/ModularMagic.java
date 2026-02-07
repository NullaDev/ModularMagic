package cn.nulladev.modularmagic.content.init;

import cn.nulladev.modularmagic.content.init.reg.*;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@Mod(ModularMagic.MODID)
@EventBusSubscriber(modid = ModularMagic.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModularMagic {
    public static final String MODID = "modularmagic";
    public static final L2Registrate REGISTRATE = new L2Registrate(MODID);

    public ModularMagic(IEventBus bus) {
//        GTRegistries.register();
        ModItems.register();
//        GTRecipes.register();
//        GTEngine.register();
//        GTEntities.register();
//        GTEffects.register();
//        GTParticles.register();
//        if (ModList.get().isLoaded(PatchouliAPI.MOD_ID)) {
//            PatchouliCompat.gen();
//        }
//        if (ModList.get().isLoaded(Apotheosis.MODID)) {
//            ApothCompat.register(bus);
//        }
//        DMG_GEN = new GTDamageTypeGen(REGISTRATE);
//        new GTClickHandler(loc("hex"));
    }

}
