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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/*
 * Batch 29 — belge listesi filtreleri, sayfalama, facet'ler, etiket araması,
 * kayıtlı aramalar ve etiket çipleri.
 * ECM kaynakları: api/rest/documents_query.rs, postgres_repository/documents.rs
 * (ILIKE), SavedSearches.tsx, TagManager.tsx.
 *
 * Tüm liste sorguları senaryonun benzersiz başlık önekiyle sınırlanır
 * (paylaşılan ortamdaki diğer belgeler sonuçları etkilemez).
 */
public class SearchSteps {

    private static final int UI_WAIT_MS = 20000;

    private String prefix;
    private final Map<String, String> tagNames = new HashMap<>();

    // ---------------------------------------------------------------------
    // Kurulum
    // ---------------------------------------------------------------------

    @Given("benzersiz bir arama öneki belirlenmiştir")
    public void benzersizAramaOneki() {
        prefix = "QAARA" + System.nanoTime();
    }

    @Given("{string} {string} klasörüne önekli {string} başlıklı {string} belgesini yüklemiştir")
    public void onekliBelgeYuklemistir(String userAlias, String folderAlias, String suffix, String documentAlias) {
        String title = prefix + " " + suffix.replace("{BS}", "\\");
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "qa-search.txt",
                        ("QA arama " + title).getBytes(StandardCharsets.UTF_8)));
        r.expect(201, "Önekli belge (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
    }

    @Given("{string} belgesine {string} arama etiketi eklenmiştir")
    public void aramaEtiketiEklenmistir(String documentAlias, String tagAlias) {
        String tag = tagNames.computeIfAbsent(tagAlias, a -> asciiTag(a));
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags",
                Map.of("tag_ids", List.of(), "tag_name", tag)).expect(201, "Etiket");
    }

    // ---------------------------------------------------------------------
    // Liste filtreleri ve sıralama (SRCH-01)
    // ---------------------------------------------------------------------

    @Then("önekli listede {string} filtresi yalnız {string} belgelerini döndürmelidir")
    public void onekliListeFiltresi(String filter, String expectedCsv) {
        List<String> actual = aliasesOf(list(TestDataFactory.admin(), resolveFilter(filter)));
        TreeSet<String> exp = new TreeSet<>(expectedCsv.isBlank() ? List.of() : List.of(expectedCsv.split(",\\s*")));
        if (!exp.equals(new TreeSet<>(actual))) {
            throw new AssertionError("'" + filter + "' filtresi " + exp + " döndürmeliydi, " + actual + " döndü.");
        }
    }

    @Then("önekli listede {string} sıralaması {string} sırasını vermelidir")
    public void onekliListeSiralamasi(String sort, String expectedCsv) {
        List<String> actual = aliasesOf(list(TestDataFactory.admin(), sort));
        List<String> exp = List.of(expectedCsv.split(",\\s*"));
        if (!exp.equals(actual)) {
            throw new AssertionError("'" + sort + "' sıralaması " + exp + " olmalıydı, " + actual + " geldi.");
        }
    }

    /*
     * SRT-01 (Batch 35): başlık sıralaması veritabanı ikili sıralamasıyla yapılıyor (büyük harf önce,
     * Türkçe harfler sonda). Beklenen davranış bir gereksinim kararı (RQ-26); burada iddia edilmez,
     * gerçek sıra kanıt olarak eklenir.
     */
    @Then("önekli listede {string} başlık sıralaması RQ-26 kanıtı olarak kaydedilmelidir; Türkçe kullanıcı beklentisi {string}")
    public void baslikSiralamasiKanit(String sort, String userExpectation) {
        List<String> actual = aliasesOf(list(TestDataFactory.admin(), sort));
        io.qameta.allure.Allure.addAttachment("RQ-26 kanıtı — başlık sıralaması", "text/plain",
                "Sorgu: " + sort + "\nGerçek sıra: " + actual + "\nTürkçe, harf büyüklüğüne duyarsız beklenti: " + userExpectation
                        + "\nAynı mı: " + actual.equals(List.of(userExpectation.split(",\\s*"))));
    }

    // ---------------------------------------------------------------------
    // Sayfalama (SRCH-02)
    // ---------------------------------------------------------------------

    @Then("önekli liste {int}'li sayfalarla örtüşmeden toplam {int} belge vermelidir")
    public void sayfalarOrtusmemeli(int pageSize, int total) {
        List<String> seen = new ArrayList<>();
        for (int offset = 0; offset < total + pageSize; offset += pageSize) {
            List<JsonObject> page = list(TestDataFactory.admin(), "sort_by=title&order=asc&limit=" + pageSize + "&offset=" + offset);
            if (page.size() > pageSize) {
                throw new AssertionError("Sayfa boyutu " + pageSize + " aşıldı: " + page.size());
            }
            for (JsonObject d : page) {
                String id = d.get("id").getAsString();
                if (seen.contains(id)) {
                    throw new AssertionError("Sayfalar örtüşüyor: " + id + " (offset " + offset + ")");
                }
                seen.add(id);
            }
        }
        if (seen.size() != total) {
            throw new AssertionError("Sayfalar toplam " + total + " belge vermeliydi, " + seen.size() + " verdi.");
        }
    }

    @When("yönetici belge listesini {string} parametresiyle istemeyi dener")
    public void listeParametresiyleIstemeyiDener(String qs) {
        ApiTestContext.lastResponse(EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?" + qs));
    }

    // ---------------------------------------------------------------------
    // Facet'ler (SRCH-03, SRCH-08)
    // ---------------------------------------------------------------------

    @Then("önekli facet'lerde durum sayıları {string} olmalı ve listeyle tutarlı olmalıdır")
    public void facetDurumSayilari(String expected) {
        Map<String, Integer> facets = statusFacets(TestDataFactory.admin());
        Map<String, Integer> exp = new HashMap<>();
        for (String kv : expected.split(",\\s*")) {
            String[] p = kv.split("=");
            exp.put(p[0], Integer.parseInt(p[1]));
        }
        if (!exp.equals(facets)) {
            throw new AssertionError("Durum facet'leri " + exp + " olmalıydı, " + facets + " geldi.");
        }
        for (Map.Entry<String, Integer> e : exp.entrySet()) {
            int listed = list(TestDataFactory.admin(), "status=" + e.getKey()).size();
            if (listed != e.getValue()) {
                throw new AssertionError("'" + e.getKey() + "' facet sayısı " + e.getValue() + " ama liste " + listed + " belge döndürdü.");
            }
        }
    }

    @Then("{string} önekli listede ve facet'lerde {int} belge görmelidir")
    public void kullaniciOnekliListeVeFacet(String userAlias, int expected) {
        int listed = list(ApiTestContext.session(userAlias), "").size();
        int faceted = statusFacets(ApiTestContext.session(userAlias)).values().stream().mapToInt(Integer::intValue).sum();
        if (listed != expected || faceted != expected) {
            throw new AssertionError("'" + userAlias + "' listede ve facet'lerde " + expected + " belge görmeliydi: liste=" + listed + " facet=" + faceted);
        }
    }

    // ---------------------------------------------------------------------
    // Başlık / etiket araması (SRCH-04, SRCH-05, SRCH-09)
    // ---------------------------------------------------------------------

    @Then("{string} başlık aramasında {string} belgesi {string}")
    public void baslikAramasinda(String term, String documentAlias, String expectation) {
        String q = term.equals("<güncel başlık>") ? currentTitle(documentAlias) : term.equals("<eski başlık>") ? ApiTestContext.name(documentAlias) : term;
        boolean found = listRaw(TestDataFactory.admin(), "title=" + enc(q)).stream()
                .anyMatch(d -> ApiTestContext.id(documentAlias).equals(d.get("id").getAsString()));
        assertExpectation(found, expectation, "'" + q + "' başlık araması / " + documentAlias);
    }

    @Then("{string} etiket aramasında {string} belgesi {string}")
    public void etiketAramasinda(String tagAlias, String documentAlias, String expectation) {
        boolean found = listRaw(TestDataFactory.admin(), "tag_names=" + enc(tagNames.get(tagAlias))).stream()
                .anyMatch(d -> ApiTestContext.id(documentAlias).equals(d.get("id").getAsString()));
        assertExpectation(found, expectation, "'" + tagAlias + "' etiket araması / " + documentAlias);
    }

    @Then("{string} ve {string} etiketleriyle {string} modunda arama yalnız {string} belgelerini döndürmelidir")
    public void etiketModuArama(String t1, String t2, String mode, String expectedCsv) {
        List<String> actual = aliasesOf(listRaw(TestDataFactory.admin(),
                "tag_names=" + enc(tagNames.get(t1) + "," + tagNames.get(t2)) + "&tag_mode=" + mode));
        TreeSet<String> exp = new TreeSet<>(List.of(expectedCsv.split(",\\s*")));
        if (!exp.equals(new TreeSet<>(actual))) {
            throw new AssertionError("tag_mode=" + mode + " " + exp + " döndürmeliydi, " + actual + " döndü.");
        }
    }

    @When("yönetici {string} belgesinden {string} etiketini kaldırır")
    public void etiketiKaldirir(String documentAlias, String tagAlias) {
        String name = tagNames.get(tagAlias);
        String tagId = null;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags").json().getAsJsonArray("tags")) {
            if (name.equals(e.getAsJsonObject().get("name").getAsString())) {
                tagId = e.getAsJsonObject().get("id").getAsString();
            }
        }
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags/" + tagId).expect(204, "Etiket kaldırma");
    }

    @Then("önekli başlık aramasında {string} terimi yalnız {string} belgelerini döndürmelidir")
    public void onekliBaslikAramasiTerim(String term, String expectedCsv) {
        String literal = term.replace("{BS}", "\\");
        List<String> actual = aliasesOf(listRaw(TestDataFactory.admin(), "limit=200&title=" + enc(prefix + " ") + enc(literal)));
        TreeSet<String> exp = new TreeSet<>(expectedCsv.isBlank() ? List.of() : List.of(expectedCsv.split(",\\s*")));
        if (!exp.equals(new TreeSet<>(actual))) {
            throw new AssertionError("Başlık araması '" + term + "' literal olmalı; yalnız " + exp + " dönmeliydi, " + actual + " döndü.");
        }
    }

    // ---------------------------------------------------------------------
    // Özellik filtreleri (SRCH-06)
    // ---------------------------------------------------------------------

    @When("yönetici {string} özellik filtresiyle {string} sınıfında arama yapmayı dener")
    public void ozellikFiltresiyleArama(String propsJson, String classAlias) {
        String qs = (classAlias.isBlank() ? "" : "class_id=" + ApiTestContext.id(classAlias) + "&") + "props=" + enc(propsJson);
        ApiTestContext.lastResponse(EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?" + qs + "&title=" + enc(prefix)));
    }

    @Then("son arama yalnız {string} belgelerini döndürmelidir")
    public void sonAramaYalniz(String expectedCsv) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse().expect(200, "Arama");
        List<JsonObject> items = new ArrayList<>();
        r.json().getAsJsonArray("items").forEach(e -> items.add(e.getAsJsonObject()));
        TreeSet<String> exp = new TreeSet<>(List.of(expectedCsv.split(",\\s*")));
        if (!exp.equals(new TreeSet<>(aliasesOf(items)))) {
            throw new AssertionError("Arama " + exp + " döndürmeliydi, " + aliasesOf(items) + " döndü.");
        }
    }

    // ---------------------------------------------------------------------
    // Kayıtlı aramalar (SRCH-07 arayüz + API, SRCH-10)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde önekle başlık filtresi uygulayıp {string} adıyla aramayı kaydeder")
    public void arayuzdeAramayiKaydeder(String searchAlias) {
        Page page = documentsPage();
        page.getByPlaceholder("Search by title…").fill(prefix);
        page.waitForResponse(r -> r.url().contains("/v1/documents?") && r.url().contains("title="), new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS), () -> { });
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("+ Save search")).click();
        String name = "QA arama " + System.nanoTime();
        ApiTestContext.putName(searchAlias, name);
        page.getByPlaceholder("Name this search…").fill(name);
        Response r = page.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/v1/saved-searches"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true)).click());
        if (r.status() != 201) {
            throw new AssertionError("Arama kaydetme HTTP " + r.status() + ": " + r.text());
        }
        String id = com.google.gson.JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(searchAlias, id);
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/saved-searches/" + id));
    }

    @When("yönetici başlık filtresini temizleyip {string} kayıtlı arama çipine tıklar")
    public void cipeTiklar(String searchAlias) {
        Page page = BrowserManager.page;
        page.getByPlaceholder("Search by title…").fill("");
        Response r = page.waitForResponse(resp -> resp.url().contains("/v1/documents?") && resp.url().contains("title=" + enc(prefix)),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(ApiTestContext.name(searchAlias)).setExact(true)).click());
        if (r.status() != 200) {
            throw new AssertionError("Kayıtlı arama uygulanınca liste isteği HTTP " + r.status());
        }
    }

    @Then("başlık filtresi kayıtlı aramadaki önekle doldurulmuş olmalıdır")
    public void baslikFiltresiDoldurulmus() {
        String value = BrowserManager.page.getByPlaceholder("Search by title…").inputValue();
        if (!prefix.equals(value)) {
            throw new AssertionError("Kayıtlı arama başlık filtresini geri yüklemeliydi: '" + prefix + "', gelen: '" + value + "'");
        }
    }

    @When("yönetici arayüzde {string} kayıtlı aramasını siler")
    public void arayuzdeKayitliAramayiSiler(String searchAlias) {
        Page page = BrowserManager.page;
        Response r = page.waitForResponse(resp -> "DELETE".equals(resp.request().method()) && resp.url().contains("/v1/saved-searches/"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByLabel("Delete saved search " + ApiTestContext.name(searchAlias)).click());
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("Kayıtlı arama silme HTTP " + r.status());
        }
    }

    @Then("{string} kayıtlı araması artık listelenmemelidir")
    public void kayitliAramaListelenmemeli(String searchAlias) {
        if (EcmApiClient.get(TestDataFactory.admin(), "/v1/saved-searches").expect(200, "Kayıtlı aramalar").body()
                .contains(ApiTestContext.id(searchAlias))) {
            throw new AssertionError("Silinen kayıtlı arama hâlâ listeleniyor.");
        }
        try {
            BrowserManager.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(ApiTestContext.name(searchAlias)).setExact(true))
                    .waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED).setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Silinen kayıtlı arama çipi arayüzde kaldı.", e);
        }
    }

    @When("yönetici {int} karakterlik adla kayıtlı arama oluşturmayı dener")
    public void uzunAdlaKayitliArama(int length) {
        ApiTestContext.lastResponse(savedSearch(TestDataFactory.admin(), "q".repeat(length), Map.of()));
    }

    @When("yönetici filtre alanı metin olan kayıtlı arama oluşturmayı dener")
    public void filtreMetinKayitliArama() {
        ApiTestContext.lastResponse(savedSearch(TestDataFactory.admin(), "QA metin filtre " + System.nanoTime(), "published"));
    }

    @Given("{string} {string} sınıfı filtresiyle {string} kayıtlı aramasını oluşturmuştur")
    public void sinifFiltresiyleKayitliArama(String userAlias, String classAlias, String searchAlias) {
        EcmApiClient.ApiResponse r = savedSearch(ApiTestContext.session(userAlias), "QA sınıf araması " + System.nanoTime(),
                Map.of("classId", ApiTestContext.id(classAlias)));
        r.expect(201, "Kayıtlı arama");
        ApiTestContext.putId(searchAlias, r.json().get("id").getAsString());
    }

    @Then("{string} {string} kayıtlı aramasını çalıştırdığında {string} belgesi {string}")
    public void kayitliAramayiCalistirinca(String userAlias, String searchAlias, String documentAlias, String expectation) {
        EcmApiClient.Session s = ApiTestContext.session(userAlias);
        JsonObject saved = EcmApiClient.get(s, "/v1/saved-searches/" + ApiTestContext.id(searchAlias)).expect(200, "Kayıtlı arama").json();
        String classId = saved.getAsJsonObject("filter").get("classId").getAsString();
        boolean found = listRaw(s, "class_id=" + classId).stream().anyMatch(d -> ApiTestContext.id(documentAlias).equals(d.get("id").getAsString()));
        assertExpectation(found, expectation, "kayıtlı arama (class_id=" + classId + ") / " + documentAlias);
    }

    // ---------------------------------------------------------------------
    // Etiket çipleri arayüzü (TAG-01, TAG-02)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} belgesine {string} etiketini ekler")
    public void arayuzdeEtiketEkler(String documentAlias, String tagAlias) {
        Page page = documentPage(documentAlias);
        String tag = tagNames.computeIfAbsent(tagAlias, a -> asciiTag(a));
        Locator input = page.locator("input[placeholder^='Type to add a tag']");
        input.fill(tag);
        input.press("Enter");
        Response r = page.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/tags"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add tags").setExact(true)).click());
        if (r.status() != 201) {
            throw new AssertionError("Etiket ekleme HTTP " + r.status());
        }
    }

    @Then("yönetici sayfayı yenilediğinde {string} etiket çipini görmelidir")
    public void yenilemedeEtiketCipiGormeli(String tagAlias) {
        Page page = BrowserManager.page;
        page.reload();
        try {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove " + tagNames.get(tagAlias)).setExact(true))
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Yenilemeden sonra '" + tagNames.get(tagAlias) + "' etiket çipi görünmedi.", e);
        }
    }

    @When("yönetici arayüzde {string} belgesindeki {string} etiket çipini kaldırır")
    public void arayuzdeEtiketCipiniKaldirir(String documentAlias, String tagAlias) {
        Page page = documentPage(documentAlias);
        Locator remove = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove " + tagNames.get(tagAlias)).setExact(true));
        remove.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        Response r = page.waitForResponse(resp -> "DELETE".equals(resp.request().method()) && resp.url().contains("/tags/"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS), remove::click);
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("Etiket kaldırma HTTP " + r.status());
        }
    }

    @Then("yönetici sayfayı yenilediğinde {string} etiket çipini görmemelidir")
    public void yenilemedeEtiketCipiGormemeli(String tagAlias) {
        Page page = BrowserManager.page;
        page.reload();
        page.getByText("Loading tags…").waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN).setTimeout(UI_WAIT_MS));
        page.locator("input[placeholder^='Type to add a tag']").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        if (page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Remove " + tagNames.get(tagAlias)).setExact(true)).count() > 0) {
            throw new AssertionError("Kaldırılan etiket çipi yenilemeden sonra hâlâ görünüyor.");
        }
    }

    // ---------------------------------------------------------------------

    private static EcmApiClient.ApiResponse savedSearch(EcmApiClient.Session s, String name, Object filter) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(s, "/v1/saved-searches", Map.of("name", name, "filter", filter));
        if (r.status() == 201) {
            String id = r.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(s, "/v1/saved-searches/" + id));
        }
        return r;
    }

    private String resolveFilter(String filter) {
        String out = ApiTestContext.resolve(filter);
        if (out.contains("{ME}")) {
            out = out.replace("{ME}", TestDataFactory.admin().userId);
        }
        return out;
    }

    private List<JsonObject> list(EcmApiClient.Session s, String extra) {
        String limit = extra.contains("limit=") ? "" : "limit=200&";
        return listRaw(s, limit + "title=" + enc(prefix) + (extra.isBlank() ? "" : "&" + extra));
    }

    private static List<JsonObject> listRaw(EcmApiClient.Session s, String qs) {
        List<JsonObject> out = new ArrayList<>();
        EcmApiClient.get(s, "/v1/documents?" + qs).expect(200, "Belge listesi (" + qs + ")").json().getAsJsonArray("items")
                .forEach(e -> out.add(e.getAsJsonObject()));
        return out;
    }

    private Map<String, Integer> statusFacets(EcmApiClient.Session s) {
        Map<String, Integer> out = new HashMap<>();
        JsonObject f = EcmApiClient.get(s, "/v1/documents/facets?title=" + enc(prefix)).expect(200, "Facet'ler").json();
        for (JsonElement e : f.getAsJsonArray("statuses")) {
            out.put(e.getAsJsonObject().get("value").getAsString(), e.getAsJsonObject().get("count").getAsInt());
        }
        return out;
    }

    /* Belge kimliğini senaryodaki takma ada çevirir (takma adı olmayan belge kimliğiyle raporlanır). */
    private static List<String> aliasesOf(List<JsonObject> docs) {
        List<String> out = new ArrayList<>();
        for (JsonObject d : docs) {
            out.add(aliasOf(d.get("id").getAsString()));
        }
        return out;
    }

    private static String aliasOf(String id) {
        return ApiTestContext.aliasOfId(id);
    }

    private static String currentTitle(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge").json().getAsJsonObject("document").get("title").getAsString();
    }

    private static void assertExpectation(boolean found, String expectation, String context) {
        boolean expected = expectation.startsWith("bulunmalı") || expectation.startsWith("görünmeli");
        if (found != expected) {
            throw new AssertionError(context + ": " + (expected ? "bulunmalıydı" : "bulunmamalıydı") + ".");
        }
    }

    private Page documentsPage() {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents");
        page.getByPlaceholder("Search by title…").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return page;
    }

    private static Page documentPage(String documentAlias) {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        }
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        page.locator("input[placeholder^='Type to add a tag']").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return page;
    }

    /* ECM etiket adı yalnız ASCII harf/rakam/boşluk/-/_ kabul ediyor (Unicode harfler reddediliyor, RQ-22). */
    private static String asciiTag(String alias) {
        String ascii = java.text.Normalizer.normalize(alias, java.text.Normalizer.Form.NFD)
                .replace("ı", "i").replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return "qa" + ascii + System.nanoTime();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
