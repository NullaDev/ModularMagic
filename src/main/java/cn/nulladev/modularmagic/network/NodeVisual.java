package cn.nulladev.modularmagic.network;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.spell.RuntimeSpells;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2magic.content.engine.context.*;
import dev.xkmc.l2magic.content.engine.core.ConfiguredEngine;
import dev.xkmc.l2magic.content.engine.helper.Scheduler;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Paid programs replay authorized cosmetic subtrees; gameplay remains server-side. */
public record NodeVisual(int user,String action,String parameters,Vec3 origin,Vec3 forward,Vec3 normal,long seed) implements CustomPacketPayload {
    public static final Type<NodeVisual> TYPE=new Type<>(ModularMagic.id("node_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NodeVisual> CODEC=new StreamCodec<>(){
        public NodeVisual decode(RegistryFriendlyByteBuf b){return new NodeVisual(b.readVarInt(),b.readUtf(131072),b.readUtf(131072),read(b),read(b),read(b),b.readLong());}
        public void encode(RegistryFriendlyByteBuf b,NodeVisual v){b.writeVarInt(v.user);b.writeUtf(v.action,131072);b.writeUtf(v.parameters,131072);write(b,v.origin);write(b,v.forward);write(b,v.normal);b.writeLong(v.seed);}
        private Vec3 read(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
        private void write(RegistryFriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    };
    @Override public Type<NodeVisual> type(){return TYPE;}
    public static void send(ConfiguredEngine<?> action,EngineContext ctx){
        send(action,ctx,ctx.rand().nextLong());
    }
    public static void send(ConfiguredEngine<?> action,EngineContext ctx,long seed){
        var json=ConfiguredEngine.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,ctx.user().level().registryAccess()),action).getOrThrow();
        var vars=new JsonObject();ctx.parameters().forEach(vars::addProperty);if(ctx.user().scheduler()!=null)vars.addProperty("Time",ctx.user().scheduler().time);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf((net.minecraft.server.level.ServerPlayer)ctx.user().user(),
                new NodeVisual(ctx.user().user().getId(),json.toString(),vars.toString(),ctx.loc().pos(),ctx.loc().dir(),ctx.loc().normal(),seed));
    }
    public void handle(Level level){
        if(!(level.getEntity(user) instanceof net.minecraft.world.entity.LivingEntity entity))return;
        var engine=ConfiguredEngine.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,level.registryAccess()),JsonParser.parseString(action)).getOrThrow();
        var vars=new java.util.LinkedHashMap<String,Double>();JsonParser.parseString(parameters).getAsJsonObject().entrySet().forEach(e->vars.put(e.getKey(),e.getValue().getAsDouble()));
        if(!engine.verify(BuilderContext.withScheduler(dev.xkmc.l2magic.init.L2Magic.LOGGER,"paid_visual",vars.keySet())))return;
        var root=level.registryAccess().registryOrThrow(EngineRegistry.SPELL).getHolder(RuntimeSpells.ANCHOR).orElseThrow();
        var scheduler=new Scheduler(){@Override public void schedule(long delay,Runnable task){if(delay<=0)task.run();else super.schedule(delay,task);}};
        scheduler.time=vars.getOrDefault("Time",0D).longValue();
        var ctx=new EngineContext(new UserContext(level,entity,root,scheduler),LocationContext.of(origin,dev.xkmc.l2magic.content.engine.helper.Orientation.of(forward,normal)),new SingleThreadedRandomSource(seed),vars);
        cn.nulladev.modularmagic.client.VisualProgram.limit(engine).execute(ctx);ctx.registerScheduler();
    }
}
