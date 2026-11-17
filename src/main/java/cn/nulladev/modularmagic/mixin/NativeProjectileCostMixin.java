package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.spell.billing.BillingRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin({AbstractArrow.class,ThrownTrident.class})
public abstract class NativeProjectileCostMixin {
    @Redirect(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean modularmagic$payBeforeDamage(Entity target,DamageSource source,float amount){
        Entity self=(Entity)(Object)this;
        if(self.level().isClientSide())return target.hurt(source,amount);
        return BillingRuntime.nativeHit(self,target,amount,()->target.hurt(source,amount));
    }
}
