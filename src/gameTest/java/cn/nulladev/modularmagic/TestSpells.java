package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.spell.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/** Minimal projectile fixture, deliberately absent from the shipped creative presets. */
final class TestSpells {
    static ItemStack projectile(HolderLookup.Provider registries) {
        try (var stream = TestSpells.class.getResourceAsStream("/fixtures/projectile.json")) {
            var document = SpellDocument.fromJson(new String(java.util.Objects.requireNonNull(stream).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            return SpellCoreItem.write(document, SpellCompiler.compile(document, registries));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
}
