package cn.nulladev.modularmagic.energy;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative balance defaults. Amounts are displayed aether units, rates are per second. */
public final class EnergyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue PASSIVE, SHARD, CRYSTAL_FEE;
    public static final ModConfigSpec.LongValue BASIC_CAPACITY, EXPANDED_CAPACITY, ULTIMATE_CAPACITY, CREATIVE_CAPACITY;
    public static final ModConfigSpec.LongValue DISRUPTOR_CAPACITY, EMITTER_CAPACITY, CONDENSER_CAPACITY, CHAMBER_CAPACITY, RESONATOR_CAPACITY, ANCHOR_CAPACITY, LOADER_CAPACITY;
    public static final ModConfigSpec.LongValue BASIC_RATE, EXPANDED_RATE, ULTIMATE_RATE, CREATIVE_RATE, DISRUPTOR_RATE, CONDENSER_RATE, CHAMBER_RATE, ANCHOR_RATE, LOADER_INPUT_RATE;
    public static final ModConfigSpec.LongValue RESONANCE_UPGRADE_RATE;
    public static final ModConfigSpec.IntValue SHARD_LIFE, PASSIVE_INTERVAL, BATCH_EXTRACT, CRYSTAL_INTERVAL, CRYSTAL_BATCH, STABILIZE_INTERVAL;
    public static final ModConfigSpec.IntValue SHOT, EXPANDED_SHOT, ULTIMATE_SHOT, CREATIVE_SHOT, SHOT_INTERVAL;
    public static final ModConfigSpec.IntValue RANGE, EXPANDED_RANGE, ULTIMATE_RANGE, CREATIVE_RANGE, RESONANCE_RANGE, RESONANCE_UPGRADES, RESONANCE_RANGE_PER_SHARD;
    public static final ModConfigSpec.DoubleValue PULSE_SPEED, PULSE_FREE_DISTANCE, RESONANCE_FREE_DISTANCE, RESONANCE_FREE_PER_SHARD, SAME_DIMENSION_FEE, CROSS_DIMENSION_FEE;
    public static final ModConfigSpec.ConfigValue<String> PULSE_LOSS, RESONANCE_LOSS;
    public static final ModConfigSpec.IntValue PLAYER_RATE, LOADER_RATE, LOADER_START, LOADER_OFFLINE_TICKS, MAX_CHUNKS;
    public static final ModConfigSpec.BooleanValue OFFLINE_LOADING;
    private static final long MAX = 1_000_000_000_000L;
    private static ModConfigSpec.LongValue amount(ModConfigSpec.Builder b,String key,long value) { return b.defineInRange(key,value,1,MAX); }
    static {
        var b = new ModConfigSpec.Builder();
        b.push("fragments");
        SHARD = amount(b,"aetherPerFragment",200);
        CRYSTAL_FEE = b.comment("Extra processing fee, not stored in a fragment and never refunded.").defineInRange("crystallizationFee",200L,0,MAX);
        SHARD_LIFE = b.defineInRange("lifetimeTicks",100,1,72000);
        BATCH_EXTRACT = b.defineInRange("sneakExtractionLimit",16,1,64);
        CRYSTAL_INTERVAL = b.defineInRange("crystallizationIntervalTicks",20,1,72000);
        CRYSTAL_BATCH = b.defineInRange("crystallizationBatch",4,1,64);
        STABILIZE_INTERVAL = b.defineInRange("stabilizationIntervalTicks",5,1,72000);
        b.pop().push("disruptor");
        PASSIVE = amount(b,"aetherPerCycle",20);
        PASSIVE_INTERVAL = b.defineInRange("cycleTicks",40,1,72000);
        DISRUPTOR_CAPACITY = amount(b,"capacity",5000);
        DISRUPTOR_RATE = amount(b,"outputPerSecond",200);
        b.pop().push("storage");
        BASIC_CAPACITY=amount(b,"basicCapacity",1000000);
        EXPANDED_CAPACITY=amount(b,"expandedCapacity",5000000);
        ULTIMATE_CAPACITY=amount(b,"ultimateCapacity",50000000);
        CREATIVE_CAPACITY=b.comment("Display buffer for infinite devices. Their source is never depleted.").defineInRange("creativeDisplayCapacity",1000000000L,1,MAX);
        BASIC_RATE=amount(b,"basicTransferPerSecond",2000);
        EXPANDED_RATE=amount(b,"expandedTransferPerSecond",10000);
        ULTIMATE_RATE=amount(b,"ultimateTransferPerSecond",50000);
        CREATIVE_RATE=amount(b,"creativeTransferPerSecond",1000000);
        b.pop().push("buffers");
        EMITTER_CAPACITY=amount(b,"emitter",10000);
        CONDENSER_CAPACITY=amount(b,"condenser",10000);
        CHAMBER_CAPACITY=amount(b,"crystallizationChamber",10000);
        RESONATOR_CAPACITY=amount(b,"resonator",10000);
        ANCHOR_CAPACITY=amount(b,"personalAnchor",10000);
        LOADER_CAPACITY=amount(b,"worldAnchor",1000);
        CONDENSER_RATE=amount(b,"condenserInputPerSecond",10000);
        CHAMBER_RATE=amount(b,"chamberInputPerSecond",10000);
        ANCHOR_RATE=amount(b,"personalAnchorTransferPerSecond",2000);
        LOADER_INPUT_RATE=amount(b,"worldAnchorInputPerSecond",2000);
        b.pop().push("emitters");
        SHOT=b.defineInRange("basicAetherPerPulse",100,1,1000000);
        EXPANDED_SHOT=b.defineInRange("expandedAetherPerPulse",1000,1,1000000);
        ULTIMATE_SHOT=b.defineInRange("ultimateAetherPerPulse",5000,1,1000000);
        CREATIVE_SHOT=b.defineInRange("creativeAetherPerPulse",1000000,1,1000000000);
        RANGE=b.defineInRange("basicRange",8,1,128);
        EXPANDED_RANGE=b.defineInRange("expandedRange",16,1,128);
        ULTIMATE_RANGE=b.defineInRange("ultimateRange",24,1,128);
        CREATIVE_RANGE=b.defineInRange("creativeRange",64,1,128);
        SHOT_INTERVAL=b.defineInRange("pulseIntervalTicks",10,1,72000);
        PULSE_SPEED=b.comment("Flight speed in blocks per game tick.").defineInRange("blocksPerTick",1D/3,.001,128);
        PULSE_FREE_DISTANCE=b.defineInRange("losslessDistance",8D,0,128);
        PULSE_LOSS=b.comment("Fraction lost after the lossless distance. Default 0: pulses are lossless.",
                "Variables: d=distance, f=losslessDistance, x=max(0,d-f), r=range, u=upgrade count (0 for emitters).",
                "Supports + - * / ^, min/max, abs, sqrt, log, floor/ceil. Result is clamped to [0,1]. Invalid formulas stop transmission.")
                .define("lossFormula","0");
        b.pop().push("resonance");
        RESONANCE_UPGRADE_RATE=amount(b,"bandwidthPerAmethystShard",100);
        RESONANCE_UPGRADES=b.defineInRange("maximumAmethystShards",64,0,64);
        RESONANCE_RANGE=b.defineInRange("baseMaximumDistance",32,1,4096);
        RESONANCE_RANGE_PER_SHARD=b.defineInRange("maximumDistancePerAmethystShard",4,0,128);
        RESONANCE_FREE_DISTANCE=b.defineInRange("baseLosslessDistance",16D,0,4096);
        RESONANCE_FREE_PER_SHARD=b.comment("Effective lossless distance is capped at the effective maximum distance.")
                .defineInRange("losslessDistancePerAmethystShard",2D,0,128);
        RESONANCE_LOSS=b.comment("Fraction of transmitted aether lost. Same variables as emitters.lossFormula.")
                .define("lossFormula","min(0.75,0.05*x)");
        b.pop().push("personalSupply");
        PLAYER_RATE=b.defineInRange("deliveryPerSecond",1000,1,1000000);
        SAME_DIMENSION_FEE=b.comment("Extra source cost per delivered unit, not a percentage integer.").defineInRange("sameDimensionExtraCost",.1,0,100);
        CROSS_DIMENSION_FEE=b.defineInRange("crossDimensionExtraCost",.25,0,100);
        b.pop().push("worldAnchor");
        LOADER_RATE=b.defineInRange("aetherPerChunkSecond",2,0,1000000);
        LOADER_START=b.defineInRange("minimumStartAether",100,0,1000000);
        LOADER_OFFLINE_TICKS=b.defineInRange("offlineGraceTicks",1200,0,720000);
        MAX_CHUNKS=b.defineInRange("maximumChunksPerPlayer",9,1,81);
        OFFLINE_LOADING=b.define("allowOfflineLoading",false);
        b.pop();SPEC=b.build();
    }
    private EnergyConfig() {}
}
