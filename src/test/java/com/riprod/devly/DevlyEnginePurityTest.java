package com.riprod.devly;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DevlyEnginePurityTest {
    @Test
    void pureLayersHaveNoHytaleImports() throws IOException {
        List<Path> roots = List.of(
                Path.of("src/main/java/com/riprod/devly/engine"),
                Path.of("src/main/java/com/riprod/devly/convert"));
        assumeTrue(roots.stream().anyMatch(Files::isDirectory), "source not at expected path; skipping purity scan");

        List<String> offenders = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : (Iterable<Path>) stream.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    for (String line : Files.readAllLines(file)) {
                        if (line.startsWith("import com.hypixel")) offenders.add(file + ": " + line.trim());
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "pure layers must not import Hytale: " + offenders);
    }
}
