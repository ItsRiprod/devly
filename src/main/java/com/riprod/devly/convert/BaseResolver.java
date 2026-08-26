package com.riprod.devly.convert;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface BaseResolver {
    @Nullable
    JsonObject resolveBase(@Nonnull String relativeTarget);
}
