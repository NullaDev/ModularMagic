package cn.nulladev.modularmagic.spell;

import cn.nulladev.modularmagic.ModularMagic;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import dev.xkmc.l2magic.content.engine.context.BuilderContext;
import dev.xkmc.l2magic.content.engine.spell.SpellAction;
import dev.xkmc.l2magic.content.engine.variable.*;
import dev.xkmc.l2magic.content.entity.core.ProjectileConfig;
import dev.xkmc.l2magic.content.entity.core.ProjectileData;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.core.*;
import net.minecraft.resources.*;
import dev.xkmc.shadow.objecthunter.exp4j.ExpressionBuilder;

import java.util.*;

/** Compiles only complete documents, using the prerequisite's actual codecs and verifier. */
public final class SpellCompiler {
    public record Result(boolean complete, List<String> errors, String spellJson, String projectilesJson) {}

    private final MappedRegistry<ProjectileConfig> projectiles = new MappedRegistry<>(EngineRegistry.PROJECTILE, Lifecycle.stable());
    private final JsonObject projectileJson = new JsonObject();
    private final ValidationLog diagnostics = new ValidationLog();
    private final RegistryOps<JsonElement> ops;
    public record RuntimeSpell(SpellAction action, MappedRegistry<ProjectileConfig> projectiles) {}

