package utils;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Çakışmasız test verisi isimleri.
 *
 * System.currentTimeMillis() ile üretilen isimler aynı milisaniyede iki kez
 * üretilebilir (ör. paralel worker'lar). ECM'nin upload ekranı, aynı klasörde
 * aynı başlık varsa yeni belge yerine mevcut belgeye yeni versiyon ekliyor
 * (DocumentNewUnclassified.tsx titleMatch), bu yüzden isim çakışması testin
 * yaptığı işi sessizce değiştirir.
 *
 * Biçim: "<prefix> <runId>-<workerId>-<sayaç>", ör. "QA Parent k3x9q2-w0-0007"
 *   runId    : JVM başına bir kez (zaman + rastgele), koşular arası benzersiz
 *   workerId : -Dtest.worker.id (varsayılan "w0"); paralel koşuda worker başına
 *   sayaç    : JVM içinde thread-safe artan sayı
 */
public final class TestData {

    public static final String RUN_ID = newRunId();

    public static final String WORKER_ID =
            System.getProperty("test.worker.id", "w0").replaceAll("[^A-Za-z0-9]", "");

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private TestData() {
    }

    public static String unique(String prefix) {
        return prefix + " " + RUN_ID + "-" + WORKER_ID + "-"
                + String.format("%04d", COUNTER.incrementAndGet());
    }

    private static String newRunId() {
        String time = Long.toString(System.currentTimeMillis() / 1000, 36);
        String rnd = Integer.toString(new SecureRandom().nextInt(36 * 36 * 36), 36);
        return time.substring(Math.max(0, time.length() - 4)) + rnd;
    }
}
