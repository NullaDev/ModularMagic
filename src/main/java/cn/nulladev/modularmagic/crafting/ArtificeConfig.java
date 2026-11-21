package cn.nulladev.modularmagic.crafting;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArtificeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue ENCHANT_CAPACITY,ENCHANT_RATE,AETHER_PER_ANVIL_LEVEL,REPAIR_PER_POINT;
    public static final ModConfigSpec.IntValue REPAIR_INTERVAL,REPAIR_POINTS;
    public static final ModConfigSpec.DoubleValue ARMOR_DISCOUNT;
    static {
        var b=new ModConfigSpec.Builder();
        ENCHANT_CAPACITY=b.defineInRange("enchanterCapacity",2000000L,1,1000000000000L);
        ENCHANT_RATE=b.defineInRange("enchanterInputPerSecond",6400L,1,1000000000000L);
        AETHER_PER_ANVIL_LEVEL=b.defineInRange("aetherPerAnvilLevel",2000L,1,1000000000L);
        REPAIR_PER_POINT=b.defineInRange("aetherPerDurability",100L,1,1000000000L);
        REPAIR_INTERVAL=b.defineInRange("repairIntervalTicks",20,1,72000);
        REPAIR_POINTS=b.defineInRange("repairPointsPerInterval",2,1,10000);
        ARMOR_DISCOUNT=b.defineInRange("fullArmorManaDiscount",0.05,0,0.9);
        SPEC=b.build();
    }
    private ArtificeConfig(){}
}
