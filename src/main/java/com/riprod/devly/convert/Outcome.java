package com.riprod.devly.convert;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Map;

public record Outcome(boolean changed, String messageKey, Map<String, String> params,
                      @Nullable String targetRelative, @Nullable JsonObject output) {
    public static Outcome skip(String messageKey, Map<String, String> params) {
        return new Outcome(false, messageKey, params, null, null);
    }

    public static Outcome converted(String targetRelative, JsonObject output,
                                    String messageKey, Map<String, String> params) {
        return new Outcome(true, messageKey, params, targetRelative, output);
    }
}
