package steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.BrowserManager;
import utils.EcmApi;
import utils.EcmApi.Response;
import utils.TestData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
 * Batch 19: çok kullanıcılı yetki, kullanıcı yaşam döngüsü, sınıf/doğrulama
 * ve statü kuralları (access_control.feature).
 *
 * Kurulum API ile yapılır (senaryoya özel kullanıcı, grup, kalıtımı kapalı
 * klasör, sınıf); DAVRANIŞ her kullanıcının kendi token'ı ile gerçek ECM
 * API'sine (ve B19-02/19/20'de UI'a) karşı doğrulanır. Beklenen sonuçlar ECM
 * ROLES.md ve kaynak kodundaki kurallardır; ECM'in yanlış davranışına göre
 * beklenti UYARLANMAZ.
 *
 * Senaryo içi takma adlar ("ayse", "Gizli", "Sozlesme") koşuya/worker'a
 * özgü gerçek adlara TestData.unique ile eşlenir.
 */
public class AccessSteps {

    private static final String ADMIN_EMAIL = "admin@local";
    private static final String ADMIN_PASSWORD = "123";

    private record User(String id, String email, String password, String token) {
    }

    private record Doc(String id, String title, String folderId) {
    }

    private record Cls(String id, String name) {
    }

    private String adminToken;
    private String adminId;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, String> folders = new HashMap<>();
    private final Map<String, Doc> docs = new HashMap<>();
    private final Map<String, String> groups = new HashMap<>();
    private final Map<String, Cls> classes = new HashMap<>();
    private final Map<String, String> docTags = new HashMap<>();
    private final Map<String, String> aclIds = new HashMap<>();
    private final Map<String, String> savedSearches = new HashMap<>();
    private Map<String, String> roleIds;
    private Response last;
    private Page userPage;
    private String archivedDocId;

    // ---------------------------------------------------------------------
    // Hooks (yalnızca batch19)
    // ---------------------------------------------------------------------

    @Before("@batch19")
    public void resetApiEvidence() {
        EcmApi.resetExchanges();
    }

    @After(value = "@batch19", order = 20000)
    public void attachApiEvidence(Scenario scenario) {
        if (scenario.isFailed()) {
            scenario.attach(EcmApi.exchangeLog().getBytes(StandardCharsets.UTF_8), "text/plain", "api-exchanges");
        }
    }

    // ---------------------------------------------------------------------
    // Ortak kurulum
    // ---------------------------------------------------------------------

    @Given("admin API oturumu açılmıştır")
    public void adminApiOturumu() {
        adminToken = EcmApi.login(ADMIN_EMAIL, ADMIN_PASSWORD);
        adminId = EcmApi.get(adminToken, "/v1/auth/me").expectOk().str("id");
    }

    @Given("{string} adında normal bir test kullanıcısı vardır")
    public void normalKullanici(String alias) {
        testKullanicisi(alias, "user");
    }

    @Given("{string} adında {string} rolünde bir test kullanıcısı vardır")
    public void testKullanicisi(String alias, String role) {
        String tag = TestData.unique(alias).toLowerCase().replaceAll("[^a-z0-9]+", ".");
        String email = "qa.b19." + tag + "@example.com";
        String password = "Qa!" + Long.toHexString(Double.doubleToLongBits(Math.random())) + "x9";
        String id = EcmApi.createUser(adminToken, email, password, role);
        users.put(alias, new User(id, email, password, EcmApi.login(email, password)));
    }

    @Given("admin kalıtımı kapalı {string} klasörünü oluşturmuştur")
    public void kalitimiKapaliKlasor(String alias) {
        folders.put(alias, EcmApi.createFolder(adminToken, TestData.unique("QA B19 " + alias), null, false));
    }

    @Given("admin {string} klasörü altında kalıtımı açık {string} klasörünü oluşturmuştur")
    public void altKlasor(String parent, String alias) {
        folders.put(alias, EcmApi.createFolder(adminToken, TestData.unique("QA B19 " + alias), folder(parent), true));
    }

    @Given("admin {string} klasörünü oluşturup sahipliğini {string} kullanıcısına devretmiştir")
    public void sahipliDevredilenKlasor(String alias, String owner) {
        String id = EcmApi.createFolder(adminToken, TestData.unique("QA B19 " + alias), null, false);
        EcmApi.patch(adminToken, "/v1/folders/" + id + "/owner", Map.of("new_owner_id", user(owner).id())).expectOk();
        folders.put(alias, id);
    }

    @Given("admin {string} klasörüne {string} belgesini yüklemiştir")
    public void adminBelgeYukler(String folderAlias, String docAlias) {
        belgeYukle(adminToken, folderAlias, docAlias);
    }

    @Given("admin Root klasörüne {string} belgesini yüklemiştir")
    public void adminRootBelge(String docAlias) {
        belgeYukle(adminToken, null, docAlias);
    }

    @Given("{string} kullanıcısı {string} klasörüne {string} belgesini yüklemiştir")
    public void kullaniciBelgeYukler(String userAlias, String folderAlias, String docAlias) {
        belgeYukle(user(userAlias).token(), folderAlias, docAlias);
    }

    private void belgeYukle(String token, String folderAlias, String docAlias) {
        String title = TestData.unique("QA B19 " + docAlias);
        String folderId = folderAlias == null ? BrowserManager.ROOT_FOLDER_ID : folder(folderAlias);
        String id = EcmApi.createDocument(token, title, folderId, "B19 content " + title);
        docs.put(docAlias, new Doc(id, title, folderId));
    }

    @Given("admin {string} belgesine benzersiz bir etiket eklemiştir")
    public void benzersizEtiket(String docAlias) {
        String tag = TestData.unique("qab19tag").toLowerCase().replaceAll("[^a-z0-9]", "");
        EcmApi.post(adminToken, "/v1/documents/" + doc(docAlias).id() + "/tags",
                Map.of("tag_ids", List.of(), "tag_name", tag)).expectOk();
        docTags.put(docAlias, tag);
    }

    @Given("admin {string} grubunu oluşturmuştur")
    public void grupOlustur(String alias) {
        String id = EcmApi.post(adminToken, "/v1/groups", Map.of("name", TestData.unique("QA B19 " + alias)))
                .expectOk().str("id");
        groups.put(alias, id);
    }

    @When("admin {string} kullanıcısını {string} grubuna ekler")
    public void grubaEkle(String userAlias, String groupAlias) {
        EcmApi.post(adminToken, "/v1/groups/" + group(groupAlias) + "/members",
                Map.of("user_id", user(userAlias).id())).expectOk();
    }

