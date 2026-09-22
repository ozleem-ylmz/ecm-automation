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
     * Initializes the runtime coverage file once per JVM/test run.
     *
     * The previous run's coverage is removed so stale scenario/endpoint
     * mappings cannot affect the current run.
     *
     * This method is safe to call before every scenario because the actual
     * initialization happens only once per JVM.
     */
    public static synchronized void initializeRun() {
        if (initialized) {
            return;
        }

        try {
            Files.createDirectories(OUTPUT.getParent());

            Files.writeString(
                    OUTPUT,
                    "scenario,method,endpoint" + System.lineSeparator(),
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

    public static synchronized void record(
            String scenario,
            String endpoint
    ) {
        if (scenario == null || endpoint == null) {
            return;
        }

        initializeRun();

        int separator = endpoint.indexOf(' ');

        if (separator <= 0) {
            return;
        }

        String method = endpoint.substring(0, separator);
        String path = endpoint.substring(separator + 1);

        try {
            String row =
                    csv(scenario) + "," +
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
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}