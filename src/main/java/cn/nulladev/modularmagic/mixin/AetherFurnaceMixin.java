package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.utility.AetherUtilityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AetherFurnaceMixin {
    @Redirect(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void modularmagic$consumeFuel(ItemStack fuel, int count) {
        if (!AetherUtilityItem.consumeFuel(fuel)) fuel.shrink(count);
    }
}
