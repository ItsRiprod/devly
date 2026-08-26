package com.riprod.devly.asset;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.riprod.patchly.store.OverridePackRegistrar;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class Packs {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Packs() {
    }

    public record Located(AssetPack pack, Path file) {
    }

    @Nonnull
    public static List<AssetPack> eligible() {
        List<AssetPack> out = new ArrayList<>();
        for (AssetPack pack : AssetModule.get().getAssetPacks()) {
            if (OverridePackRegistrar.isSynthetic(pack.getName())) continue;
            if (pack.isImmutable()) continue;
            if (pack.getRoot().getFileSystem() != FileSystems.getDefault()) continue;
            out.add(pack);
        }
        return out;
    }

    public static boolean packMatches(@Nonnull AssetPack pack, @Nonnull String query) {
        String name = pack.getName().toLowerCase(Locale.ENGLISH);
        String q = query.toLowerCase(Locale.ENGLISH);
        return name.equals(q) || name.endsWith(":" + q) || name.contains(q);
    }

    @Nonnull
    public static List<Located> locate(@Nonnull String query, @Nullable String packQuery) {
        String normalized = query.replace('\\', '/');
        List<Located> out = new ArrayList<>();
        for (AssetPack pack : eligible()) {
            if (packQuery != null && !packMatches(pack, packQuery)) continue;
            for (Path file : walk(pack, name -> true)) {
                String relative = PathUtil.normalizeRelative(pack.getRoot(), file);
                String fileName = file.getFileName().toString();
                if (fileName.equals(normalized) || relative.equals(normalized) || relative.endsWith("/" + normalized)) {
                    out.add(new Located(pack, file));
                }
            }
        }
        return out;
    }

    @Nonnull
    public static List<Path> walk(@Nonnull AssetPack pack, @Nonnull Predicate<String> nameFilter) {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(pack.getRoot())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        Path name = p.getFileName();
                        return name != null && !"manifest.json".equals(name.toString()) && nameFilter.test(name.toString());
                    })
                    .forEach(out::add);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[devly] walk failed for pack %s", pack.getName());
        }
        return out;
    }
}