    @When("admin {string} kullanıcısını {string} grubundan çıkarır")
    public void gruptanCikar(String userAlias, String groupAlias) {
        EcmApi.delete(adminToken, "/v1/groups/" + group(groupAlias) + "/members/" + user(userAlias).id()).expectOk();
    }

    // ---------------------------------------------------------------------
    // ACL grant / deny / revoke
    // ---------------------------------------------------------------------

    @When("admin {string} klasöründe {string} kullanıcısına {string} rolüyle izin verir")
    @Given("admin {string} klasöründe {string} kullanıcısına {string} rolüyle izin vermiştir")
    public void klasorKullaniciIzin(String folderAlias, String userAlias, String role) {
        Response r = EcmApi.grant(adminToken, "folders", folder(folderAlias), roleId(role), user(userAlias).id(), null, false)
                .expectOk();
        aclIds.put("folder:" + folderAlias + ":" + userAlias, r.str("id"));
    }

    @When("admin {string} klasöründe {string} kullanıcısına {string} rolüyle DENY tanımlar")
    public void klasorKullaniciDeny(String folderAlias, String userAlias, String role) {
        EcmApi.grant(adminToken, "folders", folder(folderAlias), roleId(role), user(userAlias).id(), null, true).expectOk();
    }

    @When("admin {string} belgesinde {string} kullanıcısına {string} rolüyle DENY tanımlar")
    public void belgeKullaniciDeny(String docAlias, String userAlias, String role) {
        EcmApi.grant(adminToken, "documents", doc(docAlias).id(), roleId(role), user(userAlias).id(), null, true).expectOk();
    }

    @Given("admin {string} klasöründe {string} grubuna {string} rolüyle izin vermiştir")
    public void klasorGrupIzin(String folderAlias, String groupAlias, String role) {
        EcmApi.grant(adminToken, "folders", folder(folderAlias), roleId(role), null, group(groupAlias), false).expectOk();
    }

    @When("admin {string} sınıfında {string} kullanıcısına {string} rolüyle izin verir")
    public void sinifKullaniciIzin(String classAlias, String userAlias, String role) {
        EcmApi.grant(adminToken, "classes", cls(classAlias).id(), roleId(role), user(userAlias).id(), null, false).expectOk();
    }

    @When("admin {string} klasöründeki {string} kullanıcısının iznini geri alır")
    public void izniGeriAl(String folderAlias, String userAlias) {
        String aclId = aclIds.get("folder:" + folderAlias + ":" + userAlias);
        if (aclId == null) {
            throw new IllegalStateException("Geri alınacak izin kaydı yok: " + folderAlias + "/" + userAlias);
        }
        EcmApi.delete(adminToken, "/v1/folders/" + folder(folderAlias) + "/acl/" + aclId).expectOk();
    }

    @When("admin {string} klasörünün izin kalıtımını kapatır")
    public void kalitimKapat(String folderAlias) {
        EcmApi.patch(adminToken, "/v1/folders/" + folder(folderAlias), Map.of("inherit_permissions", false)).expectOk();
    }

    @When("admin {string} klasörünün izin kalıtımını açar")
    public void kalitimAc(String folderAlias) {
        EcmApi.patch(adminToken, "/v1/folders/" + folder(folderAlias), Map.of("inherit_permissions", true)).expectOk();
    }

    // ---------------------------------------------------------------------
    // Okuma / erişim doğrulamaları
    // ---------------------------------------------------------------------

    @Then("{string} kullanıcısı {string} belgesini okuyabilmelidir")
    public void belgeyiOkuyabilir(String userAlias, String docAlias) {
        Response r = EcmApi.get(user(userAlias).token(), "/v1/documents/" + doc(docAlias).id());
        if (r.status() != 200) {
            throw new AssertionError(userAlias + " belgeyi okuyabilmeliydi: " + r.describe());
        }
        String title = r.obj().getAsJsonObject("document").get("title").getAsString();
        if (!doc(docAlias).title().equals(title)) {
            throw new AssertionError("Okunan belge başlığı beklenen değil: " + title + " | " + r.describe());
        }
    }

    @Then("{string} kullanıcısı {string} belgesini okuyamamalıdır")
    public void belgeyiOkuyamaz(String userAlias, String docAlias) {
        expectDenied(EcmApi.get(user(userAlias).token(), "/v1/documents/" + doc(docAlias).id()), userAlias + " belgeyi okuyamamalıydı");
    }

    @Then("{string} kullanıcısı {string} belgesine hiçbir belge API'si ile erişememelidir")
    public void hicbirBelgeApisi(String userAlias, String docAlias) {
        String token = user(userAlias).token();
        String id = doc(docAlias).id();
        List<String> leaks = new ArrayList<>();
        for (String path : List.of(
                "/v1/documents/" + id,
                "/v1/documents/" + id + "/content",
                "/v1/documents/" + id + "/versions",
                "/v1/documents/" + id + "/history",
                "/v1/documents/" + id + "/tags",
                "/v1/documents/" + id + "/relationships",
                "/v1/documents/" + id + "/custom-fields",
                "/v1/documents/" + id + "/acl",
                "/v1/documents/" + id + "/effective-access",
                "/v1/documents/" + id + "/lock")) {
            Response r = EcmApi.get(token, path);
            if (r.status() != 403 && r.status() != 404) {
                leaks.add(r.describe());
            }
        }
        if (!leaks.isEmpty()) {
            throw new AssertionError("İzinsiz kullanıcı gizli belge verisine erişebildi:\n" + String.join("\n", leaks));
        }
    }

    @Then("{string} kullanıcısı {string} klasörüne erişememelidir")
    public void klasoreErisemez(String userAlias, String folderAlias) {
        String token = user(userAlias).token();
        String id = folder(folderAlias);
        for (String path : List.of("/v1/folders/" + id, "/v1/folders/" + id + "/documents",
                "/v1/folders/" + id + "/children", "/v1/folders/" + id + "/acl")) {
            expectDenied(EcmApi.get(token, path), "İzinsiz kullanıcı klasör verisine erişmemeliydi");
        }
    }

