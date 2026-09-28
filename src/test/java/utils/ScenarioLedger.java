package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/*
 * Koşulan her senaryonun kaydı: target/scenario-ledger[-<worker>].csv
 *   key (feature uri:satır), status, süre (ms), worker, senaryo adı
 *
 * Amaç: paralel/shard koşularda "her senaryo TAM OLARAK bir kez koştu"
 * kanıtı. Surefire XML'i Scenario Outline örneklerini yalnızca
 * "Example #1.1" adıyla yazdığı için kimlik olarak yetersiz; uri:satır her
 * pickle için tekildir ve JVM'ler arasında aynıdır (pickle id her koşuda
 * rastgele üretilir, bu yüzden kullanılmaz).
 */
public final class ScenarioLedger {

    private static final String WORKER =
            System.getProperty("test.worker.id", "").replaceAll("[^A-Za-z0-9]", "");

    private static final Path OUTPUT = Path.of("target",
            "scenario-ledger" + (WORKER.isEmpty() ? "" : "-" + WORKER) + ".csv");

    private static boolean initialized;

    private ScenarioLedger() {
    }

    public static synchronized void record(String uri, int line, String name, String status, long millis) {
        try {
            if (!initialized) {
                Files.createDirectories(OUTPUT.getParent());
                Files.writeString(OUTPUT, "key,status,millis,worker,scenario" + System.lineSeparator(),
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                initialized = true;
            }
            String key = uri.replaceFirst("^classpath:", "") + ":" + line;
            Files.writeString(OUTPUT,
                    csv(key) + "," + csv(status) + "," + millis + "," + csv(WORKER.isEmpty() ? "w0" : WORKER)
                            + "," + csv(name) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Kanıt kaydı testi asla düşürmemeli; eksik kayıt orkestratörde yakalanır.
            System.out.println("[LEDGER] yazılamadı: " + e);
        }
    }

    private static String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"")) + "\"";
    }
}
