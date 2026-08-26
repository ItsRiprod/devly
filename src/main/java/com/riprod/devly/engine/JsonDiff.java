package com.riprod.devly.engine;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Map;

public final class JsonDiff {
    private final ArrayPolicy policy;

    public JsonDiff(ArrayPolicy policy) {
        this.policy = policy;
    }

    public JsonObject diff(JsonObject base, JsonObject edited) {
        return diffObject(base, edited, "");
    }

    private JsonObject diffObject(JsonObject base, JsonObject edited, String path) {
        JsonObject out = new JsonObject();
        for (String key : base.keySet()) {
            if (!edited.has(key)) out.add(key, JsonNull.INSTANCE);
        }
        for (Map.Entry<String, JsonElement> entry : edited.entrySet()) {
            String key = entry.getKey();
            JsonElement editedValue = entry.getValue();
            if (!base.has(key)) {
                out.add(key, editedValue);
                continue;
            }
            JsonElement baseValue = base.get(key);
            if (baseValue.equals(editedValue)) continue;
            String childPath = path.isEmpty() ? key : path + "." + key;
            if (baseValue.isJsonObject() && editedValue.isJsonObject()) {
                JsonObject sub = diffObject(baseValue.getAsJsonObject(), editedValue.getAsJsonObject(), childPath);
                if (!sub.keySet().isEmpty()) out.add(key, sub);
            } else if (baseValue.isJsonArray() && editedValue.isJsonArray()) {
                policy.emit(out, key, childPath, baseValue.getAsJsonArray(), editedValue.getAsJsonArray());
            } else {
                out.add(key, editedValue);
            }
        }
        return out;
    }
}
