package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.research.MaterialPlacement;
import cn.nulladev.modularmagic.util.AetherNumbers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.List;

public final class AetherUtilityItem extends Item {
    public enum Kind {FERTILIZER,FOOD,CANNON}
    private final Kind kind;
    public AetherUtilityItem(Kind kind){super(kind==Kind.FOOD?new Properties().stacksTo(1).food(Foods.CARROT):new Properties().stacksTo(1));this.kind=kind;}
    public long cost(){return switch(kind){case FERTILIZER->UtilityConfig.FERTILIZER.get();case FOOD->UtilityConfig.FOOD.get();case CANNON->UtilityConfig.CANNON.get();};}
    private boolean canPay(ServerPlayer p){return MaterialPlacement.pay(p,cost(),false,true);}
    @Override public InteractionResult useOn(UseOnContext ctx){
        if(kind!=Kind.FERTILIZER)return super.useOn(ctx);
        if(!(ctx.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var level=p.serverLevel();var pos=ctx.getClickedPos();
        if(!p.mayBuild()||!p.mayUseItemAt(pos,ctx.getClickedFace(),ctx.getItemInHand())||p.getServer().isUnderSpawnProtection(level,pos,p)||!dev.xkmc.l2magic.content.engine.block.BlockUtils.allowModification(level,pos)||!canPay(p))return InteractionResult.FAIL;
        var bone=new ItemStack(Items.BONE_MEAL);boolean grew=BoneMealItem.applyBonemeal(bone,level,pos,p);
        if(!grew&&level.getBlockState(pos).isFaceSturdy(level,pos,ctx.getClickedFace())){pos=pos.relative(ctx.getClickedFace());if(p.getServer().isUnderSpawnProtection(level,pos,p)||!dev.xkmc.l2magic.content.engine.block.BlockUtils.allowModification(level,pos))return InteractionResult.FAIL;grew=BoneMealItem.growWaterPlant(bone,level,pos,ctx.getClickedFace());}
        if(!grew)return InteractionResult.PASS;
        MaterialPlacement.pay(p,cost(),false,false);p.gameEvent(GameEvent.ITEM_INTERACT_FINISH);level.levelEvent(1505,pos,15);p.getInventory().setChanged();return InteractionResult.SUCCESS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(kind==Kind.FERTILIZER)return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer p&&!canPay(p))return InteractionResultHolder.fail(stack);
        if(kind==Kind.FOOD)return super.use(level,player,hand);
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
        if(player instanceof ServerPlayer p){
            if(!MaterialPlacement.pay(p,cost(),false,false))return InteractionResultHolder.fail(stack);
            var look=p.getLookAngle();double speed=UtilityConfig.CANNON_SPEED.get(),lift=UtilityConfig.CANNON_LIFT.get();
            p.setDeltaMovement(p.getDeltaMovement().add(look.x*speed,Math.max(lift,look.y*speed+lift),look.z*speed));p.setOnGround(false);p.hasImpulse=true;p.hurtMarked=true;
            p.connection.send(new ClientboundSetEntityMotionPacket(p));
            p.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER,p.getX(),p.getY(),p.getZ(),1,0,0,0,0);
            level.playSound(null,p.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.8f,1.1f);
            p.getCooldowns().addCooldown(this,UtilityConfig.CANNON_COOLDOWN.get());p.getInventory().setChanged();
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(kind==Kind.FOOD&&entity instanceof ServerPlayer p&&p.canEat(false)&&MaterialPlacement.pay(p,cost(),false,false))Items.CARROT.finishUsingItem(new ItemStack(Items.CARROT),level,p);
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.modularmagic.utility_cost",AetherNumbers.format(cost())));}
}
