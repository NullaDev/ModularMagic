package cn.nulladev.modularmagic.registry;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.item.NodeItem;
import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.item.CoreWandItem;
import cn.nulladev.modularmagic.item.ManaContainerItem;
import cn.nulladev.modularmagic.config.ResourceConfig;
import cn.nulladev.modularmagic.menu.WandMenu;
import cn.nulladev.modularmagic.menu.WritingMenu;
import cn.nulladev.modularmagic.spell.NodeCatalog;
import cn.nulladev.modularmagic.world.WritingTableBlock;
import cn.nulladev.modularmagic.world.WritingTableBlockEntity;
import cn.nulladev.modularmagic.spell.BuiltinSpells;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModularMagic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ModularMagic.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModularMagic.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModularMagic.MOD_ID);

    public static final Map<String, DeferredItem<NodeItem>> NODES = new LinkedHashMap<>();
    public static final DeferredBlock<Block> WRITING_TABLE = BLOCKS.register("spell_writing_table",
            () -> new WritingTableBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WritingTableBlockEntity>> WRITING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("spell_writing_table", () -> BlockEntityType.Builder.of(WritingTableBlockEntity::new, WRITING_TABLE.get()).build(null));
    public static final DeferredItem<BlockItem> WRITING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(WRITING_TABLE);
    public static final DeferredItem<SpellCoreItem> FORMED_CORE = ITEMS.register("formed_spell_core",
            () -> new SpellCoreItem(new Item.Properties().stacksTo(1), true));
    public static final DeferredItem<SpellCoreItem> UNFORMED_CORE = ITEMS.register("unformed_spell_core",
            () -> new SpellCoreItem(new Item.Properties().stacksTo(1), false));
    public static final DeferredItem<CoreWandItem> CORE_WAND = ITEMS.register("core_wand", () -> new CoreWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CoreWandItem> CREATIVE_WAND = ITEMS.register("creative_wand", () -> new CoreWandItem(new Item.Properties().stacksTo(1), true));
    public static final Map<String, DeferredItem<ManaContainerItem>> MANA_CONTAINERS = new LinkedHashMap<>();
    public static final DeferredHolder<MenuType<?>, MenuType<WandMenu>> WAND_MENU = MENUS.register("wand_cores",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new WandMenu(id, inventory, buf.readEnum(net.minecraft.world.InteractionHand.class))));
    public static final DeferredHolder<MenuType<?>, MenuType<WritingMenu>> WRITING_MENU = MENUS.register("spell_writing",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new WritingMenu(id, inventory, buf.readBlockPos())));

    static {
        var capacities = java.util.List.of(ResourceConfig.BASIC, ResourceConfig.EXPANDED, ResourceConfig.ULTIMATE);
        String[] tiers = {"basic", "expanded", "ultimate"};
        Rarity[] rarities = {Rarity.COMMON,Rarity.UNCOMMON,Rarity.RARE};
        for (int i = 0; i < tiers.length; i++) {
            var capacity = capacities.get(i);
            var rarity = rarities[i];
            String id = "mana_container_" + tiers[i];
            var canonical = ITEMS.register(id, () -> new ManaContainerItem(new Item.Properties().rarity(rarity), capacity::get, false));
            MANA_CONTAINERS.put(id, canonical);
        }
        MANA_CONTAINERS.put("creative_mana_container", ITEMS.register("creative_mana_container",
                () -> new ManaContainerItem(new Item.Properties().rarity(Rarity.EPIC), () -> Long.MAX_VALUE, true)));
        for (var template : NodeCatalog.all()) NODES.put(template.id(), ITEMS.register(template.item(),
                () -> new NodeItem(new Item.Properties(), template.id())));
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.modularmagic"))
                .icon(WRITING_TABLE_ITEM::toStack)
                .displayItems((parameters, output) -> {
                    output.accept(WRITING_TABLE_ITEM.get());
                    output.accept(CORE_WAND.get());
                    output.accept(CREATIVE_WAND.get());
                    cn.nulladev.modularmagic.energy.EnergyContent.ITEMS.values().forEach(item -> output.accept(item.get()));
                    output.accept(cn.nulladev.modularmagic.energy.EnergyContent.LINK.get());
                    output.accept(cn.nulladev.modularmagic.energy.EnergyContent.RECEIVER.get());
                    output.accept(cn.nulladev.modularmagic.energy.EnergyContent.CRYSTAL.get());
                    output.accept(cn.nulladev.modularmagic.energy.EnergyContent.CROSS_LENS.get());
                    MANA_CONTAINERS.values().forEach(item -> {
                        if (!item.get().infinite(item.toStack())) output.accept(item.get());
                        output.accept(item.get().filledStack());
                    });
                    NODES.values().forEach(item -> output.accept(item.get()));
                    // Cores are assembled from a root and its node items, never generated empty.
                }).build());
        TABS.register("builtin_spells", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.modularmagic.builtin_spells"))
                .icon(FORMED_CORE::toStack)
                .displayItems((parameters, output) -> BuiltinSpells.ids().forEach(id ->
                        output.accept(BuiltinSpells.create(id, parameters.holders())))).build());
    }

    private ModContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }
}
