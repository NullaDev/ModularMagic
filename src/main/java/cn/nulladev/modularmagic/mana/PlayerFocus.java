package cn.nulladev.modularmagic.mana;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.config.ResourceConfig;
import cn.nulladev.modularmagic.network.EditorNetworking;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.*;

@EventBusSubscriber(modid = ModularMagic.MOD_ID)
public final class PlayerFocus {
    private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModularMagic.MOD_ID);
    public static final java.util.function.Supplier<AttachmentType<Double>> VALUE = TYPES.register("focus", () ->
            AttachmentType.builder(() -> ResourceConfig.FOCUS_MAX.get()).serialize(Codec.DOUBLE).copyOnDeath().build());
    public static final java.util.function.Supplier<AttachmentType<Boolean>> TRIPPED = TYPES.register("focus_tripped", () ->
            AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());
    private static final java.util.function.Supplier<AttachmentType<Long>> PAUSE = TYPES.register("focus_pause", () ->
            AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());
    private static final java.util.function.Supplier<AttachmentType<Integer>> UNITS = TYPES.register("focus_units", () ->
            AttachmentType.builder(() -> 1).serialize(Codec.INT).copyOnDeath().build());
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static double value(net.minecraft.world.entity.player.Player player) {
        if(player instanceof ServerPlayer server&&player.getData(UNITS)<cn.nulladev.modularmagic.spell.ResourceBalance.FOCUS_UNIT_VERSION){
            double current=player.hasData(VALUE)?player.getData(VALUE)*cn.nulladev.modularmagic.spell.ResourceBalance.FOCUS_SCALE:ResourceConfig.FOCUS_MAX.get();
            set(server,current);
        }
        return player.getData(VALUE);
    }
    public static void set(ServerPlayer player, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite focus");
        player.setData(UNITS,cn.nulladev.modularmagic.spell.ResourceBalance.FOCUS_UNIT_VERSION);
        player.setData(VALUE, Math.min(value, ResourceConfig.FOCUS_MAX.get()));
        if (value < 0) player.setData(TRIPPED, true);
        if (value >= ResourceConfig.FOCUS_MAX.get()) player.setData(TRIPPED, false);
    }
    public static void consume(ServerPlayer player,double amount){
        if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("Invalid focus cost");
        if(amount==0)return;set(player,value(player)-amount);player.setData(PAUSE,player.level().getGameTime()+2);
        PacketDistributor.sendToPlayer(player,new EditorNetworking.FocusSnapshot(value(player),ResourceConfig.FOCUS_MAX.get(),player.getData(TRIPPED)));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        double max = ResourceConfig.FOCUS_MAX.get();
        if(player.level().getGameTime()>=player.getData(PAUSE))set(player, Math.min(max, value(player) + ResourceConfig.FOCUS_REGEN.get() / 20));
        if (player.tickCount % 5 == 0) PacketDistributor.sendToPlayer(player,
                new EditorNetworking.FocusSnapshot(value(player), max, player.getData(TRIPPED)));
    }
    private PlayerFocus() {}
}
