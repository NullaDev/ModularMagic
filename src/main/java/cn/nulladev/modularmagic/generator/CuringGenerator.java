package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.Items;

/** The device consumes the cure's ingredients and keeps its own clock; vanilla conversion is untouched. */
public final class CuringGenerator {
    private static final String KEY="ModularMagicCure";
    public static void tick(GeneratorState state){
        var level=state.level();var be=state.be;
        if(state.reserved>0&&state.actor!=null){
            var entity=level.getEntity(state.actor);
            if(entity instanceof ZombieVillager zombie){
                if(!zombie.isAlive()||zombie.isConverting()||!state.bounds().contains(zombie.position())||!owns(state,zombie)){
                    release(state);be.changed();return;
                }
                state.actorPosition=zombie.blockPosition();state.missingTicks=0;
                if(state.cureRemaining>0)state.cureRemaining--;
                state.progress=state.cureTotal<=0?0:Math.clamp(1-state.cureRemaining/(double)state.cureTotal,0,1);
                if(level.getGameTime()%10==0){var p=be.getBlockPos();level.sendParticles(ParticleTypes.WITCH,p.getX()+.5,p.getY()+1.1,p.getZ()+.5,3,.25,.12,.25,.02);}
                if(state.cureRemaining==0){
                    if(be.energy>be.capacity()-state.reserved){state.state("full",15);be.changed();return;}
                    be.energy+=state.reserved;release(state);state.progress=1;
                    state.cooldown=state.rule().period.get();state.state(state.cooldown>0?"cooldown":"weakness",state.cooldown>0?3:2);
                }else state.state("curing",4+(int)(state.progress*9));
                be.changed();return;
            }
            if(state.actorPosition!=null&&level.hasChunkAt(state.actorPosition)&&++state.missingTicks>200){release(state);be.changed();}
            state.state("target_unloaded",2);return;
        }
        if(state.cooldown>0){state.cooldown--;state.state("cooldown",3);be.changed();return;}
        if(state.retry-->0)return;state.retry=10;
        var targets=level.getEntitiesOfClass(ZombieVillager.class,state.bounds(),z->{
            if(z.getPersistentData().contains(KEY)&&claimIsStale(z))z.getPersistentData().remove(KEY);
            return z.isAlive()&&!z.isConverting()&&!z.getPersistentData().contains(KEY)&&z.hasEffect(MobEffects.WEAKNESS);
        });
        if(targets.isEmpty()){state.state("weakness",2);return;}
        long yield=state.rule().yield(0)*ManaStorage.SCALE;
        if(state.room()<yield){state.state("full",15);return;}
        for(var apple:level.getEntitiesOfClass(ItemEntity.class,state.bounds(),e->!e.isRemoved()&&e.getItem().is(Items.GOLDEN_APPLE))){
            var zombie=targets.getFirst();if(zombie.getPersistentData().contains(KEY))continue;
            int min=GeneratorCatalog.CURE_MIN.get(),max=Math.max(min,GeneratorCatalog.CURE_MAX.get());
            int time=min+level.random.nextInt(max-min+1);
            var tag=new CompoundTag();tag.putLong("Machine",be.getBlockPos().asLong());tag.putUUID("Device",be.identity);
            zombie.getPersistentData().put(KEY,tag);
            state.actor=zombie.getUUID();state.actorPosition=zombie.blockPosition();state.cureRemaining=state.cureTotal=time;state.progress=0;state.reserved=yield;
            apple.getItem().shrink(1);if(apple.getItem().isEmpty())apple.discard();else apple.setItem(apple.getItem().copy());
            zombie.removeEffect(MobEffects.WEAKNESS);state.state("curing",4);be.changed();return;
        }
        state.state("input",1);
    }
    private static boolean claimIsStale(ZombieVillager zombie){
        if(!(zombie.level() instanceof ServerLevel level))return false;
        var data=zombie.getPersistentData().getCompound(KEY);var pos=BlockPos.of(data.getLong("Machine"));
        if(!level.hasChunkAt(pos))return false;
        var be=EnergyWorld.device(level,pos);
        return be==null||be.kind()!=EnergyContent.Kind.GENERATOR||!be.generator.ruleId.equals("curing")||!owns(be.generator,zombie);
    }
    private static boolean owns(GeneratorState state,ZombieVillager zombie){
        var tag=zombie.getPersistentData().getCompound(KEY);
        return tag.hasUUID("Device")&&tag.getUUID("Device").equals(state.be.identity)&&zombie.getUUID().equals(state.actor)&&state.reserved>0;
    }
    public static void release(GeneratorState state){
        if(state.actor!=null&&state.level().getEntity(state.actor) instanceof ZombieVillager zombie&&owns(state,zombie))zombie.getPersistentData().remove(KEY);
        state.reserved=0;state.actor=null;state.actorPosition=null;state.cureRemaining=0;state.missingTicks=0;
    }
    private CuringGenerator(){}
}
