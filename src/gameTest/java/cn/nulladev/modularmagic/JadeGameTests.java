package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.compat.jade.AetherJadeData;
import cn.nulladev.modularmagic.energy.*;
import cn.nulladev.modularmagic.item.ManaContainerItem;
import cn.nulladev.modularmagic.mana.ManaStorage;
import cn.nulladev.modularmagic.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JadeGameTests {
    private static EnergyBlockEntity device(GameTestHelper h,EnergyContent.Kind kind,int x,int z) {
        var pos=h.absolutePos(new BlockPos(x,2,z));
        h.getLevel().setBlockAndUpdate(pos,EnergyContent.BLOCKS.get(kind).get().defaultBlockState());
        return (EnergyBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="empty",batch="jade")
    public static void jadeSnapshotsDoNotConsumeEnergyOrChangeRefundReservations(GameTestHelper h) {
        var be=device(h,EnergyContent.Kind.TANK,1,1);be.energy=1234*ManaStorage.SCALE;
        be.battery=ModContent.MANA_CONTAINERS.get("mana_container_basic").toStack();
        ((ManaStorage)be.battery.getItem()).receive(be.battery,321,false);ManaContainerItem.reserved(be.battery,900*ManaStorage.SCALE);
        var before=be.battery.copy();var snapshot=AetherJadeData.device(be);
        h.assertTrue(snapshot.getLong("Stored")==1234&&snapshot.getLong("Capacity")==1000000,"Jade must use displayed units, not internal fixed point");
        h.assertTrue(snapshot.getCompound("Battery").getLong("Stored")==321&&snapshot.getCompound("Battery").getLong("Reserved")==900,"Charging item and reservation must be reported separately");
        h.assertTrue(be.energy==1234*ManaStorage.SCALE&&ItemStack.matches(before,be.battery),"Looking at a machine cannot claim refunds or mutate energy");h.succeed();
    }
    @GameTest(template="empty",batch="jade")
    public static void jadeExplainsUnboundBlockedAndMissingPulseTargets(GameTestHelper h) {
        for(int x=0;x<=4;x++)h.setBlock(x,2,1,Blocks.AIR);
        var emitter=device(h,EnergyContent.Kind.EMITTER,0,1);var tank=device(h,EnergyContent.Kind.TANK,4,1);emitter.energy=1000*ManaStorage.SCALE;
        h.assertTrue(AetherJadeData.status(emitter).equals("unbound"),"Unbound routes must be explained");
        emitter.target=tank.getBlockPos();emitter.targetIdentity=tank.identity;
        h.assertTrue(AetherJadeData.status(emitter).equals("ready"),"Valid routes should be ready");
        h.setBlock(2,2,1,Blocks.STONE);h.assertTrue(AetherJadeData.status(emitter).equals("blocked"),"The actual obstruction should be reported");
        h.setBlock(2,2,1,Blocks.AIR);tank.identity=java.util.UUID.randomUUID();
        h.assertTrue(AetherJadeData.status(emitter).equals("target_missing"),"Replaced devices require rebinding");
        h.assertTrue(emitter.energy==1000*ManaStorage.SCALE&&emitter.pulseAmount==0,"Reading route status cannot fire a pulse");h.succeed();
    }
    @GameTest(template="empty",batch="jade")
    public static void jadeDistinguishesInfiniteSourcesTimedFragmentsAndCarriedStorage(GameTestHelper h) {
        var level=h.getLevel();
        var generator=device(h,EnergyContent.Kind.CREATIVE,1,1);h.assertTrue(AetherJadeData.device(generator).getBoolean("Infinite"),"Creative source must be labelled infinite");
        var battery=ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack();h.assertTrue(AetherJadeData.item(battery,level).getBoolean("Infinite"),"Creative battery is also infinite");
        var fragment=AetherShardItem.create(false,level);var fragmentData=AetherJadeData.item(fragment,level);
        h.assertTrue(!fragmentData.getBoolean("Stable")&&fragmentData.getLong("Remaining")==100,"Fragment countdown must use the server clock");
        h.assertTrue(AetherJadeData.item(AetherShardItem.create(true,level),level).getBoolean("Stable"),"Crystals never show a countdown");
        var tank=device(h,EnergyContent.Kind.TANK,2,1);tank.energy=4321*ManaStorage.SCALE;
        h.assertTrue(AetherJadeData.item(tank.asItem(),level).getLong("Stored")==0,"Broken reservoir items must show no retained aether");h.succeed();
    }
    @GameTest(template="empty",batch="jade")
    public static void crystallizationChamberUsesSuppliedAetherWithoutWater(GameTestHelper h) {
        var tank=device(h,EnergyContent.Kind.CRYSTAL_CHAMBER,1,1);tank.energy=10000*ManaStorage.SCALE;
        h.assertTrue(AetherJadeData.status(tank).equals("crystallizing")&&!tank.kind().disrupts(),"The chamber processes supplied aether rather than passively collecting water energy");h.succeed();
    }
}
