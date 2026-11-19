package cn.nulladev.modularmagic.mixin;

import cn.nulladev.modularmagic.energy.EnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** NeoForge's source event refers to neighbouring sources, so intercept only the actual destination result. */
@Mixin(FlowingFluid.class)
public abstract class AetherWaterMixin {
    @Inject(method="getNewLiquid",at=@At("RETURN"),cancellable=true)
    private void modularmagic$intercept(Level level,BlockPos pos,BlockState state,CallbackInfoReturnable<FluidState> cir) {
        var result=cir.getReturnValue();
        if(result.isSource()&&result.is(FluidTags.WATER)&&level.hasChunkAt(pos.below())&&level.getBlockEntity(pos.below()) instanceof EnergyBlockEntity be
                &&be.canInterceptWater())cir.setReturnValue(Fluids.FLOWING_WATER.getFlowing(7,false));
    }
}
