package steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.cucumber.datatable.DataTable;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/*
 * Batch 20 (document-integrity.feature): silinmiş belge bütünlüğü ve belge
 * ilişkileri. Kurulum/oturum/temizlik coverage-gap altyapısını kullanır
 * (EcmApiClient + ApiTestContext + TestDataFactory); belge/kullanıcı takma
 * adları o altyapının kayıtlarıdır.
 *
 * Beklentiler ECM'in kendi kuralından türetilir: soft delete edilmiş belge
 * salt okunurdur (etiket/rename/durum/taşıma/yeni sürüm/kopya zaten
 * reddediliyor). ECM'in yanlış davranışına göre UYARLANMAZ.
 */
public class DocumentIntegritySteps {

    // ---------------------------------------------------------------------
    // Ortak
    // ---------------------------------------------------------------------

    @Given("{string} belgesi API ile soft delete edilmiştir")
    public void softDeleteEdilmistir(String docAlias) {
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(docAlias))
                .expect(200, "Ön koşul: soft delete");
        JsonObject d = document(docAlias);
        if (!d.get("is_deleted").getAsBoolean()) {
            throw new AssertionError("Ön koşul: belge soft delete edilmiş olmalıydı: " + d);
        }
    }

    @Then("son API yanıtı başarısız olmalıdır")
    public void sonYanitBasarisiz() {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() < 400 || r.status() >= 500) {
            throw new AssertionError("İstek 4xx ile reddedilmeliydi, HTTP " + r.status() + " geldi. Yanıt: " + r.body());
        }
    }

    @Then("son API yanıtı başarılı olmalıdır")
    public void sonYanitBasarili() {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("İstek başarılı olmalıydı, HTTP " + r.status() + " geldi. Yanıt: " + r.body());
        }
    }

    // ---------------------------------------------------------------------
    // Sürüm etiketleri (B20-01)
    // ---------------------------------------------------------------------

    @Given("{string} belgesinin güncel sürümüne {string} etiketi eklenmiştir")
    public void etiketEklenmistir(String docAlias, String label) {
        EcmApiClient.post(TestDataFactory.admin(), labelsPath(docAlias), Map.of("label", label))
                .expect(201, "Ön koşul: sürüm etiketi");
    }

    @When("yönetici {string} belgesinin güncel sürümüne {string} etiketini eklemeyi dener")
    public void etiketEklemeyiDener(String docAlias, String label) {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), labelsPath(docAlias), Map.of("label", label)));
    }

    @When("yönetici {string} belgesinin güncel sürümünden {string} etiketini kaldırmayı dener")
    public void etiketKaldirmayiDener(String docAlias, String label) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(),
                labelsPath(docAlias) + "/" + URLEncoder.encode(label, StandardCharsets.UTF_8)));
    }

    @Then("{string} belgesinin güncel sürüm etiketleri yalnızca {string} olmalıdır")
    public void etiketlerYalnizca(String docAlias, String expectedCsv) {
        JsonArray labels = EcmApiClient.get(TestDataFactory.admin(), labelsPath(docAlias))
                .expect(200, "Sürüm etiketleri").json().getAsJsonArray("labels");
        TreeSet<String> actual = new TreeSet<>();
        for (JsonElement e : labels) {
            actual.add(e.getAsJsonObject().get("label").getAsString());
        }
        TreeSet<String> expected = new TreeSet<>(List.of(expectedCsv.split("\\s*,\\s*")));
        if (!expected.equals(actual)) {
            throw new AssertionError("Silinmiş belgenin sürüm etiketleri değişti. Beklenen " + expected
                    + ", gerçek " + actual);
        }
    }

    private String labelsPath(String docAlias) {
        String versionId = document(docAlias).get("current_version_id").getAsString();
        return "/v1/documents/" + ApiTestContext.id(docAlias) + "/versions/" + versionId + "/labels";
    }

    // ---------------------------------------------------------------------
    // Checkout (B20-03)
    // ---------------------------------------------------------------------

    @When("yönetici {string} belgesini checkout yapmayı dener")
    public void checkoutDener(String docAlias) {
        String id = ApiTestContext.id(docAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + id + "/checkout", Map.of());
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            // Hatalı kabul edilen kilidi temizlikte bırakma (yan etki olmasın).
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + id + "/lock"));
        }
    }

    @Then("{string} belgesinde aktif kilit bulunmamalıdır")
    public void kilitYok(String docAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(docAlias) + "/lock");
        if (r.status() == 200 && r.json().has("locked") && r.json().get("locked").getAsBoolean()) {
            throw new AssertionError("Silinmiş belge üzerinde kilit oluştu: " + r.body());
        }
    }

    // ---------------------------------------------------------------------
    // İlişkiler (B20-02, B20-06..08)
    // ---------------------------------------------------------------------

    @Given("yönlü {string} ilişki türü oluşturulmuştur")
    public void yonluTur(String typeAlias) {
        String name = TestDataFactory.unique("QA B20 " + typeAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/relationship-types",
                Map.of("name", name, "is_directed", true, "description", "B20"));
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError("Ön koşul: ilişki türü oluşturulamadı: HTTP " + r.status() + " " + r.body());
        }
        ApiTestContext.putName("reltype:" + typeAlias, name);
    }

    @Given("{string} belgesinden {string} belgesine {string} ilişkisi oluşturulmuştur")
    public void iliskiOlusturulmustur(String src, String dst, String typeAlias) {
        EcmApiClient.ApiResponse r = createRelationship(TestDataFactory.admin(), src, dst, typeAlias, null);
        r.expect(201, "Ön koşul: ilişki");
        ApiTestContext.putId("rel:" + src + ":" + typeAlias, r.json().get("id").getAsString());
    }

    @When("yönetici {string} belgesinden {string} belgesine {string} ilişkisi oluşturur")
    public void iliskiOlusturur(String src, String dst, String typeAlias) {
        EcmApiClient.ApiResponse r = createRelationship(TestDataFactory.admin(), src, dst, typeAlias, null);
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            ApiTestContext.putId("rel:" + src + ":" + typeAlias, r.json().get("id").getAsString());
        }
    }

    @When("{string} {string} belgesinden {string} belgesine {string} ilişkisi oluşturmayı dener")
    public void kullaniciIliskiOlusturmayiDener(String userAlias, String src, String dst, String typeAlias) {
        ApiTestContext.lastResponse(createRelationship(ApiTestContext.session(userAlias), src, dst, typeAlias, null));
    }

    @When("yönetici {string} belgesinin {string} ilişkisini silmeyi dener")
    public void iliskiSilmeyiDener(String src, String typeAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/"
                + ApiTestContext.id(src) + "/relationships/" + ApiTestContext.id("rel:" + src + ":" + typeAlias)));
    }

    @Then("{string} belgesinin giden ilişkileri arasında {string} belgesi bulunmalıdır")
    public void gidenIliski(String docAlias, String otherAlias) {
        iliskideVar(docAlias, "outbound", "target_doc_id", otherAlias);
    }

    @Then("{string} belgesinin gelen ilişkileri arasında {string} belgesi bulunmalıdır")
    public void gelenIliski(String docAlias, String otherAlias) {
        iliskideVar(docAlias, "inbound", "source_doc_id", otherAlias);
    }

    @Then("{string} belgesinin hiç ilişkisi olmamalıdır")
    public void iliskiYok(String docAlias) {
        JsonObject rels = relationships(docAlias);
        int n = rels.getAsJsonArray("outbound").size() + rels.getAsJsonArray("inbound").size();
        if (n != 0) {
            throw new AssertionError("Beklenmeyen ilişki kaydı var: " + rels);
        }
    }

    @Then("aşağıdaki ilişki denemeleri beklenen sonuçları vermeli ve yeni ilişki oluşmamalıdır:")
    public void iliskiKurallari(DataTable table) {
        int before = relationshipCount("A") + relationshipCount("B");
        List<String> failures = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            String binding = row.get("bağlama");
            String specificVersionOf = binding.startsWith("SPECIFIC:") ? binding.substring("SPECIFIC:".length()).split("'")[0] : null;
            String typeAlias = row.get("tür");
            // Tanımlanmamış tür takma adı (ör. "bilinmeyen") ECM'de olmayan bir ada
            // çözülür (typeName) -> "relationship type does not exist" beklenir.
            EcmApiClient.ApiResponse r = createRelationship(TestDataFactory.admin(), row.get("kaynak"), row.get("hedef"),
                    typeAlias, specificVersionOf);
            int expected = Integer.parseInt(row.get("beklenen"));
            if (r.status() != expected) {
                failures.add(row + " -> beklenen HTTP " + expected + ", gerçek " + r.status() + " " + r.body());
            }
        }
        int after = relationshipCount("A") + relationshipCount("B");
        if (after != before) {
            failures.add("İlişki sayısı değişti: " + before + " -> " + after);
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("İlişki kuralı ihlali:\n" + String.join("\n", failures));
        }
    }

    private EcmApiClient.ApiResponse createRelationship(EcmApiClient.Session as, String src, String dst,
                                                        String typeAlias, String specificVersionOfAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("target_doc_id", ApiTestContext.id(dst));
        body.put("relationship_type", typeName(typeAlias));
        if (specificVersionOfAlias != null) {
            body.put("binding_policy", "SPECIFIC");
            body.put("target_version_id", document(specificVersionOfAlias).get("current_version_id").getAsString());
        } else {
            body.put("binding_policy", "LATEST");
        }
        return EcmApiClient.post(as, "/v1/documents/" + ApiTestContext.id(src) + "/relationships", body);
    }

    private String typeName(String typeAlias) {
        try {
            return ApiTestContext.name("reltype:" + typeAlias);
        } catch (IllegalStateException e) {
            return "QA B20 yok " + typeAlias;
        }
    }

    private void iliskideVar(String docAlias, String direction, String field, String otherAlias) {
        JsonObject rels = relationships(docAlias);
        String otherId = ApiTestContext.id(otherAlias);
        for (JsonElement e : rels.getAsJsonArray(direction)) {
            if (otherId.equals(e.getAsJsonObject().get(field).getAsString())) {
                return;
            }
        }
        throw new AssertionError("'" + docAlias + "' " + direction + " ilişkilerinde '" + otherAlias + "' yok: " + rels);
    }

    private int relationshipCount(String docAlias) {
        JsonObject rels = relationships(docAlias);
        return rels.getAsJsonArray("outbound").size() + rels.getAsJsonArray("inbound").size();
    }

    private JsonObject relationships(String docAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(docAlias) + "/relationships")
                .expect(200, "İlişki listesi").json();
    }

    private JsonObject document(String docAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(docAlias))
                .expect(200, "Belge detayı").json().getAsJsonObject("document");
    }

    // ---------------------------------------------------------------------
    // Arayüz: silinmiş belgede sunulan kontroller (B20-04, B20-05)
    // ---------------------------------------------------------------------

    @Then("silinmiş belgenin sürüm geçmişinde etiket ekleme kontrolü görüntülenmemelidir")
    public void etiketEklemeKontroluYok() {
        Page page = silinmisDetayYuklendi();
        Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText("v1.0.0")).first();
        try {
            row.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            throw new AssertionError("Sürüm geçmişi satırı (v1.0.0) yüklenmedi; negatif kontrol yapılamaz. URL: " + page.url(), e);
        }
        int count = page.locator("button[aria-label='Add label']").count();
        if (count > 0) {
            throw new AssertionError("Soft delete edilmiş belgede 'Add label' kontrolü sunuluyor (" + count
                    + " adet). Backend bu belgede etiket eklemeyi KABUL ediyor (bkz. B20-01).");
        }
    }

    @Then("silinmiş belgenin ilişkiler kartında Link document kontrolü görüntülenmemelidir")
    public void linkDocumentYok() {
        Page page = silinmisDetayYuklendi();
        try {
            page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Relationships").setExact(true))
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
            page.getByText("Loading relationships…").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(15000));
        } catch (PlaywrightException e) {
            throw new AssertionError("İlişkiler kartı yüklenmedi; negatif kontrol yapılamaz. URL: " + page.url(), e);
        }
        Locator link = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Link document").setExact(true));
        if (link.count() > 0 && link.first().isVisible()) {
            throw new AssertionError("Soft delete edilmiş belgede 'Link document' kontrolü sunuluyor.");
        }
    }

    // Silinmiş belgenin detay sayfası verisiyle yüklenmiş olmalı: gerçek detay
    // URL'i, başlık ve "Deleted" rozeti (/login veya yükleniyor durumunda
    // negatif kontrol anlamsız olurdu).
    private Page silinmisDetayYuklendi() {
        Page page = BrowserManager.page;
        try {
            page.locator("h1").first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
            page.getByText("Loading document…").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.HIDDEN).setTimeout(15000));
            page.getByText("Deleted", new Page.GetByTextOptions().setExact(true)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            throw new AssertionError("Silinmiş belge detay sayfası yüklenmedi. URL: " + page.url(), e);
        }
        if (!page.url().matches(".*/documents/[0-9a-fA-F-]{36}.*")) {
            throw new AssertionError("Belge detay sayfasında değil. URL: " + page.url());
        }
        return page;
    }
}
