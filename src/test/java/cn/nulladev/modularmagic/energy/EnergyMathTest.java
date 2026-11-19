package cn.nulladev.modularmagic.energy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnergyMathTest {
    @Test void losslessDistanceAndCustomFormulasUseTheConfiguredVariables() {
        assertEquals(1,EnergyMath.efficiency("min(0.75,0.05*x)",8,8,16,0));
        assertEquals(.8,EnergyMath.efficiency("min(0.75,0.05*x)",12,8,16,0),1e-12);
        assertEquals(.75,EnergyMath.efficiency("x/r/(u+1)",16,8,16,1),1e-12);
        assertEquals(0,EnergyMath.efficiency("2",12,8,16,0));
        assertEquals(1,EnergyMath.efficiency("-1",12,8,16,0));
        assertEquals(0,EnergyMath.efficiency("1/0",12,8,16,0));
    }
    @Test void LossAndRefundsNeverCreateEnergyAtTheFixedPointBoundary() {
        for(double efficiency:new double[]{1,.99,.8,.123,.001})for(long sent:new long[]{1,7,250,10000}) {
            long delivered=EnergyMath.delivered(sent,efficiency),cost=EnergyMath.sourceCost(delivered,efficiency);
            assertTrue(delivered<=cost&&cost<=sent);
        }
        assertEquals(200,EnergyMath.delivered(250,.8));
        assertEquals(250,EnergyMath.sourceCost(200,.8));
    }
    @Test void SmallConfiguredRatesAreNotTruncatedEveryQuarterSecond() {
        long total=0;for(int i=0;i<4;i++)total+=EnergyMath.quarterUnits(1,i);
        assertEquals(cn.nulladev.modularmagic.mana.ManaStorage.SCALE,total);
    }
}
