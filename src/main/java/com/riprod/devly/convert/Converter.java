package com.riprod.devly.convert;

import com.google.gson.JsonObject;
import com.riprod.patchly.core.JsonDeepMerge;
import com.riprod.patchly.core.MergeTable;
import com.riprod.patchly.source.BasePolicy;
import com.riprod.patchly.source.SourceKind;
import com.riprod.patchly.source.SourceKindRegistry;
import com.riprod.patchly.source.SourceKindTable;
import com.riprod.devly.engine.ArrayPolicy;
import com.riprod.devly.engine.JsonDiff;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class Converter {
    private Converter() {
    }

    // only remember the kind when it disagrees with what base-presence would default to
    // (base present -> a REQUIRED .patch, base absent -> an OPTIONAL .put); otherwise it is
    // inferable and storing it would just clutter the shipped file.
    @Nullable
    private static String stickyKind(@Nonnull SourceKind kind, boolean hasBase) {
        boolean matchesDefault = (kind.basePolicy() == BasePolicy.REQUIRED) == hasBase;
        return matchesDefault ? null : kind.extension();
    }

    @FunctionalInterface
    public interface Transform {
        Outcome apply(@Nonnull String relSource, @Nonnull JsonObject root, @Nonnull BaseResolver resolver);
    }

    public static Outcome minify(@Nonnull String relSource, @Nonnull JsonObject root, @Nonnull BaseResolver resolver) {
        SourceKindTable kinds = SourceKindRegistry.table();
        String fileName = TargetNaming.fileName(relSource);
        if (KindResolver.forFile(kinds, fileName) != null) {
            return Outcome.skip("skip.alreadySource", Map.of("file", fileName));
        }

        DevlyMeta.Split split = DevlyMeta.split(root);
        DevlyMeta meta = DevlyMeta.read(split.meta());
        if (meta.ignore) {
            return Outcome.skip("skip.ignored", Map.of("file", fileName));
        }

        JsonObject base = resolver.resolveBase(relSource);
        SourceKind kind = KindResolver.decide(kinds, meta.kind, base != null, split.meta());
        if (kind == null) {
            return Outcome.skip("skip.noKind", Map.of("file", fileName));
        }
        if (base == null && kind.basePolicy() == BasePolicy.REQUIRED) {
            return Outcome.skip("skip.noBase", Map.of("file", fileName, "kind", kind.extension()));
        }

        JsonObject seed = base != null ? base : kind.seedWhenAbsent();
        MergeTable table = JsonDeepMerge.activeTable();
        ArrayPolicy policy = new ArrayPolicy(meta.arrays, table);
        JsonObject patchBody = new JsonDiff(policy).diff(seed, split.body());

        JsonObject reproduced = JsonDeepMerge.merge(seed, patchBody, table);
        if (!reproduced.equals(split.body())) {
            return Outcome.skip("skip.verifyFailed", Map.of("file", fileName));
        }

        meta.kind = stickyKind(kind, base != null);
        meta.arrays.putAll(policy.updatedDecisions());
        meta.writeInto(split.meta());

        JsonObject output = DevlyMeta.reassemble(split.meta(), patchBody);
        String outputRelative = TargetNaming.minifyOutput(relSource, kind);
        return Outcome.converted(outputRelative, output, "minify.done",
                Map.of("file", fileName, "target", TargetNaming.fileName(outputRelative)));
    }
}
