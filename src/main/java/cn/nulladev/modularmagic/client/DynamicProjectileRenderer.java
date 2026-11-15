package cn.nulladev.modularmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.l2magic.content.entity.core.LMProjectile;
import dev.xkmc.l2magic.content.entity.renderer.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** Uses L2Magic geometry/materials without its startup-sized projectile batch queue. */
public record DynamicProjectileRenderer(ResourceLocation texture) implements ProjectileRenderer {
    @Override public ResourceLocation getTexture() { return texture; }
    @Override public void render(LMProjectile entity, LMProjectileRenderer<?> renderer, float tick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.translate(0, entity.getBbHeight() / 2, 0);
        new LMProjectileType(texture).create(instance -> instance.tex(buffer.getBuffer(LMRenderStates.solid(texture))), renderer, entity, pose, tick);
        pose.popPose();
    }
}
