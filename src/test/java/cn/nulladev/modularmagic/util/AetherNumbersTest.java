package cn.nulladev.modularmagic.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AetherNumbersTest {
    @Test void quantitiesUseTheSameSuffixesAtEveryScale() {
        assertEquals("0", AetherNumbers.format(0));
        assertEquals("999", AetherNumbers.format(999));
        assertEquals("1K", AetherNumbers.format(1000));
        assertEquals("3.46K", AetherNumbers.format(3456));
        assertEquals("100K", AetherNumbers.format(100_000));
        assertEquals("5M", AetherNumbers.format(5_000_000));
        assertEquals("1.25B", AetherNumbers.format(1_250_000_000));
        assertEquals("1000B", AetherNumbers.format(1_000_000_000_000L));
    }
    @Test void roundingPromotesSuffixesAndPreservesSmallRates() {
        assertEquals("1M", AetherNumbers.format(999_999));
        assertEquals("1B", AetherNumbers.format(999_999_999));
        assertEquals("0.2", AetherNumbers.format(.2));
        assertEquals("-1.25K", AetherNumbers.format(-1250));
        assertEquals("-9223372036.85B", AetherNumbers.format(Long.MIN_VALUE));
    }
}
