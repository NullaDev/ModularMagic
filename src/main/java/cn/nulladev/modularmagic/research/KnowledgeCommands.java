package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.mana.PlayerFocus;
import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid=ModularMagic.MOD_ID)
public final class KnowledgeCommands {
    private static final DynamicCommandExceptionType INVALID=new DynamicCommandExceptionType(id->Component.translatable("commands.modularmagic.unknown_knowledge",id));
    private static List<String> ids(){var out=new ArrayList<String>();out.add("all");ResearchCatalog.all().forEach(t->out.add(t.id()));NodeCatalog.all().forEach(n->out.add(n.id()));return out;}
    private static Set<String> resolve(String id)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var result=new LinkedHashSet<String>();if(id.equals("all")){for(var t:ResearchCatalog.all()){result.add(t.id());result.addAll(t.nodes());}return result;}
        for(var t:ResearchCatalog.all())if(t.id().equalsIgnoreCase(id)){result.add(t.id());result.addAll(t.nodes());return result;}
        if(NodeCatalog.all().stream().anyMatch(n->n.id().equals(id))){result.add(id);return result;}throw INVALID.create(id);
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        var root=Commands.literal("modularmagic").requires(s->s.hasPermission(2));var knowledge=Commands.literal("knowledge");
        for(String operation:List.of("grant","revoke"))knowledge.then(Commands.literal(operation).then(Commands.argument("players",EntityArgument.players()).then(Commands.argument("knowledge",StringArgumentType.greedyString()).suggests((c,b)->SharedSuggestionProvider.suggest(ids(),b)).executes(c->{String id=StringArgumentType.getString(c,"knowledge");var selected=resolve(id);var players=EntityArgument.getPlayers(c,"players");for(var p:players)change(p,selected,operation.equals("grant"));c.getSource().sendSuccess(()->Component.translatable("commands.modularmagic."+operation,id,players.size()),true);return players.size();}))));
        knowledge.then(Commands.literal("reset").then(Commands.argument("players",EntityArgument.players()).executes(c->{var players=EntityArgument.getPlayers(c,"players");for(var p:players){p.setData(ResearchKnowledge.DATA,new CompoundTag());sync(p);}c.getSource().sendSuccess(()->Component.translatable("commands.modularmagic.reset",players.size()),true);return players.size();})));
        knowledge.then(Commands.literal("prepare").then(Commands.argument("players",EntityArgument.players()).then(Commands.argument("topic",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(ResearchCatalog.all().stream().map(ResearchCatalog.Topic::id),b)).executes(c->{String id=StringArgumentType.getString(c,"topic").toUpperCase(Locale.ROOT);ResearchCatalog.Topic topic;try{topic=ResearchCatalog.get(id);}catch(RuntimeException ex){throw INVALID.create(id);}var players=EntityArgument.getPlayers(c,"players");for(var p:players)prepare(p,topic);c.getSource().sendSuccess(()->Component.translatable("commands.modularmagic.prepare",id,players.size()),true);return players.size();}))));
        var node=e.getDispatcher().register(root.then(knowledge));e.getDispatcher().register(Commands.literal("mm").requires(s->s.hasPermission(2)).redirect(node));
    }
    public static void change(ServerPlayer p,Set<String> selected,boolean grant){var state=ResearchKnowledge.state(p).copy();for(String id:selected){if(grant)state.putBoolean("known:"+id,true);else{state.remove("known:"+id);state.remove("draft:"+id);state.remove("paid:"+id);state.remove("observed:"+id);}}p.setData(ResearchKnowledge.DATA,state);sync(p);}
    private static void ancestors(ResearchCatalog.Topic t,Set<String> out){for(String id:t.requires())if(out.add(id))ancestors(ResearchCatalog.get(id),out);}
    public static void prepare(ServerPlayer p,ResearchCatalog.Topic t){var state=ResearchKnowledge.state(p).copy();var ancestors=new LinkedHashSet<String>();ancestors(t,ancestors);for(String id:ancestors){var topic=ResearchCatalog.get(id);state.putBoolean("known:"+id,true);topic.nodes().forEach(n->state.putBoolean("known:"+n,true));}
        state.remove("known:"+t.id());state.remove("observed:"+t.id());for(String n:t.nodes()){state.remove("known:"+n);state.remove("draft:"+n);state.remove("paid:"+n);}state.remove("active");p.setData(ResearchKnowledge.DATA,state);
        var note=ResearchNoteItem.create(t.id(),t.nodes().isEmpty()||t.id().equals("K00"));if(!p.addItem(note))p.drop(note,false);for(int i=0;i<4;i++){var ink=ResearchContent.INK.toStack();if(!p.addItem(ink))p.drop(ink,false);}sync(p);
    }
    private static void sync(ServerPlayer p){ResearchNetworking.sync(p);PlayerFocus.set(p,PlayerFocus.value(p));PacketDistributor.sendToPlayer(p,new EditorNetworking.FocusSnapshot(PlayerFocus.value(p),PlayerFocus.maximum(p),p.getData(PlayerFocus.TRIPPED)));if(p.containerMenu instanceof ResearchMenu menu)menu.refreshKnowledge();}
    private KnowledgeCommands(){}
}
