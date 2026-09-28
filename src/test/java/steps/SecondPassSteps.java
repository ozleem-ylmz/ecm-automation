package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/*
 * Phase C (second-pass coverage gaps, 2026-09-28) — functional gaps NEW-06..NEW-26.
 * Each step carries the ECM source rule it asserts; expectations come from the product
 * contract, never from observed behaviour.
 */
public class SecondPassSteps {

    private static final int UI_WAIT_MS = 30000;
    private final Map<String, String> words = new LinkedHashMap<>();
    private final Map<String, JsonObject> snapshots = new LinkedHashMap<>();
    private EcmApiClient.ApiResponse last;

    static EcmApiClient.Session session(String alias) {
        return "yönetici".equals(alias) ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static JsonObject read(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "?include_deleted=true")
                .expect(200, "Belge okuma (yönetici)").json();
    }

    static String folderOf(String documentAlias) {
        JsonObject d = read(documentAlias);
        return d.get("folder").isJsonNull() ? null : d.getAsJsonObject("folder").get("id").getAsString();
    }

    static String classOf(String documentAlias) {
        JsonElement c = read(documentAlias).getAsJsonObject("document").get("class_id");
        return c == null || c.isJsonNull() ? null : c.getAsString();
    }

    static String currentVersionId(String documentAlias) {
        return read(documentAlias).getAsJsonObject("document").get("current_version_id").getAsString();
    }

    private static List<String> titlesInFolder(String folderAlias, String title) {
        List<String> ids = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&folder_id=" + ApiTestContext.id(folderAlias) + "&title=" + enc(title))
                .expect(200, "Klasör belgeleri").json().getAsJsonArray("items").forEach(e -> {
                    JsonObject o = e.getAsJsonObject();
                    // Product identity rule for titles is the trimmed value (save.rs find_versionable_duplicate);
                    // create stores titles untrimmed (RQ-34), so compare trimmed.
                    if (title.trim().equals(o.get("title").getAsString().trim())) {
                        ids.add(o.get("id").getAsString());
                    }
                });
        return ids;
    }

    // =====================================================================
    // NEW-06 — full-text search (README `q`; migration 0029 search_vector C = content_text;
    // content_extraction.rs text/plain; ts_headline StartSel=** StopSel=**)
    // =====================================================================

    @Given("tam metin araması için benzersiz bir {string} kelimesi belirlenmiştir")
    public void benzersizKelime(String wordAlias) {
        Random r = new Random();
        StringBuilder sb = new StringBuilder("zqx");
        for (int i = 0; i < 10; i++) {
            sb.append((char) ('a' + r.nextInt(26)));
        }
        words.put(wordAlias, sb.toString());
    }

