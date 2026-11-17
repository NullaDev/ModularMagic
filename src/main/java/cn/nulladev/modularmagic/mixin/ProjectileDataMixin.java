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
    @Unique @SerialField private String modularmagic$account;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;Ldev/xkmc/l2magic/content/entity/core/ProjectileParams;Lnet/minecraft/core/Holder;)V", at = @At("RETURN"))
    private void modularmagic$capture(Holder<SpellAction> root, ProjectileParams params, Holder<ProjectileConfig> holder, CallbackInfo ci) {
        String source = RuntimeSpells.source(holder);
        var chunks = new java.util.ArrayList<String>();
        for (int offset = 0; offset < source.length(); offset += 8000)
            chunks.add(source.substring(offset, Math.min(source.length(), offset + 8000)));
        modularmagic$document = chunks.toArray(String[]::new);
        var account=cn.nulladev.modularmagic.spell.billing.BillingRuntime.ACTIVE.get();
        if(account!=null)modularmagic$account=account.id.toString();
    }
    @Inject(method = "getConfig", at = @At("HEAD"), cancellable = true)
    private void modularmagic$resolve(Level level, CallbackInfoReturnable<ProjectileConfig> cir) {
        if (modularmagic$document == null || modularmagic$document.length == 0) return;
        if(!level.isClientSide()&&modularmagic$account!=null){
            var account=cn.nulladev.modularmagic.spell.billing.BillingRuntime.get(java.util.UUID.fromString(modularmagic$account));
            cir.setReturnValue(account==null||account.closed()?null:account.projectiles.get(id));return;
        }
        if (config == null) config = RuntimeSpells.get(level, String.join("", modularmagic$document)).projectiles().get(id);
        cir.setReturnValue(config);
    }
    @Inject(method="getContext",at=@At("RETURN"),cancellable=true)
    private void modularmagic$paidContext(LMProjectile self,int salt,boolean scheduled,CallbackInfoReturnable<dev.xkmc.l2magic.content.engine.context.EngineContext> cir){
        if(self.level().isClientSide()||modularmagic$account==null)return;
        var account=cn.nulladev.modularmagic.spell.billing.BillingRuntime.get(java.util.UUID.fromString(modularmagic$account));
        var ctx=cir.getReturnValue();if(account==null||account.closed()||ctx==null){cir.setReturnValue(null);return;}
        cir.setReturnValue(new dev.xkmc.l2magic.content.engine.context.EngineContext(
                new dev.xkmc.l2magic.content.engine.context.UserContext(ctx.user().level(),ctx.user().user(),ctx.user().root(),scheduled?new cn.nulladev.modularmagic.spell.billing.PaidScheduler(account):null),ctx.loc(),ctx.rand(),ctx.parameters()));
    }
    @Inject(method="hurtTargetImpl",at=@At("HEAD"),cancellable=true)
    private void modularmagic$paidHit(LMProjectile self,net.minecraft.world.entity.Entity target,CallbackInfo ci){
        if(modularmagic$account==null)return;
        if(self.level().isClientSide()){ci.cancel();return;}
        var account=cn.nulladev.modularmagic.spell.billing.BillingRuntime.get(java.util.UUID.fromString(modularmagic$account));
        if(account==null||account.closed()||!account.hit(self.getUUID()))ci.cancel();
    }
    @Inject(method={"tick","land","expire"},at=@At("HEAD"),cancellable=true)
    private void modularmagic$authorizedVisualsOnly(LMProjectile self,CallbackInfo ci){
        if(modularmagic$account!=null&&self.level().isClientSide())ci.cancel();
    }
}
