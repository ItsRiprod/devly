package com.riprod.devly.sync;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.riprod.patchly.store.OverridePackRegistrar;

import javax.annotation.Nullable;

public final class OverridePackLocator {
    private OverridePackLocator() {
    }

    @Nullable
    public static AssetPack locate() {
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (pack.getName().endsWith(OverridePackRegistrar.OVERRIDE_PACK_SUFFIX)) return pack;
        }
        return null;
    }
}
