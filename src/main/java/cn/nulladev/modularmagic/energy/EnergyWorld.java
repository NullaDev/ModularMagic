package cn.nulladev.modularmagic.energy;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.network.EnergyVisual;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.spell.billing.ManaEscrow;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.*;
import net.neoforged.neoforge.event.tick.*;
import java.util.*;

/** Indexes loaded devices only. Looking up an energy source never loads its chunk. */
@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class EnergyWorld {
    private static final Map<ServerLevel, Set<BlockPos>> LOADED = new WeakHashMap<>();
    public static final TicketController TICKETS = new TicketController(ModularMagic.id("world_anchor"), (level, helper) -> {
        // Startup cannot assume an owner is online or grant free loading. Visiting a saved, enabled anchor restarts it.
        helper.getBlockTickets().keySet().forEach(helper::removeAllTickets);
        helper.getEntityTickets().keySet().forEach(helper::removeAllTickets);
    });
    public static void registerTickets(RegisterTicketControllersEvent event) { event.register(TICKETS); }
    public static void add(ServerLevel level,BlockPos pos) { LOADED.computeIfAbsent(level,k->new LinkedHashSet<>()).add(pos.immutable()); }
    public static void remove(ServerLevel level,BlockPos pos) { var set=LOADED.get(level);if(set!=null)set.remove(pos); }
    public static EnergyBlockEntity device(ServerLevel level,BlockPos pos) {
        return pos!=null&&level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof EnergyBlockEntity be?be:null;
    }
    public static List<EnergyBlockEntity> devices(ServerLevel level) {
        return new ArrayList<>(LOADED.getOrDefault(level,Set.of())).stream().map(p->device(level,p)).filter(Objects::nonNull).toList();
    }
    public static long incoming(EnergyBlockEntity dest) {
        if(!(dest.getLevel() instanceof ServerLevel level))return 0;
        long amount=0;
        for(var be:devices(level))if(be.pulseAmount>0&&dest.getBlockPos().equals(be.target)&&dest.identity.equals(be.targetIdentity))amount+=be.pulseDelivered;
        return amount;
    }
    public static long transfer(EnergyBlockEntity source,EnergyBlockEntity dest,long requested) {return transfer(source,dest,requested,1);}
    public static long transfer(EnergyBlockEntity source,EnergyBlockEntity dest,long requested,double efficiency) {
        if(source==dest||!source.sameOwner(dest)||efficiency<=0||!ResonanceNetwork.accepts(source,dest))return 0;
        if(source.kind()==EnergyContent.Kind.RESONATOR&&ResonanceNetwork.root(source)==null)return 0;
        long available=source.extract(requested,true);
        long delivery=dest.receive(EnergyMath.delivered(available,efficiency),true);
        long cost=EnergyMath.sourceCost(delivery,efficiency);
        if(delivery<=0||cost>available)return 0;
        long taken=source.extract(cost,false),accepted=dest.receive(delivery,false);
        source.refund(taken-EnergyMath.sourceCost(accepted,efficiency));
        if (accepted > 0 && source.kind() == EnergyContent.Kind.RESONATOR && source.getLevel() instanceof ServerLevel level) {
            EnergyVisual.send(level, EnergyVisual.Kind.RESONANCE, visualPoint(source), visualPoint(dest));
        }
        return accepted;
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if(!(event.getLevel() instanceof ServerLevel level)||level.getGameTime()%5!=0)return;
        var devices=devices(level);
        for(var source:devices) {
            if(source.outputPaused)continue;
            var kind=source.kind();
            if(kind.emitter()||kind==EnergyContent.Kind.CONDENSER||kind==EnergyContent.Kind.CRYSTAL_CHAMBER||kind==EnergyContent.Kind.RESONATOR||kind==EnergyContent.Kind.ANCHOR||kind==EnergyContent.Kind.WORLD_ANCHOR)continue;
            // Production feeds any adjacent receiver; storage only feeds appliances, avoiding tank-to-tank oscillation.
            int start=(int)((level.getGameTime()/5)%6);
            for(int i=0;i<6;i++) {
                var dest=device(level,source.getBlockPos().relative(Direction.values()[(start+i)%6]));
                if(dest!=null&&!dest.kind().infinite()&&dest.kind()!=EnergyContent.Kind.DISRUPTOR
                        &&(!kind.tank()||!dest.kind().tank()))transfer(source,dest,Long.MAX_VALUE);
            }
        }
        for(var source:devices)if(source.kind()==EnergyContent.Kind.RESONATOR&&ResonanceNetwork.root(source)!=null) {
            for(var dest:devices)if(dest!=source&&ResonanceNetwork.isParent(source,dest)&&dest.channel==source.channel&&ResonanceNetwork.inRange(source,dest))
                transfer(source,dest,Long.MAX_VALUE,source.resonanceEfficiency(Math.sqrt(dest.getBlockPos().distSqr(source.getBlockPos()))));
        }
    }
    public static long charge(ServerPlayer player,long maximum,boolean simulate) {
        long amount=0;
        for(var list:List.of(player.getInventory().items,player.getInventory().offhand))for(var stack:list) {
            if(stack.getItem() instanceof ManaStorage storage&&!storage.infinite(stack)) {
                if(!simulate)ManaEscrow.get(player.getServer()).claim(stack);
                amount+=storage.receiveUnits(stack,maximum-amount,simulate);if(amount>=maximum)return amount;
            }
        }
        return amount;
    }
    @SubscribeEvent public static void playerTick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||player.level().getGameTime()%5!=0)return;
        ItemStack receiver=ItemStack.EMPTY;
        for(var list:List.of(player.getInventory().items,player.getInventory().offhand))for(var stack:list)if(stack.is(EnergyContent.RECEIVER.get())&&receiver.isEmpty())receiver=stack;
        if(receiver.isEmpty())return;
        var data=EnergyLinkItem.data(receiver);int channel=data.getInt("Channel");
        var local=devices(player.serverLevel()).stream().filter(be->be.kind()==EnergyContent.Kind.RESONATOR&&be.resonanceEnabled()&&!be.paused()
                &&data.hasUUID("Network")&&data.getUUID("Network").equals(ResonanceNetwork.root(be))
                &&be.owner!=null&&be.owner.equals(player.getUUID())&&be.channel==channel&&be.energy>0
                &&be.getBlockPos().distToCenterSqr(player.position())<=be.resonanceRange()*be.resonanceRange())
                .max(Comparator.comparingLong(be->EnergyMath.delivered(be.extract(Long.MAX_VALUE,true),be.resonanceEfficiency(Math.sqrt(be.getBlockPos().distToCenterSqr(player.position())))))).orElse(null);
        EnergyBlockEntity source=local;double efficiency=local==null?1:local.resonanceEfficiency(Math.sqrt(local.getBlockPos().distToCenterSqr(player.position())));
        if(source==null&&data.hasUUID("Device")) {
            var dim=ResourceLocation.tryParse(data.getString("Dimension"));
            var sourceLevel=dim==null?null:player.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dim));
            var be=sourceLevel==null?null:device(sourceLevel,BlockPos.of(data.getLong("Position")));
            if(be!=null&&be.kind()==EnergyContent.Kind.ANCHOR&&be.identity.equals(data.getUUID("Device"))&&player.getUUID().equals(be.owner)&&!be.paused()
                    &&(be.crossDimension||sourceLevel==player.level())) {
                source=be;efficiency=1/(1+(sourceLevel==player.level()?EnergyConfig.SAME_DIMENSION_FEE.get():EnergyConfig.CROSS_DIMENSION_FEE.get()));
            }
        }
        long delivered=0;
        if(source!=null&&efficiency>0) {
            for(var list:List.of(player.getInventory().items,player.getInventory().offhand))for(var stack:list)if(stack.getItem() instanceof ManaStorage)ManaEscrow.get(player.getServer()).claim(stack);
            long maxRate=EnergyMath.quarterUnits(local==null?EnergyConfig.PLAYER_RATE.get():source.resonanceBandwidth(),player.level().getGameTime()/5);
            long available=source.extract(Long.MAX_VALUE,true);
            long possible=Math.min(maxRate,EnergyMath.delivered(available,efficiency));
            long accepted=charge(player,possible,true),cost=EnergyMath.sourceCost(accepted,efficiency);
            if(accepted>0&&cost<=available&&source.extract(cost,false)==cost) {
                delivered=charge(player,accepted,false);
                source.refund(cost-EnergyMath.sourceCost(delivered,efficiency));source.changed();
                if (delivered > 0 && source.getLevel() instanceof ServerLevel sourceLevel) {
                    Vec3 point = visualPoint(source);
                    if (source.kind() == EnergyContent.Kind.ANCHOR) {
                        EnergyVisual.send(sourceLevel, EnergyVisual.Kind.ANCHOR, point, point);
                    } else {
                        EnergyVisual.send(sourceLevel, EnergyVisual.Kind.RESONANCE, point, player.getEyePosition());
                    }
                }
            }
        }
        if(player.level().getGameTime()%20==0) {
            data.putLong("DeliveredPerSecond",delivered*4/ManaStorage.SCALE);data.putBoolean("Connected",source!=null);EnergyLinkItem.data(receiver,data);
            player.getInventory().setChanged();
        }
    }
    private static Vec3 visualPoint(EnergyBlockEntity device) {
        return Vec3.atCenterOf(device.getBlockPos()).add(0, .6, 0);
    }
    @SubscribeEvent public static void tntTick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof net.minecraft.world.entity.item.PrimedTnt tnt)||!(tnt.level() instanceof ServerLevel level))return;
        for(var be:devices(level))if(be.kind()==EnergyContent.Kind.GENERATOR&&be.generator.captureTnt(tnt)){event.setCanceled(true);return;}
    }
    @SubscribeEvent public static void itemTick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof ItemEntity item&&!item.level().isClientSide&&AetherShardItem.expired(item.getItem(),item.level()))item.discard();
    }
    public static void release(EnergyBlockEntity be) {
        if(be.appliedRadius<0||!(be.getLevel() instanceof ServerLevel level))return;
        int x=be.getBlockPos().getX()>>4,z=be.getBlockPos().getZ()>>4;
        for(int dx=-be.appliedRadius;dx<=be.appliedRadius;dx++)for(int dz=-be.appliedRadius;dz<=be.appliedRadius;dz++)TICKETS.forceChunk(level,be.getBlockPos(),x+dx,z+dz,false,true);
        be.appliedRadius=-1;
    }
    public static void tickLoader(EnergyBlockEntity be) {
        if(!(be.getLevel() instanceof ServerLevel level))return;
        boolean online=be.owner!=null&&level.getServer().getPlayerList().getPlayer(be.owner)!=null;
        if(online)be.offlineSince=-1;else if(be.offlineSince<0)be.offlineSince=level.getGameTime();
        boolean allowed=online||EnergyConfig.OFFLINE_LOADING.get()||level.getGameTime()-be.offlineSince<EnergyConfig.LOADER_OFFLINE_TICKS.get();
        int chunks=be.loaderRadius==0?1:9;
        long fee=EnergyMath.quarterUnits((long)chunks*EnergyConfig.LOADER_RATE.get(),level.getGameTime()/5);
        if(!be.loaderEnabled||be.owner==null||!allowed||be.energy<fee||be.outputPaused) { release(be);return; }
        if(be.appliedRadius!=be.loaderRadius) {
            release(be);
            int already=0;
            for(var l:level.getServer().getAllLevels())for(var other:devices(l))if(other!=be&&be.owner.equals(other.owner)&&other.appliedRadius>=0)already+=other.appliedRadius==0?1:9;
            if(already+chunks>EnergyConfig.MAX_CHUNKS.get()||be.energy<(long)EnergyConfig.LOADER_START.get()*ManaStorage.SCALE)return;
            be.appliedRadius=be.loaderRadius;
            int x=be.getBlockPos().getX()>>4,z=be.getBlockPos().getZ()>>4;
            for(int dx=-be.loaderRadius;dx<=be.loaderRadius;dx++)for(int dz=-be.loaderRadius;dz<=be.loaderRadius;dz++)TICKETS.forceChunk(level,be.getBlockPos(),x+dx,z+dz,true,true);
        }
        be.energy-=fee;be.changed();
    }
    private EnergyWorld() {}
}
