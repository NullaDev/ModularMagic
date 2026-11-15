package cn.nulladev.modularmagic.network;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.menu.WritingMenu;
import cn.nulladev.modularmagic.spell.SpellDocument;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EditorNetworking {
    public record Edit(int container, long revision, String command) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ModularMagic.id("editor_edit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Edit> CODEC = new StreamCodec<>() {
            public Edit decode(RegistryFriendlyByteBuf buf) { return new Edit(buf.readVarInt(), buf.readLong(), buf.readUtf(32768)); }
            public void encode(RegistryFriendlyByteBuf buf, Edit value) {
                buf.writeVarInt(value.container); buf.writeLong(value.revision); buf.writeUtf(value.command, 32768);
            }
        };
        @Override public Type<Edit> type() { return TYPE; }
    }

    public record Snapshot(int container, long revision, String document, boolean complete, String status) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ModularMagic.id("editor_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC = new StreamCodec<>() {
            public Snapshot decode(RegistryFriendlyByteBuf buf) {
                return new Snapshot(buf.readVarInt(), buf.readLong(), buf.readUtf(SpellDocument.MAX_JSON), buf.readBoolean(), buf.readUtf(4096));
            }
            public void encode(RegistryFriendlyByteBuf buf, Snapshot value) {
                buf.writeVarInt(value.container); buf.writeLong(value.revision);
                buf.writeUtf(value.document, SpellDocument.MAX_JSON); buf.writeBoolean(value.complete); buf.writeUtf(value.status, 4096);
            }
        };
        @Override public Type<Snapshot> type() { return TYPE; }
    }

    private EditorNetworking() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(CastVisual.TYPE, CastVisual.CODEC, (packet, context) ->
                context.enqueueWork(() -> packet.handle(context.player().level())));
        registrar.playToServer(Edit.TYPE, Edit.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WritingMenu menu && menu.containerId == packet.container())
                menu.apply(packet.revision(), packet.command());
        }));
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof WritingMenu menu && menu.containerId == packet.container())
                menu.receive(packet);
        }));
    }
}
