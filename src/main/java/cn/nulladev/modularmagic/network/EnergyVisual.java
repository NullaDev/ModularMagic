package cn.nulladev.modularmagic.network;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.energy.EnergyContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the endpoints once. Clients fill in a beam without one packet per particle. */
public record EnergyVisual(Kind kind, Vec3 origin, Vec3 target) implements CustomPacketPayload {
    public enum Kind { RESONANCE, HARVEST, ANCHOR }

    public static final Type<EnergyVisual> TYPE = new Type<>(ModularMagic.id("energy_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyVisual> CODEC = new StreamCodec<>() {
        @Override
        public EnergyVisual decode(RegistryFriendlyByteBuf buffer) {
            return new EnergyVisual(buffer.readEnum(Kind.class), readPoint(buffer), readPoint(buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, EnergyVisual packet) {
            buffer.writeEnum(packet.kind);
            writePoint(buffer, packet.origin);
            writePoint(buffer, packet.target);
        }
    };

    @Override public Type<EnergyVisual> type() { return TYPE; }

    private static Vec3 readPoint(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writePoint(RegistryFriendlyByteBuf buffer, Vec3 point) {
        buffer.writeDouble(point.x);
        buffer.writeDouble(point.y);
        buffer.writeDouble(point.z);
    }

    public static void send(ServerLevel level, Kind kind, Vec3 origin, Vec3 target) {
        Vec3 middle = origin.lerp(target, .5);
        PacketDistributor.sendToPlayersNear(level, null, middle.x, middle.y, middle.z,
                32 + origin.distanceTo(target) * .5, new EnergyVisual(kind, origin, target));
    }

    public void show(Level level) {
        if (kind == Kind.ANCHOR) {
            for (int i = 0; i < 14; i++) {
                double angle = Math.PI * 2 * i / 14 + level.random.nextDouble() * .16;
                level.addParticle(ParticleTypes.END_ROD, origin.x, origin.y, origin.z,
                        Math.cos(angle) * .075, .014 + level.random.nextDouble() * .035, Math.sin(angle) * .075);
            }
            return;
        }
        double distance = origin.distanceTo(target);
        int steps = Math.clamp((int) Math.ceil(distance / .4), 1, 240);
        var velocity = target.subtract(origin).normalize().scale(kind == Kind.HARVEST ? .07 : .018);
        var particle = kind == Kind.HARVEST ? ParticleTypes.END_ROD : EnergyContent.RESONANCE_GLOW.get();
        for (int i = 0; i <= steps; i++) {
            var point = origin.lerp(target, (double) i / steps);
            level.addParticle(particle, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
