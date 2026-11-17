package cn.nulladev.modularmagic.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmoothedGaugeTest {
    @Test void debtAppearsImmediatelyAndRecoveryIsInterpolated(){
        var gauge=new SmoothedGauge(40,0);gauge.update(-14.8,0);
        assertEquals(-14.8,gauge.value(100_000_000));
        gauge.update(-12.8,250_000_000);assertEquals(-14.8,gauge.value(250_000_000));
        assertEquals(-13.8,gauge.value(375_000_000),1e-9);assertEquals(-12.8,gauge.value(500_000_000),1e-9);
    }
    @Test void latePacketsAnimateWithoutInventingRegeneration(){
        var gauge=new SmoothedGauge(5,0);gauge.update(35,2_000_000_000);
        assertEquals(5,gauge.value(2_000_000_000));assertEquals(20,gauge.value(2_125_000_000));
        assertEquals(35,gauge.value(20_000_000_000L));
        gauge.update(-5,20_000_000_000L);assertEquals(-5,gauge.value(20_000_000_000L));
    }
}
