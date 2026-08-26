package com.riprod.devly.asset;

import com.google.gson.JsonObject;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.riprod.patchly.store.OverridePackRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetBaseResolver {
    private AssetBaseResolver() {
    }

    @Nullable
    public static JsonObject resolve(@Nonnull String relativeTarget, @Nonnull Path excludeFile) {
        Path basePath = resolvePath(relativeTarget, excludeFile);
        return basePath == null ? null : Json.read(basePath);
    }

    @Nullable
    private static Path resolvePath(@Nonnull String relativeTarget, @Nonnull Path excludeFile) {
        AssetStore<?, ?, ?> store = AssetStores.storeFor(relativeTarget);
        if (store != null) {
            Path byKey = upstreamPath(store, idOf(relativeTarget, store.getExtension()), excludeFile);
            if (byKey != null) return byKey;
        }
        return literalPath(relativeTarget, excludeFile);
    }

    @Nullable
    @SuppressWarnings({"rawtypes"})
    private static Path upstreamPath(@Nonnull AssetStore store, @Nonnull String id, @Nonnull Path excludeFile) {
        Object key = store.decodeStringKey(id);
        if (key == null) return null;
        AssetMap map = store.getAssetMap();
        Path best = null;
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (OverridePackRegistrar.isSynthetic(pack.getName())) continue;
            Object path = map.getPathMap(pack.getName()).get(key);
            if (path == null) continue;
            if (isSameFile((Path) path, excludeFile)) continue;
            best = (Path) path;
        }
        return best;
    }

    @Nullable
    private static Path literalPath(@Nonnull String relativeTarget, @Nonnull Path excludeFile) {
        Path winning = null;
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (OverridePackRegistrar.isSynthetic(pack.getName())) continue;
            Path candidate = pack.getRoot().resolve(relativeTarget);
            if (!Files.isRegularFile(candidate)) continue;
            if (isSameFile(candidate, excludeFile)) continue;
            winning = candidate;
        }
        return winning;
    }

    @Nonnull
    private static String idOf(@Nonnull String target, @Nonnull String extension) {
        int slash = target.lastIndexOf('/');
        String fileName = slash < 0 ? target : target.substring(slash + 1);
        return fileName.endsWith(extension) ? fileName.substring(0, fileName.length() - extension.length()) : fileName;
    }

    private static boolean isSameFile(@Nonnull Path a, @Nonnull Path b) {
        try {
            return Files.isSameFile(a, b);
        } catch (IOException e) {
            return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
        }
    }
}