    private SpellCompiler(HolderLookup.Provider registries) {
        ops = RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                if (key.equals(EngineRegistry.PROJECTILE)) return (Optional) Optional.of(
                        RegistryOps.RegistryInfo.fromRegistryLookup(projectiles.asLookup()));
                return registries.lookup(key).map(RegistryOps.RegistryInfo::fromRegistryLookup);
            }
        });
    }

    public static Result compile(SpellDocument document, HolderLookup.Provider registries) {
        return compile(document, registries, null);
    }
    public static RuntimeSpell runtime(SpellDocument document, HolderLookup.Provider registries) {
        RuntimeSpell[] output = new RuntimeSpell[1];
        var result = compile(document, registries, output);
        if (!result.complete()) throw new IllegalArgumentException(String.join("; ", result.errors()));
        return output[0];
    }
    private static Result compile(SpellDocument document, HolderLookup.Provider registries, RuntimeSpell[] output) {
        var errors = document.structuralErrors();
        if (!errors.isEmpty()) return new Result(false, errors, "", "");
        try {
            for (var node : document.nodes()) if (node.template.equals("action/random")) {
                int count = Integer.parseInt(node.parameters.get("count").trim());
                // Upstream eagerly creates this many variable names during verification.
                if (count < 0 || count > SpellDocument.MAX_LIST)
                    throw new IllegalArgumentException("action/random.count: expected 0.." + SpellDocument.MAX_LIST);
            }
            var compiler = new SpellCompiler(registries);
            var json = compiler.export(document.root);
            var action = SpellAction.CODEC.parse(compiler.ops, json).getOrThrow();
            // The upstream record walker logs child failures but discards their boolean results.
            // Reuse its contexts and traversal while also collecting every diagnostic.
            boolean valid = action.action().verify(BuilderContext.withScheduler(compiler.diagnostics, "spell/action", action.params()));
            if (action.condition() != null) valid &= action.condition().verify(
                    BuilderContext.instant(compiler.diagnostics, "spell/condition", action.params()));
            if (!valid || !compiler.diagnostics.errors().isEmpty())
                return new Result(false, compiler.diagnostics.errors().isEmpty()
                        ? List.of("L2Magic: invalid expression, variable scope or node context") : compiler.diagnostics.errors(), "", "");
            checkExpressionSyntax(action, "spell", Collections.newSetFromMap(new IdentityHashMap<>()));
            if (output != null) output[0] = new RuntimeSpell(action, compiler.projectiles);
            return new Result(true, List.of(), json.toString(), compiler.projectileJson.toString());
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new Result(false, List.of(message.length() > 2048 ? message.substring(0, 2048) : message), "", "");
        }
    }

    private JsonElement export(SpellDocument.Node node) {
        var schema = node.schema();
        if (schema.category().equals("trigger")) return new JsonPrimitive(schema.nativeType());
        if (schema.id().equals("predicate/expression")) return new JsonPrimitive(node.parameters.get("value"));
        var json = new JsonObject();
        if (schema.category().equals("root")) json.addProperty("cast_type", schema.nativeType());
        else if (!schema.category().equals("projectile")) json.addProperty("type", schema.nativeType());
        for (var field : schema.fields()) {
            String value = node.parameters.getOrDefault(field.key(), "").trim();
            if (value.isEmpty() && field.optional()) continue;
            try {
                json.add(field.key(), parameter(field, value));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(schema.id() + "." + field.key() + ": " + e.getMessage(), e);
            }
        }
        for (var slot : schema.slots()) {
            var children = node.children.get(slot.key());
            if (slot.list()) {
                var array = new JsonArray();
                children.forEach(child -> array.add(export(child)));
                json.add(slot.key(), array);
            } else if (!children.isEmpty()) json.add(slot.key(), export(children.getFirst()));
        }
        if (schema.category().equals("projectile")) {
            var id = ModularMagic.id("assembled/" + node.id);
            var config = ProjectileConfig.CODEC.parse(ops, json).getOrThrow();
            if (!verifyProjectile(config)) throw new IllegalArgumentException("Invalid expression in projectile " + node.id);
            projectiles.register(ResourceKey.create(EngineRegistry.PROJECTILE, id), config, RegistrationInfo.BUILT_IN);
            projectileJson.add(id.toString(), json);
            return new JsonPrimitive(id.toString());
        }
        return json;
    }

    public static JsonElement parameter(NodeTemplate.Field field, String text) {
        return switch (field.kind()) {
            case "integer" -> new JsonPrimitive(Integer.parseInt(text));
            case "boolean" -> {
                if (!text.equals("true") && !text.equals("false")) throw new IllegalArgumentException("Expected true or false");
                yield new JsonPrimitive(Boolean.parseBoolean(text));
            }
            case "json" -> JsonParser.parseString(text);
            case "json_or_string" -> text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")
                    ? JsonParser.parseString(text) : new JsonPrimitive(text);
            default -> new JsonPrimitive(text);
        };
    }

    private boolean verifyProjectile(ProjectileConfig config) {
        var params = new HashSet<>(ProjectileData.DEFAULT_PARAMS);
        params.addAll(config.params());
        var scheduled = BuilderContext.withScheduler(diagnostics, "projectile", params);
        var instant = BuilderContext.instant(diagnostics, "projectile", params);
        boolean valid = true;
        if (config.motion() != null) valid &= config.motion().verify(instant.of("motion"));
        if (config.tick() != null) valid &= config.tick().verify(scheduled.of("tick"));
        if (config.land() != null) valid &= config.land().verify(scheduled.of("land"));
        if (config.expire() != null) valid &= config.expire().verify(scheduled.of("expire"));
        for (var hit : config.hit()) valid &= hit.verify(scheduled.of("hit"));
        if (config.size() != null) valid &= config.size().verify(instant.of("size"));
        if (config.renderer() != null) valid &= config.renderer().verify(instant.of("renderer"));
        return valid;
    }

    /** L2Magic verifies scope but does not reject an RPN expression with missing operands. */
    private static void checkExpressionSyntax(Object value, String path, Set<Object> seen) {
        if (value == null || !seen.add(value)) return;
        String text = switch (value) {
            case DoubleVariable variable -> variable.str();
            case IntVariable variable -> variable.str();
            case BooleanVariable variable -> variable.str();
            default -> null;
        };
        if (text != null) {
            var names = new HashSet<String>();
            var matcher = java.util.regex.Pattern.compile("[\\p{L}_][\\p{L}\\p{N}_]*").matcher(text);
            while (matcher.find()) {
                // Function names are not variables; scope was already checked by the engine.
                if (!text.substring(matcher.end()).stripLeading().startsWith("(")) names.add(matcher.group());
            }
            var validation = new ExpressionBuilder(text).operator(DefaultOperators.OPERATORS)
                    .functions(DefaultFunctions.FUNCTIONS).function(DefaultFunctions.rand(() -> 0))
                    .variables(names).build().validate(false);
            if (!validation.isValid()) throw new IllegalArgumentException(path + ": " + text + " - " + validation.getErrors());
        } else if (value instanceof Holder<?> holder) {
            if (holder.value() instanceof ProjectileConfig config) checkExpressionSyntax(config, path + ".config", seen);
        } else if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> checkExpressionSyntax(child, path + "." + key, seen));
        } else if (value instanceof Iterable<?> list) {
            int i = 0;
            for (var child : list) checkExpressionSyntax(child, path + "[" + i++ + "]", seen);
        } else if (value.getClass().isRecord() && value.getClass().getPackageName().startsWith("dev.xkmc.l2magic")) {
            for (var component : value.getClass().getRecordComponents()) {
                try { checkExpressionSyntax(component.getAccessor().invoke(value), path + "." + component.getName(), seen); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot validate engine record", e); }
            }
        }
    }
}
