package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import pages.DocumentsPage;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Batch 34 — Strengthen existing (06_STRENGTHEN_EXISTING, STR-01 / STR-05).
 *
 * Eski UI senaryoları yalnız bir kontrolün GÖRÜNDÜĞÜNÜ doğruluyordu. Buradaki
 * adımlar görünürlüğü ön koşul kabul edip iddia edilen işlemi açık sayfada
 * gerçekten yapar (tıklama -> gerçek API isteği) ve kalıcı sonucu yönetici
 * API'siyle doğrular. Belge kimliği açık sayfanın adresinden okunur; eski
 * DocumentSteps durumuna bağımlılık yoktur.
 */
public class StrengthenSteps {

    private static final int UI_WAIT_MS = 20000;
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String NEW_VERSION_FILE = "src/test/resources/test-files/test-document.txt";

    @Then("{string} işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır")
    public void islemGercektenCalismali(String action) {
        perform(BrowserManager.page, action);
    }

    @Then("manager {string} işlemini açık belgede gerçekten yapabilmeli ve sonucu API ile doğrulanmalıdır")
    public void managerIslemGercektenCalismali(String action) {
        if (BrowserManager.managerPage == null) {
            throw new AssertionError("Manager sayfası açık değil");
        }
        if (!action.equals("Download") && !action.equals("Preview")) {
            throw new IllegalArgumentException("Manager için yalnız okuma işlemleri desteklenir: " + action);
        }
        perform(BrowserManager.managerPage, action);
    }

    // JRN (Batch 35): arayüzdeki aktör (varsayılan yönetici) ve yolculuktaki belge.
    private String uiActorId;
    private String journeyDocumentId;

