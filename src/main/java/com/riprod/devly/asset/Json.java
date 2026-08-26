package com.riprod.devly.asset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Json {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson LENIENT = new Gson();

    private Json() {
    }

    @Nullable
    public static JsonObject read(@Nonnull Path file) {
        try {
            String content = Files.readString(file);
            try (JsonReader reader = new JsonReader(new StringReader(content))) {
                reader.setStrictness(Strictness.LENIENT);
                return LENIENT.fromJson(reader, JsonObject.class);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("[devly] failed to parse JSON: %s", file);
            return null;
        }
    }

    public static void write(@Nonnull Path file, @Nonnull JsonObject json) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".devly.tmp");
        Files.writeString(tmp, PRETTY.toJson(json) + "\n");
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
