package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;

/** Individual discoveries survive death; borrowed spell cores need no research permission. */
public final class ResearchKnowledge {
    private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,ModularMagic.MOD_ID);
    public static final java.util.function.Supplier<AttachmentType<CompoundTag>> DATA=TYPES.register("research",()->AttachmentType.builder(CompoundTag::new).serialize(CompoundTag.CODEC).copyOnDeath().build());
    public static void register(IEventBus bus){TYPES.register(bus);}
    public static boolean knows(Player p,String id){return p.getData(DATA).getBoolean("known:"+id);}
    public static boolean completed(Player p,ResearchCatalog.Topic t){return t.nodes().isEmpty()?knows(p,t.id()):t.nodes().stream().allMatch(n->knows(p,n));}
    public static boolean ready(Player p,ResearchCatalog.Topic t){return t.requires().stream().allMatch(id->completed(p,ResearchCatalog.get(id)));}
    public static void learn(Player p,String id){var copy=p.getData(DATA).copy();copy.putBoolean("known:"+id,true);p.setData(DATA,copy);}
    public static double bonus(Player p){double sum=0;for(var t:ResearchCatalog.all()){
        if(t.nodes().isEmpty()){if(p.getData(DATA).getBoolean("known:"+t.id()))sum+=ResearchContent.KNOWLEDGE_FOCUS.get();}
        else if(!t.id().equals("K00"))for(String n:t.nodes())if(p.getData(DATA).getBoolean("known:"+n))sum+=ResearchContent.KNOWLEDGE_FOCUS.get()/t.nodes().size();
    }return Math.min(ResearchContent.FOCUS_BONUS_CAP.get(),sum);}
    public static CompoundTag state(Player p){return p.getData(DATA);}
    public static void save(Player p,String key,String value){var t=p.getData(DATA).copy();t.putString(key,value);p.setData(DATA,t);}
    private ResearchKnowledge(){}
}
