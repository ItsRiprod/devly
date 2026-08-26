package com.riprod.devly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.riprod.devly.convert.Syncback;
import com.riprod.patchly.core.JsonDeepMerge;
import com.riprod.patchly.core.MergeTable;
import com.riprod.patchly.core.directive.PatchContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncbackTest {
    private static final MergeTable TABLE = JsonDeepMerge.activeTable();

    private static JsonObject obj(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject composed(JsonObject base, JsonObject patch) {
        return JsonDeepMerge.merge(base, patch, TABLE, PatchContext.ALWAYS);
    }

    private static Syncback.Result compute(JsonObject base, JsonObject patch, JsonObject edited) {
        return Syncback.compute(base, patch, edited, TABLE, PatchContext.ALWAYS);
    }

    @Test
    void unchangedComposedOutputYieldsNoWrite() {
        JsonObject base = obj("{\"a\":0,\"Tags\":[\"x\"]}");
        JsonObject patch = obj("{\"$Requires\":\"Group:Name\",\"a\":1,\"Tags+\":[\"y\"]}");

        Syncback.Result result = compute(base, patch, composed(base, patch));

        assertEquals(Syncback.Status.UNCHANGED, result.status());
        assertNull(result.candidate());
    }

    @Test
    void scalarEditKeepsExistingMatchDirective() {
        JsonObject base = obj("{\"name\":\"old\",\"Mods\":[{\"Id\":\"x\",\"v\":1}]}");
        JsonObject patch = obj("{\"Mods\":[{\"$Match\":\"Id\",\"Id\":\"x\",\"v\":5}]}");
        JsonObject edited = composed(base, patch);
        edited.addProperty("name", "new");

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        assertEquals("new", result.candidate().get("name").getAsString());
        assertEquals("Id", result.candidate().getAsJsonArray("Mods")
                .get(0).getAsJsonObject().get("$Match").getAsString());
        assertEquals(edited, composed(base, result.candidate()));
    }

    @Test
    void editorAppendAccumulatesOntoExistingAppend() {
        JsonObject base = obj("{\"Tags\":[\"x\"]}");
        JsonObject patch = obj("{\"Tags+\":[\"y\"]}");
        JsonObject edited = composed(base, patch);
        edited.getAsJsonArray("Tags").add("z");

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        assertEquals(obj("{\"Tags+\":[\"y\",\"z\"]}"), result.candidate());
        assertEquals(edited, composed(base, result.candidate()));
    }

    @Test
    void wholesaleArrayEditDropsStaleOperatorVariant() {
        JsonObject base = obj("{\"Tags\":[\"x\"]}");
        JsonObject patch = obj("{\"Tags+\":[\"y\"]}");
        JsonObject edited = obj("{\"Tags\":[\"z\"]}");

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        assertEquals(obj("{\"Tags\":[\"z\"]}"), result.candidate());
        assertEquals(edited, composed(base, result.candidate()));
    }

    @Test
    void rootMetaKeysSurviveUntouched() {
        JsonObject base = obj("{\"a\":0}");
        JsonObject patch = obj("{\"$Requires\":\"Group:Name\",\"$Priority\":5,\"a\":1}");
        JsonObject edited = composed(base, patch);
        edited.addProperty("a", 2);

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        assertEquals("Group:Name", result.candidate().get("$Requires").getAsString());
        assertEquals(5, result.candidate().get("$Priority").getAsInt());
        assertEquals(2, result.candidate().get("a").getAsInt());
    }

    @Test
    void importDirectiveRefusesSync() {
        JsonObject base = obj("{\"a\":0}");
        JsonObject patch = obj("{\"Field\":{\"$Import\":\"Some_Asset\"}}");
        JsonObject edited = obj("{\"a\":2,\"Field\":{\"imported\":true}}");

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.UNSUPPORTED, result.status());
        assertNull(result.candidate());
    }

    @Test
    void computeExpressionRefusesSync() {
        JsonObject base = obj("{\"Amount\":1}");
        JsonObject patch = obj("{\"Amount#\":\"$Mult * 2\"}");
        JsonObject edited = obj("{\"Amount\":4}");

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.UNSUPPORTED, result.status());
    }

    @Test
    void resyncOfOwnResultIsUnchanged() {
        JsonObject base = obj("{\"name\":\"old\",\"Tags\":[\"x\"]}");
        JsonObject patch = obj("{\"Tags+\":[\"y\"]}");
        JsonObject edited = composed(base, patch);
        edited.addProperty("name", "new");

        Syncback.Result first = compute(base, patch, edited);
        assertEquals(Syncback.Status.WRITE, first.status());

        JsonObject recomposed = composed(base, first.candidate());
        Syncback.Result second = compute(base, first.candidate(), recomposed);
        assertEquals(Syncback.Status.UNCHANGED, second.status());
    }

    @Test
    void putWithoutBaseSyncsAgainstSeed() {
        JsonObject seed = new JsonObject();
        JsonObject patch = obj("{\"a\":1}");
        JsonObject edited = obj("{\"a\":1,\"b\":2}");

        Syncback.Result result = compute(seed, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        assertEquals(obj("{\"a\":1,\"b\":2}"), result.candidate());
    }

    @Test
    void nestedObjectEditRecursesWithoutClobberingSiblings() {
        JsonObject base = obj("{\"Armor\":{\"Slot\":\"Head\",\"Resist\":1,\"Mana\":5}}");
        JsonObject patch = obj("{\"Armor\":{\"Resist\":3}}");
        JsonObject edited = composed(base, patch);
        edited.getAsJsonObject("Armor").addProperty("Mana", 9);

        Syncback.Result result = compute(base, patch, edited);

        assertEquals(Syncback.Status.WRITE, result.status());
        JsonObject armor = result.candidate().getAsJsonObject("Armor");
        assertEquals(3, armor.get("Resist").getAsInt());
        assertEquals(9, armor.get("Mana").getAsInt());
        assertTrue(!armor.has("Slot"), "untouched base field must not be pinned into the patch");
        assertEquals(edited, composed(base, result.candidate()));
    }
}
