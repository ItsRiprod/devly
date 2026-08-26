package com.riprod.devly.command;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

final class Lang {
    static final String LANG = "server.devly.v1.";

    private Lang() {
    }

    // markupEnabled defaults to false and Message exposes no builder setter for it, so inline
    // lang tags only render once the flag is set on the node being sent
    @Nonnull
    static Message markup(@Nonnull Message message) {
        message.getFormattedMessage().markupEnabled = true;
        return message;
    }
}
