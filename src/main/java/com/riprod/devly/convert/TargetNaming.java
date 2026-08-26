package com.riprod.devly.convert;

import com.riprod.patchly.source.SourceKind;

import javax.annotation.Nonnull;

public final class TargetNaming {
    private static final String JSON_EXTENSION = ".json";

    private TargetNaming() {
    }

    @Nonnull
    public static String minifyOutput(@Nonnull String relTarget, @Nonnull SourceKind kind) {
        String stem = relTarget.endsWith(JSON_EXTENSION)
                ? relTarget.substring(0, relTarget.length() - JSON_EXTENSION.length())
                : relTarget;
        return stem + kind.extension();
    }

    @Nonnull
    public static String fileName(@Nonnull String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? relativePath : relativePath.substring(slash + 1);
    }
}
