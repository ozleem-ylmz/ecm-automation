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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Batch 23 — klasör ACL'i, sahiplik ve klasör yaşam döngüsü.
 * ECM kaynakları: src/engine/acl/runtime.rs (assert_document_access,
 * can_manage_folder_scoped), src/engine/acl/service.rs (sıralı karar),
 * src/engine/folders/write.rs (patch/delete/active/owner),
 * src/api/rest/folders.rs (require_manage_folder).
 */
public class FolderApiSteps {

    private static final int UI_WAIT_MS = 20000;

    private final Map<String, String> aclIds = new HashMap<>();
    private final Map<String, Integer> aclCounts = new HashMap<>();
    private JsonObject uploadPicker;

    // ---------------------------------------------------------------------
    // Belge taşıma ve liste görünürlüğü (ACL-03)
    // ---------------------------------------------------------------------

    @Given("yetki devralması açık {string} oluşturulmuştur")
    public void yetkiDevralmasiAcikKlasorOlusturulmustur(String folderAlias) {
        TestDataFactory.createFolder(TestDataFactory.admin(), folderAlias, null, true);
    }

    @Given("yönetici {string} alt klasörünü {string} altında kalıtım kapalı oluşturmuştur")
    public void yoneticiKalitimKapaliAltKlasorOlusturmustur(String folderAlias, String parentAlias) {
        TestDataFactory.createFolder(TestDataFactory.admin(), folderAlias, parentAlias, false);
    }

    @When("yönetici {string} belgesini {string} klasörüne taşır")
    public void yoneticiBelgeyiKlasoreTasir(String documentAlias, String folderAlias) {
        EcmApiClient.put(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/folder",
                        Map.of("folder_id", ApiTestContext.id(folderAlias)))
                .expect(200, "Belge taşıma (" + documentAlias + " -> " + folderAlias + ")");
    }

    @Then("{string} belge aramasında {string} belgesini görmelidir")
    public void belgeAramasindaGormeli(String userAlias, String documentAlias) {
        if (!searchTitles(userAlias, documentAlias).contains(ApiTestContext.name(documentAlias))) {
            throw new AssertionError("'" + userAlias + "' belge listesi aramasında '" + documentAlias + "' görünmeliydi.");
        }
    }

    @Then("{string} belge aramasında {string} belgesini görmemelidir")
    public void belgeAramasindaGormemeli(String userAlias, String documentAlias) {
        // Pozitif kontrol: aynı sorgu yönetici için belgeyi bulmalı; aksi halde filtre
        // çalışmıyordur ve "görünmüyor" sonucu anlamsız olur.
        if (!searchTitles(TestDataFactory.admin(), documentAlias).contains(ApiTestContext.name(documentAlias))) {
            throw new AssertionError("Başlık filtresi yönetici için de '" + documentAlias + "' belgesini bulmadı; kontrol geçersiz.");
        }
        if (searchTitles(ApiTestContext.session(userAlias), documentAlias).contains(ApiTestContext.name(documentAlias))) {
            throw new AssertionError("'" + userAlias + "' belge listesi aramasında '" + documentAlias + "' görünmemeliydi.");
        }
    }

    private static List<String> searchTitles(String userAlias, String documentAlias) {
        return searchTitles(ApiTestContext.session(userAlias), documentAlias);
    }

