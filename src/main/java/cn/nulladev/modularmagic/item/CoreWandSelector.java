package cn.nulladev.modularmagic.item;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.menu.WandMenu;
import dev.xkmc.l2itemselector.select.item.CircularSelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.stream.IntStream;

public final class CoreWandSelector extends CircularSelector<Integer> {
    public CoreWandSelector() { super(ModularMagic.id("wand_core_select")); }
    @Override public boolean test(ItemStack stack) {
        return stack.getItem() instanceof CoreWandItem && WandCores.read(stack).stream().anyMatch(s -> !s.isEmpty());
    }
    @Override public List<Integer> getAll(ItemStack stack) {
        var cores = WandCores.read(stack);
        return IntStream.range(0, WandCores.CAPACITY).filter(i -> !cores.get(i).isEmpty()).boxed().toList();
    }
    @Override public Integer getSelected(ItemStack stack) { return WandCores.selected(stack); }
    @Override public List<ItemStack> getDisplayList(ItemStack stack) {
        var cores = WandCores.read(stack);
        return getListGeneric(stack, i -> {
            var core = cores.get(i);
            try {
                var doc = SpellCoreItem.read(core);
                var id = net.minecraft.resources.ResourceLocation.tryParse(doc.root.parameters.get("icon"));
                var icon = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id));
                if (icon.isEmpty()) return core.copy();
                icon.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, core.getHoverName());
                return icon;
            } catch (RuntimeException ignored) { return core.copy(); }
        });
    }
    @Override public List<ItemStack> getList(ItemStack stack) {
        // The inherited UI uses the list length; swapping below edits only the selected index.
        return getDisplayList(stack);
    }
    @Override public void swap(Player player, int index, ItemStack stack) {
        if (player.containerMenu instanceof WandMenu || player.isUsingItem()) return;
        var choices = getListGeneric(stack, i -> {
            var marker = stack.copyWithCount(1); WandCores.select(marker, i); return marker;
        });
        if (index < 0 || index >= choices.size()) return;
        WandCores.select(stack, WandCores.selected(choices.get(index)));
    }
    @Override public int getSelHash(ItemStack stack) { return java.util.Objects.hash(WandCores.selected(stack), stack.get(net.minecraft.core.component.DataComponents.CONTAINER)); }
}
