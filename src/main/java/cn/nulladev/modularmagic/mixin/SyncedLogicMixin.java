package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.spell.RuntimeSpells;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.engine.core.ConfiguredEngine;
import dev.xkmc.l2magic.content.engine.logic.SyncedLogic;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SyncedLogic.class, remap = false)
public abstract class SyncedLogicMixin {
    @Shadow public abstract ConfiguredEngine<?> child();
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void modularmagic$execute(EngineContext context, CallbackInfo ci) {
        if (!RuntimeSpells.isCustom(context)) return;
        // The complete custom program is already replayed on clients with its shared seed.
        context.execute(child());
        ci.cancel();
    }
}
