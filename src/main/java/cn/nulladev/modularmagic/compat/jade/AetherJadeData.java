package cn.nulladev.modularmagic.compat.jade;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.item.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.CostPreview;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.Objects;

/** Compact, read-only server snapshots: never claim refunds, simulate extraction or send a whole spell document. */
public final class AetherJadeData {
    public static final String KEY = "ModularMagicAether";

    public static CompoundTag device(EnergyBlockEntity be) {
        var data = new CompoundTag();
        data.putString("Kind", be.kind().name());
        data.putLong("Stored", be.energy / ManaStorage.SCALE);
        data.putLong("Capacity", be.capacity() / ManaStorage.SCALE);
        data.putBoolean("Infinite", be.kind().infinite());
        data.putString("Status", status(be));
        if(be.resonanceParent!=null)data.putLong("Upstream",be.resonanceParent.asLong());
        data.putLong("Bandwidth", be.kind().emitter() ?
                (long)be.shotSize()*20/EnergyConfig.SHOT_INTERVAL.get() : be.rate());
        if (be.kind().disrupts()) data.putDouble("Production", EnergyConfig.PASSIVE.get()*20D/EnergyConfig.PASSIVE_INTERVAL.get());
        if (be.kind().emitter()) {
            if (be.target != null) data.putLong("Target", be.target.asLong());
            data.putLong("InFlight", be.pulseAmount / ManaStorage.SCALE);
            data.putInt("Range", be.shotRange());
        }
        if(be.kind()==EnergyContent.Kind.CRYSTAL_CHAMBER)data.putInt("Products",be.product.getCount());
        if (be.kind() == EnergyContent.Kind.RESONATOR) {
            data.putInt("Upgrades",be.resonanceUpgrades);data.putInt("MaxUpgrades",EnergyConfig.RESONANCE_UPGRADES.get());
            data.putDouble("LosslessDistance",be.resonanceLosslessDistance());
            data.putInt("Channel", be.channel);
            data.putInt("Range", be.resonanceRange());
        }
        if (be.kind() == EnergyContent.Kind.ANCHOR) {
            data.putDouble("LocalFee",EnergyConfig.SAME_DIMENSION_FEE.get()*100);data.putDouble("CrossFee",EnergyConfig.CROSS_DIMENSION_FEE.get()*100);
            data.putBoolean("CrossDimension", be.crossDimension);
            data.putInt("PlayerRate", EnergyConfig.PLAYER_RATE.get());
        }
        if (be.kind() == EnergyContent.Kind.WORLD_ANCHOR) {
            data.putLong("StartCost",EnergyConfig.LOADER_START.get());
            int chunks = be.loaderRadius == 0 ? 1 : 9;
            data.putInt("Chunks", chunks);
            data.putLong("Upkeep", (long)chunks * EnergyConfig.LOADER_RATE.get());
        }
        if (!be.battery.isEmpty() && be.battery.getItem() instanceof ManaStorage storage) {
            data.putString("BatteryName", be.battery.getDescriptionId());
            var battery = new CompoundTag();
            battery.putLong("Stored", storage.stored(be.battery));
            battery.putLong("Capacity", storage.capacity(be.battery));
            battery.putBoolean("Infinite", storage.infinite(be.battery));
            battery.putLong("Reserved", ManaContainerItem.reserved(be.battery)/ManaStorage.SCALE);
            data.put("Battery", battery);
        }
        return data;
    }

    public static String status(EnergyBlockEntity be) {
        if(be.kind()==EnergyContent.Kind.RESONATOR&&!be.resonanceEnabled())return "amethyst_required";
        if (be.paused()) return "paused";
        if(be.kind()==EnergyContent.Kind.RESONATOR&&ResonanceNetwork.root(be)==null)return "upstream_asleep";
        if (be.kind().infinite()&&!be.kind().emitter()) return "generating";
        if (be.kind() == EnergyContent.Kind.WORLD_ANCHOR) {
            if (!be.loaderEnabled) return "disabled";
            if (be.appliedRadius >= 0) return "loading";
            if (be.energy < (long)EnergyConfig.LOADER_START.get()*ManaStorage.SCALE) return "loader_charge";
            return "loader_waiting";
        }
        if (be.kind() == EnergyContent.Kind.DISRUPTOR && !be.waterStructure()) return "water_required";
        if (be.kind() == EnergyContent.Kind.CONDENSER) return be.energy>=EnergyConfig.SHARD.get()*ManaStorage.SCALE?"fragment_ready":"empty";
        if (be.kind()==EnergyContent.Kind.CRYSTAL_CHAMBER) {
            if(be.product.getCount()>=64)return "output_full";
            if(!be.product.isEmpty())return "product_ready";
            return be.energy>=be.crystalCost()?"crystallizing":"empty";
        }
        if (be.kind().emitter()) return emitterStatus(be);
        if (be.energy >= be.capacity()) return "full";
        if (be.kind().disrupts() && be.waterStructure()) return "collecting";
        if (be.energy == 0) return "empty";
        if (!be.battery.isEmpty() && be.battery.getItem() instanceof ManaStorage storage) {
            return storage.infinite(be.battery) || storage.storedUnits(be.battery)+ManaContainerItem.reserved(be.battery) >= storage.capacity(be.battery)*ManaStorage.SCALE ? "charged" : "charging";
        }
        return "ready";
    }

