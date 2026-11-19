package cn.nulladev.modularmagic;

import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.mana.ManaStorage;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ModularMagic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ManaGameTests {
    @GameTest(template = "empty")
    public static void containersPersistAndNeverRegenerate(GameTestHelper helper) {
        for (String tier : java.util.List.of("basic", "expanded", "ultimate")) {
            var empty = ModContent.MANA_CONTAINERS.get("mana_container_" + tier).toStack();
            var full = ModContent.MANA_CONTAINERS.get("mana_container_" + tier).get().filledStack();
            helper.assertTrue(empty.getItem() == full.getItem(), "Empty and full must be states of the same item");
            var sink = (ManaStorage) empty.getItem(); var source = (ManaStorage) full.getItem();
            long capacity = sink.capacity(empty);
            helper.assertTrue(sink.stored(empty) == 0 && source.stored(full) == capacity, "Empty/full defaults must match capacity");
            helper.assertTrue(source.extract(full, Long.MAX_VALUE, true) == capacity && source.stored(full) == capacity, "Simulation must not consume mana");
            long moved = source.extract(full, 37, false);
            helper.assertTrue(sink.receive(empty, moved, false) == moved && source.stored(full) + sink.stored(empty) == capacity, "Transfer must conserve finite mana");
            helper.assertTrue(sink.receive(empty, -1, false) == 0 && source.extract(full, -1, false) == 0, "Negative requests must be rejected");
            var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) empty.save(helper.getLevel().registryAccess()));
            helper.assertTrue(sink.stored(restored) == moved, "Item NBT must retain stored mana");
            source.extract(full, Long.MAX_VALUE, false);
            helper.assertTrue(source.stored(full) == 0 && source.stored(full.copy()) == 0, "A depleted full-variant item must stay empty");
        }
        var creative = ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack();
        var infinite = (ManaStorage) creative.getItem();
        helper.assertTrue(infinite.infinite(creative) && infinite.extract(creative, Long.MAX_VALUE, false) == Long.MAX_VALUE
                && infinite.stored(creative) == Long.MAX_VALUE && infinite.receive(creative, 1, false) == 0, "Creative supply must remain infinite without accepting mana");
        var beginner = ModContent.CORE_WAND.toStack(); var free = ModContent.CREATIVE_WAND.toStack();
        helper.assertTrue(ModContent.CORE_WAND.get().manaMultiplier(beginner) == 1 && ModContent.CORE_WAND.get().focusMultiplier(beginner) == 1,
                "Beginner defaults must be one");
        helper.assertTrue(ModContent.CREATIVE_WAND.get().manaMultiplier(free) == 0 && ModContent.CREATIVE_WAND.get().focusMultiplier(free) == 0,
                "Creative multipliers must be zero");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inventoryTotalsAndFocus(GameTestHelper helper) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ManaOwner"));
        player.getInventory().clearContent();
        var item = ModContent.MANA_CONTAINERS.get("mana_container_basic").get();
        var full = item.filledStack(); var half = new ItemStack(item); item.receive(half, 50000, false);
        player.getInventory().setItem(0, full); player.getInventory().setItem(9, half);
        player.getInventory().offhand.set(0, new ItemStack(item));
        var sum = cn.nulladev.modularmagic.mana.InventoryMana.total(player);
        helper.assertTrue(sum.stored() == 150000 && sum.capacity() == 300000 && !sum.infinite(), "HUD total must include hotbar, backpack and offhand");
        player.getInventory().setItem(10, ModContent.MANA_CONTAINERS.get("creative_mana_container").toStack());
        helper.assertTrue(cn.nulladev.modularmagic.mana.InventoryMana.total(player).infinite(), "Creative source must mark the total infinite");
        cn.nulladev.modularmagic.mana.PlayerFocus.set(player, -2);
        cn.nulladev.modularmagic.mana.PlayerFocus.set(player, 1);
        helper.assertTrue(player.getData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED), "Focus debt must remain tripped past zero");
        cn.nulladev.modularmagic.mana.PlayerFocus.set(player, 100);
        helper.assertTrue(!player.getData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED), "Full recovery must clear the latch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void coreCostPreviewUsesStructure(GameTestHelper helper) {
        var doc = cn.nulladev.modularmagic.spell.SpellDocument.create("root/instant");
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        var action = doc.insert(doc.root.id, "action", 0, "action/processor");
        doc.insert(action.id, "selector", 0, "selector/self");
        var damage = doc.insert(action.id, "processors", 0, "effect/damage");
        doc.setParameters(damage.id, java.util.Map.of("damage", "4"));
        var low = cn.nulladev.modularmagic.spell.CostPreview.estimate(doc);
        doc.setParameters(damage.id, java.util.Map.of("damage", "100"));
        var high = cn.nulladev.modularmagic.spell.CostPreview.estimate(doc);
        helper.assertTrue(Math.abs(low.mana() - 2700) < .0001 && Math.abs(high.mana() - 175500) < .0001, "Mana quote must use the damage curve plus root");
        helper.assertTrue(Math.abs(low.focus()-8.05625) < .0001 && low.focus() == high.focus(), "Effect strength must not affect structural focus");
        doc.setParameters(damage.id, java.util.Map.of("damage", "Time"));
        var dynamic = cn.nulladev.modularmagic.spell.CostPreview.estimate(doc);
        helper.assertTrue(dynamic.dynamic() && Math.abs(dynamic.mana()-500)<.0001, "Runtime additions must not erase the fixed startup base");
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void batteryBalancesRetainTheirExactStoredUnits(GameTestHelper helper){
        var item=ModContent.MANA_CONTAINERS.get("mana_container_expanded").get();var stack=new ItemStack(item);
        cn.nulladev.modularmagic.item.ManaContainerItem.writeUnits(stack,375000*ManaStorage.SCALE+1);
        var restored=ItemStack.parseOptional(helper.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)stack.save(helper.getLevel().registryAccess()));
        helper.assertTrue(item.capacity(restored)==1000000&&item.storedUnits(restored)==375000*ManaStorage.SCALE+1,"Reloading a battery must preserve the fixed-point balance exactly");helper.succeed();
    }
    @GameTest(template="empty")
    @SuppressWarnings("unchecked")
    public static void oldFocusBalancesAndDebtConvertExactlyOnce(GameTestHelper helper){
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FocusUnits"));
        var units=(net.neoforged.neoforge.attachment.AttachmentType<Integer>)net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(ModularMagic.id("focus_units"));
        helper.assertTrue(cn.nulladev.modularmagic.mana.PlayerFocus.value(p)==100,"New players must start at the new full focus maximum");
        p.setData(cn.nulladev.modularmagic.mana.PlayerFocus.VALUE,16D);p.setData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED,true);p.setData(units,1);
        helper.assertTrue(cn.nulladev.modularmagic.mana.PlayerFocus.value(p)==40&&cn.nulladev.modularmagic.mana.PlayerFocus.value(p)==40,"An old partial balance must scale once while preserving its percentage");
        helper.assertTrue(p.getData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED),"Migrating positive recovery must preserve an existing lockout");
        p.setData(cn.nulladev.modularmagic.mana.PlayerFocus.VALUE,-8D);p.setData(units,1);
        helper.assertTrue(cn.nulladev.modularmagic.mana.PlayerFocus.value(p)==-20&&p.getData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED),"Old negative debt must scale without being forgiven");
        p.setData(cn.nulladev.modularmagic.mana.PlayerFocus.VALUE,40D);p.setData(units,1);
        helper.assertTrue(cn.nulladev.modularmagic.mana.PlayerFocus.value(p)==100&&!p.getData(cn.nulladev.modularmagic.mana.PlayerFocus.TRIPPED),"An old full balance must remain full after conversion");helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fixedParametersAreIncludedInCoreTooltips(GameTestHelper helper) {
        var doc = cn.nulladev.modularmagic.spell.SpellDocument.create("root/instant");
        doc.insert(doc.root.id,"trigger_type",0,"trigger/self_pos");
        var variable = doc.insert(doc.root.id,"action",0,"action/variable");
        doc.setParameters(variable.id,java.util.Map.of("name","d","variable","4"));
        var loop = doc.insert(variable.id,"child",0,"action/iterate");
        doc.setParameters(loop.id,java.util.Map.of("step","3","index","i"));
        var process=doc.insert(loop.id,"child",0,"action/processor");
        doc.insert(process.id,"selector",0,"selector/self");
        var damage=doc.insert(process.id,"processors",0,"effect/damage");
        doc.setParameters(damage.id,java.util.Map.of("damage","d*(i+1)"));
        var cost=cn.nulladev.modularmagic.spell.CostPreview.estimate(doc);
        helper.assertTrue(!cost.dynamic() && Math.abs(cost.mana()-15300)<.0001, "Saved local variables and each loop index must be substituted before summing damage");
        doc.setParameters(damage.id,java.util.Map.of("damage","d"));
        doc.setParameters(loop.id,java.util.Map.of("step","10000"));
        var repeated=cn.nulladev.modularmagic.spell.CostPreview.estimate(doc);
        helper.assertTrue(!repeated.dynamic() && Math.abs(repeated.mana()-22000500)<.0001,
                "A constant body must price large loops by multiplication without losing its base");
        for (String id : cn.nulladev.modularmagic.spell.BuiltinSpells.ids()) {
            var core=cn.nulladev.modularmagic.spell.BuiltinSpells.create(id,helper.getLevel().registryAccess());
            var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
            core.getItem().appendHoverText(core,Item.TooltipContext.of(helper.getLevel()),lines,TooltipFlag.NORMAL);
            helper.assertTrue(lines.size()==4, "Formed core must show type, node count and two base costs without a footer: "+id);
            var last=lines.getLast().getContents();
            helper.assertTrue(last instanceof net.minecraft.network.chat.contents.TranslatableContents c && c.getKey().equals("tooltip.modularmagic.focus_quote"),
                    "Last tooltip line must be focus cost: "+id);
        }
        var effects=cn.nulladev.modularmagic.spell.SpellDocument.create("root/instant");
        effects.insert(effects.root.id,"trigger_type",0,"trigger/self_pos");
        var action=effects.insert(effects.root.id,"action",0,"action/processor");
        var ball=effects.insert(action.id,"selector",0,"selector/ball");
        effects.setParameters(ball.id,java.util.Map.of("r","3"));
        var effect=effects.insert(action.id,"processors",0,"effect/effect");
        effects.setParameters(effect.id,java.util.Map.of("effect","minecraft:speed","duration","200","amplifier","1"));
        var result=cn.nulladev.modularmagic.spell.CostPreview.estimate(effects);
        helper.assertTrue(!result.dynamic() && result.mana()==Math.ceil(1000*(16.55+.002*36*Math.PI)),
                "Fixed area, duration and effect level must produce a numeric base cost");
        helper.succeed();
    }
}
