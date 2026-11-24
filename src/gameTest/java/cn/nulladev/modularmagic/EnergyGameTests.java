package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.billing.ManaEscrow;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyGameTests {
    private static final long U=ManaStorage.SCALE;
    private static EnergyBlockEntity device(GameTestHelper h,EnergyContent.Kind kind,int x,int y,int z) {
        var pos=h.absolutePos(new BlockPos(x,y,z));
        h.getLevel().setBlockAndUpdate(pos,EnergyContent.BLOCKS.get(kind).get().defaultBlockState());
        var be=(EnergyBlockEntity)h.getLevel().getBlockEntity(pos);EnergyWorld.add(h.getLevel(),pos);return be;
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"EnergyTester"));
        p.getInventory().clearContent();p.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());return p;
    }
    private static ItemStack receiver(GameTestHelper h,EnergyBlockEntity be) {
        var stack=EnergyContent.RECEIVER.toStack();var tag=EnergyLinkItem.data(stack);
        tag.putString("Dimension",h.getLevel().dimension().location().toString());tag.putLong("Position",be.getBlockPos().asLong());tag.putUUID("Device",be.identity);if(be.kind()==EnergyContent.Kind.RESONATOR&&ResonanceNetwork.root(be)!=null)tag.putUUID("Network",ResonanceNetwork.root(be));EnergyLinkItem.data(stack,tag);return stack;
    }
    private static String pulseProbe(EnergyBlockEntity be) {
        if(be.pulseStart==null||be.pulseEnd==null)return "source="+be.getBlockPos()+", target="+be.target+", energy="+be.energy;
        var hit=be.getLevel().clip(new net.minecraft.world.level.ClipContext(be.pulseStart,be.pulseEnd,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return "source="+be.getBlockPos()+", target="+be.target+", hit="+hit.getType()+"@"+hit.getBlockPos()+", block="+be.getLevel().getBlockState(hit.getBlockPos())+", energy="+be.energy;
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=140)
    public static void disruptorActuallyPreventsSourceRegeneration(GameTestHelper h) {
        for(int x=0;x<5;x++)for(int z=0;z<5;z++)h.setBlock(x,1,z,Blocks.STONE);
        var be=device(h,EnergyContent.Kind.DISRUPTOR,2,1,2);
        h.setBlock(1,2,2,Blocks.WATER);h.setBlock(3,2,2,Blocks.WATER);
        h.runAfterDelay(65,()->{
            var fluid=h.getLevel().getFluidState(be.getBlockPos().above());
            h.assertTrue(!fluid.isEmpty()&&!fluid.isSource(),"The middle cell must stay flowing, not produce a free water source");
            h.assertTrue(be.energy>0&&be.energy<=40*U,"Only configured passive cycles may produce mana");
            h.getLevel().setBlockAndUpdate(be.getBlockPos(),Blocks.STONE.defaultBlockState());
            h.getLevel().scheduleTick(be.getBlockPos().above(),net.minecraft.world.level.material.Fluids.FLOWING_WATER,5);
            h.runAfterDelay(20,()->{h.assertTrue(h.getLevel().getFluidState(be.getBlockPos().above()).isSource(),"Removing the disruptor must restore normal water regeneration");h.succeed();});
        });
    }
    @GameTest(template="empty",batch="energy")
    public static void touchingDebitsTheSingleCacheAndExpirationSurvivesCopies(GameTestHelper h) {
        var p=player(h);var be=device(h,EnergyContent.Kind.DISRUPTOR,1,1,1);be.energy=1000*U;
        be.interact(p,InteractionHand.MAIN_HAND);var shard=p.getInventory().getItem(0);
        h.assertTrue(shard.is(EnergyContent.SHARD.get())&&be.energy==800*U,"Touch extracts one fragment from the same cache");
        var copy=shard.copy();var tag=EnergyLinkItem.data(copy);tag.putLong("AetherExpires",AetherShardItem.clock(h.getLevel()));EnergyLinkItem.data(copy,tag);
        h.assertTrue(AetherShardItem.expired(copy,h.getLevel())&&!net.minecraft.world.item.crafting.Ingredient.of(EnergyContent.SHARD.get()).test(copy),"Expired fragments cannot match crafting ingredients");
        h.assertTrue(!AetherShardItem.expired(shard.copy(),h.getLevel()),"Copying a live fragment preserves its original deadline");h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=150)
    public static void droppedFragmentsDissipateButCrystalsRemain(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(1,2,1));
        var live=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),pos.getX(),pos.getY(),pos.getZ(),AetherShardItem.create(false,h.getLevel()));
        var stable=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),pos.getX()+1,pos.getY(),pos.getZ(),AetherShardItem.create(true,h.getLevel()));
        h.getLevel().addFreshEntity(live);h.getLevel().addFreshEntity(stable);
        h.runAfterDelay(110,()->{h.assertTrue(!live.isAlive()&&stable.isAlive(),"Only unstable fragments must disappear at their original expiry");h.succeed();});
    }
    @GameTest(template="empty",batch="energy")
    public static void transfersConserveEnergyAndSharePortBandwidth(GameTestHelper h) {
        var source=device(h,EnergyContent.Kind.TANK,1,1,1);var a=device(h,EnergyContent.Kind.TANK,3,1,1);var b=device(h,EnergyContent.Kind.TANK,3,1,3);
        source.energy=10000*U;
        long first=EnergyWorld.transfer(source,a,Long.MAX_VALUE),second=EnergyWorld.transfer(source,b,Long.MAX_VALUE);
        h.assertTrue(first==500*U&&second==0,"All faces share a single 2000/s source quota");
        h.assertTrue(source.energy+a.energy+b.energy==10000*U,"Transfer must conserve the actual stored units");h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=60)
    public static void creativeGeneratorContinuouslyFeedsAdjacentReservoir(GameTestHelper h) {
        device(h,EnergyContent.Kind.CREATIVE,1,1,1);var tank=device(h,EnergyContent.Kind.TANK,2,1,1);
        h.runAfterDelay(30,()->{h.assertTrue(tank.energy>=2000*U,"Creative generator must continue supplying, rather than acting as a one-use charged block");h.succeed();});
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=60)
    public static void pulsePaysOnArrivalAndReservesCapacity(GameTestHelper h) {
        for(int x=0;x<=4;x++)h.setBlock(x,2,1,Blocks.AIR);
        var emitter=device(h,EnergyContent.Kind.EMITTER,0,2,1);var tank=device(h,EnergyContent.Kind.TANK,4,2,1);
        emitter.energy=1000*U;emitter.target=tank.getBlockPos();emitter.targetIdentity=tank.identity;
        boolean started=emitter.startPulse();h.assertTrue(started,"A funded clear route must start a pulse: "+pulseProbe(emitter));emitter.outputPaused=true;
        h.assertTrue(tank.energy==0&&EnergyWorld.incoming(tank)==100*U&&emitter.energy+emitter.pulseAmount==1000*U,"No energy arrives before the pulse, and in-flight energy must be accounted for");
        h.runAfterDelay(30,()->{h.assertTrue(tank.energy==100*U&&emitter.energy==900*U&&emitter.pulseAmount==0,"A pulse must commit exactly once when it arrives");h.succeed();});
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=60)
    public static void obstructionCancelsWithoutDuplicatingOrLosingThePendingBalance(GameTestHelper h) {
        for(int x=0;x<=4;x++)h.setBlock(x,2,1,Blocks.AIR);
        var emitter=device(h,EnergyContent.Kind.EMITTER,0,2,1);var tank=device(h,EnergyContent.Kind.TANK,4,2,1);
        emitter.energy=1000*U;emitter.target=tank.getBlockPos();emitter.targetIdentity=tank.identity;
        boolean started=emitter.startPulse();h.assertTrue(started,"Pulse must start before a new obstacle appears: "+pulseProbe(emitter));emitter.outputPaused=true;
        h.setBlock(2,2,1,Blocks.STONE);
        h.runAfterDelay(20,()->{h.assertTrue(tank.energy==0&&emitter.energy==1000*U&&emitter.pulseAmount==0&&EnergyWorld.incoming(tank)==0,"Cancellation refunds once and frees the reservation");h.succeed();});
    }
    @GameTest(template="empty",batch="energy")
    public static void breakingReservoirLosesAetherButReturnsItsChargingContainer(GameTestHelper h) {
        var p=player(h);var tank=device(h,EnergyContent.Kind.TANK,1,1,1);tank.owner=p.getUUID();tank.energy=12345*U;
        tank.battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();((ManaStorage)tank.battery.getItem()).receive(tank.battery,789,false);
        var saved=tank.saveWithoutMetadata(h.getLevel().registryAccess());tank.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(tank.energy==12345*U,"Ordinary persistence must preserve energy");
        h.assertTrue(EnergyLinkItem.data(tank.asItem()).getCompound("AetherDevice").getLong("Energy")==0,"A broken reservoir must not retain aether in the dropped block item");
        var pos=tank.getBlockPos();h.getLevel().destroyBlock(pos,true);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(1.2));
        h.assertTrue(drops.stream().filter(e->e.getItem().getItem() instanceof ManaStorage s&&s.stored(e.getItem())==789).count()==1,"The charging item must drop exactly once with its own charge");h.succeed();
    }
    @GameTest(template="empty",batch="energy")
    public static void chargingNeverFillsCapacityReservedForSpellRefunds(GameTestHelper h) {
        var p=player(h);var stack=ModContent.MANA_CONTAINERS.get("mana_container_basic").get().filledStack();var storage=(ManaStorage)stack.getItem();
        long reserved=10000*U;storage.extractUnits(stack,reserved,false);var id=ManaEscrow.identity(stack);var cast=UUID.randomUUID();
        var sources=new LinkedHashMap<UUID,Long>();sources.put(id,reserved);var escrow=ManaEscrow.get(h.getLevel().getServer());escrow.open(cast,sources);escrow.claim(stack);
        p.getInventory().setItem(0,stack);
        h.assertTrue(EnergyWorld.charge(p,50000*U,false)==0,"The empty-looking capacity belongs to pending spell refunds");
        escrow.release(cast);escrow.claim(stack);
        h.assertTrue(storage.stored(stack)==100000&&cn.nulladev.modularmagic.item.ManaContainerItem.reserved(stack)==0,"Refund returns once without overflow");h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=40)
    public static void remoteReceiverChargesRealBatteriesAndDoesNotStack(GameTestHelper h) {
        var p=player(h);var anchor=device(h,EnergyContent.Kind.ANCHOR,1,1,1);anchor.owner=p.getUUID();anchor.energy=10000*U;
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(0,battery);
        p.getInventory().setItem(1,receiver(h,anchor));p.getInventory().setItem(2,receiver(h,anchor));
        h.runAfterDelay(5-h.getLevel().getGameTime()%5,()->{
            long before=anchor.energy;EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==250&&before-anchor.energy==275*U,"Two receivers still deliver only one 250-mana interval with a 10% source fee");h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=40)
    public static void foreignReceiverCannotDrawAnotherPlayersEnergy(GameTestHelper h) {
        var p=player(h);var anchor=device(h,EnergyContent.Kind.ANCHOR,1,1,1);anchor.owner=UUID.randomUUID();anchor.energy=10000*U;
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(0,battery);p.getInventory().setItem(1,receiver(h,anchor));
        h.runAfterDelay(5-h.getLevel().getGameTime()%5,()->{EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==0&&anchor.energy==10000*U,"Knowing coordinates and the device ID must not bypass ownership");h.succeed();});
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=40)
    public static void worldAnchorOwnsAndReleasesItsTickingTicket(GameTestHelper h) {
        var be=device(h,EnergyContent.Kind.WORLD_ANCHOR,1,1,1);be.owner=UUID.randomUUID();be.energy=200*U;be.loaderEnabled=true;
        EnergyWorld.tickLoader(be);h.assertTrue(be.appliedRadius==0,"Funded enabled loader must acquire its own ticking ticket");
        be.loaderEnabled=false;EnergyWorld.tickLoader(be);h.assertTrue(be.appliedRadius==-1,"Disabling the loader must release its own ticket");h.succeed();
    }
    @GameTest(template="empty",batch="energy")
    public static void expiredIngredientsCannotBeTakenFromAnAlreadyDisplayedCraftingResult(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());
        var menu=new net.minecraft.world.inventory.CraftingMenu(42,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(),pos));
        var shard=AetherShardItem.create(false,h.getLevel());
        ItemStack[] inputs={new ItemStack(Items.GLASS),new ItemStack(Items.GLASS),ItemStack.EMPTY,
                new ItemStack(Items.GLASS),shard,new ItemStack(Items.GLASS),ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
        for(int i=0;i<inputs.length;i++)menu.getSlot(i+1).set(inputs[i]);
        h.assertTrue(menu.getSlot(0).getItem().is(cn.nulladev.modularmagic.crafting.ArtificeContent.GLASS_ITEM.get()),"A live fragment must craft the actual reservoir recipe");
        var tag=EnergyLinkItem.data(shard);tag.putLong("AetherExpires",AetherShardItem.clock(h.getLevel()));EnergyLinkItem.data(shard,tag);
        menu.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&menu.getSlot(5).getItem().isEmpty()&&menu.getSlot(1).getItem().is(Items.GLASS),"A stale result must not be taken or consume the other materials after its fragment expires");h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=40)
    public static void crossDimensionSupplyRequiresTheInstalledUpgrade(GameTestHelper h) {
        var nether=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(nether,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CrossDimEnergy"));p.getInventory().clearContent();
        var anchor=device(h,EnergyContent.Kind.ANCHOR,1,1,1);anchor.owner=p.getUUID();anchor.energy=10000*U;
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(0,battery);p.getInventory().setItem(1,receiver(h,anchor));
        h.runAfterDelay(5-h.getLevel().getGameTime()%5,()->{
            EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==0,"Unupgraded anchors must not transmit across dimensions");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_EYE));anchor.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(!anchor.crossDimension,"An ordinary Eye of Ender must not bypass the End progression");
            p.setItemInHand(InteractionHand.MAIN_HAND,EnergyContent.CROSS_LENS.toStack());anchor.interact(p,InteractionHand.MAIN_HAND);
            p.setItemInHand(InteractionHand.MAIN_HAND,battery);EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==250&&anchor.energy==10000*U-250*U*125/100,"Cross-dimensional supply pays only the 25% fee and charges a real item");h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy")
    public static void noReservoirTierCanSupplyUnstableFragments(GameTestHelper h) {
        var p=player(h);
        for(var kind:java.util.List.of(EnergyContent.Kind.TANK,EnergyContent.Kind.EXPANDED_TANK,EnergyContent.Kind.ULTIMATE_TANK)) {
            var be=device(h,kind,1,1,1);be.owner=p.getUUID();be.energy=400*U;p.getInventory().clearContent();be.interact(p,InteractionHand.MAIN_HAND);
            p.setShiftKeyDown(true);be.interact(p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);
            h.assertTrue(p.getInventory().isEmpty()&&be.energy==400*U,"Reservoirs must only store and charge, including sneak-use: "+kind);
        }h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=60)
    public static void condenserAcceptsSuppliedAetherAndOnlyFormsFragmentsOnExtraction(GameTestHelper h) {
        var p=player(h);var tank=device(h,EnergyContent.Kind.TANK,1,1,1);var be=device(h,EnergyContent.Kind.CONDENSER,2,1,1);
        tank.owner=be.owner=p.getUUID();tank.energy=10000*U;
        h.runAfterDelay(25,()->{
            h.assertTrue(be.energy>=200*U&&be.product.isEmpty()&&!be.kind().disrupts(),"The condenser must receive from adjacent storage without water or pre-expiring output");
            long before=be.energy;be.interact(p,InteractionHand.MAIN_HAND);var shard=p.getInventory().getItem(0);
            h.assertTrue(shard.is(EnergyContent.SHARD.get())&&be.energy==before-200*U,"Empty-hand extraction must spend aether exactly once");
            h.assertTrue(AetherShardItem.remainingTicks(shard,h.getLevel())==EnergyConfig.SHARD_LIFE.get(),"The deadline starts at extraction, not receipt of aether");
            p.getInventory().clearContent();p.setShiftKeyDown(true);be.energy=4000*U;be.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(p.getInventory().getItem(0).getCount()==16&&be.energy==800*U,"Sneak-use must extract at most sixteen fragments and debit each one");
            p.setShiftKeyDown(false);h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=135)
    public static void heldFragmentCountdownUsesItsOriginalDeadline(GameTestHelper h) {
        var p=player(h);var stack=AetherShardItem.create(false,h.getLevel());p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        long start=AetherShardItem.remainingTicks(stack,h.getLevel());
        h.runAfterDelay(27,()->{
            h.assertTrue(AetherShardItem.remainingTicks(stack,h.getLevel())==start-27,"Held countdown must decrease by game ticks, not display the configured lifetime");
            h.assertTrue(AetherShardItem.remainingTicks(stack.copy(),h.getLevel())==start-27,"Copying must preserve the remaining time");
        });
        h.runAfterDelay(100,()->{
            h.assertTrue(AetherShardItem.remainingTicks(stack,h.getLevel())==0&&AetherShardItem.expired(stack,h.getLevel()),"Countdown must reach zero at its original deadline");
            stack.getItem().inventoryTick(stack,h.getLevel(),p,0,true);
            h.assertTrue(stack.isEmpty(),"The held fragment must actually disappear at zero");h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=65)
    public static void chamberCrystallizesIntoItsOutputAndHoppersCanCollectIt(GameTestHelper h) {
        h.setBlock(1,0,1,Blocks.HOPPER);var be=device(h,EnergyContent.Kind.CRYSTAL_CHAMBER,1,1,1);be.energy=1600*U;
        h.runAfterDelay(45,()->{
            var hopper=(net.minecraft.world.level.block.entity.HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,0,1)));
            int count=be.product.getCount();for(int i=0;i<hopper.getContainerSize();i++)if(hopper.getItem(i).is(EnergyContent.CRYSTAL.get()))count+=hopper.getItem(i).getCount();
            h.assertTrue(be.energy==0&&count==4,"1600 aether must become exactly four stable fragments, including hopper output");
            h.assertTrue(hopper.getItem(0).is(EnergyContent.CRYSTAL.get()),"The chamber must expose its real output to hoppers");h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy")
    public static void chamberDoesNotActAsAChargingReservoir(GameTestHelper h) {
        var p=player(h);var be=device(h,EnergyContent.Kind.CRYSTAL_CHAMBER,1,1,1);be.owner=p.getUUID();be.energy=1000*U;
        p.setItemInHand(InteractionHand.MAIN_HAND,ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack());be.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(be.battery.isEmpty()&&!p.getMainHandItem().isEmpty(),"Charging containers belong in liquid reservoirs, not beside a crystal nucleus");h.succeed();
    }
    @GameTest(template="empty",batch="energy")
    public static void emitterTiersHaveDistinctRangeAndCanAimInThreeDimensions(GameTestHelper h) {
        int range=0,shot=0;
        for(var kind:java.util.List.of(EnergyContent.Kind.EMITTER,EnergyContent.Kind.EXPANDED_EMITTER,EnergyContent.Kind.ULTIMATE_EMITTER)) {
            var be=device(h,kind,1,1,1);h.assertTrue(be.shotRange()>range&&be.shotSize()>shot,"Higher emitter tiers must improve range and payload");range=be.shotRange();shot=be.shotSize();
            be.target=be.getBlockPos().offset(3,4,-2);var expected=net.minecraft.world.phys.Vec3.atCenterOf(be.target).subtract(be.emissionPivot()).normalize();
            h.assertTrue(be.emissionDirection().dot(expected)>.999999&&be.emissionDirection().y>0,"The rendered head direction must follow the target in yaw and pitch");
        }h.succeed();
    }
    @GameTest(template="empty",batch="energy",timeoutTicks=40)
    public static void tunerResonancePairingMakesARealWirelessConnection(GameTestHelper h) {
        var p=player(h);var source=device(h,EnergyContent.Kind.RESONATOR,0,2,1);var tank=device(h,EnergyContent.Kind.TANK,4,2,1);
        source.owner=tank.owner=p.getUUID();source.channel=7;source.energy=1000*U;source.resonanceUpgrades=1;
        p.setItemInHand(InteractionHand.MAIN_HAND,EnergyContent.LINK.toStack());
        for(var be:java.util.List.of(source,tank))EnergyContent.LINK.get().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()),Direction.UP,be.getBlockPos(),false)));
        h.assertTrue(tank.channel==7,"Selecting resonator then receiver must tune the receiver without hidden channel editing");
        h.runAfterDelay(15,()->{h.assertTrue(tank.energy>0&&tank.energy<=100*U&&source.energy+tank.energy==1000*U,"The documented pairing must deliver within the low initial shared bandwidth");h.succeed();});
    }
    @GameTest(template="empty",batch="energy")
    public static void equipmentTiersRequireGoldDiamondsAndNetheriteSmithing(GameTestHelper h) {
        var recipes=h.getLevel().getRecipeManager();
        for(var pair:java.util.List.of(new EnergyContent.Kind[]{EnergyContent.Kind.EXPANDED_TANK,EnergyContent.Kind.ULTIMATE_TANK},
                new EnergyContent.Kind[]{EnergyContent.Kind.EXPANDED_EMITTER,EnergyContent.Kind.ULTIMATE_EMITTER})) {
            var expanded=recipes.byKey(ModularMagic.id(pair[0].id)).orElseThrow().value();
            h.assertTrue(expanded.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.GOLD_INGOT)))&&expanded.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.DIAMOND))),"Expanded devices must have gold and diamonds in their crafting recipe");
            var upgrade=recipes.byKey(ModularMagic.id(pair[1].id)).orElseThrow().value();
            h.assertTrue(upgrade instanceof net.minecraft.world.item.crafting.SmithingRecipe,"Netherite devices must use a smithing recipe");
            var input=new net.minecraft.world.item.crafting.SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),EnergyContent.ITEMS.get(pair[0]).toStack(),new ItemStack(Items.NETHERITE_INGOT));
            var smithing=(net.minecraft.world.item.crafting.SmithingRecipe)upgrade;
            h.assertTrue(smithing.matches(input,h.getLevel())&&smithing.assemble(input,h.getLevel().registryAccess()).is(EnergyContent.ITEMS.get(pair[1]).get()),"The actual smithing template, base device and ingot must produce the right tier");
        }h.succeed();
    }
    @GameTest(template="empty",batch="energy_config")
    public static void customFragmentValuesFeesCapacitiesAndTooltipsDriveRealTransactions(GameTestHelper h) {
        long shardValue=EnergyConfig.SHARD.get(),fee=EnergyConfig.CRYSTAL_FEE.get(),capacity=EnergyConfig.BASIC_CAPACITY.get(),rate=EnergyConfig.BASIC_RATE.get();
        int interval=EnergyConfig.CRYSTAL_INTERVAL.get(),batch=EnergyConfig.CRYSTAL_BATCH.get();
        try {
            EnergyConfig.SHARD.set(333L);EnergyConfig.CRYSTAL_FEE.set(77L);EnergyConfig.BASIC_CAPACITY.set(20000L);EnergyConfig.BASIC_RATE.set(777L);
            EnergyConfig.CRYSTAL_INTERVAL.set(1);EnergyConfig.CRYSTAL_BATCH.set(2);
            var p=player(h);var tank=device(h,EnergyContent.Kind.TANK,0,1,1);var condenser=device(h,EnergyContent.Kind.CONDENSER,2,1,1);
            tank.owner=condenser.owner=p.getUUID();condenser.energy=666*U;
            h.assertTrue(tank.capacity()==20000*U&&tank.rate()==777,"Storage capacity and bandwidth must follow the live config");
            condenser.interact(p,InteractionHand.MAIN_HAND);var unstable=p.getMainHandItem();
            h.assertTrue(AetherShardItem.value(unstable)==333&&condenser.energy==333*U,"Extraction must use the configured contents");
            var lines=new ArrayList<net.minecraft.network.chat.Component>();unstable.getItem().appendHoverText(unstable,Item.TooltipContext.of(h.getLevel()),lines,TooltipFlag.NORMAL);
            h.assertTrue(lines.getFirst().getString().contains("333")&&lines.size()==2,"Unstable tooltip must show contents before its countdown");
            tank.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(unstable.isEmpty()&&tank.energy==333*U,"A live fragment must return its stored aether to a reservoir");
            var chamber=device(h,EnergyContent.Kind.CRYSTAL_CHAMBER,4,1,1);chamber.owner=p.getUUID();chamber.energy=820*U;chamber.tick();
            h.assertTrue(chamber.energy==0&&chamber.product.getCount()==2&&AetherShardItem.value(chamber.product)==333,"Each crystal must cost contents plus the separately configured fee");
            chamber.interact(p,InteractionHand.MAIN_HAND);var crystal=p.getMainHandItem();lines.clear();crystal.getItem().appendHoverText(crystal,Item.TooltipContext.of(h.getLevel()),lines,TooltipFlag.NORMAL);
            h.assertTrue(lines.size()==1&&lines.getFirst().getString().contains("333"),"Stable tooltip must show contents without processing fees");
            tank.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(tank.energy==666*U&&crystal.isEmpty(),"Crystal deposits must not return the crystallization fee");
            var expired=AetherShardItem.create(false,h.getLevel());var data=EnergyLinkItem.data(expired);data.putLong("AetherExpires",AetherShardItem.clock(h.getLevel()));EnergyLinkItem.data(expired,data);
            p.setItemInHand(InteractionHand.MAIN_HAND,expired);tank.interact(p,InteractionHand.MAIN_HAND);
            h.assertTrue(tank.energy==666*U&&expired.isEmpty(),"Expired material cannot be redeemed");
            h.succeed();
        } finally {
            EnergyConfig.SHARD.set(shardValue);EnergyConfig.CRYSTAL_FEE.set(fee);EnergyConfig.BASIC_CAPACITY.set(capacity);EnergyConfig.BASIC_RATE.set(rate);
            EnergyConfig.CRYSTAL_INTERVAL.set(interval);EnergyConfig.CRYSTAL_BATCH.set(batch);
        }
    }
    @GameTest(template="empty",batch="energy_config")
    public static void resonanceCrystalsInstallPersistAndRespectLossySharedBandwidth(GameTestHelper h) {
        var p=player(h);var resonator=device(h,EnergyContent.Kind.RESONATOR,0,1,1);var sink=device(h,EnergyContent.Kind.TANK,4,1,1);
        resonator.owner=sink.owner=p.getUUID();resonator.energy=10000*U;
        var empty=cn.nulladev.modularmagic.compat.jade.AetherJadeData.device(resonator);resonator.tick();
        h.assertTrue(empty.getLong("Bandwidth")==0&&empty.getInt("Range")==0&&empty.getString("Status").equals("amethyst_required")&&!resonator.getBlockState().getValue(EnergyBlock.ACTIVE),"A crafted shell must be inactive and ask for amethyst");
        h.assertTrue(EnergyWorld.transfer(resonator,sink,Long.MAX_VALUE)==0&&resonator.energy==10000*U,"Even a shell with stored aether must not supply devices");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.AMETHYST_SHARD,8));resonator.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(resonator.resonanceUpgrades==1&&resonator.resonanceBandwidth()==100&&resonator.resonanceRange()==36&&resonator.resonanceLosslessDistance()==18,"The first shard must activate 100/s and the configured first-shard coverage");
        p.setShiftKeyDown(true);resonator.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(resonator.resonanceUpgrades==8&&p.getMainHandItem().isEmpty(),"Sneak-use must install a stack within the configured limit");
        var saved=resonator.saveWithoutMetadata(h.getLevel().registryAccess());resonator.resonanceUpgrades=0;resonator.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(resonator.resonanceUpgrades==8,"Installed shards must persist across save/reload");
        h.assertTrue(ResonanceNetwork.bind(resonator,sink)==null,"Workshop devices must be explicitly attached to their upstream resonator");
        long before=resonator.energy;
        double efficiency=resonator.resonanceEfficiency(resonator.resonanceLosslessDistance()+4);long delivered=EnergyWorld.transfer(resonator,sink,Long.MAX_VALUE,efficiency);
        h.assertTrue(Math.abs(efficiency-.8)<1e-9&&delivered>0&&before-resonator.energy>delivered,"Distance losses must consume real aether, independently of the added bandwidth");
        h.assertTrue(EnergyWorld.transfer(resonator,sink,Long.MAX_VALUE,efficiency)==0,"Multiple receivers must share the resonator output quota");
        resonator.interact(p,InteractionHand.MAIN_HAND);h.assertTrue(resonator.resonanceUpgrades==0&&p.getMainHandItem().is(Items.AMETHYST_SHARD)&&p.getMainHandItem().getCount()==8,"Sneak-empty-hand use must retrieve the installed crystals");
        h.assertTrue(resonator.resonanceBandwidth()==0&&resonator.resonanceLosslessDistance()==0&&resonator.resonanceRange()==0&&EnergyWorld.transfer(resonator,sink,Long.MAX_VALUE)==0,"Removing all crystals must deactivate the shell again");
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.AMETHYST_SHARD,3));
        resonator.interact(p,InteractionHand.MAIN_HAND);
        h.assertTrue(resonator.resonanceUpgrades==3&&p.getMainHandItem().getCount()==3,"Installing shards in creative leaves the hand stack intact");
        resonator.resonanceUpgrades=64;var snapshot=cn.nulladev.modularmagic.compat.jade.AetherJadeData.device(resonator);
        h.assertTrue(resonator.resonanceBandwidth()==6400&&snapshot.getLong("Bandwidth")==6400&&snapshot.getInt("Range")==288&&snapshot.getDouble("LosslessDistance")==144,"A full stack must produce exactly 6.4K/s, 144 lossless blocks and 288 maximum blocks in Jade");h.succeed();
    }
    @GameTest(template="empty",batch="energy_config",timeoutTicks=35)
    public static void resonanceCrystalsActuallyExtendPortableCoverage(GameTestHelper h) {
        var p=player(h);var resonator=device(h,EnergyContent.Kind.RESONATOR,1,1,1);resonator.owner=p.getUUID();resonator.energy=10000*U;
        resonator.resonanceUpgrades=1;
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(0,battery);p.getInventory().setItem(1,receiver(h,resonator));
        p.setPos(resonator.getBlockPos().getCenter().add(40,0,0));
        h.runAfterDelay(5-h.getLevel().getGameTime()%5,()->{
            EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)==0,"One shard must not reach forty blocks");
            resonator.resonanceUpgrades=8;
            EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).stored(battery)>0&&resonator.energy<10000*U,"Installing crystals must extend the actual portable charging range, not only the displayed range");h.succeed();
        });
    }
    @GameTest(template="empty",batch="energy_config",timeoutTicks=35)
    public static void configuredPulseSpeedAndLossAreSnapshottedUntilArrival(GameTestHelper h) {
        var oldFormula=EnergyConfig.PULSE_LOSS.get();double oldFree=EnergyConfig.PULSE_FREE_DISTANCE.get(),oldSpeed=EnergyConfig.PULSE_SPEED.get();
        var emitter=device(h,EnergyContent.Kind.EMITTER,0,2,1);var tank=device(h,EnergyContent.Kind.TANK,4,2,1);
        for(int x=1;x<4;x++)h.setBlock(x,2,1,Blocks.AIR);
        try {
            EnergyConfig.PULSE_LOSS.set("0.25");EnergyConfig.PULSE_FREE_DISTANCE.set(0D);EnergyConfig.PULSE_SPEED.set(10D);
            emitter.energy=1000*U;emitter.target=tank.getBlockPos();emitter.targetIdentity=tank.identity;
            boolean launched=emitter.startPulse();
            h.assertTrue(launched&&emitter.pulseDuration==1&&emitter.pulseDelivered==75*U,"Configured pulse: launched="+launched+", duration="+emitter.pulseDuration+", delivered="+emitter.pulseDelivered+", "+pulseProbe(emitter));emitter.outputPaused=true;
        } finally {EnergyConfig.PULSE_LOSS.set(oldFormula);EnergyConfig.PULSE_FREE_DISTANCE.set(oldFree);EnergyConfig.PULSE_SPEED.set(oldSpeed);}
        h.runAfterDelay(5,()->{h.assertTrue(tank.energy==75*U&&emitter.energy==900*U,"Only the expected delivery should arrive; changing defaults must not reprice an in-flight pulse");h.succeed();});
    }
    @GameTest(template="empty",batch="energy_config")
    public static void fourTiersHaveMatchingRaritiesAndCreativeSourcesNeedNoRecipes(GameTestHelper h) {
        var rarities=new Rarity[]{Rarity.COMMON,Rarity.UNCOMMON,Rarity.RARE,Rarity.EPIC};
        var tanks=new EnergyContent.Kind[]{EnergyContent.Kind.TANK,EnergyContent.Kind.EXPANDED_TANK,EnergyContent.Kind.ULTIMATE_TANK,EnergyContent.Kind.CREATIVE_TANK};
        var emitters=new EnergyContent.Kind[]{EnergyContent.Kind.EMITTER,EnergyContent.Kind.EXPANDED_EMITTER,EnergyContent.Kind.ULTIMATE_EMITTER,EnergyContent.Kind.CREATIVE_EMITTER};
        String[] batteries={"mana_container_basic","mana_container_expanded","mana_container_ultimate","creative_mana_container"};
        for(int i=0;i<4;i++)h.assertTrue(EnergyContent.ITEMS.get(tanks[i]).toStack().getRarity()==rarities[i]&&EnergyContent.ITEMS.get(emitters[i]).toStack().getRarity()==rarities[i]&&ModContent.MANA_CONTAINERS.get(batteries[i]).toStack().getRarity()==rarities[i],"Tier rarity must match across reservoirs, emitters and containers");
        var recipes=h.getLevel().getRecipeManager().getRecipes();
        for(var recipe:recipes){var result=recipe.value().getResultItem(h.getLevel().registryAccess());h.assertTrue(!result.is(EnergyContent.ITEMS.get(tanks[3]).get())&&!result.is(EnergyContent.ITEMS.get(emitters[3]).get())&&!result.is(ModContent.MANA_CONTAINERS.get(batteries[3]).get()),"Creative equipment must have no crafting or smithing recipe");}
        var source=device(h,EnergyContent.Kind.CREATIVE_TANK,0,1,1);var sink=device(h,EnergyContent.Kind.TANK,2,1,1);
        h.assertTrue(EnergyWorld.transfer(source,sink,100*U)==100*U&&source.energy==0,"Creative reservoirs must supply energy without a finite balance");h.succeed();
    }
    @GameTest(template="empty",batch="energy_tree",timeoutTicks=65)
    public static void connectedTreeRelaysToABoundPlayerAndDisconnectsCleanly(GameTestHelper h) {
        var p=player(h);var root=device(h,EnergyContent.Kind.RESONATOR,0,1,1);var branch=device(h,EnergyContent.Kind.RESONATOR,2,1,1);var leaf=device(h,EnergyContent.Kind.RESONATOR,4,1,1);
        for(var node:List.of(root,branch,leaf)){node.owner=p.getUUID();node.resonanceUpgrades=1;}root.energy=10000*U;
        h.assertTrue(ResonanceNetwork.bind(root,branch)==null&&ResonanceNetwork.bind(branch,leaf)==null,"A root must accept a child and grandchild");
        h.assertTrue("network_cycle".equals(ResonanceNetwork.bind(leaf,root))&&root.resonanceParent==null,"The binding tool must reject a loop without changing the old tree");
        var receiver=EnergyContent.RECEIVER.toStack();p.setItemInHand(InteractionHand.MAIN_HAND,receiver);
        EnergyContent.RECEIVER.get().useOn(new net.minecraft.world.item.context.UseOnContext(p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(root.getBlockPos().getCenter(),Direction.UP,root.getBlockPos(),false)));
        h.assertTrue(EnergyLinkItem.data(receiver).getUUID("Network").equals(root.identity),"A receiver must bind to the whole tree");
        var battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();p.getInventory().setItem(0,battery);p.getInventory().setItem(1,receiver);p.setPos(root.getBlockPos().getCenter().add(40,0,0));
        h.runAfterDelay(15-h.getLevel().getGameTime()%5,()->{
            EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)>0,"Only the connected leaf reaches the player; the tree must forward real aether to it");
            p.setPos(root.getBlockPos().getCenter());root.energy=10000*U;root.extract(Long.MAX_VALUE,false);branch.extract(Long.MAX_VALUE,false);
            long overlappingBefore=((ManaStorage)battery.getItem()).storedUnits(battery);
            EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)>overlappingBefore,"Overlapping coverage must choose remaining usable bandwidth rather than the fullest exhausted node");
            p.setPos(root.getBlockPos().getCenter().add(40,0,0));
            ResonanceNetwork.detach(branch);leaf.energy=1000*U;long before=((ManaStorage)battery.getItem()).storedUnits(battery);
            h.runAfterDelay(5,()->{
                EnergyWorld.playerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
                h.assertTrue(((ManaStorage)battery.getItem()).storedUnits(battery)==before,"A detached branch must no longer answer a receiver bound to the original root");h.succeed();
            });
        });
    }
    @GameTest(template="empty",batch="energy_tree")
    public static void treeHasSingleParentPersistsAndNeverLoadsMissingAncestors(GameTestHelper h) {
        var p=player(h);var root=device(h,EnergyContent.Kind.RESONATOR,0,1,1);var branch=device(h,EnergyContent.Kind.RESONATOR,2,1,1);var leaf=device(h,EnergyContent.Kind.RESONATOR,4,1,1);
        for(var node:List.of(root,branch,leaf)){node.owner=p.getUUID();node.resonanceUpgrades=1;}
        h.assertTrue(ResonanceNetwork.bind(root,branch)==null&&ResonanceNetwork.bind(root,leaf)==null&&ResonanceNetwork.bind(branch,leaf)==null,"A node may reparent into a branch");
        h.assertTrue(!ResonanceNetwork.accepts(root,leaf)&&ResonanceNetwork.accepts(branch,leaf),"A child must accept exactly its selected parent");
        var tag=leaf.saveWithoutMetadata(h.getLevel().registryAccess());leaf.resonanceParent=null;leaf.loadWithComponents(tag,h.getLevel().registryAccess());
        h.assertTrue(ResonanceNetwork.root(leaf).equals(root.identity),"The parent path must survive save/reload");
        var tank=device(h,EnergyContent.Kind.TANK,1,1,4);var storage=device(h,EnergyContent.Kind.TANK,2,1,4);tank.owner=storage.owner=p.getUUID();storage.energy=1000*U;
        h.assertTrue(ResonanceNetwork.bind(root,tank)==null&&EnergyWorld.transfer(storage,tank,100*U)==100*U,"A bound workshop device must still accept its ordinary wired supply");
        UUID old=root.identity;root.identity=UUID.randomUUID();h.assertTrue(ResonanceNetwork.root(leaf)==null,"Replacing an ancestor must invalidate its old links");root.identity=old;
        var far=root.getBlockPos().offset(1000000,0,0);branch.resonanceParent=far;branch.resonanceParentIdentity=UUID.randomUUID();
        h.assertTrue(!h.getLevel().hasChunkAt(far)&&ResonanceNetwork.root(leaf)==null&&!h.getLevel().hasChunkAt(far),"Resolving a missing ancestor must not wake its chunk");h.succeed();
    }
    @GameTest(template="empty",batch="energy_tree")
    public static void remoteSupplyProgressionNeedsCrystalsAndTheEnd(GameTestHelper h) {
        var recipes=h.getLevel().getRecipeManager();
        var anchor=recipes.byKey(ModularMagic.id("personal_aether_anchor")).orElseThrow().value();
        h.assertTrue(anchor.getIngredients().stream().anyMatch(i->i.test(EnergyContent.CRYSTAL.toStack()))&&!anchor.getIngredients().stream().anyMatch(i->i.test(EnergyContent.SHARD.toStack())),"Personal supply must require stable crystallized material");
        var lens=recipes.byKey(ModularMagic.id("interdimensional_lens")).orElseThrow().value();
        h.assertTrue(lens.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.DRAGON_BREATH)))&&lens.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.POPPED_CHORUS_FRUIT))),"Cross-world supply must require End materials");h.succeed();
    }
}
