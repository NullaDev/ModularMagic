package cn.nulladev.modularmagic.client;

import cn.nulladev.modularmagic.energy.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

/** Liquid reservoirs immerse their charging item; the separate chamber holds the Nether Star nucleus. */
public final class EnergyRenderer implements BlockEntityRenderer<EnergyBlockEntity> {
    private static final int[] RESONATOR_CRYSTAL_ORDER = {0, 4, 2, 6, 1, 5, 3, 7};
    public EnergyRenderer(BlockEntityRendererProvider.Context context) {}
    public static float liquidHeight(float fraction) { return 2.1F/16 + Math.clamp(fraction,0,1)*11.7F/16; }
    @Override public boolean shouldRenderOffScreen(EnergyBlockEntity be) { return be.kind().emitter(); }
    @Override public void render(EnergyBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(be.kind()==EnergyContent.Kind.CRYSTAL_CHAMBER) {
            if(be.displayedFill>.00001F) renderCrystals(be.displayedFill,pose,buffers);
        } else if(be.kind().tank()&&be.displayedFill>.00001F) {
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cn.nulladev.modularmagic.ModularMagic.id("block/aether_liquid"));
            cube(pose,buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),
                    2.05F/16,2.05F/16,2.05F/16,13.95F/16,liquidHeight(be.displayedFill),13.95F/16,
                    sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),100,223,244,205);
        }
        if(be.kind().tank()&&!be.battery.isEmpty())renderItem(be,be.battery,.23F+.38F*be.displayedFill,.48F,pose,buffers,light,overlay);
        if(be.kind()==EnergyContent.Kind.RESONATOR&&be.resonanceEnabled())renderResonatorCrystals(be.resonanceLevel(),pose,buffers);
        if(be.kind()==EnergyContent.Kind.ANCHOR&&be.crossDimension)renderItem(be,EnergyContent.CROSS_LENS.toStack(),1.06F,.6F,pose,buffers,light,overlay);
        if(be.kind()==EnergyContent.Kind.GENERATOR&&be.generator.rule()!=null)renderItem(be,be.generator.rule().icon(),1.1F,.55F,pose,buffers,light,overlay);
        if(be.kind().emitter())renderHead(be,pose,buffers);
        if(be.pulseAmount>0&&be.pulseStart!=null&&be.pulseEnd!=null) {
            double t=Math.min(1,(be.pulseAge+partial)/Math.max(1,be.pulseDuration));
            Vec3 pos=be.pulseStart.lerp(be.pulseEnd,t).subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cn.nulladev.modularmagic.ModularMagic.id("block/aether_port"));
            float r=.08F,x=(float)pos.x,y=(float)pos.y,z=(float)pos.z;
            cube(pose,buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),x-r,y-r,z-r,x+r,y+r,z+r,
                    sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),135,238,255,230);
        }
    }
    private static void renderItem(EnergyBlockEntity be,ItemStack item,float y,float scale,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,y,.5);
        pose.mulPose(Axis.YP.rotationDegrees((be.getLevel().getGameTime()%720)*.5F));pose.scale(scale,scale,scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.FIXED,light,overlay,pose,buffers,be.getLevel(),be.getBlockPos().hashCode());
        pose.popPose();
    }
    private static void renderHead(EnergyBlockEntity be,PoseStack pose,MultiBufferSource buffers) {
        var direction=be.emissionDirection();
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,-direction.z));
        float pitch=(float)Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.x*direction.x+direction.z*direction.z)));
        pose.pushPose();pose.translate(.5,.6,.5);pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));
        String frame=switch(be.kind()){case EXPANDED_EMITTER->"frame_gold";case ULTIMATE_EMITTER,CREATIVE_EMITTER->"frame_netherite";default->"frame_copper";};
        texturedCube(pose,buffers,-.23F,-.17F,-.42F,.23F,.17F,.18F,frame);
        if(be.kind()==EnergyContent.Kind.CREATIVE_EMITTER)texturedCube(pose,buffers,-.24F,-.18F,-.32F,.24F,.18F,-.27F,"creative_checker");
        texturedCube(pose,buffers,-.18F,-.12F,-.51F,.18F,.12F,-.41F,"aether_casing");
        texturedCube(pose,buffers,-.13F,-.08F,-.525F,.13F,.08F,-.51F,"aether_port");
        pose.popPose();
    }
    private static void texturedCube(PoseStack pose,MultiBufferSource buffers,float x,float y,float z,float X,float Y,float Z,String texture) {
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cn.nulladev.modularmagic.ModularMagic.id("block/"+texture));
        cube(pose,buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS)),x,y,z,X,Y,Z,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),255,255,255,255);
    }
    private static void renderResonatorCrystals(int shards, PoseStack pose, MultiBufferSource buffers) {
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.withDefaultNamespace("block/amethyst_block"));
        // Stretch a four-pixel patch across each facet to match the scale of the small crystals.
        float u0 = Mth.lerp(.375F, sprite.getU0(), sprite.getU1());
        float v0 = Mth.lerp(.375F, sprite.getV0(), sprite.getV1());
        float u1 = Mth.lerp(.625F, sprite.getU0(), sprite.getU1());
        float v1 = Mth.lerp(.625F, sprite.getV0(), sprite.getV1());
        var out = buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        int count = Math.min((shards + 7) / 8, RESONATOR_CRYSTAL_ORDER.length);
        for (int i = 0; i < count; i++) {
            int slot = RESONATOR_CRYSTAL_ORDER[i];
            float height = slot % 2 == 0 ? .37F : .27F;
            pose.pushPose();
            pose.translate(.5, .47, .5);
            pose.mulPose(Axis.YP.rotationDegrees(22.5F + slot * 45));
            pose.translate(.107, 0, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-40));
            pose.translate(0, height * .5F, 0);
            crystal(pose, out, height, .049F, u0, v0, u1, v1, 0xcab0fa);
            pose.popPose();
        }
    }
    private static void renderCrystals(float fill,PoseStack pose,MultiBufferSource buffers) {
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cn.nulladev.modularmagic.ModularMagic.id("block/aether_crystal"));
        var out=buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        // Scaling all three axes by the cube root makes crystal volume proportional to stored aether.
        float growth=(float)Math.cbrt(Math.clamp(fill,0,1));
        float[][] clusters={
                {-.23F,-.015F,0,.46F,.071F,0,18},
                {.23F,.015F,0,.50F,.075F,0,-16},
                {-.09F,.12F,-.20F,.43F,.065F,-15,12},
                {.12F,.085F,-.19F,.36F,.069F,-18,-12},
                {0,-.19F,.17F,.24F,.063F,26,0}
        };
        for(var crystal:clusters) {
            pose.pushPose();
            pose.translate(.5+crystal[0]*growth,.5+crystal[1]*growth,.5+crystal[2]*growth);
            pose.scale(growth,growth,growth);
            pose.mulPose(Axis.XP.rotationDegrees(crystal[5]));pose.mulPose(Axis.ZP.rotationDegrees(crystal[6]));
            crystal(pose,out,crystal[3],crystal[4],sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),0x82cde1);
            pose.popPose();
        }
    }
    private static void crystal(PoseStack pose,VertexConsumer out,float height,float radius,float u,float v,float U,float V,int color) {
        float bottom=-height*.5F,lower=-height*.29F,upper=height*.22F,top=height*.5F;
        for(int side=0;side<6;side++) {
            double a=side*Math.PI/3,b=(side+1)*Math.PI/3;
            float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;
            float X=(float)Math.cos(b)*radius,Z=(float)Math.sin(b)*radius;
            float nx=(float)Math.cos((a+b)/2),nz=(float)Math.sin((a+b)/2);
            int shade=side%3;
            int r=Math.min(255,(color>>16&255)+shade*22);
            int g=Math.min(255,(color>>8&255)+shade*14);
            int blue=Math.min(255,(color&255)+shade*10);
            face(pose,out,new float[]{x,lower,z,x,upper,z,X,upper,Z,X,lower,Z},u,v,U,V,r,g,blue,255,nx,0,nz);
            // A repeated tip vertex forms a triangle in Minecraft's quad buffer.
            face(pose,out,new float[]{x,upper,z,0,top,0,0,top,0,X,upper,Z},u,v,U,V,Math.min(255,r+12),Math.min(255,g+8),Math.min(255,blue+6),255,nx*.7F,.7F,nz*.7F);
            face(pose,out,new float[]{X,lower,Z,0,bottom,0,0,bottom,0,x,lower,z},u,v,U,V,r-18,g-18,blue-18,255,nx*.7F,-.7F,nz*.7F);
        }
    }
    private static void cube(PoseStack pose,VertexConsumer out,float x,float y,float z,float X,float Y,float Z,
                             float u,float v,float U,float V,int r,int g,int b,int a) {
        face(pose,out,new float[]{x,Y,z,x,Y,Z,X,Y,Z,X,Y,z},u,v,U,V,r,g,b,a,0,1,0);
        face(pose,out,new float[]{x,y,Z,x,y,z,X,y,z,X,y,Z},u,v,U,V,r,g,b,a,0,-1,0);
        face(pose,out,new float[]{x,y,z,x,Y,z,X,Y,z,X,y,z},u,v,U,V,r,g,b,a,0,0,-1);
        face(pose,out,new float[]{X,y,Z,X,Y,Z,x,Y,Z,x,y,Z},u,v,U,V,r,g,b,a,0,0,1);
        face(pose,out,new float[]{x,y,Z,x,Y,Z,x,Y,z,x,y,z},u,v,U,V,r,g,b,a,-1,0,0);
        face(pose,out,new float[]{X,y,z,X,Y,z,X,Y,Z,X,y,Z},u,v,U,V,r,g,b,a,1,0,0);
    }
    private static void face(PoseStack pose,VertexConsumer out,float[] p,float u,float v,float U,float V,int r,int g,int b,int a,float nx,float ny,float nz) {
        float[] us={u,u,U,U},vs={V,v,v,V};
        for(int i=0;i<4;i++)out.addVertex(pose.last(),p[i*3],p[i*3+1],p[i*3+2]).setColor(r,g,b,a).setUv(us[i],vs[i])
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),nx,ny,nz);
    }
}
