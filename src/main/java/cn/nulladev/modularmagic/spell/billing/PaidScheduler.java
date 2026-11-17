package cn.nulladev.modularmagic.spell.billing;

import dev.xkmc.l2magic.content.engine.helper.Scheduler;

public final class PaidScheduler extends Scheduler {
    private final CastAccount account;
    public PaidScheduler(CastAccount account){this.account=account;account.scheduler(this);}
    @Override public void schedule(long delay,Runnable task){
        if(account.closed())return;
        if(delay<0||delay+time>account.maxTicks)throw new CastAccount.Stop("gui.modularmagic.cast_limit");
        boolean batched=BillingRuntime.VISUAL_BATCH.get();
        Runnable guarded=()->{boolean before=BillingRuntime.VISUAL_BATCH.get();BillingRuntime.VISUAL_BATCH.set(batched);
            try{if(!account.closed())BillingRuntime.run(account,task);}catch(RuntimeException e){account.abort(e);}finally{BillingRuntime.VISUAL_BATCH.set(before);}};
        if(delay==0){guarded.run();return;}super.schedule(delay,guarded);
    }
    @Override public boolean tick(){if(account.closed())return true;return super.tick();}
}
