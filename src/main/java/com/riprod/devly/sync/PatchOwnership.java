package com.riprod.devly.sync;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.riprod.devly.asset.Packs;
import com.riprod.devly.convert.KindResolver;
import com.riprod.patchly.source.SourceKind;
import com.riprod.patchly.source.SourceKindRegistry;
import com.riprod.patchly.source.SourceKindTable;
import com.riprod.patchly.store.OverridePackRegistrar;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class PatchOwnership {
    public record Source(AssetPack pack, Path file, SourceKind kind, boolean writable) {
    }

    private PatchOwnership() {
    }

    @Nonnull
    public static List<Source> sourcesFor(@Nonnull String targetRelative) {
        SourceKindTable kinds = SourceKindRegistry.table();
        List<Source> out = new ArrayList<>();
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (OverridePackRegistrar.isSynthetic(pack.getName())) continue;
            boolean writable = !pack.isImmutable()
                    && pack.getRoot().getFileSystem() == FileSystems.getDefault();
            for (Path file : Packs.walk(pack, name -> KindResolver.producesTarget(kinds.kindFor(name)))) {
                String relSource = PathUtil.normalizeRelative(pack.getRoot(), file);
                SourceKind kind = kinds.kindFor(file.getFileName().toString());
                if (kind == null) continue;
                String stem = PathUtil.stripSuffix(relSource, kind.extension());
                if (stem == null) continue;
                if (!targetRelative.equals(PathUtil.recoverTargetExtension(stem))) continue;
                out.add(new Source(pack, file, kind, writable));
            }
        }
        return out;
    }
}
