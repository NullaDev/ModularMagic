package cn.nulladev.modularmagic.spell;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class SpellDocumentTest {
    private SpellDocument document() { return SpellDocument.create("root/instant"); }

    @Test void catalogCoversEveryRegisteredKindAndAuxiliaryTemplate() {
        assertEquals(100, NodeCatalog.all().size());
        assertEquals(88, NodeCatalog.all().stream().filter(t -> t.nativeType().startsWith("l2magic:")).count());
        assertEquals(100, NodeCatalog.all().stream().map(NodeTemplate::item).distinct().count());
        for (var template : NodeCatalog.all()) for (var slot : template.slots()) {
            assertNotNull(NodeCatalog.category(slot.category()));
            assertTrue(NodeCatalog.all().stream().anyMatch(t -> t.category().equals(slot.category())));
        }
    }

    @Test void emptyRootIsIncompleteAndRoundTripsWithoutInventingNodes() {
        var doc = document();
        assertFalse(doc.structuralErrors().isEmpty());
        var restored = SpellDocument.fromJson(doc.toJson());
        assertEquals(doc.toJson(), restored.toJson());
        assertEquals(1, restored.nodes().size());
        assertTrue(restored.root.children.get("action").isEmpty());
    }

    @Test void partialTreeAndInvalidParameterTextSurviveSaving() {
        var doc = document();
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        doc.setParameters(loop.id, Map.of("step", "3+", "index", "i"));
        var restored = SpellDocument.fromJson(doc.toJson());
        assertEquals("3+", restored.find(loop.id).parameters.get("step"));
        assertTrue(restored.find(loop.id).children.get("child").isEmpty());
        assertEquals(loop.id, restored.root.children.get("action").getFirst().id);
    }

    @Test void structurallyCompleteSpellAllowsOptionalConditionToStayEmpty() {
        var doc = document();
        doc.insert(doc.root.id, "trigger_type", 0, "trigger/self_pos");
        doc.insert(doc.root.id, "action", 0, "action/sound");
        assertTrue(doc.structuralErrors().isEmpty());
        assertTrue(doc.root.children.get("condition").isEmpty());
    }

    @Test void slotTypeIsEnforcedBeforeInsertion() {
        var doc = document();
        assertThrows(IllegalArgumentException.class, () -> doc.insert(doc.root.id, "action", 0, "effect/damage"));
        assertEquals(1, doc.nodes().size());
    }

    @Test void occupiedSingleSlotCannotBeOverwritten() {
        var doc = document();
        var first = doc.insert(doc.root.id, "action", 0, "action/sound");
        assertThrows(IllegalArgumentException.class, () -> doc.insert(doc.root.id, "action", 0, "action/particle"));
        assertEquals(first.id, doc.root.children.get("action").getFirst().id);
    }

    @Test void listRemovalCompactsOrderAndKeepsStableNodeIdentities() {
        var doc = document();
        var list = doc.insert(doc.root.id, "action", 0, "action/list");
        var a = doc.insert(list.id, "children", 0, "action/sound");
        var b = doc.insert(list.id, "children", 1, "action/particle");
        var c = doc.insert(list.id, "children", 2, "action/sound");
        assertEquals("action/particle", doc.remove(b.id));
        assertEquals(List.of(a.id, c.id), list.children.get("children").stream().map(n -> n.id).toList());
        doc.insert(list.id, "children", 2, "action/sound");
        assertEquals(3, list.children.get("children").size());
    }

    @Test void parentCannotMoveUntilItsLastChildHasBeenRemoved() {
        var doc = document();
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        var sound = doc.insert(loop.id, "child", 0, "action/sound");
        assertThrows(IllegalArgumentException.class, () -> doc.remove(loop.id));
        assertEquals("action/sound", doc.remove(sound.id));
        assertEquals("action/iterate", doc.remove(loop.id));
        assertThrows(IllegalArgumentException.class, () -> doc.remove(doc.root.id));
    }

    @Test void nodeItemIdentityDoesNotRetainItsOldParameters() {
        var doc = document();
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        doc.setParameters(loop.id, Map.of("step", "99", "index", "old"));
        var template = doc.remove(loop.id);
        var replacement = doc.insert(doc.root.id, "action", 0, template);
        assertNotEquals(loop.id, replacement.id);
        assertEquals("3", replacement.parameters.get("step"));
        assertEquals("", replacement.parameters.get("index"));
    }

    @Test void unsupportedAndDuplicateNodeIdentitiesCannotBeImported() {
        var doc = document();
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        var sound = doc.insert(loop.id, "child", 0, "action/sound");
        assertThrows(IllegalArgumentException.class, () -> SpellDocument.fromJson(doc.toJson().replace(sound.id, loop.id)));
        assertThrows(IllegalArgumentException.class, () -> SpellDocument.fromJson(doc.toJson().replace("action/sound", "action/unknown")));
        assertThrows(IllegalArgumentException.class, () -> SpellDocument.fromJson(doc.toJson().replace("\"format\":1", "\"format\":2")));
    }

    @Test void importingWrongCategoryOrMultipleFixedChildrenIsRejected() {
        var doc = document();
        doc.insert(doc.root.id, "action", 0, "action/sound");
        assertThrows(IllegalArgumentException.class, () -> SpellDocument.fromJson(doc.toJson().replace("action/sound", "effect/damage")));
        var json = JsonParser.parseString(doc.toJson()).getAsJsonObject();
        var array = json.getAsJsonObject("root").getAsJsonObject("children").getAsJsonArray("action");
        var extra = array.get(0).deepCopy().getAsJsonObject(); extra.addProperty("id", UUID.randomUUID().toString()); array.add(extra);
        assertThrows(IllegalArgumentException.class, () -> SpellDocument.fromJson(json.toString()));
    }

    @Test void parameterWhitelistAndLengthLimitsAreAtomic() {
        var doc = document();
        var loop = doc.insert(doc.root.id, "action", 0, "action/iterate");
        var before = doc.toJson();
        assertThrows(IllegalArgumentException.class, () -> doc.setParameters(loop.id, Map.of("step", "4", "unknown", "5")));
        assertEquals(before, doc.toJson());
        assertThrows(IllegalArgumentException.class, () -> doc.setParameters(loop.id, Map.of("step", "x".repeat(SpellDocument.MAX_FIELD + 1))));
        assertEquals(before, doc.toJson());
    }

    @Test void recursiveAndListLimitsCannotBeBypassed() {
        var doc = document(); var parent = doc.root;
        for (int i = 0; i < SpellDocument.MAX_DEPTH; i++) parent = doc.insert(parent.id, i == 0 ? "action" : "child", 0, "action/delay");
        final var last = parent;
        assertThrows(IllegalArgumentException.class, () -> doc.insert(last.id, "child", 0, "action/sound"));
        assertEquals(doc.toJson(), SpellDocument.fromJson(doc.toJson()).toJson());
        var other = document(); var list = other.insert(other.root.id, "action", 0, "action/list");
        for (int i = 0; i < SpellDocument.MAX_LIST; i++) other.insert(list.id, "children", i, "action/sound");
        assertThrows(IllegalArgumentException.class, () -> other.insert(list.id, "children", SpellDocument.MAX_LIST, "action/sound"));
    }
}
