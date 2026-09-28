package steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/*
 * Phase C (second-pass coverage gaps) — Batch C3 (P2) and C4 (P3).
 * Every expectation cites the ECM source rule in the feature comment.
 */
public class SecondPassMoreSteps {

    private static final int UI_WAIT_MS = 30000;
    private String marker;
    private String auditIdForImmutability;
    private JsonObject auditBefore;

    private static EcmApiClient.Session session(String alias) {
        return SecondPassSteps.session(alias);
    }

    private static String enc(String s) {
        return SecondPassSteps.enc(s);
    }

    private static List<String> failures() {
        return new ArrayList<>();
    }

    private static void check(List<String> failures, String what, EcmApiClient.ApiResponse r, int status, String fragment) {
        if (r.status() != status || (!fragment.isEmpty() && !r.errorMessage().contains(fragment))) {
            failures.add(what + ": beklenen " + status + " '" + fragment + "', gelen " + r.status() + " '" + r.errorMessage() + "'");
        }
    }

    private static void fail(List<String> failures, String title) {
        if (!failures.isEmpty()) {
            throw new AssertionError(title + ":\n  " + String.join("\n  ", failures));
        }
    }

    // =====================================================================
    // NEW-07 — free-form metadata + date filters; malformed filters rejected
    // (README GET /documents: meta [{key, value?, op?}] eq|contains, created_/updated_ after|before ISO-8601;
    //  documents_query.rs meta/props JSON + blank key 422; postgres documents.rs:1162-1220; saved_searches.rs:35)
    // =====================================================================

    @Given("yönetici {string} klasörüne {string} serbest metadatasıyla {string} belgesini yüklemiştir")
    public void metadataliBelge(String folderAlias, String metaJson, String documentAlias) {
        String title = TestDataFactory.unique("QA Metadata " + documentAlias);
        Map<String, Object> body = DocumentOperationsSteps.unclassifiedBody(title, ApiTestContext.id(folderAlias), "text/plain", "meta.txt",
                ("QA metadata " + title).getBytes(StandardCharsets.UTF_8));
        body.put("metadata", new com.google.gson.Gson().fromJson(metaJson, Map.class));
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified", body);
        r.expect(201, "Metadatalı belge (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
    }

    private static TreeSet<String> listAliases(String folderAlias, String extra) {
        TreeSet<String> out = new TreeSet<>();
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&folder_id=" + ApiTestContext.id(folderAlias) + "&" + extra)
                .expect(200, "Filtreli liste (" + extra + ")").json().getAsJsonArray("items")) {
            out.add(ApiTestContext.aliasOfId(e.getAsJsonObject().get("id").getAsString()));
        }
        return out;
    }

    @Then("{string} klasöründe metadata filtreleri tam olarak şu belgeleri döndürmelidir:")
    public void metadataFiltreleri(String folderAlias, DataTable table) {
        List<String> failures = failures();
        StringBuilder ev = new StringBuilder();
        for (Map<String, String> row : table.asMaps()) {
            String filter = row.get("meta filtresi").trim();
            TreeSet<String> expected = new TreeSet<>();
            for (String a : row.get("beklenen").split("\\s*,\\s*")) {
                if (!a.isBlank() && !a.equals("-")) {
                    expected.add(a.trim());
                }
            }
            TreeSet<String> got = listAliases(folderAlias, "meta=" + enc(filter));
            ev.append(filter).append(" -> ").append(got).append('\n');
            if (!got.equals(expected)) {
                failures.add(filter + ": beklenen " + expected + ", gelen " + got);
            }
        }
        Allure.addAttachment("NEW-07 kanıtı — metadata filtreleri", "text/plain", ev.toString());
        fail(failures, "Metadata filtreleri yanlış sonuç verdi");
    }

