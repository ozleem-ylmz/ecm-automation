package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Download;
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

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/*
 * Batch 24 — belge yaşam döngüsü ve güvenliği: taşıma/yükleme/kopya hedef
 * yetkisi, soft delete yetkisi, çift silme/geri yükleme, kopya soyağacı,
 * Idempotency-Key, aynı başlıkta yeni sürüm önerisi ve önizleme.
 */
public class DocumentOperationsSteps {

    private static final int UI_WAIT_MS = 20000;

    private final Map<String, String> copyIds = new HashMap<>();
    private final Map<String, byte[]> uploadedBytes = new HashMap<>();
    private EcmApiClient.ApiResponse firstIdempotent;
    private Map<String, Object> idempotentBody;
    private String idempotencyKey;

    // ---------------------------------------------------------------------
    // Taşıma / yükleme / kopya hedef yetkisi (DOC-11, DOC-03, DOC-12)
    // ---------------------------------------------------------------------

    @When("{string} {string} belgesini {string} klasörüne taşımayı dener")
    public void belgeyiTasimayiDener(String userAlias, String documentAlias, String folderAlias) {
        ApiTestContext.lastResponse(EcmApiClient.put(session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/folder",
                Map.of("folder_id", ApiTestContext.id(folderAlias))));
    }

    @Then("{string} belgesi hâlâ {string} klasöründe olmalıdır")
    public void belgeHalaKlasordeOlmali(String documentAlias, String folderAlias) {
        String actual = readAsAdmin(documentAlias).getAsJsonObject("folder").get("id").getAsString();
        if (!ApiTestContext.id(folderAlias).equals(actual)) {
            throw new AssertionError("'" + documentAlias + "' '" + folderAlias + "' klasöründe kalmalıydı; şimdi " + actual);
        }
    }

