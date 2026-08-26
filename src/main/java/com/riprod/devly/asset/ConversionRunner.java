package com.riprod.devly.asset;

import com.google.gson.JsonObject;
import com.hypixel.hytale.assetstore.AssetPack;
import com.riprod.devly.convert.BaseResolver;
import com.riprod.devly.convert.Converter;
import com.riprod.devly.convert.Outcome;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConversionRunner {
    private ConversionRunner() {
    }

    @Nonnull
    public static Outcome run(@Nonnull Converter.Transform transform, @Nonnull AssetPack pack, @Nonnull Path file, boolean apply) {
        JsonObject root = Json.read(file);
        if (root == null) return Outcome.skip("skip.parse", Map.of("file", fileName(file)));

        Path packRoot = pack.getRoot();
        String relSource = PathUtil.normalizeRelative(packRoot, file);
        BaseResolver resolver = relativeTarget -> AssetBaseResolver.resolve(relativeTarget, file);
        Outcome outcome = transform.apply(relSource, root, resolver);

        if (!apply || !outcome.changed() || outcome.targetRelative() == null || outcome.output() == null) {
            return outcome;
        }
        Path target = packRoot.resolve(outcome.targetRelative());
        try {
            Json.write(target, outcome.output());
            if (!Files.isSameFile(target, file)) {
                if (Files.deleteIfExists(file)) {
                    AssetStores.forgetDeleted(pack, file);
                }
            }
        } catch (IOException e) {
            return Outcome.skip("skip.writeFailed", Map.of("file", fileName(target), "error", String.valueOf(e.getMessage())));
        }
        return outcome;
    }

    private static String fileName(@Nonnull Path path) {
        Path name = path.getFileName();
        return name == null ? "" : name.toString();
    }
}
