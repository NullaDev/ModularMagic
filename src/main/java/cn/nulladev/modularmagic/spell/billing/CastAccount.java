package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.item.*;
import cn.nulladev.modularmagic.mana.*;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.engine.helper.Scheduler;
import dev.xkmc.l2magic.content.entity.core.ProjectileConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.*;

/** One finite, independently funded cast. All gameplay effects share this account. */
public final class CastAccount {
    public static final class Stop extends RuntimeException { public Stop(String reason){super(reason,null,false,false);} }
    public final UUID id=UUID.randomUUID();
    public final ServerPlayer player;
    public final double multiplier;
    public final boolean infinite;
    public final long started;
    public final net.minecraft.world.phys.Vec3 origin;
    private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    public final Map<ResourceLocation,ProjectileConfig> projectiles=new HashMap<>();
    public final Set<Entity> entities=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Scheduler> schedulers=Collections.newSetFromMap(new IdentityHashMap<>());
    private final ManaEscrow escrow;
    private final List<ItemStack> sources=new ArrayList<>();
    private long remaining;private double spent,compensation;private long committed;private int work,hits;private boolean closed,rootRunning=true;
    public final int targets,maxTicks,maxWork;public final double range;
    private final Map<String,String> prices=CostRules.snapshot();
    private final Map<String,Double> constants=new HashMap<>();
    private final Map<UUID,Integer> hitCounts=new HashMap<>();
    private final double refundRatio=cn.nulladev.modularmagic.config.ResourceConfig.MISS_REFUND.get();
    private final boolean missOnly=cn.nulladev.modularmagic.config.ResourceConfig.REFUND_MISS_ONLY.get();
    private boolean startedEffects,combat,targetAttempted;
    private CastAccount(ServerPlayer player,double multiplier,boolean infinite,net.minecraft.world.phys.Vec3 origin){
        this.player=player;this.multiplier=multiplier;this.infinite=infinite;this.origin=origin;dimension=player.level().dimension();started=player.level().getGameTime();escrow=ManaEscrow.get(player.getServer());
        targets=CostRules.TARGETS.get();maxTicks=CostRules.TICKS.get();maxWork=CostRules.STEPS.get();range=CostRules.RADIUS.get()*4;
        prices.forEach((key,formula)->{try{constants.put(key,CostRules.evaluate(formula,Map.of()));}catch(RuntimeException ignored){}});
    }
    public static CastAccount open(ServerPlayer player,ItemStack wand,dev.xkmc.l2magic.content.engine.context.SpellContext context,SpellBudget.Plan plan){
        var item=(CoreWandItem)wand.getItem();double rate=item.manaMultiplier(wand)*(cn.nulladev.modularmagic.crafting.ArtificeContent.fullArmor(player)?1-cn.nulladev.modularmagic.crafting.ArtificeConfig.ARMOR_DISCOUNT.get():1);
        var inventory=new ArrayList<ItemStack>();inventory.addAll(player.getInventory().items);inventory.addAll(player.getInventory().offhand);
        boolean infinite=rate==0||inventory.stream().anyMatch(s->s.getItem() instanceof ManaStorage m&&m.infinite(s));
        var a=new CastAccount(player,rate,infinite,context.origin());
        long needed=units(plan.mana()*rate),available=0;
        for(var stack:inventory)if(stack.getCount()==1&&stack.getItem() instanceof ManaContainerItem mana&&!mana.infinite(stack)){
            a.escrow.claim(stack);available=Math.min(Long.MAX_VALUE/2,available+mana.extractUnits(stack,Long.MAX_VALUE,true));
        }
        if(!infinite&&available<needed)throw new Stop("gui.modularmagic.not_enough_mana");
        a.remaining=needed;
        var held=new LinkedHashMap<UUID,Long>();long left=needed;
        if(!infinite)for(var stack:inventory)if(left>0&&stack.getCount()==1&&stack.getItem() instanceof ManaContainerItem mana&&!mana.infinite(stack)){
            long moved=mana.extractUnits(stack,left,false);if(moved>0){UUID source=ManaEscrow.identity(stack);held.merge(source,moved,Math::addExact);a.sources.add(stack);left-=moved;}
        }
        a.escrow.open(a.id,held);
        for(var source:a.sources)ManaContainerItem.reserved(source,a.escrow.outstanding(ManaEscrow.identity(source)));
        PlayerFocus.consume(player,plan.focus()*item.focusMultiplier(wand));
        return a;
    }
    public static long units(double value){try{return cn.nulladev.modularmagic.spell.ResourceBalance.wholeMana(value)*ManaStorage.SCALE;}catch(IllegalArgumentException e){throw new Stop("gui.modularmagic.invalid_budget");}}
    public void check(EngineContext ctx){
        if(!valid()||!dimension.equals(ctx.user().level().dimension()))throw new Stop("gui.modularmagic.cast_cancelled");
        if(++work>maxWork||ctx.loc().pos().distanceToSqr(origin)>range*range)throw new Stop("gui.modularmagic.cast_limit");
    }
    public void charge(double amount){
        if(closed)throw new Stop("gui.modularmagic.cast_cancelled");
        Parameters.check(amount);if(amount<0)throw new Stop("gui.modularmagic.invalid_budget");
        if(amount==0||multiplier==0)return;
        double addition=amount*multiplier-compensation,next=spent+addition;
        compensation=(next-spent)-addition;long total=units(next),due=total-committed;
        if(!infinite&&due>remaining)throw new Stop("gui.modularmagic.budget_exhausted");
        if(!infinite&&due>0){escrow.spend(id,due);remaining-=due;}
        spent=next;committed=total;
    }
    public boolean hit(UUID entity){int count=hitCounts.merge(entity,1,Integer::sum);return count<=targets;}
    public void begin(boolean combat){startedEffects=true;this.combat=combat;}
    public void targetAttempt(){targetAttempted=true;}
    public Double constantPrice(String id){return constants.get(id);}
    public double price(String id,Map<String,Double> values){var constant=constants.get(id);return constant==null?CostRules.evaluate(prices.get(id),values):constant;}
    public boolean freeRule(String id){try{return Double.parseDouble(prices.get(id).trim())==0;}catch(NumberFormatException e){return false;}}
    public void scheduler(Scheduler scheduler){schedulers.add(scheduler);}
    public void rootDone(){rootRunning=false;}
    public void tick(){
        if(closed)return;
        if(!valid()||player.level().getGameTime()-started>maxTicks){close(true);return;}
        entities.removeIf(Entity::isRemoved);schedulers.removeIf(Scheduler::isFinished);
        if(!rootRunning&&entities.isEmpty()&&schedulers.isEmpty())close(false);
    }
    public boolean closed(){return closed;}
    public boolean valid(){return !closed&&player.isAlive()&&!player.hasDisconnected()&&dimension.equals(player.level().dimension());}
    public void abort(RuntimeException error){
        if(!closed)player.displayClientMessage(Component.translatable(error instanceof Stop?error.getMessage():"gui.modularmagic.cast_limit"),true);
        if(!(error instanceof Stop))ModularMagic.LOGGER.warn("Spell execution stopped",error);close(true);
    }
    public void close(boolean cancelEntities){
        if(closed)return;closed=true;if(cancelEntities)for(var e:List.copyOf(entities))e.discard();
        escrow.release(id,startedEffects&&(!missOnly||combat&&!targetAttempted)?refundRatio:1);
        for(var source:player.getInventory().items)escrow.claim(source);for(var source:player.getInventory().offhand)escrow.claim(source);
        player.inventoryMenu.broadcastChanges();
    }
}
