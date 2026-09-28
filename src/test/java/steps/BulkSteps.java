package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Batch 30 — toplu işlemler (move / tag / untag / delete).
 * ECM kaynakları: api/rest/documents bulk_* (207 öğe bazlı sonuç, 1–100 öğe),
 * components/BulkActionsBar.tsx (seçim, işlem, sonuç özeti).
 */
public class BulkSteps {

    private static final int UI_WAIT_MS = 20000;

    private final Map<String, JsonObject> itemResults = new HashMap<>();
    private String bulkTag;

    @When("{string} {string} belgelerini {string} klasörüne toplu taşır")
    public void topluTasir(String userAlias, String documentsCsv, String folderAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("document_ids", ids(documentsCsv));
        body.put("folder_id", ApiTestContext.id(folderAlias));
        remember(EcmApiClient.post(session(userAlias), "/v1/documents/bulk/move", body));
    }

    @When("{string} {string} belgelerine toplu etiket ekler")
    public void topluEtiketEkler(String userAlias, String documentsCsv) {
        bulkTag = "qabulk" + System.nanoTime();
        remember(EcmApiClient.post(session(userAlias), "/v1/documents/bulk/tag",
                Map.of("document_ids", ids(documentsCsv), "tag_name", bulkTag)));
    }

    @When("yönetici {string} belgesine toplu etiketi ayrıca ekler")
    public void yoneticiTopluEtiketiAyricaEkler(String documentAlias) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags",
                Map.of("tag_ids", List.of(), "tag_name", bulkTag)).expect(201, "Etiket ekleme");
    }

    @When("{string} {string} belgelerinden toplu etiketi kaldırır")
    public void topluEtiketiKaldirir(String userAlias, String documentsCsv) {
        remember(EcmApiClient.post(session(userAlias), "/v1/documents/bulk/untag",
                Map.of("document_ids", ids(documentsCsv), "tag_id", tagId())));
    }

    @When("yönetici {string} belgelerini ve rastgele bir kimliği toplu siler")
    public void topluSiler(String documentsCsv) {
        List<String> list = new ArrayList<>(ids(documentsCsv));
        String random = UUID.randomUUID().toString();
        list.add(random);
        ApiTestContext.putId("rastgele kimlik", random);
        remember(EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/bulk/delete", Map.of("document_ids", list)));
    }

    @When("yönetici {int} öğeli toplu etiketleme göndermeyi dener")
    public void nOgeliTopluEtiketleme(int n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(UUID.randomUUID().toString());
        }
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/bulk/tag",
                Map.of("document_ids", list, "tag_name", "qabulkmax" + System.nanoTime())));
    }

    @Then("toplu işlem 207 dönmeli ve {string} başarılı, {string} başarısız olmalıdır")
    public void topluIslemSonucu(String okCsv, String failCsv) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() != 207) {
            throw new AssertionError("Toplu işlem 207 (öğe bazlı) dönmeliydi, HTTP " + r.status() + " " + r.body());
        }
        for (String a : split(okCsv)) {
            JsonObject item = item(a);
            if (!item.get("success").getAsBoolean()) {
                throw new AssertionError("'" + a + "' başarılı olmalıydı: " + item);
            }
        }
        for (String a : split(failCsv)) {
            JsonObject item = item(a);
            if (item.get("success").getAsBoolean()) {
                throw new AssertionError("'" + a + "' başarısız olmalıydı: " + item);
            }
        }
    }

    @Then("toplu işlemde {string} öğesinin hata kodu {string} olmalıdır")
    public void topluIslemHataKodu(String alias, String code) {
        JsonObject item = item(alias);
        String actual = item.get("error_code").isJsonNull() ? null : item.get("error_code").getAsString();
        if (!code.equals(actual)) {
            throw new AssertionError("'" + alias + "' hata kodu " + code + " olmalıydı: " + item);
        }
    }

    @Then("{string} belgeleri {string} klasöründe olmalıdır")
    public void belgelerKlasordeOlmali(String documentsCsv, String folderAlias) {
        for (String a : split(documentsCsv)) {
            String folder = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(a)).expect(200, "Belge")
                    .json().getAsJsonObject("folder").get("id").getAsString();
            if (!ApiTestContext.id(folderAlias).equals(folder)) {
                throw new AssertionError("'" + a + "' '" + folderAlias + "' klasöründe olmalıydı.");
            }
        }
    }

    @Then("toplu etiket yalnız {string} belgelerinde bulunmalıdır, {string} belgelerinde bulunmamalıdır")
    public void topluEtiketYalniz(String withCsv, String withoutCsv) {
        for (String a : split(withCsv)) {
            if (!hasTag(a)) {
                throw new AssertionError("'" + a + "' toplu etiketi taşımalıydı.");
            }
        }
        for (String a : split(withoutCsv)) {
            if (hasTag(a)) {
                throw new AssertionError("'" + a + "' toplu etiketi taşımamalıydı.");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Arayüz (BULK-05)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} belgelerini seçip toplu etiket ekler")
    public void arayuzdeTopluEtiketEkler(String documentsCsv) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        String firstTitle = ApiTestContext.name(split(documentsCsv).get(0));
        String commonPrefix = firstTitle.substring(0, firstTitle.lastIndexOf(' '));
        page.navigate(BrowserManager.getBaseUrl() + "/documents");
        page.getByPlaceholder("Search by title…").fill(commonPrefix);
        for (String a : split(documentsCsv)) {
            Locator box = page.getByLabel("Select " + ApiTestContext.name(a), new Page.GetByLabelOptions().setExact(true));
            try {
                box.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            } catch (PlaywrightException e) {
                throw new AssertionError("Listede '" + a + "' seçim kutusu görünmedi.", e);
            }
            box.check();
        }
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add tag").setExact(true)).click();
        bulkTag = "qabulkui" + System.nanoTime();
        page.getByPlaceholder("Type a tag name…").fill(bulkTag);
        Locator form = page.locator("form").filter(new Locator.FilterOptions().setHas(page.getByPlaceholder("Type a tag name…")));
        Response r = page.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/v1/documents/bulk/tag"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> form.locator("button[type='submit']").click());
        remember(new EcmApiClient.ApiResponse(r.status(), r.text(), ""));
    }

    @Then("arayüz toplu işlem özetinde {string} göstermelidir")
    public void arayuzOzetinde(String text) {
        try {
            BrowserManager.page.getByText(text).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Toplu işlem özeti '" + text + "' göstermedi.", e);
        }
    }

    // ---------------------------------------------------------------------

    private void remember(EcmApiClient.ApiResponse r) {
        ApiTestContext.lastResponse(r);
        itemResults.clear();
        if (r.status() == 207) {
            for (JsonElement e : r.json().getAsJsonArray("results")) {
                JsonObject o = e.getAsJsonObject();
                itemResults.put(o.get("document_id").getAsString(), o);
            }
        }
    }

    private JsonObject item(String alias) {
        JsonObject o = itemResults.get(ApiTestContext.id(alias));
        if (o == null) {
            throw new AssertionError("Toplu sonuçta '" + alias + "' öğesi yok: " + itemResults.keySet());
        }
        return o;
    }

    private boolean hasTag(String alias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(alias) + "/tags")
                .expect(200, "Etiketler").body().contains("\"" + bulkTag + "\"");
    }

    private String tagId() {
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/tags?limit=200&search=" + bulkTag).expect(200, "Etiketler")
                .json().getAsJsonArray("tags")) {
            if (bulkTag.equals(e.getAsJsonObject().get("name").getAsString())) {
                return e.getAsJsonObject().get("id").getAsString();
            }
        }
        throw new AssertionError("Toplu etiket bulunamadı: " + bulkTag);
    }

    private static List<String> ids(String csv) {
        List<String> out = new ArrayList<>();
        for (String a : split(csv)) {
            out.add(ApiTestContext.id(a));
        }
        return out;
    }

    private static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        if (csv.isBlank()) {
            return out;
        }
        for (String s : csv.split(",")) {
            out.add(s.trim());
        }
        return out;
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
