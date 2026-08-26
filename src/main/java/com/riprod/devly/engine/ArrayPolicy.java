package com.riprod.devly.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.riprod.patchly.core.JsonDeepMerge;
import com.riprod.patchly.core.MergeTable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArrayPolicy {
    private static final List<String> IDENTITY_PREFERENCE = List.of("Id", "Name", "Key", "Type", "Slot");
    private static final String CODE_REPLACE = "replace";
    private static final String CODE_APPEND = "+";
    private static final String CODE_APPEND_DUP = "++";
    private static final String CODE_PREPEND = "-";
    private static final String CODE_PREPEND_DUP = "--";
    private static final String CODE_MATCH = "match";
    private static final String CODE_POSITIONAL = "~";

    // each candidate builder just constructs a plausible patch fragment; verify() re-merges it
    // through patchly's real engine and is the sole arbiter of whether it reproduces the edit.
    // adding an operator is one builder + one entry here; order is the preference on a tie.
    private static final List<Strategy> STRATEGIES = List.of(
            new Strategy(CODE_APPEND, ArrayPolicy::buildAppend),
            new Strategy(CODE_APPEND_DUP, ArrayPolicy::buildAppendDup),
            new Strategy(CODE_PREPEND, ArrayPolicy::buildPrepend),
            new Strategy(CODE_PREPEND_DUP, ArrayPolicy::buildPrependDup),
            new Strategy(CODE_MATCH, ArrayPolicy::buildMatch),
            new Strategy(CODE_POSITIONAL, ArrayPolicy::buildPositional));

    private final Map<String, String> recorded;
    private final Map<String, String> updated = new LinkedHashMap<>();
    private final MergeTable table;

    public ArrayPolicy(Map<String, String> recorded) {
        this(recorded, JsonDeepMerge.activeTable());
    }

    public ArrayPolicy(Map<String, String> recorded, MergeTable table) {
        this.recorded = recorded;
        this.table = table;
    }

    public Map<String, String> updatedDecisions() {
        return updated;
    }

    public void emit(JsonObject out, String key, String path, JsonArray base, JsonArray edited) {
        Decision decision = decide(recorded.get(path), key, base, edited);
        out.add(decision.emitKey(), decision.emitValue());
        updated.put(path, decision.code());
    }

    private Decision decide(String forced, String key, JsonArray base, JsonArray edited) {
        if (forced != null) {
            Decision forcedDecision = buildForced(forced, key, base, edited);
            if (forcedDecision != null && verify(key, forcedDecision, base, edited)) return forcedDecision;
        }
        for (Strategy strategy : STRATEGIES) {
            Decision candidate = strategy.builder().build(this, key, base, edited);
            if (candidate != null && verify(key, candidate, base, edited)) return candidate;
        }
        return replace(key, edited);
    }

    private Decision buildForced(String code, String key, JsonArray base, JsonArray edited) {
        if (CODE_REPLACE.equals(code)) return replace(key, edited);
        String normalized = code.startsWith(CODE_MATCH + ":") ? CODE_MATCH : code;
        for (Strategy strategy : STRATEGIES) {
            if (strategy.code().equals(normalized)) return strategy.builder().build(this, key, base, edited);
        }
        return null;
    }

    private Decision buildAppend(String key, JsonArray base, JsonArray edited) {
        if (edited.size() <= base.size()) return null;
        return new Decision(key + CODE_APPEND, slice(edited, base.size(), edited.size()), CODE_APPEND);
    }

    private Decision buildAppendDup(String key, JsonArray base, JsonArray edited) {
        if (edited.size() <= base.size()) return null;
        return new Decision(key + CODE_APPEND_DUP, slice(edited, base.size(), edited.size()), CODE_APPEND_DUP);
    }

    private Decision buildPrepend(String key, JsonArray base, JsonArray edited) {
        if (edited.size() <= base.size()) return null;
        return new Decision(key + CODE_PREPEND, slice(edited, 0, edited.size() - base.size()), CODE_PREPEND);
    }

    private Decision buildPrependDup(String key, JsonArray base, JsonArray edited) {
        if (edited.size() <= base.size()) return null;
        return new Decision(key + CODE_PREPEND_DUP, slice(edited, 0, edited.size() - base.size()), CODE_PREPEND_DUP);
    }

    private Decision buildPositional(String key, JsonArray base, JsonArray edited) {
        if (edited.size() < base.size() || base.isEmpty()) return null;
        JsonArray elements = new JsonArray();
        int unchanged = 0;
        for (int i = 0; i < base.size(); i++) {
            JsonElement baseElem = base.get(i);
            JsonElement editedElem = edited.get(i);
            if (baseElem.equals(editedElem)) {
                if (!baseElem.isJsonObject()) return null;
                elements.add(new JsonObject());
                unchanged++;
            } else if (baseElem.isJsonObject() && editedElem.isJsonObject()) {
                elements.add(new JsonDiff(new ArrayPolicy(Map.of(), table))
                        .diff(baseElem.getAsJsonObject(), editedElem.getAsJsonObject()));
            } else {
                elements.add(editedElem);
            }
        }
        for (int i = base.size(); i < edited.size(); i++) {
            elements.add(edited.get(i));
        }
        if (unchanged == 0) return null;
        return new Decision(key + CODE_POSITIONAL, elements, CODE_POSITIONAL);
    }

    private Decision buildMatch(String key, JsonArray base, JsonArray edited) {
        if (base.size() != edited.size() || base.isEmpty()) return null;
        JsonArray elements = new JsonArray();
        for (int i = 0; i < base.size(); i++) {
            JsonElement baseElem = base.get(i);
            JsonElement editedElem = edited.get(i);
            if (baseElem.equals(editedElem)) continue;
            if (!baseElem.isJsonObject() || !editedElem.isJsonObject()) return null;
            JsonObject baseObj = baseElem.getAsJsonObject();
            JsonObject editedObj = editedElem.getAsJsonObject();
            String field = identityField(baseObj, editedObj, base);
            if (field == null) return null;
            elements.add(matchElement(baseObj, editedObj, field));
        }
        if (elements.isEmpty()) return null;
        return new Decision(key, elements, CODE_MATCH);
    }

    private JsonObject matchElement(JsonObject baseElem, JsonObject editedElem, String field) {
        JsonObject elementPatch = new JsonDiff(new ArrayPolicy(Map.of(), table)).diff(baseElem, editedElem);
        elementPatch.add("$Match", new JsonPrimitive(field));
        elementPatch.add(field, editedElem.get(field));
        return elementPatch;
    }

    private static String identityField(JsonObject baseElem, JsonObject editedElem, JsonArray base) {
        for (String candidate : IDENTITY_PREFERENCE) {
            if (isIdentity(candidate, baseElem, editedElem, base)) return candidate;
        }
        for (String candidate : baseElem.keySet()) {
            if (isIdentity(candidate, baseElem, editedElem, base)) return candidate;
        }
        return null;
    }

    private static boolean isIdentity(String field, JsonObject baseElem, JsonObject editedElem, JsonArray base) {
        if (!baseElem.has(field) || !editedElem.has(field)) return false;
        JsonElement value = baseElem.get(field);
        if (!value.isJsonPrimitive()) return false;
        if (!value.equals(editedElem.get(field))) return false;
        int matches = 0;
        for (JsonElement element : base) {
            if (element.isJsonObject() && value.equals(element.getAsJsonObject().get(field))) matches++;
        }
        return matches == 1;
    }

    private static JsonArray slice(JsonArray array, int from, int to) {
        JsonArray out = new JsonArray();
        for (int i = from; i < to; i++) out.add(array.get(i));
        return out;
    }

    private static Decision replace(String key, JsonArray edited) {
        return new Decision(key, edited, CODE_REPLACE);
    }

    private boolean verify(String key, Decision decision, JsonArray base, JsonArray edited) {
        JsonObject wrappedBase = new JsonObject();
        wrappedBase.add(key, base.deepCopy());
        JsonObject wrappedPatch = new JsonObject();
        wrappedPatch.add(decision.emitKey(), decision.emitValue().deepCopy());
        JsonObject merged = JsonDeepMerge.merge(wrappedBase, wrappedPatch, table);
        return edited.equals(merged.get(key));
    }

    @FunctionalInterface
    private interface Builder {
        Decision build(ArrayPolicy self, String key, JsonArray base, JsonArray edited);
    }

    private record Strategy(String code, Builder builder) {
    }

    private record Decision(String emitKey, JsonElement emitValue, String code) {
    }
}
