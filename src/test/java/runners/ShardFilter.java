package runners;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.PostDiscoveryFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * SÜREÇ İZOLASYONLU PARALEL KOŞU (sharding).
 *
 * BrowserManager statik ve Playwright nesneleri thread-safe değil; bu yüzden
 * paralellik JVM içinde (Cucumber parallel) DEĞİL, ayrı JVM'lerle yapılır:
 * Surefire her ShardRunner<K> sınıfını kendi fork'unda çalıştırır
 * (-DforkCount=N -DreuseForks=false -Dtest=ShardRunner1,...,ShardRunnerN).
 *
 * Her fork tüm senaryoları keşfeder; bu filtre keşiften sonra senaryoları
 * deterministik olarak böler: keşif sırasındaki i. senaryo (i mod N)+1
 * numaralı shard'a düşer. Her JVM aynı sırayı gördüğü için her senaryo
 * TAM OLARAK BİR shard'da koşar (kayıp/çift yok).
 *
 *   -Dtest.shards=N       toplam shard sayısı (zorunlu, ShardRunner koşularında)
 *
 * Seri koşu (TestRunner, IDE) etkilenmez: suite sınıfı ShardRunner değilse
 * filtre her şeyi dahil eder.
 *
 * -Dcucumber.filter.name / tags dağıtımı DEĞİŞTİRMEZ: Cucumber eşleşmeyen
 * senaryoyu kendi shard'ında "skipped" olarak raporlar (Scenario Outline
 * örneklerinin görünen adı pickle adından farklı olabildiği için burada
 * isimle eleme yapmak senaryo kaybettirebilirdi).
 *
 * Yan etki (bilinçli): shard JVM'inde test.worker.id verilmemişse "s<K>"
 * olarak ayarlanır; TestData isimleri ve runtime coverage/profil çıktıları
 * shard başına ayrışır. Keşif, yürütmeden (ve bu sınıfların yüklenmesinden)
 * önce çalışır.
 *
 * Kayıt: META-INF/services/org.junit.platform.launcher.PostDiscoveryFilter
 * + junit-platform.properties (postDiscoveryFilters.autoDetection.enabled).
 */
public class ShardFilter implements PostDiscoveryFilter {

    private static final Pattern SHARD_CLASS = Pattern.compile("(?:^|\\.)ShardRunner(\\d+)$");

    // suite unique id -> (scenario unique id -> shard no)
    private final Map<UniqueId, Map<UniqueId, Integer>> assignments = new HashMap<>();

    @Override
    public FilterResult apply(TestDescriptor descriptor) {

        if (!descriptor.isTest()) {
            return FilterResult.included("container");
        }

        Optional<TestDescriptor> suite = suiteOf(descriptor);
        if (suite.isEmpty()) {
            return FilterResult.included("not in a suite");
        }

        Matcher m = SHARD_CLASS.matcher(suite.get().getUniqueId().getLastSegment().getValue());
        if (!m.find()) {
            return FilterResult.included("not a shard suite");
        }

        int shard = Integer.parseInt(m.group(1));
        int total = totalShards();

        if (shard < 1 || shard > total) {
            return FilterResult.excluded("shard " + shard + " > test.shards=" + total);
        }

        Map<UniqueId, Integer> map = assignments.computeIfAbsent(
                suite.get().getUniqueId(), k -> assign(suite.get(), shard, total));

        Integer owner = map.get(descriptor.getUniqueId());
        return owner != null && owner == shard
                ? FilterResult.included("shard " + shard + "/" + total)
                : FilterResult.excluded("belongs to shard " + owner + "/" + total);
    }

    private static Map<UniqueId, Integer> assign(TestDescriptor suite, int shard, int total) {

        List<TestDescriptor> tests = new ArrayList<>();
        collectTests(suite, tests);

        Map<UniqueId, Integer> map = new HashMap<>();
        int index = 0;
        int mine = 0;
        for (TestDescriptor t : tests) {
            int owner = (index++ % total) + 1;
            map.put(t.getUniqueId(), owner);
            if (owner == shard) {
                mine++;
            }
        }

        if (System.getProperty("test.worker.id") == null) {
            System.setProperty("test.worker.id", "s" + shard);
        }

        System.out.println("[SHARD] shard " + shard + "/" + total + ": " + mine + " of "
                + index + " scenarios, worker id " + System.getProperty("test.worker.id"));

        return map;
    }

    // Keşif sırası (LinkedHashSet çocuklar) her JVM'de aynıdır.
    private static void collectTests(TestDescriptor node, List<TestDescriptor> out) {
        for (TestDescriptor child : node.getChildren()) {
            if (child.isTest()) {
                out.add(child);
            }
            collectTests(child, out);
        }
    }

    private static Optional<TestDescriptor> suiteOf(TestDescriptor descriptor) {
        Optional<TestDescriptor> current = Optional.of(descriptor);
        while (current.isPresent()) {
            if ("suite".equals(current.get().getUniqueId().getLastSegment().getType())) {
                return current;
            }
            current = current.get().getParent();
        }
        return Optional.empty();
    }

    private static int totalShards() {
        String value = System.getProperty("test.shards");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "ShardRunner koşusu için -Dtest.shards=N verilmelidir "
                            + "(tools/parallel/run-parallel.ps1 kullanın).");
        }
        return Integer.parseInt(value.trim());
    }

}
