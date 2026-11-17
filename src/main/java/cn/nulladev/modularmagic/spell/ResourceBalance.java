package cn.nulladev.modularmagic.spell;

/** Shared currency conversion and default complexity curve for billing and tooltips. */
public final class ResourceBalance {
    public static final long MANA_SCALE = 1000;
    public static final double FOCUS_SCALE = 2.5;
    public static final int FOCUS_UNIT_VERSION = 2;
    public static final String ROOT_FOCUS = "2+4*children+0.25*children^2";
    public static double focus(double complexity) { return FOCUS_SCALE * (2 + 4 * complexity + .25 * complexity * complexity); }
    public static double cosmeticStartup() { return .01 * MANA_SCALE; }
    public static double positionQuery() { return .001 * MANA_SCALE; }
    public static long wholeMana(double value) {
        if(!Double.isFinite(value)||value<0||value>1e12)throw new IllegalArgumentException("Invalid mana amount");
        return value==0?0:Math.max(1,(long)Math.ceil(value-1e-8));
    }
    public static long refund(long unusedMana,double ratio){
        if(unusedMana<0||!Double.isFinite(ratio)||ratio<0||ratio>1)throw new IllegalArgumentException("Invalid refund");
        return java.math.BigDecimal.valueOf(unusedMana).multiply(java.math.BigDecimal.valueOf(ratio)).setScale(0,java.math.RoundingMode.FLOOR).longValueExact();
    }
    private ResourceBalance() {}
}
