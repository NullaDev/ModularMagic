package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2magic.content.engine.context.SpellContext;
import java.util.*;

/** Finite conservative contract, with explicit spatial/lifetime/work limits. */
public final class SpellBudget {
    public record Plan(double mana,double focus,double startup){}
    private record Part(double mana,double focus,long work,boolean visual,boolean discountable,long spawns,long emissions) {
        Part(double mana,double focus,long work,boolean visual,boolean discountable){this(mana,focus,work,visual,discountable,0,0);}
        double charged(){return visual&&discountable?focus*.02:focus;}
    }
    public static Plan plan(SpellDocument document,SpellContext context){
        var vars=new HashMap<String,Range>();context.defaultArgs().forEach((k,v)->vars.put(k,Range.at(v)));
        vars.put("Time",new Range(0,CostRules.TICKS.get()));vars.put("TickCount",new Range(0,CostRules.TICKS.get()));
        vars.put("Age",new Range(0,CostRules.TICKS.get()));
        double r=CostRules.RADIUS.get()*4;
        for(String prefix:List.of("Pos","Projectile","Caster"))for(int a=0;a<3;a++){
            double center=a==0?context.origin().x:a==1?context.origin().y:context.origin().z;
            vars.put(prefix+"XYZ".charAt(a),new Range(center-r,center+r));
        }
        var p=walk(document.root,vars,0);if(p.work>CostRules.STEPS.get())throw new IllegalArgumentException("Spell exceeds work limit");
        double root=CostRules.mana(document.root.template,CostRules.defaults());
        return new Plan(p.mana,p.charged(),p.visual?Math.min(root,ResourceBalance.cosmeticStartup()):root);
    }
    private static Part walk(SpellDocument.Node node,Map<String,Range> vars,int depth){
        if(depth>SpellDocument.MAX_DEPTH)throw new IllegalArgumentException("Spell recursion limit");
        String id=node.template;var local=new HashMap<>(vars);var p=CostRules.defaults();
        var bound=new HashMap<String,Range>();
        for(var f:node.schema().fields())if(f.kind().equals("expression")||f.kind().equals("integer")){
            String text=node.parameters.getOrDefault(f.key(),"");if(text.isBlank())continue;
            Range value=Range.eval(text,vars);bound.put(f.key(),value);p.put(f.key(),value.magnitude());
            check(f.key(),value);
        }
        double n=1,ticks=0;boolean loop=id.startsWith("action/iterate")||id.startsWith("action/random_pos")||id.equals("action/while_delayed");
        if(loop){n=(long)Math.max(0,bound.getOrDefault(node.parameters.containsKey("count")?"count":"step",Range.at(0)).hi());if(id.equals("action/while_delayed")&&n<1)throw new IllegalArgumentException("While loop needs positive bound");
            String index=node.parameters.getOrDefault("index","");
            if(!index.isBlank()){
                local.put(index,new Range(0,Math.max(0,n-1)));
                if(id.equals("action/random_pos_fan")){
                    local.put(index+"_radius",union(bound.getOrDefault("minRadius",Range.at(0)),bound.get("maxRadius")));
                    local.put(index+"_angle",union(bound.getOrDefault("minAngle",Range.at(-180)),bound.getOrDefault("maxAngle",Range.at(180))));
                }
            }
            ticks=Math.max(0,n-1)*p.getOrDefault("delay",0D);
        }
        if(id.equals("action/variable"))local.put(node.parameters.get("name"),bound.getOrDefault("variable",Range.at(0)));
        if(id.equals("action/random"))for(int i=0;i<p.getOrDefault("count",0D);i++)local.put(node.parameters.get("name")+i,new Range(0,1));
        if(id.equals("action/block_in_range")||id.equals("predicate/ranged_test")){
            double r=p.getOrDefault("range",0D),h=p.getOrDefault("height",r);n=Math.pow(2*Math.ceil(r)+1,2)*(2*Math.ceil(h)+1);
            String v=node.parameters.getOrDefault("variable","");for(String axis:List.of("_x","_y","_z"))local.put(v+axis,new Range(-r,r));local.put(v+"_r",new Range(0,r));
            ticks=r*p.getOrDefault("delayPerBlock",0D);
        }
        if(id.equals("predicate/depth_test")){n=p.getOrDefault("max_depth",0D);String index=node.parameters.getOrDefault("index","");if(!index.isBlank())local.put(index,new Range(0,Math.max(0,n-1)));}
        if(id.equals("action/custom_projectile")){
            var json=com.google.gson.JsonParser.parseString(node.parameters.getOrDefault("params", "{}"));
            if(json.isJsonObject())json.getAsJsonObject().entrySet().forEach(e->local.put(e.getKey(),Range.eval(e.getValue().getAsString(),vars)));
            local.put("__life",Range.at(p.getOrDefault("life",0D)));
        }
        if(id.equals("action/delay"))ticks=p.getOrDefault("tick",0D);
        if(id.equals("effect/delay"))ticks=p.getOrDefault("time",0D);
        if(ticks>CostRules.TICKS.get())throw new IllegalArgumentException("Spell delay exceeds lifetime");
        var children=new HashMap<String,Part>();boolean visual=visual(node);long work=1;
        for(var slot:node.schema().slots()){
            Part sum=new Part(0,0,0,true,false);
            for(var child:node.children.get(slot.key()))sum=add(sum,walk(child,local,depth+1));
            children.put(slot.key(),sum);visual&=sum.visual;work=addWork(work,sum.work);
        }
        boolean root=node.schema().category().equals("root");
        double childMana=0,childFocus=0;for(var child:children.values()){childMana+=child.mana;childFocus+=visual&&!root?child.focus:child.charged();}
        long spawns=0,emissions=0;for(var child:children.values()){spawns=addWork(spawns,child.spawns);emissions=addWork(emissions,child.emissions);}
        if(Set.of("action/arrow","action/trident","action/custom_projectile","action/knock_block","action/potion_cloud","action/thunder").contains(id))spawns++;
        if(id.contains("particle")||id.equals("action/sound"))emissions++;
        Parameters.normalize(node,p);p.put("distance",CostRules.RADIUS.get()*4);p.put("n",n);p.put("ticks",ticks);p.put("q",p.getOrDefault("count",0D));
        p.put("children",childFocus);p.put("C",children.getOrDefault("child",new Part(0,0,0,true,false)).focus);
        p.put("P",children.getOrDefault(id.equals("effect/filtered")?"filter":id.equals("action/while_delayed")?"condition":"predicate",new Part(0,0,0,true,false)).focus);
        p.put("A",children.getOrDefault("action",new Part(0,0,0,true,false)).focus);p.put("B",children.getOrDefault("fallback",new Part(0,0,0,true,false)).focus);
        var prices=new HashMap<String,Range>();p.forEach((k,v)->prices.put(k,Range.at(v)));
        // Preserve true intervals for custom non-monotonic mana expressions.
        prices.putAll(bound);
        if(id.equals("effect/effect")){
            var key=net.minecraft.resources.ResourceLocation.tryParse(node.parameters.getOrDefault("effect",""));
            var effect=key==null?null:net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(key);
            prices.put("duration",effect!=null&&effect.isInstantenous()?Range.at(1):Range.eval("0.02*duration",prices));
        }
        if(id.equals("effect/knockback"))prices.put("velocity",Range.eval("abs(knockback)",prices));
        if(id.equals("effect/push"))prices.put("velocity",Range.eval("abs(speed)",prices));
        if(id.equals("effect/set_delta"))prices.put("velocity",Range.eval("sqrt(x^2+y^2+z^2)",prices));
        if(node.schema().category().equals("selector"))prices.put("volume",new Range(0,p.get("volume")));
        prices.put("distance",new Range(0,CostRules.RADIUS.get()*4));
        var interval=CostRules.manaBound(id,prices);double own=interval.hi();if(interval.lo()<0)throw new IllegalArgumentException("Negative mana");
        double mana=own+childMana;
        if(loop||id.equals("action/block_in_range")||id.equals("predicate/depth_test")||id.equals("predicate/ranged_test")){
            Part body=children.getOrDefault("child",new Part(0,0,0,true,false));mana=own+n*body.mana;
            if(id.equals("action/while_delayed")){
                Part condition=children.get("condition");mana+=n*condition.mana;
                work=addWork(work,(long)(n*condition.work));
            }
            work=addWork(work,(long)Math.min(Long.MAX_VALUE/4,n*body.work));
            spawns=addWork(spawns,(long)(Math.max(0,n-1)*body.spawns));emissions=addWork(emissions,(long)(Math.max(0,n-1)*body.emissions));
            if(id.startsWith("predicate/"))mana+=n*ResourceBalance.positionQuery();
        }
        if(id.equals("action/if")||id.equals("effect/filtered")){
            double a=children.getOrDefault("action",new Part(0,0,0,true,false)).mana,b=children.getOrDefault("fallback",new Part(0,0,0,true,false)).mana;
            mana=own+childMana-a-b+Math.max(a,b);
        }
        if(id.equals("action/processor")){
            int targets=node.children.get("selector").getFirst().template.equals("selector/self")?1:CostRules.TARGETS.get();
            var effects=children.get("processors");mana+=Math.max(0,targets-1)*effects.mana;work=addWork(work,(targets-1)*effects.work);
            spawns=addWork(spawns,(targets-1)*effects.spawns);emissions=addWork(emissions,(targets-1)*effects.emissions);
        }
        if(id.equals("projectile/config")){
            double life=vars.getOrDefault("__life",Range.at(0)).hi();
            for(String key:List.of("tick","motion")){var child=children.get(key);if(child!=null){mana+=Math.max(0,life-1)*child.mana;work=addWork(work,(long)(life*child.work));spawns=addWork(spawns,(long)(Math.max(0,life-1)*child.spawns));emissions=addWork(emissions,(long)(Math.max(0,life-1)*child.emissions));}}
            if(children.get("hit")!=null){var hit=children.get("hit");mana+=(CostRules.TARGETS.get()-1)*hit.mana;work=addWork(work,(CostRules.TARGETS.get()-1)*hit.work);spawns=addWork(spawns,(CostRules.TARGETS.get()-1)*hit.spawns);emissions=addWork(emissions,(CostRules.TARGETS.get()-1)*hit.emissions);}
        }
        if(id.equals("action/arrow")||id.equals("action/trident"))mana+=CostRules.TARGETS.get()*NativePayload.budget(node,p,vars);
        if(root&&visual)mana=mana-own+Math.min(own,ResourceBalance.cosmeticStartup());
        double focus=CostRules.focus(id,p);
        if(id.equals("selector/line")||id.equals("selector/arc"))work=addWork(work,(long)p.getOrDefault("step",0D).doubleValue()+1);
        if(work>CostRules.STEPS.get()||spawns>CostRules.ENTITIES.get()||emissions>CostRules.VISUALS.get()||!Double.isFinite(mana)||mana>1e12)throw new IllegalArgumentException("Spell budget exceeds limit at "+id+": work="+work+", spawns="+spawns+", emissions="+emissions+", mana="+mana);
        return new Part(mana,focus,work,visual,node.schema().category().equals("action"),spawns,emissions);
    }
    private static Part add(Part a,Part b){boolean pure=a.visual&&b.visual;return new Part(a.mana+b.mana,pure?a.focus+b.focus:a.charged()+b.charged(),addWork(a.work,b.work),pure,pure&&(a.discountable||b.discountable),addWork(a.spawns,b.spawns),addWork(a.emissions,b.emissions));}
    private static long addWork(long a,long b){return Math.min(Long.MAX_VALUE/2,a+b);}
    private static Range union(Range a,Range b){return new Range(Math.min(a.lo(),b.lo()),Math.max(a.hi(),b.hi()));}
    private static boolean visual(SpellDocument.Node n){String id=n.template,c=n.schema().category();if(c.equals("effect"))return false;
        return !Set.of("action/set_block","action/set_block_facing","action/remove_block","action/knock_block","action/schedule_tick","action/arrow","action/trident","action/custom_projectile",
                "action/temporary_block","action/melt_block","action/potion_cloud","action/thunder","action/remove_liquid").contains(id);}
    private static void check(String key,Range r){double max=switch(key){case "damage","maxDamage","damagePerBlock"->CostRules.DAMAGE.get();case "speed","knockback"->CostRules.SPEED.get();case "r","radius","range","height","size","minRadius","maxRadius"->CostRules.RADIUS.get();case "life","tick","time","delay","delayPerBlock"->(double)CostRules.TICKS.get();case "duration","burnTicks"->CostRules.DURATION.get();case "amplifier"->32D;case "step","count","max_depth"->(double)CostRules.STEPS.get();default->3e7;};if(r.magnitude()>max)throw new IllegalArgumentException("Spell parameter exceeds bound: "+key);}
}
