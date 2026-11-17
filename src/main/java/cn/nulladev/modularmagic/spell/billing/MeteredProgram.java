package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2magic.content.engine.context.*;
import dev.xkmc.l2magic.content.engine.core.*;
import dev.xkmc.l2magic.content.engine.logic.ProcessorEngine;
import dev.xkmc.l2magic.content.engine.selector.*;
import dev.xkmc.l2magic.content.engine.variable.*;
import dev.xkmc.l2magic.content.entity.core.*;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;

/** Per-cast server-only decorators. Native JSON, registries and client programs remain unmodified. */
public final class MeteredProgram {
    private final CastAccount account;private final String source;
    private final MappedRegistry<ProjectileConfig> projectiles=new MappedRegistry<>(EngineRegistry.PROJECTILE,com.mojang.serialization.Lifecycle.stable());
    public MeteredProgram(CastAccount account,String source){this.account=account;this.source=source;}
    public ConfiguredEngine<?> action(SpellDocument document,ConfiguredEngine<?> original){return (ConfiguredEngine<?>)transform(original,document.root.children.get("action").getFirst(),false);}
    public IPredicate condition(SpellDocument document,IPredicate original){return (IPredicate)transform(original,document.root.children.get("condition").getFirst(),false);}
    private Object transform(Object original,SpellDocument.Node node,boolean queryPosition){
        if(original instanceof Holder<?> holder&&holder.value() instanceof ProjectileConfig config){
            var id=holder.unwrapKey().orElseThrow().location();
            if(projectiles.containsKey(id))return projectiles.getHolderOrThrow(ResourceKey.create(EngineRegistry.PROJECTILE,id));
            var changed=(ProjectileConfig)transform(config,node,false);
            var ref=projectiles.register(ResourceKey.create(EngineRegistry.PROJECTILE,id),changed,RegistrationInfo.BUILT_IN);
            account.projectiles.put(id,changed);RuntimeSpells.registerSource(ref,source);return ref;
        }
        if(!original.getClass().isRecord())return original;
        var mapped=new HashSet<String>();
        Object rebuilt=mapRecord(original,(name,value)->{
            var slot=node.schema().slots().stream().filter(s->s.key().equals(name)||(s.key().equals("selectors")&&name.equals("list"))).findFirst().orElse(null);
            if(slot==null||value==null)return value;
            mapped.add(slot.key());
            var nodes=node.children.get(slot.key());
            boolean position=node.template.equals("predicate/depth_test")||node.template.equals("predicate/ranged_test");
            if(value instanceof List<?> list){var changed=new ArrayList<>();for(int i=0;i<list.size();i++)changed.add(transform(list.get(i),nodes.get(i),position));return changed;}
            if(nodes.isEmpty())return value;return transform(value,nodes.getFirst(),position);
        });
        for(var slot:node.schema().slots())if(!node.children.get(slot.key()).isEmpty()&&!mapped.contains(slot.key()))throw new IllegalArgumentException("Unmapped child slot: "+node.template+"."+slot.key());
        if(rebuilt instanceof ConfiguredEngine<?> engine)return new Action(engine,node,account,pureVisual(node),freePassives(node));
        if(rebuilt instanceof EntityProcessor<?> processor)return new Processor(processor,node,account);
        if(rebuilt instanceof EntitySelector<?> selector)return new Selector(selector,node,account);
        if(rebuilt instanceof IPredicate predicate)return new Predicate(predicate,node,account,queryPosition);
        if(rebuilt instanceof dev.xkmc.l2magic.content.engine.core.Modifier<?> modifier)return new Modification(modifier,node,account);
        if(rebuilt instanceof EntityFilter<?> filter)return new Filter(filter,node,account);
        if(rebuilt instanceof Motion<?> motion)return new Movement(motion,node,account);
        return rebuilt;
    }
    @FunctionalInterface private interface FieldMap{Object map(String name,Object value);}
    private static Object mapRecord(Object original,FieldMap mapper){
        try{var fields=original.getClass().getRecordComponents();var args=new Object[fields.length];var types=new Class<?>[fields.length];
            for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();args[i]=mapper.map(fields[i].getName(),fields[i].getAccessor().invoke(original));}
            return original.getClass().getDeclaredConstructor(types).newInstance(args);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot adapt "+original.getClass().getSimpleName(),e);}
    }
    public static Object unwrap(Object value){
        if(value instanceof Action a)return unwrap(a.delegate);if(value instanceof Processor p)return unwrap(p.delegate);if(value instanceof Selector s)return unwrap(s.delegate);
        if(value instanceof Predicate p)return unwrap(p.delegate);if(value instanceof Movement m)return unwrap(m.delegate);
        if(value instanceof Modification m)return unwrap(m.delegate);if(value instanceof Filter f)return unwrap(f.delegate);
        if(value instanceof List<?> list)return list.stream().map(MeteredProgram::unwrap).toList();
        if(value!=null&&value.getClass().isRecord()&&value.getClass().getPackageName().startsWith("dev.xkmc.l2magic"))return mapRecord(value,(name,child)->unwrap(child));
        return value;
    }
    private static void gate(CastAccount account,EngineContext ctx){account.check(ctx);}
    private static void execute(CastAccount account,Runnable task){
        boolean nested=BillingRuntime.ACTIVE.get()==account;
        try{BillingRuntime.run(account,task);}catch(RuntimeException e){account.abort(e);if(nested)throw e;}
    }
    private static boolean visual(SpellDocument.Node node){return node.template.contains("particle")||node.template.equals("action/sound");}
    private static boolean pureVisual(SpellDocument.Node node){
        String category=node.schema().category(),id=node.template;
        if(category.equals("effect")||category.equals("selector")||category.equals("projectile"))return false;
        if(category.equals("action")&&!visual(node)&&!Set.of("action/list","action/move","action/delay","action/variable","action/random","action/if","action/synced_logic","action/iterate","action/iterate_linear","action/iterate_arc","action/iterate_delayed","action/while_delayed","action/block_in_range","action/random_pos_fan","action/random_pos_sphere").contains(id))return false;
        return node.children.values().stream().flatMap(Collection::stream).allMatch(MeteredProgram::pureVisual);
    }
    private static Map<String,Double> prices(Parameters.Frozen frozen){return new HashMap<>(frozen.prices());}
    private boolean freePassives(SpellDocument.Node node){
        for(var slot:node.schema().slots())if(slot.category().endsWith("renderer")||slot.category().equals("projectile"))for(var child:node.children.get(slot.key())){
            Double price=account.constantPrice(child.template);if(price==null||price!=0||!freePassives(child))return false;
        }
        return true;
    }
    private static void passive(Object record,SpellDocument.Node node,EngineContext ctx,CastAccount account){
        mapRecord(record,(name,value)->{
            var slot=node.schema().slots().stream().filter(s->s.key().equals(name)).findFirst().orElse(null);
            if(slot==null||value==null||!(slot.category().endsWith("renderer")||slot.category().equals("projectile")))return value;
            var nodes=node.children.get(slot.key());var objects=value instanceof List<?> list?list:List.of(value);
            for(int i=0;i<nodes.size();i++){
                Object child=objects.get(i);if(child instanceof Holder<?> holder)child=holder.value();
                var childNode=nodes.get(i);
                if(!account.freeRule(childNode.template))account.charge(account.price(childNode.template,Parameters.freeze(child,childNode,ctx).prices()));
                passive(child,childNode,ctx,account);
            }
            return value;
        });
    }
    private record Action(ConfiguredEngine<?> delegate,SpellDocument.Node node,CastAccount account,boolean pureVisual,boolean freePassives) implements ConfiguredEngine<Action> {
        @Override @SuppressWarnings({"unchecked","rawtypes"})public EngineType<Action> type(){return (EngineType)delegate.type();}
        @Override public void execute(EngineContext ctx){MeteredProgram.execute(account,()->{
            Double constant=account.constantPrice(node.template);
            if(pureVisual&&freePassives&&constant!=null){
                gate(account,ctx);account.charge(constant);boolean before=BillingRuntime.VISUAL_BATCH.get();var actual=ctx;
                if(!before){long seed=ctx.rand().nextLong();actual=new EngineContext(ctx.user(),ctx.loc(),new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(seed),ctx.parameters());
                    cn.nulladev.modularmagic.network.NodeVisual.send((ConfiguredEngine<?>)unwrap(delegate),actual,seed);}
                BillingRuntime.VISUAL_BATCH.set(true);try{delegate.execute(actual);}finally{BillingRuntime.VISUAL_BATCH.set(before);}return;
            }
            gate(account,ctx);var frozen=Parameters.freeze(delegate,node,ctx);var values=prices(frozen);
            account.charge(account.price(node.template,values));
            passive(frozen.value(),node,ctx,account);
            boolean beforeBatch=BillingRuntime.VISUAL_BATCH.get();
            if(pureVisual&&!beforeBatch)cn.nulladev.modularmagic.network.NodeVisual.send((ConfiguredEngine<?>)unwrap(frozen.value()),ctx);
            BillingRuntime.VISUAL_BATCH.set(beforeBatch||pureVisual);
            var previous=BillingRuntime.NODE.get();BillingRuntime.NODE.set(node);
            var payload=BillingRuntime.PAYLOAD.get();BillingRuntime.PAYLOAD.set(NativePayload.actualEffects(frozen.value(),account));
            try{
                if(frozen.value() instanceof ProcessorEngine engine){
                    var found=engine.selector().find(ctx.user().level(),ctx,engine.target());
                    var chosen=found.entries().stream().filter(e->!e.root().isRemoved()&&e.root().isAlive()).sorted(Comparator
                            .comparingDouble((SelectedEntities.Entry e)->e.root().position().distanceToSqr(ctx.loc().pos()))
                            .thenComparing(e->e.root().getUUID())).limit(account.targets).map(SelectedEntities.Entry::e).toList();
                    var targets=new SelectedEntities(chosen);for(var effect:engine.processors())effect.process(targets,ctx);
                }else ((ConfiguredEngine<?>)frozen.value()).execute(ctx);
            }finally{BillingRuntime.VISUAL_BATCH.set(beforeBatch);if(previous==null)BillingRuntime.NODE.remove();else BillingRuntime.NODE.set(previous);if(payload==null)BillingRuntime.PAYLOAD.remove();else BillingRuntime.PAYLOAD.set(payload);}
        });}
    }
    private record Processor(EntityProcessor<?> delegate,SpellDocument.Node node,CastAccount account) implements EntityProcessor<Processor> {
        @Override @SuppressWarnings({"unchecked","rawtypes"})public ProcessorType<Processor> type(){return (ProcessorType)delegate.type();}
        @Override public boolean serverOnly(){return delegate.serverOnly();}
        @Override public void process(SelectedEntities set,EngineContext ctx){MeteredProgram.execute(account,()->{
            gate(account,ctx);var valid=new SelectedEntities(set.entries().stream().filter(e->!e.root().isRemoved()&&e.root().isAlive()).map(SelectedEntities.Entry::e).toList());
            if(valid.entries().isEmpty())return;
            var frozen=Parameters.freeze(delegate,node,ctx);var values=prices(frozen);double total=0;
            for(var entry:valid.entries()){
                if(node.template.equals("effect/teleport")){
                    var dest=new Vec3(values.getOrDefault("x",0D),values.getOrDefault("y",0D),values.getOrDefault("z",0D));
                    double distance=entry.root().position().distanceTo(dest);if(distance>account.range)throw new CastAccount.Stop("gui.modularmagic.cast_limit");values.put("distance",distance);
                }
                total+=account.price(node.template,values);
            }
            account.charge(total);
            if(!Set.of("effect/delay","effect/cast_at","effect/filtered").contains(node.template))account.targetAttempt();
            ((EntityProcessor<?>)frozen.value()).process(valid,ctx);
        });}
    }
    private record Selector(EntitySelector<?> delegate,SpellDocument.Node node,CastAccount account) implements EntitySelector<Selector> {
        @Override @SuppressWarnings({"unchecked","rawtypes"})public SelectorType<Selector> type(){return (SelectorType)delegate.type();}
        @Override public SelectedEntities find(Level level,EngineContext ctx,SelectionType type){
            gate(account,ctx);var frozen=Parameters.freeze(delegate,node,ctx);account.charge(account.price(node.template,frozen.prices()));return ((EntitySelector<?>)frozen.value()).find(level,ctx,type);
        }
    }
    private record Predicate(IPredicate delegate,SpellDocument.Node node,CastAccount account,boolean position) implements ContextPredicate<Predicate> {
        @Override @SuppressWarnings({"unchecked","rawtypes"})public PredicateType<Predicate> type(){return delegate instanceof ContextPredicate<?> predicate?(PredicateType)predicate.type():null;}
        @Override public boolean test(EngineContext ctx){
            Double constant=account.constantPrice(node.template);
            if(BillingRuntime.VISUAL_BATCH.get()&&constant!=null){gate(account,ctx);account.charge(constant+(position?ResourceBalance.positionQuery():0));return delegate.test(ctx);}
            gate(account,ctx);var frozen=Parameters.freeze(delegate,node,ctx);account.charge(account.price(node.template,frozen.prices())+(position?ResourceBalance.positionQuery():0));return ((IPredicate)frozen.value()).test(ctx);
        }
    }
    private record Movement(Motion<?> delegate,SpellDocument.Node node,CastAccount account) implements Motion<Movement> {
        @Override @SuppressWarnings({"unchecked","rawtypes"})public MotionType<Movement> type(){return (MotionType)delegate.type();}
        @Override public dev.xkmc.fastprojectileapi.entity.ProjectileMovement move(EngineContext ctx,Vec3 velocity,Vec3 pos){
            try{gate(account,ctx);var frozen=Parameters.freeze(delegate,node,ctx);account.charge(account.price(node.template,frozen.prices()));return ((Motion<?>)frozen.value()).move(ctx,velocity,pos);}
            catch(RuntimeException e){account.abort(e);return dev.xkmc.fastprojectileapi.entity.ProjectileMovement.of(Vec3.ZERO);}
        }
    }
    private record Modification(dev.xkmc.l2magic.content.engine.core.Modifier<?> delegate,SpellDocument.Node node,CastAccount account) implements dev.xkmc.l2magic.content.engine.core.Modifier<Modification> {
        @Override @SuppressWarnings({"unchecked","rawtypes"}) public ModifierType<Modification> type(){return (ModifierType)delegate.type();}
        @Override public LocationContext modify(EngineContext ctx){gate(account,ctx);Double constant=account.constantPrice(node.template);
            if(BillingRuntime.VISUAL_BATCH.get()&&constant!=null){account.charge(constant);return delegate.modify(ctx);}
            var frozen=Parameters.freeze(delegate,node,ctx);account.charge(account.price(node.template,frozen.prices()));return ((dev.xkmc.l2magic.content.engine.core.Modifier<?>)frozen.value()).modify(ctx);}
    }
    private record Filter(EntityFilter<?> delegate,SpellDocument.Node node,CastAccount account) implements EntityFilter<Filter> {
        @Override @SuppressWarnings({"unchecked","rawtypes"}) public FilterType<Filter> type(){return (FilterType)delegate.type();}
        @Override public boolean test(net.minecraft.world.entity.LivingEntity entity,EngineContext ctx){gate(account,ctx);var frozen=Parameters.freeze(delegate,node,ctx);account.charge(account.price(node.template,frozen.prices()));return ((EntityFilter<?>)frozen.value()).test(entity,ctx);}
    }
}
