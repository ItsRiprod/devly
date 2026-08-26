package com.riprod.devly.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riprod.patchly.core.MergeOperator;
import com.riprod.patchly.core.MergeTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PatchMerge {
    private PatchMerge() {
    }

    public static JsonObject merge(JsonObject existing, JsonObject delta, MergeTable table) {
        JsonObject out = existing.deepCopy();
        apply(out, delta, table);
        return out;
    }

    private static void apply(JsonObject out, JsonObject delta, MergeTable table) {
        for (Map.Entry<String, JsonElement> entry : delta.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            MergeOperator op = table.operatorFor(key);
            String suffix = op.suffix();
            if (suffix.isEmpty()) {
                dropVariants(out, key, table);
                JsonElement existingValue = out.get(key);
                if (existingValue != null && existingValue.isJsonObject() && value.isJsonObject()) {
                    apply(existingValue.getAsJsonObject(), value.getAsJsonObject(), table);
                } else {
                    out.add(key, value.deepCopy());
                }
            } else {
                JsonElement existingValue = out.get(key);
                if (existingValue != null && existingValue.isJsonArray() && value.isJsonArray()) {
                    out.add(key, combine(suffix, existingValue.getAsJsonArray(), value.getAsJsonArray(), table));
                } else {
                    out.add(key, value.deepCopy());
                }
            }
        }
    }

    private static void dropVariants(JsonObject out, String baseKey, MergeTable table) {
        List<String> doomed = new ArrayList<>();
        for (String key : out.keySet()) {
            if (key.equals(baseKey)) continue;
            MergeOperator op = table.operatorFor(key);
            if (op.suffix().isEmpty()) continue;
            if (table.baseKey(key, op).equals(baseKey)) doomed.add(key);
        }
        for (String key : doomed) out.remove(key);
    }

    private static JsonArray combine(String suffix, JsonArray existing, JsonArray delta, MergeTable table) {
        return switch (suffix) {
            case "+", "++" -> concat(existing, delta);
            case "-", "--" -> concat(delta, existing);
            case "~" -> positional(existing, delta, table);
            default -> delta.deepCopy();
        };
    }

    private static JsonArray concat(JsonArray first, JsonArray second) {
        JsonArray out = new JsonArray();
        for (JsonElement e : first) out.add(e.deepCopy());
        for (JsonElement e : second) out.add(e.deepCopy());
        return out;
    }

    private static JsonArray positional(JsonArray existing, JsonArray delta, MergeTable table) {
        JsonArray out = new JsonArray();
        int size = Math.max(existing.size(), delta.size());
        for (int i = 0; i < size; i++) {
            if (i >= delta.size()) {
                out.add(existing.get(i).deepCopy());
            } else if (i >= existing.size()) {
                out.add(delta.get(i).deepCopy());
            } else {
                JsonElement e = existing.get(i);
                JsonElement d = delta.get(i);
                if (e.isJsonObject() && d.isJsonObject()) {
                    JsonObject merged = e.getAsJsonObject().deepCopy();
                    apply(merged, d.getAsJsonObject(), table);
                    out.add(merged);
                } else {
                    out.add(d.deepCopy());
                }
            }
        }
        return out;
    }
}
