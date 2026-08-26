package com.riprod.devly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.riprod.devly.convert.BaseResolver;
import com.riprod.devly.convert.Converter;
import com.riprod.devly.convert.Outcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConverterTest {
    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static BaseResolver base(String json) {
        JsonObject base = obj(json);
        return relativeTarget -> base;
    }

    private static BaseResolver noBase() {
        return relativeTarget -> null;
    }

    private static String kind(Outcome outcome) {
        return outcome.output().getAsJsonObject("$Devly").get("kind").getAsString();
    }

    @Test
    void minifyStripsIdenticalFieldsAndTagsPatch() {
        Outcome outcome = Converter.minify("Mana.json",
                obj("{\"startingAmount\":100,\"maxAmount\":100}"),
                base("{\"startingAmount\":0,\"maxAmount\":100}"));

        assertTrue(outcome.changed());
        assertEquals("Mana.patch", outcome.targetRelative());
        assertEquals(100, outcome.output().get("startingAmount").getAsInt());
        assertFalse(outcome.output().has("maxAmount"), "identical field must be dropped");
        assertFalse(outcome.output().has("$Devly"), "a default .patch kind should not be recorded");
    }

    @Test
    void minifyNoBaseNoMetaLeavesUntouched() {
        Outcome outcome = Converter.minify("New.json", obj("{\"a\":1}"), noBase());

        assertFalse(outcome.changed());
        assertNull(outcome.targetRelative());
        assertEquals("skip.noKind", outcome.messageKey());
    }

    @Test
    void minifyNoBaseWithRequiresProducesPut() {
        Outcome outcome = Converter.minify("New.json",
                obj("{\"$Requires\":\"Group:Name\",\"a\":1}"), noBase());

        assertTrue(outcome.changed());
        assertEquals("New.put", outcome.targetRelative());
        assertFalse(outcome.output().has("$Devly"), "a default .put kind should not be recorded");
        assertTrue(outcome.output().has("$Requires"));
        assertEquals(1, outcome.output().get("a").getAsInt());
    }

    @Test
    void minifyHonorsRecordedKindOverDefault() {
        Outcome outcome = Converter.minify("Mana.json",
                obj("{\"$Devly\":{\"kind\":\".put\"},\"a\":1}"),
                base("{\"a\":0}"));

        assertTrue(outcome.changed());
        assertEquals("Mana.put", outcome.targetRelative());
        assertEquals(".put", kind(outcome));
    }

    @Test
    void minifyRecordedEnvironmentKindIsRejected() {
        Outcome outcome = Converter.minify("Globals.json",
                obj("{\"$Devly\":{\"kind\":\".vars\"},\"a\":1}"),
                base("{\"a\":0}"));

        assertFalse(outcome.changed());
        assertEquals("skip.noKind", outcome.messageKey());
    }

    @Test
    void minifyRespectsIgnoreMarker() {
        Outcome outcome = Converter.minify("Mana.json",
                obj("{\"$Devly\":{\"ignore\":true},\"a\":1}"),
                base("{\"a\":0}"));

        assertFalse(outcome.changed());
        assertEquals("skip.ignored", outcome.messageKey());
    }

    @Test
    void minifySkipsExistingPatchSources() {
        Outcome outcome = Converter.minify("Mana.patch", obj("{\"a\":1}"), base("{\"a\":0}"));

        assertFalse(outcome.changed());
        assertEquals("skip.alreadySource", outcome.messageKey());
    }

    @Test
    void minifySkipsVarsSources() {
        Outcome outcome = Converter.minify("Globals.vars", obj("{\"Mult\":2}"), noBase());

        assertFalse(outcome.changed());
        assertEquals("skip.alreadySource", outcome.messageKey());
    }
}
