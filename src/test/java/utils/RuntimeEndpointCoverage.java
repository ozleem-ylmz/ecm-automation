package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class RuntimeEndpointCoverage {

    private static final Path OUTPUT =
            Path.of("target", "runtime-endpoint-coverage.csv");

    private static final Path METADATA_OUTPUT =
            Path.of("target", "runtime-endpoint-coverage-meta.json");

    private static boolean initialized = false;

    private RuntimeEndpointCoverage() {
    }

    /**
     * Initializes runtime endpoint coverage once per JVM/test run.
     *
     * Coverage mode resolution:
     *
     * 1. Explicit runtime.coverage.mode wins.
     * 2. Cucumber name/tag filters imply PARTIAL coverage.
     * 3. An unfiltered run defaults to FULL coverage.
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

            String coverageMode = getCoverageMode();

            String metadata =
                    "{"
                            + System.lineSeparator()
                            + "  \"mode\": \"" + json(coverageMode) + "\","
                            + System.lineSeparator()
                            + "  \"generatedAt\": \"" + json(Instant.now().toString()) + "\""
                            + System.lineSeparator()
                            + "}"
                            + System.lineSeparator();

            Files.writeString(
                    METADATA_OUTPUT,
                    metadata,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            initialized = true;

            System.out.println(
                    "[COVERAGE] Runtime coverage initialized: "
                            + OUTPUT
            );

            System.out.println(
                    "[COVERAGE] Runtime coverage mode: "
                            + coverageMode
            );

            System.out.println(
                    "[COVERAGE] Runtime coverage metadata: "
                            + METADATA_OUTPUT
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
     * Records an endpoint together with the Cucumber scenario identity.
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

    private static String getCoverageMode() {
        String explicitMode =
                System.getProperty("runtime.coverage.mode");

        if (explicitMode != null && !explicitMode.isBlank()) {
            String mode =
                    explicitMode.trim().toUpperCase();

            if (!mode.equals("FULL") && !mode.equals("PARTIAL")) {
                throw new IllegalArgumentException(
                        "Invalid runtime.coverage.mode: "
                                + mode
                                + ". Expected FULL or PARTIAL."
                );
            }

            return mode;
        }

        String nameFilter =
                System.getProperty("cucumber.filter.name");

        String tagFilter =
                System.getProperty("cucumber.filter.tags");

        if (
                (nameFilter != null && !nameFilter.isBlank())
                        ||
                (tagFilter != null && !tagFilter.isBlank())
        ) {
            return "PARTIAL";
        }

        return "FULL";
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

    private static String json(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}