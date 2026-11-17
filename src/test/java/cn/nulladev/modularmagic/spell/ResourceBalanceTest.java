package cn.nulladev.modularmagic.spell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourceBalanceTest {
    @Test void refundsRespectTheConfiguredFractionAndKeepConsumptionWhole(){
        assertEquals(450,ResourceBalance.refund(900,.5));
        assertEquals(0,ResourceBalance.refund(900,0));assertEquals(900,ResourceBalance.refund(900,1));
        assertEquals(4,ResourceBalance.refund(9,.5));
        assertThrows(IllegalArgumentException.class,()->ResourceBalance.refund(900,1.1));
    }
    @Test void manaRoundsTheTotalUpWithoutRoundingEachParticle(){
        assertEquals(11,ResourceBalance.wholeMana(10.2));
        assertEquals(12,ResourceBalance.wholeMana(10+10*.2));
        assertEquals(1,ResourceBalance.wholeMana(.000001));
        assertEquals(0,ResourceBalance.wholeMana(0));
        assertEquals(100,ResourceBalance.wholeMana(100.00000000000001));
    }
    @Test void theFixedFocusCostNeverReceivesTheCosmeticDiscount(){
        assertEquals(5,ResourceBalance.focus(0));
        assertTrue(ResourceBalance.focus(.2*.02)>=5);
    }
    @Test void complexityCreatesAnIncreasingMarginalCost(){
        assertEquals(8.05625,ResourceBalance.focus(.3),1e-9);
        assertEquals(17.9,ResourceBalance.focus(1.2),1e-9);
        assertTrue(ResourceBalance.focus(1.2)>2*ResourceBalance.focus(.3));
        assertTrue(ResourceBalance.focus(6)-ResourceBalance.focus(5)>ResourceBalance.focus(1)-ResourceBalance.focus(0));
    }
    @Test void currencyConversionPreservesStoredFixedPointBalances(){
        long oldPrecision=10000,newPrecision=oldPrecision/ResourceBalance.MANA_SCALE;
        assertEquals(2.7*oldPrecision,(2.7*ResourceBalance.MANA_SCALE)*newPrecision,1e-9);
        assertEquals(.2, .0002*ResourceBalance.MANA_SCALE,1e-12);
    }
}
