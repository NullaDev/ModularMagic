package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class ResearchNetworking{
    public record Knowledge(CompoundTag data) implements CustomPacketPayload{
        public static final Type<Knowledge> TYPE=new Type<>(ModularMagic.id("knowledge"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Knowledge> CODEC=new StreamCodec<>(){
            public Knowledge decode(RegistryFriendlyByteBuf b){return new Knowledge(b.readNbt());}
            public void encode(RegistryFriendlyByteBuf b,Knowledge p){b.writeNbt(p.data);}
        };
        public Type<Knowledge> type(){return TYPE;}
    }
    public static void sync(ServerPlayer p){var t=new CompoundTag();ResearchKnowledge.state(p).getAllKeys().stream().filter(k->k.startsWith("known:")&&ResearchKnowledge.state(p).getBoolean(k)).forEach(k->t.putBoolean(k,true));PacketDistributor.sendToPlayer(p,new Knowledge(t));}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p&&p.tickCount%20==0&&(p.getMainHandItem().is(ResearchContent.LENS.get())||p.getOffhandItem().is(ResearchContent.LENS.get())))sync(p);}
    private ResearchNetworking(){}
}
