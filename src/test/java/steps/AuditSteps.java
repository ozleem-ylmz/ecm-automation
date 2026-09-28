package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/*
 * Batch 33 — denetim kayıtları ve geçmiş.
 * ECM kaynakları: api/rest/audit.rs (list/export, target_id/action filtreleri),
 * engine/history.rs (kaynak geçmişi = hedefi o kaynak olan kayıtlar),
 * components/HistoryTimeline.tsx (eylem cümleleri).
 * Denetim yazımı eşzamansızdır (emit); doğrulamalar koşul tabanlı yoklama ile yapılır.
 */
public class AuditSteps {

    private static final int UI_WAIT_MS = 20000;
    private static final long POLL_MS = 15000;

    private int countBefore = -1;
    private String aclId;

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------

    /* Mutasyon denetimi: ECM her okumayı da (document.read/download) kaydeder; testin kendi doğrulama
       GET'leri bu kayıtları üretir, bu yüzden okuma olayları mutasyon karşılaştırmalarında dışarıda bırakılır
       (action filtresi açıkça okuma isteyen sorgular hariç). */
    private static List<JsonObject> audit(String query) {
        List<JsonObject> out = new ArrayList<>();
        boolean readsRequested = query.contains("action=document.read") || query.contains("action=document.download");
        EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?limit=200&" + query).expect(200, "Denetim sorgusu")
                .json().getAsJsonArray("items").forEach(e -> {
                    String action = e.getAsJsonObject().get("action").getAsString();
                    if (readsRequested || !(action.equals("document.read") || action.equals("document.download"))) {
                        out.add(e.getAsJsonObject());
                    }
                });
        Collections.reverse(out); // en eski önce
        return out;
    }

    private static List<JsonObject> pollAudit(String query, Predicate<List<JsonObject>> done) {
        long deadline = System.currentTimeMillis() + POLL_MS;
        List<JsonObject> items = audit(query);
        while (!done.test(items) && System.currentTimeMillis() < deadline) {
            pause();
            items = audit(query);
        }
        return items;
    }

    private static List<String> actions(List<JsonObject> items) {
        List<String> out = new ArrayList<>();
        items.forEach(i -> out.add(i.get("action").getAsString()));
        return out;
    }

