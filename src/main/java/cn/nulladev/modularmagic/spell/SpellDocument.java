package cn.nulladev.modularmagic.spell;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

/** Owns one physical item per node, including the root, while it is inside a core. */
public final class SpellDocument {
    public static final int MAX_NODES = 256;
    public static final int MAX_DEPTH = 32;
    public static final int MAX_LIST = 64;
    public static final int MAX_FIELD = 2048;
    public static final int MAX_JSON = 131072;
    public final Node root;

    public static final class Node {
        public final String id;
        public final String template;
        public final Map<String, String> parameters = new LinkedHashMap<>();
        public final Map<String, List<Node>> children = new LinkedHashMap<>();

        private Node(String id, String template) {
            this.id = id;
            this.template = template;
        }

        public NodeTemplate schema() { return NodeCatalog.get(template); }
        public boolean hasChildren() { return children.values().stream().anyMatch(l -> !l.isEmpty()); }
    }

    private SpellDocument(Node root) { this.root = root; }

    public static SpellDocument create(String rootTemplate) {
        if (!NodeCatalog.get(rootTemplate).category().equals("root"))
            throw new IllegalArgumentException("A spell requires a root item");
        return new SpellDocument(createNode(rootTemplate));
    }

    private static Node createNode(String template) {
        var node = new Node(UUID.randomUUID().toString(), template);
        node.schema().fields().forEach(f -> node.parameters.put(f.key(), f.initial()));
        node.schema().slots().forEach(s -> node.children.put(s.key(), new ArrayList<>()));
        return node;
    }

    public Node find(String id) {
        return nodes().stream().filter(n -> n.id.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node no longer exists"));
    }

    public List<Node> nodes() {
        var result = new ArrayList<Node>();
        collect(root, result);
        return result;
    }

    private static void collect(Node node, List<Node> result) {
        result.add(node);
        node.children.values().forEach(list -> list.forEach(child -> collect(child, result)));
    }

    public Node insert(String parentId, String key, int index, String template) {
        var parent = find(parentId);
        var slot = parent.schema().slot(key);
        if (!slot.category().equals(NodeCatalog.get(template).category()))
            throw new IllegalArgumentException("This slot needs " + slot.category());
        var list = parent.children.get(key);
        if (index != list.size() || (!slot.list() && !list.isEmpty()) || list.size() >= MAX_LIST)
            throw new IllegalArgumentException("The child slot changed or is full");
        if (nodes().size() >= MAX_NODES || depth(root, parentId, 0) >= MAX_DEPTH)
            throw new IllegalArgumentException("Spell size limit reached");
        var child = createNode(template);
        list.add(child);
        return child;
    }

    private static int depth(Node node, String id, int level) {
        if (node.id.equals(id)) return level;
        for (var list : node.children.values()) for (var child : list) {
            int found = depth(child, id, level + 1);
            if (found >= 0) return found;
        }
        return -1;
    }

    public String remove(String id) {
        var node = find(id);
        if (node == root) throw new IllegalArgumentException("Take out the core to move the root");
        if (node.hasChildren()) throw new IllegalArgumentException("Remove child items first");
        for (var parent : nodes()) for (var list : parent.children.values()) {
            if (list.remove(node)) return node.template;
        }
        throw new IllegalArgumentException("Node no longer exists");
    }

    public void setParameters(String id, Map<String, String> values) {
        var node = find(id);
        values.forEach((key, value) -> {
            node.schema().field(key);
            if (value == null || value.length() > MAX_FIELD) throw new IllegalArgumentException("Parameter too long");
        });
        node.parameters.putAll(values);
    }

    public List<String> structuralErrors() {
        var errors = new ArrayList<String>();
        inspect(root, "root", errors);
        return errors;
    }

    private static void inspect(Node node, String path, List<String> errors) {
        for (var field : node.schema().fields()) {
            if (!field.optional() && node.parameters.getOrDefault(field.key(), "").isBlank())
                errors.add(path + "." + field.key() + ": required parameter");
        }
        for (var slot : node.schema().slots()) {
            var list = node.children.get(slot.key());
            if (!slot.optional() && !slot.list() && list.isEmpty())
                errors.add(path + "." + slot.key() + ": needs " + slot.category());
            for (int i = 0; i < list.size(); i++) inspect(list.get(i), path + "." + slot.key() + "[" + i + "]", errors);
        }
    }

    public String toJson() {
        var json = new JsonObject();
        json.addProperty("format", 1);
        json.add("root", writeNode(root));
        var result = json.toString();
        if (result.length() > MAX_JSON) throw new IllegalArgumentException("Spell document is too large");
        return result;
    }

    private static JsonObject writeNode(Node node) {
        var json = new JsonObject();
        json.addProperty("id", node.id);
        json.addProperty("template", node.template);
        var parameters = new JsonObject();
        node.parameters.forEach(parameters::addProperty);
        json.add("parameters", parameters);
        var children = new JsonObject();
        node.children.forEach((key, list) -> {
            var array = new JsonArray();
            list.forEach(child -> array.add(writeNode(child)));
            children.add(key, array);
        });
        json.add("children", children);
        return json;
    }

    public static SpellDocument fromJson(String text) {
        if (text.length() > MAX_JSON) throw new IllegalArgumentException("Spell document is too large");
        var json = JsonParser.parseString(text).getAsJsonObject();
        if (json.get("format").getAsInt() != 1) throw new IllegalArgumentException("Unsupported spell format");
        var root = readNode(json.getAsJsonObject("root"), 0, new HashSet<>());
        if (!root.schema().category().equals("root")) throw new IllegalArgumentException("Missing spell root");
        return new SpellDocument(root);
    }

    private static Node readNode(JsonObject json, int depth, Set<String> ids) {
        if (depth > MAX_DEPTH || ids.size() >= MAX_NODES) throw new IllegalArgumentException("Spell size limit reached");
        var id = json.get("id").getAsString();
        if (id.length() > 64 || !ids.add(id)) throw new IllegalArgumentException("Duplicate or invalid node identity");
        var node = new Node(id, json.get("template").getAsString());
        node.schema();
        var parameters = json.getAsJsonObject("parameters");
        for (var entry : parameters.entrySet()) {
            node.schema().field(entry.getKey());
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Draft parameters must be strings");
            var value = entry.getValue().getAsString();
            if (value.length() > MAX_FIELD) throw new IllegalArgumentException("Parameter too long");
            node.parameters.put(entry.getKey(), value);
        }
        var children = json.getAsJsonObject("children");
        for (var key : children.keySet()) node.schema().slot(key);
        for (var slot : node.schema().slots()) {
            var list = new ArrayList<Node>();
            if (children.has(slot.key())) for (var entry : children.getAsJsonArray(slot.key())) {
                var child = readNode(entry.getAsJsonObject(), depth + 1, ids);
                if (!child.schema().category().equals(slot.category())) throw new IllegalArgumentException("Wrong child category");
                list.add(child);
            }
            if ((!slot.list() && list.size() > 1) || list.size() > MAX_LIST)
                throw new IllegalArgumentException("Too many slot members");
            node.children.put(slot.key(), list);
        }
        return node;
    }
}
