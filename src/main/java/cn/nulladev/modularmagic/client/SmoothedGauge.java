package cn.nulladev.modularmagic.client;

/** Display-only interpolation. Spending is immediate; recovery joins successive server snapshots. */
public final class SmoothedGauge {
    private double start,target;
    private long changed;
    private static final double DURATION=250_000_000D;
    public SmoothedGauge(double value,long now){start=target=value;changed=now;}
    public double target(){return target;}
    public double value(long now){double fraction=Math.clamp((now-changed)/DURATION,0,1);return start+(target-start)*fraction;}
    public void update(double next,long now){
        double current=value(now);start=next<target?next:current;target=next;changed=now;
    }
}