    /* GET /v1/documents?title= (documents_query.rs ListDocumentsQuery.title). */
    private static List<String> searchTitles(EcmApiClient.Session s, String documentAlias) {
        String title = ApiTestContext.name(documentAlias);
        String q = URLEncoder.encode(title, StandardCharsets.UTF_8);
        List<String> titles = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(s, "/v1/documents?title=" + q + "&limit=100")
                .expect(200, "Belge listesi araması").json().getAsJsonArray("items")) {
            titles.add(e.getAsJsonObject().get("title").getAsString());
        }
        return titles;
    }

    // ---------------------------------------------------------------------
    // Belge düzeyi erişim istisnaları (ACL-04 / ACL-05 / ACL-09)
    // ---------------------------------------------------------------------

    @Given("{string} belgesinde {string} kullanıcısına doğrudan {string} rolü verilmiştir")
    public void belgedeDogrudanRolVerilmistir(String documentAlias, String userAlias, String aclRole) {
        aclIds.put(documentAlias + "|" + userAlias + "|allow", grantDocument(documentAlias, userAlias, aclRole, false));
    }

    @Given("{string} belgesinde {string} kullanıcısı için {string} rolü doğrudan reddedilmiştir")
    public void belgedeDogrudanReddedilmistir(String documentAlias, String userAlias, String aclRole) {
        aclIds.put(documentAlias + "|" + userAlias + "|deny", grantDocument(documentAlias, userAlias, aclRole, true));
    }

    @When("yönetici {string} belgesindeki {string} için reddetme kuralını kaldırır")
    public void yoneticiReddetmeKuraliniKaldirir(String documentAlias, String userAlias) {
        String aclId = aclIds.get(documentAlias + "|" + userAlias + "|deny");
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/acl/" + aclId)
                .expect(200, "Belge DENY kuralını kaldırma");
    }

    @Given("{string} belgesinin doğrudan erişim listesi kaydedilmiştir")
    public void belgeErisimListesiKaydedilmistir(String documentAlias) {
        aclCounts.put("doc:" + documentAlias, documentAclCount(documentAlias));
    }

    @When("{string} {string} belgesinde All Users grubuna doğrudan okuma erişimi vermeyi dener")
    public void belgedeAllUsersErisimiVermeyiDener(String userAlias, String documentAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.ROLE_VIEWER);
        body.put("group_id", TestDataFactory.ALL_USERS_GROUP);
        body.put("deny", false);
        ApiTestContext.lastResponse(EcmApiClient.post(ApiTestContext.session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/acl", body));
    }

    @Then("{string} belgesinin doğrudan erişim listesi değişmemiş olmalıdır")
    public void belgeErisimListesiDegismemis(String documentAlias) {
        int before = aclCounts.get("doc:" + documentAlias);
        int after = documentAclCount(documentAlias);
        if (before != after) {
            throw new AssertionError("'" + documentAlias + "' doğrudan ACL satır sayısı değişmemeliydi: " + before + " -> " + after);
        }
    }

    private static String grantDocument(String documentAlias, String userAlias, String aclRole, boolean deny) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.roleId(aclRole));
        body.put("user_id", ApiTestContext.id(userAlias));
        body.put("deny", deny);
        return EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/acl", body)
                .expect(200, "Belge ACL (" + (deny ? "deny" : "allow") + ")").json().get("id").getAsString();
    }

    private static int documentAclCount(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/acl")
                .expect(200, "Belge ACL listesi").json().getAsJsonArray("items").size();
    }

    // ---------------------------------------------------------------------
    // Klasör ACL kuralları (ACL-07 / ACL-12)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasöründe {string} kullanıcısına {string} rolünü tekrar vermeyi dener")
    public void yoneticiAyniKuraliTekrarVermeyiDener(String folderAlias, String userAlias, String aclRole) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.roleId(aclRole));
        body.put("user_id", ApiTestContext.id(userAlias));
        body.put("deny", false);
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(),
                "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl", body));
    }

    @Then("{string} klasöründe {string} kullanıcısı için tek izin satırı bulunmalıdır")
    public void klasordeTekIzinSatiri(String folderAlias, String userAlias) {
        long n = folderAcl(folderAlias).stream()
                .filter(a -> userAlias != null && ApiTestContext.id(userAlias).equals(str(a, "user_id")))
                .count();
        if (n != 1) {
            throw new AssertionError("'" + userAlias + "' için klasörde tek ACL satırı olmalıydı, " + n + " bulundu.");
        }
    }

    @Given("{string} klasöründe {string} grubu için {string} rolü reddedilmiştir")
    public void klasordeGrupIcinReddedilmistir(String folderAlias, String groupAlias, String aclRole) {
        TestDataFactory.grantFolder(folderAlias, TestDataFactory.roleId(aclRole), groupAlias, true, true);
    }

    @When("yönetici {string} klasöründe {string} kullanıcısının izin kuralını kaldırır")
    public void yoneticiKullaniciIzinKuraliniKaldirir(String folderAlias, String userAlias) {
        String userId = ApiTestContext.id(userAlias);
        String aclId = folderAcl(folderAlias).stream()
                .filter(a -> userId.equals(str(a, "user_id")) && !a.get("deny").getAsBoolean())
                .map(a -> a.get("id").getAsString()).findFirst()
                .orElseThrow(() -> new AssertionError("'" + userAlias + "' için allow satırı yok."));
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl/" + aclId)
                .expect(200, "Klasör ACL satırı kaldırma");
    }

    // ---------------------------------------------------------------------
    // Klasör silme ve kök klasör korumaları (FLD-01 / FLD-04)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasörünü silmeyi dener")
    public void yoneticiKlasoruSilmeyiDener(String folderAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias)));
    }

    @When("yönetici kök klasörü silmeyi dener")
    public void yoneticiKokKlasoruSilmeyiDener() {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID));
    }

    @Then("{string} klasörü artık bulunmamalıdır")
    public void klasorArtikBulunmamali(String folderAlias) {
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias)).status();
        if (s != 404) {
            throw new AssertionError("Silinen klasör için GET 404 olmalıydı, HTTP " + s);
        }
    }

    @Then("{string} klasörü hâlâ bulunmalıdır")
    public void klasorHalaBulunmali(String folderAlias) {
        EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias))
                .expect(200, "Silinmemesi gereken klasör");
    }

    /* İşlemler: pasifleştirme | kalıtım değiştirme | sahiplik devri */
    @When("yönetici kök klasörde {string} işlemini dener")
    public void yoneticiKokKlasordeIslemDener(String action) {
        String base = "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID;
        EcmApiClient.Session a = TestDataFactory.admin();
        EcmApiClient.ApiResponse r = switch (action) {
            case "pasifleştirme" -> EcmApiClient.put(a, base + "/active", Map.of("is_active", false));
            case "kalıtım değiştirme" -> EcmApiClient.patch(a, base, Map.of("inherit_permissions", false));
            case "sahiplik devri" -> EcmApiClient.patch(a, base + "/owner", Map.of("new_owner_id", a.userId));
            default -> throw new IllegalArgumentException("Bilinmeyen kök klasör işlemi: " + action);
        };
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            restoreRoot();
        }
    }

    @When("{string} kök klasörü yeniden adlandırmayı dener")
    public void kokKlasoruYenidenAdlandirmayiDener(String userAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.patch(ApiTestContext.session(userAlias),
                "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID, Map.of("name", "QA İzinsiz Kök " + System.nanoTime()));
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            restoreRoot();
        }
    }

    @Then("kök klasör {string} adıyla aktif ve kalıtımı açık kalmalıdır")
    public void kokKlasorDegismemisOlmali(String expectedName) {
        JsonObject root = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID)
                .expect(200, "Kök klasör").json().getAsJsonObject("folder");
        if (!expectedName.equals(root.get("name").getAsString()) || !root.get("is_active").getAsBoolean()
                || !root.get("inherit_permissions").getAsBoolean()) {
            throw new AssertionError("Kök klasör değişmemeliydi: " + root);
        }
    }

    /* Paylaşılan ortam korunur: beklenmeyen kabulde kök hemen eski haline döner. */
    private static void restoreRoot() {
        String base = "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID;
        EcmApiClient.put(TestDataFactory.admin(), base + "/active", Map.of("is_active", true));
        EcmApiClient.patch(TestDataFactory.admin(), base, Map.of("name", "Root", "inherit_permissions", true));
        System.out.println("[FLD-04] Kök klasör değişikliği kabul edildi ve geri alındı.");
    }

    @Then("yönetici tarayıcıda {string} klasörünü açtığında Delete butonu görünmelidir")
    public void tarayicidaDeleteButonuGorunmeli(String folderAlias) {
        if (!deleteButtonVisible(folderAlias)) {
            throw new AssertionError("Boş klasörde admin için Delete butonu görünmeliydi. URL: " + BrowserManager.page.url());
        }
    }

    @Then("yönetici tarayıcıda {string} klasörünü açtığında Delete butonu görünmemelidir")
    public void tarayicidaDeleteButonuGorunmemeli(String folderAlias) {
        if (deleteButtonVisible(folderAlias)) {
            throw new AssertionError("İçeriği olan klasörde Delete butonu görünmemeliydi. URL: " + BrowserManager.page.url());
        }
    }

    private static boolean deleteButtonVisible(String folderAlias) {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        }
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/folders/" + ApiTestContext.id(folderAlias));
        try {
            // FolderDetail.tsx: admin eylem çubuğu yüklendiğinde "Inactivate" her zaman görünür.
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Inactivate").setExact(true))
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.getByText(ApiTestContext.name(folderAlias)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Klasör detayı yüklenmedi. URL: " + page.url(), e);
        }
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        Locator delete = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete").setExact(true));
        return delete.count() > 0 && delete.first().isVisible();
    }

    // ---------------------------------------------------------------------
    // Pasif klasör (FLD-03), sahip pasifleştirme (OWN-04)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasörünü pasifleştirir")
    public void yoneticiKlasoruPasiflestirir(String folderAlias) {
        setFolderActive(TestDataFactory.admin(), folderAlias, false).expect(200, "Klasör pasifleştirme");
    }

    @When("yönetici {string} klasörünü aktifleştirir")
    public void yoneticiKlasoruAktiflestirir(String folderAlias) {
        setFolderActive(TestDataFactory.admin(), folderAlias, true).expect(200, "Klasör aktifleştirme");
    }

    @When("{string} {string} klasörünü pasifleştirmeyi dener")
    public void kullaniciKlasoruPasiflestirmeyiDener(String userAlias, String folderAlias) {
        ApiTestContext.lastResponse(setFolderActive(ApiTestContext.session(userAlias), folderAlias, false));
    }

    @When("{string} {string} klasörünü aktifleştirmeyi dener")
    public void kullaniciKlasoruAktiflestirmeyiDener(String userAlias, String folderAlias) {
        ApiTestContext.lastResponse(setFolderActive(ApiTestContext.session(userAlias), folderAlias, true));
    }

    @Then("{string} klasörü pasif olmalıdır")
    public void klasorPasifOlmali(String folderAlias) {
        if (folder(folderAlias).get("is_active").getAsBoolean()) {
            throw new AssertionError("'" + folderAlias + "' pasif olmalıydı.");
        }
    }

    @Then("{string} klasörü aktif olmalıdır")
    public void klasorAktifOlmali(String folderAlias) {
        if (!folder(folderAlias).get("is_active").getAsBoolean()) {
            throw new AssertionError("'" + folderAlias + "' aktif olmalıydı.");
        }
    }

    @When("{string} {string} klasörüne belge yüklemeyi dener")
    public void kullaniciKlasoreBelgeYuklemeyiDener(String userAlias, String folderAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        byte[] bytes = "QA pasif klasör yükleme".getBytes(StandardCharsets.UTF_8);
        List<Integer> content = new ArrayList<>();
        for (byte b : bytes) {
            content.add(b & 0xff);
        }
        String title = TestDataFactory.unique("QA Pasif Yükleme");
        body.put("title", title);
        body.put("metadata", Map.of());
        body.put("mime_type", "text/plain");
        body.put("original_filename", "qa-inactive.txt");
        body.put("folder_id", ApiTestContext.id(folderAlias));
        body.put("content", content);
        EcmApiClient.ApiResponse r = EcmApiClient.post(ApiTestContext.session(userAlias), "/v1/documents/unclassified", body);
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            TestDataFactory.registerDocument("beklenmeyen " + title, r.json().get("document_id").getAsString(), title);
        }
    }

    private static EcmApiClient.ApiResponse setFolderActive(EcmApiClient.Session s, String folderAlias, boolean active) {
        return EcmApiClient.put(s, "/v1/folders/" + ApiTestContext.id(folderAlias) + "/active", Map.of("is_active", active));
    }

    // ---------------------------------------------------------------------
    // Klasör içerik listeleri ve yükleme seçicisi (FLD-05 / FLD-08)
    // ---------------------------------------------------------------------

    @Then("{string} {string} klasörünün alt klasörlerinde {string} klasörünü görmemelidir")
    public void altKlasorlerdeGormemeli(String userAlias, String folderAlias, String childAlias) {
        if (listIds(userAlias, "/v1/folders/" + ApiTestContext.id(folderAlias) + "/children").contains(ApiTestContext.id(childAlias))) {
            throw new AssertionError("'" + userAlias + "' erişemediği '" + childAlias + "' alt klasörünü görmemeliydi.");
        }
    }

    @Then("{string} {string} klasörünün belge listesinde {string} belgesini görmelidir")
    public void klasorBelgeListesindeGormeli(String userAlias, String folderAlias, String documentAlias) {
        if (!listIds(userAlias, "/v1/folders/" + ApiTestContext.id(folderAlias) + "/documents").contains(ApiTestContext.id(documentAlias))) {
            throw new AssertionError("'" + userAlias + "' '" + folderAlias + "' belge listesinde '" + documentAlias + "' görmeliydi.");
        }
    }

    @Then("{string} yükleme klasör seçicisinde {string} klasörünü görmelidir")
    public void yuklemeSecicisindeGormeli(String userAlias, String folderAlias) {
        if (!pickerIds(userAlias).contains(ApiTestContext.id(folderAlias))) {
            throw new AssertionError("'" + userAlias + "' yazabildiği '" + folderAlias + "' klasörünü yükleme seçicisinde görmeliydi.");
        }
    }

    @Then("{string} yükleme klasör seçicisinde {string} klasörünü görmemelidir")
    public void yuklemeSecicisindeGormemeli(String userAlias, String folderAlias) {
        if (pickerIds(userAlias).contains(ApiTestContext.id(folderAlias))) {
            throw new AssertionError("'" + userAlias + "' yalnız okuyabildiği '" + folderAlias + "' klasörünü yükleme seçicisinde görmemeliydi.");
        }
    }

    /* Seçici admin olmayan kullanıcıda ~18 sn sürüyor (BUG-03); senaryoda bir kez çağrılır. */
    private List<String> pickerIds(String userAlias) {
        if (uploadPicker == null) {
            long t0 = System.nanoTime();
            uploadPicker = EcmApiClient.getSlow(ApiTestContext.session(userAlias), "/v1/folders/picker?for=upload")
                    .expect(200, "Yükleme klasör seçicisi").json();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("[FLD-08] GET /v1/folders/picker?for=upload süresi: " + ms + " ms (BUG-03 kanıtı)");
            Allure.addAttachment("FLD-08 picker duration", "text/plain", ms + " ms");
        }
        List<String> ids = new ArrayList<>();
        uploadPicker.getAsJsonArray("items").forEach(e -> ids.add(e.getAsJsonObject().get("id").getAsString()));
        return ids;
    }

    private static List<String> listIds(String userAlias, String path) {
        List<String> ids = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(ApiTestContext.session(userAlias), path).expect(200, path).json().getAsJsonArray("items")) {
            ids.add(e.getAsJsonObject().get("id").getAsString());
        }
        return ids;
    }

    // ---------------------------------------------------------------------
    // Sahiplik (OWN-02 / USER-05 / FLD-09 / ACL-14)
    // ---------------------------------------------------------------------

    @Given("yönetici {string} klasörünün sahipliğini {string} kullanıcısına devretmiştir")
    public void yoneticiSahiplikDevretmistir(String folderAlias, String userAlias) {
        transferOwner(folderAlias, userAlias).expect(200, "Sahiplik devri (" + folderAlias + " -> " + userAlias + ")");
    }

    @When("yönetici {string} klasörünün sahipliğini {string} kullanıcısına devretmeyi dener")
    public void yoneticiSahiplikDevretmeyiDener(String folderAlias, String userAlias) {
        ApiTestContext.lastResponse(transferOwner(folderAlias, userAlias));
    }

    @Then("{string} {string} klasöründe {string} kullanıcısına {string} rolü verebilmelidir")
    public void klasordeKullaniciyaRolVerebilmeli(String ownerAlias, String folderAlias, String userAlias, String aclRole) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.roleId(aclRole));
        body.put("user_id", ApiTestContext.id(userAlias));
        body.put("deny", false);
        EcmApiClient.post(ApiTestContext.session(ownerAlias), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl", body)
                .expect(200, "'" + ownerAlias + "' klasör yetkisi verme");
        String userId = ApiTestContext.id(userAlias);
        if (folderAcl(folderAlias).stream().noneMatch(a -> userId.equals(str(a, "user_id")))) {
            throw new AssertionError("Verilen yetki klasör ACL listesinde görünmüyor.");
        }
    }

    @Then("yönetici {string} klasörünü ve {string} belgesini okuyabilmelidir")
    public void yoneticiKlasorVeBelgeOkuyabilmeli(String folderAlias, String documentAlias) {
        folder(folderAlias);
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Yönetici belge okuma");
    }

    @When("{string} {string} klasörünün yetki devralmasını kapatmayı dener")
    public void yetkiDevralmasiniKapatmayiDener(String userAlias, String folderAlias) {
        ApiTestContext.lastResponse(EcmApiClient.patch(ApiTestContext.session(userAlias),
                "/v1/folders/" + ApiTestContext.id(folderAlias), Map.of("inherit_permissions", false)));
    }

    @When("yönetici {string} klasörünün yetki devralmasını kapatır")
    public void yoneticiYetkiDevralmasiniKapatir(String folderAlias) {
        EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias),
                Map.of("inherit_permissions", false)).expect(200, "Kalıtımı kapatma");
    }

    @Then("{string} klasörünün yetki devralması açık kalmalıdır")
    public void yetkiDevralmasiAcikKalmali(String folderAlias) {
        if (!folder(folderAlias).get("inherit_permissions").getAsBoolean()) {
            throw new AssertionError("'" + folderAlias + "' kalıtımı değişmemeliydi (açık kalmalıydı).");
        }
    }

    @Then("{string} klasöründe hiç izin satırı bulunmamalıdır")
    public void klasordeHicIzinSatiriBulunmamali(String folderAlias) {
        List<JsonObject> acl = folderAcl(folderAlias);
        if (!acl.isEmpty()) {
            throw new AssertionError("'" + folderAlias + "' ACL listesi boş olmalıydı: " + acl);
        }
    }

    /* RQ-19: ROLES.md "yalnız okuma tutamacı" der, motor yaratıcıya belge düzeyinde manager verir. */
    @Then("{string} kullanıcısının {string} belgesindeki yazma sonucu RQ-19 kanıtı olarak kaydedilmelidir")
    public void yaraticiYazmaSonucuKanit(String userAlias, String documentAlias) {
        EcmApiClient.ApiResponse nv = EcmApiClient.post(ApiTestContext.session(userAlias), "/v1/documents/unclassified",
                TestDataFactory.newVersionBody(documentAlias, "Patch", "QA RQ-19 " + System.nanoTime()));
        JsonObject docAcl = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/acl")
                .expect(200, "Belge ACL").json();
        String evidence = "RQ-19 (sahiplik devri + kalıtım kapalı sonrası yaratıcının yazma hakkı):\n"
                + "  yaratıcı yeni sürüm: HTTP " + nv.status() + "\n"
                + "  belge ACL (auto_grant_document_creator): " + docAcl + "\n"
                + "  ROLES.md: 'Management of that document passes to whoever now owns its folder.'\n";
        System.out.println("[RQ-19] " + evidence.replace("\n", " | "));
        Allure.addAttachment("RQ-19 evidence — creator write after chown", "text/plain", evidence);
    }

    /*
     * RQ-18: ROLES.md sahibin geri yükleyebileceğini, REST handler ve SPA yorumu kapsamlı
     * manager'ın geri yükleyebileceğini söyler; motor (admin_restore_document) yalnız admin'e
     * izin verir ve SPA silme onayı "yalnız yönetici geri alabilir" der. Sonuç kanıt olarak eklenir.
     */
    @Then("{string} kullanıcısının {string} belgesini geri yükleme sonucu RQ-18 kanıtı olarak kaydedilmelidir")
    public void geriYuklemeSonucuKanit(String userAlias, String documentAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(ApiTestContext.session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/restore", Map.of());
        boolean deleted = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma").json().getAsJsonObject("document").get("is_deleted").getAsBoolean();
        String evidence = "RQ-18 (klasör sahibinin geri yüklemesi):\n"
                + "  POST /restore: HTTP " + r.status() + " " + r.body() + "\n"
                + "  belge hâlâ silinmiş mi: " + deleted + "\n"
                + "  ROLES.md:78 sahip geri yükleyebilir; documents.rs restore_document yorumu kapsamlı manager;\n"
                + "  engine/documents/admin.rs admin_restore_document assert_actor_is_admin (yalnız admin).\n";
        System.out.println("[RQ-18] " + evidence.replace("\n", " | "));
        Allure.addAttachment("RQ-18 evidence — owner restore", "text/plain", evidence);
    }

    private static EcmApiClient.ApiResponse transferOwner(String folderAlias, String userAlias) {
        return EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/owner",
                Map.of("new_owner_id", ApiTestContext.id(userAlias)));
    }

    // ---------------------------------------------------------------------
    // Kullanıcı arama (ACL-17)
    // ---------------------------------------------------------------------

    @Then("{string} kullanıcı aramasında {string} e-postasının tamamıyla {int} almalıdır")
    public void kullaniciAramasindaTamEposta(String userAlias, String targetAlias, int expected) {
        lookup(userAlias, ApiTestContext.name(targetAlias), expected);
    }

    @Then("{string} kullanıcı aramasında {string} e-postasının ön ekiyle {int} almalıdır")
    public void kullaniciAramasindaOnEk(String userAlias, String targetAlias, int expected) {
        String email = ApiTestContext.name(targetAlias);
        lookup(userAlias, email.substring(0, email.indexOf('@')), expected);
    }

    @Then("{string} kullanıcı aramasında boş e-postayla {int} almalıdır")
    public void kullaniciAramasindaBos(String userAlias, int expected) {
        lookup(userAlias, "", expected);
    }

    private static void lookup(String userAlias, String email, int expected) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(ApiTestContext.session(userAlias),
                "/v1/users/lookup?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
        ApiTestContext.lastResponse(r);
        if (r.status() != expected) {
            throw new AssertionError("Kullanıcı araması HTTP " + expected + " dönmeliydi, " + r.status() + " " + r.body());
        }
        if (expected == 200 && !email.equalsIgnoreCase(r.json().get("email").getAsString())) {
            throw new AssertionError("Arama farklı kullanıcı döndürdü: " + r.body());
        }
    }

    // ---------------------------------------------------------------------
    // Grup silme (GRP-02) ve klasörsüz kalan belge (FLD-10)
    // ---------------------------------------------------------------------

    @When("yönetici {string} grubunu siler")
    public void yoneticiGrubuSiler(String groupAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias)));
    }

    @Then("grup silme reddedilmediyse {string} klasöründe {string} grubuna ait izin satırı kalmamalıdır")
    public void grupSilindiyseYetimAclKalmamali(String folderAlias, String groupAlias) {
        int status = ApiTestContext.lastResponse().status();
        String groupId = ApiTestContext.id(groupAlias);
        boolean rowExists = folderAcl(folderAlias).stream().anyMatch(a -> groupId.equals(str(a, "group_id")));
        if (status == 204 || status == 200) {
            if (rowExists) {
                throw new AssertionError("Grup silindi ama klasörde yetim ACL satırı kaldı (group_id=" + groupId + ").");
            }
            int g = EcmApiClient.get(TestDataFactory.admin(), "/v1/groups/" + groupId).status();
            if (g != 404) {
                throw new AssertionError("Silinen grup için GET 404 olmalıydı, HTTP " + g);
            }
        } else if (status == 409) {
            if (!rowExists) {
                throw new AssertionError("Grup silme 409 ile reddedildi ama ACL satırı kaybolmuş.");
            }
        } else {
            throw new AssertionError("Grup silme 204 (cascade) ya da 409 (reddet) dönmeliydi, HTTP " + status + " "
                    + ApiTestContext.lastResponse().body());
        }
    }

    @Then("{string} belgesinin etkin erişim raporunda {string} grubu görünmemelidir")
    public void etkinErisimdeGrupGorunmemeli(String documentAlias, String groupAlias) {
        String body = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/effective-access")
                .expect(200, "Etkin erişim").body();
        if (body.contains(ApiTestContext.id(groupAlias))) {
            throw new AssertionError("Silinen grup etkin erişim raporunda hâlâ görünüyor: " + groupAlias);
        }
    }

    @When("yönetici {string} belgesini geri yükler")
    public void yoneticiBelgeyiGeriYukler(String documentAlias) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/restore", Map.of())
                .expect(200, "Yönetici geri yükleme");
    }

    @Then("{string} belgesi geçerli bir klasörde bulunmalıdır")
    public void belgeGecerliKlasordeBulunmali(String documentAlias) {
        JsonObject doc = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma").json();
        JsonElement folder = doc.get("folder");
        if (folder == null || folder.isJsonNull()) {
            throw new AssertionError("'" + documentAlias + "' geri yüklendikten sonra klasörsüz kaldı (folder=null). "
                    + "Klasörsüz belgeleri yalnız admin yönetebilir (runtime.rs can_manage_document_scoped); "
                    + "klasördeki yetkili kullanıcılar erişimi kaybeder.");
        }
        String folderId = folder.getAsJsonObject().get("id").getAsString();
        int fs = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + folderId).status();
        if (fs != 200) {
            throw new AssertionError("'" + documentAlias + "' var olmayan bir klasöre bağlı: " + folderId + " (HTTP " + fs + ")");
        }
    }

    // ---------------------------------------------------------------------
    // Sınıf kısıtlı belge ve etkin erişim kartı (ACL-15)
    // ---------------------------------------------------------------------

    @Given("yönetici erişimi {string} kullanıcısıyla sınırlı {string} sınıfını oluşturmuştur")
    public void yoneticiErisimiSinirliSinifOlusturmustur(String userAlias, String classAlias) {
        JsonObject created = EcmApiClient.post(TestDataFactory.admin(), "/v1/classes",
                        Map.of("name", TestDataFactory.unique("QA Kısıtlı Sınıf"), "description", "QA ACL-15"))
                .expect(200, "Sınıf oluşturma").json();
        String classId = created.get("id").getAsString();
        ApiTestContext.putId(classAlias, classId);
        ApiTestContext.putName(classAlias + "#version", created.get("version").getAsString());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.ROLE_VIEWER);
        body.put("user_id", ApiTestContext.id(userAlias));
        body.put("deny", false);
        EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + classId + "/acl", body)
                .expect(200, "Sınıf ACL (" + userAlias + ")");
        ApiTestContext.onCleanup(() -> {
            long version = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + classId)
                    .expect(200, "Sınıf okuma (temizlik)").json().get("version").getAsLong();
            EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + classId + "/active",
                    Map.of("expected_version", version, "is_active", false));
        });
    }

    @Given("yönetici {string} sınıfıyla {string} klasörüne {string} belgesini yüklemiştir")
    public void yoneticiSinifliBelgeYuklemistir(String classAlias, String folderAlias, String documentAlias) {
        String title = TestDataFactory.unique("QA Sınıflı Belge");
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents",
                TestDataFactory.classifiedDocumentBody(title, ApiTestContext.id(classAlias),
                        Long.parseLong(ApiTestContext.name(classAlias + "#version")), Map.of(), folderAlias));
        r.expect(201, "Sınıflı belge yükleme");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
    }

    @Then("{string} belgesinin etkin erişim raporu sınıf iznini yalnız {string} için göstermelidir")
    public void etkinErisimSinifIzniYalniz(String documentAlias, String userAlias) {
        JsonObject report = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/effective-access")
                .expect(200, "Etkin erişim").json();
        List<String> classPrincipals = new ArrayList<>();
        for (JsonElement e : report.getAsJsonArray("items")) {
            JsonObject g = e.getAsJsonObject();
            if ("Class ACL".equals(g.get("source").getAsString())) {
                classPrincipals.add(g.get("principal").getAsString());
            }
        }
        String allowed = ApiTestContext.id(userAlias);
        if (classPrincipals.isEmpty() || classPrincipals.stream().anyMatch(p -> !p.contains(allowed))) {
            throw new AssertionError("Etkin erişim raporundaki 'Class ACL' satırları yalnız '" + userAlias
                    + "' için olmalıydı: " + classPrincipals);
        }
    }

    @Then("yönetici tarayıcıda {string} belgesinin etkin erişim kartında sınıf kaynağını görmelidir")
    public void tarayicidaEtkinErisimKartiSinifKaynagi(String documentAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        Response resp = page.waitForResponse(
                r -> r.url().endsWith("/v1/documents/" + ApiTestContext.id(documentAlias) + "/effective-access"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias))
        );
        if (resp.status() != 200) {
            throw new AssertionError("Etkin erişim kartı isteği HTTP " + resp.status());
        }
        try {
            page.getByText("Effective access").first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.getByText("Class ACL").first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Effective access kartında 'Class ACL' kaynağı görünmedi.", e);
        }
    }

    // ---------------------------------------------------------------------
    // Klasör sahibinin arayüzden e-posta ile paylaşımı (ACL-10)
    // ---------------------------------------------------------------------

    @When("{string} tarayıcıda {string} klasörünün yetki kartından {string} kullanıcısına e-posta ile okuma yetkisi verir")
    public void tarayicidaEpostaIleOkumaYetkisiVerir(String ownerAlias, String folderAlias, String userAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(ownerAlias), TestDataFactory.TEST_USER_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/folders/" + ApiTestContext.id(folderAlias));
        Locator addGrant = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add grant"));
        try {
            addGrant.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Klasör sahibi için yetki kartı (Add grant) görünmedi. URL: " + page.url(), e);
        }
        Locator form = page.locator("form").filter(new Locator.FilterOptions().setHas(addGrant));
        Locator selects = form.locator("select");
        page.waitForCondition(() -> selects.first().locator("option").count() > 1,
                new Page.WaitForConditionOptions().setTimeout(UI_WAIT_MS));
        selects.nth(0).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("Read only"));
        selects.nth(1).selectOption("user");
        form.locator("input[type='email']").fill(ApiTestContext.name(userAlias));
        form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Find")).click();
        form.getByText(ApiTestContext.name(userAlias)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        Response grant = page.waitForResponse(
                r -> "POST".equals(r.request().method()) && r.url().endsWith("/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                addGrant::click
        );
        if (grant.status() != 200) {
            throw new AssertionError("Arayüzden yetki verme HTTP " + grant.status() + " döndü: " + grant.text());
        }
        aclIds.put(folderAlias + "|" + userAlias + "|ui", JsonParserHolder.id(grant.text()));
        page.getByTitle("Revoke").first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    @When("{string} tarayıcıda {string} klasörünün yetki kartından {string} satırını kaldırır")
    public void tarayicidaYetkiSatiriniKaldirir(String ownerAlias, String folderAlias, String userAlias) {
        Page page = BrowserManager.page;
        String aclId = aclIds.get(folderAlias + "|" + userAlias + "|ui");
        Response revoke = page.waitForResponse(
                r -> "DELETE".equals(r.request().method()) && r.url().endsWith("/acl/" + aclId),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByTitle("Revoke").first().click()
        );
        if (revoke.status() != 200) {
            throw new AssertionError("Arayüzden yetki kaldırma HTTP " + revoke.status());
        }
    }

    @Then("{string} klasörünün yetki listesinde {string} için okuma satırı bulunmalıdır")
    public void yetkiListesindeOkumaSatiri(String folderAlias, String userAlias) {
        String userId = ApiTestContext.id(userAlias);
        boolean found = folderAcl(folderAlias).stream().anyMatch(a -> userId.equals(str(a, "user_id"))
                && TestDataFactory.ROLE_VIEWER.equals(a.get("role_id").getAsString()) && !a.get("deny").getAsBoolean());
        if (!found) {
            throw new AssertionError("'" + userAlias + "' için viewer allow satırı klasör ACL'inde yok.");
        }
    }

    // ---------------------------------------------------------------------

    private static JsonObject folder(String folderAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias))
                .expect(200, "Klasör okuma").json().getAsJsonObject("folder");
    }

    private static List<JsonObject> folderAcl(String folderAlias) {
        List<JsonObject> out = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl")
                .expect(200, "Klasör ACL").json().getAsJsonArray("items").forEach(e -> out.add(e.getAsJsonObject()));
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static final class JsonParserHolder {
        static String id(String json) {
            return com.google.gson.JsonParser.parseString(json).getAsJsonObject().get("id").getAsString();
        }
    }
}
