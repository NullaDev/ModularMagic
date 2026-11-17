package cn.nulladev.modularmagic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative settings, loaded and synchronized by NeoForge. No regeneration of item mana. */
public final class ResourceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue SMALL, MEDIUM, LARGE;
    public static final ModConfigSpec.DoubleValue WAND_MANA, WAND_FOCUS;
    public static final ModConfigSpec.DoubleValue FOCUS_MAX, FOCUS_REGEN;
    public static final ModConfigSpec.DoubleValue MISS_REFUND;
    public static final ModConfigSpec.BooleanValue REFUND_MISS_ONLY;
    public static final ModConfigSpec.IntValue PARTICLES_PER_TICK;
    private static final ModConfigSpec.IntValue UNIT_VERSION;
    private static final ModConfigSpec.IntValue FOCUS_VERSION;
    static {
        var b = new ModConfigSpec.Builder();
        UNIT_VERSION=b.comment("Automatic mana currency migration version; do not edit.").defineInRange("manaUnitVersion",1,1,2);
        FOCUS_VERSION=b.comment("Automatic focus unit/recovery migration version; do not edit.").defineInRange("focusBalanceVersion",3,1,3);
        b.push("containers");
        SMALL = b.comment("Displayed mana capacity. Legacy defaults convert once to 100000/1000000/10000000 on loading.").defineInRange("smallCapacity", 100L, 1L, 1_000_000_000_000L);
        MEDIUM = b.defineInRange("mediumCapacity", 1000L, 1L, 1_000_000_000_000L);
        LARGE = b.defineInRange("largeCapacity", 10000L, 1L, 1_000_000_000_000L);
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
    public static void migrate(net.neoforged.fml.config.ModConfig config){
        if(config.getSpec()!=SPEC)return;
        boolean changed=false;
        if(UNIT_VERSION.get()<2){
            for(var capacity:java.util.List.of(SMALL,MEDIUM,LARGE))capacity.set(Math.multiplyExact(capacity.get(),cn.nulladev.modularmagic.spell.ResourceBalance.MANA_SCALE));
            UNIT_VERSION.set(2);changed=true;
        }
        if(FOCUS_VERSION.get()<3){
            if(FOCUS_VERSION.get()<2&&FOCUS_MAX.get()==40&&FOCUS_REGEN.get()==8)FOCUS_REGEN.set(4D);
            FOCUS_MAX.set(FOCUS_MAX.get()*cn.nulladev.modularmagic.spell.ResourceBalance.FOCUS_SCALE);
            FOCUS_REGEN.set(FOCUS_REGEN.get()*cn.nulladev.modularmagic.spell.ResourceBalance.FOCUS_SCALE);
            FOCUS_VERSION.set(3);changed=true;
        }
        if(changed)config.getLoadedConfig().save();
    }
    private ResourceConfig() {}
}
