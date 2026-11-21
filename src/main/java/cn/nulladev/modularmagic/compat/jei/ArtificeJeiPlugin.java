package cn.nulladev.modularmagic.compat.jei;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.crafting.*;
import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.generator.*;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import cn.nulladev.modularmagic.util.AetherNumbers;
import mezz.jei.api.*;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

@JeiPlugin
public final class ArtificeJeiPlugin implements IModPlugin {
    public static final RecipeType<BindingDisplay> BINDING=RecipeType.create(ModularMagic.MOD_ID,"mimic_binding",BindingDisplay.class);
    public record BindingDisplay(MimicFamily family,ItemStack material,boolean separate){}
    public static final RecipeType<CarvingRecipe> CARVING=RecipeType.create(ModularMagic.MOD_ID,"rune_carving",CarvingRecipe.class);
    public static final RecipeType<PlaceholderRecipe> SIGIL=RecipeType.create(ModularMagic.MOD_ID,"sigil",PlaceholderRecipe.class);
    public static final RecipeType<EnchantDisplay> ENCHANTING=RecipeType.create(ModularMagic.MOD_ID,"enchanting",EnchantDisplay.class);
    public static final RecipeType<Condensing> CONDENSING=RecipeType.create(ModularMagic.MOD_ID,"condensing",Condensing.class);
    public static final RecipeType<Assembly> ASSEMBLY=RecipeType.create(ModularMagic.MOD_ID,"spell_assembly",Assembly.class);
    public static mezz.jei.api.runtime.IJeiRuntime runtime;
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime value){runtime=value;}
    @Override public void onRuntimeUnavailable(){runtime=null;}
    public record Assembly(ItemStack root,ItemStack trigger,ItemStack action,ItemStack result){}
    public record EnchantDisplay(List<ItemStack> targets,List<ItemStack> samples,List<ItemStack> results,long cost){}
    public record Condensing(ItemStack device,ItemStack result,long cost){}
    @Override public ResourceLocation getPluginUid(){return ModularMagic.id("artifice");}
    private static Component tr(String key,Object...args){return Component.translatable("jei.modularmagic."+key,args);}
    private static Component knowledge(String id){return id.startsWith("K")||id.startsWith("P")?Component.translatable(ResearchCatalog.get(id).key()):Component.translatable(NodeCatalog.get(id).translationKey());}
    @Override public void registerItemSubtypes(ISubtypeRegistration r){
        r.registerSubtypeInterpreter(EnergyContent.ITEMS.get(EnergyContent.Kind.GENERATOR).get(),(stack,context)->GeneratorItem.ruleId(stack));
        r.registerSubtypeInterpreter(PlaceholderContent.ITEM.get(),(stack,context)->String.valueOf(PlaceholderItem.target(stack)));
    }
    @Override public void registerCategories(IRecipeCategoryRegistration r){var helper=r.getJeiHelpers().getGuiHelper();r.addRecipeCategories(new Carving(helper),new Sigils(helper),new Enchanting(helper),new Condensation(helper),new SpellAssembly(helper),new Binding(helper));}
    @Override public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration r){
        r.getCraftingCategory().addExtension(ContainerUpgradeRecipe.class,new mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension<ContainerUpgradeRecipe>(){
            @Override public int getWidth(RecipeHolder<ContainerUpgradeRecipe> holder){return holder.value().delegate().getWidth();}
            @Override public int getHeight(RecipeHolder<ContainerUpgradeRecipe> holder){return holder.value().delegate().getHeight();}
            @Override public void setRecipe(RecipeHolder<ContainerUpgradeRecipe> holder,IRecipeLayoutBuilder builder,mezz.jei.api.gui.ingredient.ICraftingGridHelper grid,IFocusGroup focuses){
                var recipe=holder.value().delegate();grid.createAndSetInputs(builder,recipe.getIngredients().stream().map(i->Arrays.asList(i.getItems())).toList(),recipe.getWidth(),recipe.getHeight());
                grid.createAndSetOutputs(builder,List.of(cn.nulladev.modularmagic.registry.ModContent.MANA_CONTAINERS.get("mana_container_expanded").toStack()));
            }
        });
        r.getCraftingCategory().addExtension(GeneratorHousingRecipe.class,new mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension<GeneratorHousingRecipe>(){
            @Override public void setRecipe(RecipeHolder<GeneratorHousingRecipe> holder,IRecipeLayoutBuilder builder,mezz.jei.api.gui.ingredient.ICraftingGridHelper grid,IFocusGroup focuses){
                grid.createAndSetInputs(builder,holder.value().getIngredients().stream().map(i->Arrays.asList(i.getItems())).toList(),0,0);
                grid.createAndSetOutputs(builder,List.of(GeneratorItems.HOUSING.toStack()));builder.setShapeless();
            }
        });
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        r.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE),BINDING);
        r.addRecipeCatalyst(ResearchContent.CARVER_ITEM.toStack(),CARVING);r.addRecipeCatalyst(PlaceholderContent.ITEM.toStack(),SIGIL);
        r.addRecipeCatalyst(EnergyContent.ITEMS.get(EnergyContent.Kind.ENCHANTER).toStack(),ENCHANTING);
        for(var k:List.of(EnergyContent.Kind.DISRUPTOR,EnergyContent.Kind.CONDENSER,EnergyContent.Kind.CRYSTAL_CHAMBER))r.addRecipeCatalyst(EnergyContent.ITEMS.get(k).toStack(),CONDENSING);
        r.addRecipeCatalyst(cn.nulladev.modularmagic.registry.ModContent.WRITING_TABLE_ITEM.toStack(),ASSEMBLY);
    }
    @Override public void registerRecipes(IRecipeRegistration r){
        var level=Minecraft.getInstance().level;if(level==null)return;
        var bindings=new ArrayList<BindingDisplay>();for(var item:BuiltInRegistries.ITEM){var stack=new ItemStack(item);if(MimicBindingRecipe.material(stack)){bindings.add(new BindingDisplay(MimicFamily.of(((BlockItem)stack.getItem()).getBlock()),stack,false));bindings.add(new BindingDisplay(MimicFamily.of(((BlockItem)stack.getItem()).getBlock()),stack,true));}}r.addRecipes(BINDING,bindings);
        r.addRecipes(CARVING,CarvingRecipe.all(level));r.addRecipes(SIGIL,level.getRecipeManager().getAllRecipesFor(PlaceholderContent.TYPE.get()).stream().map(RecipeHolder::value).toList());
        // Custom crafting recipes need an explicit entry: the vanilla scanner skips special recipes.
        r.addRecipes(RecipeTypes.CRAFTING,level.getRecipeManager().getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING).stream().filter(h->h.value() instanceof GeneratorHousingRecipe).toList());
        var examples=new ArrayList<EnchantDisplay>();
        for(var enchantment:level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().toList()){
            var targets=BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->s.isDamageableItem()&&s.supportsEnchantment(enchantment)).toList();
            if(targets.isEmpty())continue;
            var value=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);value.set(enchantment,1);var pattern=value.toImmutable();
            var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,pattern);
            var gearSamples=targets.stream().map(s->{var copy=s.copy();copy.set(DataComponents.ENCHANTMENTS,pattern);return copy;}).toList();
            for(var samples:List.of(List.of(book),gearSamples))for(int old=0;old<=Math.min(1,enchantment.value().getMaxLevel()-1);old++){
                var inputs=new ArrayList<ItemStack>();var results=new ArrayList<ItemStack>();long cost=0;
                for(var target:targets){var copy=target.copy();if(old>0)copy.set(DataComponents.ENCHANTMENTS,pattern);var q=AetherEnchanting.quote(copy,samples.getFirst());if(q.result().isEmpty())continue;inputs.add(copy);results.add(q.result());cost=q.aether();}
                if(!inputs.isEmpty())examples.add(new EnchantDisplay(inputs,samples,results,cost));
            }
        }
        r.addRecipes(ENCHANTING,examples);var fragments=new ArrayList<Condensing>();
        var assemblies=new ArrayList<Assembly>();for(String kind:List.of("instant","charge","continuous")){
            var d=cn.nulladev.modularmagic.spell.SpellDocument.create("root/"+kind);d.insert(d.root.id,"trigger_type",0,"trigger/self_pos");d.insert(d.root.id,"action",0,"action/particle");
            var compiled=cn.nulladev.modularmagic.spell.SpellCompiler.compile(d,level.registryAccess());
            assemblies.add(new Assembly(cn.nulladev.modularmagic.registry.ModContent.NODES.get("root/"+kind).toStack(),cn.nulladev.modularmagic.registry.ModContent.NODES.get("trigger/self_pos").toStack(),cn.nulladev.modularmagic.registry.ModContent.NODES.get("action/particle").toStack(),cn.nulladev.modularmagic.item.SpellCoreItem.write(d,compiled)));
        }
        var incomplete=cn.nulladev.modularmagic.spell.SpellDocument.create("root/instant");incomplete.insert(incomplete.root.id,"trigger_type",0,"trigger/self_pos");
        assemblies.add(new Assembly(cn.nulladev.modularmagic.registry.ModContent.NODES.get("root/instant").toStack(),cn.nulladev.modularmagic.registry.ModContent.NODES.get("trigger/self_pos").toStack(),ItemStack.EMPTY,cn.nulladev.modularmagic.item.SpellCoreItem.write(incomplete,cn.nulladev.modularmagic.spell.SpellCompiler.compile(incomplete,level.registryAccess()))));
        r.addRecipes(ASSEMBLY,assemblies);
        for(var k:List.of(EnergyContent.Kind.DISRUPTOR,EnergyContent.Kind.CONDENSER,EnergyContent.Kind.CRYSTAL_CHAMBER)){boolean stable=k==EnergyContent.Kind.CRYSTAL_CHAMBER;fragments.add(new Condensing(EnergyContent.ITEMS.get(k).toStack(),AetherShardItem.create(stable,level),EnergyConfig.SHARD.get()+(stable?EnergyConfig.CRYSTAL_FEE.get():0)));}r.addRecipes(CONDENSING,fragments);
    }
    private abstract static class Category<T> implements IRecipeCategory<T>{
        private final RecipeType<T> type;private final Component title;private final IDrawable background,icon;
        Category(IGuiHelper h,RecipeType<T> type,String name,ItemStack icon,int height){this.type=type;title=tr(name);background=h.createBlankDrawable(160,height);this.icon=h.createDrawableItemStack(icon);}
        public RecipeType<T> getRecipeType(){return type;}public Component getTitle(){return title;}public IDrawable getBackground(){return background;}public IDrawable getIcon(){return icon;}
        void text(GuiGraphics g,Component text,int x,int y){g.drawString(Minecraft.getInstance().font,text,x,y,0xff444444,false);}
    }
    private static final class Carving extends Category<CarvingRecipe>{
        Carving(IGuiHelper h){super(h,CARVING,"carving",ResearchContent.CARVER_ITEM.toStack(),72);}
        public void setRecipe(IRecipeLayoutBuilder b,CarvingRecipe r,IFocusGroup focus){b.addSlot(RecipeIngredientRole.INPUT,18,8).addIngredients(r.ingredient());b.addSlot(RecipeIngredientRole.OUTPUT,122,8).addItemStack(r.result());}
        public void draw(CarvingRecipe r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){text(g,Component.literal("→"),74,12);g.drawWordWrap(Minecraft.getInstance().font,tr("knowledge",knowledge(r.knowledge())),2,36,156,0xff444444);}
    }
    private static ItemStack reference(PlaceholderRecipe r){var family=MimicFamily.fromTarget(r.target());if(family!=null)return new ItemStack(family.example);String id=r.target().getPath();return new ItemStack(switch(id){case "water"->Items.WATER_BUCKET;case "lava"->Items.LAVA_BUCKET;case "powder_snow"->Items.POWDER_SNOW_BUCKET;case "fire"->Items.FLINT_AND_STEEL;default->BuiltInRegistries.ITEM.get(r.target());});}
    private static final class Sigils extends Category<PlaceholderRecipe>{
        Sigils(IGuiHelper h){super(h,SIGIL,"sigil",PlaceholderContent.ITEM.toStack(),104);}
        public void setRecipe(IRecipeLayoutBuilder b,PlaceholderRecipe r,IFocusGroup focus){int[][] ring={{48,4},{66,4},{84,4},{48,22},{84,22},{48,40},{66,40},{84,40}};b.addSlot(RecipeIngredientRole.INPUT,6,22).addItemStack(PlaceholderContent.ITEM.toStack());b.addSlot(RecipeIngredientRole.CATALYST,66,22).addItemStack(reference(r));for(int i=0;i<r.ingredients().size();i++)b.addSlot(RecipeIngredientRole.INPUT,ring[i][0],ring[i][1]).addIngredients(r.ingredients().get(i));b.addSlot(RecipeIngredientRole.OUTPUT,132,22).addItemStack(PlaceholderItem.create(r.target()));}
        public void draw(PlaceholderRecipe r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){text(g,tr("sample"),2,66);text(g,tr("knowledge",knowledge("K31")),2,84);}
    }
    private static final class Binding extends Category<BindingDisplay>{
        Binding(IGuiHelper h){super(h,BINDING,"binding",PlaceholderContent.ITEM.toStack(),52);}
        public void setRecipe(IRecipeLayoutBuilder b,BindingDisplay r,IFocusGroup f){var id=BuiltInRegistries.BLOCK.getKey(((BlockItem)r.material.getItem()).getBlock());var bound=PlaceholderItem.create(id);
            b.addSlot(RecipeIngredientRole.INPUT,6,8).addItemStack(r.separate?bound:r.family.template());
            if(!r.separate)b.addSlot(RecipeIngredientRole.INPUT,34,8).addItemStack(r.material);
            b.addSlot(RecipeIngredientRole.OUTPUT,106,8).addItemStack(r.separate?r.family.template():bound);
            if(r.separate)b.addSlot(RecipeIngredientRole.OUTPUT,134,8).addItemStack(r.material);
        }
        public void draw(BindingDisplay r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){text(g,Component.literal("\u2192"),72,12);}
    }
    private static final class Enchanting extends Category<EnchantDisplay>{
        Enchanting(IGuiHelper h){super(h,ENCHANTING,"enchanting",EnergyContent.ITEMS.get(EnergyContent.Kind.ENCHANTER).toStack(),64);}
        public void setRecipe(IRecipeLayoutBuilder b,EnchantDisplay r,IFocusGroup f){var input=b.addSlot(RecipeIngredientRole.INPUT,8,4).addItemStacks(r.targets);b.addSlot(RecipeIngredientRole.CATALYST,54,4).addItemStacks(r.samples);var output=b.addSlot(RecipeIngredientRole.OUTPUT,130,4).addItemStacks(r.results);b.createFocusLink(input,output);}
        public void draw(EnchantDisplay r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){text(g,tr("sample"),2,30);text(g,tr("aether",AetherNumbers.format(r.cost)),2,46);}
    }
    private static final class Condensation extends Category<Condensing>{
        Condensation(IGuiHelper h){super(h,CONDENSING,"condensing",EnergyContent.CRYSTAL.toStack(),54);}
        public void setRecipe(IRecipeLayoutBuilder b,Condensing r,IFocusGroup f){b.addSlot(RecipeIngredientRole.CATALYST,16,4).addItemStack(r.device);b.addSlot(RecipeIngredientRole.OUTPUT,128,4).addItemStack(r.result);}
        public void draw(Condensing r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){text(g,Component.literal("→"),74,8);text(g,tr("aether",AetherNumbers.format(r.cost)),2,34);}
    }
    private static final class SpellAssembly extends Category<Assembly>{
        SpellAssembly(IGuiHelper h){super(h,ASSEMBLY,"assembly",cn.nulladev.modularmagic.registry.ModContent.WRITING_TABLE_ITEM.toStack(),66);}
        public void setRecipe(IRecipeLayoutBuilder b,Assembly r,IFocusGroup f){b.addSlot(RecipeIngredientRole.INPUT,2,4).addItemStack(r.root);b.addSlot(RecipeIngredientRole.INPUT,29,4).addItemStack(r.trigger);if(!r.action.isEmpty())b.addSlot(RecipeIngredientRole.INPUT,56,4).addItemStack(r.action);b.addSlot(RecipeIngredientRole.OUTPUT,132,4).addItemStack(r.result);}
        public void draw(Assembly r,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){g.drawWordWrap(Minecraft.getInstance().font,tr("assembly_example"),2,34,156,0xff444444);}
    }
}
