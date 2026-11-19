package cn.nulladev.modularmagic.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared, locale-independent display of aether quantities. Does not change stored values. */
public final class AetherNumbers {
    public static String format(long value) { return compact(BigDecimal.valueOf(value)); }
    public static String format(double value) {
        if (!Double.isFinite(value)) return value > 0 ? "∞" : value < 0 ? "-∞" : "—";
        return compact(BigDecimal.valueOf(value));
    }
    private static String compact(BigDecimal value) {
        String[] suffixes = {"", "K", "M", "B"};
        int tier = 0;
        while (tier < 3 && value.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            value = value.movePointLeft(3);
            tier++;
        }
        value = value.setScale(2, RoundingMode.HALF_UP);
        if (tier < 3 && value.abs().compareTo(BigDecimal.valueOf(1000)) >= 0) {
            value = value.movePointLeft(3).setScale(2, RoundingMode.HALF_UP);
            tier++;
        }
        return value.stripTrailingZeros().toPlainString() + suffixes[tier];
    }
    private AetherNumbers() {}
}
