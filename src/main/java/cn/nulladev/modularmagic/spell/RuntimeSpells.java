package cn.nulladev.modularmagic.spell;

import cn.nulladev.modularmagic.ModularMagic;
import dev.xkmc.l2magic.content.engine.context.*;
import dev.xkmc.l2magic.content.engine.helper.Scheduler;
import dev.xkmc.l2magic.content.entity.core.ProjectileConfig;
import dev.xkmc.l2magic.init.registrate.EngineRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import java.util.*;

/** Runtime programs are private to a level; the frozen datapack registries stay untouched. */
public final class RuntimeSpells {
    public static final ResourceLocation ANCHOR = ModularMagic.id("runtime_anchor");
    private static final Map<Level, Map<String, SpellCompiler.RuntimeSpell>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Holder<ProjectileConfig>, String> SOURCES = Collections.synchronizedMap(new WeakHashMap<>());

    public static SpellCompiler.RuntimeSpell get(Level level, String document) {
        synchronized (CACHE) {
            var cache = CACHE.computeIfAbsent(level, k -> new LinkedHashMap<>(16, .75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, SpellCompiler.RuntimeSpell> e) { return size() > 64; }
            });
            return cache.computeIfAbsent(document, text -> {
                var compiled = SpellCompiler.runtime(SpellDocument.fromJson(text), level.registryAccess());
                compiled.projectiles().holders().forEach(holder -> SOURCES.put(holder, text));
                return compiled;
            });
        }
    }

    public static String source(Holder<ProjectileConfig> config) { return SOURCES.getOrDefault(config, ""); }
    public static boolean isCustom(EngineContext context) {
        return context.user().root().unwrapKey().map(key -> key.location().equals(ANCHOR)).orElse(false);
    }
    public static void execute(Level level, String document, SpellContext context) {
        var spell = get(level, document);
        var anchor = level.registryAccess().registryOrThrow(EngineRegistry.SPELL).getHolder(ANCHOR).orElseThrow();
        var engine = new EngineContext(new UserContext(level, context.user(), anchor, new Scheduler()),
                LocationContext.of(context.origin(), context.facing()), new SingleThreadedRandomSource(context.seed()), context.defaultArgs());
        spell.action().action().execute(engine);
        engine.registerScheduler();
    }
    private RuntimeSpells() {}
}
