package cn.nulladev.modularmagic.energy;

import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import java.util.List;

public final class EnergyLinkItem extends Item {
    public final boolean receiver;
    public EnergyLinkItem(boolean receiver) { super(new Properties().stacksTo(1)); this.receiver = receiver; }
    public static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static void data(ItemStack stack, CompoundTag tag) { stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        var level=ctx.getLevel();var player=ctx.getPlayer();
        if (player==null || !(level.getBlockEntity(ctx.getClickedPos()) instanceof EnergyBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!be.allowed(player)) { player.displayClientMessage(Component.translatable("energy.modularmagic.denied"),true); return InteractionResult.FAIL; }
        var stack=ctx.getItemInHand();var tag=data(stack);
        if (receiver) {
            if (be.kind()!=EnergyContent.Kind.ANCHOR && be.kind()!=EnergyContent.Kind.RESONATOR) return InteractionResult.FAIL;
            if(be.kind()==EnergyContent.Kind.RESONATOR) {
                var root=ResonanceNetwork.root(be);
                if(root==null){player.displayClientMessage(Component.translatable("energy.modularmagic.network_crystal"),true);return InteractionResult.FAIL;}
                tag.putUUID("Network",root);
            } else tag.remove("Network");
            tag.putString("Dimension",level.dimension().location().toString()); tag.putLong("Position",be.getBlockPos().asLong());tag.putUUID("Device",be.identity);
            tag.putInt("Channel",be.channel);data(stack,tag);
            tag.putBoolean("Connected",false);tag.putLong("DeliveredPerSecond",0);data(stack,tag);
            player.displayClientMessage(Component.translatable("energy.modularmagic.receiver_bound"),true);
        } else if (player.isShiftKeyDown()) {
            if (be.kind()==EnergyContent.Kind.RESONATOR&&be.resonanceParent!=null) {
                ResonanceNetwork.detach(be);clearSelection(stack);
                player.displayClientMessage(Component.translatable("energy.modularmagic.network_detached"),true);return InteractionResult.SUCCESS;
            } else if (be.kind()==EnergyContent.Kind.RESONATOR) be.channel=(be.channel+1)%16;
            else if(be.kind()==EnergyContent.Kind.WORLD_ANCHOR) be.loaderRadius=be.loaderRadius==0?1:0;
            else be.outputPaused=!be.outputPaused;
            Object setting=be.kind()==EnergyContent.Kind.RESONATOR?be.channel:be.kind()==EnergyContent.Kind.WORLD_ANCHOR?(be.loaderRadius==0?1:9):
                    Component.translatable(be.outputPaused?"energy.modularmagic.output_paused":"energy.modularmagic.output_running");
            be.changed();player.displayClientMessage(Component.translatable("energy.modularmagic.setting",setting),true);
        } else if (tag.contains("Emitter") && tag.getString("EmitterDimension").equals(level.dimension().location().toString())) {
            var origin=BlockPos.of(tag.getLong("Emitter"));
            if (level.hasChunkAt(origin) && level.getBlockEntity(origin) instanceof EnergyBlockEntity emitter && emitter.kind().emitter() && emitter.allowed(player)) {
                if(origin.equals(be.getBlockPos())) {clearSelection(stack);return InteractionResult.SUCCESS;}
                if(origin.distSqr(be.getBlockPos())>emitter.shotRange()*emitter.shotRange()) { player.displayClientMessage(Component.translatable("energy.modularmagic.out_of_range"),true);return InteractionResult.FAIL; }
                emitter.target=be.getBlockPos().immutable();emitter.targetIdentity=be.identity;emitter.changed();
                tag.remove("Emitter");data(stack,tag);player.displayClientMessage(Component.translatable("energy.modularmagic.linked"),true);
            }
        } else if(tag.contains("Resonator")&&tag.getString("ResonatorDimension").equals(level.dimension().location().toString())) {
            var origin=BlockPos.of(tag.getLong("Resonator"));
            if(level.hasChunkAt(origin)&&level.getBlockEntity(origin) instanceof EnergyBlockEntity source&&source.kind()==EnergyContent.Kind.RESONATOR&&source.allowed(player)) {
                var error=ResonanceNetwork.bind(source,be);
                if(error!=null){player.displayClientMessage(Component.translatable("energy.modularmagic."+error),true);return InteractionResult.FAIL;}
                clearSelection(stack);
                player.displayClientMessage(Component.translatable("energy.modularmagic.frequency_bound",be.channel),true);
            }
        } else if (be.kind().emitter()) {
            clearSelection(stack);tag=data(stack);tag.putLong("Emitter",be.getBlockPos().asLong());tag.putString("EmitterDimension",level.dimension().location().toString());data(stack,tag);
            player.displayClientMessage(Component.translatable("energy.modularmagic.select_target"),true);
        } else if(be.kind()==EnergyContent.Kind.RESONATOR) {
            clearSelection(stack);tag=data(stack);tag.putLong("Resonator",be.getBlockPos().asLong());tag.putString("ResonatorDimension",level.dimension().location().toString());data(stack,tag);
            player.displayClientMessage(Component.translatable("energy.modularmagic.select_resonance_target"),true);
        } else player.displayClientMessage(be.status(),true);
        return InteractionResult.SUCCESS;
    }
    private static void clearSelection(ItemStack stack) {var tag=data(stack);tag.remove("Emitter");tag.remove("EmitterDimension");tag.remove("Resonator");tag.remove("ResonatorDimension");data(stack,tag);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(receiver)return InteractionResultHolder.pass(player.getItemInHand(hand));
        if(!level.isClientSide) {clearSelection(player.getItemInHand(hand));player.displayClientMessage(Component.translatable("energy.modularmagic.selection_cleared"),true);}
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(receiver?"energy.modularmagic.receiver_help":"energy.modularmagic.tuner_help"));
    }
}
