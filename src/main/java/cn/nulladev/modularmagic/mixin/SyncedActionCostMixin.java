package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.spell.RuntimeSpells;
import dev.xkmc.l2magic.content.engine.context.*;
import dev.xkmc.l2magic.content.engine.extension.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=SyncedActionEntry.class,remap=false)
public abstract class SyncedActionCostMixin {
    @Inject(method="run",at=@At("HEAD"),cancellable=true)
    private static void modularmagic$local(EngineContext ctx,EngineContextData data,SyncedAction<?> action,CallbackInfo ci){
        if(!RuntimeSpells.isCustom(ctx)||cn.nulladev.modularmagic.spell.billing.BillingRuntime.ACTIVE.get()==null)return;
        data.remap(ctx).execute(action.child());ci.cancel();
    }
}
