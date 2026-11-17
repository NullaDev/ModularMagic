package cn.nulladev.modularmagic.spell;

import dev.xkmc.l2magic.content.engine.variable.*;
import dev.xkmc.shadow.objecthunter.exp4j.ExpressionBuilder;
import java.util.*;

/** Base price from saved parameters, one target and handbook rates. This is not a reservation or final bill.
 * Genuinely runtime-dependent additions are tracked separately from the numeric base. */
public final class CostPreview {
    public record Quote(double mana, double focus, boolean cosmetic, boolean dynamic) {
        public Quote(double mana, double focus, boolean cosmetic) { this(mana, focus, cosmetic, !valid(mana) || !valid(focus)); }
        public Quote { if (!valid(mana)) mana = 0; if (!valid(focus)) focus = 0; }
    }
    private record Pricing(boolean dynamic) {
        Quote make(double mana, double focus, boolean cosmetic) {
            return new Quote(mana, focus, cosmetic, dynamic || !valid(mana) || !valid(focus));
        }
    }
    private static final Map<String, Quote> CACHE = new LinkedHashMap<>(64, .75F, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Quote> e) { return size() > 64; }
    };
    public static synchronized Quote estimate(SpellDocument document) {
        return CACHE.computeIfAbsent(document.toJson(), key -> {
            var quote=visit(document.root,document.root.template.equals("root/instant") ? Map.of("Power", 1D, "TickUsing", 0D) : Map.of("Power", 1D),new int[]{16384});
            return new Quote(ResourceBalance.wholeMana(quote.mana*ResourceBalance.MANA_SCALE),quote.focus,quote.cosmetic,quote.dynamic);
        });
    }
    private static Quote visit(SpellDocument.Node node, Map<String, Double> vars, int[] budget) {
        if (--budget[0] < 0) return new Quote(Double.NaN, Double.NaN, false);
        String id = node.template, category = node.schema().category();
        var local = new HashMap<>(vars);
        if (id.equals("action/variable")) local.put(node.parameters.get("name"), value(node, "variable", vars, Double.NaN));
        if (id.equals("action/custom_projectile")) {
            try {
                var params = com.google.gson.JsonParser.parseString(node.parameters.getOrDefault("params", "{}"));
                if (params.isJsonObject()) params.getAsJsonObject().entrySet().forEach(e -> {
                    try { local.put(e.getKey(), expression(e.getValue().getAsString(), vars)); } catch (RuntimeException ignored) {}
                });
            } catch (RuntimeException ignored) {}
            local.put("__life", value(node, "life", vars, Double.NaN));
        }
        boolean loop = id.startsWith("action/iterate") || id.equals("action/random_pos_fan") || id.equals("action/random_pos_sphere") || id.equals("action/while_delayed");
        double repetitions = loop ? value(node, node.parameters.containsKey("count") ? "count" : "step", vars, Double.NaN) : 1;
        if (Double.isFinite(repetitions)) repetitions = (int) repetitions;
        if (loop) local.put(node.parameters.getOrDefault("index", ""), 0D);
        var children = new HashMap<String, Quote>();
        for (var slot : node.schema().slots()) {
            Quote sum = new Quote(0, 0, true);
            for (var child : node.children.get(slot.key())) sum = add(sum, visit(child, local, budget));
            children.put(slot.key(), sum);
        }
        Quote all = new Quote(0, 0, true);
        for (var q : children.values()) all = add(all, q);
        var pricing = new Pricing(all.dynamic);
        boolean visual = category.endsWith("renderer") || category.equals("trigger") || id.contains("particle") || id.equals("action/sound");
        if (category.equals("root")) return pricing.make((all.cosmetic ? .01 : .5) + all.mana, ResourceBalance.focus(all.cosmetic ? .02*all.focus : all.focus), all.cosmetic);
        if (category.equals("trigger") || category.endsWith("renderer")) return pricing.make(all.mana, all.focus, true);
        if (category.equals("modifier")) return pricing.make(0, .05, false);
        if (id.contains("particle")) return pricing.make(.0002, all.focus, true);
        if (id.equals("action/sound")) return pricing.make(.001, 0, true);
        double focus = all.focus, mana = all.mana;
        var body = children.getOrDefault("child", new Quote(0, 0, true));
        if (id.equals("action/move") || id.equals("selector/move")) {
            var modifiers = children.getOrDefault("modifiers", new Quote(0, 0, true));
            return pricing.make(modifiers.mana+body.mana, modifiers.focus+body.focus, body.cosmetic);
        }
        if (id.equals("action/if") || id.equals("effect/filtered")) {
            var a = children.getOrDefault("action", new Quote(0, 0, true));
            var b = children.getOrDefault("fallback", new Quote(0, 0, true));
            var p = children.getOrDefault(id.equals("action/if") ? "predicate" : "filter", new Quote(0, 0, true));
            return pricing.make(p.mana + Math.max(a.mana, b.mana), .1 + p.focus + Math.max(a.focus, b.focus) + .25 * Math.min(a.focus, b.focus), a.cosmetic && b.cosmetic);
        }
        if (id.startsWith("action/iterate") || id.equals("action/random_pos_fan") || id.equals("action/random_pos_sphere") || id.equals("action/while_delayed")) {
            double n = repetitions;
            if (n < 0 || n > 1000000) n = Double.NaN;
            double delay = value(node, "delay", vars, 0); if (Double.isFinite(delay)) delay = (int) delay;
            double overhead = .2 + (node.parameters.containsKey("delay") ? .15 + h(Math.max(0, n - 1) * delay) : 0);
            double bodyMana = n == 0 ? 0 : n * body.mana, bodyFocus = body.focus;
            boolean uncertain = all.dynamic;
            String index = node.parameters.getOrDefault("index", "");
            boolean indexed = !index.isBlank() && node.children.get("child").size() == 1 && references(node.children.get("child").getFirst(), index);
            if (indexed && n > 0 && n <= 4096 && budget[0] > n) {
                bodyMana = 0; uncertain = false;
                for (int i = 0; i < (int) n; i++) {
                    var scope = new HashMap<>(local); scope.put(index, (double) i);
                    var part = visit(node.children.get("child").getFirst(), scope, budget);
                    bodyMana += part.mana; bodyFocus = Math.max(bodyFocus, part.focus); uncertain |= part.dynamic;
                }
            } else if (indexed && n > 4096) { bodyMana = 0; uncertain = true; }
            focus = overhead + log(1 + n) * bodyFocus;
            if (id.equals("action/while_delayed")) focus += .1 + children.get("condition").focus;
            return new Pricing(uncertain).make(bodyMana, focus, body.cosmetic);
        }
        if (id.equals("action/list")) return all;
        if (id.equals("action/delay") || id.equals("effect/delay")) focus += .15 + h(value(node, id.equals("action/delay") ? "tick" : "time", vars, Double.NaN));
        else if (id.equals("action/move") || id.equals("selector/move")) { /* modifiers already included */ }
        else if (category.equals("selector") || category.equals("predicate") || category.equals("filter")) {
            focus += .1;
            if (category.equals("selector") && !id.equals("selector/self") && !id.equals("selector/compound")) mana += query(node, vars);
            if (id.equals("predicate/depth_test") || id.equals("predicate/ranged_test")) {
                double n = id.equals("predicate/depth_test") ? value(node,"max_depth",vars,Double.NaN) : positions(node,vars);
                mana = n * (.001 + body.mana); focus = .1 + log(1+n)*body.focus;
            }
        } else if (category.equals("motion")) { focus += .05; mana = .001*vars.getOrDefault("__life",0D); }
        else if (category.equals("projectile")) {
            double life = vars.getOrDefault("__life",0D);
            mana = 0;
            for (var entry : children.entrySet()) mana += entry.getValue().mana * (entry.getKey().equals("tick") ? life : 1);
        }
        else if (id.equals("action/random")) focus += .1 + .02 * value(node, "count", vars, Double.NaN);
        else if (id.equals("action/synced_logic")) focus += .05;
        else focus += .1;
        double d = value(node, "damage", vars, Double.NaN);
        switch (id) {
            case "effect/damage" -> mana += damage(d);
            case "effect/knockback" -> mana += force(value(node, "knockback", vars, Double.NaN));
            case "effect/push" -> mana += force(value(node, "speed", vars, Double.NaN));
            case "effect/set_delta" -> mana += force(Math.sqrt(sq(value(node,"x",vars,0))+sq(value(node,"y",vars,0))+sq(value(node,"z",vars,0))));
            case "effect/ignite" -> mana += .02 * value(node, "burnTicks", vars, Double.NaN);
            case "effect/property" -> mana += .02 * value(node, "duration", vars, Double.NaN);
            case "effect/projectile_hit" -> mana += .5125;
            case "action/set_block", "action/set_block_facing" -> mana += .2;
            case "action/remove_block" -> mana += .5;
            case "action/schedule_tick" -> { mana += .1; focus = .15 + h(value(node,"tick",vars,Double.NaN)); }
            case "effect/effect" -> mana += status(node.parameters.getOrDefault("effect",""), value(node,"duration",vars,0),value(node,"amplifier",vars,0));
            case "effect/teleport" -> { return new Pricing(true).make(mana + 2, focus, false); }
            case "action/custom_projectile" -> mana += .5 + .1*sq(value(node,"speed",vars,0));
            case "action/arrow", "action/trident" -> {
                double speed = value(node,"speed",vars,1);
                mana += .5 + .1*sq(speed) + damage(id.equals("action/trident") ? 8 : Math.ceil(2*Math.abs(speed)));
                if (id.equals("action/arrow")) {
                    try {
                        var effects = com.google.gson.JsonParser.parseString(node.parameters.getOrDefault("effects","[]"));
                        for (var entry : effects.getAsJsonArray()) {
                            var effect = entry.getAsJsonObject();
                            mana += status(effect.get("effect").getAsString(), expression(effect.get("duration").getAsString(),vars),
                                    effect.has("amplifier") ? expression(effect.get("amplifier").getAsString(),vars) : 0);
                        }
                    } catch (RuntimeException ignored) {}
                }
            }
            case "action/knock_block" -> mana += 2 + sq(value(node,"speed",vars,0)) + damage(value(node,"maxDamage",vars,0));
            case "action/block_in_range" -> {
                double n = positions(node,vars), delay=value(node,"delayPerBlock",vars,0);
                mana = n*body.mana;
                focus = .2 + log(1+n)*body.focus + (delay>0 ? .15+h(value(node,"range",vars,0)*delay) : 0);
            }
        }
        boolean cosmetic = all.cosmetic && (id.equals("action/move") || id.equals("action/delay") || id.equals("action/variable")
                || id.equals("action/random") || id.equals("action/synced_logic"));
        return pricing.make(finite(mana), finite(focus), cosmetic || visual);
    }
    private static double value(SpellDocument.Node n, String key, Map<String, Double> vars, double fallback) {
        String text = n.parameters.getOrDefault(key, "").trim(); if (text.isBlank()) return fallback;
        return expression(text, vars);
    }
    private static boolean references(SpellDocument.Node node, String variable) {
        var pattern = java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}_])" + java.util.regex.Pattern.quote(variable) + "(?![\\p{L}\\p{N}_])");
        if (node.parameters.values().stream().anyMatch(value -> pattern.matcher(value).find())) return true;
        return node.children.values().stream().flatMap(Collection::stream).anyMatch(child -> references(child, variable));
    }
    private static double expression(String text, Map<String, Double> vars) {
        try {
            double v = new ExpressionBuilder(text).operator(DefaultOperators.OPERATORS).functions(DefaultFunctions.FUNCTIONS)
                    .variables(vars.keySet()).build().setVariables(vars).evaluate();
            return Double.isFinite(v) ? v : Double.NaN;
        } catch (RuntimeException e) { return Double.NaN; }
    }
    private static double query(SpellDocument.Node n, Map<String,Double> vars) {
        double r=value(n,"r",vars,0), size=value(n,"size",vars,0), y=value(n,"y",vars,size);
        return switch (n.template) {
            case "selector/box" -> .05+.002*sq(size)*y;
            case "selector/ball" -> .05+.002*4*Math.PI*r*r*r/3;
            case "selector/cylinder" -> .05+.002*Math.PI*sq(r)*y;
            case "selector/line", "selector/arc" -> (Math.max(0,value(n,"step",vars,0))+1)*(.05+.002*size*size*size);
            default -> 0;
        };
    }
    private static double positions(SpellDocument.Node n, Map<String,Double> vars) {
        double r=value(n,"range",vars,Double.NaN), height=value(n,"height",vars,r);
        if (!valid(r) || !valid(height)) return Double.NaN;
        // Position-independent enclosing grid; actual occupied blocks are not a core parameter.
        return sq(2*Math.ceil(r)+1)*(2*Math.ceil(height)+1);
    }
    private static double status(String effect, double ticks, double amplifier) {
        if (!valid(ticks) || !valid(amplifier)) return Double.NaN;
        boolean instant=effect.equals("minecraft:instant_health") || effect.equals("minecraft:instant_damage");
        return (instant ? 1 : .02*ticks)*sq(amplifier+1);
    }
    private static Quote add(Quote a, Quote b) {
        boolean visual = a.cosmetic&&b.cosmetic;
        return new Quote(a.mana+b.mana, visual ? a.focus+b.focus : adjusted(a)+adjusted(b), visual, a.dynamic||b.dynamic);
    }
    private static double adjusted(Quote q) { return q.cosmetic ? .02*q.focus : q.focus; }
    private static double finite(double v) { return Double.isFinite(v) && v >= 0 ? v : Double.NaN; }
    private static boolean valid(double v) { return Double.isFinite(v) && v >= 0; }
    private static double damage(double d) { return d < 0 ? Double.NaN : .5*d+.0125*d*d; }
    private static double force(double v) { v = Math.abs(v); return .5*v+.5*v*v; }
    private static double sq(double v) { return v*v; }
    private static double log(double v) { return Math.log(v)/Math.log(2); }
    private static double h(double ticks) { return .05*log(1+ticks/20); }
    public static String number(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    public static String manaNumber(double v) { return Long.toString(ResourceBalance.wholeMana(v)); }
    private CostPreview() {}
}
