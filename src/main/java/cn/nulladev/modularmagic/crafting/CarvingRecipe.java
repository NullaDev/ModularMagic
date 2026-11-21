package cn.nulladev.modularmagic.crafting;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.research.*;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.*;

public record CarvingRecipe(String key,String knowledge,Ingredient ingredient,ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CarvingRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        com.mojang.serialization.Codec.STRING.fieldOf("key").forGetter(CarvingRecipe::key),
        com.mojang.serialization.Codec.STRING.fieldOf("knowledge").forGetter(CarvingRecipe::knowledge),
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CarvingRecipe::ingredient),
        ItemStack.CODEC.fieldOf("result").forGetter(CarvingRecipe::result)).apply(i,CarvingRecipe::new));
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_TYPE,ModularMagic.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,ModularMagic.MOD_ID);
    public static final DeferredHolder<RecipeType<?>,RecipeType<CarvingRecipe>> TYPE=TYPES.register("rune_carving",()->new RecipeType<>(){});
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<CarvingRecipe>> SERIALIZER=SERIALIZERS.register("rune_carving",()->new RecipeSerializer<>(){
        public MapCodec<CarvingRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,CarvingRecipe> streamCodec(){return new StreamCodec<>(){
            public CarvingRecipe decode(RegistryFriendlyByteBuf b){return new CarvingRecipe(b.readUtf(),b.readUtf(),Ingredient.CONTENTS_STREAM_CODEC.decode(b),ItemStack.STREAM_CODEC.decode(b));}
            public void encode(RegistryFriendlyByteBuf b,CarvingRecipe r){b.writeUtf(r.key);b.writeUtf(r.knowledge);Ingredient.CONTENTS_STREAM_CODEC.encode(b,r.ingredient);ItemStack.STREAM_CODEC.encode(b,r.result);}
        };}
    });
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);}
    public boolean unlocked(Player p){if(knowledge.startsWith("K")||knowledge.startsWith("P"))return ResearchKnowledge.completed(p,ResearchCatalog.get(knowledge));return ResearchKnowledge.knows(p,knowledge);}
    public boolean matches(SingleRecipeInput input,Level level){return ingredient.test(input.item());}
    public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider r){return result.copy();}
    public ItemStack getResultItem(HolderLookup.Provider r){return result.copy();}
    public NonNullList<Ingredient> getIngredients(){return NonNullList.of(Ingredient.EMPTY,ingredient);}
    public boolean canCraftInDimensions(int w,int h){return w*h>=1;}
    public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
    public RecipeType<?> getType(){return TYPE.get();}
    public boolean isSpecial(){return true;}
    public static List<CarvingRecipe> all(Level level){return level.getRecipeManager().getAllRecipesFor(TYPE.get()).stream().map(RecipeHolder::value).toList();}
}
