package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RuntimeEndpointCoverage {

    private static final Path OUTPUT =
            Path.of("target", "runtime-endpoint-coverage.csv");

    private static boolean initialized = false;

    private RuntimeEndpointCoverage() {
    }

    /**
     * Initializes the coverage file once per JVM/test run.
     *
     * The existing file is truncated only on the first initialization.
     * Subsequent scenarios in the same run append to the same file.
     */
    public static synchronized void initializeRun() {
        if (initialized) {
            return;
        }

        try {
            Files.createDirectories(OUTPUT.getParent());

            Files.writeString(
                    OUTPUT,
                    "scenario_id,scenario_uri,scenario,method,endpoint"
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            initialized = true;

            System.out.println(
                    "[COVERAGE] Runtime coverage initialized: "
                            + OUTPUT
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Runtime endpoint coverage could not be initialized",
                    e
            );
        }
    }

    /**
     * Records an endpoint for the current Cucumber scenario.
     */
    public static synchronized void record(
            String scenarioName,
            String endpoint
    ) {
        record(
                "",
                "",
                scenarioName,
                endpoint
        );
    }

    /**
     * Records an endpoint together with the stable Cucumber scenario
     * identity.
     */
    public static synchronized void record(
            String scenarioId,
            String scenarioUri,
            String scenarioName,
            String endpoint
    ) {
        if (scenarioName == null || scenarioName.isBlank()) {
            return;
        }

        if (endpoint == null || endpoint.isBlank()) {
            return;
        }

        initializeRun();

        int separator = endpoint.indexOf(' ');

        if (separator <= 0 || separator >= endpoint.length() - 1) {
            return;
        }

        String method =
                endpoint.substring(0, separator).trim();

        String path =
                endpoint.substring(separator + 1).trim();

        if (method.isBlank() || path.isBlank()) {
            return;
        }

        try {
            String row =
                    csv(scenarioId) + "," +
                    csv(scenarioUri) + "," +
                    csv(scenarioName) + "," +
                    csv(method) + "," +
                    csv(path) +
                    System.lineSeparator();

            Files.writeString(
                    OUTPUT,
                    row,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
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
        if (value == null) {
            return "\"\"";
        }

        return "\"" +
                value
                        .replace("\r", " ")
                        .replace("\n", " ")
                        .replace("\"", "\"\"") +
                "\"";
    }
}