    @Then("{string} klasöründe tarih filtreleri {string} belgesinin oluşturulma ve {string} belgesinin güncellenme zamanına göre doğru sonuç vermelidir")
    public void tarihFiltreleri(String folderAlias, String firstAlias, String updatedAlias) {
        JsonObject first = SecondPassSteps.read(firstAlias).getAsJsonObject("document");
        JsonObject upd = SecondPassSteps.read(updatedAlias).getAsJsonObject("document");
        String firstCreated = first.get("created_at").getAsString();
        String updatedAt = upd.get("updated_at").getAsString();
        TreeSet<String> all = listAliases(folderAlias, "");
        List<String> failures = failures();
        StringBuilder ev = new StringBuilder("first.created_at=" + firstCreated + "\nupdated.updated_at=" + updatedAt + "\nall=" + all + "\n");
        Map<String, TreeSet<String>> cases = new LinkedHashMap<>();
        cases.put("created_after=" + enc(firstCreated), all);
        cases.put("created_before=" + enc("2000-01-01T00:00:00Z"), new TreeSet<>());
        cases.put("created_after=" + enc("2999-01-01T00:00:00Z"), new TreeSet<>());
        cases.put("updated_after=" + enc(updatedAt), new TreeSet<>(List.of(updatedAlias)));
        for (Map.Entry<String, TreeSet<String>> c : cases.entrySet()) {
            TreeSet<String> got = listAliases(folderAlias, c.getKey());
            ev.append(c.getKey()).append(" -> ").append(got).append('\n');
            if (!got.equals(c.getValue())) {
                failures.add(c.getKey() + ": beklenen " + c.getValue() + ", gelen " + got);
            }
        }
        Allure.addAttachment("NEW-07 kanıtı — tarih filtreleri", "text/plain", ev.toString());
        fail(failures, "Tarih filtreleri yanlış sonuç verdi");
    }

    @Then("hatalı liste filtreleri doğrulama hatasıyla reddedilmeli ve sunucu hatası vermemelidir:")
    public void hataliFiltreler(DataTable table) {
        List<String> failures = failures();
        StringBuilder ev = new StringBuilder();
        for (Map<String, String> row : table.asMaps()) {
            String qs = ApiTestContext.resolve(row.get("sorgu").trim());
            EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?" + qs);
            ev.append(qs).append(" -> ").append(r.status()).append(" ").append(r.errorMessage()).append('\n');
            String frag = row.get("mesaj").trim();
            if (r.status() >= 500) {
                failures.add(qs + " -> sunucu hatası HTTP " + r.status());
            } else if (r.status() < 400 || r.status() >= 500 || (!frag.equals("-") && !r.errorMessage().contains(frag))) {
                failures.add(qs + " -> beklenen 4xx '" + frag + "', gelen " + r.status() + " '" + r.errorMessage() + "'");
            }
        }
        Allure.addAttachment("NEW-07 kanıtı — hatalı filtreler", "text/plain", ev.toString());
        fail(failures, "Hatalı filtreler doğru reddedilmedi");
    }

