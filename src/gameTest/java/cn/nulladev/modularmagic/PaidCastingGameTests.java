package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.item.*;
import cn.nulladev.modularmagic.mana.*;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PaidCastingGameTests {
    @GameTest(template="empty") public static void advancedWandAppliesBothDiscountsToRealCasting(GameTestHelper h){
        var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_expanded").get().filledStack();p.getInventory().setItem(10,battery);var doc=damage(4,0);var beginner=wand(h,p,doc);var advanced=ModContent.ADVANCED_WAND.toStack();WandCores.write(advanced,WandCores.read(beginner));p.setItemInHand(InteractionHand.MAIN_HAND,advanced);
        var item=ModContent.ADVANCED_WAND.get();double focus=PlayerFocus.value(p);h.assertTrue(item.manaMultiplier(advanced)==.5&&item.focusMultiplier(advanced)==.8,"Advanced wand defaults must be 0.5 aether and 0.8 focus");
        h.assertTrue(item.cast(advanced,p,0),"The advanced wand executes the same stored core in survival");
        h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==1000000-1350,"The actual cast pays half of the beginner wand's 2700 aether fee");h.assertTrue(Math.abs(focus-PlayerFocus.value(p)-CostPreview.estimate(doc).focus()*.8)<1e-8,"The actual cast pays 80 percent of the structural focus cost");
        h.assertTrue(new CoreWandSelector().test(advanced)&&WandCores.read(advanced).getFirst().getCount()==1,"The advanced wand retains its real core and supports the same selection controls");
        var recipe=(net.minecraft.world.item.crafting.CraftingRecipe)h.getLevel().getRecipeManager().byKey(ModularMagic.id("advanced_wand")).orElseThrow().value();var grid=net.minecraft.core.NonNullList.withSize(9,ItemStack.EMPTY);grid.set(2,cn.nulladev.modularmagic.crafting.ArtificeContent.MATTER.toStack());grid.set(4,new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));grid.set(6,new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT));var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
        h.assertTrue(recipe.matches(input,h.getLevel())&&recipe.assemble(input,h.getLevel().registryAccess()).is(ModContent.ADVANCED_WAND.get()),"Two gold ingots form the handle below the dark matter tip");h.succeed();
    }
    @GameTest(template="empty") public static void aFullUnformedSuitDiscountsManaWithoutDiscountingFocus(GameTestHelper h){
        var p=player(h);for(var pair:Map.of(net.minecraft.world.entity.EquipmentSlot.HEAD,"helmet",net.minecraft.world.entity.EquipmentSlot.CHEST,"chestplate",net.minecraft.world.entity.EquipmentSlot.LEGS,"leggings",net.minecraft.world.entity.EquipmentSlot.FEET,"boots").entrySet())p.setItemSlot(pair.getKey(),cn.nulladev.modularmagic.crafting.ArtificeContent.GEAR.get(pair.getValue()).toStack());
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_expanded").get().filledStack();p.getInventory().setItem(10,battery);var doc=damage(4,0);var wand=wand(h,p,doc);double focus=PlayerFocus.value(p);
        h.assertTrue(ModContent.CORE_WAND.get().cast(wand,p,0),"The complete suit must still allow the funded spell");
        long cost=(long)Math.ceil(2700*(1-cn.nulladev.modularmagic.crafting.ArtificeConfig.ARMOR_DISCOUNT.get()));
        h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)==(1000000-cost)*ManaStorage.SCALE,"The full suit reduces the actual item-mana charge by its configured rate");
        h.assertTrue(Math.abs(focus-PlayerFocus.value(p)-CostPreview.estimate(doc).focus())<1e-8,"Armor must not discount the structure's focus cost");h.succeed();
    }
    private static net.neoforged.neoforge.common.util.FakePlayer player(GameTestHelper h){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PaidCaster"));
        p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(1,2,1)).getCenter());
        p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);p.setHealth(p.getMaxHealth());return p;
    }
    private static SpellDocument damage(double amount,int delay){
        var doc=SpellDocument.create("root/instant");doc.insert(doc.root.id,"trigger_type",0,"trigger/self_pos");
        var parent=doc.root;String field="action";
        if(delay>0){parent=doc.insert(parent.id,field,0,"action/delay");doc.setParameters(parent.id,Map.of("tick",Integer.toString(delay)));field="child";}
        var action=doc.insert(parent.id,field,0,"action/processor");doc.insert(action.id,"selector",0,"selector/self");
        var hit=doc.insert(action.id,"processors",0,"effect/damage");doc.setParameters(hit.id,Map.of("damage",Double.toString(amount)));return doc;
    }
    private static ItemStack wand(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,SpellDocument doc){
        var item=ModContent.CORE_WAND.toStack();var list=WandCores.read(item);var compiled=SpellCompiler.compile(doc,h.getLevel().registryAccess());
        h.assertTrue(compiled.complete(),"Fixture must compile: "+compiled.errors());list.set(0,SpellCoreItem.write(doc,compiled));WandCores.write(item,list);p.setItemInHand(InteractionHand.MAIN_HAND,item);return item;
    }
    @GameTest(template="empty")
    public static void survivalPaysAndCreativeNeverPays(GameTestHelper h){
        var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_expanded").get().filledStack();var storage=(ManaStorage)battery.getItem();
        p.getInventory().setItem(10,battery);var wand=wand(h,p,damage(4,0));double before=PlayerFocus.value(p);
        h.assertTrue(ModContent.CORE_WAND.get().cast(wand,p,0),"Funded survival spell must cast");
        h.assertTrue(storage.storedUnits(battery)==1000000*ManaStorage.SCALE-2700*ManaStorage.SCALE,"Must charge 500 startup and 2200 damage with fractional precision");
        h.assertTrue(PlayerFocus.value(p)<before,"Structure focus must be paid");
        long mana=storage.storedUnits(battery);double focus=PlayerFocus.value(p);p.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.CREATIVE);
        h.assertTrue(ModContent.CORE_WAND.get().cast(wand,p,0),"Creative spell must cast");
        h.assertTrue(storage.storedUnits(battery)==mana&&PlayerFocus.value(p)==focus,"Creative mode must never debit either resource");h.succeed();
    }
    @GameTest(template="empty")
    public static void insufficientManaHasNoEffectsOrFocusCost(GameTestHelper h){
        var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
        var wand=wand(h,p,damage(1000,0));float hp=p.getHealth();double focus=PlayerFocus.value(p);
        h.assertTrue(!ModContent.CORE_WAND.get().cast(wand,p,0),"Unfundable damage must not start");
        h.assertTrue(p.getHealth()==hp&&PlayerFocus.value(p)==focus&&((ManaStorage)battery.getItem()).stored(battery)==100000,"Rejected cast must not consume anything");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void overdrawFinishesDelayedSpellAndBlocksNextCast(GameTestHelper h){
        var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
        var doc=damage(4,10);var process=doc.nodes().stream().filter(n->n.template.equals("action/processor")).findFirst().orElseThrow();
        doc.remove(process.children.get("processors").getFirst().id);
        var freeze=doc.insert(process.id,"processors",0,"effect/property");doc.setParameters(freeze.id,Map.of("property","FREEZE","duration","100"));
        var wand=wand(h,p,doc);PlayerFocus.set(p,.1);
        h.assertTrue(ModContent.CORE_WAND.get().cast(wand,p,0),"Focus overdraw must allow the funded cast");
        h.assertTrue(p.getData(PlayerFocus.TRIPPED)&&PlayerFocus.value(p)<0,"Overdraw must latch circuit breaker");
        long remaining=((ManaStorage)battery.getItem()).storedUnits(battery);
        h.assertTrue(!ModContent.CORE_WAND.get().cast(wand,p,0)&&((ManaStorage)battery.getItem()).storedUnits(battery)==remaining,"New cast must be blocked without consuming mana");
        h.runAfterDelay(12,()->{h.assertTrue(p.getTicksFrozen()==100,"Already paid delayed effect must finish while tripped");
            h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)==100000*ManaStorage.SCALE-2500*ManaStorage.SCALE,"Delay must not duplicate the effect charge");h.succeed();});
    }
    @GameTest(template="empty")
    public static void fractionalCosmeticsAggregateWithoutRoundingEachNode(GameTestHelper h){
        var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
        var d=SpellDocument.create("root/instant");d.insert(d.root.id,"trigger_type",0,"trigger/self_pos");var loop=d.insert(d.root.id,"action",0,"action/iterate");
        d.setParameters(loop.id,Map.of("step","10"));d.insert(loop.id,"child",0,"action/particle");var w=wand(h,p,d);
        double focusBefore=PlayerFocus.value(p);
        h.assertTrue(ModContent.CORE_WAND.get().cast(w,p,0),"Cosmetic spell must cast");
        double quote=CostPreview.estimate(d).focus();
        h.assertTrue(quote>=5&&Math.abs(focusBefore-PlayerFocus.value(p)-quote)<1e-8,"Cosmetic structure may be cheap, but must still pay the full fixed focus cost");
        h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)==100000*ManaStorage.SCALE-12*ManaStorage.SCALE,"10 startup plus ten 0.2 particles must cost exactly 12");h.succeed();
    }
    @GameTest(template="empty")
    public static void multipleTargetsAreFundedTogetherAndUnusedReserveReturns(GameTestHelper h){
        var p=player(h);var doc=damage(4,0);var process=doc.root.children.get("action").getFirst();
        doc.remove(process.children.get("selector").getFirst().id);
        var selector=doc.insert(process.id,"selector",0,"selector/ball");doc.setParameters(selector.id,Map.of("r","2"));
        var first=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,1,2,1);first.setNoAi(true);
        var second=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,2,2,1);second.setNoAi(true);
        var w=wand(h,p,doc);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();
        var storage=(ManaStorage)battery.getItem();p.getInventory().setItem(10,battery);
        // Other tests can populate FastProjectile's per-tick section cache before these mobs spawn.
        // Query on a fresh tick so this verifies payment, rather than an empty cached selection.
        h.runAfterDelay(2,()->{
        storage.receive(battery,3000,false);
        h.assertTrue(!ModContent.CORE_WAND.get().cast(w,p,0)&&first.getHealth()==20&&second.getHealth()==20,"Insufficient funds must not damage only an affordable subset");
        storage.receive(battery,97000,false);
        h.assertTrue(ModContent.CORE_WAND.get().cast(w,p,0),"Funded selection must cast");
        h.assertTrue(first.getHealth()<20&&second.getHealth()<20,"Both selected targets must receive the effect: health="+first.getHealth()+","+second.getHealth()+", origin="+p.position());
        double cost=1000*(.5+.05+.002*(4*Math.PI*8/3)+2*2.2);
        h.assertTrue(storage.storedUnits(battery)==100000*ManaStorage.SCALE-cn.nulladev.modularmagic.spell.billing.CastAccount.units(cost),"Unused target slots must be refunded, while both actual effects remain paid");h.succeed();});
    }
    @GameTest(template="empty")
    public static void zeroTargetsRefundEffectsAndZeroRateWandIgnoresFocusLockout(GameTestHelper h){
        var p=player(h);var doc=damage(1000,0);var process=doc.root.children.get("action").getFirst();
        doc.remove(process.children.get("selector").getFirst().id);
        var selector=doc.insert(process.id,"selector",0,"selector/ball");doc.setParameters(selector.id,Map.of("r","0.1"));
        var battery=ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack();p.getInventory().setItem(10,battery);
        var w=wand(h,p,doc);h.assertTrue(ModContent.CORE_WAND.get().cast(w,p,0),"Infinite item mana must fund a large empty selection");
        h.assertTrue(PlayerFocus.value(p)<100,"Infinite item mana must still cost focus");
        var free=ModContent.CREATIVE_WAND.toStack();WandCores.write(free,WandCores.read(w));PlayerFocus.set(p,-10);
        h.assertTrue(ModContent.CREATIVE_WAND.get().cast(free,p,0)&&PlayerFocus.value(p)==-10,"Zero-rate wand must cast during lockout without charging focus");h.succeed();
    }
    @GameTest(template="empty")
    public static void everyBuiltinSpellCanBeInstrumentedWithoutChangingThePrerequisite(GameTestHelper h){
        var p=player(h);
        for(String id:BuiltinSpells.ids()){
            var doc=SpellCoreItem.read(BuiltinSpells.create(id,h.getLevel().registryAccess()));var json=doc.toJson();
            var runtime=RuntimeSpells.get(h.getLevel(),json);
            var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,20,1,0);
            var a=cn.nulladev.modularmagic.spell.billing.CastAccount.open(p,ModContent.CREATIVE_WAND.toStack(),context,new cn.nulladev.modularmagic.spell.billing.SpellBudget.Plan(0,0,0));
            try{var program=new cn.nulladev.modularmagic.spell.billing.MeteredProgram(a,json);program.action(doc,runtime.action().action());
                if(runtime.action().condition()!=null)program.condition(doc,runtime.action().condition());
            }catch(RuntimeException e){throw new IllegalStateException("Cannot instrument "+id,e);}finally{a.close(true);}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void complexSpellsCostMoreFocusAndMatchTheirTooltips(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(10,ModContent.MANA_CONTAINERS.get("mana_container_ultimate").get().filledStack());
        var simple=damage(4,0);var complex=damage(4,0);var action=complex.root.children.get("action").getFirst();
        for(int i=1;i<10;i++)complex.insert(action.id,"processors",i,"effect/damage");
        var small=CostPreview.estimate(simple);var large=CostPreview.estimate(complex);
        h.assertTrue(Math.abs(small.focus()-8.05625)<1e-8&&Math.abs(large.focus()-17.9)<1e-8,"Fixed base plus complexity curve must distinguish one effect from ten");
        h.assertTrue(large.focus()>small.focus()*2,"Complexity must visibly increase focus cost");
        var w=wand(h,p,complex);double before=PlayerFocus.value(p);
        h.assertTrue(ModContent.CORE_WAND.get().cast(w,p,0),"Complex funded spell must cast");
        h.assertTrue(Math.abs(before-PlayerFocus.value(p)-large.focus())<1e-8,"Tooltip and actual structural charge must agree");h.succeed();
    }
    @GameTest(template="empty")
    public static void builtinSpellBudgetsAreFinite(GameTestHelper h){
        var p=player(h);var errors=new ArrayList<String>();
        for(String id:BuiltinSpells.ids()){
            var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,20,1,0);
            try{var doc=SpellCoreItem.read(BuiltinSpells.create(id,h.getLevel().registryAccess()));var plan=cn.nulladev.modularmagic.spell.billing.SpellBudget.plan(doc,context);h.assertTrue(Double.isFinite(plan.mana())&&Double.isFinite(plan.focus()),"Invalid budget: "+id);}
            catch(RuntimeException e){errors.add(id+": "+e.getMessage());}
        }
        h.assertTrue(errors.isEmpty(),String.join("; ",errors));h.succeed();
    }
    @GameTest(template="empty",batch="paid_builtins",timeoutTicks=100)
    public static void everyBuiltinCoreStartsThroughSurvivalBilling(GameTestHelper h){
        var p=player(h);p.getInventory().setItem(10,ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack());
        var anchor=h.getLevel().registryAccess().registryOrThrow(dev.xkmc.l2magic.init.registrate.EngineRegistry.SPELL).getHolder(RuntimeSpells.ANCHOR).orElseThrow();
        for(String id:BuiltinSpells.ids()){
            PlayerFocus.set(p,100);var core=BuiltinSpells.create(id,h.getLevel().registryAccess());var w=ModContent.CORE_WAND.toStack();
            var cores=WandCores.read(w);cores.set(0,core);WandCores.write(w,cores);p.setItemInHand(InteractionHand.MAIN_HAND,w);
            var doc=SpellCoreItem.read(core);var json=doc.toJson();var runtime=RuntimeSpells.get(h.getLevel(),json);
            var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,20,1,0);
            try{boolean allowed=runtime.action().test(anchor,context);boolean cast=cn.nulladev.modularmagic.spell.billing.PaidCasting.execute(p,w,json,context,runtime);
                h.assertTrue(cast==allowed,"Survival core must obey its casting condition: "+id);
            }catch(RuntimeException e){h.fail("Cannot cast example in survival: "+id+": "+e.getMessage());}
            finally{cn.nulladev.modularmagic.spell.billing.BillingRuntime.cancel(p);}
        }h.succeed();
    }
    @GameTest(template="empty",batch="focus_earth_spike",timeoutTicks=400)
    public static void earthSpikeFocusRecoversUniformlyDuringDelayedEffects(GameTestHelper h){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"FocusProbe"));
        p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(1,2,1)).getCenter());PlayerFocus.set(p,100);
        p.getInventory().setItem(10,ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack());
        var core=BuiltinSpells.create("earth_spike_field",h.getLevel().registryAccess());var doc=SpellCoreItem.read(core);var w=wand(h,p,doc);
        var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,20,1,0);
        var runtime=RuntimeSpells.get(h.getLevel(),doc.toJson());double expected=CostPreview.estimate(doc).focus();long wall=System.nanoTime();
        h.assertTrue(cn.nulladev.modularmagic.spell.billing.PaidCasting.execute(p,w,doc.toJson(),context,runtime),"Earth spike field must start");
        double initial=PlayerFocus.value(p);h.assertTrue(Math.abs(initial-(100-expected))<1e-6,"Earth spike focus quote differs from actual charge: quote="+expected+", actual="+(40-initial));
        double perTick=cn.nulladev.modularmagic.config.ResourceConfig.FOCUS_REGEN.get()/20;
        int duration=(int)Math.ceil(expected/perTick)+3;
        for(int i=1;i<=duration;i++){final int tick=i;h.runAfterDelay(i,()->{
            double before=PlayerFocus.value(p);PlayerFocus.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            double after=PlayerFocus.value(p);h.assertTrue(after>=before&&after-before<=perTick+1e-6,"Delayed spell must neither charge focus again nor jump its regeneration");
            if(tick==duration){h.assertTrue(after==100&&!p.getData(PlayerFocus.TRIPPED),"Earth spike focus must fully recover and clear lockout");ModularMagic.LOGGER.info("FOCUS_PROBE: cost={}, focus={}, elapsedMs={}",expected,after,(System.nanoTime()-wall)/1000000);cn.nulladev.modularmagic.spell.billing.BillingRuntime.cancel(p);h.succeed();}
        });}
    }
    @GameTest(template="empty")
    public static void missRefundsFollowTheRatioAndAreSettledOnlyOnce(GameTestHelper h){
        var rule=cn.nulladev.modularmagic.config.ResourceConfig.MISS_REFUND;double original=rule.get();
        try{for(double ratio:new double[]{0,.5,1}){
            rule.set(ratio);var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
            var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,0,1,0);
            var account=cn.nulladev.modularmagic.spell.billing.CastAccount.open(p,ModContent.CORE_WAND.toStack(),context,new cn.nulladev.modularmagic.spell.billing.SpellBudget.Plan(1000,0,100));
            account.begin(true);account.charge(100);rule.set(1D);account.close(false);account.close(false);
            long expected=100000-1000+ResourceBalance.refund(900,ratio);
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==expected,"Miss refund must use the cast snapshot and settle only once: "+ratio);
        }}finally{rule.set(original);}h.succeed();
    }
    @GameTest(template="empty")
    public static void hitAndCosmeticRefundsAreFullUnlessConfiguredOtherwise(GameTestHelper h){
        var mode=cn.nulladev.modularmagic.config.ResourceConfig.REFUND_MISS_ONLY;var rule=cn.nulladev.modularmagic.config.ResourceConfig.MISS_REFUND;
        boolean oldMode=mode.get();double oldRatio=rule.get();
        try{rule.set(.5);for(int scenario=0;scenario<4;scenario++){
            mode.set(scenario!=3);var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
            var context=new dev.xkmc.l2magic.content.engine.context.SpellContext(p,p.position(),dev.xkmc.l2magic.content.engine.helper.Orientation.of(p.getLookAngle(),new net.minecraft.world.phys.Vec3(0,1,0)),0,0,1,0);
            var a=cn.nulladev.modularmagic.spell.billing.CastAccount.open(p,ModContent.CORE_WAND.toStack(),context,new cn.nulladev.modularmagic.spell.billing.SpellBudget.Plan(1000,0,0));
            if(scenario!=2)a.begin(scenario!=1);a.charge(100);if(scenario==0||scenario==3)a.targetAttempt();a.close(false);
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==(scenario==3?99450:99900),"Refund scope must respect hits, cosmetics and preflight cancellation: "+scenario);
        }}finally{mode.set(oldMode);rule.set(oldRatio);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240)
    public static void emptyFocusTakesTenSecondsToRecover(GameTestHelper h){
        var p=player(h);PlayerFocus.set(p,-1);PlayerFocus.set(p,0);
        h.assertTrue(cn.nulladev.modularmagic.config.ResourceConfig.FOCUS_MAX.get()==100&&cn.nulladev.modularmagic.config.ResourceConfig.FOCUS_REGEN.get()==10,"Default focus settings must be 100 maximum and 10 per second");
        for(int i=1;i<=200;i++){final int elapsed=i;h.runAfterDelay(i,()->{
            PlayerFocus.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(Math.abs(PlayerFocus.value(p)-Math.min(100,elapsed*.5))<1e-8,"Focus must recover steadily over 200 ticks");
            h.assertTrue(p.getData(PlayerFocus.TRIPPED)==(elapsed<200),"Lockout must remain until full recovery");
            if(elapsed==200)h.succeed();
        });}
    }
    @GameTest(template="empty")
    public static void aCompleteMissNeverRefundsFocus(GameTestHelper h){
        var refund=cn.nulladev.modularmagic.config.ResourceConfig.MISS_REFUND;double original=refund.get();
        double firstFocus=0;long firstMana=0;
        try{for(double ratio:new double[]{0,1}){
            refund.set(ratio);var p=player(h);var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();p.getInventory().setItem(10,battery);
            var doc=damage(4,0);var process=doc.root.children.get("action").getFirst();doc.remove(process.children.get("selector").getFirst().id);
            var selector=doc.insert(process.id,"selector",0,"selector/ball");doc.setParameters(selector.id,Map.of("r","0.1"));
            var w=wand(h,p,doc);double before=PlayerFocus.value(p);
            h.assertTrue(ModContent.CORE_WAND.get().cast(w,p,0),"Funded empty selection must cast and settle");
            double paid=before-PlayerFocus.value(p);long mana=((ManaStorage)battery.getItem()).stored(battery);
            h.assertTrue(Math.abs(paid-CostPreview.estimate(doc).focus())<1e-8,"A complete miss must keep its entire structural focus charge after mana settlement");
            if(ratio==0){firstFocus=paid;firstMana=mana;}else{
                h.assertTrue(paid==firstFocus&&mana>firstMana,"Changing mana refund from zero to full must not refund any focus");
            }
        }}finally{refund.set(original);}h.succeed();
    }
}
