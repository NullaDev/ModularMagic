package cn.nulladev.modularmagic.research;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.BuiltinSpells;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public record ExampleSpellLoot(String spell) implements LootItemFunction {
    private static final MapCodec<ExampleSpellLoot> CODEC = Codec.STRING.validate(id -> BuiltinSpells.ids().contains(id)
            ? DataResult.success(id) : DataResult.error(() -> "Unknown Modular Magic example spell " + id))
            .fieldOf("spell").xmap(ExampleSpellLoot::new, ExampleSpellLoot::spell);
    private static final DeferredRegister<LootItemFunctionType<?>> REGISTRY = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, ModularMagic.MOD_ID);
    private static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<ExampleSpellLoot>> TYPE =
            REGISTRY.register("example_spell", () -> new LootItemFunctionType<>(CODEC));

    private enum Tier {
        COMMON(0.06F, "glimmering/flower", "glimmering/snow", "glimmering/sponge", "glimmering/vine", "magic_arrows"),
        UNCOMMON(0.10F, "glimmering/amethyst", "glimmering/stone_bridge", "glimmering/flame_dash", "glimmering/icy_flash", "frost_nova"),
        RARE(0.15F, "glimmering/meteor", "glimmering/thunderstorm", "glimmering/ocean_shelter", "glimmering/amethyst_penetration", "starfall");

        final float chance;
        final List<String> spells;

        Tier(float chance, String... spells) {
            this.chance = chance;
            this.spells = List.of(spells);
        }
    }

    public static void register(IEventBus bus) { REGISTRY.register(bus); }

    static void addTo(LootTableLoadEvent event) {
        String path = event.getName().getPath();
        if (!path.startsWith("chests/")) return;
        Tier tier = tier(path);
        if (tier == null) return;
        var pool = LootPool.lootPool().name("modularmagic_example_spells").setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(tier.chance));
        for (String id : tier.spells) pool.add(LootItem.lootTableItem(ModContent.FORMED_CORE.get()).apply(() -> new ExampleSpellLoot(id)));
        event.getTable().addPool(pool.build());
    }

    private static Tier tier(String path) {
        if (path.startsWith("chests/village/") || path.equals("chests/simple_dungeon") || path.equals("chests/abandoned_mineshaft")) return Tier.COMMON;
        if (path.startsWith("chests/trial_chambers/")) return switch (path) {
            case "chests/trial_chambers/reward" -> Tier.UNCOMMON;
            case "chests/trial_chambers/reward_ominous" -> Tier.RARE;
            default -> null;
        };
        if (path.contains("ancient_city") || path.contains("end_city") || path.contains("bastion") || path.contains("stronghold")) return Tier.RARE;
        if (path.contains("desert_pyramid") || path.contains("jungle_temple") || path.contains("nether_bridge") || path.contains("woodland_mansion")) return Tier.UNCOMMON;
        return null;
    }

    @Override public LootItemFunctionType<ExampleSpellLoot> getType() { return TYPE.get(); }

    @Override public ItemStack apply(ItemStack stack, LootContext context) {
        try {
            return BuiltinSpells.create(spell, context.getLevel().registryAccess());
        } catch (RuntimeException failure) {
            ModularMagic.LOGGER.warn("Could not assemble example spell {} for loot", spell, failure);
            return ItemStack.EMPTY;
        }
    }
}
