package com.riprod.devly.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DevlyMeta {
    private static final String META_PREFIX = "$";
    private static final String DEVLY_KEY = "$Devly";
    private static final String KIND_FIELD = "kind";
    private static final String ARRAYS_FIELD = "arrays";
    private static final String IGNORE_FIELD = "ignore";
    private static final String COMMENT_KEY = "$Comment";

    @Nullable
    public String kind;
    public boolean ignore;
    public final Map<String, String> arrays = new LinkedHashMap<>();

    @Nonnull
    public static Split split(@Nonnull JsonObject root) {
        JsonObject meta = new JsonObject();
        JsonObject body = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getKey().startsWith(META_PREFIX)) {
                meta.add(entry.getKey(), entry.getValue());
            } else {
                body.add(entry.getKey(), entry.getValue());
            }
        }
        return new Split(meta, body);
    }

    @Nonnull
    public static DevlyMeta read(@Nonnull JsonObject meta) {
        DevlyMeta out = new DevlyMeta();
        JsonElement raw = meta.get(DEVLY_KEY);
        if (raw == null || !raw.isJsonObject()) return out;
        JsonObject obj = raw.getAsJsonObject();
        if (obj.has(KIND_FIELD) && obj.get(KIND_FIELD).isJsonPrimitive()) {
            out.kind = obj.get(KIND_FIELD).getAsString();
        }
        if (obj.has(IGNORE_FIELD) && obj.get(IGNORE_FIELD).isJsonPrimitive()) {
            out.ignore = obj.get(IGNORE_FIELD).getAsBoolean();
        }
        if (obj.has(ARRAYS_FIELD) && obj.get(ARRAYS_FIELD).isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject().entrySet()) {
                out.arrays.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return out;
    }

    public void writeInto(@Nonnull JsonObject meta) {
        if (kind == null && !ignore && arrays.isEmpty()) {
            meta.remove(DEVLY_KEY);
            return;
        }
        JsonObject obj = new JsonObject();
        if (kind != null) obj.addProperty(KIND_FIELD, kind);
        if (ignore) obj.addProperty(IGNORE_FIELD, true);
        if (!arrays.isEmpty()) {
            JsonObject arraysObj = new JsonObject();
            for (Map.Entry<String, String> entry : arrays.entrySet()) {
                arraysObj.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add(ARRAYS_FIELD, arraysObj);
        }
    }

    public static boolean hasPatchlyMeta(@Nonnull JsonObject meta) {
        for (String key : meta.keySet()) {
            if (!COMMENT_KEY.equals(key)) return true;
        }
        return false;
    }

    @Nonnull
    public static JsonObject reassemble(@Nonnull JsonObject meta, @Nonnull JsonObject body) {
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : meta.entrySet()) out.add(entry.getKey(), entry.getValue());
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) out.add(entry.getKey(), entry.getValue());
        return out;
    }

    public record Split(JsonObject meta, JsonObject body) {
    }
}