    @Then("{string} belgesi {string} kullanıcısının hiçbir liste ve arama sonucunda görünmemelidir")
    public void hicbirListedeGorunmez(String docAlias, String userAlias) {
        String token = user(userAlias).token();
        Doc d = doc(docAlias);
        List<String> leaks = new ArrayList<>();
        for (Response r : listAndSearchResponses(token, d)) {
            if (r.status() != 200) {
                leaks.add("beklenmeyen durum: " + r.describe());
            } else if (r.body().contains(d.id()) || r.body().contains(d.title())) {
                leaks.add("SIZINTI: " + r.describe());
            }
        }
        // Facet sayıları: başlığa göre filtrelenmiş facet hiçbir kova içermemeli.
        Response facets = EcmApi.get(token, "/v1/documents/facets?title=" + enc(d.title()));
        if (facets.status() != 200 || facets.body().contains("\"count\"")) {
            leaks.add("facet sızıntısı: " + facets.describe());
        }
        if (!leaks.isEmpty()) {
            throw new AssertionError("Gizli belge izinsiz kullanıcının sonuçlarında göründü:\n" + String.join("\n", leaks));
        }
    }

    @Then("{string} belgesi admin kullanıcısının liste ve etiket araması sonuçlarında görünmelidir")
    public void admindeGorunur(String docAlias) {
        Doc d = doc(docAlias);
        Response list = EcmApi.get(adminToken, "/v1/documents?title=" + enc(d.title()));
        Response tag = EcmApi.get(adminToken, "/v1/documents/search?tag_names=" + enc(docTags.get(docAlias)));
        if (!list.body().contains(d.id()) || !tag.body().contains(d.id())) {
            throw new AssertionError("Kontrol (pozitif) başarısız - admin belgeyi bulamadı, negatif test anlamsız olurdu:\n"
                    + list.describe() + "\n" + tag.describe());
        }
    }

    @Then("{string} belgesi {string} kullanıcısının belge listesinde görünmemelidir")
    public void listedeGorunmez(String docAlias, String userAlias) {
        Doc d = doc(docAlias);
        Response r = EcmApi.get(user(userAlias).token(), "/v1/documents?title=" + enc(d.title()));
        if (r.status() != 200 || r.body().contains(d.id())) {
            throw new AssertionError("Belge kullanıcının listesinde görünmemeliydi: " + r.describe());
        }
    }

    private List<Response> listAndSearchResponses(String token, Doc d) {
        List<Response> out = new ArrayList<>();
        out.add(EcmApi.get(token, "/v1/documents?title=" + enc(d.title())));
        out.add(EcmApi.get(token, "/v1/documents?limit=200"));
        out.add(EcmApi.get(token, "/v1/documents?include_deleted=true&limit=200"));
        if (docTags.containsKey(nameOf(d))) {
            out.add(EcmApi.get(token, "/v1/documents/search?tag_names=" + enc(docTags.get(nameOf(d)))));
            out.add(EcmApi.get(token, "/v1/documents/search?mode=all&tag_names=" + enc(docTags.get(nameOf(d)))));
        }
        out.add(EcmApi.get(token, "/v1/documents/title-match?folder_id=" + d.folderId() + "&title=" + enc(d.title())));
        // STR-10 (Batch 34): seçici/ağaç süreleri yalnız kanıt olarak kaydedilir (eşik PERF-01 / RQ-6);
        // BUG-03 yavaşlığı sızıntı sonucunu zaman aşımıyla maskelemesin diye uzun zaman aşımı kullanılır.
        StringBuilder timings = new StringBuilder();
        for (String path : List.of("/v1/folders/picker", "/v1/folders/picker?for=upload", "/v1/folders/tree", "/v1/folders")) {
            long t0 = System.nanoTime();
            out.add(EcmApi.getSlow(token, path));
            timings.append(path).append(" : ").append((System.nanoTime() - t0) / 1_000_000).append(" ms\n");
        }
        io.qameta.allure.Allure.addAttachment("STR-10 kanıtı — klasör seçici süreleri (PERF-01, eşik RQ-6)", "text/plain",
                timings.toString());
        return out;
    }

    private String nameOf(Doc d) {
        return docs.entrySet().stream().filter(e -> e.getValue().equals(d)).map(Map.Entry::getKey).findFirst().orElse("");
    }

    // ---------------------------------------------------------------------
    // UI: izinsiz kullanıcı detay sayfası (B19-02)
    // ---------------------------------------------------------------------

    @When("{string} kullanıcısı arayüzde {string} belgesinin detay adresini açar")
    public void kullaniciArayuzdeAcar(String userAlias, String docAlias) {
        BrowserManager.startBrowser();
        userPage = BrowserManager.createManagerPage();
        User u = user(userAlias);
        BrowserManager.apiLogin(userPage.context(), u.email(), u.password());
        userPage.navigate(BrowserManager.getBaseUrl() + "/documents/" + doc(docAlias).id());
    }

    @Then("arayüzde erişim reddi mesajı görüntülenmeli ve {string} başlığı gösterilmemelidir")
    public void erisimReddiMesaji(String docAlias) {
        try {
            userPage.getByText("You do not have permission to view this document").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            throw new AssertionError("Erişim reddi mesajı görüntülenmedi. URL: " + userPage.url(), e);
        }
        if (!userPage.url().contains("/documents/" + doc(docAlias).id())) {
            throw new AssertionError("Kullanıcı detay adresinde kalmalıydı (oturum düşmemeli). URL: " + userPage.url());
        }
        if (userPage.getByText(doc(docAlias).title()).count() > 0) {
            throw new AssertionError("Gizli belgenin başlığı arayüzde gösterildi: " + doc(docAlias).title());
        }
    }

    // ---------------------------------------------------------------------
    // Yazma denemeleri (B19-09)
    // ---------------------------------------------------------------------