    @Given("{string} {string} klasörüne içeriğinde {string} kelimesi geçen ama başlığında geçmeyen {string} belgesini yüklemiştir")
    public void icerikteKelimeGecenBelge(String userAlias, String folderAlias, String wordAlias, String documentAlias) {
        String title = TestDataFactory.unique("QA Tam Metin " + documentAlias);
        String text = "QA otomasyon tam metin sınaması. Bu paragraf " + words.get(wordAlias) + " kelimesini yalnız içerikte taşır.";
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "tam-metin.txt", utf8(text)));
        r.expect(201, "Tam metin belgesi (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
    }

    @Then("{string} tam metin aramasında {string} kelimesiyle yalnız {string} belgelerini vurgulu parçayla bulmalıdır")
    public void tamMetinArama(String userAlias, String wordAlias, String expectedCsv) {
        String word = words.get(wordAlias);
        TreeSet<String> expected = new TreeSet<>();
        for (String a : expectedCsv.split("\\s*,\\s*")) {
            if (!a.isBlank()) {
                expected.add(ApiTestContext.id(a));
            }
        }
        TreeSet<String> got = new TreeSet<>();
        List<String> snippetProblems = new ArrayList<>();
        StringBuilder ev = new StringBuilder("GET /v1/documents?q=" + word + " as " + userAlias + "\n");
        for (JsonElement e : EcmApiClient.get(session(userAlias), "/v1/documents?limit=200&q=" + enc(word))
                .expect(200, "Tam metin araması").json().getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            String id = o.get("id").getAsString();
            got.add(id);
            JsonElement sn = o.get("snippet");
            String snippet = sn == null || sn.isJsonNull() ? null : sn.getAsString();
            ev.append("  ").append(ApiTestContext.aliasOfId(id)).append(" snippet=").append(snippet).append('\n');
            if (snippet == null || !snippet.toLowerCase().contains("**" + word + "**")) {
                snippetProblems.add(ApiTestContext.aliasOfId(id) + ": " + snippet);
            }
        }
        Allure.addAttachment("NEW-06 kanıtı — tam metin araması (" + userAlias + ")", "text/plain", ev.toString());
        if (!got.equals(expected)) {
            throw new AssertionError("q=" + word + " sonucu " + names(got) + " olmalıydı " + names(expected));
        }
        if (!snippetProblems.isEmpty()) {
            throw new AssertionError("Sonuçlar içerikteki kelimeyi **" + word + "** ile vurgulayan snippet taşımalıydı: " + snippetProblems);
        }
    }

    private static List<String> names(TreeSet<String> ids) {
        List<String> out = new ArrayList<>();
        ids.forEach(i -> out.add(ApiTestContext.aliasOfId(i)));
        return out;
    }

    // =====================================================================
    // NEW-08 — duplicate-title fold (README; save.rs find_versionable_duplicate:
    // same folder + exact trimmed title + same class context + caller has Write → new version,
    // default Patch; otherwise a new document)
    // =====================================================================

    @When("{string} {string} klasörüne {string} belgesinin başlığını {string} biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç {string} olarak kaydedilir")
    public void ayniBaslikOlustur(String userAlias, String folderAlias, String sourceAlias, String variant, String resultAlias) {
        String title = ApiTestContext.name(sourceAlias);
        if ("küçük harf".equals(variant)) {
            title = title.toLowerCase(java.util.Locale.ROOT);
        } else if ("boşluklu".equals(variant)) {
            title = "  " + title + "  ";
        } else if (!"birebir".equals(variant)) {
            throw new IllegalArgumentException("Bilinmeyen başlık biçimi: " + variant);
        }
        byte[] content = utf8("QA başlık katlama içeriği " + resultAlias + " " + System.nanoTime());
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "katlama.txt", content));
        r.expect(201, "Aynı başlıkla oluşturma (" + resultAlias + ")");
        JsonObject j = r.json();
        String id = j.get("document_id").getAsString();
        ApiTestContext.putName(resultAlias + "#response", j.toString());
        ApiTestContext.putName(resultAlias + "#bytes", new String(content, StandardCharsets.UTF_8));
        if (ApiTestContext.hasId(sourceAlias) && !id.equals(ApiTestContext.id(sourceAlias))) {
            TestDataFactory.registerDocument(resultAlias, id, title.trim());
        } else {
            ApiTestContext.putId(resultAlias, id);
        }
    }

    @Then("{string} sonucu {string} belgesine {string} sürümü olarak katlanmış olmalı ve klasörde o başlıkta tek belge kalmalıdır")
    public void katlanmisOlmali(String resultAlias, String sourceAlias, String version) {
        JsonObject resp = com.google.gson.JsonParser.parseString(ApiTestContext.name(resultAlias + "#response")).getAsJsonObject();
        String got = resp.get("version_major").getAsInt() + "." + resp.get("version_minor").getAsInt() + "." + resp.get("version_patch").getAsInt();
        String folder = ApiTestContext.aliasOfId(folderOf(sourceAlias));
        List<String> same = titlesInFolder(folder, ApiTestContext.name(sourceAlias));
        byte[] bytes = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(sourceAlias) + "/content");
        String stored = read(sourceAlias).getAsJsonObject("document").get("title").getAsString();
        String ev = "response=" + resp + "\nsame-title docs in folder (trimmed match)=" + same.size() + "\ncurrent content="
                + new String(bytes, StandardCharsets.UTF_8) + "\nstored title after fold (RQ-34 evidence)=[" + stored + "]";
        Allure.addAttachment("NEW-08 kanıtı — " + resultAlias, "text/plain", ev);
        if (!ApiTestContext.id(sourceAlias).equals(resp.get("document_id").getAsString()) || !version.equals(got)) {
            throw new AssertionError("Aynı başlıklı oluşturma '" + sourceAlias + "' belgesine " + version + " olarak katlanmalıydı: " + resp);
        }
        if (same.size() != 1 || !new String(bytes, StandardCharsets.UTF_8).equals(ApiTestContext.name(resultAlias + "#bytes"))) {
            throw new AssertionError("Klasörde tek belge ve yeni içerik güncel sürüm olmalıydı\n" + ev);
        }
    }

    @Then("{string} sonucu {string} belgesinden farklı yeni bir belge olmalıdır")
    public void yeniBelgeOlmali(String resultAlias, String sourceAlias) {
        JsonObject resp = com.google.gson.JsonParser.parseString(ApiTestContext.name(resultAlias + "#response")).getAsJsonObject();
        String id = resp.get("document_id").getAsString();
        String v = resp.get("version_major").getAsInt() + "." + resp.get("version_minor").getAsInt() + "." + resp.get("version_patch").getAsInt();
        int sourceVersions = VersionSteps.versionsAsc(sourceAlias).size();
        Allure.addAttachment("NEW-08 kanıtı — " + resultAlias, "text/plain", "response=" + resp + "\nsource versions=" + sourceVersions);
        if (id.equals(ApiTestContext.id(sourceAlias)) || !"1.0.0".equals(v)) {
            throw new AssertionError("'" + resultAlias + "' yeni bir belge (1.0.0) olmalıydı, '" + sourceAlias + "' belgesine katlandı: " + resp);
        }
    }

    @Then("{string} belgesinin sürüm sayısı {int} olmalıdır")
    public void surumSayisi(String documentAlias, int n) {
        int got = VersionSteps.versionsAsc(documentAlias).size();
        if (got != n) {
            throw new AssertionError("'" + documentAlias + "' " + n + " sürüm olmalıydı, " + got + " var");
        }
    }

    // =====================================================================
    // NEW-09 — allowed-class / inactive-class enforcement on move, copy, reclassify, rollback
    // (folders/write.rs:161, copy.rs:84, reclassify.rs:56/74, rollback.rs:78/86)
    // =====================================================================

    @Given("yönetici {string} sınıfıyla {string} klasörüne sınıflı {string} belgesini oluşturmuştur")
    public void sinifliBelge(String classAlias, String folderAlias, String documentAlias) {
        String title = TestDataFactory.unique("QA Sınıf Politikası " + documentAlias);
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(title, ApiTestContext.id(classAlias),
                ClassAdminSteps.classVersion(classAlias), Map.of(), folderAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents", body);
        r.expect(201, "Sınıflı belge (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
    }

    @Given("yönetici {string} belgesine {string} sınıfıyla yeni bir sürüm kaydetmiştir")
    public void sinifliYeniSurum(String documentAlias, String classAlias) {
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(ApiTestContext.name(documentAlias), ApiTestContext.id(classAlias),
                ClassAdminSteps.classVersion(classAlias), Map.of(), null);
        body.put("document_id", ApiTestContext.id(documentAlias));
        body.put("version_type", "Minor");
        String folder = folderOf(documentAlias);
        body.put("folder_id", folder);
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents", body).expect(201, "Sınıflı yeni sürüm (" + documentAlias + ")");
    }

    @Given("{string} belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm")
    public void durumKaydet(String documentAlias) {
        JsonObject s = new JsonObject();
        s.addProperty("folder", folderOf(documentAlias));
        s.addProperty("class", classOf(documentAlias));
        s.addProperty("version", currentVersionId(documentAlias));
        s.addProperty("versions", VersionSteps.versionsAsc(documentAlias).size());
        snapshots.put(documentAlias, s);
    }

    @When("yönetici {string} belgesini {string} klasörüne taşımayı dener")
    public void tasimayiDener(String documentAlias, String folderAlias) {
        last = EcmApiClient.put(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/folder",
                Map.of("folder_id", ApiTestContext.id(folderAlias)));
        ApiTestContext.lastResponse(last);
    }

    @When("yönetici {string} belgesini {string} klasörüne kopyalamayı dener")
    public void kopyalamayiDener(String documentAlias, String folderAlias) {
        ApiTestContext.putName(folderAlias + "#countBefore", String.valueOf(countInFolder(folderAlias)));
        last = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/copy",
                Map.of("target_folder_id", ApiTestContext.id(folderAlias)));
        ApiTestContext.lastResponse(last);
        if (last.status() >= 200 && last.status() < 300) {
            String id = last.json().get("document_id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + id));
        }
    }

    @When("yönetici {string} belgesini {string} sınıfına yeniden sınıflandırmayı dener")
    public void yenidenSiniflandir(String documentAlias, String classAlias) {
        last = EcmApiClient.patch(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/reclassify",
                Map.of("class_id", ApiTestContext.id(classAlias), "class_version", ClassAdminSteps.classVersion(classAlias), "metadata", Map.of()));
        ApiTestContext.lastResponse(last);
    }

    @When("yönetici {string} belgesini ilk sürümüne geri almayı dener")
    public void ilkSurumeGeriAl(String documentAlias) {
        String v1 = VersionSteps.versionsAsc(documentAlias).get(0).get("id").getAsString();
        last = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions/" + v1 + "/rollback", Map.of());
        ApiTestContext.lastResponse(last);
    }

    @Then("{string} belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır")
    public void durumAyni(String documentAlias) {
        JsonObject before = snapshots.get(documentAlias);
        JsonObject now = new JsonObject();
        now.addProperty("folder", folderOf(documentAlias));
        now.addProperty("class", classOf(documentAlias));
        now.addProperty("version", currentVersionId(documentAlias));
        now.addProperty("versions", VersionSteps.versionsAsc(documentAlias).size());
        Allure.addAttachment("Değişmezlik kanıtı — " + documentAlias, "text/plain", "önce=" + before + "\nsonra=" + now);
        if (!before.equals(now)) {
            throw new AssertionError("Reddedilen işlem '" + documentAlias + "' belgesini değiştirmemeliydi.\nÖnce: " + before + "\nSonra: " + now);
        }
    }

    @Then("{string} klasöründeki belge sayısı değişmemiş olmalıdır")
    public void klasorSayisiAyni(String folderAlias) {
        int before = Integer.parseInt(ApiTestContext.name(folderAlias + "#countBefore"));
        int now = countInFolder(folderAlias);
        if (before != now) {
            throw new AssertionError("Reddedilen kopya '" + folderAlias + "' klasörüne belge eklememeliydi: " + before + " → " + now);
        }
    }

    private static int countInFolder(String folderAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&folder_id=" + ApiTestContext.id(folderAlias))
                .expect(200, "Klasör belgeleri").json().getAsJsonArray("items").size();
    }

    @Given("yönetici {string} sınıfını pasifleştirmiştir")
    public void sinifPasif(String classAlias) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/active",
                Map.of("expected_version", ClassAdminSteps.classVersion(classAlias), "is_active", false)).expect(200, "Sınıf pasifleştirme");
    }

    // =====================================================================
    // NEW-10 — inactive group grants nothing (user guide §6; postgres_repository/mod.rs:1147-1156
    // list_user_groups: "Only active memberships count", AND g.is_active = TRUE)
    // =====================================================================

    @When("yönetici {string} grubunu {string} yapar")
    public void grupAktiflik(String groupAlias, String state) {
        boolean active = "aktif".equals(state);
        EcmApiClient.put(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias), Map.of("is_active", active))
                .expect(200, "Grup aktiflik (" + state + ")");
        JsonObject g = EcmApiClient.get(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias)).expect(200, "Grup").json();
        JsonObject grp = g.has("group") ? g.getAsJsonObject("group") : g;
        if (grp.get("is_active").getAsBoolean() != active) {
            throw new AssertionError("Grup durumu kalıcı değil: " + g);
        }
    }

    @Then("{string} {string} belgesini okuyabilmeli ve belge listesinde görmelidir")
    public void okuyabilmeli(String userAlias, String documentAlias) {
        int s = EcmApiClient.get(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias)).status();
        boolean listed = listedFor(userAlias, documentAlias);
        if (s != 200 || !listed) {
            throw new AssertionError("'" + userAlias + "' '" + documentAlias + "' belgesini okuyabilmeliydi (HTTP " + s + ", listede " + listed + ")");
        }
    }

    @Then("{string} {string} belgesini okuyamamalı ve belge listesinde görmemelidir")
    public void okuyamamali(String userAlias, String documentAlias) {
        int s = EcmApiClient.get(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias)).status();
        boolean listed = listedFor(userAlias, documentAlias);
        if ((s != 403 && s != 404) || listed) {
            throw new AssertionError("'" + userAlias + "' '" + documentAlias + "' belgesine erişememeliydi (HTTP " + s + ", listede " + listed + ")");
        }
    }

    private static boolean listedFor(String userAlias, String documentAlias) {
        String title = ApiTestContext.name(documentAlias);
        for (JsonElement e : EcmApiClient.get(session(userAlias), "/v1/documents?limit=200&title=" + enc(title)).expect(200, "Liste").json()
                .getAsJsonArray("items")) {
            if (ApiTestContext.id(documentAlias).equals(e.getAsJsonObject().get("id").getAsString())) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // NEW-20 — classified upload through the UI (user guide 2.6; DocumentNew.tsx: class select,
    // required property inputs carry the HTML `required` attribute, success navigates to /documents/{id})
    // =====================================================================

    private Page adminPage() {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
        }
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        return BrowserManager.page;
    }

    @When("yönetici arayüzde {string} klasörüne {string} sınıfını seçip zorunlu {string} özelliğini boş bırakarak {string} belgesini yüklemeyi dener")
    public void zorunluBosYukleme(String folderAlias, String classAlias, String propertyKey, String documentAlias) throws Exception {
        Page p = adminPage();
        String title = TestDataFactory.unique("QA Arayüz Sınıflı " + documentAlias);
        ApiTestContext.putName(documentAlias, title);
        fillClassifiedForm(p, folderAlias, classAlias, title, documentAlias);
        List<String> posts = new ArrayList<>();
        java.util.function.Consumer<Request> listener = rq -> {
            if ("POST".equals(rq.method()) && rq.url().matches(".*/v1/documents(/stream)?$")) {
                posts.add(rq.url());
            }
        };
        p.onRequest(listener);
        p.locator("form button[type='submit']").first().click();
        // HTML5 constraint validation (required attribute) blocks the submit: the input is invalid
        // and no upload request leaves the page. Waiting on the form's own validity state, not a timer.
        Locator input = propertyInput(p, propertyKey);
        p.waitForFunction("el => el.matches(':invalid')", input.elementHandle());
        boolean valid = (Boolean) input.evaluate("el => el.checkValidity()");
        p.offRequest(listener);
        Allure.addAttachment("NEW-20 kanıtı — zorunlu alan boş", "text/plain",
                "input.checkValidity()=" + valid + "\nPOST istekleri=" + posts + "\nURL=" + p.url());
        if (valid || !posts.isEmpty() || !p.url().contains("/documents/new")) {
            throw new AssertionError("Zorunlu özellik boşken form gönderilmemeliydi (geçerli=" + valid + ", istekler=" + posts + ", URL=" + p.url() + ")");
        }
    }

    @When("yönetici arayüzde {string} özelliğine {string} yazıp yüklemeyi tamamlar")
    public void zorunluDoldurYukle(String propertyKey, String value) {
        Page p = BrowserManager.page;
        propertyInput(p, propertyKey).fill(value);
        Response r = p.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().matches(".*/v1/documents(/stream)?$"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> p.locator("form button[type='submit']").first().click());
        if (r.status() != 201) {
            throw new AssertionError("Sınıflı yükleme HTTP " + r.status() + ": " + r.text());
        }
        String id = com.google.gson.JsonParser.parseString(r.text()).getAsJsonObject().get("document_id").getAsString();
        String alias = ApiTestContext.name("NEW-20#alias");
        TestDataFactory.registerDocument(alias, id, ApiTestContext.name(alias));
        p.waitForURL(url -> url.endsWith("/documents/" + id), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("arayüzde yüklenen {string} belgesi API'de {string} klasöründe, {string} sınıfında, {string}={string} özelliğiyle ve seçilen dosyanın baytlarıyla bulunmalıdır")
    public void arayuzYuklemesiDogrula(String documentAlias, String folderAlias, String classAlias, String key, String value) throws Exception {
        JsonObject d = read(documentAlias);
        List<String> failures = new ArrayList<>();
        if (!ApiTestContext.id(folderAlias).equals(d.getAsJsonObject("folder").get("id").getAsString())) {
            failures.add("klasör");
        }
        if (!ApiTestContext.id(classAlias).equals(d.getAsJsonObject("document").get("class_id").getAsString())) {
            failures.add("sınıf");
        }
        boolean prop = false;
        for (JsonElement e : d.getAsJsonArray("property_values")) {
            JsonElement v = e.getAsJsonObject().get("value");
            prop |= v != null && !v.isJsonNull() && value.equals(v.getAsString());
        }
        if (!prop) {
            failures.add("özellik değeri " + d.getAsJsonArray("property_values"));
        }
        byte[] file = Files.readAllBytes(Path.of("src/test/resources/test-files/test-document.txt"));
        byte[] api = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content");
        if (!java.util.Arrays.equals(file, api)) {
            failures.add("içerik baytları");
        }
        if (titlesInFolder(folderAlias, ApiTestContext.name(documentAlias)).size() != 1) {
            failures.add("klasörde aynı başlıkta tek belge olmalı (ilk reddedilen deneme belge oluşturmamalı)");
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Arayüzden sınıflı yükleme kalıcı sonucu hatalı: " + failures);
        }
    }

    private void fillClassifiedForm(Page p, String folderAlias, String classAlias, String title, String documentAlias) {
        ApiTestContext.putName("NEW-20#alias", documentAlias);
        p.navigate(BrowserManager.getBaseUrl() + "/documents/new?folder_id=" + ApiTestContext.id(folderAlias));
        Locator classSelect = p.locator("form select").first();
        classSelect.locator("option[value='" + ApiTestContext.id(classAlias) + "']")
                .waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED).setTimeout(UI_WAIT_MS));
        classSelect.selectOption(new SelectOption().setValue(ApiTestContext.id(classAlias)));
        p.getByPlaceholder("e.g. Q3 Invoice — Acme Corp").fill(title);
        p.locator("input[type='file']").setInputFiles(Path.of("src/test/resources/test-files/test-document.txt"));
    }

    private static Locator propertyInput(Page p, String propertyKey) {
        Locator label = p.locator("span", new Page.LocatorOptions().setHasText(java.util.regex.Pattern.compile("^" + propertyKey.toUpperCase() + "$")));
        label.first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return label.first().locator("xpath=ancestor::div[1]/parent::*").locator("input, textarea").first();
    }
}
