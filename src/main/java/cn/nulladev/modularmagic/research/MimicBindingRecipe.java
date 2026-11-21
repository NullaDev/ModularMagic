package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** A reusable rule borrows one material from a shared family, then returns that exact material on separation. */
public record MimicBindingRecipe(MimicFamily family,boolean separate) implements CraftingRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,ModularMagic.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<MimicBindingRecipe>> SERIALIZER=SERIALIZERS.register("mimic_binding",()->new RecipeSerializer<>(){
        public MapCodec<MimicBindingRecipe> codec(){return RecordCodecBuilder.mapCodec(i->i.group(com.mojang.serialization.Codec.STRING.xmap(MimicFamily::parse,f->f.id).fieldOf("family").forGetter(MimicBindingRecipe::family),com.mojang.serialization.Codec.BOOL.fieldOf("separate").forGetter(MimicBindingRecipe::separate)).apply(i,MimicBindingRecipe::new));}
        public StreamCodec<RegistryFriendlyByteBuf,MimicBindingRecipe> streamCodec(){return new StreamCodec<>(){public MimicBindingRecipe decode(RegistryFriendlyByteBuf b){return new MimicBindingRecipe(b.readEnum(MimicFamily.class),b.readBoolean());}public void encode(RegistryFriendlyByteBuf b,MimicBindingRecipe r){b.writeEnum(r.family);b.writeBoolean(r.separate);}};}
    });
    public static void register(IEventBus bus){SERIALIZERS.register(bus);}
    public static boolean material(ItemStack stack){return stack.getItem() instanceof BlockItem b&&MimicFamily.of(b.getBlock())!=null&&stack.getComponentsPatch().isEmpty();}
    public static boolean bound(ItemStack stack){var id=PlaceholderItem.target(stack);return stack.is(PlaceholderContent.ITEM.get())&&id!=null&&MimicFamily.of(BuiltInRegistries.BLOCK.get(id))!=null;}
    public boolean matches(CraftingInput input,Level level){var stacks=input.items().stream().filter(s->!s.isEmpty()).toList();return separate?stacks.size()==1&&bound(stacks.getFirst())&&family.matches(BuiltInRegistries.BLOCK.get(PlaceholderItem.target(stacks.getFirst()))):stacks.size()==2&&stacks.stream().anyMatch(s->s.is(family.template().getItem()))&&stacks.stream().anyMatch(s->material(s)&&family.matches(((BlockItem)s.getItem()).getBlock()));}
    public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){if(separate)return family.template();for(var stack:input.items())if(material(stack))return PlaceholderItem.create(BuiltInRegistries.BLOCK.getKey(((BlockItem)stack.getItem()).getBlock()));return ItemStack.EMPTY;}
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){var remaining=NonNullList.withSize(input.size(),ItemStack.EMPTY);if(separate)for(int i=0;i<input.size();i++)if(bound(input.getItem(i)))remaining.set(i,new ItemStack(BuiltInRegistries.BLOCK.get(PlaceholderItem.target(input.getItem(i))).asItem()));return remaining;}
    public ItemStack getResultItem(HolderLookup.Provider registries){return separate?family.template():PlaceholderItem.create(BuiltInRegistries.BLOCK.getKey(((BlockItem)family.example).getBlock()));}
    public boolean canCraftInDimensions(int width,int height){return width*height>=(separate?1:2);}
    public CraftingBookCategory category(){return CraftingBookCategory.MISC;}
    public boolean isSpecial(){return true;}
    public RecipeSerializer<?> getSerializer(){return SERIALIZER.get();}
}
