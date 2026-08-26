package com.riprod.devly;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.riprod.devly.command.DevlyCommand;
import com.riprod.devly.sync.SyncMonitor;

public final class DevlyPlugin extends JavaPlugin {
    public DevlyPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new DevlyCommand(new SyncMonitor()));
    }
}
