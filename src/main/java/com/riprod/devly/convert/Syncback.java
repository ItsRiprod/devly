package com.riprod.devly.convert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riprod.devly.engine.ArrayPolicy;
import com.riprod.devly.engine.JsonDiff;
import com.riprod.devly.engine.PatchMerge;
import com.riprod.patchly.core.JsonDeepMerge;
import com.riprod.patchly.core.MergeTable;
import com.riprod.patchly.core.directive.PatchContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Syncback {
    public enum Status {
        UNCHANGED,
        WRITE,
        VERIFY_FAILED,
        UNSUPPORTED
    }

    private static final String IMPORT_KEY = "$Import";
    private static final String COMPUTE_SUFFIX = "#";

    public record Result(@Nonnull Status status, @Nullable JsonObject candidate) {
    }

    private Syncback() {
    }

    @Nonnull
    public static Result compute(@Nonnull JsonObject seed, @Nonnull JsonObject existingPatch,
                                 @Nonnull JsonObject editedComposed, @Nonnull MergeTable table,
                                 @Nonnull PatchContext ctx) {
        if (containsUnreconstructible(existingPatch)) {
            return new Result(Status.UNSUPPORTED, null);
        }

        JsonObject reconstructed = JsonDeepMerge.merge(seed, existingPatch, table, ctx);

        DevlyMeta meta = DevlyMeta.read(DevlyMeta.split(existingPatch).meta());
        ArrayPolicy policy = new ArrayPolicy(meta.arrays, table);
        JsonObject delta = new JsonDiff(policy).diff(reconstructed, editedComposed);
        if (delta.keySet().isEmpty()) {
            return new Result(Status.UNCHANGED, null);
        }

        JsonObject candidate = PatchMerge.merge(existingPatch, delta, table);
        JsonObject verified = JsonDeepMerge.merge(seed, candidate, table, ctx);
        if (!verified.equals(editedComposed)) {
            return new Result(Status.VERIFY_FAILED, null);
        }
        if (candidate.equals(existingPatch)) {
            return new Result(Status.UNCHANGED, null);
        }
        return new Result(Status.WRITE, candidate);
    }

    private static boolean containsUnreconstructible(@Nonnull JsonElement element) {
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (IMPORT_KEY.equals(entry.getKey()) || entry.getKey().endsWith(COMPUTE_SUFFIX)) return true;
                if (containsUnreconstructible(entry.getValue())) return true;
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsUnreconstructible(child)) return true;
            }
        }
        return false;
    }
}