    private static void pause() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String str(JsonObject o, String k) {
        return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    private static JsonObject obj(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : null;
    }

    // ---------------------------------------------------------------------
    // AUD-01 — belge yaşam döngüsü kayıtları
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin denetim kayıtları sırasıyla {string} eylemlerini yönetici aktörüyle içermelidir")
    public void belgeDenetimKayitlariSirasiyla(String documentAlias, String csv) {
        List<String> expected = List.of(csv.split(",\\s*"));
        List<JsonObject> items = pollAudit("target_id=" + ApiTestContext.id(documentAlias), l -> l.size() >= expected.size());
        List<String> got = actions(items);
        if (!got.equals(expected)) {
            throw new AssertionError("Denetim eylem sırası " + expected + " olmalıydı, " + got + " bulundu.");
        }
        for (JsonObject i : items) {
            if (!TestDataFactory.ADMIN_EMAIL.equals(str(i, "actor_email"))) {
                throw new AssertionError("Aktör admin@local olmalıydı: " + i);
            }
        }
        for (JsonObject i : items) {
            switch (i.get("action").getAsString()) {
                case "document.status_change" -> expectBeforeAfter(i, "status", "draft", "under_review");
                case "document.soft_delete" -> expectBeforeAfter(i, "is_deleted", "false", "true");
                case "document.restore" -> expectBeforeAfter(i, "is_deleted", "true", "false");
                case "document.rename" -> {
                    String before = str(obj(i, "before_state"), "title");
                    String after = str(obj(i, "after_state"), "title");
                    if (before == null || after == null || before.equals(after)) {
                        throw new AssertionError("Yeniden adlandırma kaydında önce/sonra başlık farklı olmalıydı: " + i);
                    }
                }
                case "document.move" -> {
                    String before = str(obj(i, "before_state"), "folder_id");
                    String after = str(obj(i, "after_state"), "folder_id");
                    if (before == null || after == null || before.equals(after)) {
                        throw new AssertionError("Taşıma kaydında önce/sonra klasör farklı olmalıydı: " + i);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void expectBeforeAfter(JsonObject item, String field, String before, String after) {
        String b = str(obj(item, "before_state"), field);
        String a = str(obj(item, "after_state"), field);
        if (!before.equals(b) || !after.equals(a)) {
            throw new AssertionError(item.get("action").getAsString() + " kaydında " + field + " " + before + "->" + after
                    + " olmalıydı, " + b + "->" + a + " bulundu.");
        }
    }

    // ---------------------------------------------------------------------
    // AUD-02 — reddedilen işlemler kayda geçmez
    // ---------------------------------------------------------------------

    @Given("{string} belgesinin denetim kaydı sayısı kaydedilmiştir")
    public void denetimKaydiSayisiKaydedilmistir(String documentAlias) {
        countBefore = pollAudit("target_id=" + ApiTestContext.id(documentAlias), l -> !l.isEmpty()).size();
    }

    @Then("{string} belgesinin denetim kayıtlarına yalnız son başarılı {string} eklenmiş olmalıdır")
    public void yalnizSonBasariliEklenmis(String documentAlias, String action) {
        List<JsonObject> items = pollAudit("target_id=" + ApiTestContext.id(documentAlias),
                l -> actions(l).stream().filter(action::equals).count() >= 1 && l.size() > countBefore);
        pause();
        items = audit("target_id=" + ApiTestContext.id(documentAlias));
        List<String> added = actions(items).subList(countBefore, items.size());
        if (!added.equals(List.of(action))) {
            throw new AssertionError("Reddedilen işlemler denetime yazılmamalıydı; eklenen kayıtlar " + added + " (yalnız [" + action + "] bekleniyordu).");
        }
    }

    // ---------------------------------------------------------------------
    // AUD-03 — izin, pasifleştirme, başarısız giriş
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasöründe {string} kullanıcısına okuma izni verip kaldırır")
    public void izinVeripKaldirir(String folderAlias, String userAlias) {
        aclId = TestDataFactory.grantFolder(folderAlias, TestDataFactory.ROLE_VIEWER, userAlias, false, false);
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl/" + aclId).expect(200, "İzin kaldırma");
    }

    @When("{string} yanlış parolayla giriş yapmayı dener")
    public void yanlisParolaylaGiris(String userAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.rawLogin(ApiTestContext.name(userAlias), "Yanlis!Parola99");
        if (r.status() != 401) {
            throw new AssertionError("Yanlış parola 401 dönmeliydi, HTTP " + r.status());
        }
    }

    @Then("izin kaydı için {string} ve {string} kayıtları klasör kimliğiyle yazılmış olmalıdır")
    public void izinKayitlari(String grantAction, String revokeAction) {
        List<JsonObject> items = pollAudit("target_id=" + aclId, l -> l.size() >= 2);
        if (!actions(items).equals(List.of(grantAction, revokeAction))) {
            throw new AssertionError("İzin kaydı eylemleri [" + grantAction + ", " + revokeAction + "] olmalıydı: " + actions(items));
        }
        if (!TestDataFactory.ADMIN_EMAIL.equals(str(items.get(0), "actor_email"))) {
            throw new AssertionError("İzin kaydının aktörü admin olmalıydı: " + items.get(0));
        }
    }

    @Then("{string} kullanıcısı için {string} kaydı {string} aktörüyle bulunmalıdır")
    public void kullaniciKaydiAktorle(String userAlias, String action, String actorAlias) {
        String actor = actorAlias.equals("yönetici") ? TestDataFactory.ADMIN_EMAIL : ApiTestContext.name(actorAlias);
        List<JsonObject> items = pollAudit("target_id=" + ApiTestContext.id(userAlias) + "&action=" + action, l -> !l.isEmpty());
        if (items.isEmpty() || !actor.equalsIgnoreCase(str(items.get(items.size() - 1), "actor_email"))) {
            throw new AssertionError("'" + action + "' kaydı " + actor + " aktörüyle bulunmalıydı: " + items);
        }
    }

    // ---------------------------------------------------------------------
    // AUD-04 — CSV dışa aktarım
    // ---------------------------------------------------------------------

    @Then("{string} belgesi için filtreli CSV dışa aktarımı sorguyla aynı kayıtları geçerli CSV olarak vermelidir")
    public void csvDisaAktarim(String documentAlias) {
        String id = ApiTestContext.id(documentAlias);
        pollAudit("target_id=" + id, l -> l.size() >= 3);
        // CSV okuma olaylarını da içerir: karşılaştırma filtresiz liste ile yapılır.
        List<JsonObject> items = new ArrayList<>();
        EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?limit=200&target_id=" + id).expect(200, "Denetim sorgusu")
                .json().getAsJsonArray("items").forEach(e -> items.add(e.getAsJsonObject()));
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs/export?target_id=" + id).expect(200, "CSV");
        List<String> lines = new ArrayList<>(List.of(r.body().split("\\r?\\n")));
        lines.removeIf(String::isBlank);
        String header = lines.get(0);
        if (!header.equals("id,occurred_at,actor_id,actor_email,action,target_type,target_id,ip_address,correlation_id")) {
            throw new AssertionError("CSV başlığı beklenenden farklı: " + header);
        }
        if (lines.size() - 1 != items.size()) {
            throw new AssertionError("CSV satır sayısı (" + (lines.size() - 1) + ") sorgu toplamıyla (" + items.size() + ") aynı olmalıydı.");
        }
        List<String> csvIds = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> fields = parseCsv(line);
            if (fields.size() != 9 || !id.equals(fields.get(6))) {
                throw new AssertionError("Geçersiz CSV satırı ya da filtre dışı kayıt: " + line);
            }
            csvIds.add(fields.get(0));
        }
        for (JsonObject i : items) {
            if (!csvIds.contains(i.get("id").getAsString())) {
                throw new AssertionError("Sorgudaki kayıt CSV'de yok: " + i.get("id"));
            }
        }
    }

    private static List<String> parseCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // ---------------------------------------------------------------------
    // AUD-05 / AUD-08 — geçmiş panelleri
    // ---------------------------------------------------------------------

    @Then("yönetici arayüzde {string} belgesinin geçmiş panelinde {string} ifadelerini görmelidir")
    public void belgeGecmisPaneli(String documentAlias, String csv) {
        openHistory("/documents/" + ApiTestContext.id(documentAlias), csv);
    }

    @Then("yönetici arayüzde {string} klasörünün geçmiş panelinde {string} ifadelerini görmelidir")
    public void klasorGecmisPaneli(String folderAlias, String csv) {
        openHistory("/folders/" + ApiTestContext.id(folderAlias), csv);
    }

    private static void openHistory(String path, String csv) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page p = BrowserManager.page;
        p.navigate(BrowserManager.getBaseUrl() + path);
        Locator heading = p.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("History").setExact(true));
        try {
            heading.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("History paneli görünmedi. URL: " + p.url(), e);
        }
        heading.click();
        Locator section = heading.locator("xpath=ancestor::section[1]");
        List<String> missing = new ArrayList<>();
        for (String phrase : csv.split(",\\s*")) {
            try {
                section.getByText(phrase).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            } catch (PlaywrightException e) {
                missing.add(phrase);
            }
        }
        if (!missing.isEmpty()) {
            throw new AssertionError("Geçmiş panelinde eksik girdiler: " + missing);
        }
    }

    @When("yönetici {string} klasörünü yeniden adlandırır, pasifleştirip aktifleştirir, {string} kullanıcısına izin verir ve sahipliğini devreder")
    public void klasorIslemleri(String folderAlias, String userAlias) {
        EcmApiClient.Session a = TestDataFactory.admin();
        String id = ApiTestContext.id(folderAlias);
        EcmApiClient.patch(a, "/v1/folders/" + id, Map.of("name", TestDataFactory.unique("QA Geçmiş"))).expect(200, "Yeniden adlandırma");
        EcmApiClient.put(a, "/v1/folders/" + id + "/active", Map.of("is_active", false)).expect(200, "Pasifleştirme");
        EcmApiClient.put(a, "/v1/folders/" + id + "/active", Map.of("is_active", true)).expect(200, "Aktifleştirme");
        aclId = TestDataFactory.grantFolder(folderAlias, TestDataFactory.ROLE_VIEWER, userAlias, false, false);
        EcmApiClient.patch(a, "/v1/folders/" + id + "/owner", Map.of("new_owner_id", ApiTestContext.id(userAlias))).expect(200, "Sahiplik devri");
    }

    @Then("{string} klasörünün geçmişi {string} eylemlerini yönetici aktörüyle içermelidir")
    public void klasorGecmisiIcermeli(String folderAlias, String csv) {
        List<String> expected = List.of(csv.split(",\\s*"));
        long deadline = System.currentTimeMillis() + POLL_MS;
        List<String> got = new ArrayList<>();
        while (System.currentTimeMillis() < deadline) {
            got.clear();
            for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/history")
                    .expect(200, "Klasör geçmişi").json().getAsJsonArray("items")) {
                JsonObject o = e.getAsJsonObject();
                if (TestDataFactory.ADMIN_EMAIL.equals(str(o, "actor_email"))) {
                    got.add(o.get("action").getAsString());
                }
            }
            if (got.containsAll(expected)) {
                return;
            }
            pause();
        }
        List<String> missing = new ArrayList<>(expected);
        missing.removeAll(got);
        throw new AssertionError("Klasör geçmişinde eksik eylemler " + missing + " (bulunan: " + got + "). Not: ACL olayları hedef türü 'acl' ile "
                + "yazıldığı için engine/history.rs load_history(Folder, id) bunları getirmiyor; HistoryTimeline.tsx 'folder.acl_grant' için "
                + "'granted access' cümlesini beklediği hâlde.");
    }

    @Then("{string} sınıfının geçmişi {string} eylemlerini yönetici aktörüyle içermeli ve var olmayan sınıf 404 dönmelidir")
    public void sinifGecmisi(String classAlias, String csv) {
        List<String> expected = List.of(csv.split(",\\s*"));
        long deadline = System.currentTimeMillis() + POLL_MS;
        List<String> got = new ArrayList<>();
        while (System.currentTimeMillis() < deadline) {
            got.clear();
            for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/history")
                    .expect(200, "Sınıf geçmişi").json().getAsJsonArray("items")) {
                got.add(e.getAsJsonObject().get("action").getAsString());
            }
            if (got.containsAll(expected)) {
                break;
            }
            pause();
        }
        if (!got.containsAll(expected)) {
            throw new AssertionError("Sınıf geçmişi " + expected + " içermeliydi: " + got);
        }
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + UUID.randomUUID() + "/history").status();
        if (s != 404) {
            throw new AssertionError("Var olmayan sınıfın geçmişi 404 olmalıydı, HTTP " + s);
        }
    }

