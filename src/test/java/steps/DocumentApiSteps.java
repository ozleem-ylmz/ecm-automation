package steps;

import com.google.gson.JsonObject;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/*
 * Master plan batch'lerinin ortak belge eylemleri: bir kullanıcının belge
 * üzerinde bir işlemi denemesi, beklenen durum kodu ve belgenin kalıcı
 * durumunun (başlık, statü, silinme, güncel sürüm, etiketler) değişmediğinin
 * ya da beklendiği gibi değiştiğinin API ile doğrulanması.
 *
 * İşlem adları (Gherkin'de):
 *   okuma | indirme | yeniden adlandırma | etiket ekleme | yeni sürüm |
 *   soft delete | geri yükleme | hard delete | checkout | checkin |
 *   statü <draft|under_review|published|archived> | taşıma <klasör takma adı>
 */
public class DocumentApiSteps {

    private static final String RENAME_SUFFIX = " (qa-yeni ad)";

    private final Map<String, Snapshot> snapshots = new HashMap<>();
    private final Map<String, String> attemptedTags = new HashMap<>();

    record Snapshot(String title, String status, boolean deleted, String currentVersionId, TreeSet<String> tags) {
    }

    @When("{string} {string} belgesinde {string} işlemini dener")
    public void belgedeIslemiDener(String userAlias, String documentAlias, String action) {
        ApiTestContext.lastResponse(perform(userAlias, documentAlias, action));
    }

    @Then("{string} {string} belgesinde şu işlemlerin her biri {int} ile reddedilmelidir")
    public void islemlerinHerBiriReddedilmeli(String userAlias, String documentAlias, int expected, DataTable table) {
        List<String> mismatches = new ArrayList<>();
        for (String action : table.asList()) {
            EcmApiClient.ApiResponse r = perform(userAlias, documentAlias, action.trim());
            if (r.status() != expected) {
                mismatches.add(action + " -> HTTP " + r.status() + " " + EcmApiClient.redact(r.body()));
            }
        }
        if (!mismatches.isEmpty()) {
            throw new AssertionError("'" + userAlias + "' için '" + documentAlias + "' belgesinde her işlem HTTP "
                    + expected + " ile reddedilmeliydi. Beklenmeyen: " + mismatches);
        }
    }

