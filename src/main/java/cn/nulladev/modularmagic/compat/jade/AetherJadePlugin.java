package cn.nulladev.modularmagic.compat.jade;

import cn.nulladev.modularmagic.energy.EnergyBlock;
import cn.nulladev.modularmagic.energy.EnergyBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import snownee.jade.api.*;

@WailaPlugin
public final class AetherJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(AetherBlockProvider.INSTANCE, EnergyBlockEntity.class);
        registration.registerEntityDataProvider(AetherItemProvider.INSTANCE, ItemEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(AetherBlockProvider.INSTANCE, EnergyBlock.class);
        registration.registerEntityComponent(AetherItemProvider.INSTANCE, ItemEntity.class);
    }
}
