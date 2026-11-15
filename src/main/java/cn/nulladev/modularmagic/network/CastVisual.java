package cn.nulladev.modularmagic.network;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2magic.content.engine.context.SpellContext;
import dev.xkmc.l2magic.content.engine.helper.Orientation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record CastVisual(String document, int user, long seed, Vec3 origin, Vec3 facing, Vec3 normal, double ticks, double power) implements CustomPacketPayload {
    public static final Type<CastVisual> TYPE = new Type<>(ModularMagic.id("cast_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastVisual> CODEC = new StreamCodec<>() {
        public CastVisual decode(RegistryFriendlyByteBuf b) {
            return new CastVisual(b.readUtf(SpellDocument.MAX_JSON), b.readVarInt(), b.readLong(), read(b), read(b), read(b), b.readDouble(), b.readDouble());
        }
        public void encode(RegistryFriendlyByteBuf b, CastVisual p) {
            b.writeUtf(p.document, SpellDocument.MAX_JSON); b.writeVarInt(p.user); b.writeLong(p.seed);
            write(b, p.origin); write(b, p.facing); write(b, p.normal); b.writeDouble(p.ticks); b.writeDouble(p.power);
        }
        private Vec3 read(RegistryFriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
        private void write(RegistryFriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    };
    public static CastVisual of(String document, SpellContext c) {
        return new CastVisual(document, c.user().getId(), c.seed(), c.origin(), c.facing().forward(), c.facing().normal(), c.tickUsing(), c.power());
    }
    public void handle(Level level) {
        if (!(level.getEntity(user) instanceof LivingEntity entity)) return;
        try { RuntimeSpells.execute(level, document, new SpellContext(entity, origin, Orientation.of(facing, normal), seed, ticks, power, 0)); }
        catch (RuntimeException e) { ModularMagic.LOGGER.warn("Could not replay custom spell: {}", e.getMessage()); }
    }
    @Override public Type<CastVisual> type() { return TYPE; }
}
