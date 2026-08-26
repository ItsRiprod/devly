package com.riprod.devly.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.riprod.devly.sync.SyncMonitor;

import javax.annotation.Nonnull;

public final class DevlyCommand extends AbstractCommandCollection {
    public DevlyCommand(@Nonnull SyncMonitor monitor) {
        super("devly", Lang.LANG + "command.desc");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        addSubCommand(new MinifyCommand());
        addSubCommand(new MonitorCommand(monitor));
    }
}
