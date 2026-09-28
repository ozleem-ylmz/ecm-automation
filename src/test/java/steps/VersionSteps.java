package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * Batch 25 — sürümler: eski sürüm indirme, geri alma (rollback), sürüm başına
 * sınıf erişimi, sürüm detay sayfası ve sürüm etiketleri.
 * ECM kaynakları: engine/documents/versions*.rs rollback, runtime.rs
 * assert_version_access (sürümün KENDİ sınıfı), labels.rs, DocumentVersionDetail.tsx.
 */
public class VersionSteps {

    private static final int UI_WAIT_MS = 20000;

    // ---------------------------------------------------------------------
    // Kurulum
    // ---------------------------------------------------------------------

    @Given("{string} {string} belgesine {string} içerikli yeni bir yama sürümü eklemiştir")
    public void yeniYamaSurumuEklemistir(String userAlias, String documentAlias, String text) {
        EcmApiClient.post(session(userAlias), "/v1/documents/unclassified",
                        TestDataFactory.newVersionBody(documentAlias, "Patch", text))
                .expect(201, "Yeni yama sürümü (" + documentAlias + ")");
    }

    @Given("{string} belgesinin sürümleri {string} ve {string} olarak adlandırılmıştır")
    public void surumleriAdlandirilmistir(String documentAlias, String firstAlias, String secondAlias) {
        List<JsonObject> v = versionsAsc(documentAlias);
        ApiTestContext.putId(firstAlias, v.get(0).get("id").getAsString());
        ApiTestContext.putId(secondAlias, v.get(1).get("id").getAsString());
        ApiTestContext.putName(firstAlias + "#doc", documentAlias);
        ApiTestContext.putName(secondAlias + "#doc", documentAlias);
    }

    // ---------------------------------------------------------------------
    // Eski sürüm içeriği (VER-02)
    // ---------------------------------------------------------------------