    @Then("geçersiz durum içeren kayıtlı arama filtresi {int} ile reddedilmeli ve kayıtlı arama oluşmamalıdır")
    public void gecersizKayitliArama(int status) {
        String name = TestDataFactory.unique("QA Geçersiz Filtre");
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/saved-searches",
                Map.of("name", name, "filter", Map.of("status", "bogus")));
        String list = EcmApiClient.get(TestDataFactory.admin(), "/v1/saved-searches").expect(200, "Kayıtlı aramalar").body();
        Allure.addAttachment("NEW-07 kanıtı — kayıtlı arama", "text/plain", r.status() + " " + r.errorMessage());
        if (r.status() >= 200 && r.status() < 300) {
            String id = r.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/saved-searches/" + id));
        }
        if (r.status() != status || !r.errorMessage().contains("Invalid status 'bogus' in filter") || list.contains(name)) {
            throw new AssertionError("Geçersiz durumlu kayıtlı arama " + status + " ile reddedilmeli ve saklanmamalıydı: HTTP " + r.status()
                    + " '" + r.errorMessage() + "', listede=" + list.contains(name));
        }
    }

    // =====================================================================
    // NEW-14 — classified upload payload validation (property_value_codec.rs "property '{key}' {msg}";
    // doc_management_engine.rs:280 "metadata must be a JSON object"; api/rest/documents.rs:71 stream metadata JSON)
    // =====================================================================

    @Given("benzersiz bir yükleme işareti belirlenmiştir")
    public void isaret() {
        marker = "qaPC" + System.nanoTime();
    }

    @Then("{string} sınıfıyla şu metadata gövdeleriyle yükleme reddedilmeli ve hiçbir belge oluşmamalıdır:")
    public void sinifliHataliGovdeler(String classAlias, DataTable table) {
        List<String> failures = failures();
        StringBuilder ev = new StringBuilder();
        for (Map<String, String> row : table.asMaps()) {
            String meta = row.get("metadata").trim();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("title", marker + " " + row.get("durum").trim() + " " + ev.length());
            body.put("metadata", JsonParser.parseString(meta).isJsonObject()
                    ? new com.google.gson.Gson().fromJson(meta, Map.class)
                    : new com.google.gson.Gson().fromJson(meta, List.class));
            body.put("mime_type", "text/plain");
            body.put("original_filename", "tip.txt");
            body.put("folder_id", BrowserManager.ROOT_FOLDER_ID);
            body.put("content", List.of(113, 97));
            body.put("class_id", ApiTestContext.id(classAlias));
            body.put("class_version", ClassAdminSteps.classVersion(classAlias));
            EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents", body);
            ev.append(meta).append(" -> ").append(r.status()).append(" ").append(r.errorMessage()).append('\n');
            if (r.status() >= 200 && r.status() < 300) {
                String id = r.json().get("document_id").getAsString();
                ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + id + "/hard"));
            }
            check(failures, meta, r, Integer.parseInt(row.get("durum").trim()), row.get("mesaj").trim());
        }
        int leaked = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&title=" + enc(marker)).expect(200, "Liste").json()
                .getAsJsonArray("items").size();
        if (leaked != 0) {
            failures.add("reddedilen yüklemelerden " + leaked + " belge oluştu");
        }
        Allure.addAttachment("NEW-14 kanıtı — sınıflı gövde doğrulaması", "text/plain", ev.toString());
        fail(failures, "Sınıflı yükleme doğrulaması");
    }

    @Then("akış yüklemesinde bozuk metadata JSON {int} ve {string} ile reddedilmeli ve belge oluşmamalıdır")
    public void akisBozukMetadata(int status, String fragment) {
        String title = marker + " akış bozuk";
        byte[] meta = ("{\"title\":\"" + title + "\",\"metadata\":{},\"mime_type\":\"text/plain\",").getBytes(StandardCharsets.UTF_8);
        EcmApiClient.ApiResponse r = EcmApiClient.postMultipart(TestDataFactory.admin(), "/v1/documents/unclassified/stream", List.of(
                new EcmApiClient.Part("metadata", null, "application/json", meta),
                new EcmApiClient.Part("file", "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8))));
        int leaked = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&title=" + enc(marker + " akış")).json().getAsJsonArray("items").size();
        Allure.addAttachment("NEW-14 kanıtı — akış metadata", "text/plain", r.status() + " " + r.errorMessage() + "\nleaked=" + leaked);
        if (r.status() != status || !r.errorMessage().contains(fragment) || leaked != 0) {
            throw new AssertionError("Bozuk akış metadatası " + status + " '" + fragment + "' ile reddedilmeli ve belge oluşmamalıydı: HTTP "
                    + r.status() + " '" + r.errorMessage() + "', oluşan " + leaked);
        }
    }

    // =====================================================================
    // NEW-17 — document/class ACL audit (acl/management/document.rs:92/135, class.rs:90/134:
    // action document|class.acl_grant/revoke, target_type acl, target_id = ACL row, after_state carries the owner id)
    // =====================================================================

    @When("yönetici {string} {string} üzerinde {string} kullanıcısına okuma izni verip kaldırır")
    public void aclVerKaldir(String kind, String resourceAlias, String userAlias) {
        String base = ("belge".equals(kind) ? "/v1/documents/" : "/v1/classes/") + ApiTestContext.id(resourceAlias) + "/acl";
        JsonObject acl = EcmApiClient.post(TestDataFactory.admin(), base,
                Map.of("user_id", ApiTestContext.id(userAlias), "role_id", TestDataFactory.ROLE_VIEWER, "deny", false)).expect(200, "İzin verme").json();
        String aclId = acl.get("id").getAsString();
        ApiTestContext.putId(resourceAlias + "#acl", aclId);
        EcmApiClient.ApiResponse del = EcmApiClient.delete(TestDataFactory.admin(), base + "/" + aclId);
        if (del.status() >= 300) {
            throw new AssertionError("İzin kaldırma HTTP " + del.status() + ": " + del.errorMessage());
        }
    }

    @Then("{string} izni için {string} ve {string} denetim kayıtları yönetici aktörüyle ve {string} bağlamıyla yazılmış olmalıdır")
    public void aclDenetim(String resourceAlias, String grantAction, String revokeAction, String ownerField) {
        String aclId = ApiTestContext.id(resourceAlias + "#acl");
        String admin = TestDataFactory.admin().userId;
        long deadline = System.currentTimeMillis() + 10000;
        List<JsonObject> rows = new ArrayList<>();
        while (true) {
            rows.clear();
            EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?target_id=" + aclId + "&limit=50").expect(200, "Denetim").json()
                    .getAsJsonArray("items").forEach(e -> rows.add(e.getAsJsonObject()));
            boolean g = rows.stream().anyMatch(r -> grantAction.equals(r.get("action").getAsString()));
            boolean v = rows.stream().anyMatch(r -> revokeAction.equals(r.get("action").getAsString()));
            if ((g && v) || System.currentTimeMillis() > deadline) {
                break;
            }
            sleep();
        }
        List<String> failures = failures();
        StringBuilder ev = new StringBuilder();
        for (String action : List.of(grantAction, revokeAction)) {
            JsonObject row = rows.stream().filter(r -> action.equals(r.get("action").getAsString())).findFirst().orElse(null);
            ev.append(action).append(": ").append(row == null ? "YOK" : row.get("target_type") + " actor=" + row.get("actor_email")).append('\n');
            if (row == null) {
                failures.add(action + " kaydı yok");
                continue;
            }
            if (!"acl".equals(row.get("target_type").getAsString()) || !admin.equals(row.get("actor_id").getAsString())) {
                failures.add(action + ": hedef türü acl ve aktör yönetici olmalıydı");
            }
        }
        JsonObject grant = rows.stream().filter(r -> grantAction.equals(r.get("action").getAsString())).findFirst().orElse(null);
        if (grant != null) {
            JsonElement after = grant.get("after_state");
            String expectedOwner = ApiTestContext.id(resourceAlias);
            if (after == null || after.isJsonNull() || !after.toString().contains(expectedOwner) || !after.toString().contains(ownerField)) {
                failures.add(grantAction + " after_state '" + ownerField + "'=" + expectedOwner + " taşımalıydı: " + after);
            }
        }
        Allure.addAttachment("NEW-17 kanıtı — " + resourceAlias, "text/plain", ev.toString());
        fail(failures, "ACL denetim kayıtları");
    }

    private static void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    // =====================================================================
    // NEW-19 — relationship types admin page (README Relationships; RelationshipTypes.tsx; App.tsx AdminGuard)
    // =====================================================================

    private Page adminPage() {
        if (BrowserManager.page == null) {
            BrowserManager.startBrowser();
        }
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        return BrowserManager.page;
    }

    private static JsonObject typeByName(String name) {
        JsonObject list = EcmApiClient.get(TestDataFactory.admin(), "/v1/relationship-types").expect(200, "İlişki türleri").json();
        for (JsonElement e : list.getAsJsonArray("items")) {
            if (name.equals(e.getAsJsonObject().get("name").getAsString())) {
                return e.getAsJsonObject();
            }
        }
        return null;
    }

    @When("yönetici arayüzde ilişki türleri sayfasında {string} türünü yönlü olarak oluşturur")
    public void iliskiTuruOlustur(String alias) {
        Page p = adminPage();
        String name = "qa-" + TestDataFactory.unique("tur").toLowerCase().replace(' ', '-');
        ApiTestContext.putName(alias, name);
        p.navigate(BrowserManager.getBaseUrl() + "/admin/relationship-types");
        p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("New type")).click();
        p.getByPlaceholder("e.g. supersedes").fill(name);
        Locator directed = p.locator("#is-directed");
        if (!directed.isChecked()) {
            directed.check();
        }
        com.microsoft.playwright.Response r = p.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().endsWith("/v1/relationship-types"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> p.locator("form button[type='submit']").first().click());
        if (r.status() >= 300) {
            throw new AssertionError("İlişki türü oluşturma HTTP " + r.status() + ": " + r.text());
        }
        JsonObject created = JsonParser.parseString(r.text()).getAsJsonObject();
        ApiTestContext.putId(alias, created.get("id").getAsString());
        p.getByText(name, new Page.GetByTextOptions().setExact(true)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    @When("yönetici arayüzde {string} türünü yeni bir adla yeniden adlandırır")
    public void iliskiTuruAdlandir(String alias) {
        Page p = BrowserManager.page;
        String newName = ApiTestContext.name(alias) + "-yeni";
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(ApiTestContext.name(alias))).first();
        row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit")).click();
        Locator input = p.locator("tr").filter(new Locator.FilterOptions().setHas(p.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Save").setExact(true)))).first().locator("input").first();
        input.fill(newName);
        com.microsoft.playwright.Response r = p.waitForResponse(resp -> "PUT".equals(resp.request().method()) && resp.url().contains("/v1/relationship-types/"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true)).click());
        if (r.status() >= 300) {
            throw new AssertionError("İlişki türü yeniden adlandırma HTTP " + r.status() + ": " + r.text());
        }
        ApiTestContext.putName(alias + "#new", newName);
    }

    @Then("{string} türü API'de yeni adıyla ve yönlü olarak kayıtlı olmalı, eski adla kayıt kalmamalıdır")
    public void iliskiTuruKalici(String alias) {
        JsonObject t = typeByName(ApiTestContext.name(alias + "#new"));
        if (t == null || !ApiTestContext.id(alias).equals(t.get("id").getAsString()) || !t.get("is_directed").getAsBoolean()
                || t.get("is_system").getAsBoolean() || typeByName(ApiTestContext.name(alias)) != null) {
            throw new AssertionError("İlişki türü API'de yeni adla, yönlü ve sistem dışı olmalıydı; eski ad kalmamalıydı: " + t);
        }
        adminPage().reload();
        BrowserManager.page.getByText(ApiTestContext.name(alias + "#new"), new Page.GetByTextOptions().setExact(true)).first()
                .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("sistem ilişki türü {string} arayüzde salt okunur görünmeli ve düzenleme kontrolü sunulmamalıdır")
    public void sistemTuruSaltOkunur(String systemName) {
        JsonObject t = typeByName(systemName);
        if (t == null || !t.get("is_system").getAsBoolean()) {
            throw new AssertionError("'" + systemName + "' bir sistem ilişki türü olarak bulunmalıydı: " + t);
        }
        Page p = BrowserManager.page;
        Locator row = p.locator("tr").filter(new Locator.FilterOptions().setHasText(systemName)).first();
        row.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        boolean readOnly = row.getByText("Read-only").count() > 0;
        boolean edit = row.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit")).count() > 0;
        if (!readOnly || edit) {
            throw new AssertionError("Sistem türü satırı 'Read-only' göstermeli ve Edit sunmamalıydı (readOnly=" + readOnly + ", edit=" + edit + ")");
        }
    }

    // =====================================================================
    // NEW-22 — upload form rejects empty / blocked file with a visible message, no request, no document
    // (frontend api/documents.ts:88 validateUploadFile; DocumentNewUnclassified.tsx:100)
    // =====================================================================

    @Then("yönetici arayüzde {string} klasörüne {string} dosyasını {string} içerikle yüklemeyi denediğinde {string} mesajını görmeli, istek gönderilmemeli ve belge oluşmamalıdır")
    public void arayuzReddedilenYukleme(String folderAlias, String fileName, String content, String message) throws Exception {
        Page p = adminPage();
        String title = TestDataFactory.unique("QA Reddedilen Yükleme");
        Path dir = Files.createTempDirectory("qa-upload");
        Path file = dir.resolve(fileName);
        Files.write(file, "(boş)".equals(content) ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
        p.navigate(BrowserManager.getBaseUrl() + "/documents/new/unclassified?folder_id=" + ApiTestContext.id(folderAlias));
        p.getByPlaceholder("e.g. Q3 Budget Spreadsheet").fill(title);
        p.locator("input[type='file']").setInputFiles(file);
        List<String> posts = new ArrayList<>();
        java.util.function.Consumer<Request> listener = rq -> {
            if ("POST".equals(rq.method()) && rq.url().contains("/v1/documents")) {
                posts.add(rq.url());
            }
        };
        p.onRequest(listener);
        p.locator("form button[type='submit']").first().click();
        p.getByText(message).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        p.offRequest(listener);
        int created = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&title=" + enc(title)).json().getAsJsonArray("items").size();
        Allure.addAttachment("NEW-22 kanıtı — " + fileName, "text/plain", "mesaj görünür: " + message + "\nPOST=" + posts + "\nURL=" + p.url() + "\noluşan=" + created);
        Files.deleteIfExists(file);
        Files.deleteIfExists(dir);
        if (!posts.isEmpty() || !p.url().contains("/documents/new/unclassified") || created != 0) {
            throw new AssertionError("Reddedilen yükleme istek göndermemeli, sayfada kalmalı ve belge oluşturmamalıydı (POST=" + posts
                    + ", URL=" + p.url() + ", oluşan=" + created + ")");
        }
    }

    // =====================================================================
    // NEW-24 — effective access matches enforcement (README ACL: mutating endpoints honor the same computed
    // RBAC permissions the effective-access engine reports). Editor excluded (BUG-10 regression, ACL-16).
    // =====================================================================

    private static TreeSet<String> effectiveActions(String documentAlias, String userAlias) {
        TreeSet<String> allowed = new TreeSet<>();
        TreeSet<String> denied = new TreeSet<>();
        String uid = ApiTestContext.id(userAlias);
        JsonObject ea = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/effective-access")
                .expect(200, "Etkin erişim").json();
        for (JsonElement e : ea.getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            if (!o.get("principal").getAsString().contains(uid)) {
                continue;
            }
            String a = o.get("action").getAsString();
            if (a.startsWith("document:")) {
                (o.get("deny").getAsBoolean() ? denied : allowed).add(a);
            }
        }
        allowed.removeAll(denied);
        return allowed;
    }

    @Then("{string} için {string} belgesinde etkin erişim okuma={string} yazma={string} bildirmeli ve gerçek okuma ile yeniden adlandırma buna uymalıdır")
    public void etkinErisimUyumu(String userAlias, String documentAlias, String readFlag, String writeFlag) {
        TreeSet<String> reported = effectiveActions(documentAlias, userAlias);
        boolean expectRead = "var".equals(readFlag);
        boolean expectWrite = "var".equals(writeFlag);
        String id = ApiTestContext.id(documentAlias);
        EcmApiClient.Session s = session(userAlias);
        int read = EcmApiClient.get(s, "/v1/documents/" + id).status();
        String before = SecondPassSteps.read(documentAlias).getAsJsonObject("document").get("title").getAsString();
        EcmApiClient.ApiResponse rename = EcmApiClient.patch(s, "/v1/documents/" + id + "/title", Map.of("title", before + " QA"));
        String afterRename = SecondPassSteps.read(documentAlias).getAsJsonObject("document").get("title").getAsString();
        String ev = "reported=" + reported + " expected read=" + expectRead + " write=" + expectWrite + "\nread=" + read + " rename=" + rename.status()
                + " title " + before + " -> " + afterRename;
        Allure.addAttachment("NEW-24 kanıtı — " + userAlias, "text/plain", ev);
        List<String> failures = failures();
        boolean mayRead = reported.contains("document:read");
        boolean mayWrite = reported.contains("document:write");
        if (mayRead != expectRead || mayWrite != expectWrite) {
            failures.add("etkin erişim okuma=" + mayRead + " yazma=" + mayWrite + " bildirdi; senaryo okuma=" + expectRead + " yazma=" + expectWrite + " bekliyor");
        }
        if (mayRead != (read == 200)) {
            failures.add("okuma " + read + " ama etkin erişim document:read=" + mayRead);
        }
        if (mayWrite != (rename.status() == 200)) {
            failures.add("yeniden adlandırma " + rename.status() + " ama etkin erişim document:write=" + mayWrite);
        }
        if (!mayWrite && !before.equals(afterRename)) {
            failures.add("reddedilen yeniden adlandırma başlığı değiştirdi");
        }
        if (mayWrite && rename.status() == 200) {
            EcmApiClient.patch(TestDataFactory.admin(), "/v1/documents/" + id + "/title", Map.of("title", before)).expect(200, "Başlığı geri al");
        }
        fail(failures, "Etkin erişim ile gerçek yetki uyuşmuyor\n" + ev);
    }

    @Given("{string} klasöründe {string} kullanıcısına DENY okuma kuralı verilmiştir")
    public void denyKurali(String folderAlias, String userAlias) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl",
                Map.of("user_id", ApiTestContext.id(userAlias), "role_id", TestDataFactory.ROLE_VIEWER, "deny", true)).expect(200, "DENY kuralı");
    }

    // =====================================================================
    // NEW-26 — restore after folder rename + ACL change (user guide 7.1 restore returns the document;
    // access is folder-chain based; history keeps delete + restore)
    // =====================================================================

    @When("yönetici {string} klasörünü yeniden adlandırır")
    public void klasoruAdlandir(String folderAlias) {
        String name = TestDataFactory.unique("QA Yeni Klasör Adı");
        EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias), Map.of("name", name)).expect(200, "Klasör adı");
        ApiTestContext.putName(folderAlias + "#renamed", name);
    }

    @When("yönetici {string} klasöründe {string} kullanıcısının iznini kaldırır")
    public void izinKaldir(String folderAlias, String userAlias) {
        String uid = ApiTestContext.id(userAlias);
        JsonElement acl = JsonParser.parseString(EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl")
                .expect(200, "Klasör ACL").body());
        JsonArray items = acl.isJsonArray() ? acl.getAsJsonArray() : acl.getAsJsonObject().getAsJsonArray("items");
        for (JsonElement e : items) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("user_id") && !o.get("user_id").isJsonNull() && uid.equals(o.get("user_id").getAsString())) {
                EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl/" + o.get("id").getAsString())
                        .expect(200, "İzin kaldırma");
                return;
            }
        }
        throw new AssertionError("'" + userAlias + "' için klasör izni bulunamadı");
    }

    @Then("geri yüklenen {string} belgesi yeniden adlandırılan {string} klasöründe olmalı, geçmişi silme ve geri yüklemeyi içermelidir")
    public void geriYuklenenKlasorde(String documentAlias, String folderAlias) {
        JsonObject d = SecondPassSteps.read(documentAlias);
        JsonObject f = d.getAsJsonObject("folder");
        List<String> failures = failures();
        if (d.getAsJsonObject("document").get("is_deleted").getAsBoolean()) {
            failures.add("belge hâlâ silinmiş");
        }
        if (!ApiTestContext.id(folderAlias).equals(f.get("id").getAsString()) || !ApiTestContext.name(folderAlias + "#renamed").equals(f.get("name").getAsString())) {
            failures.add("klasör " + f.get("id") + " / " + f.get("name") + " olmalıydı " + ApiTestContext.name(folderAlias + "#renamed"));
        }
        List<String> actions = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10000;
        do {
            actions.clear();
            JsonElement h = JsonParser.parseString(EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/history")
                    .expect(200, "Geçmiş").body());
            (h.isJsonArray() ? h.getAsJsonArray() : h.getAsJsonObject().getAsJsonArray("items"))
                    .forEach(e -> actions.add(e.getAsJsonObject().get("action").getAsString()));
            if (actions.contains("document.soft_delete") && actions.contains("document.restore")) {
                break;
            }
            sleep();
        } while (System.currentTimeMillis() < deadline);
        if (!actions.contains("document.soft_delete") || !actions.contains("document.restore")) {
            failures.add("geçmiş silme ve geri yüklemeyi içermeli: " + actions);
        }
        Allure.addAttachment("NEW-26 kanıtı", "text/plain", "folder=" + f + "\nhistory=" + actions);
        fail(failures, "Geri yükleme sonrası durum");
    }

    // =====================================================================
    // C4: NEW-15 (classify.rs:40, reclassify.rs:44, copy.rs:44), NEW-16 (folders/write.rs:275),
    // NEW-18 (user guide 1.18 immutable audit; routes.rs exposes only GET /admin/audit-logs/{id})
    // =====================================================================

    @Then("durum dışı sınıflandırma, yeniden sınıflandırma ve kopyalama istekleri reddedilmeli ve hiçbir şey değişmemelidir: sınıflı {string}, sınıfsız {string}, sınıf {string}")
    public void durumDisiIstekler(String classifiedAlias, String unclassifiedAlias, String classAlias) {
        String cBefore = SecondPassSteps.classOf(classifiedAlias);
        String uBefore = SecondPassSteps.classOf(unclassifiedAlias);
        String folder = SecondPassSteps.folderOf(classifiedAlias);
        int countBefore = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&folder_id=" + folder).json().getAsJsonArray("items").size();
        Map<String, Object> body = Map.of("class_id", ApiTestContext.id(classAlias), "class_version", ClassAdminSteps.classVersion(classAlias), "metadata", Map.of());
        EcmApiClient.ApiResponse classify = EcmApiClient.patch(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(classifiedAlias) + "/class", body);
        EcmApiClient.ApiResponse reclassify = EcmApiClient.patch(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(unclassifiedAlias) + "/reclassify", body);
        EcmApiClient.ApiResponse copy = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(classifiedAlias) + "/copy",
                Map.of("target_folder_id", folder, "copy_mode", "all_versions"));
        List<String> failures = failures();
        check(failures, "sınıflı belgeye classify", classify, 422, "This document already has a class.");
        check(failures, "sınıfsız belgeye reclassify", reclassify, 422, "This document is unclassified; use classify instead of reclassify.");
        check(failures, "desteklenmeyen copy_mode", copy, 422, "Unsupported copy_mode 'all_versions'");
        int countAfter = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&folder_id=" + folder).json().getAsJsonArray("items").size();
        if (!java.util.Objects.equals(cBefore, SecondPassSteps.classOf(classifiedAlias)) || !java.util.Objects.equals(uBefore, SecondPassSteps.classOf(unclassifiedAlias))
                || countBefore != countAfter) {
            failures.add("reddedilen istekler sınıfı ya da belge sayısını değiştirdi");
        }
        fail(failures, "Durum dışı istekler");
    }

    @Then("{string} klasörüne alansız PATCH isteği {int} ve {string} ile reddedilmeli, klasör değişmemelidir")
    public void bosPatch(String folderAlias, int status, String fragment) {
        JsonObject before = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias)).expect(200, "Klasör").json().getAsJsonObject("folder");
        EcmApiClient.ApiResponse r = EcmApiClient.patch(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias), Map.of());
        JsonObject after = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias)).expect(200, "Klasör").json().getAsJsonObject("folder");
        if (r.status() != status || !r.errorMessage().contains(fragment) || !before.equals(after)) {
            throw new AssertionError("Boş PATCH " + status + " '" + fragment + "' ile reddedilmeli ve klasör değişmemeliydi: HTTP " + r.status()
                    + " '" + r.errorMessage() + "'\nönce=" + before + "\nsonra=" + after);
        }
    }

    @Given("{string} belgesinin bir denetim kaydı seçilmiştir")
    public void denetimKaydiSec(String documentAlias) {
        long deadline = System.currentTimeMillis() + 10000;
        while (auditIdForImmutability == null) {
            JsonArray items = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?target_id=" + ApiTestContext.id(documentAlias) + "&limit=20")
                    .expect(200, "Denetim").json().getAsJsonArray("items");
            if (items.size() > 0) {
                auditIdForImmutability = items.get(0).getAsJsonObject().get("id").getAsString();
            } else if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Belge için denetim kaydı oluşmadı");
            } else {
                sleep();
            }
        }
        auditBefore = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs/" + auditIdForImmutability).expect(200, "Denetim ayrıntısı").json();
    }

    @Then("yönetici denetim kaydını değiştirme ve silme isteklerinde reddedilmeli ve kayıt aynen kalmalıdır")
    public void denetimDegismez() {
        String path = "/v1/admin/audit-logs/" + auditIdForImmutability;
        Map<String, Object> body = Map.of("action", "qa.tampered", "actor_email", "qa@example.com");
        List<EcmApiClient.ApiResponse> rs = List.of(EcmApiClient.put(TestDataFactory.admin(), path, body),
                EcmApiClient.patch(TestDataFactory.admin(), path, body), EcmApiClient.delete(TestDataFactory.admin(), path));
        JsonObject after = EcmApiClient.get(TestDataFactory.admin(), path).expect(200, "Denetim ayrıntısı").json();
        StringBuilder ev = new StringBuilder();
        List<String> failures = failures();
        String[] names = {"PUT", "PATCH", "DELETE"};
        for (int i = 0; i < rs.size(); i++) {
            ev.append(names[i]).append(" -> ").append(rs.get(i).status()).append('\n');
            if (rs.get(i).status() != 404 && rs.get(i).status() != 405) {
                failures.add(names[i] + " " + rs.get(i).status() + " (404/405 bekleniyordu)");
            }
        }
        if (!auditBefore.equals(after)) {
            failures.add("denetim kaydı değişti");
        }
        Allure.addAttachment("NEW-18 kanıtı", "text/plain", ev + "kayıt aynı: " + auditBefore.equals(after));
        fail(failures, "Denetim kaydı değiştirilebilir olmamalı");
    }
}
