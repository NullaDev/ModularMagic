package cn.nulladev.modularmagic.utility;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UtilityConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue CRAFT,CACHE,MATERIALIZER_CAPACITY,MATERIALIZER_RATE;
    public static final ModConfigSpec.IntValue CANNON_COOLDOWN,MATERIALIZER_INTERVAL;
    public static final ModConfigSpec.DoubleValue CANNON_SPEED,CANNON_LIFT;
    static {
        var b=new ModConfigSpec.Builder();
        CACHE=b.defineInRange("unitCache",64000L,1,1_000_000_000L);
        MATERIALIZER_CAPACITY=b.defineInRange("materializerCapacity",128000L,1,1_000_000_000L);
        MATERIALIZER_RATE=b.defineInRange("materializerInputPerSecond",12000L,1,1_000_000_000L);
        MATERIALIZER_INTERVAL=b.defineInRange("materializerInterval",20,1,1200);
        CRAFT=b.comment("Cost per completed crafting operation, plus generation fees for missing mimic materials.").defineInRange("craftingAether",100L,0,1_000_000_000L);
        CANNON_COOLDOWN=b.defineInRange("cannonCooldownTicks",40,0,1200);
        CANNON_SPEED=b.defineInRange("cannonSpeed",2.,0,10);
        CANNON_LIFT=b.defineInRange("cannonMinimumLift",.9,0,10);
        SPEC=b.build();
    }
    private UtilityConfig(){}
}
