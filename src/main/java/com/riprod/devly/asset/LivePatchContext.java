package com.riprod.devly.asset;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.riprod.patchly.core.directive.PatchContext;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class LivePatchContext implements PatchContext {
    private Map<String, Semver> present;

    private Map<String, Semver> present() {
        if (present == null) {
            Map<String, Semver> versions = new HashMap<>();
            for (AssetPack pack : AssetModule.get().getAssetPacks()) {
                versions.put(pack.getName(), pack.getManifest().getVersion());
            }
            PluginManager pluginManager = PluginManager.get();
            if (pluginManager != null) {
                for (PluginBase plugin : pluginManager.getPlugins()) {
                    versions.putIfAbsent(plugin.getIdentifier().toString(),
                            plugin.getManifest().getVersion());
                }
            }
            present = versions;
        }
        return present;
    }

    @Override
    public boolean packPresent(@Nonnull String packName) {
        return present().containsKey(packName);
    }

    @Override
    public boolean versionSatisfies(@Nonnull String packName, @Nonnull String range) {
        Semver version = present().get(packName);
        if (version == null) return false;
        try {
            return version.satisfies(SemverRange.fromString(range));
        } catch (RuntimeException e) {
            return true;
        }
    }
}
