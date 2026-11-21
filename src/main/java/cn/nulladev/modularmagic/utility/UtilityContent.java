package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.ModularMagic;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

public final class UtilityContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ModularMagic.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(ModularMagic.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ModularMagic.MOD_ID);
    public static final DeferredItem<EndlessFuelItem> FUEL=ITEMS.register("endless_fuel",EndlessFuelItem::new);
    public static final DeferredItem<AetherUtilityItem> FERTILIZER=ITEMS.register("endless_fertilizer",()->new AetherUtilityItem(AetherUtilityItem.Kind.FERTILIZER));
    public static final DeferredItem<AetherUtilityItem> FOOD=ITEMS.register("endless_food",()->new AetherUtilityItem(AetherUtilityItem.Kind.FOOD));
    public static final DeferredItem<AetherUtilityItem> CANNON=ITEMS.register("portable_tnt_cannon",()->new AetherUtilityItem(AetherUtilityItem.Kind.CANNON));
    public static final DeferredItem<AutoCrafterItem> CRAFTER=ITEMS.register("auto_crafter",AutoCrafterItem::new);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<CrafterPattern>> PATTERN=COMPONENTS.registerComponentType("crafter_pattern",b->b.persistent(CrafterPattern.CODEC).networkSynchronized(CrafterPattern.STREAM_CODEC));
    public static final DeferredHolder<MenuType<?>,MenuType<AutoCrafterMenu>> MENU=MENUS.register("auto_crafter",()->IMenuTypeExtension.create((id,inv,buf)->new AutoCrafterMenu(id,inv,buf.readEnum(net.minecraft.world.InteractionHand.class))));
    public static void register(IEventBus bus){ITEMS.register(bus);COMPONENTS.register(bus);MENUS.register(bus);}
    private UtilityContent(){}
}
