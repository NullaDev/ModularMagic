package cn.nulladev.modularmagic.utility;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Immutable reference stacks. They never enter a physical inventory. */
public record CrafterPattern(List<ItemStack> samples,boolean enabled,String recipe) {
    public static final CrafterPattern EMPTY=new CrafterPattern(NonNullList.withSize(9,ItemStack.EMPTY),false,"");
    public static final com.mojang.serialization.Codec<CrafterPattern> CODEC=RecordCodecBuilder.create(i->i.group(ItemStack.OPTIONAL_CODEC.listOf().fieldOf("samples").forGetter(CrafterPattern::samples),com.mojang.serialization.Codec.BOOL.fieldOf("enabled").forGetter(CrafterPattern::enabled),com.mojang.serialization.Codec.STRING.fieldOf("recipe").forGetter(CrafterPattern::recipe)).apply(i,CrafterPattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf,CrafterPattern> STREAM_CODEC=new StreamCodec<>(){
        public CrafterPattern decode(RegistryFriendlyByteBuf b){var stacks=new ArrayList<ItemStack>();for(int i=0;i<9;i++)stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));return new CrafterPattern(stacks,b.readBoolean(),b.readUtf());}
        public void encode(RegistryFriendlyByteBuf b,CrafterPattern p){for(var s:p.samples)ItemStack.OPTIONAL_STREAM_CODEC.encode(b,s);b.writeBoolean(p.enabled);b.writeUtf(p.recipe);}
    };
    public CrafterPattern {if(samples.size()!=9)throw new IllegalArgumentException("A crafter needs nine references");samples=samples.stream().map(s->s.isEmpty()?ItemStack.EMPTY:s.copyWithCount(1)).toList();}
    public CrafterPattern active(boolean active){return new CrafterPattern(samples,active,recipe);}
}