    @io.cucumber.java.en.Given("{string} arayüzde kendi yüklediği {string} belgesinin detay sayfasındadır")
    public void kullaniciKendiBelgesininDetayinda(String userAlias, String documentAlias) {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
        }
        BrowserManager.apiLoginMainContext(ApiTestContext.name(userAlias), TestDataFactory.TEST_USER_PASSWORD);
        uiActorId = ApiTestContext.id(userAlias);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        page.locator("input[placeholder^='Type to add a tag']").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("açık belgenin denetim kaydında {string} eylemleri sırasıyla {string} aktörüyle bulunmalıdır")
    public void denetimYolculugu(String actions, String actorAlias) {
        if (journeyDocumentId == null) {
            throw new AssertionError("Yolculukta işlem yapılan belge yok");
        }
        List<String> expected = Arrays.stream(actions.split(",")).map(String::trim).toList();
        String actor = actorAlias.equals("yönetici") ? TestDataFactory.admin().userId : ApiTestContext.id(actorAlias);
        List<String> trail = new ArrayList<>();
        int matched = 0;
        long deadline = System.currentTimeMillis() + 15000;
        while (true) {
            List<JsonObject> rows = new ArrayList<>();
            EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?target_id=" + journeyDocumentId + "&limit=200")
                    .expect(200, "Denetim kayıtları").json().getAsJsonArray("items").forEach(e -> rows.add(e.getAsJsonObject()));
            rows.sort(java.util.Comparator.comparing(r -> r.get("occurred_at").getAsString()));
            trail.clear();
            matched = 0;
            for (JsonObject r : rows) {
                String a = r.get("action").getAsString();
                if (a.equals("document.read") || a.equals("document.download")) {
                    continue;
                }
                trail.add(a + "@" + (actor.equals(r.get("actor_id").getAsString()) ? actorAlias : r.get("actor_email").getAsString()));
                if (matched < expected.size() && a.equals(expected.get(matched)) && actor.equals(r.get("actor_id").getAsString())) {
                    matched++;
                }
            }
            if (matched == expected.size() || System.currentTimeMillis() > deadline) {
                break;
            }
            sleepQuietly();
        }
        Allure.addAttachment("JRN kanıtı — denetim izi", "text/plain", "Beklenen sıra: " + expected + "\nGerçek iz: " + trail);
        if (matched != expected.size()) {
            throw new AssertionError("Denetim izi beklenen eylemleri sırasıyla '" + actorAlias + "' aktörüyle içermeli ("
                    + matched + "/" + expected.size() + " eşleşti).\nBeklenen: " + expected + "\nGerçek: " + trail);
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void perform(Page page, String action) {
        String id = idFromUrl(page.url(), "documents");
        journeyDocumentId = id;
        DocumentsPage dp = new DocumentsPage(page);
        StringBuilder ev = new StringBuilder("Belge: " + id + "\nİşlem: " + action + "\n");
        if (action.startsWith("Sürüm geçmişi ")) {
            versionHistory(id, action.substring("Sürüm geçmişi ".length()).trim(), ev);
        } else {
            switch (action) {
                case "Check in" -> checkIn(page, id, ev);
                case "Check out" -> checkOut(dp, id, ev);
                case "Download" -> download(dp, id, ev);
                case "Preview" -> preview(page, id, ev);
                case "New version" -> newVersion(dp, id, ev);
                case "Version comment" -> versionComment(dp, id, ev);
                case "Classify" -> classify(page, dp, id, ev);
                case "Tag" -> tag(page, id, ev);
                case "Copy" -> copy(dp, id, ev, true);
                case "Copy onay" -> copy(dp, id, ev, false);
                case "Delete" -> softDelete(dp, id, ev);
                case "Move" -> move(page, dp, id, ev, true);
                case "Move onay" -> move(page, dp, id, ev, false);
                case "Rename" -> rename(page, dp, id, ev, true);
                case "Rename iptal" -> rename(page, dp, id, ev, false);
                default -> throw new IllegalArgumentException("Bilinmeyen güçlendirme işlemi: " + action);
            }
        }
        Allure.addAttachment("STR-01 kanıtı — " + action, "text/plain", ev.toString());
    }

    // ---------------------------------------------------------------------
    // Belge işlemleri
    // ---------------------------------------------------------------------

    private void checkIn(Page page, String id, StringBuilder ev) {
        requireLock(id, true, "Check in öncesi belge kilitli olmalıydı");
        Response r = page.waitForResponse(
                resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/v1/documents/" + id + "/checkin"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> button(page, "Check in").click());
        if (r.status() >= 300) {
            throw new AssertionError("Check in HTTP " + r.status() + ": " + r.text());
        }
        requireLock(id, false, "Check in sonrası kilit kalkmalıydı");
        button(page, "Check out").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        ev.append("POST /checkin ").append(r.status()).append("; API lock.locked=false; UI Check out tekrar sunuldu\n");
    }

    private void checkOut(DocumentsPage dp, String id, StringBuilder ev) {
        requireLock(id, false, "Check out öncesi belge kilitsiz olmalıydı");
        dp.belgeyiCheckoutYap();
        JsonObject lock = requireLock(id, true, "Check out sonrası belge kilitlenmeliydi");
        String holder = uiActorId != null ? uiActorId : TestDataFactory.admin().userId;
        if (!holder.equals(lock.get("locked_by").getAsString())) {
            throw new AssertionError("Kilit arayüzdeki kullanıcıda olmalıydı: " + lock);
        }
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + id + "/lock"));
        ev.append("API lock.locked=true, locked_by=arayüz kullanıcısı\n");
    }

    private void download(DocumentsPage dp, String id, StringBuilder ev) {
        Download d = dp.belgeyiIndir();
        byte[] got;
        try {
            got = Files.readAllBytes(d.path());
        } catch (Exception e) {
            throw new AssertionError("İndirilen dosya okunamadı", e);
        }
        byte[] expected = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + id + "/content");
        if (got.length == 0 || !Arrays.equals(got, expected)) {
            throw new AssertionError("İndirilen içerik API içeriğiyle aynı değil (UI " + got.length
                    + " bayt, API " + expected.length + " bayt)");
        }
        // Dosya adı burada yalnız kanıt olarak kaydedilir: UI adı belge kimliğine düşüyor (BUG-17,
        // CORS Content-Disposition'ı açmıyor). Tek regresyon: DL-01 (documents.feature @known-bug).
        String filename = currentVersion(id).get("original_filename").getAsString();
        ev.append("Download ").append(got.length).append(" bayt == API /content; özgün ad '").append(filename)
                .append("', tarayıcının verdiği ad '").append(d.suggestedFilename()).append("' (BUG-17)\n");
    }

