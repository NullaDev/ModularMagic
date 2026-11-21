package cn.nulladev.modularmagic.generator;

import cn.nulladev.modularmagic.ModularMagic;
import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One shared chassis, independently configured world interactions. Amounts are displayed aether. */
public final class GeneratorCatalog {
    public record Defaults(String id,String topic,String icon,String zh,String en,int width,int depth,int height,
                           int period,int batch,long capacity,long output,long[] yields,String storage,boolean coldStart) {}
    public static final class Rule {
        public final Defaults defaults;
        public final ModConfigSpec.IntValue width,depth,height,period,batch;
        public final ModConfigSpec.LongValue capacity,output;
        public final List<ModConfigSpec.LongValue> yields = new ArrayList<>();
        private Rule(Defaults d,ModConfigSpec.Builder b) {
            defaults=d;b.push(d.id());
            width=b.defineInRange("width",d.width(),1,32);depth=b.defineInRange("depth",d.depth(),1,32);
            height=b.defineInRange("height",d.height(),1,32);period=b.defineInRange(d.id().equals("curing")?"cooldownAfterCureTicks":"periodTicks",d.id().equals("curing")?0:d.period(),d.id().equals("curing")?0:1,72000);
            batch=b.defineInRange("maximumUnitsPerCycle",d.batch(),1,32768);
            capacity=b.defineInRange("capacity",d.capacity(),1,1_000_000_000_000L);
            output=b.defineInRange("outputPerSecond",d.output(),1,1_000_000_000_000L);
            for(int i=0;i<d.yields().length;i++)yields.add(b.defineInRange("aetherPerUnit"+(i==0?"":i+1),d.yields()[i],1,1_000_000_000_000L));
            b.pop();
        }
        public String id(){return defaults.id();}
        public long yield(int variant){return yields.get(Math.clamp(variant,0,yields.size()-1)).get();}
        public ItemStack icon(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(defaults.icon())));}
        public String translation(){return "generator.modularmagic."+id();}
    }
    public static final Map<String,Rule> RULES=new LinkedHashMap<>();
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue CURE_MIN,CURE_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ARMOR_YIELDS;
    static {
        var b=new ModConfigSpec.Builder();
        try(var stream=Objects.requireNonNull(GeneratorCatalog.class.getResourceAsStream("/data/modularmagic/generator_defaults.json"));
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            for(var d:new Gson().fromJson(reader,Defaults[].class))RULES.put(d.id(),new Rule(d,b));
        }catch(Exception e){throw new ExceptionInInitializerError(e);}
        b.push("curing");CURE_MIN=b.defineInRange("minimumCureTicks",3600,20,72000);CURE_MAX=b.defineInRange("maximumCureTicks",6000,20,72000);b.pop();
        var armor=new ArrayList<String>();String[] types={"leather","golden","iron","chainmail","diamond","netherite"};
        String[] slots={"helmet","chestplate","leggings","boots"};long[][] prices={{200,320,280,160},{1000,1600,1400,800},{2500,4000,3500,2000},{2500,4000,3500,2000},{12500,20000,17500,10000},{42500,50000,47500,40000}};
        for(int i=0;i<types.length;i++)for(int j=0;j<slots.length;j++)armor.add("minecraft:"+types[i]+"_"+slots[j]+"="+prices[i][j]);
        ARMOR_YIELDS=b.comment("Exact item IDs and full durability yield. Enchantments never increase yield.").defineListAllowEmpty("armorYields",armor,()->"minecraft:iron_chestplate=4000",v->v instanceof String s&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=\\d+"));
        SPEC=b.build();
    }
    public static Rule get(String id){return RULES.get(id);}
    public static long armorYield(ItemStack stack){
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for(String line:ARMOR_YIELDS.get())if(line.startsWith(id+"="))try{
            long value=Long.parseLong(line.substring(id.length()+1));
            return stack.isDamageableItem()?(long)Math.floor(value*(1D-stack.getDamageValue()/(double)stack.getMaxDamage())):value;
        }catch(NumberFormatException ignored){return 0;}
        return 0;
    }
    private GeneratorCatalog(){}
}
