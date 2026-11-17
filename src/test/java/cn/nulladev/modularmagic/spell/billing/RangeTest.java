package cn.nulladev.modularmagic.spell.billing;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RangeTest {
    @Test void correlatedRandomRatiosRemainBoundedWithoutAllowingArbitraryDivisionByZero(){
        var vars=Map.of("a",new Range(0,1),"b",new Range(0,1));
        assertEquals(new Range(0,1),Range.eval("(a-min(a,b))/(a-b)",vars));
        assertEquals(new Range(-1,0),Range.eval("(b-min(a,b))/(a-b)",vars));
        assertEquals(new Range(0,1),Range.eval("(b-min(b,a))/(b-a)",vars));
        assertThrows(IllegalArgumentException.class,()->Range.eval("(a-min(a,b))/(a-b)",Map.of("a",Range.at(1),"b",Range.at(1))));
        assertThrows(IllegalArgumentException.class,()->Range.eval("1/(a-b)",vars));
    }
    @Test void scalarRulesEvaluateExactly() {
        assertEquals(-4, Range.eval("-2^2", Map.of()).hi());
        assertEquals(.5, Range.eval("sinDegree(30)", Map.of()).hi(), 1e-12);
        assertEquals(1, Range.eval("4>2", Map.of()).hi());
        assertEquals(0, Range.eval("4<2", Map.of()).hi());
        assertEquals(1, Range.eval("7%3", Map.of()).hi());
        assertEquals(2.2, Range.eval("0.5*d+0.0125*d^2", Map.of("d",Range.at(4))).hi(),1e-12);
    }
    @Test void reservationContainsEverySampledActualPrice() {
        var bound=Range.eval("2+sin(d)^2+0.0125*d^2",Map.of("d",new Range(0,20)));
        for(int i=0;i<=200;i++){
            double actual=Range.eval("2+sin(d)^2+0.0125*d^2",Map.of("d",Range.at(i/10D))).hi();
            assertTrue(actual>=bound.lo()&&actual<=bound.hi());
        }
        assertThrows(IllegalArgumentException.class,()->Range.eval("1/d",Map.of("d",new Range(-1,1))));
        assertThrows(IllegalArgumentException.class,()->Range.eval("unknown+1",Map.of()));
    }
}
