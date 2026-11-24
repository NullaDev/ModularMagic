package cn.nulladev.modularmagic.spell;

import cn.nulladev.modularmagic.ModularMagic;
import cn.nulladev.modularmagic.item.SpellCoreItem;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.xkmc.l2magic.content.engine.spell.SpellAction;
import dev.xkmc.l2magic.content.entity.core.ProjectileConfig;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.*;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Editable example cores backed by the L2Magic spell registry. */
public final class BuiltinSpells {
    private static final List<String> L2MAGIC = List.of(
            "absolute_zero", "blazing_hammer", "circular", "circular_entity", "cobwebs", "earthquake",
            "earth_spike", "earth_spike_field", "echo_blades", "echo_rings", "flame_burst", "flame_charge",
            "flame_pillar", "floor", "frost_nova", "glacial_domain", "icy_flash", "icy_shatter",
            "magic_arrows", "magic_arrow_ring", "magma_shield", "magnet_core", "master_spark", "melt",
            "starfall", "swept", "tornado", "winter_storm");
    private static final Map<String, ResourceLocation> SOURCES = sources();

    private static Map<String, ResourceLocation> sources() {
        var ids = new LinkedHashMap<String, ResourceLocation>();
        L2MAGIC.forEach(id -> ids.put(id, ResourceLocation.fromNamespaceAndPath("l2magic", id)));
        try (var stream = Objects.requireNonNull(BuiltinSpells.class.getResourceAsStream("/data/modularmagic/builtin_spells/glimmering.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                String id = element.getAsString();
                if (ids.putIfAbsent(id, ModularMagic.id(id)) != null) throw new IllegalStateException("Duplicate example spell " + id);
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read example spell list", e);
        }
        return Collections.unmodifiableMap(ids);
    }

    private BuiltinSpells() {}
    public static Set<String> ids() { return SOURCES.keySet(); }
    public static ResourceLocation sourceId(String id) {
        var source = SOURCES.get(id);
        if (source == null) throw new IllegalArgumentException("Unknown example spell " + id);
        return source;
    }
    public static ItemStack create(String id, HolderLookup.Provider registries) {
        var key = sourceId(id);
        var action = registries.lookupOrThrow(EngineRegistry.SPELL)
                .getOrThrow(ResourceKey.create(EngineRegistry.SPELL, key)).value();
        var json = SpellAction.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), action).getOrThrow().getAsJsonObject();
        var document = SpellDocument.create(template("root", json.get("cast_type").getAsString()).id());
        populate(document, document.root, json, registries);
        var compiled = SpellCompiler.compile(document, registries);
        if (!compiled.complete()) throw new IllegalStateException("Cannot import L2Magic spell " + key + ": " + compiled.errors());
        var core = SpellCoreItem.write(document, compiled);
        core.set(DataComponents.CUSTOM_NAME, Component.translatable("spell.modularmagic." + id.replace('/', '.')));
        return core;
    }

    private static NodeTemplate template(String category, String type) {
        return NodeCatalog.all().stream().filter(t -> t.category().equals(category) && t.nativeType().equals(type))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported L2Magic node " + category + ": " + type));
    }

    private static void populate(SpellDocument document, SpellDocument.Node node, JsonElement json, HolderLookup.Provider registries) {
        var schema = node.schema();
        if (schema.category().equals("trigger")) return;
        if (schema.id().equals("predicate/expression")) {
            document.setParameters(node.id, Map.of("value", json.getAsString()));
            return;
        }
        var object = json.getAsJsonObject();
        var parameters = new LinkedHashMap<String, String>();
        for (var field : schema.fields()) {
            var value = object.get(field.key());
            parameters.put(field.key(), value == null ? "" : value.isJsonPrimitive() && !field.kind().equals("json")
                    ? value.getAsString() : value.toString());
        }
        document.setParameters(node.id, parameters);
        for (var slot : schema.slots()) {
            var value = object.get(slot.key());
            if (value == null || value.isJsonNull()) continue;
            Iterable<JsonElement> values = slot.list() ? value.getAsJsonArray() : List.of(value);
            int index = 0;
            for (var child : values) {
                if (slot.category().equals("projectile") && child.isJsonPrimitive()) {
                    var holder = registries.lookupOrThrow(EngineRegistry.PROJECTILE).getOrThrow(ResourceKey.create(
                            EngineRegistry.PROJECTILE, ResourceLocation.parse(child.getAsString())));
                    child = ProjectileConfig.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), holder.value()).getOrThrow();
                }
                var childTemplate = slot.category().equals("projectile") ? NodeCatalog.get("projectile/config")
                        : slot.category().equals("predicate") && child.isJsonPrimitive() ? NodeCatalog.get("predicate/expression")
                        : template(slot.category(), slot.category().equals("trigger") ? child.getAsString() : child.getAsJsonObject().get("type").getAsString());
                var inserted = document.insert(node.id, slot.key(), index++, childTemplate.id());
                populate(document, inserted, child, registries);
            }
        }
    }
}
