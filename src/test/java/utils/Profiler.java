package utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

/*
 * Opt-in timing instrumentation (-Dprofile=true). Kapalıyken hiçbir şey
 * yazmaz ve ölçüm maliyeti yoktur.
 *
 * Çıktılar target/profile altında:
 *   steps.csv  -> ProfilingPlugin (senaryo / step / hook süreleri)
 *   spans.csv  -> Profiler.span(...) ile işaretlenmiş altyapı işlemleri
 *   traces/    -> senaryo başına Playwright trace'i (snapshot/screenshot yok)
 */
public final class Profiler {

    public static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("profile", "false"));

    // -Dtest.worker.id verilirse worker başına ayrı klasör (paralel koşu hazırlığı).
    public static final Path DIR = System.getProperty("test.worker.id", "").isBlank()
            ? Path.of("target", "profile")
            : Path.of("target", "profile", System.getProperty("test.worker.id").replaceAll("[^A-Za-z0-9]", ""));

    private static final Path SPANS = DIR.resolve("spans.csv");

    private static volatile String currentScenario = "";

    static {
        if (ENABLED) {
            try {
                Files.createDirectories(DIR.resolve("traces"));
                Files.writeString(
                        SPANS,
                        "start_epoch_ms,duration_ms,category,label,scenario"
                                + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Profiler() {
    }

    public static void setScenario(String name) {
        currentScenario = name == null ? "" : name;
    }

    public static String scenario() {
        return currentScenario;
    }

    public static <T> T span(String category, String label, Supplier<T> action) {
        if (!ENABLED) {
            return action.get();
        }
        long start = System.currentTimeMillis();
        long t0 = System.nanoTime();
        try {
            return action.get();
        } finally {
            write(start, (System.nanoTime() - t0) / 1_000_000.0, category, label);
        }
    }

    public static void span(String category, String label, Runnable action) {
        span(category, label, () -> {
            action.run();
            return null;
        });
    }

    private static synchronized void write(
            long startEpochMs,
            double durationMs,
            String category,
            String label
    ) {
        try {
            Files.writeString(
                    SPANS,
                    startEpochMs + ","
                            + String.format(java.util.Locale.ROOT, "%.1f", durationMs) + ","
                            + csv(category) + ","
                            + csv(label) + ","
                            + csv(currentScenario)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Profiling must never fail the test.
        }
    }

    static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
