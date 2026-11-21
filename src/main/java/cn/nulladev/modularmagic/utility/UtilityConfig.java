package cn.nulladev.modularmagic.utility;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UtilityConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.LongValue FERTILIZER,FOOD,CANNON,CRAFT;
    public static final ModConfigSpec.IntValue CANNON_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CANNON_SPEED,CANNON_LIFT;
    static {
        var b=new ModConfigSpec.Builder();
        FERTILIZER=b.defineInRange("fertilizerAether",1000L,0,1_000_000_000L);
        FOOD=b.defineInRange("foodAether",300L,0,1_000_000_000L);
        CANNON=b.defineInRange("cannonAether",4000L,0,1_000_000_000L);
        CRAFT=b.comment("Cost per completed crafting operation, plus generation fees for missing mimic materials.").defineInRange("craftingAether",100L,0,1_000_000_000L);
        CANNON_COOLDOWN=b.defineInRange("cannonCooldownTicks",40,0,1200);
        CANNON_SPEED=b.defineInRange("cannonSpeed",2.,0,10);
        CANNON_LIFT=b.defineInRange("cannonMinimumLift",.9,0,10);
        SPEC=b.build();
    }
    private UtilityConfig(){}
}
