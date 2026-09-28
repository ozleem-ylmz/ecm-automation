package steps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * AUTH-01: pasif hesap. ECM üç noktada is_active kontrol eder:
 * login (auth.rs:281), refresh (auth.rs:470) ve her istekte require_auth
 * (auth.rs:591). Üçü de AccessDenied -> 403 döndürür.
 */
public class AuthSessionSteps {

    private static final String DEACTIVATED = "This account has been deactivated";

    @Given("{string} kendi profil bilgisini alabilmelidir")
    public void kendiProfilBilgisiniAlabilmelidir(String alias) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        String email = EcmApiClient.get(s, "/v1/auth/me")
                .expect(200, "Profil (" + alias + ")").json()
                .get("email").getAsString();
        if (!email.equalsIgnoreCase(s.email)) {
            throw new AssertionError("Profil e-postası '" + s.email + "' olmalıydı, '" + email + "' geldi.");
        }
    }

    @When("yönetici {string} hesabını pasifleştirir")
    public void yoneticiHesabiPasiflestirir(String alias) {
        setActive(alias, false);
    }

    @When("yönetici {string} hesabını tekrar aktifleştirir")
    public void yoneticiHesabiTekrarAktiflestirir(String alias) {
        setActive(alias, true);
    }

    @Then("{string} mevcut access token ile profil isteğinde pasif hesap hatası almalıdır")
    public void mevcutTokenIleProfilIsteginde403(String alias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(ApiTestContext.session(alias), "/v1/auth/me");
        assertDeactivated(r, "Açık oturumla /auth/me");
    }

    @Then("{string} refresh token ile oturumunu yenileyememelidir")
    public void refreshTokenIleOturumunuYenileyememelidir(String alias) {
        EcmApiClient.ApiResponse r = EcmApiClient.refresh(ApiTestContext.session(alias));
        assertDeactivated(r, "Refresh");
    }

    @Then("{string} yeniden giriş yapmaya çalıştığında pasif hesap hatası almalıdır")
    public void yenidenGirisPasifHesapHatasi(String alias) {
        EcmApiClient.ApiResponse r = EcmApiClient.rawLogin(ApiTestContext.name(alias), TestDataFactory.TEST_USER_PASSWORD);
        assertDeactivated(r, "Login");
    }

    @Then("{string} yeniden giriş yapabilmelidir")
    public void yenidenGirisYapabilmelidir(String alias) {
        EcmApiClient.Session s = TestDataFactory.loginAs(alias);
        EcmApiClient.get(s, "/v1/auth/me").expect(200, "Yeniden aktifleştirilen kullanıcının profili");
    }

    // =====================================================================
    // Batch 21 — Auth / session
    //
    // Token değerleri hiçbir zaman loglanmaz, assertion mesajına ya da
    // Allure'a yazılmaz; yalnızca "var/yok/farklı" olarak karşılaştırılır.
    // =====================================================================

    private static final int UI_WAIT_MS = 20000;
    private static final Pattern LOGIN_URL = Pattern.compile(".*/login(\\?.*)?$");
    private static final Pattern TAGS_PATH = Pattern.compile("^/v1/documents/[0-9a-fA-F-]{36}/tags$");

    /* Senaryo içinde "önceki" refresh token (rotasyon / logout / parola değişimi öncesi). */
    private String staleRefreshToken;
    private EcmApiClient.ApiResponse lastLogout;
    private int uiLogoutStatus = -1;
    private String uiRefreshBeforeLogout;
    private int uiRegisterStatus = -1;
    private Page secondTab;

    private final Map<String, String> tagNames = new HashMap<>();
    private final AtomicBoolean invalidateNextTagPost = new AtomicBoolean(false);
    private final List<String> tabEvents = Collections.synchronizedList(new ArrayList<>());
    private final List<String> secondTabEvents = Collections.synchronizedList(new ArrayList<>());

    // ---------------- AUTH-02: refresh token tek kullanımlıktır ----------------

    @When("{string} refresh token ile oturumunu yeniler ve yeni bir refresh token alır")
    public void refreshIleYenilerVeYeniTokenAlir(String alias) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        staleRefreshToken = s.refreshToken();
        EcmApiClient.ApiResponse r = EcmApiClient.refresh(s).expect(200, "İlk refresh (" + alias + ")");
        String rotated = EcmApiClient.refreshCookieValue(r).orElseThrow(
                () -> new AssertionError("Başarılı refresh yanıtında yeni refresh_token cookie'si yok."));
        if (rotated.equals(staleRefreshToken)) {
            throw new AssertionError("Refresh token rotate edilmedi: yanıt aynı refresh token'ı geri verdi.");
        }
    }

    @Then("{string} kullanılmış eski refresh token ile oturum yenileyememelidir")
    public void kullanilmisEskiTokenIleYenileyememelidir(String alias) {
        assertRefreshRejected(staleRefreshToken, "Kullanılmış (rotate edilmiş) refresh token ile ikinci refresh");
    }

    @Then("{string} yeni refresh token ile oturumunu yenileyebilmeli ve profilini alabilmelidir")
    public void yeniTokenIleYenileyebilmeli(String alias) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        EcmApiClient.refresh(s).expect(200, "Rotate edilen yeni refresh token ile refresh (" + alias + ")");
        kendiProfilBilgisiniAlabilmelidir(alias);
    }

    // ---------------- AUTH-03 / AUTH-12: arayüzden çıkış ----------------

    @Given("{string} tarayıcıda oturum açmış olarak {string} sayfasını açar")
    public void tarayicidaOturumAcmisOlarakSayfayiAcar(String alias, String path) {
        openUiSession(alias);
        BrowserManager.page.navigate(BrowserManager.getBaseUrl() + path);
        waitAppShell(BrowserManager.page);
    }

    @When("kullanıcı arayüzde kullanıcı menüsünden çıkış yapar")
    public void kullaniciMenusundenCikisYapar() {
        Page page = BrowserManager.page;
        uiRefreshBeforeLogout = browserRefreshCookie().orElseThrow(
                () -> new AssertionError("Çıkıştan önce tarayıcıda refresh_token cookie'si yok; oturum kurulmamış."));
        page.locator("button[aria-haspopup='menu']").last().click();
        Response logout = page.waitForResponse(
                r -> "POST".equals(r.request().method()) && r.url().endsWith("/v1/auth/logout"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Sign out")).click()
        );
        uiLogoutStatus = logout.status();
    }

    @Then("çıkış isteği sunucuda başarıyla tamamlanmalı ve login sayfası görüntülenmelidir")
    public void cikisIstegiBasariliVeLoginGoruntulenmeli() {
        if (uiLogoutStatus != 204) {
            throw new AssertionError("POST /v1/auth/logout 204 dönmeliydi, " + uiLogoutStatus + " döndü.");
        }
        waitLogin(BrowserManager.page, "Çıkış sonrası");
    }

    @Then("tarayıcıda refresh token cookie'si kalmamalıdır")
    public void tarayicidaRefreshCookieKalmamali() {
        if (browserRefreshCookie().isPresent()) {
            throw new AssertionError("Çıkış sonrası tarayıcıda hâlâ dolu bir refresh_token cookie'si var.");
        }
    }

    @Then("tarayıcıda {string} adresine gidildiğinde login sayfasına yönlendirilmeli ve veri gösterilmemelidir")
    public void adreseGidildigindeLogineYonlendirilmeli(String path) {
        assertRedirectsToLoginWithoutData(BrowserManager.page, BrowserManager.getBaseUrl() + path, List.of());
    }

    @Then("çıkıştan önceki refresh token ile oturum yenilenememelidir")
    public void cikistanOncekiTokenIleYenilenememeli() {
        assertRefreshRejected(uiRefreshBeforeLogout, "Çıkıştan önceki (iptal edilmiş) refresh token ile refresh");
    }

    // ---------------- AUTH-04 / AUTH-05: parola değişimi ----------------

    @When("yönetici {string} hesabının parolasını {string} olarak değiştirir")
    public void yoneticiParolayiDegistirir(String alias, String newPassword) {
        staleRefreshToken = ApiTestContext.session(alias).refreshToken();
        EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias),
                        Map.of("password", newPassword))
                .expect(200, "Yöneticinin parola değiştirmesi (" + alias + ")");
    }

    @Then("{string} eski parolasıyla giriş yapamamalıdır")
    public void eskiParolaylaGirisYapamamali(String alias) {
        EcmApiClient.ApiResponse r = EcmApiClient.rawLogin(ApiTestContext.name(alias), TestDataFactory.TEST_USER_PASSWORD);
        ApiTestContext.lastResponse(r);
        if (r.status() != 401 || r.body().contains("\"token\"")) {
            throw new AssertionError("Eski parola ile giriş HTTP 401 ile reddedilmeliydi. Gelen: HTTP "
                    + r.status() + " " + EcmApiClient.redact(r.body()));
        }
    }

    @Then("{string} {string} parolasıyla giriş yapabilmelidir")
    public void yeniParolaylaGirisYapabilmeli(String alias, String password) {
        EcmApiClient.Session s = EcmApiClient.login(ApiTestContext.name(alias), password);
        ApiTestContext.putSession(alias, s);
        kendiProfilBilgisiniAlabilmelidir(alias);
    }

    @Then("{string} parola değişikliğinden önceki refresh token ile oturum yenileyememelidir")
    public void paroladanOncekiTokenIleYenileyememeli(String alias) {
        assertRefreshRejected(staleRefreshToken, "Parola değişikliğinden önce verilmiş refresh token ile refresh");
    }

    @When("{string} mevcut parolasını göndermeden kendi parolasını {string} yapmaya çalışır")
    public void mevcutParolasizKendiParolasiniDegistirmeyeCalisir(String alias, String newPassword) {
        selfPasswordChange(alias, newPassword, null);
    }

    @When("{string} yanlış mevcut parolayla kendi parolasını {string} yapmaya çalışır")
    public void yanlisMevcutParolaylaDegistirmeyeCalisir(String alias, String newPassword) {
        selfPasswordChange(alias, newPassword, "Yanlis!Parola9");
    }

    @Then("{string} reddedilen denemelerden sonra eski parolasıyla giriş yapabilmelidir")
    public void reddedilenDenemelerdenSonraEskiParolaGecerli(String alias) {
        EcmApiClient.login(ApiTestContext.name(alias), TestDataFactory.TEST_USER_PASSWORD);
    }

    @When("{string} doğru mevcut parolayla kendi parolasını {string} yapar")
    public void dogruMevcutParolaylaDegistirir(String alias, String newPassword) {
        selfPasswordChange(alias, newPassword, TestDataFactory.TEST_USER_PASSWORD)
                .expect(200, "Doğru current_password ile kendi parolasını değiştirme (" + alias + ")");
    }

    private static EcmApiClient.ApiResponse selfPasswordChange(String alias, String newPassword, String current) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("password", newPassword);
        if (current != null) {
            body.put("current_password", current);
        }
        EcmApiClient.ApiResponse r = EcmApiClient.put(s, "/v1/users/" + s.userId, body);
        ApiTestContext.lastResponse(r);
        return r;
    }

    // ---------------- AUTH-07: kayıt ----------------

    @When("anonim ziyaretçi arayüzdeki kayıt formunu {string} için benzersiz bir e-posta ile doldurup gönderir")
    public void arayuzKayitFormunuGonderir(String alias) {
        String email = TestDataFactory.uniqueEmail();
        BrowserManager.startBrowser();
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/signup");
        page.locator("input[type='email']").waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        page.locator("input[type='text']").first().fill("QA Auto " + alias);
        page.locator("input[type='email']").fill(email);
        Locator passwords = page.locator("input[type='password']");
        passwords.nth(0).fill(TestDataFactory.TEST_USER_PASSWORD);
        passwords.nth(1).fill(TestDataFactory.TEST_USER_PASSWORD);
        Response register = page.waitForResponse(
                r -> "POST".equals(r.request().method()) && r.url().endsWith("/v1/auth/register"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> page.locator("button[type='submit']").click()
        );
        uiRegisterStatus = register.status();
        ApiTestContext.putName(alias, email);
        if (uiRegisterStatus == 200) {
            String userId = JsonParser.parseString(register.text()).getAsJsonObject()
                    .getAsJsonObject("user").get("id").getAsString();
            registerCreatedAccount(alias, userId);
        }
    }

    @Then("kayıt sonrası uygulamaya oturum açmış olarak yönlendirilmelidir")
    public void kayitSonrasiOturumAcmisOlarakYonlendirilmeli() {
        if (uiRegisterStatus != 200) {
            throw new AssertionError("POST /v1/auth/register 200 dönmeliydi, " + uiRegisterStatus + " döndü. URL: "
                    + BrowserManager.page.url());
        }
        BrowserManager.page.waitForURL("**/classes", new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
        waitAppShell(BrowserManager.page);
        if (browserRefreshCookie().isEmpty()) {
            throw new AssertionError("Kayıt sonrası tarayıcıya refresh_token cookie'si yazılmadı (oturum kurulmadı).");
        }
    }

    @When("anonim ziyaretçi {string} için API ile {string} rolü isteyerek kayıt olur")
    public void apiIleRolIsteyerekKayitOlur(String alias, String requestedRole) {
        String email = TestDataFactory.uniqueEmail();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", TestDataFactory.TEST_USER_PASSWORD);
        body.put("full_name", "QA Auto " + alias);
        body.put("role", requestedRole);
        body.put("is_active", true);
        EcmApiClient.ApiResponse r = EcmApiClient.anonymous("POST", "/v1/auth/register", body)
                .expect(200, "API ile kayıt (" + alias + ")");
        EcmApiClient.Session s = EcmApiClient.sessionFrom(email, r);
        ApiTestContext.putSession(alias, s);
        ApiTestContext.putName(alias, email);
        registerCreatedAccount(alias, s.userId);
    }

    @Then("kayıt yanıtında {string} hesabının rolü {string} olmalıdır")
    public void kayitYanitindaRol(String alias, String expectedRole) {
        String role = ApiTestContext.session(alias).role;
        if (!expectedRole.equals(role)) {
            throw new AssertionError("Kayıt yanıtında rol '" + expectedRole + "' olmalıydı, '" + role + "' geldi.");
        }
    }

    @Then("yönetici {string} hesabını aktif ve {string} rolünde görmelidir")
    public void yoneticiHesabiAktifVeRoldeGormeli(String alias, String expectedRole) {
        JsonObject user = EcmApiClient.get(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias))
                .expect(200, "Kullanıcı okuma (" + alias + ")").json();
        String role = user.get("role").getAsString();
        boolean active = user.get("is_active").getAsBoolean();
        if (!expectedRole.equals(role) || !active) {
            throw new AssertionError("'" + alias + "' aktif ve '" + expectedRole + "' rolünde olmalıydı. Gelen: role="
                    + role + " is_active=" + active);
        }
    }

    @Then("{string} yalnız yöneticiye açık kullanıcı listesine erişememelidir")
    public void kullaniciListesineErisememeli(String alias) {
        EcmApiClient.Session s;
        try {
            s = ApiTestContext.session(alias);
        } catch (IllegalStateException e) {
            s = TestDataFactory.loginAs(alias);
        }
        EcmApiClient.ApiResponse r = EcmApiClient.get(s, "/v1/users");
        if (r.status() != 403) {
            throw new AssertionError("Kayıtlı hesap GET /v1/users için 403 almalıydı, HTTP " + r.status() + " aldı.");
        }
    }

    private static void registerCreatedAccount(String alias, String userId) {
        ApiTestContext.putId(alias, userId);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + userId,
                Map.of("is_active", false)));
    }

    // ---------------- AUTH-09: oturumsuz erişim ----------------

    @Then("oturumsuz tarayıcıda şu adresler login sayfasına yönlendirilmeli ve veri göstermemelidir")
    public void oturumsuzAdreslerLogineYonlendirilmeli(DataTable table) {
        BrowserManager.startBrowser();
        List<String> rows = table.asList();
        List<String> secrets = new ArrayList<>();
        Matcher m = Pattern.compile("\\{([^}]+)}").matcher(String.join(" ", rows));
        while (m.find()) {
            secrets.add(ApiTestContext.name(m.group(1)));
        }
        for (String row : rows) {
            assertRedirectsToLoginWithoutData(BrowserManager.page,
                    BrowserManager.getBaseUrl() + ApiTestContext.resolve(row.trim()), secrets);
        }
    }

    @Then("oturumsuz {string} {string} isteği 401 almalıdır")
    public void oturumsuzIstek401(String method, String path) {
        EcmApiClient.ApiResponse r = EcmApiClient.anonymous(method, ApiTestContext.resolve(path), null);
        if (r.status() != 401) {
            throw new AssertionError("Oturumsuz " + method + " " + path + " 401 almalıydı, HTTP " + r.status()
                    + " geldi: " + EcmApiClient.redact(r.body()));
        }
    }

    // ---------------- AUTH-10: çoklu oturum ----------------

    @Given("{string} iki ayrı oturumla API ile giriş yapmıştır")
    public void ikiAyriOturumlaGiris(String alias) {
        String email = ApiTestContext.name(alias);
        ApiTestContext.putSession(alias + "#1", EcmApiClient.login(email, TestDataFactory.TEST_USER_PASSWORD));
        ApiTestContext.putSession(alias + "#2", EcmApiClient.login(email, TestDataFactory.TEST_USER_PASSWORD));
    }

    @When("{string} birinci oturumdan çıkış yapar")
    public void birinciOturumdanCikis(String alias) {
        EcmApiClient.Session s1 = ApiTestContext.session(alias + "#1");
        staleRefreshToken = s1.refreshToken();
        lastLogout = EcmApiClient.logout(s1);
        ApiTestContext.lastResponse(lastLogout);
    }

    @Then("çıkış yanıtı 204 olmalı ve refresh cookie'sini temizlemelidir")
    public void cikisYaniti204VeCookieTemizler() {
        if (lastLogout.status() != 204 || !EcmApiClient.clearsRefreshCookie(lastLogout)) {
            throw new AssertionError("Logout 204 ve 'refresh_token=; Max-Age=0' dönmeliydi. Gelen: HTTP "
                    + lastLogout.status() + ", Set-Cookie temizliyor mu: " + EcmApiClient.clearsRefreshCookie(lastLogout));
        }
    }

    @Then("{string} ikinci oturumunu yenileyebilmeli ve profilini alabilmelidir")
    public void ikinciOturumuYenileyebilmeli(String alias) {
        EcmApiClient.Session s2 = ApiTestContext.session(alias + "#2");
        EcmApiClient.refresh(s2).expect(200, "Diğer oturum çıkış yaptıktan sonra ikinci oturumun refresh'i");
        String email = EcmApiClient.get(s2, "/v1/auth/me").expect(200, "İkinci oturum profil").json()
                .get("email").getAsString();
        if (!email.equalsIgnoreCase(s2.email)) {
            throw new AssertionError("İkinci oturumun profili '" + s2.email + "' olmalıydı, '" + email + "' geldi.");
        }
    }

    @Then("{string} birinci oturumun refresh token'ı ile oturum yenileyememelidir")
    public void birinciOturumTokenIleYenileyememeli(String alias) {
        assertRefreshRejected(staleRefreshToken, "Çıkış yapılan oturumun refresh token'ı ile refresh");
    }

    // ---------------- AUTH-11 / AUTH-12: istemci yenileme ve sekmeler ----------------

    @Given("{string} tarayıcıda oturum açmış olarak {string} belgesinin detayını açar")
    public void tarayicidaBelgeDetayiniAcar(String alias, String documentAlias) {
        openUiSession(alias);
        BrowserContext context = BrowserManager.context;
        context.route("**/v1/documents/*/tags", route -> {
            if ("POST".equals(route.request().method()) && invalidateNextTagPost.getAndSet(false)) {
                Map<String, String> headers = new HashMap<>(route.request().headers());
                headers.remove("Authorization");
                headers.put("authorization", "Bearer qa-invalid-access-token");
                route.resume(new Route.ResumeOptions().setHeaders(headers));
            } else {
                route.resume();
            }
        });
        trackSessionTraffic(BrowserManager.page, tabEvents);
        openDocumentDetail(BrowserManager.page, documentAlias);
    }

    @Given("aynı tarayıcıda ikinci sekmede {string} belgesinin detayı açılır")
    public void ikinciSekmedeBelgeDetayiAcilir(String documentAlias) {
        secondTab = BrowserManager.context.newPage();
        trackSessionTraffic(secondTab, secondTabEvents);
        openDocumentDetail(secondTab, documentAlias);
    }

    @When("sonraki etiket ekleme isteği geçersiz bir access token ile gönderilecek şekilde ayarlanır")
    public void sonrakiEtiketIstegiGecersizTokenla() {
        tabEvents.clear();
        invalidateNextTagPost.set(true);
    }

    @When("kullanıcı arayüzde belgeye {string} etiketini ekler")
    public void kullaniciArayuzdeEtiketEkler(String tagAlias) {
        addTagInUi(BrowserManager.page, tagAlias, tabEvents);
    }

    @When("ikinci sekmede kullanıcı belgeye {string} etiketini eklemeyi dener")
    public void ikinciSekmedeEtiketEklemeyiDener(String tagAlias) {
        secondTabEvents.clear();
        addTagInUi(secondTab, tagAlias, secondTabEvents);
    }

    @Then("etiket isteği önce 401 almalı, oturum sessizce yenilenmeli ve istek bir kez başarıyla tekrarlanmalıdır")
    public void etiketIstegi401SonraYenilemeVeTekrar() {
        List<String> events = snapshot(tabEvents);
        boolean ok = events.size() == 3
                && events.get(0).equals("TAG 401")
                && events.get(1).equals("REFRESH 200")
                && events.get(2).startsWith("TAG 2");
        if (!ok) {
            throw new AssertionError("Beklenen akış [TAG 401, REFRESH 200, TAG 2xx] idi. Gözlenen: " + events);
        }
    }

    @Then("{string} etiketi arayüzde ve API'de {string} belgesinde görünmelidir")
    public void etiketArayuzdeVeApideGorunmeli(String tagAlias, String documentAlias) {
        String name = tagNames.get(tagAlias);
        try {
            // TagChip.tsx: eklenmiş etiketin chip'i "Remove <ad>" butonunu taşır (TagInput'taki bekleyen
            // chip'lerde bu etiket yok); chip metni "<ad>×" olduğundan tam metin eşleşmesi kullanılamaz.
            BrowserManager.page.getByRole(AriaRole.BUTTON,
                            new Page.GetByRoleOptions().setName("Remove " + name).setExact(true)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("'" + name + "' etiketi arayüzdeki Tags kartında görünmedi.", e);
        }
        if (!documentTagNames(documentAlias).contains(name)) {
            throw new AssertionError("'" + name + "' etiketi API'de belgede yok: " + documentTagNames(documentAlias));
        }
    }

    @When("tarayıcıdaki refresh token cookie'si silinir")
    public void tarayicidakiRefreshCookieSilinir() {
        BrowserManager.context.clearCookies();
        if (browserRefreshCookie().isPresent()) {
            throw new AssertionError("refresh_token cookie'si silinemedi.");
        }
    }

    @Then("oturum yenileme 401 almalı ve etiket isteği tekrarlanmamalıdır")
    public void oturumYenileme401VeTekrarYok() {
        List<String> events = snapshot(tabEvents);
        if (!events.equals(List.of("TAG 401", "REFRESH 401"))) {
            throw new AssertionError("Beklenen akış [TAG 401, REFRESH 401] idi (tekrar yok). Gözlenen: " + events);
        }
    }

    @Then("{string} etiketi API'de {string} belgesinde bulunmamalıdır")
    public void etiketApideBulunmamali(String tagAlias, String documentAlias) {
        String name = tagNames.get(tagAlias);
        List<String> names = documentTagNames(documentAlias);
        if (names.contains(name)) {
            throw new AssertionError("'" + name + "' etiketi kaydedilmemeliydi ama belgede var: " + names);
        }
    }

    @Then("sayfa yenilendiğinde login sayfası görüntülenmeli ve veri gösterilmemelidir")
    public void sayfaYenilenenceLoginGoruntulenmeli() {
        Page page = BrowserManager.page;
        assertRedirectsToLoginWithoutData(page, page.url(), List.of(ApiTestContext.name("çalışma belgesi")));
    }

    @Then("ikinci sekmedeki işlemin sonucu RQ-15 kararı için kanıt olarak kaydedilmelidir")
    public void ikinciSekmeSonucuKanitOlarakKaydedilmeli() {
        List<String> events = snapshot(secondTabEvents);
        if (events.stream().noneMatch(e -> e.startsWith("TAG "))) {
            throw new AssertionError("İkinci sekmede etiket isteği hiç gönderilmedi; kanıt oluşmadı. Olaylar: " + events);
        }
        String name = tagNames.get("sekme etiketi");
        boolean persisted = documentTagNames("çalışma belgesi").contains(name);
        String evidence = "RQ-15 (logout sonrası diğer sekmedeki işlem):\n"
                + "  jwt ttl: 8 saat (rest.yaml); logout yalnız refresh token'ı iptal eder (auth.rs logout).\n"
                + "  ikinci sekme ağ olayları: " + events + "\n"
                + "  etiket kalıcı mı (API): " + persisted + "\n";
        System.out.println("[RQ-15] " + evidence.replace("\n", " | "));
        Allure.addAttachment("RQ-15 evidence — stale tab action after logout", "text/plain", evidence);
    }

    @Then("ikinci sekme yenilendiğinde login sayfası görüntülenmeli ve belge verisi gösterilmemelidir")
    public void ikinciSekmeYenilenenceLogin() {
        secondTabEvents.clear();
        assertRedirectsToLoginWithoutData(secondTab, secondTab.url(), List.of(ApiTestContext.name("çalışma belgesi")));
    }

    @Then("ikinci sekmede oturum yenileme başarısız olmalıdır")
    public void ikinciSekmedeYenilemeBasarisiz() {
        List<String> refreshes = snapshot(secondTabEvents).stream().filter(e -> e.startsWith("REFRESH")).toList();
        if (refreshes.isEmpty() || refreshes.stream().anyMatch(e -> !e.equals("REFRESH 401"))) {
            throw new AssertionError("İkinci sekmenin yeniden yüklemesinde refresh yalnız 401 almalıydı. Gözlenen: "
                    + refreshes);
        }
    }

    // ---------------- yardımcılar ----------------

    private static void openUiSession(String alias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(alias), TestDataFactory.TEST_USER_PASSWORD);
    }

    private static void waitAppShell(Page page) {
        page.locator("button[aria-haspopup='menu']").last()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    private static void waitLogin(Page page, String context) {
        try {
            page.waitForURL(LOGIN_URL, new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
            page.locator("input[type='password']")
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError(context + ": login sayfası görüntülenmedi. URL: " + page.url(), e);
        }
    }

    /*
     * Adres açılır; login'e yönlendirme beklenir ve bu sırada kimlik gerektiren
     * hiçbir veri ucunun (auth dışı /v1/*) başarılı yanıt vermediği, sayfada da
     * gizli isimlerin görünmediği doğrulanır.
     */
    private static void assertRedirectsToLoginWithoutData(Page page, String url, List<String> secrets) {
        List<String> dataResponses = Collections.synchronizedList(new ArrayList<>());
        Consumer<Response> listener = r -> {
            try {
                String path = URI.create(r.url()).getPath();
                if (path.startsWith("/v1/") && !path.startsWith("/v1/auth/") && r.status() < 400) {
                    dataResponses.add(r.request().method() + " " + path + " -> " + r.status());
                }
            } catch (Exception ignored) {
                // yalnız kanıt toplama
            }
        };
        page.onResponse(listener);
        try {
            page.navigate(url);
            waitLogin(page, url);
            try {
                page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
            } catch (PlaywrightException ignored) {
                // Açık bağlantı kalsa da aşağıdaki kontroller geçerlidir.
            }
            String body = page.locator("body").innerText();
            for (String secret : secrets) {
                if (body.contains(secret)) {
                    throw new AssertionError(url + ": oturumsuz sayfada gizli veri görüntülendi: '" + secret + "'");
                }
            }
            if (!dataResponses.isEmpty()) {
                throw new AssertionError(url + ": oturumsuz sayfa veri uçlarından başarılı yanıt aldı: " + dataResponses);
            }
        } finally {
            page.offResponse(listener);
        }
    }

    private static void trackSessionTraffic(Page page, List<String> events) {
        page.onResponse(r -> {
            try {
                String path = URI.create(r.url()).getPath();
                String method = r.request().method();
                if ("POST".equals(method) && path.equals("/v1/auth/refresh")) {
                    events.add("REFRESH " + r.status());
                } else if ("POST".equals(method) && TAGS_PATH.matcher(path).matches()) {
                    events.add("TAG " + r.status());
                }
            } catch (Exception ignored) {
                // yalnız kanıt toplama
            }
        });
    }

    private static void openDocumentDetail(Page page, String documentAlias) {
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        try {
            page.getByText(ApiTestContext.name(documentAlias)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            page.locator("input[placeholder^='Type to add a tag']")
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Belge detayı ve etiket girişi yüklenmedi. URL: " + page.url(), e);
        }
    }

    /* TagManager.tsx: isim yazılır, Enter ile bekleyen listeye eklenir, "Add tags" ile gönderilir. */
    private void addTagInUi(Page page, String tagAlias, List<String> events) {
        String name = "qa-" + TestDataFactory.unique("auth").toLowerCase();
        tagNames.put(tagAlias, name);
        Locator input = page.locator("input[placeholder^='Type to add a tag']");
        input.fill(name);
        input.press("Enter");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add tags").setExact(true)).click();
        try {
            page.waitForCondition(
                    () -> events.stream().anyMatch(e -> e.startsWith("TAG "))
                            && page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Adding…")).count() == 0,
                    new Page.WaitForConditionOptions().setTimeout(UI_WAIT_MS)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError("Etiket ekleme isteği tamamlanmadı. Olaylar: " + snapshot(events), e);
        }
    }

    private static List<String> documentTagNames(String documentAlias) {
        JsonObject json = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags")
                .expect(200, "Belge etiketleri").json();
        List<String> names = new ArrayList<>();
        json.getAsJsonArray("tags").forEach(t -> names.add(t.getAsJsonObject().get("name").getAsString().toLowerCase()));
        return names;
    }

    private static List<String> snapshot(List<String> events) {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    private static Optional<String> browserRefreshCookie() {
        String host = URI.create(BrowserManager.getAppApiBaseUrl()).getHost();
        return BrowserManager.context.cookies().stream()
                .filter(c -> "refresh_token".equals(c.name) && host.equals(c.domain) && !c.value.isBlank())
                .map(c -> c.value)
                .findFirst();
    }

    private static void assertRefreshRejected(String refreshToken, String context) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalStateException(context + ": karşılaştırılacak refresh token kaydedilmemiş.");
        }
        EcmApiClient.ApiResponse r = EcmApiClient.refreshWith(refreshToken);
        ApiTestContext.lastResponse(r);
        boolean newCookie = EcmApiClient.refreshCookieValue(r).isPresent();
        if (r.status() != 401 || newCookie || r.body().contains("\"token\"")) {
            throw new AssertionError(context + ": HTTP 401, yeni refresh cookie'si ve access token olmadan reddedilmeliydi. "
                    + "Gelen: HTTP " + r.status() + ", yeni refresh cookie: " + newCookie
                    + ", gövde: " + EcmApiClient.redact(r.body()));
        }
    }

    private static void setActive(String alias, boolean active) {
        EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias),
                        Map.of("is_active", active))
                .expect(200, (active ? "Aktifleştirme" : "Pasifleştirme") + " (" + alias + ")");
    }

    private static void assertDeactivated(EcmApiClient.ApiResponse r, String context) {
        ApiTestContext.lastResponse(r);
        if (r.status() != 403 || !r.errorMessage().contains(DEACTIVATED)) {
            throw new AssertionError(
                    context + ": HTTP 403 ve '" + DEACTIVATED + "' bekleniyordu. Gelen: HTTP "
                            + r.status() + " " + r.body()
            );
        }
    }
}
