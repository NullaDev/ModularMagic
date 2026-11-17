package cn.nulladev.modularmagic.item;

import cn.nulladev.modularmagic.menu.WandMenu;
import cn.nulladev.modularmagic.network.CastVisual;
import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import dev.xkmc.l2magic.content.engine.context.SpellContext;
import dev.xkmc.l2magic.content.engine.spell.*;
import dev.xkmc.l2magic.content.item.spell.WandItem;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;

public final class CoreWandItem extends WandItem {
    private final boolean creative;
    public CoreWandItem(Properties properties) { this(properties, false); }
    public CoreWandItem(Properties properties, boolean creative) { super(properties); this.creative = creative; }
    public double manaMultiplier(ItemStack stack) { return creative ? 0 : cn.nulladev.modularmagic.config.ResourceConfig.WAND_MANA.get(); }
    public double focusMultiplier(ItemStack stack) { return creative ? 0 : cn.nulladev.modularmagic.config.ResourceConfig.WAND_FOCUS.get(); }
    private static ItemStack core(ItemStack stack) { return WandCores.read(stack).get(WandCores.selected(stack)); }
    private static String document(ItemStack stack) { return SpellCoreItem.read(core(stack)).toJson(); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inventory, user) -> new WandMenu(id, inventory, hand),
                            Component.translatable("gui.modularmagic.wand_inventory")), buf -> buf.writeEnum(hand));
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        try {
            var spell = RuntimeSpells.get(level, document(stack)).action();
            if (spell.castType() != SpellCastType.INSTANT) return ItemUtils.startUsingInstantly(level, player, hand);
            if (level.isClientSide() || cast(stack, player, 0)) {
                if (!level.isClientSide()) player.getCooldowns().addCooldown(this, 10);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.fail(stack);
        } catch (RuntimeException ignored) {}
        if (!level.isClientSide()) player.displayClientMessage(Component.translatable("gui.modularmagic.wand_invalid"), true);
        return InteractionResultHolder.fail(stack);
    }

    public boolean cast(ItemStack stack, Player player, int useTicks) {
        if (player.level().isClientSide()) return false;
        try {
            String json = document(stack);
            var runtime = RuntimeSpells.get(player.level(), json);
            var context = SpellContext.castSpell(player, runtime.action(), useTicks, 1, getDistance(stack), 0);
            if (context == null) return false;
            var anchor = player.level().registryAccess().registryOrThrow(EngineRegistry.SPELL).getHolder(RuntimeSpells.ANCHOR).orElseThrow();
            if (player.isCreative()) {
                if(!runtime.action().test(anchor,context))return false;
                RuntimeSpells.execute(player.level(), json, context);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, CastVisual.of(json, context));
            } else {
                if(!CastCosts.tryConsume(player,stack,core(stack),context))return false;
                return cn.nulladev.modularmagic.spell.billing.PaidCasting.execute((ServerPlayer)player,stack,json,context,runtime);
            }
            return true;
        } catch (cn.nulladev.modularmagic.spell.billing.CastAccount.Stop e) {
            player.displayClientMessage(Component.translatable(e.getMessage()),true);
            player.stopUsingItem();return false;
        } catch (RuntimeException e) {
            player.displayClientMessage(Component.translatable("gui.modularmagic.cast_failed", e.getMessage()), true);
            return false;
        }
    }

    @Override public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(user instanceof Player player)) return;
        try {
            if (RuntimeSpells.get(level, document(stack)).action().castType() == SpellCastType.CONTINUOUS)
                if(!cast(stack, player, getUseDuration(stack, user) - remaining))user.stopUsingItem();
        } catch (RuntimeException ignored) { user.stopUsingItem(); }
    }
    @Override public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level.isClientSide() || !(user instanceof Player player)) return;
        try {
            if (RuntimeSpells.get(level, document(stack)).action().castType() == SpellCastType.CHARGE)
                cast(stack, player, getUseDuration(stack, user) - remaining);
        } catch (RuntimeException ignored) {}
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!selected || !level.isClientSide() || !(entity instanceof Player player) || core(stack).isEmpty()) return;
        try {
            if (RuntimeSpells.get(level, document(stack)).action().triggerType() == SpellTriggerType.TARGET_ENTITY)
                RayTraceUtil.clientUpdateTarget(player, getDistance(stack));
        } catch (RuntimeException ignored) {}
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var cores = WandCores.read(stack);
        tooltip.add(Component.translatable("tooltip.modularmagic.wand_count", cores.stream().filter(c -> !c.isEmpty()).count(), WandCores.CAPACITY));
        tooltip.add(Component.translatable("tooltip.modularmagic.wand_multipliers", manaMultiplier(stack), focusMultiplier(stack)));
        if (!core(stack).isEmpty()) tooltip.add(Component.translatable("tooltip.modularmagic.wand_selected", core(stack).getHoverName()));
    }
}
