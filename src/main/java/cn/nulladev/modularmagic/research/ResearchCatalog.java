package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.spell.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ResearchCatalog {
    public record Topic(String id,List<String> requires,List<String> nodes,List<String> sources,double reward,String zh,String en){public String key(){return "research.modularmagic."+id.toLowerCase(Locale.ROOT);}}
    private static final Map<String,Topic> TOPICS=new LinkedHashMap<>();
    static {try(var reader=new InputStreamReader(Objects.requireNonNull(ResearchCatalog.class.getResourceAsStream("/data/modularmagic/research/topics.json")),StandardCharsets.UTF_8)){
        for(var e:JsonParser.parseReader(reader).getAsJsonArray()){var t=new Gson().fromJson(e,Topic.class);TOPICS.put(t.id(),t);}
    }catch(Exception e){throw new ExceptionInInitializerError(e);}}
    public static Collection<Topic> all(){return Collections.unmodifiableCollection(TOPICS.values());}
    public static Topic get(String id){var t=TOPICS.get(id);if(t==null)throw new IllegalArgumentException("Unknown research");return t;}
    public static List<Topic> source(String block){return all().stream().filter(t->t.sources().contains(block)).toList();}
    public static String next(net.minecraft.world.entity.player.Player p,Topic t){return t.nodes().stream().filter(n->!ResearchKnowledge.knows(p,n)).findFirst().orElse("");}

    private static final Map<String,String> DEFAULT=Map.ofEntries(Map.entry("action","action/particle"),Map.entry("trigger","trigger/self_pos"),Map.entry("selector","selector/self"),Map.entry("effect","effect/push"),Map.entry("modifier","modifier/forward"),Map.entry("predicate","predicate/expression"),Map.entry("filter","filter/effect"),Map.entry("motion","motion/simple"),Map.entry("particle_renderer","particle_renderer/simple"),Map.entry("projectile_renderer","projectile_renderer/simple"),Map.entry("projectile","projectile/config"));
    private static void fill(SpellDocument d,SpellDocument.Node n,int depth){if(depth>12)throw new IllegalArgumentException("Recursive lesson");for(var s:n.schema().slots()){
        if(!s.optional()&&n.children.get(s.key()).isEmpty())d.insert(n.id,s.key(),0,DEFAULT.get(s.category()));
        for(var child:n.children.get(s.key()))fill(d,child,depth+1);
    }}
    private static SpellDocument.Node add(SpellDocument d,SpellDocument.Node p,String slot,String template){return d.insert(p.id,slot,p.children.get(slot).size(),template);}
    /** Small, finite assembly exercises. No simulation or live spell execution is involved. */
    public static SpellDocument reference(String target){
        var t=NodeCatalog.get(target);var d=SpellDocument.create(t.category().equals("root")?target:"root/instant");var root=d.root;SpellDocument.Node node;
        switch(t.category()){
            case "root" -> node=root;
            case "trigger" -> node=add(d,root,"trigger_type",target);
            case "action" -> node=add(d,root,"action",target);
            case "predicate" -> node=add(d,root,"condition",target);
            case "effect","selector","filter" -> {
                var a=add(d,root,"action","action/processor");
                if(t.category().equals("selector"))node=add(d,a,"selector",target);
                else if(t.category().equals("effect"))node=add(d,a,"processors",target);
                else {var e=add(d,a,"processors","effect/filtered");node=add(d,e,"filter",target);}
            }
            case "modifier" -> {var a=add(d,root,"action","action/move");node=add(d,a,"modifiers",target);}
            case "motion","particle_renderer" -> {var a=add(d,root,"action","action/custom_particle");node=add(d,a,t.category().equals("motion")?"motion":"renderer",target);}
            case "projectile","projectile_renderer" -> {var a=add(d,root,"action","action/custom_projectile");node=add(d,a,"config","projectile/config");if(t.category().equals("projectile_renderer"))node=add(d,node,"renderer",target);}
            default -> throw new IllegalArgumentException("Unsupported lesson");
        }
        fill(d,root,0);return d;
    }
    public static boolean equivalent(SpellDocument.Node a,SpellDocument.Node b){
        if(!a.template.equals(b.template)||!a.parameters.keySet().equals(b.parameters.keySet())||!a.children.keySet().equals(b.children.keySet()))return false;
        for(String key:a.parameters.keySet())if(!sameValue(a.parameters.get(key),b.parameters.get(key)))return false;
        for(String k:a.children.keySet()){var x=a.children.get(k);var y=b.children.get(k);if(x.size()!=y.size())return false;for(int i=0;i<x.size();i++)if(!equivalent(x.get(i),y.get(i)))return false;}return true;
    }
    private static boolean sameValue(String a,String b){if(a.trim().equals(b.trim()))return true;try{return JsonParser.parseString(a).equals(JsonParser.parseString(b));}catch(RuntimeException e){return a.replaceAll("\\s+","").equals(b.replaceAll("\\s+",""));}}
    private ResearchCatalog(){}
}
