package cn.nulladev.modularmagic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative settings, loaded and synchronized by NeoForge. No regeneration of item mana. */
public final class ResourceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue BASIC, EXPANDED, ULTIMATE;
    public static final ModConfigSpec.DoubleValue WAND_MANA, WAND_FOCUS;
    public static final ModConfigSpec.DoubleValue FOCUS_MAX, FOCUS_REGEN;
    public static final ModConfigSpec.DoubleValue MISS_REFUND;
    public static final ModConfigSpec.BooleanValue REFUND_MISS_ONLY;
    public static final ModConfigSpec.IntValue PARTICLES_PER_TICK;
    static {
        var b = new ModConfigSpec.Builder();
        b.push("containers");
        BASIC = b.comment("Displayed aether capacity of a basic portable container.").defineInRange("basicCapacity", 100000L, 1L, 1_000_000_000_000L);
        EXPANDED = b.defineInRange("expandedCapacity", 1000000L, 1L, 1_000_000_000_000L);
        ULTIMATE = b.defineInRange("ultimateCapacity", 10000000L, 1L, 1_000_000_000_000L);
        b.pop().push("beginnerWand");
        WAND_MANA = b.comment("Multiplier for survival/adventure casting. Creative game mode is always free.")
                .defineInRange("manaMultiplier", 1.0, 0.0, 1000.0);
        WAND_FOCUS = b.defineInRange("focusMultiplier", 1.0, 0.0, 1000.0);
        b.pop().push("focus");
        FOCUS_MAX = b.defineInRange("maximum", 100.0, 1.0, 2500000.0);
        FOCUS_REGEN = b.comment("Focus regenerated per second. Default 10 with maximum 100 takes 10 seconds from zero; negative debt takes additional time.")
                .defineInRange("regenerationPerSecond", 10.0, 0.0, 2500000.0);
        b.pop().push("refunds");
        MISS_REFUND=b.comment("Fraction of unused reserved mana returned after a complete miss. 0 returns none; 1 returns all. Spent mana is never refunded.")
                .defineInRange("unusedManaRefundRatio",.5,0,1);
        REFUND_MISS_ONLY=b.comment("When true, partial refunds apply only to completely missed entity-effect spells. Successful target attempts, pure cosmetics and block spells return all unused reserves.")
                .define("onlyOnCompleteMiss",true);
        b.pop().push("visuals");
        PARTICLES_PER_TICK=b.comment("Maximum spell particles displayed per client world tick. Excess particles are skipped without changing gameplay, billing or focus regeneration.")
                .defineInRange("particlesPerTick",2048,64,65536);
        b.pop(); SPEC = b.build();
    }
    private ResourceConfig() {}
}
