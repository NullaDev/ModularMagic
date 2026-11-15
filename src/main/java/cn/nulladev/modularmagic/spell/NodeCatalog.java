package cn.nulladev.modularmagic.spell;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Explicit UI schemas for the pinned, unmodified L2Magic engine. */
public final class NodeCatalog {
    private static final Map<String, NodeTemplate> TEMPLATES = new LinkedHashMap<>();
    private static final Map<String, Category> CATEGORIES = new LinkedHashMap<>();

    public record Category(String color, String code) {
        public int rgb() { return Integer.parseInt(color.substring(1), 16); }
    }

    static {
        try (var stream = Objects.requireNonNull(NodeCatalog.class.getResourceAsStream(
                "/data/modularmagic/node_templates.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            var gson = new Gson();
            json.getAsJsonObject("categories").entrySet().forEach(e ->
                    CATEGORIES.put(e.getKey(), gson.fromJson(e.getValue(), Category.class)));
            json.getAsJsonArray("templates").forEach(e -> {
                var template = gson.fromJson(e, NodeTemplate.class);
                if (TEMPLATES.put(template.id(), template) != null) throw new IllegalStateException("Duplicate template");
            });
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private NodeCatalog() {}
    public static Collection<NodeTemplate> all() { return java.util.Collections.unmodifiableCollection(TEMPLATES.values()); }
    public static NodeTemplate get(String id) {
        var result = TEMPLATES.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown node: " + id);
        return result;
    }
    public static Category category(String id) { return Objects.requireNonNull(CATEGORIES.get(id)); }
}
