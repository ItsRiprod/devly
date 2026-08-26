package com.riprod.devly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.riprod.devly.engine.ArrayPolicy;
import com.riprod.devly.engine.JsonDiff;
import com.riprod.patchly.core.JsonDeepMerge;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonDiffRoundTripTest {

    private void roundTrip(String base, String edited) {
        JsonObject baseObj = JsonParser.parseString(base).getAsJsonObject();
        JsonObject editedObj = JsonParser.parseString(edited).getAsJsonObject();
        JsonObject patch = new JsonDiff(new ArrayPolicy(Map.of())).diff(baseObj, editedObj);
        JsonObject merged = JsonDeepMerge.merge(baseObj, patch, JsonDeepMerge.activeTable());
        assertEquals(editedObj, merged, "diff did not reproduce edited; patch=" + patch);
    }

    @Test
    void scalarChange() {
        roundTrip("{\"startingAmount\":0,\"maxAmount\":100}", "{\"startingAmount\":100,\"maxAmount\":100}");
    }

    @Test
    void nestedObjectChangeAndAdd() {
        roundTrip("{\"Armor\":{\"Slot\":\"Head\",\"Resist\":1}}",
                "{\"Armor\":{\"Slot\":\"Head\",\"Resist\":3,\"Mana\":5}}");
    }

    @Test
    void keyRemoval() {
        roundTrip("{\"a\":1,\"b\":2}", "{\"a\":1}");
    }

    @Test
    void arrayAppend() {
        roundTrip("{\"Tags\":[\"a\",\"b\"]}", "{\"Tags\":[\"a\",\"b\",\"c\"]}");
    }

    @Test
    void arrayMatchSingleElement() {
        roundTrip("{\"Mods\":[{\"Id\":\"x\",\"v\":1},{\"Id\":\"y\",\"v\":2}]}",
                "{\"Mods\":[{\"Id\":\"x\",\"v\":1},{\"Id\":\"y\",\"v\":9}]}");
    }

    @Test
    void arrayMatchMultipleElements() {
        roundTrip("{\"Mods\":[{\"Id\":\"x\",\"v\":1},{\"Id\":\"y\",\"v\":2},{\"Id\":\"z\",\"v\":3}]}",
                "{\"Mods\":[{\"Id\":\"x\",\"v\":8},{\"Id\":\"y\",\"v\":2},{\"Id\":\"z\",\"v\":9}]}");
    }

    @Test
    void arrayPrepend() {
        roundTrip("{\"Tags\":[\"b\",\"c\"]}", "{\"Tags\":[\"a\",\"b\",\"c\"]}");
    }

    @Test
    void arrayShrinkReplaces() {
        roundTrip("{\"Tags\":[\"a\",\"b\",\"c\"]}", "{\"Tags\":[\"a\"]}");
    }

    @Test
    void putWholeBodyFromEmptyBase() {
        roundTrip("{}", "{\"startingAmount\":100,\"Tags\":[\"a\"]}");
    }

    @Test
    void noChange() {
        roundTrip("{\"a\":1,\"nested\":{\"b\":2}}", "{\"a\":1,\"nested\":{\"b\":2}}");
    }

    @Test
    void arrayAppendDuplicate() {
        roundTrip("{\"Tags\":[\"a\"]}", "{\"Tags\":[\"a\",\"a\"]}");
    }

    @Test
    void arrayPrependDuplicate() {
        roundTrip("{\"Tags\":[\"a\",\"b\"]}", "{\"Tags\":[\"b\",\"a\",\"b\"]}");
    }

    @Test
    void arrayPositionalNoIdentityField() {
        roundTrip("{\"Mods\":[{\"n\":1},{\"n\":2}]}", "{\"Mods\":[{\"n\":1},{\"n\":3}]}");
    }

    @Test
    void arrayPositionalMixedScalarChange() {
        roundTrip("{\"Mods\":[{\"n\":1},{\"n\":2},{\"n\":3}]}",
                "{\"Mods\":[{\"n\":1},{\"n\":9},{\"n\":3}]}");
    }
}
