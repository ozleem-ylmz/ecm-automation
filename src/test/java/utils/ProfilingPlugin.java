package utils;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.TestStepStarted;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/*
 * Opt-in (-Dprofile=true) Cucumber listener: senaryo, step ve hook
 * sürelerini target/profile/steps.csv dosyasına yazar.
 */
public class ProfilingPlugin implements ConcurrentEventListener {

    private static final Path STEPS = Profiler.DIR.resolve("steps.csv");

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        if (!Profiler.ENABLED) {
            return;
        }

        try {
            Files.createDirectories(Profiler.DIR);
            Files.writeString(
                    STEPS,
                    "kind,start_epoch_ms,duration_ms,status,scenario,line,text"
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        publisher.registerHandlerFor(TestStepStarted.class, e -> {
            if (e.getTestStep() instanceof HookTestStep) {
                // @Before hook'undan önce senaryo adını profiler'a bildir.
                Profiler.setScenario(e.getTestCase().getName());
            }
        });

        publisher.registerHandlerFor(TestStepFinished.class, e -> {
            String kind;
            String text;
            int line;

            if (e.getTestStep() instanceof PickleStepTestStep step) {
                kind = "step";
                text = step.getStep().getKeyword() + step.getStep().getText();
                line = step.getStep().getLine();
            } else if (e.getTestStep() instanceof HookTestStep hook) {
                kind = "hook";
                text = hook.getHookType() + " " + hook.getCodeLocation();
                line = e.getTestCase().getLocation().getLine();
            } else {
                return;
            }

            write(kind,
                    e.getInstant().toEpochMilli() - e.getResult().getDuration().toMillis(),
                    e.getResult().getDuration().toNanos() / 1_000_000.0,
                    e.getResult().getStatus().name(),
                    e.getTestCase().getName(),
                    line,
                    text);
        });

        publisher.registerHandlerFor(TestCaseFinished.class, e -> write(
                "scenario",
                e.getInstant().toEpochMilli() - e.getResult().getDuration().toMillis(),
                e.getResult().getDuration().toNanos() / 1_000_000.0,
                e.getResult().getStatus().name(),
                e.getTestCase().getName(),
                e.getTestCase().getLocation().getLine(),
                ""
        ));
    }

    private static synchronized void write(
            String kind,
            long startEpochMs,
            double durationMs,
            String status,
            String scenario,
            int line,
            String text
    ) {
        try {
            Files.writeString(
                    STEPS,
                    kind + "," + startEpochMs + ","
                            + String.format(Locale.ROOT, "%.1f", durationMs) + ","
                            + status + ","
                            + Profiler.csv(scenario) + ","
                            + line + ","
                            + Profiler.csv(text)
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Profiling must never fail the test.
        }
    }
}
