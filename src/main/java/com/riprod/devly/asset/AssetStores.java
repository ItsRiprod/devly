package com.riprod.devly.asset;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class AssetStores {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private AssetStores() {
    }

    @Nullable
    public static AssetStore<?, ?, ?> storeFor(@Nonnull String relativeTarget) {
        AssetStore<?, ?, ?> best = null;
        int bestLen = -1;
        for (AssetStore<?, ?, ?> store : AssetRegistry.getStoreMap().values()) {
            String path = store.getPath();
            if (path == null) continue;
            String prefix = "Server/" + path;
            if (prefix.length() > bestLen
                    && relativeTarget.startsWith(prefix + "/")
                    && relativeTarget.endsWith(store.getExtension())) {
                best = store;
                bestLen = prefix.length();
            }
        }
        return best;
    }

    public static void forgetDeleted(@Nonnull AssetPack pack, @Nonnull Path deletedFile) {
        String relative = PathUtil.normalizeRelative(pack.getRoot(), deletedFile);
        AssetStore<?, ?, ?> store = storeFor(relative);
        if (store == null || store.isUnmodifiable()) return;
        try {
            Set<?> removed = store.removeAssetWithPaths(pack.getName(), List.of(deletedFile));
            LOGGER.atFine().log("[devly] unregistered %d asset(s) for deleted %s", removed.size(), relative);
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log(
                    "failed to unregister deleted asset %s; it may stay bound to a missing path until restart",
                    deletedFile);
        }
    }
}
