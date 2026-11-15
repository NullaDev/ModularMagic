package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.spell.RuntimeSpells;
import dev.xkmc.l2magic.content.engine.spell.SpellAction;
import dev.xkmc.l2magic.content.entity.core.*;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Preserve local projectile programs through the prerequisite's spawn/NBT serialization. */
@Mixin(value = ProjectileData.class, remap = false)
public abstract class ProjectileDataMixin {
    @Shadow private ProjectileConfig config;
    @Shadow private ResourceLocation id;
    @Unique @SerialField private String[] modularmagic$document;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Ldev/xkmc/l2magic/content/entity/core/ProjectileParams;Lnet/minecraft/core/Holder;)V", at = @At("RETURN"))
    private void modularmagic$capture(Holder<SpellAction> root, ProjectileParams params, Holder<ProjectileConfig> holder, CallbackInfo ci) {
        String source = RuntimeSpells.source(holder);
        var chunks = new java.util.ArrayList<String>();
        for (int offset = 0; offset < source.length(); offset += 8000)
            chunks.add(source.substring(offset, Math.min(source.length(), offset + 8000)));
        modularmagic$document = chunks.toArray(String[]::new);
    }
    @Inject(method = "getConfig", at = @At("HEAD"), cancellable = true)
    private void modularmagic$resolve(Level level, CallbackInfoReturnable<ProjectileConfig> cir) {
        if (modularmagic$document == null || modularmagic$document.length == 0) return;
        if (config == null) config = RuntimeSpells.get(level, String.join("", modularmagic$document)).projectiles().get(id);
        cir.setReturnValue(config);
    }
}
