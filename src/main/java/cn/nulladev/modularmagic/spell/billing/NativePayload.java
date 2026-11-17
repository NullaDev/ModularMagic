package cn.nulladev.modularmagic.spell.billing;

import cn.nulladev.modularmagic.spell.SpellDocument;
import java.util.*;

/** Native arrows/tridents carry their own damage outside the spell AST. */
public final class NativePayload {
    public static double damage(double d){var values=CostRules.defaults();values.put("damage",d);return CostRules.mana("effect/damage",values);}
    public static double actualEffects(Object record,CastAccount account){
        double total=0;try{for(var c:record.getClass().getRecordComponents())if(c.getAccessor().invoke(record) instanceof List<?> list)
            for(var entry:list)if(entry instanceof dev.xkmc.l2magic.content.engine.helper.EffectInstanceEntry e){
                double t=Double.parseDouble(e.duration().str()),a=Double.parseDouble(e.amplifier().str());
                var values=CostRules.defaults();values.put("duration",e.effect().value().isInstantenous()?1:.02*t);values.put("amplifier",a);
                total+=account.price("effect/effect",values);
            }}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}return total;
    }
    public static double budget(SpellDocument.Node node,Map<String,Double> p,Map<String,Range> vars){
        double d=node.template.equals("action/trident")?8:Math.ceil(2*Math.abs(p.getOrDefault("speed",1D)))*1.5+2;
        return bound("effect/damage",Map.of("damage",new Range(0,d)))+effects(node,vars);
    }
    public static double effects(SpellDocument.Node node,Map<String,Range> vars){
        String text=node.parameters.getOrDefault("effects","");if(text.isBlank())return 0;
        double total=0;var json=com.google.gson.JsonParser.parseString(text);if(!json.isJsonArray())return 0;
        for(var value:json.getAsJsonArray()){
            var o=value.getAsJsonObject();var duration=Range.eval(o.get("duration").getAsString(),vars);
            var amplifier=o.has("amplifier")?Range.eval(o.get("amplifier").getAsString(),vars):Range.at(0);
            if(duration.magnitude()>CostRules.DURATION.get()||amplifier.magnitude()>32)throw new IllegalArgumentException("Arrow payload exceeds limit");
            var key=net.minecraft.resources.ResourceLocation.parse(o.get("effect").getAsString());
            var effect=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(key);
            var timed=effect!=null&&effect.isInstantenous()?Range.at(1):new Range(.02*duration.lo(),.02*duration.hi());
            total+=bound("effect/effect",Map.of("duration",timed,"amplifier",amplifier));
        }return total;
    }
    private static double bound(String id,Map<String,Range> overrides){
        var values=new HashMap<String,Range>();CostRules.defaults().forEach((k,v)->values.put(k,Range.at(v)));values.putAll(overrides);
        var price=CostRules.manaBound(id,values);if(price.lo()<0||price.hi()>1e12)throw new IllegalArgumentException("Invalid payload price");return price.hi();
    }
    private NativePayload(){}
}