    @Then("{string} {string} sürümünün içeriğini indirdiğinde {string} almalıdır")
    public void surumIceriginiIndirdigindeAlmali(String userAlias, String versionAlias, String expectedContentKey) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(session(userAlias), "/v1/documents/versions/" + ApiTestContext.id(versionAlias) + "/content")
                .expect(200, "Sürüm içeriği (" + versionAlias + ")");
        String expected = expectedContent(expectedContentKey);
        if (!expected.equals(r.body())) {
            throw new AssertionError("'" + versionAlias + "' içeriği '" + expected + "' olmalıydı, '" + r.body() + "' geldi.");
        }
    }

    @When("{string} {string} sürümünün içeriğini indirmeyi dener")
    public void surumIceriginiIndirmeyiDener(String userAlias, String versionAlias) {
        ApiTestContext.lastResponse(EcmApiClient.get(session(userAlias),
                "/v1/documents/versions/" + ApiTestContext.id(versionAlias) + "/content"));
    }

    @When("{string} {string} belgesinin {string} sürüm detayını istemeyi dener")
    public void surumDetayiniIstemeyiDener(String userAlias, String documentAlias, String versionAlias) {
        ApiTestContext.lastResponse(EcmApiClient.get(session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions/" + ApiTestContext.id(versionAlias)));
    }

    @Then("{string} {string} belgesinin sürüm listesinde {string} sürümünü görmemelidir")
    public void surumListesindeGormemeli(String userAlias, String documentAlias, String versionAlias) {
        String id = ApiTestContext.id(versionAlias);
        for (JsonElement e : EcmApiClient.get(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions")
                .expect(200, "Sürüm listesi").json().getAsJsonArray("items")) {
            if (id.equals(e.getAsJsonObject().get("id").getAsString())) {
                throw new AssertionError("'" + userAlias + "' kısıtlı '" + versionAlias + "' sürümünü listede görmemeliydi.");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Geri alma (VER-01 arayüz, VER-04 API)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} belgesinin {string} sürüm sayfasından geri alma yapar")
    public void arayuzdeGeriAlmaYapar(String documentAlias, String versionAlias) {
        Page page = adminPage();
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias)
                + "/versions/" + ApiTestContext.id(versionAlias));
        Locator restore = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore this version"));
        try {
            restore.first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Eski sürüm sayfasında 'Restore this version' görünmedi. URL: " + page.url(), e);
        }
        restore.first().click();
        Locator dialog = page.locator("[aria-modal='true']");
        dialog.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        Response rb = page.waitForResponse(
                r -> "POST".equals(r.request().method()) && r.url().endsWith("/rollback"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Restore this version")).click()
        );
        if (rb.status() != 200) {
            throw new AssertionError("Arayüzden geri alma HTTP " + rb.status() + " döndü: " + rb.text());
        }
        page.waitForURL("**/documents/" + ApiTestContext.id(documentAlias), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("{string} belgesinde {int} sürüm olmalı ve yeni güncel sürüm {string} içeriğini taşımalıdır")
    public void surumSayisiVeGuncelIcerik(String documentAlias, int count, String contentKey) {
        List<JsonObject> v = versionsAsc(documentAlias);
        if (v.size() != count) {
            throw new AssertionError("'" + documentAlias + "' " + count + " sürüm içermeliydi, " + v.size() + " var.");
        }
        JsonObject newest = v.get(v.size() - 1);
        if (!newest.get("is_current").getAsBoolean()) {
            throw new AssertionError("En yüksek numaralı sürüm güncel olmalıydı: " + newest);
        }
        String content = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content")
                .expect(200, "Güncel içerik").body();
        if (!expectedContent(contentKey).equals(content)) {
            throw new AssertionError("Geri alma sonrası güncel içerik '" + expectedContent(contentKey) + "' olmalıydı, '" + content + "' geldi.");
        }
        ApiTestContext.putName(documentAlias + "#current", newest.get("id").getAsString());
        ApiTestContext.putName(documentAlias + "#currentLabel", label(newest));
    }

    @Then("sayfa yenilendiğinde {string} belgesinin güncel sürümü aynı kalmalı ve arayüzde gösterilmelidir")
    public void sayfaYenilenenceGuncelSurumAyni(String documentAlias) {
        Page page = BrowserManager.page;
        page.reload();
        String labelText = ApiTestContext.name(documentAlias + "#currentLabel");
        try {
            page.getByText(labelText).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Yenilemeden sonra güncel sürüm etiketi '" + labelText + "' görünmedi.", e);
        }
        String current = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge").json().getAsJsonObject("document").get("current_version_id").getAsString();
        if (!current.equals(ApiTestContext.name(documentAlias + "#current"))) {
            throw new AssertionError("Yenilemeden sonra güncel sürüm değişmemeliydi.");
        }
    }

    @When("{string} {string} belgesini {string} sürümüne geri almayı dener")
    public void geriAlmayiDener(String userAlias, String documentAlias, String versionAlias) {
        ApiTestContext.putName(documentAlias + "#countBefore", String.valueOf(versionsAsc(documentAlias).size()));
        ApiTestContext.lastResponse(EcmApiClient.post(session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions/" + ApiTestContext.id(versionAlias) + "/rollback",
                Map.of()));
    }

    @Then("{string} belgesinin sürüm sayısı değişmemiş olmalıdır")
    public void surumSayisiDegismemis(String documentAlias) {
        int before = Integer.parseInt(ApiTestContext.name(documentAlias + "#countBefore"));
        int after = versionsAsc(documentAlias).size();
        if (before != after) {
            throw new AssertionError("Reddedilen işlem sürüm sayısını değiştirmemeliydi: " + before + " -> " + after);
        }
    }

    // ---------------------------------------------------------------------
    // Sürüm detay sayfası (VER-05)
    // ---------------------------------------------------------------------

    @Then("{string} arayüzde {string} belgesinin {string} sürüm sayfasında sürüm numarasını ve dosya adını görmelidir")
    public void surumSayfasindaNumaraVeDosyaAdi(String userAlias, String documentAlias, String versionAlias) {
        Page page = openVersionPage(userAlias, documentAlias, versionAlias);
        JsonObject v = version(documentAlias, versionAlias);
        try {
            page.getByText(label(v)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.getByText(v.get("original_filename").getAsString()).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Sürüm sayfasında " + label(v) + " ve dosya adı görünmedi.", e);
        }
    }

    @Then("sürüm sayfasında Restore this version butonu görünmelidir")
    public void surumSayfasindaRestoreGorunmeli() {
        if (!buttonVisible("Restore this version")) {
            throw new AssertionError("Yazma yetkili kullanıcı için eski sürümde 'Restore this version' görünmeliydi.");
        }
    }

    @Then("sürüm sayfasında Restore this version butonu görünmemelidir")
    public void surumSayfasindaRestoreGorunmemeli() {
        if (buttonVisible("Restore this version")) {
            throw new AssertionError("'Restore this version' görünmemeliydi. URL: " + BrowserManager.page.url());
        }
    }

    @Then("sürüm sayfasında Download butonu ve Current rozeti {string} olmalıdır")
    public void downloadVeCurrentRozeti(String currentExpectation) {
        if (!buttonVisible("Download")) {
            throw new AssertionError("Sürüm sayfasında Download butonu görünmeliydi.");
        }
        boolean currentBadge = BrowserManager.page.getByText("Current", new Page.GetByTextOptions().setExact(true)).count() > 0;
        boolean expected = "görünür".equals(currentExpectation);
        if (currentBadge != expected) {
            throw new AssertionError("Current rozeti " + currentExpectation + " olmalıydı.");
        }
    }

    @Then("yönetici belge detayındaki sürüm geçmişinde {string} sürümünü yükleyen olarak {string} e-postasını görmelidir")
    public void surumGecmisindeYukleyen(String versionAlias, String userAlias) {
        Page page = adminPage();
        String documentId = BrowserManager.page.url().replaceAll(".*/documents/([0-9a-f-]{36}).*", "$1");
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + documentId);
        try {
            page.getByText(ApiTestContext.name(userAlias)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Sürüm geçmişinde yükleyen e-postası (" + userAlias + ") görünmedi.", e);
        }
    }

    // ---------------------------------------------------------------------
    // Sürüm etiketleri (VER-07, VER-09)
    // ---------------------------------------------------------------------

    @When("{string} {string} sürümüne {string} etiketini eklemeyi dener")
    public void surumeEtiketEklemeyiDener(String userAlias, String versionAlias, String labelText) {
        String documentId = documentOfVersion(versionAlias);
        ApiTestContext.lastResponse(EcmApiClient.post(session(userAlias),
                "/v1/documents/" + documentId + "/versions/" + ApiTestContext.id(versionAlias) + "/labels",
                Map.of("label", labelText)));
    }

    @When("{string} {string} sürümünden {string} etiketini kaldırmayı dener")
    public void surumdenEtiketKaldirmayiDener(String userAlias, String versionAlias, String labelText) {
        String documentId = documentOfVersion(versionAlias);
        ApiTestContext.lastResponse(EcmApiClient.delete(session(userAlias),
                "/v1/documents/" + documentId + "/versions/" + ApiTestContext.id(versionAlias) + "/labels/" + labelText));
    }

    @Given("yönetici {string} sürümüne {string} etiketini eklemiştir")
    public void yoneticiSurumeEtiketEklemistir(String versionAlias, String labelText) {
        String documentId = documentOfVersion(versionAlias);
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + documentId + "/versions/" + ApiTestContext.id(versionAlias) + "/labels",
                Map.of("label", labelText)).expect(201, "Sürüm etiketi ekleme");
    }

    @Then("{string} belgesinin sürüm etiketleri tam olarak {string} olmalıdır")
    public void surumEtiketleriTamOlarak(String documentAlias, String expected) {
        TreeMap<String, TreeSet<String>> actual = new TreeMap<>();
        for (JsonObject v : versionsAsc(documentAlias)) {
            for (JsonElement l : v.getAsJsonArray("labels")) {
                String text = l.isJsonObject() ? l.getAsJsonObject().get("label").getAsString() : l.getAsString();
                actual.computeIfAbsent(label(v), k -> new TreeSet<>()).add(text);
            }
        }
        TreeMap<String, TreeSet<String>> exp = new TreeMap<>();
        if (!expected.isBlank()) {
            for (String pair : expected.split(",")) {
                String[] kv = pair.trim().split("=");
                exp.computeIfAbsent(kv[1].trim(), k -> new TreeSet<>()).add(kv[0].trim());
            }
        }
        if (!exp.equals(actual)) {
            throw new AssertionError("'" + documentAlias + "' sürüm etiketleri " + exp + " olmalıydı, " + actual + " bulundu.");
        }
    }

    @Then("{string} belgesinin toplu sürüm etiketi listesi {string} etiketini {string} sürümüne, {string} etiketini {string} sürümüne eşlemelidir")
    public void topluSurumEtiketListesi(String documentAlias, String l1, String v1, String l2, String v2) {
        JsonObject map = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/version-labels")
                .expect(200, "Toplu sürüm etiketleri").json().getAsJsonObject("labels_by_version");
        assertLabelUnder(map, ApiTestContext.id(v1), l1);
        assertLabelUnder(map, ApiTestContext.id(v2), l2);
        if (labelsUnder(map, ApiTestContext.id(v1)).contains(l2) || labelsUnder(map, ApiTestContext.id(v2)).contains(l1)) {
            throw new AssertionError("Etiketler yanlış sürüme eşlenmiş: " + map);
        }
    }

    @Then("var olmayan bir belgenin toplu sürüm etiketi listesi 404 dönmelidir")
    public void varOlmayanBelgeEtiketListesi404() {
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + java.util.UUID.randomUUID() + "/version-labels").status();
        if (s != 404) {
            throw new AssertionError("Var olmayan belge için 404 bekleniyordu, HTTP " + s);
        }
    }

    // ---------------------------------------------------------------------
    // Sürüm başına sınıf (VER-03)
    // ---------------------------------------------------------------------

    @Given("yönetici erişimi {string} kullanıcısıyla sınırlı {string} sınıfını da oluşturmuştur")
    public void yoneticiIkinciSinifOlusturmustur(String userAlias, String classAlias) {
        new FolderApiSteps().yoneticiErisimiSinirliSinifOlusturmustur(userAlias, classAlias);
    }

    @Given("yönetici {string} belgesine {string} sınıfıyla yeni bir sürüm eklemiştir")
    public void yoneticiFarkliSinifliSurumEklemistir(String documentAlias, String classAlias) {
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(ApiTestContext.name(documentAlias),
                ApiTestContext.id(classAlias), Long.parseLong(ApiTestContext.name(classAlias + "#version")), Map.of(), null);
        body.put("document_id", ApiTestContext.id(documentAlias));
        body.put("version_type", "Patch");
        body.put("folder_id", EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge").json().getAsJsonObject("folder").get("id").getAsString());
        body.put("content", toList("QA açık sınıf sürümü"));
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents", body).expect(201, "Farklı sınıflı yeni sürüm");
    }

    // ---------------------------------------------------------------------

    private static String expectedContent(String key) {
        if (key.startsWith("ilk içerik:")) {
            return "QA otomasyon içeriği " + ApiTestContext.name(key.substring("ilk içerik:".length()).trim());
        }
        return key;
    }

    private static List<Integer> toList(String s) {
        List<Integer> out = new ArrayList<>();
        for (byte b : s.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            out.add(b & 0xff);
        }
        return out;
    }

    private static void assertLabelUnder(JsonObject map, String versionId, String labelText) {
        if (!labelsUnder(map, versionId).contains(labelText)) {
            throw new AssertionError("'" + labelText + "' etiketi " + versionId + " sürümü altında olmalıydı: " + map);
        }
    }

    private static List<String> labelsUnder(JsonObject map, String versionId) {
        List<String> out = new ArrayList<>();
        if (map.has(versionId)) {
            map.getAsJsonArray(versionId).forEach(e -> out.add(e.getAsJsonObject().get("label").getAsString()));
        }
        return out;
    }

    private boolean buttonVisible(String name) {
        Page page = BrowserManager.page;
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        Locator b = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
        return b.count() > 0 && b.first().isVisible();
    }

    private static Page adminPage() {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        }
        return BrowserManager.page;
    }

    private static Page openVersionPage(String userAlias, String documentAlias, String versionAlias) {
        BrowserManager.startBrowser();
        if (userAlias.equals("yönetici")) {
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        } else {
            BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(userAlias), TestDataFactory.TEST_USER_PASSWORD);
        }
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias) + "/versions/" + ApiTestContext.id(versionAlias));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true))
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return page;
    }

    private static JsonObject version(String documentAlias, String versionAlias) {
        String id = ApiTestContext.id(versionAlias);
        return versionsAsc(documentAlias).stream().filter(v -> id.equals(v.get("id").getAsString())).findFirst()
                .orElseThrow(() -> new AssertionError("Sürüm bulunamadı: " + versionAlias));
    }

    private static String documentOfVersion(String versionAlias) {
        String doc = ApiTestContext.name(versionAlias + "#doc");
        return ApiTestContext.id(doc);
    }

    static List<JsonObject> versionsAsc(String documentAlias) {
        List<JsonObject> out = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions")
                .expect(200, "Sürüm listesi").json().getAsJsonArray("items").forEach(e -> out.add(e.getAsJsonObject()));
        out.sort(Comparator.comparingInt((JsonObject v) -> v.get("version_major").getAsInt())
                .thenComparingInt(v -> v.get("version_minor").getAsInt())
                .thenComparingInt(v -> v.get("version_patch").getAsInt()));
        return out;
    }

    private static String label(JsonObject v) {
        return "v" + v.get("version_major").getAsInt() + "." + v.get("version_minor").getAsInt() + "." + v.get("version_patch").getAsInt();
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