    @Then("arayüzden indirilen dosya güncel sürümün özgün dosya adını taşımalıdır")
    public void indirilenDosyaOzgunAdiTasimali() {
        Page page = BrowserManager.page;
        String id = idFromUrl(page.url(), "documents");
        Download d = new DocumentsPage(page).belgeyiIndir();
        String filename = currentVersion(id).get("original_filename").getAsString();
        Allure.addAttachment("DL-01 kanıtı — indirme dosya adı", "text/plain",
                "Özgün ad (API current_version.original_filename): " + filename
                        + "\nTarayıcının kaydettiği ad: " + d.suggestedFilename()
                        + "\nUI, API'yi farklı kökenden çağırıyor (5173 -> 8080); CORS expose_headers yalnız x-total-count,"
                        + " bu yüzden Content-Disposition okunamıyor ve downloadFromUrl belge kimliğine düşüyor.");
        if (!filename.equals(d.suggestedFilename())) {
            throw new AssertionError("İndirilen dosya adı '" + d.suggestedFilename() + "' olmamalı, özgün ad '"
                    + filename + "' korunmalıydı (BUG-17)");
        }
    }

    private void preview(Page page, String id, StringBuilder ev) {
        Response r = page.waitForResponse(
                resp -> "GET".equals(resp.request().method()) && resp.url().contains("/v1/documents/" + id + "/content"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> button(page, "Preview").click());
        if (r.status() != 200) {
            throw new AssertionError("Önizleme içeriği HTTP " + r.status());
        }
        String text = new String(EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + id + "/content"),
                StandardCharsets.UTF_8);
        String firstLine = Arrays.stream(text.split("\\R")).filter(l -> !l.isBlank()).findFirst()
                .orElseThrow(() -> new AssertionError("Belge içeriği boş"));
        String probe = firstLine.trim().length() > 40 ? firstLine.trim().substring(0, 40) : firstLine.trim();
        page.getByText(probe).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        ev.append("GET /content 200; önizleme belge metnini gösteriyor: '").append(probe).append("'\n");
    }

    private void newVersion(DocumentsPage dp, String id, StringBuilder ev) {
        List<JsonObject> before = versions(id);
        dp.yeniVersiyonSayfasiniAc();
        dp.yeniVersiyonDosyasiniSec();
        dp.yeniVersiyonuKaydet();
        List<JsonObject> after = poll(() -> versions(id), v -> v.size() == before.size() + 1, "Yeni sürüm API'de oluşmadı");
        JsonObject newest = after.get(after.size() - 1);
        if (!newest.get("is_current").getAsBoolean() || compare(newest, before.get(before.size() - 1)) <= 0) {
            throw new AssertionError("Yeni sürüm güncel ve öncekinden büyük olmalıydı: " + label(newest));
        }
        assertContentEqualsFile(id);
        ev.append("Sürüm ").append(label(before.get(before.size() - 1))).append(" -> ").append(label(newest))
                .append(" (güncel); /content == yüklenen dosya\n");
    }

    private void versionComment(DocumentsPage dp, String id, StringBuilder ev) {
        String comment = "QA STR sürüm notu " + System.nanoTime();
        List<JsonObject> before = versions(id);
        dp.versionCommentGir(comment);
        dp.versionTipiniSec("Patch");
        dp.yeniVersiyonDosyasiniSec();
        dp.yeniVersiyonuKaydet();
        List<JsonObject> after = poll(() -> versions(id), v -> v.size() == before.size() + 1, "Yeni sürüm API'de oluşmadı");
        JsonObject newest = after.get(after.size() - 1);
        JsonElement saved = newest.get("change_comment");
        if (saved == null || saved.isJsonNull() || !comment.equals(saved.getAsString())) {
            throw new AssertionError("Sürüm notu API'de '" + comment + "' olmalıydı: " + saved);
        }
        ev.append(label(newest)).append(" change_comment='").append(comment).append("'\n");
    }

