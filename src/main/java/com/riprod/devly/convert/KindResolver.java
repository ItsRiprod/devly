package com.riprod.devly.convert;

import com.google.gson.JsonObject;
import com.riprod.patchly.source.BasePolicy;
import com.riprod.patchly.source.SourceKind;
import com.riprod.patchly.source.SourceKindTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class KindResolver {
    // patchly's two canonical builtin kinds. the choice is policy-shaped (a REQUIRED override
    // when a base exists, otherwise an OPTIONAL place-or-override) but SourceKindTable exposes no
    // enumeration, so the defaults are named here once and always resolved through the live registry.
    static final String REQUIRED_DEFAULT = ".patch";
    static final String OPTIONAL_DEFAULT = ".put";

    private KindResolver() {
    }

    public static boolean producesTarget(@Nullable SourceKind kind) {
        if (kind == null) return false;
        BasePolicy policy = kind.basePolicy();
        return policy == BasePolicy.REQUIRED || policy == BasePolicy.OPTIONAL;
    }

    @Nullable
    public static SourceKind forFile(@Nonnull SourceKindTable kinds, @Nonnull String fileName) {
        return kinds.kindFor(fileName);
    }

    @Nullable
    public static SourceKind byExtension(@Nonnull SourceKindTable kinds, @Nonnull String extension) {
        SourceKind kind = kinds.kindFor(extension);
        return producesTarget(kind) ? kind : null;
    }

    @Nullable
    public static SourceKind decide(@Nonnull SourceKindTable kinds, @Nullable String recordedExtension,
                                    boolean hasBase, @Nonnull JsonObject meta) {
        if (recordedExtension != null) return byExtension(kinds, recordedExtension);
        if (hasBase) return byExtension(kinds, REQUIRED_DEFAULT);
        if (DevlyMeta.hasPatchlyMeta(meta)) return byExtension(kinds, OPTIONAL_DEFAULT);
        return null;
    }
}
