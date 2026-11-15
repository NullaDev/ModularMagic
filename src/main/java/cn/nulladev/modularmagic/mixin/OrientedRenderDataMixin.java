package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.client.DynamicProjectileRenderer;
import cn.nulladev.modularmagic.spell.RuntimeSpells;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.entity.renderer.*;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OrientedRenderData.class, remap = false)
public abstract class OrientedRenderDataMixin {
    @Shadow public abstract ResourceLocation texture();
    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private void modularmagic$dynamicRenderer(EngineContext context, CallbackInfoReturnable<ProjectileRenderer> cir) {
        if (RuntimeSpells.isCustom(context)) cir.setReturnValue(new DynamicProjectileRenderer(texture()));
    }
}
