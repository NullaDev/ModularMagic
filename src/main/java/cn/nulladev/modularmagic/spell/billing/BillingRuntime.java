package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.spell.SpellDocument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class BillingRuntime {
    public static final ThreadLocal<CastAccount> ACTIVE=new ThreadLocal<>();
    public static final ThreadLocal<SpellDocument.Node> NODE=new ThreadLocal<>();
    public static final ThreadLocal<Double> PAYLOAD=new ThreadLocal<>();
    public static final ThreadLocal<Boolean> VISUAL_BATCH=ThreadLocal.withInitial(()->false);
    private record Payload(CastAccount account,double effects){}
    private static final Map<UUID,CastAccount> CASTS=new HashMap<>();
    private static final Map<Entity,Payload> NATIVE=new WeakHashMap<>();
    public static CastAccount get(UUID id){return CASTS.get(id);}
    public static void add(CastAccount account){CASTS.put(account.id,account);}
    public static long count(UUID player){return CASTS.values().stream().filter(a->!a.closed()&&a.player.getUUID().equals(player)).count();}
    public static void run(CastAccount account,Runnable task){
        var before=ACTIVE.get();ACTIVE.set(account);try{task.run();}finally{if(before==null)ACTIVE.remove();else ACTIVE.set(before);}
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide())return;var account=ACTIVE.get();if(account==null)return;
        var entity=event.getEntity();account.entities.add(entity);
        if(entity instanceof AbstractArrow){double effects=PAYLOAD.get()==null?0:PAYLOAD.get();
            NATIVE.put(entity,new Payload(account,effects));
            var tag=entity.getPersistentData();tag.putBoolean("modularmagic_paid_projectile",true);
        }
    }
    public static boolean nativeHit(Entity projectile,Entity target,float damage,java.util.function.BooleanSupplier original){
        var payload=NATIVE.get(projectile);
        if(payload==null){if(projectile.getPersistentData().getBoolean("modularmagic_paid_projectile")){projectile.discard();return false;}return original.getAsBoolean();}
        try{if(!payload.account.valid()||!payload.account.hit(projectile.getUUID())){projectile.discard();return false;}
            var values=CostRules.defaults();values.put("damage",(double)damage);
            payload.account.charge(payload.account.price("effect/damage",values)+payload.effects);payload.account.targetAttempt();return original.getAsBoolean();
        }catch(RuntimeException e){payload.account.abort(e);projectile.discard();return false;}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        for(var a:List.copyOf(CASTS.values()))a.tick();CASTS.values().removeIf(CastAccount::closed);
    }
    public static void cancel(net.minecraft.server.level.ServerPlayer player){for(var a:List.copyOf(CASTS.values()))if(a.player==player)a.close(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)cancel(player);}
    @SubscribeEvent public static void stop(ServerStoppingEvent event){for(var a:List.copyOf(CASTS.values()))a.close(true);CASTS.clear();NATIVE.clear();}
    private BillingRuntime(){}
}