    // ---------------------------------------------------------------------
    // AUD-07 — admin ACL atlaması
    // ---------------------------------------------------------------------

    @When("yönetici {string} belgesini okuyup indirir")
    public void yoneticiOkuyupIndirir(String documentAlias) {
        EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias)).expect(200, "Okuma");
        EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content");
    }

    @Then("{string} belgesi için yönetici okuma ve indirme kayıtları bulunmalı, atlama bilgisi RQ-12 kanıtı olarak kaydedilmelidir")
    public void okumaIndirmeKayitlari(String documentAlias) {
        String id = ApiTestContext.id(documentAlias);
        List<JsonObject> read = pollAudit("target_id=" + id + "&action=document.read", l -> !l.isEmpty());
        List<JsonObject> dl = pollAudit("target_id=" + id + "&action=document.download", l -> !l.isEmpty());
        if (read.isEmpty() || dl.isEmpty()) {
            throw new AssertionError("Yöneticinin okuma/indirme işlemi denetime yazılmalıydı: read=" + read.size() + " download=" + dl.size());
        }
        JsonObject last = dl.get(dl.size() - 1);
        if (!TestDataFactory.ADMIN_EMAIL.equals(str(last, "actor_email"))) {
            throw new AssertionError("İndirme kaydının aktörü admin olmalıydı: " + last);
        }
        String evidence = "RQ-12 (admin ACL atlaması denetimi): document.read ve document.download kayıtları var; "
                + "kayıtta atlama (bypass) bilgisi: " + (last.toString().contains("bypass") ? "VAR" : "YOK") + " — detail=" + last.get("detail");
        System.out.println("[RQ-12] " + evidence);
        Allure.addAttachment("RQ-12 evidence — admin bypass audit", "text/plain", evidence);
    }
}
