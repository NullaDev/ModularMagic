package cn.nulladev.modularmagic.utility;

import cn.nulladev.modularmagic.research.MaterialPlacement;
import cn.nulladev.modularmagic.research.PlaceholderContent;
import cn.nulladev.modularmagic.research.PlaceholderItem;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.Arrays;

public final class HandheldCraftingMenu extends CraftingMenu {
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack table;
    private final CraftingContainer grid;
    private final ResultContainer output;
    private final SimpleContainer blank = new SimpleContainer(1);
    private final int[] reference = new int[9];
    private final ItemStack[] referenceStack = new ItemStack[9];
    private int selected = -1;
    private ItemStack selectedStack = ItemStack.EMPTY;
    private boolean updating;
    private boolean placing;
    private boolean derived;
    private int clientStatus;

    public HandheldCraftingMenu(int id, Inventory inventory, InteractionHand hand) {
        super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        owner = inventory.player;
        this.hand = hand;
        table = owner.getItemInHand(hand);
        grid = (CraftingContainer) slots.get(1).container;
        output = (ResultContainer) slots.getFirst().container;
        Arrays.fill(reference, -1);
        var result = new ResultSlot(owner, grid, output, 0, 124, 35) {
            @Override
            public boolean mayPickup(Player player) {
                return super.mayPickup(player) && (!derived || canDerive());
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (!derived) {
                    super.onTake(player, stack);
                    return;
                }
                if (player instanceof ServerPlayer server) {
                    long price = PlaceholderItem.price(stack);
                    if (price < 0) price = PlaceholderItem.price(derive());
                    if (price >= 0 && PlaceholderItem.blank(blank.getItem(0))
                            && MaterialPlacement.pay(server, price, false, false)) consumeDerivationInputs();
                    refresh();
                }
            }
        };
        result.index = 0;
        slots.set(0, result);
        blank.addListener(container -> refresh());
        addSlot(new Slot(blank, 0, 92, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PlaceholderItem.blank(stack);
            }
        });
        for (int i = 0; i < reference.length; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return reference[index]; }
                @Override public void set(int value) { reference[index] = value; }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return derived ? 1 : 0; }
            @Override public void set(int value) { derived = value != 0; }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return status(); }
            @Override public void set(int value) { clientStatus = value; }
        });
    }

    @Override
    public MenuType<?> getType() {
        return UtilityContent.HANDHELD_MENU.get();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getItemInHand(hand) == table;
    }

    public ItemStack reference(int index) {
        int source = reference[index];
        return source < 0 ? ItemStack.EMPTY : grid.getItem(source);
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!player.level().isClientSide && !stillValid(player)) return;
        if (slot >= 1 && slot <= 9 && type == ClickType.PICKUP && getCarried().isEmpty()) {
            int index = slot - 1;
            if (button == 1 && PlaceholderItem.target(grid.getItem(index)) != null) {
                selected = selected == index ? -1 : index;
                selectedStack = selected < 0 ? ItemStack.EMPTY : grid.getItem(selected);
                return;
            }
            if (grid.getItem(index).isEmpty() && (reference[index] >= 0 || (button == 1 && selected >= 0))) {
                reference[index] = button == 1 ? selected : -1;
                referenceStack[index] = reference[index] < 0 ? null : selectedStack;
                refresh();
                return;
            }
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public void slotsChanged(Container container) {
        if (grid != null) refresh();
    }

    @Override
    public void beginPlacingRecipe() {
        placing = true;
        super.beginPlacingRecipe();
    }

    @Override
    public void finishPlacingRecipe(RecipeHolder<CraftingRecipe> recipe) {
        placing = false;
        super.finishPlacingRecipe(recipe);
        refresh();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, blank);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == 0 && derived) {
            Slot slot = slots.getFirst();
            if (!slot.mayPickup(player)) return ItemStack.EMPTY;
            ItemStack product = slot.getItem().copy();
            if (!moveItemStackTo(product, 10, 46, true)) return ItemStack.EMPTY;
            ItemStack taken = slot.getItem().copy();
            slot.onTake(player, taken);
            return taken;
        }
        return super.quickMoveStack(player, index);
    }

    private boolean canDerive() {
        return status() == 0;
    }

    private void consumeDerivationInputs() {
        updating = true;
        try {
            blank.removeItem(0, 1);
            // References share the physical sample at their source slot.
            for (int index = 0; index < grid.getContainerSize(); index++) {
                if (PlaceholderItem.target(grid.getItem(index)) != null) grid.removeItem(index, 1);
            }
        } finally {
            updating = false;
        }
    }

    public int status() {
        if (!(owner instanceof ServerPlayer server)) return clientStatus;
        if (!derived) return 0;
        if (!PlaceholderItem.blank(blank.getItem(0))) return 1;
        long price = PlaceholderItem.price(output.getItem(0));
        return price >= 0 && MaterialPlacement.pay(server, price, false, true) ? 0 : 2;
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer server) || updating || placing) return;
        updating = true;
        try {
            if (selected >= 0 && (grid.getItem(selected) != selectedStack || PlaceholderItem.target(selectedStack) == null)) {
                selected = -1;
                selectedStack = ItemStack.EMPTY;
            }
            for (int i = 0; i < reference.length; i++) {
                if (reference[i] >= 0 && (!grid.getItem(i).isEmpty() || reference(i) != referenceStack[i]
                        || PlaceholderItem.target(reference(i)) == null)) {
                    reference[i] = -1;
                    referenceStack[i] = null;
                }
            }
            var product = derive();
            derived = !product.isEmpty();
            if (!derived) {
                super.slotsChanged(grid);
                return;
            }
            output.setItem(0, product);
            setRemoteSlot(0, product);
            server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, product));
        } finally {
            updating = false;
        }
    }

    private ItemStack derive() {
        var level = owner.level();
        var recipes = level.getRecipeManager();
        var materials = NonNullList.withSize(9, ItemStack.EMPTY);
        var targets = NonNullList.withSize(9, ItemStack.EMPTY);
        long total = 0;
        int actual = 0;
        int samples = 0;
        ItemStack onlySample = ItemStack.EMPTY;
        ItemStack onlyActual = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            var input = grid.getItem(i);
            if (input.isEmpty()) input = reference(i);
            if (input.isEmpty()) continue;
            long price = PlaceholderItem.price(input);
            var material = PlaceholderItem.material(input);
            if (price < 0 || material.isEmpty()) {
                if (input.getItem() instanceof PlaceholderItem) return ItemStack.EMPTY;
                actual++;
                onlyActual = input;
                continue;
            }
            if (owner instanceof ServerPlayer server && !server.isCreative() && input.is(PlaceholderContent.CREATIVE.get())) return ItemStack.EMPTY;
            samples++;
            onlySample = input;
            materials.set(i, material);
            targets.set(i, input);
            total += price;
        }

        if (actual == 0 && samples > 0) {
            var input = CraftingInput.of(3, 3, materials);
            var match = recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
            if (match.isPresent()) {
                var recipe = match.get().value();
                if (recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe) {
                    return priced(recipe.assemble(input, level.registryAccess()), total);
                }
            }
        } else if (actual == 1 && samples == 1 && PlaceholderItem.recordable(onlyActual)) {
            var input = new SingleRecipeInput(PlaceholderItem.material(onlySample));
            for (var match : recipes.getRecipesFor(RecipeType.STONECUTTING, input, level)) {
                var product = match.value().assemble(input, level.registryAccess());
                if (ItemStack.isSameItemSameComponents(onlyActual, product)) return priced(product, total);
            }
        } else if (actual == 1 && samples == 2 && grid.getItem(4).is(Items.FURNACE)
                && !targets.get(3).isEmpty() && !targets.get(5).isEmpty()) {
            var fuel = materials.get(5);
            int burn = fuel.getBurnTime(RecipeType.SMELTING);
            if (burn <= 0 || !fuel.getCraftingRemainingItem().isEmpty()) return ItemStack.EMPTY;
            var input = new SingleRecipeInput(materials.get(3));
            var match = recipes.getRecipeFor(RecipeType.SMELTING, input, level);
            if (match.isPresent()) {
                var recipe = match.get().value();
                double cost = PlaceholderItem.price(targets.get(3)) +
                        (double) PlaceholderItem.price(targets.get(5)) * recipe.getCookingTime() / burn;
                return priced(recipe.assemble(input, level.registryAccess()), cost);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack priced(ItemStack product, double total) {
        if (!PlaceholderItem.recordable(product)) return ItemStack.EMPTY;
        long price = Math.max(PlaceholderContent.MIN_DERIVATION.get(), (long) Math.ceil(total / product.getCount()));
        return PlaceholderItem.derived(product, price);
    }
}
