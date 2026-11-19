package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.energy.AetherShardItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rejects a stale result displayed before its ingredients expired, including shift-click crafting. */
@Mixin(AbstractContainerMenu.class)
public abstract class AetherMenuMixin {
    @Shadow @Final public NonNullList<Slot> slots;
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void modularmagic$expire(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        if(player.level().isClientSide)return;
        boolean expired=false;
        for(var s:slots)if(AetherShardItem.expired(s.getItem(),player.level())) {s.set(ItemStack.EMPTY);s.setChanged();expired=true;}
        var self=(AbstractContainerMenu)(Object)this;
        if(AetherShardItem.expired(self.getCarried(),player.level())) {self.setCarried(ItemStack.EMPTY);expired=true;}
        if(expired) {self.broadcastChanges();ci.cancel();}
    }
}
