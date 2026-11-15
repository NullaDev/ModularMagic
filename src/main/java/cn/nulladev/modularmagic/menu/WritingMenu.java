package cn.nulladev.modularmagic.menu;

import cn.nulladev.modularmagic.item.NodeItem;
import cn.nulladev.modularmagic.item.SpellCoreItem;
import cn.nulladev.modularmagic.network.EditorNetworking;
import cn.nulladev.modularmagic.registry.ModContent;
import cn.nulladev.modularmagic.spell.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerListener;
import cn.nulladev.modularmagic.world.WritingTableBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;

/** Editing session over the persistent, exclusively opened writing table inventory. */
public final class WritingMenu extends AbstractContainerMenu {
    private final Player owner;
    private final ContainerLevelAccess access;
    private final SimpleContainer input;
    private final WritingTableBlockEntity table;
    private final ContainerListener inputListener = container -> inputChanged();
    public static final int REFERENCE_START = 37, REFERENCE_COUNT = 10;
    private final SimpleContainer references = new SimpleContainer(REFERENCE_COUNT);
    private boolean writingInput;
    private long lastSentRevision = -1;
    private long revision;
    private SpellDocument document;
    private SpellCompiler.Result result = new SpellCompiler.Result(false, List.of(), "", "");
    private String notice = "";
    private long snapshots;

