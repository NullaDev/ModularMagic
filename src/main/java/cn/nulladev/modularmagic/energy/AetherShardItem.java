package cn.nulladev.modularmagic.energy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.List;

public final class AetherShardItem extends Item {
    public final boolean stable;
    public AetherShardItem(boolean stable) { super(new Properties().rarity(stable?Rarity.UNCOMMON:Rarity.COMMON)); this.stable = stable; }
    public static long clock(Level level) { return level.getServer() == null ? level.getGameTime() : level.getServer().overworld().getGameTime(); }
    public static ItemStack create(boolean stable, Level level) {
        var stack = new ItemStack(stable ? EnergyContent.CRYSTAL.get() : EnergyContent.SHARD.get());
        var data = new CompoundTag();
        data.putLong("AetherValue", EnergyConfig.SHARD.get());
        if (!stable) data.putLong("AetherExpires", clock(level) + EnergyConfig.SHARD_LIFE.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); return stack;
    }
    public static boolean expired(ItemStack stack, Level level) {
        if (!(stack.getItem() instanceof AetherShardItem shard) || shard.stable) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains("AetherExpires") || clock(level) >= tag.getLong("AetherExpires");
    }
    public static long remainingTicks(ItemStack stack, Level level) {
        if (!(stack.getItem() instanceof AetherShardItem shard) || shard.stable) return 0;
        long end = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("AetherExpires");
        return Math.max(0, end - clock(level));
    }
    public static Component countdown(ItemStack stack, Level level) {
        return Component.translatable("energy.modularmagic.fragment_time", remainingTicks(stack, level));
    }
    public static boolean expiredOnServer(ItemStack stack) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null && expired(stack, server.overworld());
    }
    public static long value(ItemStack stack) {
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(1_000_000_000_000L, data.contains("AetherValue")?data.getLong("AetherValue"):EnergyConfig.SHARD.get()));
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && expired(stack, level)) stack.setCount(0);
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !stable; }
    @Override public int getBarWidth(ItemStack stack) {
        return (int)Math.clamp(13L * remainingForDisplay(stack) / Math.max(1,EnergyConfig.SHARD_LIFE.get()), 0,13);
    }
    private static long remainingForDisplay(ItemStack stack) {
        // A client must use its own ticking clock, including in an integrated-server world.
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return ClientClock.remaining(stack);
        var server=ServerLifecycleHooks.getCurrentServer();
        return server==null?0:remainingTicks(stack,server.overworld());
    }
    private static final class ClientClock {
        static long remaining(ItemStack stack) {
            var level=net.minecraft.client.Minecraft.getInstance().level;
            return level==null?0:remainingTicks(stack,level);
        }
    }
    @Override public int getBarColor(ItemStack stack) { return 0x62d8e8; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("energy.modularmagic.fragment_value", cn.nulladev.modularmagic.util.AetherNumbers.format(value(stack))));
        if (!stable) lines.add(Component.translatable("energy.modularmagic.fragment_time", remainingForDisplay(stack)));
    }
}