    private static String emitterStatus(EnergyBlockEntity be) {
        if (be.pulseAmount > 0) return "in_flight";
        if (be.target == null) return "unbound";
        if (be.target.distSqr(be.getBlockPos()) > be.shotRange()*be.shotRange()) return "out_of_range";
        if (!(be.getLevel() instanceof ServerLevel level) || !level.hasChunkAt(be.target)) return "target_unloaded";
        var target = EnergyWorld.device(level, be.target);
        if (target == null || !Objects.equals(be.targetIdentity, target.identity)) return "target_missing";
        if (!be.sameOwner(target)) return "foreign_target";
        var start = be.emissionStart();
        var end = Vec3.atCenterOf(be.target);
        int steps = Math.max(1,(int)Math.ceil(start.distanceTo(end)*2));
        for (int i=0; i<=steps; i++) if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(start.lerp(end,(double)i/steps)))) return "target_unloaded";
        var hit = level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.MISS && !hit.getBlockPos().equals(be.target)) return "blocked";
        if (be.energy == 0) return "empty";
        if (target.room() == 0) return "target_full";
        return "ready";
    }

    public static CompoundTag item(ItemStack stack, Level level) {
        var data = new CompoundTag();
        if (stack.getItem() instanceof ManaStorage storage) {
            data.putString("Type", "container");
            data.putLong("Stored", storage.stored(stack));
            data.putLong("Capacity", storage.capacity(stack));
            data.putBoolean("Infinite", storage.infinite(stack));
            data.putLong("Reserved", ManaContainerItem.reserved(stack)/ManaStorage.SCALE);
        } else if (stack.getItem() instanceof AetherShardItem shard) {
            data.putString("Type", "fragment");
            data.putBoolean("Stable", shard.stable);
            data.putLong("Value", AetherShardItem.value(stack));
            if (!shard.stable) data.putLong("Remaining", AetherShardItem.remainingTicks(stack,level));
        } else if (stack.getItem() instanceof CoreWandItem wand) {
            data.putString("Type", "wand");
            data.putDouble("ManaMultiplier", wand.manaMultiplier(stack));
            data.putDouble("FocusMultiplier", wand.focusMultiplier(stack));
            data.putInt("Cores", (int)WandCores.read(stack).stream().filter(s -> !s.isEmpty()).count());
        } else if (stack.getItem() instanceof SpellCoreItem) {
            data.putString("Type", "core");
            data.putBoolean("Formed", stack.is(ModContent.FORMED_CORE.get()));
            if (stack.is(ModContent.FORMED_CORE.get())) {
                try {
                    var quote = CostPreview.estimate(SpellCoreItem.read(stack));
                    data.putLong("Mana", (long)Math.ceil(quote.mana()));data.putDouble("Focus", quote.focus());
                } catch (RuntimeException ignored) { }
            }
        } else if (stack.getItem() instanceof EnergyLinkItem link && link.receiver) {
            data.putString("Type", "receiver");
            data.putBoolean("Bound", EnergyLinkItem.data(stack).hasUUID("Device"));
            // A dropped receiver is not carried and therefore is not receiving energy.
        } else if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof EnergyBlock block) {
            data.putString("Type", "device");
            data.putLong("Stored", Math.max(0,EnergyLinkItem.data(stack).getCompound("AetherDevice").getLong("Energy"))/ManaStorage.SCALE);
            data.putBoolean("Infinite", block.kind.infinite());
        }
        return data;
    }
    private AetherJadeData() {}
}
