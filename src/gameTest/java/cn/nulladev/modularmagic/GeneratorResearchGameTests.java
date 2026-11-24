package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.generator.*;
import cn.nulladev.modularmagic.research.*;
import cn.nulladev.modularmagic.spell.*;
import cn.nulladev.modularmagic.mana.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.*;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GeneratorResearchGameTests{
    private static final long U=ManaStorage.SCALE;
    private static EnergyBlockEntity machine(GameTestHelper h,String id){var p=h.absolutePos(new BlockPos(2,2,2));h.getLevel().removeBlock(p,false);h.getLevel().setBlockAndUpdate(p,EnergyContent.BLOCKS.get(EnergyContent.Kind.GENERATOR).get().defaultBlockState());var b=(EnergyBlockEntity)h.getLevel().getBlockEntity(p);b.generator.ruleId=id;b.generator.cooldown=0;EnergyWorld.add(h.getLevel(),p);return b;}
    private static ItemEntity drop(GameTestHelper h,EnergyBlockEntity b,ItemStack stack){var p=b.getBlockPos().above().getCenter();var e=new ItemEntity(h.getLevel(),p.x,p.y,p.z,stack);e.setNoGravity(true);h.getLevel().addFreshEntity(e);return e;}
    private static ServerPlayer player(GameTestHelper h){var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RuneTester"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.setPos(h.absolutePos(new BlockPos(2,3,2)).getCenter());return p;}
    @GameTest(template="empty",batch="generators") public static void railsConsumeOnlyWhatFitsAndHaveOrdinaryPlacementFee(GameTestHelper h){
        var b=machine(h,"rails");long value=b.generator.rule().yield(0);b.energy=b.capacity()-2*value*U;var rails=drop(h,b,new ItemStack(Items.POWERED_RAIL,5));b.generator.tick();h.assertTrue(rails.getItem().getCount()==3&&b.energy==b.capacity(),"Only two real rails fit; remaining three must survive");h.assertTrue(cn.nulladev.modularmagic.spell.billing.CostRules.evaluate(cn.nulladev.modularmagic.spell.billing.CostRules.snapshot().get("action/set_block"),Map.of())==200,"Rails use the ordinary configured placement fee");h.succeed();}
    @GameTest(template="empty",batch="generators") public static void fullAndPausedMachinesNeverEatInputs(GameTestHelper h){var b=machine(h,"water");var p=b.getBlockPos().east();h.getLevel().setBlockAndUpdate(p,Blocks.WATER.defaultBlockState());b.energy=b.capacity();b.generator.tick();h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.WATER)&&b.generator.signal==15,"Full machine must leave water intact");b.energy=0;b.outputPaused=true;b.generator.tick();h.assertTrue(h.getLevel().getBlockState(p).is(Blocks.WATER)&&b.generator.signal==0,"Paused machine leaves water intact");b.outputPaused=false;b.generator.tick();h.assertTrue(h.getLevel().getBlockState(p).isAir()&&b.energy==100*U,"Water input is removed once and yields configured aether");h.succeed();}
    @GameTest(template="empty",batch="generators") public static void snowUsesItsChunkAndRequiresFullFirstCooldown(GameTestHelper h){var b=machine(h,"snow");b.generator.placed();var box=b.generator.bounds();var chunk=new net.minecraft.world.level.ChunkPos(b.getBlockPos());h.assertTrue(box.minX==chunk.getMinBlockX()&&box.maxX==chunk.getMaxBlockX()+1&&box.minZ==chunk.getMinBlockZ()&&box.maxZ==chunk.getMaxBlockZ()+1,"Snow field is exactly its chunk, including negative chunk coordinates");b.generator.tick();h.assertTrue(b.energy==0&&b.generator.cooldown==b.generator.rule().period.get()-1,"Placement must wait the complete snow cycle");var saved=b.generator.save();b.generator.load(saved);h.assertTrue(b.generator.cooldown==b.generator.rule().period.get()-1,"Saving must not reset cooldown");h.succeed();}
    @GameTest(template="empty",batch="generators") public static void chorusReallyReturnsToItsGrowingState(GameTestHelper h){var b=machine(h,"chorus");var p=b.getBlockPos().east().above();h.getLevel().setBlock(p.below(),Blocks.END_STONE.defaultBlockState(),2);h.getLevel().setBlock(p,Blocks.CHORUS_FLOWER.defaultBlockState().setValue(ChorusFlowerBlock.AGE,5),2);b.generator.tick();h.assertTrue(h.getLevel().getBlockState(p).getValue(ChorusFlowerBlock.AGE)==0&&b.energy==1600*U,"Dead flowers become young flowers");h.succeed();}
    @GameTest(template="empty",batch="generators") public static void tntEventCapturesBeforeExplosionButCannotSkipCooldown(GameTestHelper h){var b=machine(h,"tnt");var p=b.getBlockPos().above().getCenter();var t=new PrimedTnt(h.getLevel(),p.x,p.y,p.z,null);t.setFuse(1);h.getLevel().addFreshEntity(t);h.runAfterDelay(2,()->{h.assertTrue(t.isRemoved()&&b.energy==8000*U&&!b.isRemoved(),"Even a last-tick TNT is intercepted before explosion");var next=new PrimedTnt(h.getLevel(),p.x,p.y,p.z,null);next.setFuse(40);h.assertTrue(!b.generator.captureTnt(next)&&!next.isRemoved(),"TNT continues its fuse during the 10-second cooldown");b.generator.cooldown=0;b.energy=b.capacity();h.assertTrue(!b.generator.captureTnt(next),"A full cache cannot disarm TNT");b.energy=0;b.outputPaused=true;h.assertTrue(!b.generator.captureTnt(next),"Paused generators cannot act as free TNT disposal");h.succeed();});}
    @GameTest(template="empty",batch="generators") public static void cureFailsWithoutReplacingOrReleasingTheZombie(GameTestHelper h){
        var b=machine(h,"curing");var p=b.getBlockPos().above().getCenter();var z=new ZombieVillager(EntityType.ZOMBIE_VILLAGER,h.getLevel());z.setPos(p);z.setNoAi(true);z.setInvulnerable(true);z.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1200));h.getLevel().addFreshEntity(z);var id=z.getUUID();var apple=drop(h,b,new ItemStack(Items.GOLDEN_APPLE));
        b.generator.tick();h.assertTrue(!z.isConverting()&&!z.hasEffect(MobEffects.WEAKNESS)&&apple.isRemoved()&&b.generator.reserved==1200000*U,"The device consumes apple and weakness without starting vanilla conversion");
        b.outputPaused=true;int before=b.generator.cureRemaining;b.generator.tick();h.assertTrue(b.generator.cureRemaining==before,"Redstone pauses the device's own timer");
        var saved=b.generator.save();b.generator.cureRemaining=0;b.generator.load(saved);h.assertTrue(b.generator.cureRemaining==before&&b.generator.reserved==1200000*U,"The entire pending cycle survives saving");
        b.outputPaused=false;b.generator.cureRemaining=1;b.generator.tick();h.assertTrue(!z.isConverting()&&z.isAlive()&&id.equals(z.getUUID())&&b.energy==1200000*U&&b.generator.reserved==0,"Completion keeps the exact zombie and produces energy once");
        b.generator.retry=0;b.generator.cooldown=0;var spare=drop(h,b,new ItemStack(Items.GOLDEN_APPLE));b.generator.tick();h.assertTrue(!spare.isRemoved()&&b.energy==1200000*U,"A new cycle needs fresh weakness");h.succeed();
    }
    @GameTest(template="empty",batch="generators") public static void ordinaryCuringIsUnaffected(GameTestHelper h){
        var z=new ZombieVillager(EntityType.ZOMBIE_VILLAGER,h.getLevel());z.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());z.setNoAi(true);z.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1200));h.getLevel().addFreshEntity(z);var p=player(h);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE));z.interact(p,net.minecraft.world.InteractionHand.MAIN_HAND);h.assertTrue(z.isConverting()&&p.getMainHandItem().isEmpty(),"A player's ordinary golden apple still starts vanilla curing");h.succeed();
    }
    @GameTest(template="empty",batch="generators") public static void brokenCuringDeviceReleasesItsClaimWithoutStartingConversion(GameTestHelper h){
        var b=machine(h,"curing");var z=new ZombieVillager(EntityType.ZOMBIE_VILLAGER,h.getLevel());z.setPos(b.getBlockPos().above().getCenter());z.setNoAi(true);z.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,1200));h.getLevel().addFreshEntity(z);drop(h,b,new ItemStack(Items.GOLDEN_APPLE));b.generator.tick();h.getLevel().removeBlock(b.getBlockPos(),false);h.assertTrue(z.isAlive()&&!z.isConverting()&&!z.getPersistentData().contains("ModularMagicCure"),"Breaking the device only discards its pending cycle, with no vanilla cure left running");h.succeed();
    }
    @GameTest(template="empty",batch="generators")
    public static void comparatorsFollowRemainingCooldown(GameTestHelper h) {
        for (String id : GeneratorCatalog.RULES.keySet()) {
            var b = machine(h, id);
            var state = b.getBlockState();
            int period = b.generator.rule().period.get();
            b.generator.cooldown = period;
            h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), b.getBlockPos()) == (period == 0 ? 0 : 15),
                    "Full cooldown must output strength 15 when enabled: " + id);
            if (period >= 15) {
                b.generator.cooldown = period / 2;
                h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), b.getBlockPos()) == 8,
                        "Half the cooldown must round up to strength 8: " + id);
                b.generator.cooldown = 1;
                h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), b.getBlockPos()) == 1,
                        "One remaining tick must output strength 1: " + id);
            }
            b.generator.cooldown = 0;
            b.energy = b.capacity();
            b.generator.signal = 15;
            h.assertTrue(state.getAnalogOutputSignal(h.getLevel(), b.getBlockPos()) == 0,
                    "A finished cooldown must output strength 0 even with a full buffer: " + id);
        }
        h.succeed();
    }
    @GameTest(template="empty",batch="generators") public static void everyNodeHasFiniteStructurallyValidResearchAndKnowledgeMapping(GameTestHelper h){for(var t:NodeCatalog.all()){var d=ResearchCatalog.reference(t.id());h.assertTrue(d.structuralErrors().isEmpty(),"Invalid research structure: "+t.id()+" "+d.structuralErrors());h.assertTrue(d.nodes().stream().anyMatch(n->n.template.equals(t.id())),"Missing research target "+t.id());h.assertTrue(ResearchCatalog.all().stream().anyMatch(k->k.nodes().contains(t.id())),"Unmapped node "+t.id());var copy=SpellDocument.fromJson(d.toJson());h.assertTrue(ResearchCatalog.equivalent(copy.root,d.root),"Round-trip changed the research");}h.succeed();}
    @GameTest(template="empty",batch="generators") public static void puzzleInkIsPaidOnceAndVirtualPiecesCannotBeMultiplied(GameTestHelper h){var p=player(h);var pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(pos,ResearchContent.TABLE.get().defaultBlockState());ResearchCatalog.get("K00").nodes().forEach(n->ResearchKnowledge.learn(p,n));ResearchKnowledge.save(p,"observed:K01","yes");ResearchKnowledge.save(p,"active","K01");var m=new ResearchMenu(3,p.getInventory(),pos,false);m.supplies.setItem(0,ResearchNoteItem.create("K01",false));m.supplies.setItem(1,ResearchContent.INK.toStack());int cost=m.inkCost;m.apply(m.revision,"{\"op\":\"start\"}");h.assertTrue(m.paid&&m.supplies.getItem(1).getDamageValue()==cost,"One ink payment starts the course");m.removed(p);var reopened=new ResearchMenu(4,p.getInventory(),pos,false);h.assertTrue(!reopened.paid&&reopened.document==null&&m.supplies.getItem(1).getDamageValue()==cost,"An unfinished attempt clears progress and keeps its spent ink spent");h.assertTrue(reopened.slots.size()==46&&reopened.getCarried().isEmpty(),"Supplies and backpack slots must stay separate from virtual research pieces");h.succeed();}
    @GameTest(template="empty",batch="generators") public static void knowledgeAddsFocusWithoutGivingFreeKnowledgeOnDeath(GameTestHelper h){var p=player(h);double before=PlayerFocus.maximum(p);ResearchCatalog.get("K01").nodes().forEach(n->ResearchKnowledge.learn(p,n));h.assertTrue(PlayerFocus.maximum(p)==before+.5,"A complete knowledge group adds its configured focus bonus");h.assertTrue(!ResearchKnowledge.knows(p,"effect/damage"),"Research does not grant unrelated pieces");h.succeed();}
}
