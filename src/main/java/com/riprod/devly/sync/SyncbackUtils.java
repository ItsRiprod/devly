package com.riprod.devly.sync;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.devly.asset.AssetBaseResolver;
import com.riprod.devly.asset.Json;
import com.riprod.devly.asset.LivePatchContext;
import com.riprod.devly.convert.Syncback;
import com.riprod.patchly.core.JsonDeepMerge;
import com.riprod.patchly.source.BasePolicy;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class SyncbackUtils {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private SyncbackUtils() {
    }

    public static void sync(@Nonnull Path overrideRoot, @Nonnull String targetRelative) {
        Path composedFile = overrideRoot.resolve(targetRelative);
        if (!Files.isRegularFile(composedFile)) return;
        JsonObject composed = Json.read(composedFile);
        if (composed == null) return;

        List<PatchOwnership.Source> sources = PatchOwnership.sourcesFor(targetRelative);
        if (sources.isEmpty()) {
            LOGGER.at(Level.INFO).log("[devly] no patch source owns %s; edit not synced", targetRelative);
            return;
        }
        if (sources.size() > 1) {
            StringBuilder listing = new StringBuilder();
            for (PatchOwnership.Source source : sources) {
                listing.append("\n  ").append(source.pack().getName()).append(" : ").append(source.file());
            }
            LOGGER.atWarning().log(
                    "[devly] %s has %d patch sources; refusing to sync:%s",
                    targetRelative, sources.size(), listing);
            return;
        }
        PatchOwnership.Source source = sources.get(0);
        if (!source.writable()) {
            LOGGER.atWarning().log(
                    "[devly] the only patch source for %s (%s) is not writable; edit not synced",
                    targetRelative, source.pack().getName());
            return;
        }

        JsonObject existing = Json.read(source.file());
        if (existing == null) return;

        JsonObject base = AssetBaseResolver.resolve(targetRelative, composedFile);
        if (base == null && source.kind().basePolicy() == BasePolicy.REQUIRED) {
            LOGGER.atWarning().log(
                    "[devly] no base asset for %s; edit not synced", targetRelative);
            return;
        }
        JsonObject seed = base != null ? base : source.kind().seedWhenAbsent();

        Syncback.Result result = Syncback.compute(seed, existing, composed,
                JsonDeepMerge.activeTable(), new LivePatchContext());
        switch (result.status()) {
            case UNCHANGED -> LOGGER.atFine().log("[devly] %s matches its patch; nothing to sync", targetRelative);
            case UNSUPPORTED -> LOGGER.atWarning().log(
                    "[devly] %s uses $Import or # expressions that only resolve inside patchly; "
                            + "edit the patch source by hand: %s",
                    targetRelative, source.file());
            case VERIFY_FAILED -> LOGGER.atWarning().log(
                    "[devly] could not derive a faithful patch for %s; "
                            + "edit the patch source by hand: %s",
                    targetRelative, source.file());
            case WRITE -> {
                try {
                    Json.write(source.file(), result.candidate());
                    LOGGER.at(Level.INFO).log("[devly] synced %s -> %s", targetRelative, source.file());
                } catch (IOException e) {
                    LOGGER.atWarning().withCause(e).log(
                            "[devly] failed writing synced patch %s", source.file());
                }
            }
        }
    }
}
