package com.riprod.devly.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.riprod.devly.sync.SyncMonitor;

import javax.annotation.Nonnull;

public final class MonitorCommand extends CommandBase {
    private final SyncMonitor monitor;
    private final OptionalArg<Boolean> enable;

    public MonitorCommand(@Nonnull SyncMonitor monitor) {
        super("monitor", Lang.LANG + "command.monitor.desc");
        this.monitor = monitor;
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        this.enable = withOptionalArg("enable", Lang.LANG + "command.monitor.enable.desc", ArgTypes.BOOLEAN);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        Boolean explicit = ctx.get(enable);
        boolean value = explicit != null ? explicit : !monitor.isEnabled();
        String key = switch (monitor.setEnabled(value)) {
            case CHANGED -> value ? "monitor.started" : "monitor.stopped";
            case ALREADY -> "monitor.already";
            case UNAVAILABLE -> "monitor.unavailable";
        };
        ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + key)));
    }
}