    public WritingMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModContent.WRITING_MENU.get(), id);
        owner = inventory.player;
        access = ContainerLevelAccess.create(owner.level(), pos);
        table = owner.level().isClientSide() ? null : (WritingTableBlockEntity) owner.level().getBlockEntity(pos);
        if (!owner.level().isClientSide() && table == null) throw new IllegalStateException("Missing writing table");
        input = table == null ? new SimpleContainer(1) : table.inventory();
        if (table != null) table.acquire(this);
        addSlot(inputSlot(330, 238));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 232 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 290));
        for (int i = 0; i < REFERENCE_COUNT; i++) addSlot(referenceSlot(i, -1000, -1000));
        input.addListener(inputListener);
        inputChanged();
    }

    private Slot inputSlot(int x, int y) {
        return new Slot(input, 0, x, y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) {
                try { SpellCoreItem.read(stack); return true; }
                catch (RuntimeException e) { return false; }
            }
        };
    }

    /** Slot positions are presentation only; their index and physical container never change. */
    public void layout(int width, int height) {
        replaceSlot(0, inputSlot(width - 178, 29));
        for (int index = 1; index < REFERENCE_START; index++) {
            var old = slots.get(index);
            int local = index - 1;
            int x = width - 178 + (local % 9) * 18;
            int y = local < 27 ? height - 88 + (local / 9) * 18 : height - 30;
            replaceSlot(index, new Slot(old.container, old.getContainerSlot(), x, y));
        }
    }

    private void replaceSlot(int index, Slot slot) { slot.index = index; slots.set(index, slot); }

    private Slot referenceSlot(int index, int x, int y) {
        return new Slot(references, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean isFake() { return true; }
            @Override public boolean isActive() { return x >= 0; }
        };
    }
    public void reference(int index, int x, int y, ItemStack preview) {
        if (!owner.level().isClientSide()) return;
        references.setItem(index, preview);
        replaceSlot(REFERENCE_START + index, referenceSlot(index, x, y));
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= REFERENCE_START) return;
        super.clicked(slot, button, type, player);
    }

    private void inputChanged() {
        if (writingInput || owner.level().isClientSide()) return;
        notice = "";
        if (input.isEmpty()) {
            document = null;
            result = new SpellCompiler.Result(false, List.of(), "", "");
        } else {
            try {
                document = SpellCoreItem.read(input.getItem(0));
                result = SpellCompiler.compile(document, owner.level().registryAccess());
                store(SpellCoreItem.write(document, result, input.getItem(0)));
            } catch (RuntimeException e) {
                document = null;
                notice = "Invalid core: " + e.getMessage();
            }
        }
        revision++;
    }

    private void store(ItemStack stack) {
        writingInput = true;
        try { input.setItem(0, stack); }
        finally { writingInput = false; }
    }

    public SpellDocument document() { return document; }
    public long revision() { return revision; }
    public long snapshots() { return snapshots; }
    public boolean complete() { return result.complete(); }
    public String status() {
        if (!notice.isEmpty()) return notice;
        return result.errors().stream().limit(6).map(s -> s.substring(0, Math.min(400, s.length())))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public void receive(EditorNetworking.Snapshot packet) {
        document = packet.document().isEmpty() ? null : SpellDocument.fromJson(packet.document());
        revision = packet.revision();
        result = new SpellCompiler.Result(packet.complete(), List.of(), "", "");
        notice = packet.status();
        snapshots++;
    }

    /** The client supplies an operation, never an authoritative document or item stack. */
    public void apply(long expectedRevision, String command) {
        if (owner.level().isClientSide() || !stillValid(owner)) return;
        try {
            if (expectedRevision != revision) throw new IllegalArgumentException("Assembly changed; try again");
            if (document == null) throw new IllegalArgumentException("Insert a spell root or saved core first");
            var request = JsonParser.parseString(command).getAsJsonObject();
            var candidate = SpellDocument.fromJson(document.toJson());
            if (request.has("edits")) applyEdits(candidate, request.getAsJsonObject("edits"));
            String operation = request.get("op").getAsString();
            String removedTemplate = null;
            if (operation.equals("insert")) {
                if (!(getCarried().getItem() instanceof NodeItem item)) throw new IllegalArgumentException("Hold a node item on the cursor");
                candidate.insert(request.get("parent").getAsString(), request.get("field").getAsString(),
                        request.get("index").getAsInt(), item.template().id());
            } else if (operation.equals("remove")) {
                removedTemplate = candidate.remove(request.get("node").getAsString());
                var returned = new ItemStack(ModContent.NODES.get(removedTemplate).get());
                if (!getCarried().isEmpty() && (!ItemStack.isSameItemSameComponents(getCarried(), returned)
                        || getCarried().getCount() >= getCarried().getMaxStackSize()))
                    throw new IllegalArgumentException("Empty the cursor before removing this item");
            } else if (operation.equals("reference")) {
                String id = request.get("node").getAsString(), key = request.get("field").getAsString();
                var schema = candidate.find(id).schema();
                if (ParameterItems.kind(schema, key).isEmpty()) throw new IllegalArgumentException("Not an item reference field");
                String value = request.has("clear") && request.get("clear").getAsBoolean() ? "" : ParameterItems.value(schema, key, getCarried());
                candidate.setParameters(id, java.util.Map.of(key, value));
            } else if (operation.equals("take")) {
                if (!getCarried().isEmpty()) throw new IllegalArgumentException("Empty the cursor before taking the core");
            } else if (!operation.equals("apply")) throw new IllegalArgumentException("Unknown editor operation");

            // Serialize and validate before touching any physical items: failed edits cannot consume gems.
            var compiled = SpellCompiler.compile(candidate, owner.level().registryAccess());
            var core = SpellCoreItem.write(candidate, compiled, input.getItem(0));
            if (operation.equals("insert")) getCarried().shrink(1);
            if (removedTemplate != null) {
                if (getCarried().isEmpty()) setCarried(new ItemStack(ModContent.NODES.get(removedTemplate).get()));
                else getCarried().grow(1);
            }
            if (operation.equals("take")) {
                setCarried(core);
                store(ItemStack.EMPTY);
                document = null;
                result = new SpellCompiler.Result(false, List.of(), "", "");
            } else {
                document = candidate;
                result = compiled;
                store(core);
            }
            notice = "";
            revision++;
        } catch (RuntimeException e) {
            notice = e.getMessage() == null ? "Invalid editor operation" : e.getMessage();
            notice = notice.substring(0, Math.min(2000, notice.length()));
        }
        lastSentRevision = -1;
        broadcastChanges();
    }

    private static void applyEdits(SpellDocument document, JsonObject edits) {
        if (edits.size() > SpellDocument.MAX_NODES) throw new IllegalArgumentException("Too many edits");
        edits.entrySet().forEach(entry -> {
            var values = new LinkedHashMap<String, String>();
            entry.getValue().getAsJsonObject().entrySet().forEach(field -> values.put(field.getKey(), field.getValue().getAsString()));
            document.setParameters(entry.getKey(), values);
        });
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (owner instanceof ServerPlayer player && player.containerMenu == this && revision != lastSentRevision) {
            PacketDistributor.sendToPlayer(player, new EditorNetworking.Snapshot(containerId, revision,
                    document == null ? "" : document.toJson(), complete(), status()));
            lastSentRevision = revision;
        }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= REFERENCE_START) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, REFERENCE_START, true)) return ItemStack.EMPTY;
        } else if (slots.getFirst().mayPlace(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    public boolean isOpenForOwner() { return owner.containerMenu == this; }
    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, ModContent.WRITING_TABLE.get()) &&
                (table == null || (!table.isRemoved() && owner.level().getBlockEntity(table.getBlockPos()) == table));
    }
    @Override public void removed(Player player) {
        super.removed(player);
        input.removeListener(inputListener);
        if (table != null) table.release(this);
    }
}
