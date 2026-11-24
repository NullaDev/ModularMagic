package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.utility.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UtilityGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"UtilityTester"));p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().selected=1;p.setPos(h.absolutePos(new BlockPos(2,2,2)).getCenter());return p;}
    private static ItemStack battery(ServerPlayer p,long amount){var b=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();((ManaStorage)b.getItem()).receive(b,amount,false);p.getInventory().setItem(0,b);return b;}
    private static long mana(ServerPlayer p){long n=0;for(var s:p.getInventory().items)if(s.getItem() instanceof ManaStorage m)n+=m.stored(s);return n;}
    private static ItemStack mimic(String id){return PlaceholderItem.create(ResourceLocation.withDefaultNamespace(id));}
    private static ItemStack crafter(ServerPlayer p,String recipe,ItemStack... samples){var grid=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<samples.length;i++)grid.set(i,samples[i]);var tool=UtilityContent.CRAFTER.toStack();tool.set(UtilityContent.PATTERN.get(),new CrafterPattern(grid,true,recipe.contains(":")?recipe:"minecraft:"+recipe));p.setItemInHand(InteractionHand.MAIN_HAND,tool);return tool;}
    private static int count(ServerPlayer p,Item item){return p.getInventory().items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private static UseOnContext context(ServerPlayer p,BlockPos pos){return new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));}

    private static ItemStack unit(cn.nulladev.modularmagic.utility.AetherUtilityItem item,ItemStack sample,long energy){
        var stack=new ItemStack(item);
        AetherUtilityItem.setSample(stack,sample);
        item.receive(stack,energy,false);
        return stack;
    }
    @GameTest(template="empty",batch="utilities") public static void burningUnitPaysForActualFurnaceIgnitions(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.FURNACE.defaultBlockState());
        var furnace=(FurnaceBlockEntity)h.getLevel().getBlockEntity(pos);
        var fuel=unit(UtilityContent.BURN.get(),PlaceholderItem.bind(MimicFamily.CARPETS,new ItemStack(Items.WHITE_CARPET)),2_000);
        int burn=fuel.getBurnTime(RecipeType.SMELTING);
        h.assertTrue(burn==new ItemStack(Items.WHITE_CARPET).getBurnTime(RecipeType.SMELTING),"The sample determines burn duration");
        furnace.setItem(0,new ItemStack(Items.RAW_IRON,3));furnace.setItem(1,fuel);
        for(int tick=0;tick<610;tick++)AbstractFurnaceBlockEntity.serverTick(h.getLevel(),pos,h.getLevel().getBlockState(pos),furnace);
        h.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT)&&furnace.getItem(2).getCount()==3,"Furnace keeps smelting while its unit has aether");
        h.assertTrue(furnace.getItem(1).is(UtilityContent.BURN.get())&&UtilityContent.BURN.get().stored(fuel)<2_000,"The furnace drains the unit and retains its sample");
        var player=player(h);
        var target=pos.east();
        h.getLevel().setBlockAndUpdate(target,Blocks.COBBLESTONE.defaultBlockState());
        var charcoal=PlaceholderItem.derived(new ItemStack(Items.CHARCOAL),800);
        var handheld=unit(UtilityContent.BURN.get(),charcoal,2_000);
        player.setItemInHand(InteractionHand.MAIN_HAND,handheld);
        UtilityContent.BURN.get().useOn(context(player,target));
        h.assertTrue(h.getLevel().getBlockState(target).is(Blocks.STONE)&&UtilityContent.BURN.get().stored(handheld)==1_900,
                "World smelting replaces the block and pays the fraction of fuel used");
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void launchUnitFertilizesValidTargetsWithoutConsumingItsSample(GameTestHelper h){
        var p=player(h);
        var item=UtilityContent.LAUNCH.get();
        var sample=mimic("bone_meal");
        var stack=unit(item,sample,5_000);
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        item.useOn(context(p,pos));
        h.assertTrue(item.stored(stack)==5_000,"Invalid fertilization leaves the cache alone");
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,Blocks.WHEAT.defaultBlockState());
        item.useOn(context(p,pos));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(net.minecraft.world.level.block.CropBlock.AGE)>0
                &&item.stored(stack)==5_000-PlaceholderItem.price(sample),"Valid fertilization consumes the sample's fee from the cache");
        h.assertTrue(PlaceholderItem.material(AetherUtilityItem.sample(stack)).is(Items.BONE_MEAL),"The sample remains fitted");
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void eatingUnitUsesItsBoundFoodAndCache(GameTestHelper h){
        var p=player(h);
        var item=UtilityContent.FOOD.get();
        var sample=PlaceholderItem.bind(MimicFamily.PLANTS,new ItemStack(Items.CARROT));
        var food=unit(item,sample,5_000);
        p.setItemInHand(InteractionHand.MAIN_HAND,food);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);
        h.assertTrue(food.getUseDuration(p)>0,"The sampled food defines the use duration");
        var returned=food.finishUsingItem(h.getLevel(),p);
        h.assertTrue(returned==food&&p.getFoodData().getFoodLevel()==13&&Math.abs(p.getFoodData().getSaturationLevel()-3.6f)<.001,"Sampled carrot reproduces vanilla nutrition");
        h.assertTrue(item.stored(food)==5_000-PlaceholderItem.price(sample),"Food charges the unit cache");
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void cannonPropelsPlayerWithoutDamageAndChargesOnce(GameTestHelper h){
        var p=player(h);var sample=mimic("tnt");
        var item=UtilityContent.CANNON.get();
        var cannon=unit(item,sample,50_000);
        p.setItemInHand(InteractionHand.MAIN_HAND,cannon);
        var bounds=new AABB(p.blockPosition()).inflate(6);
        p.setDeltaMovement(Vec3.ZERO);p.setXRot(0);p.fallDistance=12;
        float health=p.getHealth();
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        var motion=p.getDeltaMovement();
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(motion.y>=UtilityConfig.CANNON_LIFT.get()&&motion.horizontalDistance()>0&&p.getDeltaMovement().equals(motion),"One use propels the wearer and cooldown stops another boost");
        h.assertTrue(p.getHealth()==health&&p.fallDistance==12,"The impulse does no damage and preserves ordinary fall damage");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,bounds).isEmpty(),"Propulsion does not spawn a TNT entity");
        h.assertTrue(item.stored(cannon)==50_000-PlaceholderItem.price(sample),"The cannon charges for the successful boost");
        var egg=mimic("egg");
        var launcher=unit(UtilityContent.LAUNCH.get(),egg,2_000);
        p.setItemInHand(InteractionHand.MAIN_HAND,launcher);
        UtilityContent.LAUNCH.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownEgg.class,bounds).size()==1
                &&UtilityContent.LAUNCH.get().stored(launcher)==2_000-PlaceholderItem.price(egg),"The survival launch unit throws an egg and retains its sample");
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void utilityRecipesLoadAndSampleIngredientsKeepTheirTarget(GameTestHelper h){
        for(String name:List.of("placement_unit","eating_unit","launch_unit","burning_unit","tool_unit","portable_tnt_cannon","bone_meal_sample","handheld_aether_crafting_table","aether_materializer","auto_crafter")){
            var r=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ModularMagic.id(name)).orElseThrow().value();
            var displayed=r.getIngredients().stream().map(i->i.getItems()[0].copy()).toList();
            var input=CraftingInput.of(3,3,pad(displayed));
            h.assertTrue(r.matches(input,h.getLevel())&&!r.assemble(input,h.getLevel().registryAccess()).isEmpty(),"JEI-visible recipe must match displayed ingredients: "+name);
            if(name.equals("bone_meal_sample")||name.equals("portable_tnt_cannon")){
                var sample=r.getIngredients().stream().filter(i->i.getCustomIngredient() instanceof SampleIngredient).findFirst().orElseThrow();
                h.assertTrue(!sample.test(mimic("dirt"))&&!sample.test(PlaceholderContent.ITEM.toStack()),"The ingredient must match the exact material target");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void handheldDerivesWoodAndStoneWithOneBlankEach(GameTestHelper h){
        var p=player(h);var energy=battery(p,10_000);
        p.setItemInHand(InteractionHand.MAIN_HAND,UtilityContent.HANDHELD.toStack());
        var menu=new HandheldCraftingMenu(8,p.getInventory(),InteractionHand.MAIN_HAND);
        var original=mimic("oak_log");
        menu.getSlot(1).set(original);
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty(),"A preview cannot be taken without its blank");
        menu.getSlot(46).set(PlaceholderContent.ITEM.toStack());
        menu.clicked(0,0,ClickType.PICKUP,p);
        var planks=menu.getCarried();
        long price=PlaceholderItem.price(planks);
        h.assertTrue(PlaceholderItem.material(planks).is(Items.OAK_PLANKS)&&price==300,"One log has the one-quarter plank generation price");
        h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(46).getItem().isEmpty()&&((ManaStorage)energy.getItem()).stored(energy)==10_000-price,"Taking a derivation pays once and incorporates the source sample and blank");
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(1).set(planks.copy());
        menu.clicked(1,1,ClickType.PICKUP,p);
        menu.clicked(4,1,ClickType.PICKUP,p);
        menu.getSlot(46).set(PlaceholderContent.ITEM.toStack());
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(PlaceholderItem.material(menu.getCarried()).is(Items.STICK)&&PlaceholderItem.price(menu.getCarried())==150,
                "One plank sample can fill two ingredient positions");
        h.assertTrue(menu.getSlot(1).getItem().isEmpty()&&menu.reference(3).isEmpty(),"A repeated reference consumes its single physical sample and clears the reference");
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(1).set(mimic("stone"));
        menu.getSlot(2).set(new ItemStack(Items.STONE_SLAB));
        menu.getSlot(46).set(PlaceholderContent.ITEM.toStack());
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(PlaceholderItem.material(menu.getCarried()).is(Items.STONE_SLAB)&&PlaceholderItem.price(menu.getCarried())==240,"A real slab selects the two-slab cutting recipe");
        h.assertTrue(menu.getSlot(2).getItem().is(Items.STONE_SLAB),"The cutting selector remains in the grid");
        h.assertTrue(menu.getSlot(1).getItem().isEmpty(),"The stone sample becomes part of its result");
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(1).set(ItemStack.EMPTY);
        menu.getSlot(2).set(ItemStack.EMPTY);
        menu.getSlot(4).set(mimic("oak_log"));
        menu.getSlot(5).set(new ItemStack(Items.FURNACE));
        menu.getSlot(6).set(planks);
        menu.getSlot(46).set(PlaceholderContent.ITEM.toStack());
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(PlaceholderItem.material(menu.getCarried()).is(Items.CHARCOAL)&&PlaceholderItem.price(menu.getCarried())==1_400,
                "The furnace derivation adds the proportional fuel cost");
        h.assertTrue(menu.getSlot(5).getItem().is(Items.FURNACE)&&menu.getSlot(4).getItem().isEmpty()
                &&menu.getSlot(6).getItem().isEmpty(),"The furnace remains while both samples become part of the result");
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(4).set(ItemStack.EMPTY);
        menu.getSlot(5).set(ItemStack.EMPTY);
        menu.getSlot(6).set(ItemStack.EMPTY);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG));
        long balance=((ManaStorage)energy.getItem()).stored(energy);
        menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().is(Items.OAK_PLANKS)&&menu.getCarried().getCount()==4
                        && ((ManaStorage)energy.getItem()).stored(energy)==balance,
                "Physical materials fall back to ordinary crafting without an aether charge");
        h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void unformedToolsUseStickSamplesAndCreativeShowsPhysicalSamples(GameTestHelper h){
        var level=h.getLevel();
        var stick=PlaceholderItem.derived(new ItemStack(Items.STICK),150);
        for(String tool:List.of("sword","pickaxe","axe","shovel","hoe")){
            var recipe=(ShapedRecipe)level.getRecipeManager().byKey(ModularMagic.id("unformed_"+tool)).orElseThrow().value();
            var samples=recipe.getIngredients().stream().map(ingredient->{
                if(ingredient.isEmpty())return ItemStack.EMPTY;
                return ingredient.getCustomIngredient() instanceof SampleIngredient?stick.copy():ingredient.getItems()[0].copy();
            }).toList();
            h.assertTrue(samples.stream().anyMatch(stack->PlaceholderItem.material(stack).is(Items.STICK))
                    &&recipe.matches(CraftingInput.of(recipe.getWidth(),recipe.getHeight(),samples),level),"Derived sticks assemble the unformed "+tool);
            var physical=samples.stream().map(stack->PlaceholderItem.target(stack)==null?stack:new ItemStack(Items.STICK)).toList();
            h.assertTrue(!recipe.matches(CraftingInput.of(recipe.getWidth(),recipe.getHeight(),physical),level),"Physical sticks cannot replace samples");
        }
        var shown=PlaceholderContent.physicalSamples();
        h.assertTrue(builtinSamplesAreUnbound()
                &&shown.stream().anyMatch(stack->PlaceholderItem.material(stack).is(Items.BONE_MEAL))
                &&shown.stream().noneMatch(MimicBindingRecipe::bound)
                &&shown.stream().noneMatch(stack->PlaceholderItem.material(stack).is(Items.STICK)),
                "Creative uses one blank for each family and lists only directly shaped material samples");
        h.succeed();
    }
    private static boolean builtinSamplesAreUnbound(){
        return Arrays.stream(MimicFamily.values()).allMatch(family->PlaceholderItem.target(family.template())==null);
    }
    @GameTest(template="empty",batch="utilities") public static void materializerNeedsBucketsAndPaysPerOutput(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,1,1));
        var block=cn.nulladev.modularmagic.energy.EnergyContent.BLOCKS.get(cn.nulladev.modularmagic.energy.EnergyContent.Kind.MATERIALIZER).get();
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var device=(cn.nulladev.modularmagic.energy.EnergyBlockEntity)h.getLevel().getBlockEntity(pos);
        device.setItem(0,mimic("water"));
        device.energy=600*ManaStorage.SCALE;
        int interval=UtilityConfig.MATERIALIZER_INTERVAL.get();
        h.runAfterDelay(interval+1,()->{
            h.assertTrue(device.product.isEmpty()&&device.energy==600*ManaStorage.SCALE,"No bucket means no output or payment");
            device.setItem(2,new ItemStack(Items.BUCKET));
        });
        h.runAfterDelay(interval*2+3,()->{
            h.assertTrue(device.product.is(Items.WATER_BUCKET)&&device.emptyBuckets.isEmpty()&&device.energy==100*ManaStorage.SCALE,"Materialization consumes one real bucket and one material fee");
            h.assertTrue(PlaceholderItem.target(device.getItem(0)).equals(ResourceLocation.withDefaultNamespace("water")),"The machine retains its sample");
            h.succeed();
        });
    }
    private static List<ItemStack> pad(List<ItemStack> samples){var g=NonNullList.withSize(9,ItemStack.EMPTY);for(int i=0;i<samples.size();i++)g.set(i,samples.get(i));return g;}
    @GameTest(template="empty",batch="utilities") public static void autoCrafterPrefersRealInputsThenPaysForMimicGeneration(GameTestHelper h){
        var p=player(h);battery(p,10000);var tool=crafter(p,"oak_planks",new ItemStack(Items.OAK_LOG));p.getInventory().setItem(2,new ItemStack(Items.OAK_LOG));p.getInventory().setItem(3,mimic("oak_log"));long base=UtilityConfig.CRAFT.get(),generation=(long)Math.ceil(MaterialPlacement.fee(Blocks.OAK_LOG.defaultBlockState()));
        h.assertTrue(AutoCrafting.tryCraft(p,tool)&&count(p,Items.OAK_PLANKS)==4&&mana(p)==10000-base,"A physical log is preferred over generating one");
        h.assertTrue(AutoCrafting.tryCraft(p,tool)&&count(p,Items.OAK_PLANKS)==8&&mana(p)==10000-2*base-generation&&p.getInventory().getItem(3).getCount()==1,"A bound plant mimic supplies one missing log for its configured fee");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void autoCrafterRollsBackMissingMaterialsAetherAndOutputSpace(GameTestHelper h){
        var p=player(h);battery(p,0);var tool=crafter(p,"oak_planks",new ItemStack(Items.OAK_LOG));p.getInventory().setItem(2,new ItemStack(Items.OAK_LOG,2));h.assertTrue(!AutoCrafting.tryCraft(p,tool)&&count(p,Items.OAK_LOG)==2,"Unfunded operation cannot consume inputs");
        battery(p,10000);p.getInventory().setItem(2,ItemStack.EMPTY);h.assertTrue(!AutoCrafting.tryCraft(p,tool)&&mana(p)==10000,"Missing materials cannot charge the base fee");
        p.getInventory().setItem(2,new ItemStack(Items.OAK_LOG,2));for(int i=3;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));h.assertTrue(!AutoCrafting.tryCraft(p,tool)&&count(p,Items.OAK_LOG)==2&&mana(p)==10000,"A full backpack preserves all resources");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void autoCrafterConservesFluidContainersAndVanillaRemainders(GameTestHelper h){
        var p=player(h);battery(p,20000);var tool=crafter(p,"mud_bricks",new ItemStack(Items.PACKED_MUD),new ItemStack(Items.PACKED_MUD),ItemStack.EMPTY,new ItemStack(Items.PACKED_MUD),new ItemStack(Items.PACKED_MUD));
        // Cake consumes three fluid ingredients and returns their real empty buckets.
        var grid=List.of(new ItemStack(Items.MILK_BUCKET),new ItemStack(Items.MILK_BUCKET),new ItemStack(Items.MILK_BUCKET),new ItemStack(Items.SUGAR),new ItemStack(Items.EGG),new ItemStack(Items.SUGAR),new ItemStack(Items.WHEAT),new ItemStack(Items.WHEAT),new ItemStack(Items.WHEAT));tool.set(UtilityContent.PATTERN.get(),new CrafterPattern(grid,true,"minecraft:cake"));
        for(int i=0;i<grid.size();i++)p.getInventory().setItem(i+2,grid.get(i).copy());h.assertTrue(AutoCrafting.tryCraft(p,tool)&&count(p,Items.CAKE)==1&&count(p,Items.BUCKET)==3,"Real fluid ingredients must keep their vanilla crafting remainders");
        // A water bucket in a custom recipe is generated from water, never from bucket metal.
        var concrete=(CraftingRecipe)h.getLevel().getRecipeManager().byKey(ModularMagic.id("generator_water")).orElseThrow().value();
        var samples=concrete.getIngredients().stream().map(i->i.getItems()[0].copy()).toList();tool.set(UtilityContent.PATTERN.get(),new CrafterPattern(pad(samples),true,"modularmagic:generator_water"));
        p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,tool);battery(p,100000);p.getInventory().setItem(2,mimic("water"));int slot=3;for(var s:samples)if(!s.is(Items.WATER_BUCKET))p.getInventory().setItem(slot++,s.copy());
        long before=mana(p);h.assertTrue(!AutoCrafting.tryCraft(p,tool)&&mana(p)==before,"Water mimics cannot conjure the metal for water buckets");p.getInventory().setItem(20,new ItemStack(Items.BUCKET,2));
        h.assertTrue(AutoCrafting.tryCraft(p,tool)&&count(p,Items.BUCKET)==2,"Generating and using water buckets conserves both real buckets");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void autoCrafterHandlesMirroredPatternsAndIngredientAlternatives(GameTestHelper h){
        var p=player(h);battery(p,10000);var tool=crafter(p,"wooden_axe",ItemStack.EMPTY,new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.OAK_PLANKS),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY);
        p.getInventory().setItem(2,new ItemStack(Items.BIRCH_PLANKS,3));p.getInventory().setItem(3,new ItemStack(Items.STICK,2));h.assertTrue(AutoCrafting.tryCraft(p,tool)&&count(p,Items.WOODEN_AXE)==1,"A mirrored axe pattern accepts other planks from the recipe's ingredient tag");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void autoCrafterPreservesContainerIdentityWhileUpgrading(GameTestHelper h){
        var p=player(h);var source=battery(p,10000);source.set(DataComponents.CUSTOM_NAME,Component.literal("My Jar"));var identity=cn.nulladev.modularmagic.spell.billing.ManaEscrow.identity(source);
        var recipe=(cn.nulladev.modularmagic.crafting.ContainerUpgradeRecipe)h.getLevel().getRecipeManager().byKey(ModularMagic.id("mana_container_expanded")).orElseThrow().value();var samples=recipe.getIngredients().stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList();var tool=crafter(p,"modularmagic:mana_container_expanded",samples.toArray(ItemStack[]::new));int next=2;
        for(var s:samples)if(!s.isEmpty()&&!s.is(source.getItem()))p.getInventory().setItem(next++,s.copy());
        h.assertTrue(AutoCrafting.tryCraft(p,tool),"An upgrade can pay its fee from the retained aether");var result=p.getInventory().items.stream().filter(s->s.is(ModContent.MANA_CONTAINERS.get("mana_container_expanded").get())).findFirst().orElseThrow();
        h.assertTrue(mana(p)==10000-UtilityConfig.CRAFT.get()&&result.getHoverName().getString().equals("My Jar")&&cn.nulladev.modularmagic.spell.billing.ManaEscrow.identity(result).equals(identity),"Crafting cannot erase a container's aether, name, or delayed refund identity");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void crafterGhostSlotsNeverBecomeRealItemsAndEditsPauseIt(GameTestHelper h){
        var p=player(h);var tool=crafter(p,"oak_planks",new ItemStack(Items.OAK_LOG));var m=new AutoCrafterMenu(1,p.getInventory(),InteractionHand.MAIN_HAND);m.setCarried(new ItemStack(Items.OAK_LOG,12));m.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(m.getCarried().getCount()==12&&m.pattern.getItem(0).getCount()==1&&m.preview.getItem(0).is(Items.OAK_PLANKS)&&!m.enabled(),"A reference click keeps the cursor and pauses edited patterns");m.setCarried(ItemStack.EMPTY);m.clicked(9,0,ClickType.PICKUP,p);h.assertTrue(m.getCarried().isEmpty()&&count(p,Items.OAK_PLANKS)==0,"The preview must never be taken");
        h.assertTrue(m.clickMenuButton(p,0)&&m.enabled(),"A valid pattern can be enabled");m.clicked(0,1,ClickType.PICKUP,p);h.assertTrue(m.pattern.getItem(0).isEmpty()&&!m.enabled(),"Right click clears a reference without producing an item");
        m.clicked(38,0,ClickType.PICKUP,p);h.assertTrue(p.getMainHandItem()==tool&&m.getCarried().isEmpty(),"The operating tool is locked in its hotbar slot");m.removed(p);h.assertTrue(count(p,Items.OAK_LOG)==0&&count(p,Items.OAK_PLANKS)==0,"Closing the menu cannot return its ghost references as real ingredients");h.succeed();
    }
    @GameTest(template="empty",batch="utilities") public static void autoCrafterRunsOncePerItemTickButPausesInItsEditor(GameTestHelper h){
        var p=player(h);battery(p,10000);var tool=crafter(p,"oak_planks",new ItemStack(Items.OAK_LOG));p.getInventory().setItem(2,new ItemStack(Items.OAK_LOG,3));tool.inventoryTick(h.getLevel(),p,1,true);h.assertTrue(count(p,Items.OAK_PLANKS)==4&&count(p,Items.OAK_LOG)==2,"An inventory tick performs exactly one craft");
        p.containerMenu=new AutoCrafterMenu(3,p.getInventory(),InteractionHand.MAIN_HAND);tool.inventoryTick(h.getLevel(),p,1,true);h.assertTrue(count(p,Items.OAK_PLANKS)==4,"Crafting pauses while its pattern editor is open");p.containerMenu=p.inventoryMenu;p.setGameMode(GameType.CREATIVE);p.getInventory().setItem(0,ItemStack.EMPTY);tool.inventoryTick(h.getLevel(),p,1,true);h.assertTrue(count(p,Items.OAK_PLANKS)==8,"Creative auto crafting still consumes materials but no aether");h.succeed();
    }
}
