package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RuntimeEndpointCoverage {

    private static final Path OUTPUT =
            Path.of("target", "runtime-endpoint-coverage.csv");

    private RuntimeEndpointCoverage() {
    }

    public static synchronized void record(
            String scenario,
            String endpoint
    ) {
        if (scenario == null || endpoint == null) {
            return;
        }

        int separator = endpoint.indexOf(' ');

        if (separator <= 0) {
            return;
        }

        String method = endpoint.substring(0, separator);
        String path = endpoint.substring(separator + 1);

        try {
            Files.createDirectories(OUTPUT.getParent());

            if (!Files.exists(OUTPUT)) {
                Files.writeString(
                        OUTPUT,
                        "scenario,method,endpoint" + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE
                );
            }

            String row =
                    csv(scenario) + "," +
                    csv(method) + "," +
                    csv(path) +
                    System.lineSeparator();

            Files.writeString(
                    OUTPUT,
                    row,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Runtime endpoint coverage could not be written",
                    e
            );
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}