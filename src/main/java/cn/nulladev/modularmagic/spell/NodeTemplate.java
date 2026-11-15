package cn.nulladev.modularmagic.spell;

import java.util.List;

public record NodeTemplate(String id, String category, String nativeType, String item, String name,
                           List<Field> fields, List<ChildSlot> slots) {
    public record Field(String key, String kind, boolean optional, String initial) {}
    public record ChildSlot(String key, String category, boolean list, boolean optional) {}

    public String translationKey() { return "item.modularmagic." + item; }

    public ChildSlot slot(String key) {
        return slots.stream().filter(s -> s.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown child slot: " + key));
    }

    public Field field(String key) {
        return fields.stream().filter(f -> f.key().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown parameter: " + key));
    }
}
