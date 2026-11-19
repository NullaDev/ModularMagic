package cn.nulladev.modularmagic.energy;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.spell.billing.Range;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Pure distance rules and conservative fixed-point transfer accounting. */
public final class EnergyMath {
    public static long quarterUnits(long rate,long window) {
        long units=rate*cn.nulladev.modularmagic.mana.ManaStorage.SCALE;
        return units/4+((window&3)<units%4?1:0);
    }
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    public static double efficiency(String formula,double distance,double free,double range,int upgrades) {
        if(distance<=free)return 1;
        try {
            if(formula.matches("(?s).*\\brand\\s*\\(.*"))throw new IllegalArgumentException("Random loss is not supported");
            var result=Range.eval(formula,Map.of("d",Range.at(distance),"f",Range.at(free),
                    "x",Range.at(Math.max(0,distance-free)),"r",Range.at(range),"u",Range.at(upgrades)));
            return 1-Math.clamp(result.hi(),0,1);
        } catch(RuntimeException e) {
            if(WARNED.add(formula))ModularMagic.LOGGER.warn("Invalid aether loss formula '{}'; transmission stopped",formula,e);
            return 0;
        }
    }
    public static long delivered(long sent,double efficiency) {
        if(sent<=0||efficiency<=0)return 0;
        return BigDecimal.valueOf(sent).multiply(BigDecimal.valueOf(Math.min(1,efficiency))).setScale(0,RoundingMode.FLOOR).longValueExact();
    }
    public static long sourceCost(long delivered,double efficiency) {
        if(delivered<=0)return 0;
        if(efficiency<=0)return Long.MAX_VALUE;
        return BigDecimal.valueOf(delivered).divide(BigDecimal.valueOf(Math.min(1,efficiency)),0,RoundingMode.CEILING).min(BigDecimal.valueOf(Long.MAX_VALUE)).longValueExact();
    }
    private EnergyMath() {}
}
