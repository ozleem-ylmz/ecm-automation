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
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Batch 28 — ilişkiler ve bütünlük: hard delete temizliği, ilişki görünürlüğü,
 * yönsüz tür yazma kuralı, sürüme bağlı ilişki, silme yetkisi, ilişki türleri,
 * Link document arayüzü ve silinmiş belgenin özel alanları.
 * ECM kaynakları: engine/relationships/mod.rs, api/rest/relationships.rs, LinkDocumentModal.tsx.
 */
public class RelationshipSteps {

    private static final int UI_WAIT_MS = 20000;

    // ---------------------------------------------------------------------
    // Türler
    // ---------------------------------------------------------------------

    @Given("yönsüz {string} ilişki türü oluşturulmuştur")
    public void yonsuzTurOlusturulmustur(String typeAlias) {
        createType(typeAlias, false);
    }

    @Given("yönlü {string} ilişki türü yönetici tarafından oluşturulmuştur")
    public void yonluTurOlusturulmustur(String typeAlias) {
        createType(typeAlias, true);
    }

    @When("yönetici {string} ilişki türüyle aynı adda tür oluşturmayı dener")
    public void ayniAddaTurOlusturmayiDener(String typeAlias) {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/relationship-types",
                Map.of("name", ApiTestContext.name("reltype:" + typeAlias), "is_directed", true)));
    }

    @When("yönetici boş adla ilişki türü oluşturmayı dener")
    public void bosAdlaTurOlusturmayiDener() {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/relationship-types",
                Map.of("name", "", "is_directed", true)));
    }

    @When("yönetici bir sistem ilişki türünü düzenlemeyi dener")
    public void sistemTurunuDuzenlemeyiDener() {
        JsonObject sys = null;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/relationship-types").expect(200, "Türler").json().getAsJsonArray("items")) {
            if (e.getAsJsonObject().get("is_system").getAsBoolean()) {
                sys = e.getAsJsonObject();
                break;
            }
        }
        if (sys == null) {
            // Sistem türü yoksa kuralın koşulu kurulamaz; sonuç açıkça bildirilir.
            throw new AssertionError("Ortamda sistem ilişki türü (is_system=true) yok; kural doğrulanamaz.");
        }
        ApiTestContext.putName("systemtype:id", sys.get("id").getAsString());
        ApiTestContext.putName("systemtype:name", sys.get("name").getAsString());
        ApiTestContext.lastResponse(EcmApiClient.put(TestDataFactory.admin(), "/v1/relationship-types/" + sys.get("id").getAsString(),
                Map.of("name", "QA hack " + System.nanoTime(), "is_directed", sys.get("is_directed").getAsBoolean())));
    }

    @Then("sistem ilişki türünün adı değişmemiş olmalıdır")
    public void sistemTurAdiDegismemis() {
        String id = ApiTestContext.name("systemtype:id");
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/relationship-types").json().getAsJsonArray("items")) {
            JsonObject t = e.getAsJsonObject();
            if (id.equals(t.get("id").getAsString()) && !ApiTestContext.name("systemtype:name").equals(t.get("name").getAsString())) {
                throw new AssertionError("Sistem ilişki türünün adı değişmemeliydi: " + t);
            }
        }
    }

    @When("yönetici {string} ilişki türünü yeniden adlandırır")
    public void turuYenidenAdlandirir(String typeAlias) {
        String id = typeId(typeAlias);
        String newName = ApiTestContext.name("reltype:" + typeAlias) + " yeni";
        EcmApiClient.put(TestDataFactory.admin(), "/v1/relationship-types/" + id, Map.of("name", newName, "is_directed", true))
                .expect(200, "İlişki türü yeniden adlandırma");
        ApiTestContext.putName("reltype:" + typeAlias, newName);
    }

    @Then("{string} belgesinin giden ilişkisi {string} türünün güncel adıyla görünmelidir")
    public void gidenIliskiGuncelAdla(String documentAlias, String typeAlias) {
        String name = ApiTestContext.name("reltype:" + typeAlias);
        boolean found = false;
        for (JsonElement e : relationships(TestDataFactory.admin(), documentAlias).getAsJsonArray("outbound")) {
            found |= name.equals(e.getAsJsonObject().get("relationship_type").getAsString());
        }
        if (!found) {
            throw new AssertionError("Mevcut ilişki yeniden adlandırılan türün güncel adıyla (" + name + ") görünmeli: "
                    + relationships(TestDataFactory.admin(), documentAlias));
        }
    }

    // ---------------------------------------------------------------------
    // İlişki oluşturma / görünürlük / silme
    // ---------------------------------------------------------------------

    @When("{string} {string} belgesinden {string} belgesine {string} türünde {string} bağlamalı ilişki oluşturur")
    public void baglamaliIliskiOlusturur(String userAlias, String src, String dst, String typeAlias, String binding) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("target_doc_id", ApiTestContext.id(dst));
        body.put("relationship_type", ApiTestContext.name("reltype:" + typeAlias));
        body.put("binding_policy", binding);
        if ("SPECIFIC".equals(binding)) {
            body.put("target_version_id", VersionSteps.versionsAsc(dst).get(0).get("id").getAsString());
        }
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/" + ApiTestContext.id(src) + "/relationships", body);
        r.expect(201, "İlişki oluşturma (" + binding + ")");
        ApiTestContext.putId("rel:" + src + ":" + binding, r.json().get("id").getAsString());
        ApiTestContext.putName("rel:" + src + ":" + binding + ":pinned",
                "SPECIFIC".equals(binding) ? VersionSteps.versionsAsc(dst).get(0).get("id").getAsString() : "");
    }

    @When("{string} {string} belgesinden {string} belgesine {string} türünde ilişki oluşturmayı dener")
    public void turundeIliskiOlusturmayiDener(String userAlias, String src, String dst, String typeAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("target_doc_id", ApiTestContext.id(dst));
        body.put("relationship_type", ApiTestContext.name("reltype:" + typeAlias));
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/" + ApiTestContext.id(src) + "/relationships", body);
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            ApiTestContext.putId("rel:" + src + ":" + typeAlias, r.json().get("id").getAsString());
        }
    }

    @Then("{string} belgesinin {string} bağlamalı ilişkisi hedefin ilk sürümüne bağlı kalmalıdır")
    public void specificIliskiIlkSurumdeKalmali(String src, String binding) {
        JsonObject rel = relationship(src, ApiTestContext.id("rel:" + src + ":" + binding));
        String pinned = ApiTestContext.name("rel:" + src + ":" + binding + ":pinned");
        if (!pinned.equals(rel.get("target_version_id").getAsString()) || !"v1.0.0".equals(rel.get("target_version_label").getAsString())) {
            throw new AssertionError("SPECIFIC ilişki hedefin v1.0.0 sürümüne bağlı kalmalıydı: " + rel);
        }
    }

    @Then("{string} belgesinin {string} bağlamalı ilişkisi hedefin güncel sürümünü göstermelidir")
    public void latestIliskiGuncelSurum(String src, String binding) {
        JsonObject rel = relationship(src, ApiTestContext.id("rel:" + src + ":" + binding));
        JsonElement tv = rel.get("target_version_id");
        if (tv != null && !tv.isJsonNull() && !tv.getAsString().isBlank()) {
            String current = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + rel.get("target_doc_id").getAsString())
                    .expect(200, "Hedef").json().getAsJsonObject("document").get("current_version_id").getAsString();
            if (!current.equals(tv.getAsString())) {
                throw new AssertionError("LATEST ilişki hedefin güncel sürümünü göstermeliydi: " + rel);
            }
        }
        if (!"LATEST".equals(rel.get("binding_policy").getAsString())) {
            throw new AssertionError("İlişki LATEST bağlamalı olmalıydı: " + rel);
        }
    }

    @Then("{string} {string} belgesinin ilişkilerinde {string} belgesini ve başlığını görmemelidir")
    public void iliskilerdeBelgeVeBasligiGormemeli(String userAlias, String documentAlias, String hiddenAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/relationships")
                .expect(200, "İlişkiler");
        if (r.body().contains(ApiTestContext.id(hiddenAlias)) || r.body().contains(ApiTestContext.name(hiddenAlias))) {
            throw new AssertionError("Okunamayan belgenin kimliği/başlığı ilişki listesinde sızdı: " + r.body());
        }
    }

    @Then("{string} {string} belgesinin ilişkilerinde {string} belgesini görmelidir")
    public void iliskilerdeBelgeyiGormeli(String userAlias, String documentAlias, String visibleAlias) {
        String body = EcmApiClient.get(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/relationships")
                .expect(200, "İlişkiler").body();
        if (!body.contains(ApiTestContext.id(visibleAlias))) {
            throw new AssertionError("Okunabilen hedef ilişki listesinde görünmeliydi: " + body);
        }
    }

    @When("{string} {string} belgesinin {string} ilişkisini silmeyi dener")
    public void kullaniciIliskiyiSilmeyiDener(String userAlias, String src, String typeAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(session(userAlias), "/v1/documents/" + ApiTestContext.id(src)
                + "/relationships/" + ApiTestContext.id("rel:" + src + ":" + typeAlias)));
    }

    @Then("{string} belgesinin {string} ilişkisi hâlâ bulunmalıdır")
    public void iliskiHalaBulunmali(String src, String typeAlias) {
        relationship(src, ApiTestContext.id("rel:" + src + ":" + typeAlias));
    }

    /* RQ-21: DELETE yolundaki belge kimliği handler'da kullanılmıyor (_document_id). */
    @Then("{string} belgesinin {string} ilişkisini ilgisiz {string} belgesinin yolundan silme sonucu RQ-21 kanıtı olarak kaydedilmelidir")
    public void ilgisizYoldanSilmeKanit(String src, String typeAlias, String otherAlias) {
        String relId = ApiTestContext.id("rel:" + src + ":" + typeAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(otherAlias) + "/relationships/" + relId);
        boolean stillThere = relationships(TestDataFactory.admin(), src).toString().contains(relId);
        String evidence = "RQ-21 (ilişki silme yolu belge kapsamı):\n"
                + "  DELETE /v1/documents/{ilgisiz belge}/relationships/{rel}: HTTP " + r.status() + "\n"
                + "  ilişki hâlâ var mı: " + stillThere + "\n"
                + "  api/rest/relationships.rs delete_relationship yol parametresini (_document_id) yok sayar; yetki ilişkinin\n"
                + "  gerçek kaynağına göre kontrol edilir (normal kullanıcı başka belgenin ilişkisini silemez — doğrulandı).\n";
        System.out.println("[RQ-21] " + evidence.replace("\n", " | "));
        Allure.addAttachment("RQ-21 evidence — relationship delete path scope", "text/plain", evidence);
    }

    // ---------------------------------------------------------------------
    // Hard delete temizliği (DOC-02)
    // ---------------------------------------------------------------------

    @When("yönetici {string} belgesini kalıcı olarak siler")
    public void yoneticiBelgeyiKaliciSiler(String documentAlias) {
        ApiTestContext.putName(documentAlias + "#oldVersion", VersionSteps.versionsAsc(documentAlias).get(0).get("id").getAsString());
        EcmApiClient.ApiResponse r = EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/hard");
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("Hard delete başarılı olmalıydı: HTTP " + r.status() + " " + r.body());
        }
    }

    @Then("{string} belgesinin kaydı, içeriği, sürümleri ve eski sürüm içeriği 404 dönmelidir")
    public void kayitIcerikSurumler404(String documentAlias) {
        String id = ApiTestContext.id(documentAlias);
        for (String path : List.of("/v1/documents/" + id, "/v1/documents/" + id + "/content", "/v1/documents/" + id + "/versions",
                "/v1/documents/versions/" + ApiTestContext.name(documentAlias + "#oldVersion") + "/content")) {
            int s = EcmApiClient.get(TestDataFactory.admin(), path).status();
            if (s != 404) {
                throw new AssertionError("Kalıcı silinen belge için " + path + " 404 olmalıydı, HTTP " + s);
            }
        }
    }

    @Then("{string} belgesinin ilişkilerinde {string} belgesine ait kayıt kalmamalıdır")
    public void iliskilerdeKayitKalmamali(String documentAlias, String deletedAlias) {
        String body = relationships(TestDataFactory.admin(), documentAlias).toString();
        if (body.contains(ApiTestContext.id(deletedAlias))) {
            throw new AssertionError("Kalıcı silinen belgeye ait yetim ilişki kaldı: " + body);
        }
    }

    @Then("{string} etiketiyle yapılan aramada {string} belgesi bulunmamalıdır")
    public void etiketAramasindaBulunmamali(String tagPrefix, String documentAlias) {
        String tag = ApiTestContext.name(documentAlias + "#tag");
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?tag_names=" + URLEncoder.encode(tag, StandardCharsets.UTF_8));
        if (r.status() == 200 && r.body().contains(ApiTestContext.id(documentAlias))) {
            throw new AssertionError("Kalıcı silinen belge etiket aramasında hâlâ bulunuyor.");
        }
    }

    // ---------------------------------------------------------------------
    // Link document arayüzü (REL-06)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} belgesinden {string} belgesine {string} türünde Link document ile ilişki kurar")
    public void arayuzdeLinkDocument(String src, String dst, String typeAlias) {
        Page page = openDocument(src);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Link document").setExact(true)).first().click();
        Locator form = page.locator("form").filter(new Locator.FilterOptions().setHas(page.getByPlaceholder("Paste the target document UUID")));
        form.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        form.locator("select").first().selectOption(ApiTestContext.name("reltype:" + typeAlias));
        form.getByPlaceholder("Paste the target document UUID").fill(ApiTestContext.id(dst));
        Locator submit = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Link document").setExact(true));
        page.waitForCondition(submit::isEnabled, new Page.WaitForConditionOptions().setTimeout(UI_WAIT_MS));
        Response r = page.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/relationships"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS), submit::click);
        if (r.status() != 201) {
            throw new AssertionError("Link document HTTP " + r.status() + ": " + r.text());
        }
        ApiTestContext.putId("rel:" + src + ":" + typeAlias,
                com.google.gson.JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString());
    }

    @Then("yönetici arayüzde {string} belgesinin ilişkiler kartında {string} başlığını görmelidir")
    public void iliskilerKartindaBasligiGormeli(String documentAlias, String otherAlias) {
        Page page = openDocument(documentAlias);
        try {
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(ApiTestContext.name(otherAlias))).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("İlişkiler kartında '" + otherAlias + "' bağlantısı görünmedi.", e);
        }
    }

    @When("yönetici arayüzde {string} belgesinin ilişkisini Unlink ile kaldırır")
    public void arayuzdeUnlink(String documentAlias) {
        Page page = openDocument(documentAlias);
        page.getByTitle("Remove relationship").first().click();
        Locator dialog = page.locator("[aria-modal='true']");
        dialog.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        Response r = page.waitForResponse(resp -> "DELETE".equals(resp.request().method()) && resp.url().contains("/relationships/"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove").setExact(true)).click());
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("Unlink HTTP " + r.status());
        }
    }

    // ---------------------------------------------------------------------

    private static Page openDocument(String documentAlias) {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        }
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Link document").setExact(true)).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return page;
    }

    private static void createType(String typeAlias, boolean directed) {
        String name = TestDataFactory.unique("QA B28 " + typeAlias);
        EcmApiClient.post(TestDataFactory.admin(), "/v1/relationship-types", Map.of("name", name, "is_directed", directed, "description", "QA B28"))
                .expect(201, "İlişki türü oluşturma");
        ApiTestContext.putName("reltype:" + typeAlias, name);
    }

    private static String typeId(String typeAlias) {
        String name = ApiTestContext.name("reltype:" + typeAlias);
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/relationship-types").json().getAsJsonArray("items")) {
            if (name.equals(e.getAsJsonObject().get("name").getAsString())) {
                return e.getAsJsonObject().get("id").getAsString();
            }
        }
        throw new AssertionError("İlişki türü bulunamadı: " + name);
    }

    private static JsonObject relationship(String src, String relId) {
        for (JsonElement e : relationships(TestDataFactory.admin(), src).getAsJsonArray("outbound")) {
            if (relId.equals(e.getAsJsonObject().get("id").getAsString())) {
                return e.getAsJsonObject();
            }
        }
        throw new AssertionError("İlişki bulunamadı: " + relId);
    }

    private static JsonObject relationships(EcmApiClient.Session s, String documentAlias) {
        return EcmApiClient.get(s, "/v1/documents/" + ApiTestContext.id(documentAlias) + "/relationships").expect(200, "İlişkiler").json();
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
