package cn.nulladev.modularmagic.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

final class GlowParticle extends TextureSheetParticle {
    private final float riseTime;

    private GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprite) {
        super(level, x, y, z);
        xd = vx;
        yd = vy;
        zd = vz;
        gravity = 0;
        hasPhysics = false;
        // Offset the peaks so a fresh beam packet doesn't make the whole line flash at once.
        riseTime = 2F + random.nextFloat() * 3F;
        lifetime = 24 + random.nextInt(5);
        quadSize = .05F;
        pickSprite(sprite);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float elapsed = age + partialTick;
        float rise = Mth.clamp(elapsed / riseTime, 0, 1);
        float glow = 1F - Mth.clamp((elapsed - riseTime) / (lifetime - riseTime), 0, 1);
        setColor(Mth.lerp(glow, .38F, 1F), Mth.lerp(glow, .22F, .95F), Mth.lerp(glow, .55F, 1F));
        setAlpha(.86F * rise * rise * (3F - 2F * rise) * glow * glow);
        super.render(buffer, camera, partialTick);
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override public int getLightColor(float partialTick) { return LightTexture.FULL_BRIGHT; }

    record Provider(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, sprite);
        }
    }
}
