package cn.nulladev.modularmagic.content.init.registry;

import cn.nulladev.modularmagic.content.block.MagicCompilerMenu;
import cn.nulladev.modularmagic.content.init.ModularMagic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRegistry {

    public static final DeferredRegister<MenuType<?>> MT = DeferredRegister.create(
            BuiltInRegistries.MENU,
            ModularMagic.MODID
    );
    public static final Supplier<MenuType<MagicCompilerMenu>> MAGIC_COMPILER_MENU = MT.register("magic_compiler", () -> new MenuType<>(MagicCompilerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register() {
    }
}