    @Then("{string} kullanıcısının {string} belgesindeki tüm yazma denemeleri reddedilmeli ve belge değişmemelidir")
    public void yazmaDenemeleriReddedilir(String userAlias, String docAlias) {
        String token = user(userAlias).token();
        Doc d = doc(docAlias);
        JsonObject before = adminDocument(d.id());

        Map<String, Response> attempts = new LinkedHashMap<>();
        attempts.put("rename", EcmApi.patch(token, "/v1/documents/" + d.id() + "/title", Map.of("title", d.title() + " hacked")));
        attempts.put("status", EcmApi.patch(token, "/v1/documents/" + d.id() + "/status", Map.of("status", "under_review")));
        attempts.put("tag", EcmApi.post(token, "/v1/documents/" + d.id() + "/tags", Map.of("tag_ids", List.of(), "tag_name", "qab19viewer")));
        attempts.put("new version", newVersion(token, d));
        attempts.put("checkout", EcmApi.post(token, "/v1/documents/" + d.id() + "/checkout", Map.of()));
        attempts.put("soft delete", EcmApi.delete(token, "/v1/documents/" + d.id()));
        attempts.put("hard delete", EcmApi.delete(token, "/v1/documents/" + d.id() + "/hard"));
        attempts.put("grant acl", EcmApi.grant(token, "documents", d.id(), roleId("manager"), user(userAlias).id(), null, false));

        List<String> allowed = new ArrayList<>();
        attempts.forEach((k, r) -> {
            if (r.ok()) {
                allowed.add(k + ": " + r.describe());
            }
        });

        JsonObject after = adminDocument(d.id());
        List<String> changed = diff(before, after, "title", "status", "is_deleted", "current_version_id");
        String tags = EcmApi.get(adminToken, "/v1/documents/" + d.id() + "/tags").body();
        if (tags.contains("qab19viewer")) {
            changed.add("tag eklendi");
        }
        if (!allowed.isEmpty() || !changed.isEmpty()) {
            throw new AssertionError("Viewer yetkisiyle yazma işlemi gerçekleşti.\nİzin verilenler: " + allowed
                    + "\nDeğişen alanlar: " + changed);
        }
    }