    private void classify(Page page, DocumentsPage dp, String id, StringBuilder ev) {
        if (!document(id).get("class_id").isJsonNull()) {
            throw new AssertionError("Belge sınıflandırma öncesi sınıfsız olmalıydı");
        }
        String classId = createClass();
        String className = ApiTestContext.name("STR sınıfı " + classId);
        dp.classifyButonunaTikla();
        if (!dp.classifyEkraniGorunuyorMu()) {
            throw new AssertionError("Classify ekranı açılmadı");
        }
        Locator select = page.locator("form select").first();
        select.locator("option", new Locator.LocatorOptions().setHasText(className))
                .waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED).setTimeout(UI_WAIT_MS));
        select.selectOption(new SelectOption().setLabel(className));
        Locator assign = button(page, "Assign class");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(assign)
                .isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions().setTimeout(UI_WAIT_MS));
        assign.click();
        page.waitForURL(url -> url.endsWith("/documents/" + id), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
        JsonObject doc = poll(() -> document(id), d -> !d.get("class_id").isJsonNull(), "Belge API'de sınıflandırılmadı");
        if (!classId.equals(doc.get("class_id").getAsString())) {
            throw new AssertionError("Belge sınıfı " + classId + " olmalıydı: " + doc.get("class_id"));
        }
        ev.append("Sınıf '").append(className).append("' atandı; API class_id eşleşiyor\n");
    }

    private void tag(Page page, String id, StringBuilder ev) {
        String tag = "qastr" + Long.toString(System.nanoTime(), 36);
        Locator input = page.locator("input[placeholder^='Type to add a tag']");
        input.fill(tag);
        input.press("Enter");
        Response r = page.waitForResponse(
                resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/v1/documents/" + id + "/tags"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> button(page, "Add tags").click());
        if (r.status() != 201) {
            throw new AssertionError("Etiket ekleme HTTP " + r.status() + ": " + r.text());
        }
        List<String> tags = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/tags").expect(200, "Belge etiketleri")
                .json().getAsJsonArray("tags").forEach(e -> tags.add(e.getAsJsonObject().get("name").getAsString()));
        if (!tags.contains(tag)) {
            throw new AssertionError("Etiket '" + tag + "' API'de belgede olmalıydı: " + tags);
        }
        button(page, "Remove " + tag).waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        ev.append("Etiket ").append(tag).append(" eklendi; API etiketleri ").append(tags).append('\n');
    }

    /* openDialog=false: senaryo Copy penceresini zaten açık bırakmış (pencere açılabilir iddiası). */
    private void copy(DocumentsPage dp, String id, StringBuilder ev, boolean openDialog) {
        JsonObject source = document(id);
        String copyTitle = TestDataFactory.unique("QA STR Kopya");
        if (openDialog) {
            dp.copyPenceresiniAc();
        }
        dp.copyBasligiGir(copyTitle);
        String copyId = dp.copyIsleminiOnayla();
        dp.kopyaBelgeDetayiniBekle(copyTitle, copyId);
        if (copyId.equals(id)) {
            throw new AssertionError("Kopya yeni bir belge olmalıydı");
        }
        JsonObject copyDoc = document(copyId);
        if (!copyTitle.equals(copyDoc.get("title").getAsString()) || copyDoc.get("is_deleted").getAsBoolean()) {
            throw new AssertionError("Kopya başlığı/durumu hatalı: " + copyDoc);
        }
        byte[] src = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + id + "/content");
        byte[] cpy = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + copyId + "/content");
        if (!Arrays.equals(src, cpy)) {
            throw new AssertionError("Kopya içeriği kaynakla aynı olmalıydı");
        }
        if (!source.get("title").getAsString().equals(document(id).get("title").getAsString())) {
            throw new AssertionError("Kaynak belge kopyalama sırasında değişmemeliydi");
        }
        ev.append("Kopya ").append(copyId).append(" '").append(copyTitle).append("'; içerik kaynağa eşit (")
                .append(cpy.length).append(" bayt); kaynak değişmedi\n");
    }

    private void softDelete(DocumentsPage dp, String id, StringBuilder ev) {
        dp.belgeyiSoftDeleteYap();
        JsonObject doc = poll(() -> document(id), d -> d.get("is_deleted").getAsBoolean(), "Belge API'de silinmedi");
        ev.append("API is_deleted=true, deleted_by=").append(doc.get("deleted_by")).append('\n');
    }

    /* openDialog=false: senaryo Move penceresini zaten açık bırakmış. */
    private void move(Page page, DocumentsPage dp, String id, StringBuilder ev, boolean openDialog) {
        String sourceFolder = folderOf(id);
        String alias = "STR hedef " + id;
        String targetId = TestDataFactory.createFolder(TestDataFactory.admin(), alias, null, true);
        if (!openDialog) {
            // Açık pencerenin klasör listesi hedef klasörden önce yüklendi; pencere kapatılıp yeniden açılır.
            dp.moveIsleminiIptalEt();
        }
        dp.moveButonunaTikla();
        if (!dp.movePenceresiGorunuyorMu()) {
            throw new AssertionError("Move penceresi açılmadı");
        }
        dp.moveHedefKlasorSec(ApiTestContext.name(alias));
        dp.moveIsleminiOnayla();
        poll(() -> folderOf(id), targetId::equals, "Belge API'de hedef klasöre taşınmadı");
        ev.append("Klasör ").append(sourceFolder).append(" -> ").append(targetId).append('\n');
    }

    private void rename(Page page, DocumentsPage dp, String id, StringBuilder ev, boolean confirm) {
        String before = document(id).get("title").getAsString();
        // Önceki işlem (ör. Classify) detay sayfasına yeni dönmüş olabilir: başlık render edilene kadar bekle.
        // Düzenleyici açıksa başlık yerine giriş alanı gösterilir (B15-02/04/05).
        if (!dp.renameEditoruAcikMi()) {
            page.locator("h1").filter(new Locator.FilterOptions().setHasText(before)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            dp.renameEditorunuAc();
        }
        String newTitle = TestDataFactory.unique("QA STR Yeni Ad");
        dp.renameBasligiGir(newTitle);
        if (confirm) {
            dp.renameKaydet();
            dp.renameSonucunuBekle(newTitle);
            poll(() -> document(id).get("title").getAsString(), newTitle::equals, "Yeni başlık API'ye yazılmadı");
            ev.append("Başlık '").append(before).append("' -> '").append(newTitle).append("' (API)\n");
        } else {
            dp.renameIptalEt();
            String after = document(id).get("title").getAsString();
            if (!before.equals(after) || !before.equals(dp.mevcutBelgeBasliginiAl().trim())) {
                throw new AssertionError("İptal sonrası başlık değişmemeliydi: API '" + after + "', UI '"
                        + dp.mevcutBelgeBasliginiAl() + "'");
            }
            ev.append("İptal: başlık '").append(before).append("' API ve UI'da değişmedi\n");
        }
    }

    private void versionHistory(String id, String expectedLabel, StringBuilder ev) {
        List<JsonObject> vs = versions(id);
        JsonObject newest = vs.get(vs.size() - 1);
        int expectedCount = expectedLabel.equals("v1.0.0") ? 1 : 2;
        if (vs.size() != expectedCount || !expectedLabel.equals(label(newest)) || !newest.get("is_current").getAsBoolean()) {
            throw new AssertionError("API sürüm geçmişi " + expectedCount + " sürüm ve güncel " + expectedLabel
                    + " içermeliydi: " + vs.stream().map(StrengthenSteps::label).toList());
        }
        JsonObject cur = currentVersion(id);
        if (!newest.get("id").getAsString().equals(cur.get("id").getAsString())) {
            throw new AssertionError("Belgenin current_version kaydı en yeni sürüm olmalıydı");
        }
        ev.append("API sürümleri ").append(vs.stream().map(StrengthenSteps::label).toList())
                .append("; current_version = ").append(expectedLabel).append('\n');
    }

    // ---------------------------------------------------------------------
    // Klasör güçlendirmeleri (STR-01 folders, STR-05)
    // ---------------------------------------------------------------------

    @Then("açık klasörün breadcrumb'ı API'deki üst klasör zinciriyle eşleşmeli ve üst klasöre gidilebilmelidir")
    public void breadcrumbApiIleEslesmeli() {
        Page page = BrowserManager.page;
        String id = idFromUrl(page.url(), "folders");
        JsonObject folder = folder(id);
        String parentId = folder.get("parent_id").getAsString();
        JsonObject parent = folder(parentId);
        Locator nav = page.locator("nav[aria-label='Breadcrumb']");
        nav.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        nav.locator("[title=\"" + folder.get("name").getAsString() + "\"]").first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        List<String> titles = new ArrayList<>();
        for (Locator l : nav.locator("[title]").all()) {
            titles.add(l.getAttribute("title"));
        }
        List<String> tail = titles.subList(Math.max(0, titles.size() - 2), titles.size());
        List<String> expected = List.of(parent.get("name").getAsString(), folder.get("name").getAsString());
        if (!tail.equals(expected)) {
            throw new AssertionError("Breadcrumb sonu API zinciri " + expected + " olmalıydı: " + titles);
        }
        nav.locator("a[title=\"" + parent.get("name").getAsString() + "\"]").first().click();
        page.waitForURL(url -> url.endsWith("/folders/" + parentId), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
        Allure.addAttachment("STR-01 kanıtı — breadcrumb", "text/plain",
                "Breadcrumb: " + titles + "\nAPI: " + expected + "\nÜst klasör bağlantısı /folders/" + parentId + " adresine gitti");
    }

    @Then("Explorer bölümü API ile eklenen alt klasörü ve belgeyi listelemeli ve alt klasör açılabilmelidir")
    public void explorerApiIleEslesmeli() {
        Page page = BrowserManager.page;
        String id = idFromUrl(page.url(), "folders");
        ApiTestContext.putId("STR explorer", id);
        String childId = TestDataFactory.createFolder(TestDataFactory.admin(), "STR alt klasör", "STR explorer", true);
        String docId = TestDataFactory.createUnclassifiedDocument(TestDataFactory.admin(), "STR explorer belgesi", "STR explorer");
        int children = folderDetail(id).getAsJsonArray("children").size();
        JsonObject list = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?folder_id=" + id + "&limit=100")
                .expect(200, "Klasör belgeleri").json();
        int docs = list.getAsJsonArray("items").size();
        page.reload();
        String counter = children + " subfolder" + (children == 1 ? "" : "s") + " · " + docs + " document" + (docs == 1 ? "" : "s");
        page.getByText(counter, new Page.GetByTextOptions().setExact(true)).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        page.locator("a[href$='/documents/" + docId + "']").first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        page.locator("a[href$='/folders/" + childId + "']").first().click();
        page.waitForURL(url -> url.endsWith("/folders/" + childId), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
        Allure.addAttachment("STR-01 kanıtı — Explorer", "text/plain",
                "API: " + children + " alt klasör, " + docs + " belge; UI sayacı '" + counter
                        + "'; belge bağlantısı var; alt klasör bağlantısı /folders/" + childId + " açtı");
    }

    @Then("açık klasörün adı sunucuda 255 karakterle değiştirilebilmeli, 256 karakter reddedilmelidir")
    public void klasorAdiSunucuSiniri() {
        String id = idFromUrl(BrowserManager.page.url(), "folders");
        String original = folder(id).get("name").getAsString();
        String n256 = "Q".repeat(256);
        EcmApiClient.ApiResponse r = EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + id, Map.of("name", n256));
        if (r.status() != 422 || !original.equals(folder(id).get("name").getAsString())) {
            throw new AssertionError("256 karakterlik ad 422 ile reddedilmeli ve ad değişmemeliydi: HTTP " + r.status()
                    + " " + r.body());
        }
        String n255 = ("QA STR " + System.nanoTime() + "-").concat("R".repeat(255)).substring(0, 255);
        EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + id, Map.of("name", n255))
                .expect(200, "255 karakterlik klasör adı");
        if (!n255.equals(folder(id).get("name").getAsString())) {
            throw new AssertionError("255 karakterlik ad kalıcı olarak saklanmalıydı");
        }
        Allure.addAttachment("STR-05 kanıtı — sunucu ad sınırı", "text/plain",
                "PATCH 256 karakter -> " + r.status() + " (" + r.errorMessage() + "), ad değişmedi; PATCH 255 karakter -> 200, saklandı");
    }

    // ---------------------------------------------------------------------

    static String idFromUrl(String url, String segment) {
        Matcher m = Pattern.compile("/" + segment + "/(" + UUID.pattern() + ")").matcher(url);
        if (!m.find()) {
            m = UUID.matcher(url);
            if (!m.find()) {
                throw new AssertionError("Adreste " + segment + " kimliği yok: " + url);
            }
            return m.group();
        }
        return m.group(1);
    }

    private static Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private static JsonObject read(String id) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id).expect(200, "Belge okuma (yönetici)").json();
    }

    private static JsonObject document(String id) {
        return read(id).getAsJsonObject("document");
    }

    private static JsonObject currentVersion(String id) {
        return read(id).getAsJsonObject("current_version");
    }

    private static String folderOf(String id) {
        return read(id).getAsJsonObject("folder").get("id").getAsString();
    }

    private static JsonObject folderDetail(String id) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + id).expect(200, "Klasör okuma").json();
    }

    private static JsonObject folder(String id) {
        return folderDetail(id).getAsJsonObject("folder");
    }

    private static JsonObject requireLock(String id, boolean locked, String message) {
        return poll(() -> EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/lock").expect(200, "Kilit").json(),
                l -> l.get("locked").getAsBoolean() == locked, message);
    }

    private static List<JsonObject> versions(String id) {
        List<JsonObject> out = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/versions").expect(200, "Sürümler")
                .json().getAsJsonArray("items").forEach(e -> out.add(e.getAsJsonObject()));
        out.sort(StrengthenSteps::compare);
        return out;
    }

    private static int compare(JsonObject a, JsonObject b) {
        for (String k : new String[]{"version_major", "version_minor", "version_patch"}) {
            int c = Integer.compare(a.get(k).getAsInt(), b.get(k).getAsInt());
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static String label(JsonObject v) {
        return "v" + v.get("version_major").getAsInt() + "." + v.get("version_minor").getAsInt() + "." + v.get("version_patch").getAsInt();
    }

    private static void assertContentEqualsFile(String id) {
        try {
            byte[] file = Files.readAllBytes(Paths.get(NEW_VERSION_FILE));
            byte[] api = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + id + "/content");
            if (!Arrays.equals(file, api)) {
                throw new AssertionError("Güncel sürüm içeriği yüklenen dosyayla aynı olmalıydı");
            }
        } catch (java.io.IOException e) {
            throw new AssertionError("Test dosyası okunamadı", e);
        }
    }

    private static String createClass() {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("key", "ref");
        prop.put("display_name", "REF");
        prop.put("data_type", "text");
        prop.put("is_multi_value", false);
        prop.put("required", false);
        String name = TestDataFactory.unique("QA STR Sınıf");
        JsonObject created = EcmApiClient.post(TestDataFactory.admin(), "/v1/classes",
                        Map.of("name", name, "description", "QA Batch 34", "properties", List.of(prop)))
                .expect(200, "Sınıf oluşturma").json();
        String classId = created.get("id").getAsString();
        ApiTestContext.putName("STR sınıfı " + classId, name);
        ApiTestContext.onCleanup(() -> {
            JsonObject c = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + classId).json();
            if (c.has("version")) {
                EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + classId + "/active",
                        Map.of("expected_version", c.get("version").getAsLong(), "is_active", false));
            }
        });
        return classId;
    }

    private static <T> T poll(Supplier<T> read, Predicate<T> ok, String message) {
        long deadline = System.currentTimeMillis() + 10000;
        T last = read.get();
        while (!ok.test(last)) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(message + ": " + last);
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(message, e);
            }
            last = read.get();
        }
        return last;
    }
}
