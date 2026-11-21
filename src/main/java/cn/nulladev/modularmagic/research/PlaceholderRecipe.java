package cn.nulladev.modularmagic.research;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** The center is a reference, the ring is an unordered, physically consumed recipe. */
public record PlaceholderRecipe(ResourceLocation target,List<Ingredient> ingredients) implements Recipe<CraftingInput>{
    public static final MapCodec<PlaceholderRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(ResourceLocation.CODEC.fieldOf("target").forGetter(PlaceholderRecipe::target),Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(PlaceholderRecipe::ingredients)).apply(i,PlaceholderRecipe::new));
    public PlaceholderRecipe{if(ingredients.isEmpty()||ingredients.size()>8)throw new IllegalArgumentException("A placeholder needs 1 to 8 ingredients");ingredients=List.copyOf(ingredients);}
    public boolean matchesReference(ResourceLocation reference){var family=MimicFamily.fromTarget(target);return family!=null?family.matches(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(reference)):target.equals(reference);}
    @Override public boolean matches(CraftingInput input,Level level){var stacks=input.items().stream().filter(s->!s.isEmpty()).toList();return stacks.size()==ingredients.size()&&match(stacks,0,new boolean[ingredients.size()]);}
    private boolean match(List<ItemStack> stacks,int index,boolean[] used){if(index==stacks.size())return true;for(int i=0;i<ingredients.size();i++)if(!used[i]&&ingredients.get(i).test(stacks.get(index))){used[i]=true;if(match(stacks,index+1,used))return true;used[i]=false;}return false;}
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){return PlaceholderItem.create(target);}
    public ItemStack getResultItem(HolderLookup.Provider registries){return PlaceholderItem.create(target);}
    public boolean canCraftInDimensions(int w,int h){return w*h>=ingredients.size();}
    public RecipeSerializer<?> getSerializer(){return PlaceholderContent.SERIALIZER.get();}
    public RecipeType<?> getType(){return PlaceholderContent.TYPE.get();}
    public boolean isSpecial(){return true;}
    public static final class Serializer implements RecipeSerializer<PlaceholderRecipe>{
        public MapCodec<PlaceholderRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,PlaceholderRecipe> streamCodec(){return new StreamCodec<>(){
            public PlaceholderRecipe decode(RegistryFriendlyByteBuf b){var id=b.readResourceLocation();int n=b.readVarInt();if(n<1||n>8)throw new IllegalArgumentException("Invalid ring size");var list=new ArrayList<Ingredient>();for(int i=0;i<n;i++)list.add(Ingredient.CONTENTS_STREAM_CODEC.decode(b));return new PlaceholderRecipe(id,list);}
            public void encode(RegistryFriendlyByteBuf b,PlaceholderRecipe r){b.writeResourceLocation(r.target);b.writeVarInt(r.ingredients.size());r.ingredients.forEach(i->Ingredient.CONTENTS_STREAM_CODEC.encode(b,i));}
        };}
    }
}