    private Response newVersion(String token, Doc d) {
        byte[] bytes = ("new version by " + token.hashCode()).getBytes(StandardCharsets.UTF_8);
        List<Integer> content = new ArrayList<>();
        for (byte b : bytes) {
            content.add(b & 0xff);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", d.title());
        body.put("metadata", Map.of());
        body.put("mime_type", "text/plain");
        body.put("original_filename", "b19-v2.txt");
        body.put("document_id", d.id());
        body.put("version_type", "Patch");
        body.put("folder_id", d.folderId());
        body.put("content", content);
        return EcmApi.post(token, "/v1/documents/unclassified", body);
    }

    // ---------------------------------------------------------------------
    // Kullanıcı yaşam döngüsü (B19-11..13)
    // ---------------------------------------------------------------------

    @Then("{string} kullanıcısının açık oturumu geçerli olmalıdır")
    public void oturumGecerli(String userAlias) {
        EcmApi.get(user(userAlias).token(), "/v1/auth/me").expectOk();
    }

    @When("admin {string} kullanıcısını pasifleştirir")
    public void pasiflestir(String userAlias) {
        EcmApi.put(adminToken, "/v1/users/" + user(userAlias).id(), Map.of("is_active", false)).expectOk();
    }

    @When("admin {string} kullanıcısını aktifleştirir")
    public void aktiflestir(String userAlias) {
        EcmApi.put(adminToken, "/v1/users/" + user(userAlias).id(), Map.of("is_active", true)).expectOk();
    }

    @Then("{string} kullanıcısının açık oturumu hesap pasif hatasıyla reddedilmelidir")
    public void oturumReddedilir(String userAlias) {
        Response r = EcmApi.get(user(userAlias).token(), "/v1/auth/me");
        if (r.status() != 403 || !r.body().contains("deactivated")) {
            throw new AssertionError("Pasif kullanıcının açık token'ı reddedilmeliydi: " + r.describe());
        }
        Response docs = EcmApi.get(user(userAlias).token(), "/v1/documents?limit=1");
        if (docs.ok()) {
            throw new AssertionError("Pasif kullanıcının token'ı belge listesine erişti: " + docs.describe());
        }
    }

    @Then("{string} kullanıcısı giriş yapamamalıdır")
    public void girisYapamaz(String userAlias) {
        User u = user(userAlias);
        Response r = EcmApi.tryLogin(u.email(), u.password());
        if (r.ok()) {
            throw new AssertionError("Pasif kullanıcı giriş yapabildi: " + r.describe());
        }
    }

    @Then("{string} kullanıcısı giriş yapabilmelidir")
    public void girisYapabilir(String userAlias) {
        User u = user(userAlias);
        EcmApi.tryLogin(u.email(), u.password()).expectOk();
    }

    @Then("{string} kullanıcısının tüm yönetici işlemi denemeleri reddedilmelidir")
    public void yoneticiIslemleriReddedilir(String userAlias) {
        User u = user(userAlias);
        String t = u.token();
        String other = user("mehmet").id();
        Map<String, Response> attempts = new LinkedHashMap<>();
        attempts.put("self role=admin", EcmApi.put(t, "/v1/users/" + u.id(), Map.of("role", "admin")));
        attempts.put("deactivate other user", EcmApi.put(t, "/v1/users/" + other, Map.of("is_active", false)));
        attempts.put("create admin user", EcmApi.post(t, "/v1/users", Map.of(
                "email", "qa.b19.evil." + TestData.RUN_ID + "@example.com", "password", "Qa!evil12345", "role", "admin")));
        attempts.put("create group", EcmApi.post(t, "/v1/groups", Map.of("name", TestData.unique("QA B19 evil"))));
        attempts.put("self join group", EcmApi.post(t, "/v1/groups/" + group("Hukuk") + "/members", Map.of("user_id", u.id())));
        attempts.put("create role", EcmApi.post(t, "/v1/rbac/roles", Map.of("name", TestData.unique("QA B19 evil role"))));
        attempts.put("change viewer permissions", EcmApi.put(t, "/v1/rbac/roles/" + roleId("viewer") + "/permissions",
                Map.of("permission_ids", List.of())));
        attempts.put("create class", EcmApi.post(t, "/v1/classes", Map.of("name", TestData.unique("QA B19 evil class"),
                "properties", List.of())));
        attempts.put("create root folder", EcmApi.post(t, "/v1/folders", Map.of("name", TestData.unique("QA B19 evil root"),
                "parent_id", BrowserManager.ROOT_FOLDER_ID, "inherit_permissions", true)));
        attempts.put("audit logs", EcmApi.get(t, "/v1/admin/audit-logs"));
        attempts.put("audit export", EcmApi.get(t, "/v1/admin/audit-logs/export"));
        attempts.put("storage gc", EcmApi.post(t, "/v1/admin/storage-gc", Map.of()));
        attempts.put("list all users", EcmApi.get(t, "/v1/users"));

        List<String> allowed = new ArrayList<>();
        attempts.forEach((k, r) -> {
            if (r.ok()) {
                allowed.add(k + ": " + r.describe());
            }
        });
        if (!allowed.isEmpty()) {
            throw new AssertionError("Normal kullanıcı yönetici işlemi yapabildi:\n" + String.join("\n", allowed));
        }
    }

    @Then("{string} kullanıcısının rolü {string} kalmalı ve {string} grubunda üye olmamalıdır")
    public void rolDegismedi(String userAlias, String role, String groupAlias) {
        JsonObject u = EcmApi.get(adminToken, "/v1/users/" + user(userAlias).id()).expectOk().obj();
        JsonObject userObj = u.has("user") ? u.getAsJsonObject("user") : u;
        if (!role.equals(userObj.get("role").getAsString())) {
            throw new AssertionError("Kullanıcı rolü değişti: " + u);
        }
        String members = EcmApi.get(adminToken, "/v1/groups/" + group(groupAlias) + "/members").expectOk().body();
        if (members.contains(user(userAlias).id())) {
            throw new AssertionError("Kullanıcı kendini gruba ekleyebildi: " + members);
        }
    }

    @Then("{string} kullanıcısı aktif kalmalıdır")
    public void aktifKalir(String userAlias) {
        JsonObject u = EcmApi.get(adminToken, "/v1/users/" + user(userAlias).id()).expectOk().obj();
        JsonObject userObj = u.has("user") ? u.getAsJsonObject("user") : u;
        if (!userObj.get("is_active").getAsBoolean()) {
            throw new AssertionError("Başka bir kullanıcı pasifleştirilebildi: " + u);
        }
    }

    @When("admin {string} kullanıcısının rolünü {string} yapar")
    public void rolDegistir(String userAlias, String role) {
        EcmApi.put(adminToken, "/v1/users/" + user(userAlias).id(), Map.of("role", role)).expectOk();
    }

    @Then("{string} kullanıcısının açık oturumu yönetici işlemi yapabilmelidir")
    public void yoneticiIslemiYapabilir(String userAlias) {
        Response r = EcmApi.get(user(userAlias).token(), "/v1/users?limit=1");
        if (r.status() != 200) {
            throw new AssertionError("Admin rolündeki kullanıcı yönetici listesini açamadı: " + r.describe());
        }
    }

    @Then("{string} kullanıcısının açık oturumu yönetici işlemi yapamamalıdır")
    public void yoneticiIslemiYapamaz(String userAlias) {
        String t = user(userAlias).token();
        Response list = EcmApi.get(t, "/v1/users?limit=1");
        Response create = EcmApi.post(t, "/v1/groups", Map.of("name", TestData.unique("QA B19 demoted")));
        if (list.ok() || create.ok()) {
            throw new AssertionError("Rolü düşürülen kullanıcının açık oturumu hâlâ admin işlemi yapabiliyor:\n"
                    + list.describe() + "\n" + create.describe());
        }
    }

    // ---------------------------------------------------------------------
    // Checkout kilidi (B19-14)
    // ---------------------------------------------------------------------

    @When("{string} kullanıcısı {string} belgesini checkout yapar")
    public void checkout(String userAlias, String docAlias) {
        EcmApi.post(user(userAlias).token(), "/v1/documents/" + doc(docAlias).id() + "/checkout",
                Map.of()).expectOk();
    }

    @Then("admin'in {string} belgesindeki tüm değişiklik denemeleri 409 ile reddedilmeli ve belge değişmemelidir")
    public void adminKilitliBelge(String docAlias) {
        Doc d = doc(docAlias);
        JsonObject before = adminDocument(d.id());
        Map<String, Response> attempts = new LinkedHashMap<>();
        attempts.put("rename", EcmApi.patch(adminToken, "/v1/documents/" + d.id() + "/title", Map.of("title", d.title() + " admin")));
        attempts.put("status", EcmApi.patch(adminToken, "/v1/documents/" + d.id() + "/status", Map.of("status", "under_review")));
        attempts.put("tag", EcmApi.post(adminToken, "/v1/documents/" + d.id() + "/tags", Map.of("tag_ids", List.of(), "tag_name", "qab19lock")));
        attempts.put("new version", newVersion(adminToken, d));
        attempts.put("soft delete", EcmApi.delete(adminToken, "/v1/documents/" + d.id()));
        List<String> wrong = new ArrayList<>();
        attempts.forEach((k, r) -> {
            if (r.status() != 409) {
                wrong.add(k + ": " + r.describe());
            }
        });
        List<String> changed = diff(before, adminDocument(d.id()), "title", "status", "is_deleted", "current_version_id");
        if (!wrong.isEmpty() || !changed.isEmpty()) {
            throw new AssertionError("Kilitli belgede admin değişikliği 409 ile reddedilmedi.\n" + String.join("\n", wrong)
                    + "\nDeğişen alanlar: " + changed);
        }
    }

    @Then("admin {string} belgesinin kopyasını oluşturabilmelidir")
    public void adminKopyalar(String docAlias) {
        Doc d = doc(docAlias);
        Response r = EcmApi.post(adminToken, "/v1/documents/" + d.id() + "/copy",
                Map.of("target_folder_id", d.folderId(), "new_title", d.title() + " kopya"));
        if (!r.ok()) {
            throw new AssertionError("Copy kilitten muaf olmalıydı (ROLES.md): " + r.describe());
        }
    }

    @When("admin {string} belgesinin kilidini zorla kaldırır")
    public void kilitKaldir(String docAlias) {
        EcmApi.delete(adminToken, "/v1/documents/" + doc(docAlias).id() + "/lock").expectOk();
    }

    @Then("admin {string} belgesini yeniden adlandırabilmelidir")
    public void adminYenidenAdlandirir(String docAlias) {
        yenidenAdlandirabilir(adminToken, doc(docAlias), "admin");
    }

    // ---------------------------------------------------------------------
    // Klasör sahipliği (B19-15..17)
    // ---------------------------------------------------------------------

    @Then("{string} kullanıcısı {string} belgesini yeniden adlandıramamalıdır")
    public void yenidenAdlandiramaz(String userAlias, String docAlias) {
        Doc d = doc(docAlias);
        Response r = EcmApi.patch(user(userAlias).token(), "/v1/documents/" + d.id() + "/title", Map.of("title", d.title() + " x"));
        expectDenied(r, "Admin belgesi admin olmayan klasör sahibi tarafından değiştirilmemeliydi");
        String title = adminDocument(d.id()).get("title").getAsString();
        if (!d.title().equals(title)) {
            throw new AssertionError("Başlık değişti: " + title);
        }
    }

    @Then("{string} kullanıcısı {string} belgesini yeniden adlandırabilmelidir")
    public void kullaniciYenidenAdlandirir(String userAlias, String docAlias) {
        yenidenAdlandirabilir(user(userAlias).token(), doc(docAlias), userAlias);
    }

    private void yenidenAdlandirabilir(String token, Doc d, String who) {
        String newTitle = d.title() + " R";
        Response r = EcmApi.patch(token, "/v1/documents/" + d.id() + "/title", Map.of("title", newTitle));
        if (!r.ok()) {
            throw new AssertionError(who + " belgeyi yeniden adlandırabilmeliydi: " + r.describe());
        }
        String persisted = adminDocument(d.id()).get("title").getAsString();
        if (!newTitle.equals(persisted)) {
            throw new AssertionError("Yeni başlık kalıcı değil: " + persisted);
        }
        docs.put(nameOf(d), new Doc(d.id(), newTitle, d.folderId()));
    }

    @Then("{string} kullanıcısı {string} belgesini kalıcı olarak silememeli ve belge mevcut kalmalıdır")
    public void hardDeleteYapamaz(String userAlias, String docAlias) {
        Doc d = doc(docAlias);
        Response r = EcmApi.delete(user(userAlias).token(), "/v1/documents/" + d.id() + "/hard");
        expectDenied(r, "Hard delete yalnızca admin'e açık olmalı (ROLES.md)");
        EcmApi.get(adminToken, "/v1/documents/" + d.id()).expectOk();
    }

    @When("{string} kullanıcısı {string} klasöründe {string} grubuna {string} rolüyle izin vermeyi dener")
    public void sahipGrubaIzin(String userAlias, String folderAlias, String groupAlias, String role) {
        last = EcmApi.grant(user(userAlias).token(), "folders", folder(folderAlias), roleId(role), null, group(groupAlias), false);
    }

    @When("{string} kullanıcısı {string} klasöründe admin kullanıcısına {string} rolüyle izin vermeyi dener")
    public void sahipAdmineIzin(String userAlias, String folderAlias, String role) {
        last = EcmApi.grant(user(userAlias).token(), "folders", folder(folderAlias), roleId(role), adminId, null, false);
    }

    @Then("istek {int} ile reddedilmelidir")
    public void istekReddedilir(int status) {
        if (last.status() != status) {
            throw new AssertionError("Beklenen HTTP " + status + ": " + last.describe());
        }
    }

    @Then("istek reddedilmelidir")
    public void istekReddedilirGenel() {
        if (last.ok()) {
            throw new AssertionError("İstek reddedilmeliydi: " + last.describe());
        }
    }

    @Then("istek başarılı olmalıdır")
    public void istekBasarili() {
        last.expectOk();
    }

    @Then("{string} klasörünün ACL listesinde {string} grubu bulunmamalıdır")
    public void aclGrupYok(String folderAlias, String groupAlias) {
        String acl = EcmApi.get(adminToken, "/v1/folders/" + folder(folderAlias) + "/acl").expectOk().body();
        if (acl.contains(group(groupAlias))) {
            throw new AssertionError("Reddedilen grup yetkisi ACL'e yazılmış: " + acl);
        }
    }

    @Then("{string} klasörünün ACL listesinde admin kullanıcısı bulunmamalıdır")
    public void aclAdminYok(String folderAlias) {
        String acl = EcmApi.get(adminToken, "/v1/folders/" + folder(folderAlias) + "/acl").expectOk().body();
        if (acl.contains(adminId)) {
            throw new AssertionError("Admin kullanıcısını hedefleyen bireysel ACL satırı yazılmış: " + acl);
        }
    }

    // ---------------------------------------------------------------------
    // Statü durum makinesi (B19-18..20)
    // ---------------------------------------------------------------------

    @Then("belge statü geçişleri aşağıdaki sonuçları vermelidir:")
    public void statuGecisleri(DataTable table) {
        List<String> failures = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            String from = row.get("başlangıç");
            String to = row.get("hedef");
            int expected = Integer.parseInt(row.get("sonuç"));
            String id = EcmApi.createDocument(adminToken, TestData.unique("QA B19 Status " + from + "-" + to), null, "status");
            statuYolu(id, from);
            Response r = EcmApi.patch(adminToken, "/v1/documents/" + id + "/status", Map.of("status", to));
            String finalStatus = adminDocument(id).get("status").getAsString();
            String expectedFinal = expected == 200 ? to : from;
            if (r.status() != expected || !expectedFinal.equals(finalStatus)) {
                failures.add(from + " -> " + to + ": beklenen HTTP " + expected + " / statü " + expectedFinal
                        + ", gerçek: " + r.describe() + " / statü " + finalStatus);
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Statü geçiş kuralı ihlali:\n" + String.join("\n", failures));
        }
    }

    // Belgeyi draft'tan istenen başlangıç statüsüne yasal geçişlerle götürür.
    private void statuYolu(String id, String target) {
        List<String> path = switch (target) {
            case "draft" -> List.of();
            case "under_review" -> List.of("under_review");
            case "published" -> List.of("published");
            case "archived" -> List.of("archived");
            default -> throw new IllegalArgumentException("Bilinmeyen statü: " + target);
        };
        for (String s : path) {
            EcmApi.patch(adminToken, "/v1/documents/" + id + "/status", Map.of("status", s)).expectOk();
        }
    }

    @Given("admin arşivlenmiş yeni bir belgenin detay sayfasındadır")
    public void arsivlenmisBelgeDetay() {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(ADMIN_EMAIL, ADMIN_PASSWORD);
        adminToken = BrowserManager.getAccessToken();
        archivedDocId = EcmApi.createDocument(adminToken, TestData.unique("QA B19 Archived"), null, "archived");
        EcmApi.patch(adminToken, "/v1/documents/" + archivedDocId + "/status", Map.of("status", "archived")).expectOk();
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + archivedDocId);
        statusSelect(page).waitFor(new Locator.WaitForOptions().setTimeout(15000));
    }

