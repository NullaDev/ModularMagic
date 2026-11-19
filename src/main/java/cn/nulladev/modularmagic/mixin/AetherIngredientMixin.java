package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.energy.AetherShardItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class AetherIngredientMixin {
    @Inject(method="test(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void modularmagic$expired(ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(stack!=null&&stack.getItem() instanceof AetherShardItem&&AetherShardItem.expiredOnServer(stack))cir.setReturnValue(false);
    }
}
