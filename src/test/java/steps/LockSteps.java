package steps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import pages.DocumentsPage;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/*
 * Batch 26 — checkout kilidi, çok kullanıcılı ve bayat sayfa durumları.
 * ECM kaynakları: engine/documents/locks.rs (checkout/checkin/force release,
 * ensure_not_locked_by_other), hooks/useAutoCheckout.ts (yeni sürüm sayfası),
 * ROLES.md:18 (copy/restore/hard delete kilitten muaf).
 */
public class LockSteps {

    private static final int UI_WAIT_MS = 20000;

    private final Map<String, String> lockTokens = new HashMap<>();
    private Page userPage;
    private int lastUiStatus = -1;
    private String lastUiError = "";

    // ---------------------------------------------------------------------
    // API: checkout / checkin / force release
    // ---------------------------------------------------------------------

    @Given("{string} {string} belgesini checkout yapmıştır")
    public void belgeyiCheckoutYapmistir(String userAlias, String documentAlias) {
        checkout(userAlias, documentAlias, null);
    }

    @When("{string} {string} belgesini {int} saniyelik kilitle checkout yapar")
    public void belgeyiSureliCheckoutYapar(String userAlias, String documentAlias, int seconds) {
        checkout(userAlias, documentAlias, seconds);
    }

    @Then("{string} belgesinin kilit süresi dolana kadar beklendiğinde kilit kalkmalıdır")
    public void kilitSuresiDolanaKadarBeklenir(String documentAlias) {
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            if (!lock(documentAlias).get("locked").getAsBoolean()) {
                return;
            }
            pollPause();
        }
        throw new AssertionError("Süreli kilit expires_at geçtikten sonra da kalkmadı: " + lock(documentAlias));
    }

    @Then("{string} belgesinin kilidi {string} kullanıcısında olmalıdır")
    public void kilitKullanicidaOlmali(String documentAlias, String userAlias) {
        JsonObject l = lock(documentAlias);
        String expected = userAlias.equals("yönetici") ? TestDataFactory.admin().userId : ApiTestContext.id(userAlias);
        if (!l.get("locked").getAsBoolean() || !expected.equals(l.get("locked_by").getAsString())) {
            throw new AssertionError("'" + documentAlias + "' kilidi '" + userAlias + "' kullanıcısında olmalıydı: " + l);
        }
    }

    @When("{string} {string} belgesinde yanlış kilit anahtarıyla check-in yapmayı dener")
    public void yanlisAnahtarlaCheckin(String userAlias, String documentAlias) {
        ApiTestContext.lastResponse(checkin(userAlias, documentAlias, Map.of("lock_token", "qa-yanlis-anahtar")));
    }

    @When("{string} {string} belgesinde doğru kilit anahtarıyla check-in yapmayı dener")
    public void dogruAnahtarlaCheckin(String userAlias, String documentAlias) {
        ApiTestContext.lastResponse(checkin(userAlias, documentAlias, Map.of("lock_token", lockTokens.get(documentAlias))));
    }

    @When("{string} {string} belgesinde kilit anahtarı olmadan check-in yapmayı dener")
    public void anahtarsizCheckin(String userAlias, String documentAlias) {
        ApiTestContext.lastResponse(checkin(userAlias, documentAlias, Map.of()));
    }

    @When("{string} {string} belgesinin kilidini zorla kaldırmayı dener")
    public void kilidiZorlaKaldirmayiDener(String userAlias, String documentAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/lock"));
    }

    @Then("{string} belgesi kilitli olmalıdır")
    public void belgeKilitliOlmali(String documentAlias) {
        if (!lock(documentAlias).get("locked").getAsBoolean()) {
            throw new AssertionError("'" + documentAlias + "' kilitli olmalıydı.");
        }
    }

    @Then("{string} belgesinin kilidi kalkmış olmalıdır")
    public void kilidiKalkmisOlmali(String documentAlias) {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (!lock(documentAlias).get("locked").getAsBoolean()) {
                return;
            }
            pollPause();
        }
        throw new AssertionError("'" + documentAlias + "' kilidi kalkmalıydı: " + lock(documentAlias));
    }

    // ---------------------------------------------------------------------
    // Silinmiş + kilitli belge (LOCK-05 / LOCK-06; kurulum bilinen BUG-06 yolunu kullanır)
    // ---------------------------------------------------------------------

    @When("{string} soft delete edilmiş {string} belgesini checkout yapmayı dener")
    public void silinmisBelgeyiCheckoutYapmayiDener(String userAlias, String documentAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/checkout", Map.of());
        ApiTestContext.putName(documentAlias + "#deletedCheckout", String.valueOf(r.status()));
        String note = "Silinmiş belgede checkout: HTTP " + r.status()
                + (r.status() == 200 ? " (BUG-06 hâlâ mevcut: silinmiş belge kilitlenebiliyor)" : " (silinmiş belge kilitlenemedi)");
        System.out.println("[LOCK] " + note);
        Allure.addAttachment("Checkout on deleted document", "text/plain", note);
    }

    @Then("yönetici arayüzde silinmiş {string} belgesindeki Restore ve Hard delete butonlarının durumu RQ-11 kanıtı olarak kaydedilmelidir")
    public void restoreHardDeleteButonDurumuKanit(String documentAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        Locator restore = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore").setExact(true));
        Locator hard = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Hard delete…"));
        try {
            restore.first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Silinmiş belgede Restore butonu görünmedi.", e);
        }
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        boolean locked = lock(documentAlias).get("locked").getAsBoolean();
        String evidence = "RQ-11 (kilitten muaf işlemlerin arayüz durumu):\n"
                + "  belge kilitli (başka kullanıcı): " + locked + "\n"
                + "  Restore butonu etkin: " + restore.first().isEnabled() + "\n"
                + "  Hard delete butonu etkin: " + (hard.count() > 0 && hard.first().isEnabled()) + "\n"
                + "  ROLES.md:18 restore ve hard delete kilitten muaftır; DocumentDetail.tsx disabled={destructive || lockedByOther}.\n";
        System.out.println("[RQ-11] " + evidence.replace("\n", " | "));
        Allure.addAttachment("RQ-11 evidence — exempt actions on locked document", "text/plain", evidence);
    }

    @When("yönetici {string} belgesini geri yüklemeyi dener")
    public void yoneticiGeriYuklemeyiDener(String documentAlias) {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/restore", Map.of()));
    }

    @When("yönetici kilitli {string} belgesini kalıcı olarak siler")
    public void yoneticiKilitliBelgeyiKaliciSiler(String documentAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/hard"));
    }

    @Then("{string} belgesi artık bulunmamalıdır")
    public void belgeArtikBulunmamali(String documentAlias) {
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias)).status();
        if (s != 404) {
            throw new AssertionError("Kalıcı silinen belge için GET 404 olmalıydı, HTTP " + s);
        }
    }

    // ---------------------------------------------------------------------
    // Arayüz: açık sayfa + başka kullanıcının değişikliği (ACL-16, DOC-17, LOCK-04, VER-08)
    // ---------------------------------------------------------------------

    @Given("{string} tarayıcıda {string} belgesinin detay sayfasını açmıştır")
    public void tarayicidaDetaySayfasiniAcmistir(String userAlias, String documentAlias) {
        BrowserManager.startBrowser();
        if (userAlias.equals("yönetici")) {
            BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        } else {
            BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(userAlias), TestDataFactory.TEST_USER_PASSWORD);
        }
        userPage = BrowserManager.page;
        userPage.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        try {
            userPage.getByText(ApiTestContext.name(documentAlias)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            userPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Belge detay sayfası yüklenmedi. URL: " + userPage.url(), e);
        }
        // NETWORKIDLE yerine kilit durumunun render edilmesi beklenir: admin olmayan kullanıcıda
        // breadcrumb'ın klasör seçici isteği yük altında 30 sn'yi aşıyor (BUG-03) ve ağ hiç boşa çıkmıyordu.
        userPage.getByText(java.util.regex.Pattern.compile("^(Not checked out\\.|Checked out by .+)$")).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    @When("kullanıcı açık sayfada belgeye etiket eklemeyi dener")
    public void acikSayfadaEtiketEklemeyiDener() {
        Locator input = userPage.locator("input[placeholder^='Type to add a tag']");
        input.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        input.fill("qa-bayat-" + System.nanoTime());
        input.press("Enter");
        Response r = userPage.waitForResponse(
                resp -> "POST".equals(resp.request().method()) && resp.url().matches(".*/v1/documents/[0-9a-f-]{36}/tags$"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> userPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add tags").setExact(true)).click()
        );
        remember(r);
    }

    @When("kullanıcı açık sayfada belgeyi yeniden adlandırmayı dener")
    public void acikSayfadaYenidenAdlandirmayiDener() {
        DocumentsPage documents = new DocumentsPage(userPage);
        documents.renameEditorunuAc();
        documents.renameBasligiGir("QA bayat sayfa adı " + System.nanoTime());
        Response r = userPage.waitForResponse(
                resp -> "PATCH".equals(resp.request().method()) && resp.url().endsWith("/title"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                documents::renameKaydet
        );
        remember(r);
    }

    @Then("açık sayfadaki işlem {int} ile reddedilmeli ve hata mesajı arayüzde görünmelidir")
    public void acikSayfadakiIslemReddedilmeli(int expected) {
        if (lastUiStatus != expected) {
            throw new AssertionError("Arayüz işlemi HTTP " + expected + " ile reddedilmeliydi, " + lastUiStatus + " döndü: " + lastUiError);
        }
        String fragment = lastUiError.length() > 40 ? lastUiError.substring(0, 40) : lastUiError;
        try {
            userPage.getByText(fragment).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Reddedilen işlemin hata mesajı arayüzde görünmedi: '" + fragment + "'", e);
        }
    }

    @Then("açık sayfadaki işlem bir istemci hatasıyla reddedilmeli ve hata mesajı arayüzde görünmelidir")
    public void acikSayfadakiIslem4xxReddedilmeli() {
        if (lastUiStatus < 400 || lastUiStatus >= 500) {
            throw new AssertionError("Arayüz işlemi 4xx ile reddedilmeliydi, HTTP " + lastUiStatus + " döndü: " + lastUiError);
        }
        acikSayfadakiIslemReddedilmeli(lastUiStatus);
    }

    /* BUG-10 önkoşulu: klasör rolü düzenlemeye izin veriyorsa etiket girişi (TagManager canWrite) görünür. */
    @Then("kullanıcı açık sayfada belgeyi düzenleyebilmelidir")
    public void acikSayfadaDuzenleyebilmeli() {
        Locator input = userPage.locator("input[placeholder^='Type to add a tag']");
        try {
            input.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Kullanıcının klasör rolü belge düzenlemeye izin vermeli (etiket girişi görünmeli) "
                    + "ama düzenleme kontrolleri gösterilmedi. URL: " + userPage.url(), e);
        }
    }

    @Then("kullanıcı sayfayı yenilediğinde belgeyi görüntüleme izni olmadığı mesajını görmelidir")
    public void yenilemedeIzinYokMesaji() {
        userPage.reload();
        try {
            userPage.getByText("You do not have permission to view this document").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Yenilemeden sonra erişim reddi mesajı görünmedi. URL: " + userPage.url(), e);
        }
    }

    @Then("kullanıcı açık sayfada {string} sürümünü görmelidir")
    public void acikSayfadaSurumuGormeli(String label) {
        try {
            userPage.getByText(label).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Sayfada " + label + " görünmedi.", e);
        }
    }

    @When("yönetici {string} belgesine {string} içerikli minor sürüm ekler")
    public void yoneticiMinorSurumEkler(String documentAlias, String text) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                TestDataFactory.newVersionBody(documentAlias, "Minor", text)).expect(201, "Minor sürüm");
    }

    @Then("kullanıcı sayfayı yenileyince {string} sürümünü görmeli ve indirdiğinde {string} almalıdır")
    public void yenileyinceSurumVeIndirme(String label, String expectedContent) throws Exception {
        userPage.reload();
        acikSayfadaSurumuGormeli(label);
        Download dl = userPage.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(UI_WAIT_MS),
                () -> userPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true)).first().click());
        String got = Files.readString(dl.path());
        if (!expectedContent.equals(got)) {
            throw new AssertionError("İndirilen içerik yeni sürüm içeriği olmalıydı: '" + expectedContent + "', gelen: '" + got + "'");
        }
    }

    // ---------------------------------------------------------------------
    // Yeni sürüm sayfası otomatik kilidi (LOCK-07)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} belgesinin yeni sürüm sayfasını açar")
    public void yoneticiYeniSurumSayfasiniAcar(String documentAlias) {
        tarayicidaDetaySayfasiniAcmistir("yönetici", documentAlias);
        new DocumentsPage(userPage).yeniVersiyonSayfasiniAc();
        long deadline = System.currentTimeMillis() + UI_WAIT_MS;
        while (System.currentTimeMillis() < deadline && !lock(documentAlias).get("locked").getAsBoolean()) {
            pollPause();
        }
    }

    @When("yönetici yeni sürüm sayfasından Cancel ile ayrılır")
    public void yoneticiYeniSurumSayfasindanAyrilir() {
        userPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel").setExact(true)).first().click();
        userPage.waitForURL(url -> !url.contains("/documents/new"), new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
    }

    // ---------------------------------------------------------------------

    private void checkout(String userAlias, String documentAlias, Integer seconds) {
        Map<String, Object> body = seconds == null ? Map.of() : Map.of("expires_in_secs", seconds);
        JsonObject lock = EcmApiClient.post(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/checkout", body)
                .expect(200, "Checkout (" + userAlias + ")").json();
        lockTokens.put(documentAlias, lock.get("lock_token").getAsString());
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/lock"));
    }

    private static EcmApiClient.ApiResponse checkin(String userAlias, String documentAlias, Map<String, Object> body) {
        return EcmApiClient.post(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/checkin", body);
    }

    static JsonObject lock(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/lock")
                .expect(200, "Kilit durumu").json();
    }

    private void remember(Response r) {
        lastUiStatus = r.status();
        try {
            lastUiError = JsonParser.parseString(r.text()).getAsJsonObject().getAsJsonObject("error").get("message").getAsString();
        } catch (RuntimeException e) {
            lastUiError = "";
        }
    }

    /* Koşul tabanlı yoklama aralığı (sabit bekleme değil; çağıran döngü zaman aşımıyla sınırlı). */
    private static void pollPause() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