    private static Locator statusSelect(Page page) {
        return page.locator("select").filter(new Locator.FilterOptions()
                .setHas(page.locator("option[value='under_review']"))).first();
    }

    @When("admin arayüzde statüyü {string} olarak güncellemeyi dener")
    public void arayuzdeStatuGuncelle(String label) {
        Page page = BrowserManager.page;
        statusSelect(page).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(label));
        com.microsoft.playwright.Response r = page.waitForResponse(
                x -> x.url().contains("/v1/documents/" + archivedDocId + "/status") && "PATCH".equals(x.request().method()),
                new Page.WaitForResponseOptions().setTimeout(15000),
                () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update status").setExact(true)).click()
        );
        // Kanıt: arayüzün gönderdiği gerçek istek ve ECM yanıtı.
        System.out.println("[EVIDENCE] UI PATCH " + r.url() + " body=" + r.request().postData()
                + " -> HTTP " + r.status() + " " + r.text());
    }

    @Then("arayüzde geçersiz statü geçişi hatası görüntülenmelidir")
    public void statuHatasiGorunur() {
        try {
            BrowserManager.page.getByText("Illegal status transition").first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            throw new AssertionError("Geçersiz statü geçişi hatası arayüzde görüntülenmedi", e);
        }
    }

    @Then("belgenin API statüsü {string} kalmalıdır")
    public void apiStatusu(String status) {
        String s = adminDocument(archivedDocId).get("status").getAsString();
        if (!status.equals(s)) {
            throw new AssertionError("Belge statüsü değişti: " + s);
        }
    }

    @Then("statü seçiminde yalnızca {string} seçenekleri sunulmalıdır")
    public void statuSecenekleri(String expectedCsv) {
        Set<String> expected = new TreeSet<>(List.of(expectedCsv.split("\\s*,\\s*")));
        Set<String> actual = new TreeSet<>();
        Locator options = statusSelect(BrowserManager.page).locator("option");
        for (int i = 0; i < options.count(); i++) {
            if (!options.nth(i).isDisabled()) {
                actual.add(options.nth(i).innerText().trim());
            }
        }
        if (!expected.equals(actual)) {
            throw new AssertionError("Arşivlenmiş belgede sunulan statü seçenekleri " + actual
                    + "; ECM durum makinesine göre (engine/documents/admin.rs is_legal_transition) yalnızca "
                    + expected + " geçerli. Diğer seçenekler her zaman 422 ile reddedilir.");
        }
    }

    // ---------------------------------------------------------------------
    // Sınıf / doğrulama (B19-10, B19-21, B19-22)
    // ---------------------------------------------------------------------

    @Given("admin zorunlu metin alanı {string} olan {string} sınıfını oluşturmuştur")
    public void basitSinif(String key, String alias) {
        sinifOlustur(alias, List.of(prop(key, "text", true)));
    }

    @Given("admin {string} sınıfını zorunlu metin alanı {string} ve sayısal alan {string} ile oluşturmuştur")
    public void ikiAlanliSinif(String alias, String textKey, String numKey) {
        sinifOlustur(alias, List.of(prop(textKey, "text", true), prop(numKey, "numeric", false)));
    }

    private void sinifOlustur(String alias, List<Map<String, Object>> props) {
        String name = TestData.unique("QA B19 " + alias);
        String id = EcmApi.post(adminToken, "/v1/classes", Map.of("name", name, "description", "B19", "properties", props))
                .expectOk().str("id");
        classes.put(alias, new Cls(id, name));
    }

    private static Map<String, Object> prop(String key, String type, boolean required) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("key", key);
        p.put("display_name", key);
        p.put("data_type", type);
        p.put("is_multi_value", false);
        p.put("required", required);
        return p;
    }

    @Given("admin {string} sınıfının {string} alanına {string} regex kuralı eklemiştir")
    public void regexKurali(String classAlias, String key, String pattern) {
        kuralEkle(classAlias, key, "regex", Map.of("pattern", pattern, "full_match", true), key + " format hatalı");
    }

    @Given("admin {string} sınıfının {string} alanına en küçük değer {int} kuralı eklemiştir")
    public void minKurali(String classAlias, String key, int min) {
        kuralEkle(classAlias, key, "min_value", min, key + " en az " + min + " olmalı");
    }

    private void kuralEkle(String classAlias, String key, String type, Object value, String message) {
        String propId = propertyId(classAlias, key);
        EcmApi.post(adminToken, "/v1/classes/" + cls(classAlias).id() + "/properties/" + propId + "/rules",
                Map.of("constraint_type", type, "constraint_value", value, "error_message", message)).expectOk();
    }

    @When("admin {string} belgesini {string} sınıfına geçerli metadata ile sınıflandırır")
    @Given("admin {string} belgesini {string} sınıfına geçerli metadata ile sınıflandırmıştır")
    public void gecerliSiniflandir(String docAlias, String classAlias) {
        siniflandir(docAlias, classAlias, "{\"ref\": \"B19-REF\"}").expectOk();
    }

    @When("^admin \"([^\"]*)\" belgesini \"([^\"]*)\" sınıfına (\\{.*\\}) metadata ile sınıflandırır$")
    public void metadataIleSiniflandir(String docAlias, String classAlias, String json) {
        siniflandir(docAlias, classAlias, json).expectOk();
    }

    private Response siniflandir(String docAlias, String classAlias, String metadataJson) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("class_id", cls(classAlias).id());
        body.put("class_version", classVersion(classAlias));
        body.put("metadata", new com.google.gson.Gson().fromJson(metadataJson, Map.class));
        last = EcmApi.patch(adminToken, "/v1/documents/" + doc(docAlias).id() + "/class", body);
        return last;
    }

    @Then("{string} belgesinin {string} sınıfına sınıflandırması aşağıdaki metadata ile reddedilmelidir:")
    public void gecersizMetadata(String docAlias, String classAlias, DataTable table) {
        List<String> failures = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            Response r = siniflandir(docAlias, classAlias, row.get("metadata"));
            String code = row.get("hata kodu");
            if (r.status() != 422 || !code.equals(r.errorCode())) {
                failures.add(row.get("metadata") + " -> beklenen 422/" + code + ", gerçek: " + r.describe());
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Geçersiz metadata doğrulaması beklendiği gibi değil:\n" + String.join("\n", failures));
        }
    }

    @Then("{string} belgesi sınıfsız kalmalıdır")
    public void sinifsizKalir(String docAlias) {
        JsonElement cls = adminDocument(doc(docAlias).id()).get("class_id");
        if (cls != null && !cls.isJsonNull()) {
            throw new AssertionError("Reddedilen sınıflandırma sonrası belge sınıflı: " + cls);
        }
    }

    @Then("{string} belgesi {string} sınıfında ve {string} değeri {string} olarak kalıcı olmalıdır")
    public void sinifDegeriKalici(String docAlias, String classAlias, String key, String value) {
        JsonObject d = adminDocument(doc(docAlias).id());
        String classId = d.get("class_id").isJsonNull() ? null : d.get("class_id").getAsString();
        JsonElement v = d.getAsJsonObject("metadata").get(key);
        if (!cls(classAlias).id().equals(classId) || v == null || !value.equals(v.getAsString())) {
            throw new AssertionError("Sınıflandırma kalıcı değil: class_id=" + classId + " metadata=" + d.get("metadata"));
        }
    }

    @When("admin {string} sınıfını pasifleştirir")
    public void sinifPasif(String classAlias) {
        EcmApi.post(adminToken, "/v1/classes/" + cls(classAlias).id() + "/active",
                Map.of("expected_version", classVersion(classAlias), "is_active", false)).expectOk();
    }

    @Then("{string} belgesi {string} sınıfına sınıflandırılamamalı ve sınıfsız kalmalıdır")
    public void pasifSinifaSiniflandirilamaz(String docAlias, String classAlias) {
        Response r = siniflandir(docAlias, classAlias, "{\"ref\": \"B19-REF\"}");
        if (r.status() != 422 || !r.body().contains("inactive")) {
            throw new AssertionError("Pasif sınıfa sınıflandırma reddedilmeliydi: " + r.describe());
        }
        sinifsizKalir(docAlias);
    }

    @Then("{string} sınıfı silinememeli ve {int} çakışma hatası dönmelidir")
    public void sinifSilinemez(String classAlias, int status) {
        Response r = EcmApi.delete(adminToken, "/v1/classes/" + cls(classAlias).id() + "?expected_version=" + classVersion(classAlias));
        if (r.status() != status) {
            throw new AssertionError("Belgesi olan sınıf silinmemeliydi: " + r.describe());
        }
        EcmApi.get(adminToken, "/v1/classes/" + cls(classAlias).id()).expectOk();
    }

    @Then("{string} sınıfı silinebilmeli ve sonrasında bulunamamalıdır")
    public void sinifSilinir(String classAlias) {
        EcmApi.delete(adminToken, "/v1/classes/" + cls(classAlias).id() + "?expected_version=" + classVersion(classAlias))
                .expectOk();
        Response r = EcmApi.get(adminToken, "/v1/classes/" + cls(classAlias).id());
        if (r.status() != 404) {
            throw new AssertionError("Silinen sınıf hâlâ bulunuyor: " + r.describe());
        }
    }

    private long classVersion(String classAlias) {
        return EcmApi.get(adminToken, "/v1/classes/" + cls(classAlias).id()).expectOk().obj().get("version").getAsLong();
    }

    private String propertyId(String classAlias, String key) {
        JsonArray props = EcmApi.get(adminToken, "/v1/classes/" + cls(classAlias).id()).expectOk().obj().getAsJsonArray("properties");
        for (JsonElement p : props) {
            if (key.equals(p.getAsJsonObject().get("key").getAsString())) {
                return p.getAsJsonObject().get("id").getAsString();
            }
        }
        throw new IllegalStateException("Sınıfta özellik yok: " + key);
    }

    // ---------------------------------------------------------------------
    // Kaydedilmiş arama (B19-23)
    // ---------------------------------------------------------------------

    @When("{string} kullanıcısı {string} adlı kaydedilmiş aramayı oluşturur")
    public void kayitliAramaOlustur(String userAlias, String name) {
        String id = EcmApi.post(user(userAlias).token(), "/v1/saved-searches",
                Map.of("name", TestData.unique("QA B19 " + name), "filter", Map.of("title", "b19"))).expectOk().str("id");
        savedSearches.put(name, id);
    }

    @Then("{string} kullanıcısı {string} aramasını göremez, listesinde bulamaz ve silemez")
    public void aramayiGoremez(String userAlias, String name) {
        String t = user(userAlias).token();
        String id = savedSearches.get(name);
        Response get = EcmApi.get(t, "/v1/saved-searches/" + id);
        Response list = EcmApi.get(t, "/v1/saved-searches");
        Response del = EcmApi.delete(t, "/v1/saved-searches/" + id);
        List<String> leaks = new ArrayList<>();
        if (get.ok()) {
            leaks.add(get.describe());
        }
        if (!list.ok() || list.body().contains(id)) {
            leaks.add(list.describe());
        }
        if (del.ok()) {
            leaks.add(del.describe());
        }
        if (!leaks.isEmpty()) {
            throw new AssertionError("Başka kullanıcının kaydedilmiş aramasına erişildi:\n" + String.join("\n", leaks));
        }
    }

    @Then("{string} kullanıcısı {string} aramasını hâlâ görebilmelidir")
    public void aramayiGorur(String userAlias, String name) {
        EcmApi.get(user(userAlias).token(), "/v1/saved-searches/" + savedSearches.get(name)).expectOk();
    }

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------

    private JsonObject adminDocument(String id) {
        return EcmApi.get(adminToken, "/v1/documents/" + id + "?include_deleted=true").expectOk().obj().getAsJsonObject("document");
    }

    private static List<String> diff(JsonObject before, JsonObject after, String... fields) {
        List<String> out = new ArrayList<>();
        for (String f : fields) {
            String b = String.valueOf(before.get(f));
            String a = String.valueOf(after.get(f));
            if (!b.equals(a)) {
                out.add(f + ": " + b + " -> " + a);
            }
        }
        return out;
    }

    private static void expectDenied(Response r, String message) {
        if (r.status() != 403 && r.status() != 404) {
            throw new AssertionError(message + ": " + r.describe());
        }
    }

    private String roleId(String name) {
        if (roleIds == null) {
            roleIds = new HashMap<>();
            for (JsonElement e : EcmApi.get(adminToken, "/v1/rbac/roles").expectOk().obj().getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                if (o.get("is_system_role").getAsBoolean()) {
                    roleIds.put(o.get("name").getAsString(), o.get("id").getAsString());
                }
            }
        }
        String id = roleIds.get(name);
        if (id == null) {
            throw new IllegalStateException("Sistem rolü bulunamadı: " + name + " " + roleIds.keySet());
        }
        return id;
    }

    private User user(String alias) {
        return require(users, alias, "kullanıcı");
    }

    private String folder(String alias) {
        return require(folders, alias, "klasör");
    }

    private Doc doc(String alias) {
        return require(docs, alias, "belge");
    }

    private String group(String alias) {
        return require(groups, alias, "grup");
    }

    private Cls cls(String alias) {
        return require(classes, alias, "sınıf");
    }

    private static <T> T require(Map<String, T> map, String alias, String kind) {
        T v = map.get(alias);
        if (v == null) {
            throw new IllegalStateException("Senaryoda tanımlanmamış " + kind + ": " + alias);
        }
        return v;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static JsonElement parse(String s) {
        return JsonParser.parseString(s);
    }
}
