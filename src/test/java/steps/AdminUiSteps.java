package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DocumentsPage;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/*
 * Batch 31 — yönetim arayüzleri ve ön yüz ↔ arka uç tutarlılığı: Users, Groups,
 * Roles, PropertyTemplates, Classes/ClassDetail, AuditLogs, AdminGuard,
 * DocumentDetail/FolderDetail yetkiye bağlı kontroller, XSS, depolama GC, /info.
 * Her arayüz işleminin sonucu ayrıca API ile doğrulanır.
 */
public class AdminUiSteps {

    private static final int UI_WAIT_MS = 20000;
    private static final String XSS_IMG = "<img src=x onerror=\"window.__qaXss=1\">";
    private static final String XSS_SCRIPT = "<script>window.__qaXss=2</script>";

    // ---------------------------------------------------------------------
    // Ortak
    // ---------------------------------------------------------------------

    private static Page adminPage() {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        return BrowserManager.page;
    }

    private static Page userPage(String userAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(userAlias), TestDataFactory.TEST_USER_PASSWORD);
        return BrowserManager.page;
    }

    private static Page page() {
        return BrowserManager.page;
    }

    private static void waitVisible(Locator l, String what) {
        try {
            l.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError(what + " görünmedi. URL: " + page().url(), e);
        }
    }

    private static Response clickAndWait(Page p, String method, String urlPart, Runnable action) {
        return p.waitForResponse(r -> method.equals(r.request().method()) && r.url().contains(urlPart),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS), action);
    }

    private static void expectOk(Response r, String what) {
        if (r.status() < 200 || r.status() >= 300) {
            throw new AssertionError(what + " HTTP " + r.status() + ": " + r.text());
        }
    }

    private static Locator dialog(Page p) {
        return p.locator("[aria-modal='true']").last();
    }

    private static boolean visibleButton(Page p, String name) {
        Locator b = p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
        return b.count() > 0 && b.first().isVisible();
    }

    // ---------------------------------------------------------------------
    // USER-06 — Kullanıcılar sayfası
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} adında yeni bir standart kullanıcı oluşturur")
    public void arayuzdeKullaniciOlusturur(String alias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/users");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New user").setExact(true)).click();
        String email = TestDataFactory.uniqueEmail();
        p.getByPlaceholder("user@company.com").fill(email);
        p.getByPlaceholder("Optional").first().fill("QA Arayüz " + alias);
        p.locator("select").last().selectOption("user");
        p.getByPlaceholder("Required").first().fill(TestDataFactory.TEST_USER_PASSWORD);
        Response r = clickAndWait(p, "POST", "/v1/users",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create user").setExact(true)).click());
        expectOk(r, "Arayüzden kullanıcı oluşturma");
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(alias, id);
        ApiTestContext.putName(alias, email);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + id, Map.of("is_active", false)));
    }

    @When("yönetici arayüzde {string} kullanıcısının rolünü {string} yapar")
    public void arayuzdeRolDegistirir(String alias, String role) {
        Page p = page();
        Locator row = userRow(p, alias);
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit").setExact(true)).click();
        waitVisible(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save changes").setExact(true)), "Kullanıcı düzenleme formu");
        p.locator("select").last().selectOption(role);
        expectOk(clickAndWait(p, "PUT", "/v1/users/" + ApiTestContext.id(alias),
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save changes").setExact(true)).click()), "Rol değişikliği");
    }

    @When("yönetici arayüzde {string} kullanıcısını pasifleştirir")
    public void arayuzdeKullaniciyiPasiflestirir(String alias) {
        Page p = page();
        Locator row = userRow(p, alias);
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deactivate").setExact(true)).click();
        expectOk(clickAndWait(p, "PUT", "/v1/users/" + ApiTestContext.id(alias),
                () -> dialog(p).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deactivate").setExact(true)).click()), "Pasifleştirme");
    }

    @Then("yönetici kullanıcılar sayfasını yenilediğinde {string} satırında {string} ve {string} görmelidir")
    public void kullanicilarSayfasindaSatir(String alias, String roleText, String statusText) {
        Page p = page();
        p.reload();
        Locator row = userRow(p, alias);
        waitVisible(row.getByText(roleText).first(), "'" + roleText + "'");
        waitVisible(row.getByText(statusText, new Locator.GetByTextOptions().setExact(true)).first(), "'" + statusText + "'");
    }

    @Then("yönetici {string} hesabını {string} rolünde ve pasif görmelidir")
    public void yoneticiHesabiRoldePasif(String alias, String role) {
        JsonObject u = EcmApiClient.get(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias)).expect(200, "Kullanıcı").json();
        if (!role.equals(u.get("role").getAsString()) || u.get("is_active").getAsBoolean()) {
            throw new AssertionError("Kullanıcı " + role + " ve pasif olmalıydı: " + u);
        }
    }

    private static Locator userRow(Page p, String alias) {
        String email = ApiTestContext.name(alias);
        p.getByPlaceholder("Search by name or email…").fill(email);
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search").setExact(true)).click();
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(email));
        waitVisible(row.first(), "Kullanıcı satırı (" + email + ")");
        return row.first();
    }

    // ---------------------------------------------------------------------
    // GRP-04 — Gruplar sayfası
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} grubunu oluşturur")
    public void arayuzdeGrupOlusturur(String alias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/groups");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New group").setExact(true)).click();
        String name = TestDataFactory.unique("QA Arayüz Grup");
        p.getByPlaceholder("e.g. Engineering").fill(name);
        Response r = clickAndWait(p, "POST", "/v1/groups",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create group").setExact(true)).click());
        expectOk(r, "Grup oluşturma");
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(alias, id);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/groups/" + id));
    }

    @When("yönetici arayüzde {string} grubuna {string} kullanıcısını ekler")
    public void arayuzdeGrubaUyeEkler(String groupAlias, String userAlias) {
        Page p = page();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/groups/" + ApiTestContext.id(groupAlias));
        Locator search = p.getByPlaceholder("Search users by email or name…");
        waitVisible(search, "Üye arama kutusu");
        String email = ApiTestContext.name(userAlias);
        search.fill(email);
        Locator option = p.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText(email));
        waitVisible(option.first(), "Kullanıcı seçeneği");
        option.first().click();
        expectOk(clickAndWait(p, "POST", "/members",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add").setExact(true)).click()), "Üye ekleme");
    }

    @When("yönetici arayüzde {string} grubundan {string} kullanıcısını çıkarır")
    public void arayuzdeGruptanCikarir(String groupAlias, String userAlias) {
        Page p = page();
        Locator item = p.locator("li").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(userAlias)));
        waitVisible(item.first(), "Üye satırı");
        expectOk(clickAndWait(p, "DELETE", "/members/" + ApiTestContext.id(userAlias),
                () -> item.first().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove").setExact(true)).click()), "Üye çıkarma");
    }

    @Then("yönetici grup sayfasını yenilediğinde üyeler arasında {string} görmelidir")
    public void grupSayfasindaUyeGormeli(String userAlias) {
        page().reload();
        waitVisible(page().locator("li").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(userAlias))).first(), "Üye");
    }

    @Then("yönetici grup sayfasını yenilediğinde üyeler arasında {string} görmemelidir")
    public void grupSayfasindaUyeGormemeli(String userAlias) {
        Page p = page();
        p.reload();
        waitVisible(p.getByPlaceholder("Search users by email or name…"), "Grup detayı");
        p.waitForLoadState(LoadState.NETWORKIDLE);
        if (p.locator("li").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(userAlias))).count() > 0) {
            throw new AssertionError("Çıkarılan üye yenilemeden sonra listede.");
        }
    }

    @Then("{string} grubunun üyeleri arasında {string} bulunmamalıdır")
    public void grupUyeleriArasindaBulunmamali(String groupAlias, String userAlias) {
        if (EcmApiClient.get(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias) + "/members").expect(200, "Üyeler")
                .body().contains(ApiTestContext.id(userAlias))) {
            throw new AssertionError("'" + userAlias + "' API üye listesinde hâlâ var.");
        }
    }

    // ---------------------------------------------------------------------
    // RBAC-08 — Roller sayfası
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} izinleriyle {string} özel rolünü oluşturur")
    public void arayuzdeOzelRolOlusturur(String actionsCsv, String alias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/roles");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New role").setExact(true)).click();
        String name = ("qa-rol-" + System.nanoTime()).toLowerCase();
        p.getByPlaceholder("Role name (e.g. auditor)").fill(name);
        Response r = clickAndWait(p, "POST", "/v1/rbac/roles",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create").setExact(true)).click());
        expectOk(r, "Rol oluşturma");
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(alias, id);
        ApiTestContext.putName(alias, name);
        selectRole(p, name);
        for (String action : actionsCsv.split(",")) {
            permissionBox(p, action.trim()).check();
        }
        expectOk(clickAndWait(p, "PUT", "/v1/rbac/roles/" + id + "/permissions",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save changes").setExact(true)).click()), "İzin kaydı");
    }

    @Then("yönetici roller sayfasını yenilediğinde {string} rolünde yalnız {string} izinleri işaretli olmalıdır")
    public void rollerSayfasindaIzinlerIsaretli(String alias, String actionsCsv) {
        Page p = page();
        p.reload();
        selectRole(p, ApiTestContext.name(alias));
        List<String> expected = new ArrayList<>();
        for (String a : actionsCsv.split(",")) {
            expected.add(a.trim());
        }
        for (String action : List.of("document:read", "document:write", "document:delete", "folder:read", "folder:write")) {
            boolean checked = permissionBox(p, action).isChecked();
            if (checked != expected.contains(action)) {
                throw new AssertionError("'" + action + "' kutusu " + (expected.contains(action) ? "işaretli" : "boş") + " olmalıydı.");
            }
        }
        String body = EcmApiClient.get(TestDataFactory.admin(), "/v1/rbac/roles/" + ApiTestContext.id(alias)).expect(200, "Rol").body();
        int n = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("permission_ids").size();
        if (n != expected.size()) {
            throw new AssertionError("API rol izin sayısı " + expected.size() + " olmalıydı, " + n);
        }
    }

    @Then("yönetici arayüzde viewer sistem rolünü açtığında izin kutuları devre dışı ve kaydetme kontrolü olmamalıdır")
    public void viewerSistemRoluSaltOkunur() {
        Page p = page();
        selectRole(p, "viewer");
        waitVisible(p.getByText("read-only", new Page.GetByTextOptions().setExact(true)).first(), "'read-only' rozeti");
        Locator boxes = p.locator("input[type='checkbox']");
        for (int i = 0; i < boxes.count(); i++) {
            if (boxes.nth(i).isEnabled()) {
                throw new AssertionError("Sistem rolünün izin kutuları devre dışı olmalıydı.");
            }
        }
        if (visibleButton(p, "Save changes")) {
            throw new AssertionError("Sistem rolünde 'Save changes' sunulmamalıydı.");
        }
    }

    private static void selectRole(Page p, String name) {
        Locator b = p.getByRole(AriaRole.BUTTON).filter(new Locator.FilterOptions().setHasText(name));
        waitVisible(b.first(), "Rol listesi (" + name + ")");
        b.first().click();
        waitVisible(p.getByRole(AriaRole.HEADING).filter(new Locator.FilterOptions().setHasText(name)).first(), "Rol başlığı");
    }

    private static Locator permissionBox(Page p, String action) {
        Locator label = p.locator("label").filter(new Locator.FilterOptions().setHasText(action));
        waitVisible(label.first(), "İzin '" + action + "'");
        return label.first().locator("input[type='checkbox']");
    }

    // ---------------------------------------------------------------------
    // PT-03 — Özellik şablonları sayfası
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} türünde {string} şablonunu oluşturur")
    public void arayuzdeSablonOlusturur(String type, String alias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/property-templates");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New template").setExact(true)).click();
        String name = TestDataFactory.unique("QA Arayüz Şablon");
        p.getByPlaceholder("e.g. Invoice Amount").fill(name);
        Locator form = p.locator("form").filter(new Locator.FilterOptions().setHas(p.getByPlaceholder("e.g. Invoice Amount")));
        form.locator("select").first().selectOption(type);
        form.getByPlaceholder("Optional").fill("ilk açıklama");
        Response r = clickAndWait(p, "POST", "/v1/property-templates",
                () -> form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Create").setExact(true)).click());
        expectOk(r, "Şablon oluşturma");
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(alias, id);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/property-templates/" + id));
    }

    @When("yönetici arayüzde {string} şablonunun açıklamasını {string} yapar")
    public void arayuzdeSablonAciklamasi(String alias, String description) {
        Page p = page();
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(alias))).first();
        waitVisible(row, "Şablon satırı");
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit").setExact(true)).click();
        row.locator("input").first().fill(description);
        expectOk(clickAndWait(p, "PATCH", "/v1/property-templates/" + ApiTestContext.id(alias),
                () -> row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click()), "Şablon düzenleme");
    }

    @Then("yenilemeden sonra {string} şablonu {string} türü ve {string} açıklamasıyla görünmeli ve API aynı değerleri döndürmelidir")
    public void sablonYenilemedenSonra(String alias, String type, String description) {
        Page p = page();
        p.reload();
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(alias))).first();
        waitVisible(row.getByText(description).first(), "Şablon açıklaması");
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates").expect(200, "Şablonlar").json().getAsJsonArray("items")) {
            JsonObject t = e.getAsJsonObject();
            if (ApiTestContext.id(alias).equals(t.get("id").getAsString())) {
                if (!type.equals(t.get("data_type").getAsString()) || !description.equals(t.get("description").getAsString())) {
                    throw new AssertionError("API şablon değerleri farklı: " + t);
                }
                return;
            }
        }
        throw new AssertionError("Şablon API listesinde yok.");
    }

    // ---------------------------------------------------------------------
    // CLS-14 — Sınıflar / sınıf detayı
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} anahtarlı zorunlu metin özelliğiyle {string} sınıfını oluşturur")
    public void arayuzdeSinifOlusturur(String key, String alias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/classes");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New class").setExact(true)).click();
        String name = TestDataFactory.unique("QA Arayüz Sınıf");
        p.getByPlaceholder("e.g. Invoice").fill(name);
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add property").setExact(true)).click();
        p.getByPlaceholder("key").last().fill(key);
        p.getByPlaceholder("Display name").last().fill(key.toUpperCase());
        Locator propRow = p.getByPlaceholder("key").last().locator("xpath=..");
        propRow.locator("select").first().selectOption("text");
        Locator required = propRow.locator("input[type='checkbox']");
        if (required.count() > 0) {
            // Toggle anahtarı: gerçek checkbox sr-only (görünmez); etiket tıklaması yerine zorlanmış check.
            required.first().check(new Locator.CheckOptions().setForce(true));
        }
        Response r = clickAndWait(p, "POST", "/v1/classes",
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create class").setExact(true)).click());
        expectOk(r, "Sınıf oluşturma");
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("id").getAsString();
        ApiTestContext.putId(alias, id);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> {
            long v = ClassAdminSteps.classVersion(alias);
            EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + id + "/active", Map.of("expected_version", v, "is_active", false));
        });
    }

    @When("yönetici arayüzde {string} sınıfına {string} anahtarlı sayısal özellik ekler")
    public void arayuzdeOzellikEkler(String alias, String key) {
        Page p = page();
        p.navigate(BrowserManager.getBaseUrl() + "/classes/" + ApiTestContext.id(alias));
        Locator add = p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add property").setExact(true));
        waitVisible(add, "Add property");
        add.click();
        // AddPropertyModal.tsx: aria-modal yok ve gönder butonu formun dışında (modal alt çubuğu).
        Locator modal = p.locator("div.fixed").filter(new Locator.FilterOptions().setHasText("New properties are always optional")).last();
        Locator form = modal.locator("form");
        waitVisible(form, "Özellik ekleme formu");
        form.locator("input:not([type='checkbox'])").nth(0).fill(key);
        form.locator("input:not([type='checkbox'])").nth(1).fill(key.toUpperCase());
        form.locator("select").first().selectOption("numeric");
        expectOk(clickAndWait(p, "POST", "/properties",
                () -> modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add property").setExact(true)).click()), "Özellik ekleme");
    }

    @When("yönetici arayüzde {string} sınıfının {string} özelliğine en fazla {int} karakter kuralı ekler")
    public void arayuzdeKuralEkler(String alias, String key, int max) {
        Page p = page();
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(key)).first();
        waitVisible(row, "Özellik satırı");
        // ValidationRulesModal.tsx load(): GET …/rules + GET /validation-constraints yanıtı geldiğinde seçimi (eski
        // closure değerine göre) ilk türe sıfırlıyor; dev modunda efekt iki kez çalıştığı için ikinci yanıt seçimimizden
        // sonra gelebiliyordu (CLS-14 allowed_values kaydetti). Modalın bu istekleri bitene kadar beklenir, sonra seçilir.
        java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Predicate<String> modalCall = u -> u.contains("/validation-constraints") || u.matches(".*/properties/[^/]+/rules(\\?.*)?$");
        java.util.function.Consumer<com.microsoft.playwright.Request> onReq = rq -> { if ("GET".equals(rq.method()) && modalCall.test(rq.url())) pending.incrementAndGet(); };
        java.util.function.Consumer<com.microsoft.playwright.Request> onEnd = rq -> {
            if ("GET".equals(rq.method()) && modalCall.test(rq.url())) { pending.decrementAndGet(); done.incrementAndGet(); }
        };
        p.onRequest(onReq);
        p.onRequestFinished(onEnd);
        p.onRequestFailed(onEnd);
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Rules").setExact(true)).click();
        Locator form = p.locator("form").filter(new Locator.FilterOptions().setHas(
                p.locator("button[type='submit']").filter(new Locator.FilterOptions().setHasText("Add rule")))).last();
        waitVisible(form, "Kural formu");
        Locator type = form.locator("select").first();
        type.locator("option[value='max_length']").waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED).setTimeout(UI_WAIT_MS));
        long deadline = System.currentTimeMillis() + UI_WAIT_MS;
        while (pending.get() > 0 || done.get() < 2) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Kural modalı yüklemesi tamamlanmadı (bekleyen " + pending.get() + ", biten " + done.get() + ")");
            }
            p.waitForTimeout(50); // olay döngüsünün yanıt olaylarını işlemesi için
        }
        p.offRequest(onReq);
        p.offRequestFinished(onEnd);
        p.offRequestFailed(onEnd);
        Locator message = form.getByPlaceholder("Shown when a value violates this rule");
        type.selectOption("max_length");
        form.locator("input:not([type='checkbox'])").first().fill(String.valueOf(max));
        message.fill(key + " en fazla " + max + " karakter");
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(type).hasValue("max_length");
        expectOk(clickAndWait(p, "POST", "/rules",
                () -> form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add rule").setExact(true)).click()), "Kural ekleme");
        p.getByLabel("Close").last().click();
    }

    @When("yönetici arayüzde {string} sınıfını pasifleştirir")
    public void arayuzdeSinifiPasiflestirir(String alias) {
        Page p = page();
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Deactivate").setExact(true)).click();
        expectOk(clickAndWait(p, "POST", "/active",
                () -> dialog(p).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Deactivate").setExact(true)).click()), "Sınıf pasifleştirme");
    }

    @Then("yenilemeden sonra {string} sınıfı pasif görünmeli, API'de {int} özellik ve {string} üzerinde kural bulunmalı, yükleme sayfasında devre dışı olmalıdır")
    public void sinifYenilemedenSonra(String alias, int propCount, String key) {
        Page p = page();
        p.reload();
        waitVisible(p.getByText("Inactive", new Page.GetByTextOptions().setExact(true)).first(), "'Inactive'");
        JsonObject c = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(alias)).expect(200, "Sınıf").json();
        if (c.get("is_active").getAsBoolean() || c.getAsJsonArray("properties").size() != propCount) {
            throw new AssertionError("Sınıf pasif ve " + propCount + " özellikli olmalıydı: " + c);
        }
        String propId = null;
        for (JsonElement e : c.getAsJsonArray("properties")) {
            if (key.equals(e.getAsJsonObject().get("key").getAsString())) {
                propId = e.getAsJsonObject().get("id").getAsString();
            }
        }
        String rules = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(alias) + "/properties/" + propId + "/rules")
                .expect(200, "Kurallar").body();
        if (!rules.contains("max_length")) {
            throw new AssertionError("'" + key + "' üzerinde max_length kuralı olmalıydı: " + rules);
        }
        // DocumentNew.tsx: pasif sınıf listede kalır ama <option disabled> ve "(inactive)" etiketiyle sunulur.
        p.navigate(BrowserManager.getBaseUrl() + "/documents/new");
        Locator option = p.locator("option[value='" + ApiTestContext.id(alias) + "']");
        waitVisible(p.locator("select:has(option[value='" + ApiTestContext.id(alias) + "'])").first(), "Yükleme sayfası sınıf seçimi");
        if (option.first().isEnabled() || !option.first().innerText().contains("(inactive)")) {
            throw new AssertionError("Pasif sınıf yükleme sayfasında devre dışı ve '(inactive)' olarak sunulmalıydı: '"
                    + option.first().innerText() + "' enabled=" + option.first().isEnabled());
        }
    }

    // ---------------------------------------------------------------------
    // AUD-06 — Denetim kayıtları sayfası
    // ---------------------------------------------------------------------

    @Given("yönetici {string} klasörünü benzersiz bir adla yeniden adlandırmıştır")
    public void klasoruYenidenAdlandirmistir(String folderAlias) {
        String name = TestDataFactory.unique("QA Denetim Adı");
        EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias), Map.of("name", name))
                .expect(200, "Klasör yeniden adlandırma");
        ApiTestContext.putName(folderAlias + "#renamed", name);
    }

    @Then("yönetici denetim kayıtlarında {string} eylemini filtreleyip {string} klasörünün kaydını ayrıntısıyla görmelidir")
    public void denetimKayitlarindaGormeli(String action, String folderAlias) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/admin/audit-logs");
        Locator actionSelect = p.locator("select:has(option[value='" + action + "'])").first();
        waitVisible(actionSelect, "Eylem filtresi");
        actionSelect.selectOption(action);
        // Sayfanın kendi ilk (filtresiz) isteği geç dönebilir; yalnız filtreyi taşıyan istek beklenir.
        Response r = p.waitForResponse(resp -> "GET".equals(resp.request().method()) && resp.url().contains("/v1/admin/audit-logs")
                        && resp.url().contains("action=" + action),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters").setExact(true)).click());
        expectOk(r, "Denetim sorgusu");
        if (!r.url().contains("action=" + action)) {
            throw new AssertionError("Filtre isteğe yansımadı: " + r.url());
        }
        Locator rows = p.locator("tr[role='button']");
        waitVisible(rows.first(), "Denetim satırı");
        String expectedName = ApiTestContext.name(folderAlias + "#renamed");
        for (int i = 0; i < rows.count(); i++) {
            if (!rows.nth(i).innerText().contains(action)) {
                throw new AssertionError("Filtrelenmiş listede başka eylem var: " + rows.nth(i).innerText());
            }
        }
        for (int i = 0; i < Math.min(rows.count(), 15); i++) {
            rows.nth(i).click();
            try {
                p.getByText(expectedName).first().waitFor(new Locator.WaitForOptions().setTimeout(3000));
                waitVisible(p.getByText("After state").first(), "'After state'");
                waitVisible(p.getByText(TestDataFactory.ADMIN_EMAIL).first(), "Aktör");
                return;
            } catch (PlaywrightException ignored) {
                p.keyboard().press("Escape");
            }
        }
        throw new AssertionError("Yeniden adlandırma kaydının ayrıntısında yeni ad (" + expectedName + ") bulunamadı.");
    }

    // ---------------------------------------------------------------------
    // SYS-01 — Yönetim sayfaları yalnız admin
    // ---------------------------------------------------------------------

    @Then("{string} arayüzde {string} adresine gittiğinde yönetim verisi görmeden {string} adresine yönlendirilmelidir")
    public void adminSayfasiYonlendirme(String userAlias, String path, String target) {
        Page p = BrowserManager.page == null ? userPage(userAlias) : page();
        List<String> adminResponses = Collections.synchronizedList(new ArrayList<>());
        Consumer<Response> listener = r -> {
            String u = URI.create(r.url()).getPath();
            if (u.startsWith("/v1/admin/") || u.equals("/v1/users") || u.equals("/v1/info")) {
                if (r.status() < 400) {
                    adminResponses.add(u + " -> " + r.status());
                }
            }
        };
        p.onResponse(listener);
        try {
            p.navigate(BrowserManager.getBaseUrl() + path);
            p.waitForURL("**" + target, new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
            p.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (PlaywrightException e) {
            throw new AssertionError(path + " normal kullanıcıyı " + target + " adresine yönlendirmedi. URL: " + p.url(), e);
        } finally {
            p.offResponse(listener);
        }
        if (!adminResponses.isEmpty()) {
            throw new AssertionError("Normal kullanıcı yönetim verisi aldı: " + adminResponses);
        }
    }

    @Given("{string} tarayıcıda oturum açmıştır")
    public void tarayicidaOturumAcmistir(String userAlias) {
        userPage(userAlias);
    }

    // ---------------------------------------------------------------------
    // FE-01 / FE-02 / DOC-18 — Belge detayındaki kontroller
    // ---------------------------------------------------------------------

    @Then("yönetici arayüzde {string} belgesinin durum seçeneklerinde {string} sunulmamalıdır")
    public void durumSecenekleriSunulmamali(String documentAlias, String optionLabel) {
        Page p = adminPage();
        p.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        Locator select = p.locator("select").filter(new Locator.FilterOptions().setHas(p.locator("option[value='under_review']")));
        waitVisible(select.first(), "Durum seçimi");
        List<String> options = new ArrayList<>(select.first().locator("option").allInnerTexts());
        if (options.contains(optionLabel)) {
            throw new AssertionError("Mevcut durumdan geçilemeyen '" + optionLabel + "' seçeneği sunuluyor. Seçenekler: " + options);
        }
    }

    @Then("{string} arayüzde {string} belgesinde yazma kontrollerini görmemeli ama indirme ve önizlemeyi görmelidir")
    public void viewerYazmaKontrolleriYok(String userAlias, String documentAlias) {
        Page p = userPage(userAlias);
        p.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        waitVisible(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Download").setExact(true)).first(), "Download");
        waitVisible(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Preview").setExact(true)).first(), "Preview");
        waitVisible(p.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Tags").setExact(true)).first(), "Tags kartı");
        // NETWORKIDLE yerine kilit durumu (yetki/kilit verisi yüklendi): BUG-03 seçici isteği ağı meşgul tutuyor.
        p.getByText(java.util.regex.Pattern.compile("^(Not checked out\\.|Checked out by .+)$")).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        List<String> shown = new ArrayList<>();
        for (String b : List.of("New version", "Move to folder…", "Classify", "Reclassify", "Delete", "Hard delete…", "Check out", "Link document")) {
            if (visibleButton(p, b)) {
                shown.add(b);
            }
        }
        try {
            if (new DocumentsPage(p).renameEditButonuGorunuyorMu()) {
                shown.add("rename");
            }
        } catch (AssertionError notFound) {
            // DocumentsPage.renameEditButton() kontrol yoksa AssertionError fırlatır: yok = beklenen durum.
        }
        if (p.locator("input[placeholder^='Type to add a tag']").count() > 0) {
            shown.add("tag input");
        }
        if (!shown.isEmpty()) {
            throw new AssertionError("Viewer için yazma kontrolleri sunulmamalıydı: " + shown);
        }
    }

    @Then("yönetici arayüzde silinmiş {string} belgesinde Deleted rozetini görmeli ve New version kontrolü sunulmamalıdır")
    public void silinmisBelgedeNewVersionYok(String documentAlias) {
        Page p = BrowserManager.page == null ? adminPage() : page();
        p.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        waitVisible(p.getByText("Deleted", new Page.GetByTextOptions().setExact(true)).first(), "'Deleted' rozeti");
        waitVisible(p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Restore").setExact(true)).first(), "Restore");
        p.waitForLoadState(LoadState.NETWORKIDLE);
        int n = p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New version").setExact(true)).count();
        if (n != 0) {
            throw new AssertionError("Soft delete edilmiş belgede 'New version' kontrolü sunuluyor (count=" + n + "); "
                    + "DocumentDetail.tsx canEdit is_deleted kontrol etmiyor.");
        }
    }

    // ---------------------------------------------------------------------
    // FE-03 — Klasör detayındaki sahip kontrolleri
    // ---------------------------------------------------------------------

    @Then("{string} arayüzde {string} klasöründe Rename, Inactivate ve Add grant kontrollerini {string}")
    public void klasorKontrolleri(String userAlias, String folderAlias, String expectation) {
        Page p = userPage(userAlias);
        p.navigate(BrowserManager.getBaseUrl() + "/folders/" + ApiTestContext.id(folderAlias));
        waitVisible(p.getByText(ApiTestContext.name(folderAlias)).first(), "Klasör başlığı");
        // NETWORKIDLE yerine klasör detay verisinin render edilmesi (Explorer başlığı aynı yanıttan gelir, sahip
        // kontrolleri de): admin olmayan kullanıcıda breadcrumb'ın seçici isteği ~22 sn sürüyor (BUG-03) ve ağ boşa çıkmıyor.
        waitVisible(p.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Explorer").setExact(true)).first(), "Klasör detay verisi");
        boolean expected = expectation.startsWith("görmelidir");
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String b : List.of("Rename", "Inactivate", "Add grant")) {
            if (expected) {
                // "Add grant" FolderAclCard'ın kendi isteğinden sonra çizilir: beklenen kontroller koşullu beklenir.
                try {
                    p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(b).setExact(true)).first()
                            .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
                } catch (PlaywrightException ignored) {
                    // Aşağıdaki karşılaştırma eksik kontrolü raporlar.
                }
            }
            seen.put(b, visibleButton(p, b));
        }
        for (Map.Entry<String, Boolean> e : seen.entrySet()) {
            if (e.getValue() != expected) {
                throw new AssertionError("'" + userAlias + "' için klasör kontrolleri " + (expected ? "görünmeliydi" : "görünmemeliydi") + ": " + seen);
            }
        }
    }

    // ---------------------------------------------------------------------
    // DATA-01 — Saklı XSS
    // ---------------------------------------------------------------------

    @Given("yönetici betik içeren başlık, klasör adı, sürüm açıklaması ve kayıtlı arama adıyla kayıtlar oluşturmuştur")
    public void betikIcerenKayitlar() {
        EcmApiClient.Session a = TestDataFactory.admin();
        String marker = "QAXSS" + System.nanoTime();
        ApiTestContext.putName("xss#marker", marker);
        // Klasör adı: ürün reddedebilir; kabul edilirse de sayfada çalışmamalıdır.
        Map<String, Object> folderBody = new LinkedHashMap<>();
        folderBody.put("name", marker + " " + XSS_IMG);
        folderBody.put("parent_id", BrowserManager.ROOT_FOLDER_ID);
        folderBody.put("inherit_permissions", false);
        EcmApiClient.ApiResponse f = EcmApiClient.post(a, "/v1/folders", folderBody);
        String folderId;
        if (f.status() == 200) {
            folderId = f.json().get("id").getAsString();
            ApiTestContext.putName("xss#folderAccepted", "true");
        } else {
            ApiTestContext.putName("xss#folderAccepted", "false (HTTP " + f.status() + ")");
            folderId = TestDataFactory.createFolder(a, "xss yedek klasör", null, false);
        }
        String fid = folderId;
        ApiTestContext.putId("xss klasörü", folderId);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/folders/" + fid + "/active", Map.of("is_active", false)));
        String title = marker + " " + XSS_IMG + " " + XSS_SCRIPT;
        EcmApiClient.ApiResponse d = EcmApiClient.post(a, "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(title, folderId, "text/plain", "xss.txt", "QA xss".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        d.expect(201, "Betikli başlıklı belge");
        TestDataFactory.registerDocument("xss belgesi", d.json().get("document_id").getAsString(), title);
        Map<String, Object> nv = TestDataFactory.newVersionBody("xss belgesi", "Patch", "QA xss v2");
        nv.put("change_comment", marker + " " + XSS_IMG);
        EcmApiClient.post(a, "/v1/documents/unclassified", nv).expect(201, "Betikli açıklamalı sürüm");
        EcmApiClient.ApiResponse s = EcmApiClient.post(a, "/v1/saved-searches", Map.of("name", marker + " " + XSS_IMG, "filter", Map.of("title", marker)));
        if (s.status() == 201) {
            String sid = s.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/saved-searches/" + sid));
        }
    }

    @Then("betik içeren kayıtlar liste, detay, geçmiş, klasör ve kayıtlı arama görünümlerinde çalışmadan düz metin gösterilmelidir")
    public void betiklerCalismamali() {
        Page p = adminPage();
        List<String> dialogs = Collections.synchronizedList(new ArrayList<>());
        p.onDialog(dlg -> {
            dialogs.add(dlg.message());
            dlg.dismiss();
        });
        String marker = ApiTestContext.name("xss#marker");
        List<String> failures = new ArrayList<>();
        Map<String, String> views = new LinkedHashMap<>();
        views.put("belge listesi", "/documents");
        views.put("belge detayı", "/documents/" + ApiTestContext.id("xss belgesi"));
        views.put("klasör detayı", "/folders/" + ApiTestContext.id("xss klasörü"));
        views.put("klasör ağacı", "/folders");
        for (Map.Entry<String, String> v : views.entrySet()) {
            p.navigate(BrowserManager.getBaseUrl() + v.getValue());
            if (v.getKey().equals("belge listesi")) {
                p.getByPlaceholder("Search by title…").fill(marker);
            }
            try {
                p.getByText(marker).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            } catch (PlaywrightException e) {
                if (!v.getKey().equals("klasör ağacı")) {
                    failures.add(v.getKey() + ": işaretli kayıt görünmedi");
                }
            }
            p.waitForLoadState(LoadState.NETWORKIDLE);
            Object xss = p.evaluate("() => window.__qaXss");
            if (xss != null) {
                failures.add(v.getKey() + ": betik çalıştı (window.__qaXss=" + xss + ")");
            }
            if (p.locator("img[src='x']").count() > 0) {
                failures.add(v.getKey() + ": enjekte edilen <img> DOM'a eklendi");
            }
        }
        // Metin literal gösterilmeli (detay sayfası)
        p.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id("xss belgesi"));
        try {
            p.getByText("<img src=x", new Page.GetByTextOptions()).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            failures.add("belge detayı: başlıktaki işaretleme düz metin olarak gösterilmedi");
        }
        if (!dialogs.isEmpty()) {
            failures.add("tarayıcı diyaloğu açıldı: " + dialogs);
        }
        io.qameta.allure.Allure.addAttachment("DATA-01 folder name with markup accepted", "text/plain", ApiTestContext.name("xss#folderAccepted"));
        if (!failures.isEmpty()) {
            throw new AssertionError("Saklı XSS kontrolü başarısız: " + failures);
        }
    }

    // ---------------------------------------------------------------------
    // SYS-02 — Depolama temizliği (yalnız testin kendi belgesi)
    // ---------------------------------------------------------------------

    @When("yönetici depolama temizliğinde {string} belgesinin {string} sürümünü siler")
    public void depolamaTemizligindeSurumSiler(String documentAlias, String versionAlias) {
        boolean listed = false;
        for (JsonElement g : JsonParser.parseString(EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/storage-gc/versions")
                .expect(200, "GC sürüm listesi").body()).getAsJsonArray()) {
            JsonObject grp = g.getAsJsonObject();
            if (ApiTestContext.id(documentAlias).equals(grp.get("document_id").getAsString())) {
                listed = grp.toString().contains(ApiTestContext.id(versionAlias));
            }
        }
        if (!listed) {
            throw new AssertionError("Test belgesinin sürümü GC listesinde yok.");
        }
        JsonObject r = EcmApiClient.post(TestDataFactory.admin(), "/v1/admin/storage-gc/versions/delete",
                Map.of("version_ids", List.of(ApiTestContext.id(versionAlias)))).expect(200, "GC sürüm silme").json();
        if (r.get("succeeded").getAsInt() != 1 || r.get("documents_deleted").getAsInt() != 0) {
            throw new AssertionError("Yalnız seçilen sürüm silinmeliydi: " + r);
        }
    }

    @Then("{string} sürümünün içeriği artık 404 dönmelidir")
    public void surumIcerigi404(String versionAlias) {
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/versions/" + ApiTestContext.id(versionAlias) + "/content").status();
        if (s != 404) {
            throw new AssertionError("Silinen sürüm içeriği 404 olmalıydı, HTTP " + s);
        }
    }

    @Then("{string} belgesinin güncel içeriği {string} olmalıdır")
    public void guncelIcerik(String documentAlias, String expected) {
        String body = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content")
                .expect(200, "Güncel içerik").body();
        if (!expected.equals(body)) {
            throw new AssertionError("Güncel içerik '" + expected + "' olmalıydı, '" + body + "'");
        }
    }

    @Then("{string} belgesinde {int} sürüm kalmalı ve güncel sürüm {string} olmalıdır")
    public void surumSayisiVeGuncel(String documentAlias, int count, String versionAlias) {
        List<JsonObject> v = VersionSteps.versionsAsc(documentAlias);
        String current = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias)).expect(200, "Belge")
                .json().getAsJsonObject("document").get("current_version_id").getAsString();
        if (v.size() != count || !ApiTestContext.id(versionAlias).equals(current)) {
            throw new AssertionError(count + " sürüm ve güncel " + versionAlias + " bekleniyordu: sürüm=" + v.size() + " güncel=" + current);
        }
    }

    @Given("{string} belgesinin sürümleri {string}, {string} ve {string} olarak adlandırılmıştır")
    public void ucSurumAdlandirilmistir(String documentAlias, String a, String b, String c) {
        List<JsonObject> v = VersionSteps.versionsAsc(documentAlias);
        ApiTestContext.putId(a, v.get(0).get("id").getAsString());
        ApiTestContext.putId(b, v.get(1).get("id").getAsString());
        ApiTestContext.putId(c, v.get(2).get("id").getAsString());
    }

    // ---------------------------------------------------------------------
    // SYS-03 — /v1/info ve /v1/config/system
    // ---------------------------------------------------------------------

    @Then("{string} için sistem bilgisi ucu {int} dönmeli ve gizli yapılandırma içermemelidir")
    public void sistemBilgisiUcu(String userAlias, int expected) {
        EcmApiClient.Session s = userAlias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(userAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.get(s, "/v1/info");
        if (r.status() != expected) {
            throw new AssertionError("/v1/info HTTP " + expected + " dönmeliydi, " + r.status());
        }
        String lower = r.body().toLowerCase();
        for (String secret : List.of("password", "secret", "jwt", "access_key", "secret_key", "dev-only-change-me")) {
            if (lower.contains(secret)) {
                throw new AssertionError("/v1/info gizli yapılandırma sızdırıyor: '" + secret + "'");
            }
        }
    }

    @Then("{string} için sistem yapılandırma ucu yalnız All Users grup kimliğini döndürmelidir")
    public void sistemYapilandirmaUcu(String userAlias) {
        JsonObject c = EcmApiClient.get(ApiTestContext.session(userAlias), "/v1/config/system").expect(200, "config/system").json();
        if (c.keySet().size() != 1 || !TestDataFactory.ALL_USERS_GROUP.equals(c.get("all_users_group_id").getAsString())) {
            throw new AssertionError("/v1/config/system yalnız all_users_group_id döndürmeliydi: " + c);
        }
    }
}
