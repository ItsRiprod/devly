package com.riprod.devly.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;

import javax.annotation.Nonnull;

public final class MinifyCommand extends CommandBase {
    private final RequiredArg<String> target;
    private final OptionalArg<String> pack;
    private final FlagArg dryRun;

    public MinifyCommand() {
        super("minify", Lang.LANG + "command.minify.desc");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        this.target = withRequiredArg("target", Lang.LANG + "command.minify.target.desc", ArgTypes.STRING);
        this.pack = withOptionalArg("pack", Lang.LANG + "command.minify.pack.desc", ArgTypes.STRING);
        this.dryRun = withFlagArg("dryrun", Lang.LANG + "command.minify.dryrun.desc");
        addUsageVariant(new MinifyAllCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        Conversions.single(ctx, ctx.get(target), ctx.get(pack), Boolean.TRUE.equals(ctx.get(dryRun)));
    }
}
