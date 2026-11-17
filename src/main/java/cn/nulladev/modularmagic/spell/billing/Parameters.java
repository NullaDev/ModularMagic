package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.spell.*;
import dev.xkmc.l2magic.content.engine.context.EngineContext;
import dev.xkmc.l2magic.content.engine.variable.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.lang.reflect.*;
import java.util.*;

/** Freeze only this record's numeric fields. Never evaluate child scopes or random payloads twice. */
public final class Parameters {
    public record Frozen(Object value,Map<String,Double> prices){}
    public static Frozen freeze(Object record,SpellDocument.Node node,EngineContext ctx){
        if(record instanceof BooleanVariable variable){
            double number=variable.exp().eval(ctx);check(number);var prices=CostRules.defaults();prices.put("value",number);
            String text=Double.toString(number);return new Frozen(new BooleanVariable(text,ExpressionHolder.ofVerified(text)),prices);
        }
        try {
            var components=record.getClass().getRecordComponents();var args=new Object[components.length];var types=new Class<?>[components.length];
            var values=CostRules.defaults();var evaluated=new HashMap<String,Double>();var named=new HashSet<String>();
            for(int i=0;i<components.length;i++){
                var c=components[i];types[i]=c.getType();Object value=c.getAccessor().invoke(record);
                if(value instanceof DoubleVariable v){double n=v.eval(ctx);check(n);evaluated.put(v.str(),n);values.put(c.getName(),n);named.add(c.getName());String text=Double.toString(n);value=new DoubleVariable(text,ExpressionHolder.ofVerified(text));}
                else if(value instanceof IntVariable v){double n=v.eval(ctx);evaluated.put(v.str(),n);values.put(c.getName(),n);named.add(c.getName());value=constantInt((int)n);}
                else if(value instanceof List<?> list&&!list.isEmpty()&&list.getFirst() instanceof dev.xkmc.l2magic.content.engine.helper.EffectInstanceEntry){
                    var result=new ArrayList<>();for(var entry:list){var effect=(dev.xkmc.l2magic.content.engine.helper.EffectInstanceEntry)entry;
                        result.add(new dev.xkmc.l2magic.content.engine.helper.EffectInstanceEntry(effect.effect(),constantInt(effect.duration().eval(ctx)),constantInt(effect.amplifier().eval(ctx))));}
                    value=result;
                }
                args[i]=value;
            }
            for(var f:node.schema().fields())if(f.kind().equals("expression")||f.kind().equals("integer")){
                String text=node.parameters.getOrDefault(f.key(),"");if(text.isBlank())continue;
                Double n=evaluated.get(text);if(n!=null&&!named.contains(f.key()))values.put(f.key(),n);
            }
            if(node.template.equals("selector/box"))values.put("size",values.get("r"));
            normalize(node,values);
            return new Frozen(record.getClass().getDeclaredConstructor(types).newInstance(args),values);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot meter "+record.getClass().getSimpleName(),e);}
    }
    public static void normalize(SpellDocument.Node node,Map<String,Double> v){
        String id=node.template;
        if(id.equals("effect/knockback"))v.put("velocity",Math.abs(v.getOrDefault("knockback",0D)));
        if(id.equals("effect/push"))v.put("velocity",Math.abs(v.getOrDefault("speed",0D)));
        if(id.equals("effect/set_delta"))v.put("velocity",Math.sqrt(sq(v.getOrDefault("x",0D))+sq(v.getOrDefault("y",0D))+sq(v.getOrDefault("z",0D))));
        if(id.startsWith("action/iterate")||id.startsWith("action/random_pos")||id.equals("action/while_delayed"))v.put("n",Math.max(0,v.getOrDefault(node.parameters.containsKey("count")?"count":"step",0D)));
        v.put("q",v.getOrDefault("count",0D));
        if(id.equals("action/delay")||id.equals("action/schedule_tick"))v.put("ticks",v.getOrDefault("tick",0D));
        if(id.equals("effect/delay"))v.put("ticks",v.getOrDefault("time",0D));
        if(id.equals("action/iterate_delayed")||id.equals("action/while_delayed"))v.put("ticks",Math.max(0,v.getOrDefault("n",0D)-1)*v.getOrDefault("delay",0D));
        if(id.equals("effect/effect")){
            var key=ResourceLocation.tryParse(node.parameters.getOrDefault("effect",""));var effect=key==null?null:BuiltInRegistries.MOB_EFFECT.get(key);
            v.put("duration",effect!=null&&effect.isInstantenous()?1D:.02*v.getOrDefault("duration",0D));
        }
        if(node.schema().category().equals("selector")){
            double r=v.getOrDefault("r",0D),size=v.getOrDefault("size",v.getOrDefault("r",0D)),y=v.getOrDefault("y",size);
            v.put("volume",switch(id){case "selector/box"->size*size*y;case "selector/ball"->4*Math.PI*r*r*r/3;case "selector/cylinder"->Math.PI*r*r*y;
                case "selector/line","selector/arc"->Math.max(0,v.getOrDefault("step",0D)+1)*Math.pow(v.getOrDefault("size",0D),3);default->0D;});
        }
    }
    public static void check(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite spell value");}
    private static IntVariable constantInt(int value){String text=Integer.toString(value);return new IntVariable(text,ExpressionHolder.ofVerified(text));}
    private static double sq(double v){return v*v;}
    private Parameters(){}
}
