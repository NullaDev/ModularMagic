package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.spell.*;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.*;

/** One server TOML rule per template. Expressions are pure and use normalized parameters. */
public final class CostRules {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue TARGETS,TICKS,STEPS,PARALLEL,ENTITIES,VISUALS;
    public static final ModConfigSpec.DoubleValue RADIUS,SPEED,DAMAGE,DURATION;
    private static final Map<String,ModConfigSpec.ConfigValue<String>> MANA=new HashMap<>(),FOCUS=new HashMap<>();
    static {
        var b=new ModConfigSpec.Builder();b.push("limits");
        TARGETS=b.defineInRange("maxTargetsPerSelection",8,1,64);TICKS=b.defineInRange("maxLifetimeTicks",1200,20,72000);
        STEPS=b.defineInRange("maxWorkPerCast",8000000,64,16000000);PARALLEL=b.defineInRange("maxActiveCasts",32,1,256);
        ENTITIES=b.defineInRange("maxProjectilesPerCast",256,1,4096);VISUALS=b.defineInRange("maxVisualEmissionsPerCast",2000000,1,4000000);
        RADIUS=b.defineInRange("maxRange",32.0,1,128);SPEED=b.defineInRange("maxSpeed",16.0,0.1,128);
        DAMAGE=b.defineInRange("maxDamage",1000.0,1,1000000);DURATION=b.defineInRange("maxEffectTicks",72000.0,1,10000000);b.pop();
        b.comment("Pure arithmetic expressions; no random functions. Use + - * / ^, min/max, abs, sqrt, log, floor/ceil, sin/cos.",
                "Mana formulas use normalized base units. Multiply their results by 1000 to obtain displayed mana.",
                "Mana variables: numeric node fields, plus damage, velocity, distance, volume, duration, amplifier, burnTicks, speed, n, ticks, q.",
                "Status duration is normalized to ticks*0.02 (instant effects use 1). Damage/effect factors default to 1.",
                "Focus variables: children (sum), C (loop body), P (condition), A/B (branches), n, ticks, q.",
                "Root focus formulas use normalized units and are multiplied by 2.5 once. Other focus formulas describe structural complexity.",
                "Prices must remain nonnegative and finite across the full parameter range; otherwise the spell is rejected.")
                .push("nodes");for(var node:NodeCatalog.all()) {
            b.push(node.id().replace('/','_'));
            MANA.put(node.id(),b.comment("Own mana per event/per target. Child effects are charged separately.")
                    .define("mana",defaultMana(node)));
            FOCUS.put(node.id(),b.comment("Whole subtree structural focus; children, C, P, A, B, n and ticks are available.")
                    .define("focus",defaultFocus(node)));
            b.pop();
        }b.pop();SPEC=b.build();
    }
    public static double mana(String id,Map<String,Double> values){var vars=new HashMap<String,Range>();values.forEach((k,v)->vars.put(k,Range.at(v)));return cost(manaBound(id,vars));}
    public static Map<String,String> snapshot(){var copy=new HashMap<String,String>();MANA.forEach((id,rule)->{
        String formula=rule.get().trim();copy.put(id,formula.matches("[+-]?0(?:\\.0+)?")?"0":"("+formula+")*"+ResourceBalance.MANA_SCALE);
    });return Map.copyOf(copy);}
    public static double evaluate(String formula,Map<String,Double> values){var vars=new HashMap<String,Range>();values.forEach((k,v)->vars.put(k,Range.at(v)));return cost(rule(formula,vars));}
    public static Range manaBound(String id,Map<String,Range> values){var price=MANA.get(id);if(price==null)throw new IllegalArgumentException("Unpriced node "+id);var range=rule(price.get(),values);return new Range(range.lo()*ResourceBalance.MANA_SCALE,range.hi()*ResourceBalance.MANA_SCALE);}
    public static double focus(String id,Map<String,Double> values){var vars=new HashMap<String,Range>();values.forEach((k,v)->vars.put(k,Range.at(v)));return cost(rule(FOCUS.get(id).get(),vars))*(id.startsWith("root/")?ResourceBalance.FOCUS_SCALE:1);}
    private static Range rule(String formula,Map<String,Range> values){if(formula.matches("(?s).*\\brand\\s*\\(.*"))throw new IllegalArgumentException("Resource rules must be deterministic");return Range.eval(formula,values);}
    private static double cost(Range r){if(r.lo()<0||r.hi()>1e12)throw new IllegalArgumentException("Invalid resource price");return r.hi();}
    public static Map<String,Double> defaults(){var v=new HashMap<String,Double>();for(String key:List.of("damage","speed","distance","duration","amplifier","burnTicks","volume","n","ticks","children","C","P","A","B","q","maxDamage","velocity","typeFactor","effectFactor"))v.put(key,0D);v.put("typeFactor",1D);v.put("effectFactor",1D);return v;}
    private static String defaultMana(NodeTemplate t){String id=t.id();if(t.category().equals("selector")&&!id.equals("selector/self")&&!id.equals("selector/move")&&!id.equals("selector/compound"))return "0.05+0.002*volume";
        return switch(id){
            case "root/instant","root/charge","root/continuous"->"0.5";
            case "action/particle","action/block_particle","action/item_particle","action/dust_particle","action/transition_particle","action/custom_particle"->"0.0002";
            case "action/sound"->"0.001";
            case "effect/damage"->"typeFactor*(0.5*damage+0.0125*damage^2)";
            case "effect/push","effect/knockback","effect/set_delta"->"0.5*velocity+0.5*velocity^2";
            case "effect/teleport"->"2+0.15*distance+0.002*distance^2";
            case "effect/effect"->"effectFactor*duration*(amplifier+1)^2";
            case "effect/property"->"0.02*duration";
            case "effect/ignite"->"0.02*burnTicks";
            case "effect/projectile_hit"->"0.5125";
            case "effect/procreation"->"0.25";
            case "effect/heal_interval"->"0.05+0.5*max(0,heal)";
            case "effect/stacking_status"->"0.02*max(0,duration)";
            case "action/set_block","action/set_block_facing"->"0.2";
            case "action/temporary_block","action/melt_block"->"0.2";
            case "action/potion_cloud"->"0.5+0.1*radius^2";
            case "action/thunder"->"0.5+0.5*max(0,damage)";
            case "action/remove_liquid"->"0.1+0.02*max(0,limit)";
            case "action/remove_block"->"0.5";case "action/knock_block"->"2+speed^2+0.5*maxDamage+0.0125*maxDamage^2";
            case "action/schedule_tick"->"0.1";
            case "action/arrow","action/trident","action/custom_projectile"->"0.5+0.1*speed^2";
            case "motion/simple","motion/control_position","motion/control_velocity"->"0.001";
            default->"0";
        };}
    private static String defaultFocus(NodeTemplate t){String id=t.id(),c=t.category();if(c.equals("root"))return ResourceBalance.ROOT_FOCUS;if(c.equals("trigger")||c.endsWith("renderer"))return "0";if(c.equals("modifier"))return "0.05";
        if(id.contains("particle")||id.equals("action/sound"))return "0";
        if(id.equals("action/list")||id.equals("action/move")||id.equals("selector/move")||c.equals("projectile"))return "children";
        if(id.equals("action/if")||id.equals("effect/filtered"))return "0.1+P+max(A,B)+0.25*min(A,B)";
        if(id.equals("action/iterate_delayed"))return "0.35+log(1+n)/log(2)*C+0.05*log(1+ticks/20)/log(2)";
        if(id.equals("action/while_delayed"))return "0.45+P+log(1+n)/log(2)*C+0.05*log(1+ticks/20)/log(2)";
        if(id.equals("predicate/depth_test")||id.equals("predicate/ranged_test"))return "0.1+log(1+n)/log(2)*C";
        if(id.startsWith("action/iterate")||id.startsWith("action/random_pos")||id.equals("action/block_in_range"))return "0.2+log(1+n)/log(2)*C";
        if(id.equals("action/delay")||id.equals("effect/delay")||id.equals("action/schedule_tick"))return "0.15+children+0.05*log(1+ticks/20)/log(2)";
        if(id.equals("action/random"))return "0.1+0.02*q+children";if(id.equals("action/synced_logic")||c.equals("motion"))return "0.05+children";
        return "0.1+children";
    }
    private CostRules(){}
}