    @When("{string} {string} klasörüne {string} başlıklı belge yüklemeyi dener")
    public void basliklaBelgeYuklemeyiDener(String userAlias, String folderAlias, String titleAlias) {
        String title = TestDataFactory.unique("QA Reddedilmeli");
        ApiTestContext.putName(titleAlias, title);
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/unclassified",
                unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "qa.txt",
                        ("QA " + title).getBytes(StandardCharsets.UTF_8)));
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            TestDataFactory.registerDocument("beklenmeyen " + title, r.json().get("document_id").getAsString(), title);
        }
    }

    @Then("{string} başlığıyla hiçbir belge oluşmamış olmalıdır")
    public void baslikylaBelgeOlusmamis(String titleAlias) {
        int n = titleCount(ApiTestContext.name(titleAlias));
        if (n != 0) {
            throw new AssertionError("Reddedilen yükleme belge oluşturmamalıydı; '" + titleAlias + "' başlığıyla " + n + " belge var.");
        }
    }

    @When("{string} {string} belgesini {string} klasörüne kopyalamayı dener")
    public void belgeyiKopyalamayiDener(String userAlias, String documentAlias, String folderAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/copy",
                Map.of("target_folder_id", ApiTestContext.id(folderAlias)));
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            String id = r.json().get("document_id").getAsString();
            copyIds.put(documentAlias, id);
            TestDataFactory.registerDocument(documentAlias + " kopyası", id, ApiTestContext.name(documentAlias) + " (copy)");
        }
    }

    @Then("{string} belgesinin kopyası oluşmamış olmalıdır")
    public void kopyasiOlusmamis(String documentAlias) {
        int n = titleCount(ApiTestContext.name(documentAlias) + " (copy)");
        if (n != 0 || copyIds.containsKey(documentAlias)) {
            throw new AssertionError("'" + documentAlias + "' için kopya oluşmamalıydı (" + n + " kopya bulundu).");
        }
    }

    // ---------------------------------------------------------------------
    // Çift silme / geri yükleme (DOC-10)
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin silinme durumu {string} olmalıdır")
    public void silinmeDurumu(String documentAlias, String expected) {
        boolean deleted = readAsAdmin(documentAlias).getAsJsonObject("document").get("is_deleted").getAsBoolean();
        if (deleted != Boolean.parseBoolean(expected)) {
            throw new AssertionError("'" + documentAlias + "' is_deleted=" + expected + " olmalıydı, " + deleted + " bulundu.");
        }
    }

    // ---------------------------------------------------------------------
    // Kopya soyağacı ve bağımsızlığı (DOC-13)
    // ---------------------------------------------------------------------

    @Given("{string} belgesine {int} yama sürümü eklenmiştir")
    public void belgeyeYamaSurumuEklenmistir(String documentAlias, int count) {
        for (int i = 1; i <= count; i++) {
            EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                            TestDataFactory.newVersionBody(documentAlias, "Patch", "QA sürüm " + i + " " + System.nanoTime()))
                    .expect(201, "Yama sürümü " + i);
        }
    }

    @Given("{string} belgesine {string} etiketi eklenmiştir")
    public void belgeyeEtiketEklenmistir(String documentAlias, String tagName) {
        String tag = tagName + "-" + System.nanoTime();
        ApiTestContext.putName(documentAlias + "#tag", tag);
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags",
                Map.of("tag_ids", List.of(), "tag_name", tag)).expect(201, "Etiket ekleme");
    }

    @When("yönetici {string} belgesini aynı klasöre kopyalar")
    public void yoneticiAyniKlasoreKopyalar(String documentAlias) {
        String folderId = readAsAdmin(documentAlias).getAsJsonObject("folder").get("id").getAsString();
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/copy", Map.of("target_folder_id", folderId));
        r.expect(200, "Kopyalama");
        String id = r.json().get("document_id").getAsString();
        copyIds.put(documentAlias, id);
        TestDataFactory.registerDocument(documentAlias + " kopyası", id, ApiTestContext.name(documentAlias) + " (copy)");
        ApiTestContext.putName(documentAlias + "#sourceVersion",
                readAsAdmin(documentAlias).getAsJsonObject("document").get("current_version_id").getAsString());
    }

    @Then("{string} kopyası yeni kimlikli, taslak durumunda ve kaynağın güncel içeriğiyle oluşmalıdır")
    public void kopyaYeniKimlikliTaslak(String documentAlias) {
        String copyId = copyIds.get(documentAlias);
        if (copyId.equals(ApiTestContext.id(documentAlias))) {
            throw new AssertionError("Kopya kaynakla aynı kimliği taşıyor.");
        }
        JsonObject copy = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId).expect(200, "Kopya okuma").json();
        JsonObject d = copy.getAsJsonObject("document");
        String expectedTitle = ApiTestContext.name(documentAlias) + " (copy)";
        if (!"draft".equals(d.get("status").getAsString()) || !expectedTitle.equals(d.get("title").getAsString())) {
            throw new AssertionError("Kopya '" + expectedTitle + "' başlıklı ve draft olmalıydı: " + d);
        }
        String sourceContent = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content")
                .expect(200, "Kaynak içerik").body();
        String copyContent = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId + "/content")
                .expect(200, "Kopya içerik").body();
        if (!sourceContent.equals(copyContent)) {
            throw new AssertionError("Kopya içeriği kaynağın güncel sürümüyle aynı olmalıydı.");
        }
    }

    @Then("{string} kopyasında kaynağa işaret eden copied_from ilişkisi kaynağın o anki sürümüne bağlı olmalıdır")
    public void kopyadaCopiedFromIliskisi(String documentAlias) {
        String copyId = copyIds.get(documentAlias);
        JsonObject rels = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId + "/relationships")
                .expect(200, "Kopya ilişkileri").json();
        boolean found = false;
        for (JsonElement e : rels.getAsJsonArray("outbound")) {
            JsonObject r = e.getAsJsonObject();
            if ("copied_from".equals(r.get("relationship_type").getAsString())
                    && ApiTestContext.id(documentAlias).equals(r.get("target_doc_id").getAsString())
                    && ApiTestContext.name(documentAlias + "#sourceVersion").equals(r.get("target_version_id").getAsString())) {
                found = true;
            }
        }
        if (!found) {
            throw new AssertionError("Kopyada kaynağın kopyalama anındaki sürümüne bağlı copied_from ilişkisi yok: " + rels);
        }
    }

    @Then("kaynağa yeni sürüm eklenince {string} kopyası değişmemelidir")
    public void kaynagaYeniSurumEkleninceKopyaDegismemeli(String documentAlias) {
        String copyId = copyIds.get(documentAlias);
        JsonObject before = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId).expect(200, "Kopya").json();
        String contentBefore = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId + "/content").body();
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                TestDataFactory.newVersionBody(documentAlias, "Patch", "QA kaynak sonrası " + System.nanoTime())).expect(201, "Kaynak yeni sürüm");
        JsonObject after = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId).expect(200, "Kopya").json();
        String contentAfter = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId + "/content").body();
        String vb = before.getAsJsonObject("document").get("current_version_id").getAsString();
        String va = after.getAsJsonObject("document").get("current_version_id").getAsString();
        if (!vb.equals(va) || !contentBefore.equals(contentAfter)) {
            throw new AssertionError("Kaynağın yeni sürümü kopyayı değiştirmemeliydi.");
        }
        String tags = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + copyId + "/tags").body();
        String evidence = "DOC-13 kopya etiketleri (etiket kopyalama kuralı gereksinim açık — plan 'Partial'): " + tags
                + "; kaynak etiketi: " + ApiTestContext.name(documentAlias + "#tag");
        System.out.println("[DOC-13] " + evidence);
        Allure.addAttachment("DOC-13 tag copy evidence", "text/plain", evidence);
    }

    // ---------------------------------------------------------------------
    // Idempotency-Key (DOC-15)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasörüne aynı Idempotency-Key ile aynı belgeyi iki kez yükler")
    public void ayniIdempotencyKeyIleIkiKezYukler(String folderAlias) {
        String title = TestDataFactory.unique("QA Idempotent");
        ApiTestContext.putName("idempotent belge", title);
        idempotencyKey = "qa-" + TestDataFactory.unique("idem");
        idempotentBody = unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "qa-idem.txt",
                ("QA idempotent " + title).getBytes(StandardCharsets.UTF_8));
        firstIdempotent = EcmApiClient.postWithHeaders(TestDataFactory.admin(), "/v1/documents/unclassified", idempotentBody,
                Map.of("Idempotency-Key", idempotencyKey)).expect(201, "İlk idempotent yükleme");
        TestDataFactory.registerDocument("idempotent belge", firstIdempotent.json().get("document_id").getAsString(), title);
        ApiTestContext.lastResponse(EcmApiClient.postWithHeaders(TestDataFactory.admin(), "/v1/documents/unclassified",
                idempotentBody, Map.of("Idempotency-Key", idempotencyKey)));
    }

    @Then("ikinci yanıt aynı belge kimliğini döndürmeli ve tek belge oluşmalıdır")
    public void ikinciYanitAyniKimlik() {
        EcmApiClient.ApiResponse second = ApiTestContext.lastResponse();
        if (second.status() != 201) {
            throw new AssertionError("Aynı anahtarla tekrar kayıtlı yanıtı (201) dönmeliydi, HTTP " + second.status() + " " + second.body());
        }
        String a = firstIdempotent.json().get("document_id").getAsString();
        String b = second.json().get("document_id").getAsString();
        if (!a.equals(b)) {
            throw new AssertionError("Tekrar edilen istek farklı belge döndürdü: " + a + " / " + b);
        }
        int n = titleCount(ApiTestContext.name("idempotent belge"));
        if (n != 1) {
            throw new AssertionError("Aynı Idempotency-Key ile tek belge oluşmalıydı, " + n + " belge var.");
        }
    }

    @When("yönetici aynı Idempotency-Key ile farklı gövdeli yükleme gönderir")
    public void ayniAnahtarFarkliGovde() {
        Map<String, Object> changed = new LinkedHashMap<>(idempotentBody);
        changed.put("title", idempotentBody.get("title") + " farklı");
        ApiTestContext.putName("farklı gövdeli belge", (String) changed.get("title"));
        EcmApiClient.ApiResponse r = EcmApiClient.postWithHeaders(TestDataFactory.admin(), "/v1/documents/unclassified",
                changed, Map.of("Idempotency-Key", idempotencyKey));
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            TestDataFactory.registerDocument("beklenmeyen idempotent", r.json().get("document_id").getAsString(), (String) changed.get("title"));
        }
    }

    // ---------------------------------------------------------------------
    // Aynı başlıkta yükleme -> yeni sürüm önerisi (DOC-16, arayüz)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} klasörüne {string} ile aynı başlıkta dosya yükler")
    public void arayuzdeAyniBasliktaYukler(String folderAlias, String documentAlias) throws Exception {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/new/unclassified?folder_id=" + ApiTestContext.id(folderAlias));
        Locator title = page.getByPlaceholder("e.g. Q3 Budget Spreadsheet");
        title.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        title.fill(ApiTestContext.name(documentAlias));
        Locator saveNewVersion = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save new version"));
        try {
            saveNewVersion.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.getByText("exists in this folder").first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Aynı başlık için 'yeni sürüm' uyarısı ve 'Save new version' butonu görünmedi.", e);
        }
        Path file = Files.createTempFile("qa-doc16-", ".txt");
        Files.writeString(file, "QA DOC-16 yeni sürüm içeriği " + System.nanoTime());
        page.locator("input[type='file']").setInputFiles(file);
        Response save = page.waitForResponse(
                r -> "POST".equals(r.request().method()) && r.url().contains("/v1/documents/unclassified"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                saveNewVersion::click
        );
        ApiTestContext.putName(documentAlias + "#uiSaveStatus", String.valueOf(save.status()));
        ApiTestContext.putName(documentAlias + "#uiSaveDoc",
                com.google.gson.JsonParser.parseString(save.text()).getAsJsonObject().get("document_id").getAsString());
        page.waitForURL("**/documents/" + ApiTestContext.id(documentAlias), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("{string} belgesinin sürümü {string} olmalı ve klasörde o başlıkta tek belge bulunmalıdır")
    public void surumuOlmaliVeTekBelge(String documentAlias, String expectedVersion) {
        String status = ApiTestContext.name(documentAlias + "#uiSaveStatus");
        String savedDoc = ApiTestContext.name(documentAlias + "#uiSaveDoc");
        if (!"201".equals(status) || !ApiTestContext.id(documentAlias).equals(savedDoc)) {
            throw new AssertionError("Arayüz kaydı mevcut belgeye yeni sürüm eklemeliydi (201, aynı belge). HTTP " + status + " doc=" + savedDoc);
        }
        JsonObject cv = readAsAdmin(documentAlias).getAsJsonObject("current_version");
        String v = cv.get("version_major").getAsInt() + "." + cv.get("version_minor").getAsInt() + "." + cv.get("version_patch").getAsInt();
        if (!expectedVersion.equals(v)) {
            throw new AssertionError("'" + documentAlias + "' sürümü " + expectedVersion + " olmalıydı, " + v + " bulundu.");
        }
        int n = titleCount(ApiTestContext.name(documentAlias));
        if (n != 1) {
            throw new AssertionError("Aynı başlıkta tek belge olmalıydı, " + n + " bulundu.");
        }
    }

    // ---------------------------------------------------------------------
    // PDF / PNG önizleme, desteklenmeyen tip (DOC-19)
    // ---------------------------------------------------------------------

    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Given("yönetici {string} klasörüne {string} türünde {string} belgesini yüklemiştir")
    public void yoneticiTurundeBelgeYuklemistir(String folderAlias, String kind, String documentAlias) throws Exception {
        byte[] bytes;
        String mime;
        String name;
        switch (kind) {
            case "pdf" -> { bytes = minimalPdf(); mime = "application/pdf"; name = "qa-preview.pdf"; }
            case "png" -> { bytes = PNG_1X1; mime = "image/png"; name = "qa-preview.png"; }
            case "zip" -> { bytes = zipBytes(); mime = "application/zip"; name = "qa-preview.zip"; }
            default -> throw new IllegalArgumentException("Bilinmeyen tür: " + kind);
        }
        String title = TestDataFactory.unique("QA Önizleme " + kind);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                unclassifiedBody(title, ApiTestContext.id(folderAlias), mime, name, bytes));
        r.expect(201, "Belge yükleme (" + kind + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
        uploadedBytes.put(documentAlias, bytes);
    }

    @Then("yönetici arayüzde {string} belgesinin önizlemesinde PDF görüntüleyiciyi görmelidir")
    public void onizlemedePdf(String documentAlias) {
        Page page = openDetailAndPreview(documentAlias);
        Locator frame = page.locator("iframe[title='Document preview']");
        try {
            frame.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("PDF önizleme iframe'i görünmedi.", e);
        }
        String src = frame.getAttribute("src");
        if (src == null || !src.startsWith("blob:")) {
            throw new AssertionError("PDF önizleme yüklenen içerikten (blob) oluşmalıydı, src=" + src);
        }
    }

    @Then("yönetici arayüzde {string} belgesinin önizlemesinde görselin çözümlendiğini görmelidir")
    public void onizlemedeGorsel(String documentAlias) {
        Page page = openDetailAndPreview(documentAlias);
        Locator img = page.locator("img[alt='Document preview']");
        try {
            img.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.waitForCondition(() -> ((Number) img.evaluate("i => i.complete ? i.naturalWidth : 0")).intValue() > 0,
                    new Page.WaitForConditionOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("PNG önizleme görseli çözümlenmedi (naturalWidth=0).", e);
        }
    }

    @Then("yönetici arayüzde {string} belgesi için önizleme sunulmamalı ama indirme aynı içeriği vermelidir")
    public void onizlemeYokIndirmeVar(String documentAlias) throws Exception {
        Page page = openDetail(documentAlias);
        Locator preview = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preview").setExact(true));
        if (preview.count() > 0 && preview.first().isVisible()) {
            throw new AssertionError("Desteklenmeyen tür için Preview butonu sunulmamalıydı.");
        }
        Download dl = page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true)).first().click());
        byte[] got = Files.readAllBytes(dl.path());
        if (!java.util.Arrays.equals(got, uploadedBytes.get(documentAlias))) {
            throw new AssertionError("İndirilen dosya yüklenen içerikle aynı değil (" + got.length + " bayt).");
        }
    }

    private Page openDetailAndPreview(String documentAlias) {
        Page page = openDetail(documentAlias);
        Locator preview = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preview").setExact(true));
        try {
            preview.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Preview butonu görünmedi.", e);
        }
        Response content = page.waitForResponse(r -> r.url().contains("/v1/documents/" + ApiTestContext.id(documentAlias) + "/content"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS), preview::click);
        if (content.status() != 200) {
            throw new AssertionError("Önizleme içerik isteği HTTP " + content.status());
        }
        return page;
    }

    private Page openDetail(String documentAlias) {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        }
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        page.getByText(ApiTestContext.name(documentAlias)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true)).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        return page;
    }

    private static byte[] minimalPdf() {
        String pdf = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\n"
                + "trailer<</Root 1 0 R>>\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] zipBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("qa.txt"));
            zip.write("QA zip içeriği".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    // ---------------------------------------------------------------------

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }

    private static JsonObject readAsAdmin(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma (" + documentAlias + ")").json();
    }

    /* GET /v1/documents?title= (yönetici, tüm belgeler) ile tam başlık eşleşmesi sayısı. */
    static int titleCount(String title) {
        int n = 0;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(),
                        "/v1/documents?limit=100&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8))
                .expect(200, "Başlık araması").json().getAsJsonArray("items")) {
            if (title.equals(e.getAsJsonObject().get("title").getAsString())) {
                n++;
            }
        }
        return n;
    }

    static Map<String, Object> unclassifiedBody(String title, String folderId, String mime, String filename, byte[] bytes) {
        List<Integer> content = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            content.add(b & 0xff);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("metadata", Map.of());
        body.put("mime_type", mime);
        body.put("original_filename", filename);
        body.put("folder_id", folderId);
        body.put("content", content);
        return body;
    }
}