    @Then("{string} {string} belgesinde şu işlemleri başarıyla yapabilmelidir")
    public void islemleriBasariylaYapabilmeli(String userAlias, String documentAlias, DataTable table) {
        List<String> failures = new ArrayList<>();
        for (String action : table.asList()) {
            EcmApiClient.ApiResponse r = perform(userAlias, documentAlias, action.trim());
            if (r.status() < 200 || r.status() >= 300) {
                failures.add(action + " -> HTTP " + r.status() + " " + EcmApiClient.redact(r.body()));
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("'" + userAlias + "' '" + documentAlias + "' belgesinde bu işlemleri yapabilmeliydi: "
                    + failures);
        }
    }

    @Given("{string} belgesinin mevcut durumu kaydedilmiştir")
    public void belgeninMevcutDurumuKaydedilmistir(String documentAlias) {
        snapshots.put(documentAlias, snapshot(documentAlias));
    }

    @Then("{string} belgesi kaydedilen durumundan değişmemiş olmalıdır")
    public void kaydedilenDurumdanDegismemisOlmali(String documentAlias) {
        Snapshot before = requireSnapshot(documentAlias);
        Snapshot after = snapshot(documentAlias);
        if (!before.equals(after)) {
            throw new AssertionError("'" + documentAlias + "' değişmemeliydi.\nÖnce: " + before + "\nSonra: " + after);
        }
    }

    @Then("{string} belgesinin başlığı yeniden adlandırılmış olmalıdır")
    public void basligiYenidenAdlandirilmisOlmali(String documentAlias) {
        String expected = requireSnapshot(documentAlias).title() + RENAME_SUFFIX;
        String actual = snapshot(documentAlias).title();
        if (!expected.equals(actual)) {
            throw new AssertionError("Başlık '" + expected + "' olmalıydı, '" + actual + "' bulundu.");
        }
    }

    @Then("{string} belgesi soft delete edilmiş olmalıdır")
    public void softDeleteEdilmisOlmali(String documentAlias) {
        if (!snapshot(documentAlias).deleted()) {
            throw new AssertionError("'" + documentAlias + "' soft delete edilmiş olmalıydı (is_deleted=false).");
        }
    }

    @Then("{string} belgesinin güncel sürümü değişmiş olmalıdır")
    public void guncelSurumuDegismisOlmali(String documentAlias) {
        String before = requireSnapshot(documentAlias).currentVersionId();
        String after = snapshot(documentAlias).currentVersionId();
        if (before.equals(after)) {
            throw new AssertionError("'" + documentAlias + "' için yeni sürüm oluşmalıydı; current_version_id değişmedi.");
        }
    }

    @Then("{string} {string} belgesini okuyup içeriğini indirebilmelidir")
    public void okuyupIcerigiIndirebilmeli(String userAlias, String documentAlias) {
        perform(userAlias, documentAlias, "okuma").expect(200, "'" + userAlias + "' belge okuma");
        EcmApiClient.ApiResponse content = perform(userAlias, documentAlias, "indirme")
                .expect(200, "'" + userAlias + "' belge indirme");
        String expected = "QA otomasyon içeriği " + ApiTestContext.name(documentAlias);
        if (!content.body().equals(expected)) {
            throw new AssertionError("İndirilen içerik yüklenen içerikle aynı olmalıydı. Beklenen: '" + expected
                    + "', gelen: '" + content.body() + "'");
        }
    }

    // ---------------------------------------------------------------------

    private EcmApiClient.ApiResponse perform(String userAlias, String documentAlias, String action) {
        EcmApiClient.Session s = userAlias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(userAlias);
        String id = ApiTestContext.id(documentAlias);
        String base = "/v1/documents/" + id;
        if (action.startsWith("statü ")) {
            return EcmApiClient.patch(s, base + "/status", Map.of("status", action.substring("statü ".length()).trim()));
        }
        if (action.startsWith("taşıma ")) {
            String folder = ApiTestContext.id(action.substring("taşıma ".length()).trim());
            return EcmApiClient.put(s, base + "/folder", Map.of("folder_id", folder));
        }
        return switch (action) {
            case "okuma" -> EcmApiClient.get(s, base);
            case "indirme" -> EcmApiClient.get(s, base + "/content");
            case "yeniden adlandırma" -> EcmApiClient.patch(s, base + "/title",
                    Map.of("title", currentTitle(documentAlias) + RENAME_SUFFIX));
            case "etiket ekleme" -> {
                String tag = "qa-" + TestDataFactory.unique("tag").toLowerCase();
                attemptedTags.put(documentAlias, tag);
                yield EcmApiClient.post(s, base + "/tags", Map.of("tag_ids", List.of(), "tag_name", tag));
            }
            case "yeni sürüm" -> EcmApiClient.post(s, "/v1/documents/unclassified",
                    TestDataFactory.newVersionBody(documentAlias, "Patch", "QA yeni sürüm " + System.nanoTime()));
            case "soft delete" -> EcmApiClient.delete(s, base);
            case "geri yükleme" -> EcmApiClient.post(s, base + "/restore", Map.of());
            case "hard delete" -> EcmApiClient.delete(s, base + "/hard");
            case "checkout" -> EcmApiClient.post(s, base + "/checkout", Map.of());
            case "checkin" -> EcmApiClient.post(s, base + "/checkin", Map.of());
            default -> throw new IllegalArgumentException("Bilinmeyen belge işlemi: '" + action + "'");
        };
    }

    private static String currentTitle(String documentAlias) {
        return readAsAdmin(documentAlias).getAsJsonObject("document").get("title").getAsString();
    }

    private static JsonObject readAsAdmin(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma (yönetici, " + documentAlias + ")").json();
    }

    static Snapshot snapshot(String documentAlias) {
        JsonObject d = readAsAdmin(documentAlias).getAsJsonObject("document");
        TreeSet<String> tags = new TreeSet<>();
        EcmApiClient.ApiResponse t = EcmApiClient.get(TestDataFactory.admin(),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags");
        if (t.status() == 200) {
            t.json().getAsJsonArray("tags").forEach(e -> tags.add(e.getAsJsonObject().get("name").getAsString()));
        }
        return new Snapshot(
                d.get("title").getAsString(),
                d.get("status").getAsString(),
                d.get("is_deleted").getAsBoolean(),
                d.get("current_version_id").isJsonNull() ? "" : d.get("current_version_id").getAsString(),
                tags
        );
    }

    private Snapshot requireSnapshot(String documentAlias) {
        Snapshot s = snapshots.get(documentAlias);
        if (s == null) {
            throw new IllegalStateException("'" + documentAlias + "' için kayıtlı durum yok; önce durum kaydedilmeli.");
        }
        return s;
    }
}
