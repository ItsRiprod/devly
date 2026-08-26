package com.riprod.devly.sync;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.monitor.AssetMonitor;
import com.hypixel.hytale.server.core.asset.monitor.AssetMonitorHandler;
import com.hypixel.hytale.server.core.asset.monitor.EventKind;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SyncMonitor implements AssetMonitorHandler {
    public enum Toggle {
        CHANGED, ALREADY, UNAVAILABLE
    }

    private static final long SETTLE_MILLIS = 100;

    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final Map<String, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();
    private volatile Path overrideRoot;
    private volatile boolean installed;

    public boolean isEnabled() {
        return enabled.get();
    }

    @Nonnull
    public Toggle setEnabled(boolean value) {
        if (!enabled.compareAndSet(!value, value)) return Toggle.ALREADY;
        if (value && !ensureInstalled()) {
            enabled.set(false);
            return Toggle.UNAVAILABLE;
        }
        return Toggle.CHANGED;
    }

    private synchronized boolean ensureInstalled() {
        if (installed) return true;
        AssetPack pack = OverridePackLocator.locate();
        AssetMonitor monitor = AssetModule.get().getAssetMonitor();
        if (pack == null || monitor == null) return false;
        Path serverDir = pack.getRoot().resolve("Server");
        if (!Files.isDirectory(serverDir)) return false;
        overrideRoot = pack.getRoot();
        monitor.monitorDirectoryFiles(serverDir, this);
        installed = true;
        return true;
    }

    @Override
    public Object getKey() {
        return "DevlySyncMonitor";
    }

    @Override
    public boolean test(Path path, EventKind eventKind) {
        return enabled.get() && eventKind != EventKind.ENTRY_DELETE;
    }

    @Override
    public void accept(Map<Path, EventKind> events) {
        if (!enabled.get()) return;
        Path root = overrideRoot;
        if (root == null) return;
        for (Map.Entry<Path, EventKind> event : events.entrySet()) {
            if (event.getValue() == EventKind.ENTRY_DELETE) continue;
            if (!Files.isRegularFile(event.getKey())) continue;
            schedule(root, PathUtil.normalizeRelative(root, event.getKey()));
        }
    }

    private void schedule(Path root, String target) {
        AtomicReference<ScheduledFuture<?>> self = new AtomicReference<>();
        ScheduledFuture<?> future = AssetMonitor.runTask(() -> {
            ScheduledFuture<?> f = self.get();
            if (f != null) f.cancel(false);
            pending.remove(target, f);
            if (!enabled.get()) return;
            SyncbackUtils.sync(root, target);
        }, SETTLE_MILLIS);
        self.set(future);
        ScheduledFuture<?> previous = pending.put(target, future);
        if (previous != null) previous.cancel(false);
    }
}
