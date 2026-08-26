package com.riprod.devly.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;

import javax.annotation.Nonnull;

final class MinifyAllCommand extends CommandBase {
    private final FlagArg all;
    private final OptionalArg<String> pack;
    private final FlagArg dryRun;
    private final FlagArg verbose;

    MinifyAllCommand() {
        super(Lang.LANG + "command.minify.desc");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        this.all = withFlagArg("all", Lang.LANG + "command.minify.all.desc");
        this.pack = withOptionalArg("pack", Lang.LANG + "command.minify.pack.desc", ArgTypes.STRING);
        this.dryRun = withFlagArg("dryrun", Lang.LANG + "command.minify.dryrun.desc");
        this.verbose = withFlagArg("verbose", Lang.LANG + "command.minify.verbose.desc");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!Boolean.TRUE.equals(ctx.get(all))) {
            ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + "minify.usage")));
            return;
        }
        Conversions.all(ctx, ctx.get(pack), Boolean.TRUE.equals(ctx.get(dryRun)), Boolean.TRUE.equals(ctx.get(verbose)));
    }
}
