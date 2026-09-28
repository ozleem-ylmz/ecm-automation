package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/*
 * Batch 35 — plan dışı eşzamanlılık testleri. Aynı kaynağa aynı anda gelen
 * istekler: kaybeden taraf 409 (çakışma) almalı, sunucu hatası (5xx) olmamalı,
 * kalıcı durum tutarlı kalmalıdır.
 *
 * ECM kaynakları: db/migrations/0009_document_version.sql UNIQUE(document_id, major, minor, patch),
 * api/rest/error.rs From<sqlx::Error> (23505 -> 409), engine/documents/locks.rs,
 * folders unique name index, POST /tags get-or-create.
 */
public class ConcurrencySteps {

    private final List<EcmApiClient.ApiResponse> responses = new ArrayList<>();

    /* Tüm istekleri aynı anda serbest bırakır (başlangıç kapısı), yanıtları istek sırasıyla döndürür. */
    static List<EcmApiClient.ApiResponse> race(int n, Function<Integer, EcmApiClient.ApiResponse> request) {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch gate = new CountDownLatch(1);
        try {
            List<Future<EcmApiClient.ApiResponse>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final int idx = i;
                Callable<EcmApiClient.ApiResponse> c = () -> {
                    gate.await();
                    return request.apply(idx);
                };
                futures.add(pool.submit(c));
            }
            gate.countDown();
            List<EcmApiClient.ApiResponse> out = new ArrayList<>();
            for (Future<EcmApiClient.ApiResponse> f : futures) {
                out.add(f.get(120, TimeUnit.SECONDS));
            }
            return out;
        } catch (Exception e) {
            throw new AssertionError("Eşzamanlı istekler tamamlanamadı", e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static Map<Integer, Integer> histogram(List<EcmApiClient.ApiResponse> rs) {
        Map<Integer, Integer> h = new TreeMap<>();
        rs.forEach(r -> h.merge(r.status(), 1, Integer::sum));
        return h;
    }

    private static String serverErrors(List<EcmApiClient.ApiResponse> rs) {
        StringBuilder sb = new StringBuilder();
        rs.stream().filter(r -> r.status() >= 500).limit(3)
                .forEach(r -> sb.append("  HTTP ").append(r.status()).append(' ').append(EcmApiClient.redact(r.body())).append('\n'));
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // CON-01 — eşzamanlı yeni sürüm
    // ---------------------------------------------------------------------

    @When("yönetici {string} belgesine aynı anda {int} yeni sürüm kaydetmeyi dener")
    public void eszamanliYeniSurum(String documentAlias, int n) {
        EcmApiClient.Session admin = TestDataFactory.admin();
        List<Map<String, Object>> bodies = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            bodies.add(TestDataFactory.newVersionBody(documentAlias, "Patch", "QA eşzamanlı sürüm " + i + " " + System.nanoTime()));
        }
        ApiTestContext.putName(documentAlias + "#versionsBefore", String.valueOf(VersionSteps.versionsAsc(documentAlias).size()));
        responses.clear();
        responses.addAll(race(n, i -> EcmApiClient.post(admin, "/v1/documents/unclassified", bodies.get(i))));
    }

    @Then("eşzamanlı sürüm isteklerinin hiçbiri sunucu hatası vermemeli ve {string} belgesinin sürüm geçmişi tutarlı kalmalıdır")
    public void surumYarisiTutarli(String documentAlias) {
        int before = Integer.parseInt(ApiTestContext.name(documentAlias + "#versionsBefore"));
        List<JsonObject> versions = VersionSteps.versionsAsc(documentAlias);
        long created = responses.stream().filter(r -> r.status() == 201).count();
        Set<String> numbers = new HashSet<>();
        int current = 0;
        for (JsonObject v : versions) {
            numbers.add(v.get("version_major").getAsInt() + "." + v.get("version_minor").getAsInt() + "." + v.get("version_patch").getAsInt());
            current += v.get("is_current").getAsBoolean() ? 1 : 0;
        }
        String ev = "Yanıt durumları: " + histogram(responses) + "\nSürüm sayısı: " + before + " -> " + versions.size()
                + " (201 sayısı " + created + ")\nBenzersiz sürüm numarası: " + numbers.size() + ", güncel işaretli: " + current
                + "\n" + serverErrors(responses);
        Allure.addAttachment("CON-01 kanıtı — eşzamanlı yeni sürüm", "text/plain", ev);
        List<String> failures = new ArrayList<>();
        if (versions.size() != before + created) {
            failures.add("Başarılı (201) istek sayısı kadar sürüm eklenmeliydi");
        }
        if (numbers.size() != versions.size() || current != 1) {
            failures.add("Sürüm numaraları benzersiz ve tam olarak bir güncel sürüm olmalıydı");
        }
        if (responses.stream().anyMatch(r -> r.status() >= 500)) {
            failures.add("Kaybeden istekler 409 (çakışma) almalıydı; sunucu hatası (5xx) döndü");
        }
        if (responses.stream().anyMatch(r -> r.status() != 201 && r.status() != 409 && r.status() < 500)) {
            failures.add("Beklenmeyen durum kodu");
        }
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures) + "\n" + ev);
        }
    }

    // ---------------------------------------------------------------------
    // CON-02 — aynı adla eşzamanlı klasör
    // ---------------------------------------------------------------------

    @Then("{string} altında aynı adla {int} eşzamanlı alt klasör isteğinden tam olarak biri oluşmalı, diğerleri 409 almalıdır")
    public void eszamanliKlasor(String parentAlias, int n) {
        EcmApiClient.Session admin = TestDataFactory.admin();
        String parentId = ApiTestContext.id(parentAlias);
        String name = TestDataFactory.unique("QA Eşzamanlı Klasör");
        List<EcmApiClient.ApiResponse> rs = race(n, i -> EcmApiClient.post(admin, "/v1/folders",
                Map.of("name", name, "parent_id", parentId, "inherit_permissions", true)));
        List<String> ids = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(admin, "/v1/folders/" + parentId).expect(200, "Üst klasör").json().getAsJsonArray("children")) {
            if (name.equals(e.getAsJsonObject().get("name").getAsString())) {
                ids.add(e.getAsJsonObject().get("id").getAsString());
            }
        }
        ids.forEach(id -> ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + id)));
        long ok = rs.stream().filter(r -> r.status() == 200).count();
        long conflict = rs.stream().filter(r -> r.status() == 409 && r.body().contains("A folder with this name already exists")).count();
        String ev = "Durumlar: " + histogram(rs) + "\nAynı adlı alt klasör sayısı: " + ids.size() + "\n" + serverErrors(rs);
        Allure.addAttachment("CON-02 kanıtı — eşzamanlı klasör", "text/plain", ev);
        if (ok != 1 || conflict != n - 1 || ids.size() != 1) {
            throw new AssertionError("Tam olarak bir klasör oluşmalı ve diğer " + (n - 1) + " istek 409 almalıydı\n" + ev);
        }
    }

    // ---------------------------------------------------------------------
    // CON-03 — aynı etiketin eşzamanlı oluşturulması
    // ---------------------------------------------------------------------

    @Then("aynı etiket adıyla {int} eşzamanlı istek tek bir etiket oluşturmalı ve hepsi aynı kimliği döndürmelidir")
    public void eszamanliEtiket(int n) {
        EcmApiClient.Session admin = TestDataFactory.admin();
        String tag = "qacon" + Long.toString(System.nanoTime(), 36);
        List<EcmApiClient.ApiResponse> rs = race(n, i -> EcmApiClient.post(admin, "/v1/tags", Map.of("name", tag)));
        Set<String> ids = new HashSet<>();
        rs.stream().filter(r -> r.status() < 300).forEach(r -> ids.add(r.json().get("id").getAsString()));
        // Yarıştan sonra tek bir get-or-create daha: kalıcı etiket tekse aynı kimliği döndürür.
        String after = EcmApiClient.post(admin, "/v1/tags", Map.of("name", tag)).json().get("id").getAsString();
        String ev = "Durumlar: " + histogram(rs) + "\nDönen farklı kimlik: " + ids.size() + "\nSonraki get-or-create kimliği aynı mı: "
                + ids.contains(after) + "\n" + serverErrors(rs);
        Allure.addAttachment("CON-03 kanıtı — eşzamanlı etiket", "text/plain", ev);
        if (rs.stream().anyMatch(r -> r.status() >= 300) || ids.size() != 1 || !ids.contains(after)) {
            throw new AssertionError("Etiket get-or-create eşzamanlı çağrıda tek kayıt ve tek kimlik vermeliydi\n" + ev);
        }
    }

    // ---------------------------------------------------------------------
    // CON-04 — iki kullanıcının aynı anda checkout denemesi
    // ---------------------------------------------------------------------

    @Then("yönetici ve {string} {string} belgesini aynı anda checkout ettiğinde tam olarak biri kilidi almalı, diğeri 409 almalıdır")
    public void eszamanliCheckout(String userAlias, String documentAlias) {
        EcmApiClient.Session admin = TestDataFactory.admin();
        EcmApiClient.Session user = ApiTestContext.session(userAlias);
        String path = "/v1/documents/" + ApiTestContext.id(documentAlias);
        StringBuilder ev = new StringBuilder();
        for (int round = 1; round <= 3; round++) {
            List<EcmApiClient.Session> who = List.of(admin, user);
            List<EcmApiClient.ApiResponse> rs = race(2, i -> EcmApiClient.post(who.get(i), path + "/checkout", Map.of()));
            JsonObject lock = EcmApiClient.get(admin, path + "/lock").expect(200, "Kilit").json();
            EcmApiClient.delete(admin, path + "/lock");
            ev.append("Tur ").append(round).append(": yönetici ").append(rs.get(0).status()).append(", ").append(userAlias)
                    .append(' ').append(rs.get(1).status()).append(", kilit sahibi ")
                    .append(lock.get("locked_by").isJsonNull() ? "yok" : lock.get("locked_by").getAsString().equals(admin.userId) ? "yönetici" : userAlias)
                    .append('\n');
            long winners = rs.stream().filter(r -> r.status() == 200).count();
            long losers = rs.stream().filter(r -> r.status() == 409).count();
            int winner = rs.get(0).status() == 200 ? 0 : 1;
            String expectedHolder = who.get(winner).userId;
            if (winners != 1 || losers != 1 || !lock.get("locked").getAsBoolean()
                    || !expectedHolder.equals(lock.get("locked_by").getAsString())) {
                Allure.addAttachment("CON-04 kanıtı — eşzamanlı checkout", "text/plain", ev.toString());
                throw new AssertionError("Tam olarak bir kazanan olmalı ve kilit kazananda olmalıydı\n" + ev);
            }
        }
        Allure.addAttachment("CON-04 kanıtı — eşzamanlı checkout", "text/plain", ev.toString());
    }
}
