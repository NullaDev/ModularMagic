package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

public final class UtilityContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(ModularMagic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ModularMagic.MOD_ID);
    public static final DeferredItem<AetherUtilityItem> PLACE=unit("placement_unit",AetherUtilityItem.Kind.PLACE);
    public static final DeferredItem<AetherUtilityItem> FOOD=unit("eating_unit",AetherUtilityItem.Kind.FOOD);
    public static final DeferredItem<AetherUtilityItem> LAUNCH=unit("launch_unit",AetherUtilityItem.Kind.LAUNCH);
    public static final DeferredItem<AetherUtilityItem> BURN=unit("burning_unit",AetherUtilityItem.Kind.BURN);
    public static final DeferredItem<AetherUtilityItem> TOOL=unit("tool_unit",AetherUtilityItem.Kind.TOOL);
    public static final DeferredItem<AetherUtilityItem> CANNON=ITEMS.register("portable_tnt_cannon",()->new AetherUtilityItem(AetherUtilityItem.Kind.CANNON));
    public static final DeferredItem<AutoCrafterItem> CRAFTER=ITEMS.register("auto_crafter",AutoCrafterItem::new);
    public static final DeferredItem<Item> HANDHELD=ITEMS.register("handheld_aether_crafting_table",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            if(player instanceof ServerPlayer server)server.openMenu(new SimpleMenuProvider(
                    (id,inventory,user)->new HandheldCraftingMenu(id,inventory,hand),
                    Component.translatable("item.modularmagic.handheld_aether_crafting_table")),buffer->buffer.writeEnum(hand));
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        }
    });
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<CrafterPattern>> PATTERN=COMPONENTS.registerComponentType("crafter_pattern",b->b.persistent(CrafterPattern.CODEC).networkSynchronized(CrafterPattern.STREAM_CODEC));
    public static final DeferredHolder<MenuType<?>,MenuType<AutoCrafterMenu>> MENU=MENUS.register("auto_crafter",()->IMenuTypeExtension.create((id,inv,buf)->new AutoCrafterMenu(id,inv,buf.readEnum(net.minecraft.world.InteractionHand.class))));
    public static final DeferredHolder<MenuType<?>,MenuType<CraftingMenu>> HANDHELD_MENU=MENUS.register("handheld_aether_crafting_table",()->IMenuTypeExtension.create((id,inv,buf)->new HandheldCraftingMenu(id,inv,buf.readEnum(InteractionHand.class))));
    public static final DeferredHolder<MenuType<?>,MenuType<UnitSampleMenu>> SAMPLE_MENU=MENUS.register("unit_sample",()->IMenuTypeExtension.create((id,inv,buf)->new UnitSampleMenu(id,inv,buf.readEnum(InteractionHand.class))));
    public static final DeferredHolder<MenuType<?>,MenuType<MaterializerMenu>> MATERIALIZER_MENU=MENUS.register("aether_materializer",()->IMenuTypeExtension.create((id,inv,buf)->new MaterializerMenu(id,inv)));
    private static DeferredItem<AetherUtilityItem> unit(String id,AetherUtilityItem.Kind kind){return ITEMS.register(id,()->new AetherUtilityItem(kind));}
    public static void register(IEventBus bus){ITEMS.register(bus);COMPONENTS.register(bus);MENUS.register(bus);CannonRecipe.register(bus);}
    private UtilityContent(){}
}
