package com.riprod.devly.command;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.riprod.devly.asset.ConversionRunner;
import com.riprod.devly.asset.Packs;
import com.riprod.devly.convert.Converter;
import com.riprod.devly.convert.Outcome;
import com.riprod.patchly.util.PathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

final class Conversions {
    private Conversions() {
    }

    static void single(@Nonnull CommandContext ctx, @Nonnull String query,
                       @Nullable String packQuery, boolean dry) {
        List<Packs.Located> matches = Packs.locate(query, packQuery);
        if (matches.isEmpty()) {
            Message none = Message.translation(Lang.LANG + (packQuery == null ? "match.none" : "match.nonePack"))
                    .param("query", query);
            if (packQuery != null) none = none.param("pack", packQuery);
            ctx.sendMessage(Lang.markup(none));
            return;
        }
        if (matches.size() > 1) {
            ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + "match.ambiguous")
                    .param("query", query)
                    .param("count", matches.size())));
            for (Packs.Located match : matches) {
                ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + "match.line")
                        .param("pack", match.pack().getName())
                        .param("path", PathUtil.normalizeRelative(match.pack().getRoot(), match.file()))));
            }
            return;
        }
        Packs.Located match = matches.get(0);
        Outcome outcome = ConversionRunner.run(Converter::minify, match.pack(), match.file(), !dry);
        ctx.sendMessage(wrapDry(outcomeMessage(outcome), dry));
    }

    static void all(@Nonnull CommandContext ctx, @Nullable String packQuery, boolean dry, boolean verbose) {
        int changed = 0;
        int skipped = 0;
        for (AssetPack pack : Packs.eligible()) {
            if (packQuery != null && !Packs.packMatches(pack, packQuery)) continue;
            for (Path file : Packs.walk(pack, name -> name.endsWith(".json"))) {
                Outcome outcome = ConversionRunner.run(Converter::minify, pack, file, !dry);
                if (outcome.changed()) {
                    changed++;
                    ctx.sendMessage(wrapDry(outcomeMessage(outcome), dry));
                } else {
                    skipped++;
                    if (verbose) {
                        ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + "skip.line")
                                .param("msg", outcomeMessage(outcome))));
                    }
                }
            }
        }
        ctx.sendMessage(Lang.markup(Message.translation(Lang.LANG + (dry ? "all.dry" : "all.done"))
                .param("changed", changed)
                .param("skipped", skipped)));
    }

    @Nonnull
    private static Message outcomeMessage(@Nonnull Outcome outcome) {
        Message message = Message.translation(Lang.LANG + outcome.messageKey());
        for (Map.Entry<String, String> param : outcome.params().entrySet()) {
            message = message.param(param.getKey(), param.getValue());
        }
        return Lang.markup(message);
    }

    @Nonnull
    private static Message wrapDry(@Nonnull Message message, boolean dry) {
        if (!dry) return message;
        return Lang.markup(Message.translation(Lang.LANG + "dry.wrap").param("msg", message));
    }
}
