package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Batch 32 — doğrulama, sınırlar ve dosya işleme.
 *
 * Tablo adımı: her satır bir istek gönderir ve durum kodu + hata mesajı parçasını
 * doğrular. Gövde yer tutucuları:
 *   {M}              senaryoya özgü işaret (oluşmaması gereken kayıtları aramak için)
 *   {TEKRAR:c:n}     c karakterinin n kez tekrarı
 *   {RASTGELE:n}     n uzunlukta rastgele harf/rakam (sıkıştırılamaz; tekrar dizisi veritabanında sıkışıp sınırı gizler)
 *   {EPOSTA:takma}   kayıtlı kullanıcının e-postası ({AD:takma} kaynağın adı)
 *   {takma ad}       kayıtlı kaynak kimliği (ApiTestContext.resolve)
 * Beklenen durum "422", "400|422" ya da "2xx" olabilir. Başarılı oluşturmalar
 * temizlik için kaydedilir.
 */
public class BoundarySteps {

    private static final int UI_WAIT_MS = 30000;
    private static final Pattern REPEAT = Pattern.compile("\\{TEKRAR:(.):(\\d+)}");
    private static final Pattern EMAIL = Pattern.compile("\\{(?:EPOSTA|AD):([^}]+)}");
    private static final Pattern RANDOM = Pattern.compile("\\{RASTGELE:(\\d+)}");

    private String marker;
    private byte[] uploaded;
    private EcmApiClient.ApiResponse lastUpload;

    @Given("benzersiz bir doğrulama işareti belirlenmiştir")
    public void benzersizIsaret() {
        marker = "qa" + System.nanoTime();
    }

    @Then("aşağıdaki istekler beklenen durum ve mesajları döndürmelidir")
    public void istekTablosu(DataTable table) {
        List<String> failures = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            String[] req = row.get("istek").trim().split("\\s+", 2);
            String body = expand(row.getOrDefault("gövde", ""));
            String path = expand(req[1]);
            boolean anonymous = "anonim".equals(row.getOrDefault("oturum", "yönetici").trim());
            EcmApiClient.ApiResponse r = anonymous
                    ? EcmApiClient.anonymous(req[0], path, body.isBlank() ? null : JsonParser.parseString(body))
                    : EcmApiClient.sendRawJson(TestDataFactory.admin(), req[0], path, body.isBlank() ? null : body);
            ApiTestContext.lastResponse(r);
            registerCleanup(req[0], path, r);
            String expectedStatus = row.get("durum").trim();
            boolean statusOk = expectedStatus.equals("2xx") ? r.status() >= 200 && r.status() < 300
                    : List.of(expectedStatus.split("\\|")).contains(String.valueOf(r.status()));
            String fragment = row.getOrDefault("mesaj", "-").trim();
            boolean messageOk = fragment.equals("-") || r.errorMessage().contains(fragment);
            if (r.status() >= 500) {
                failures.add(row.get("istek") + " -> sunucu hatası HTTP " + r.status() + " " + r.body());
            } else if (!statusOk || !messageOk) {
                failures.add(row.get("istek") + " " + abbreviate(body) + " -> beklenen " + expectedStatus + " '" + fragment
                        + "', gelen HTTP " + r.status() + " '" + r.errorMessage() + "'");
            }
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Doğrulama tablosu beklenen sonuçları vermedi:\n  " + String.join("\n  ", failures));
        }
    }

    @Given("{string} sınıfının özellikleri {string} ve {string} olarak adlandırılmıştır")
    public void sinifOzellikleriAdlandirilmistir(String classAlias, String first, String second) {
        JsonObject c = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias)).expect(200, "Sınıf").json();
        List<JsonObject> props = new ArrayList<>();
        c.getAsJsonArray("properties").forEach(e -> props.add(e.getAsJsonObject()));
        ApiTestContext.putId(first, props.get(0).get("id").getAsString());
        ApiTestContext.putId(second, props.get(1).get("id").getAsString());
    }

    @Given("{string} belgesinin tek sürümü {string} olarak adlandırılmıştır")
    public void tekSurumAdlandirilmistir(String documentAlias, String versionAlias) {
        ApiTestContext.putId(versionAlias, VersionSteps.versionsAsc(documentAlias).get(0).get("id").getAsString());
    }

    @Then("işaretli başlıkla hiçbir belge oluşmamış olmalıdır")
    public void isaretliBaslikBelgeYok() {
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&title=" + enc(marker));
        if (r.json().getAsJsonArray("items").size() != 0) {
            throw new AssertionError("Reddedilen yüklemeler belge oluşturmamalıydı: " + r.body());
        }
    }

    @Then("işaretli e-postayla aktif kullanıcı bulunmamalıdır")
    public void isaretliEpostaKullaniciYok() {
        int s = EcmApiClient.get(TestDataFactory.admin(), "/v1/users/lookup?email=" + enc("qa." + marker + "@example.com")).status();
        if (s != 404) {
            throw new AssertionError("Reddedilen istek kullanıcı oluşturmamalıydı (lookup HTTP " + s + ").");
        }
    }

    @Then("{string} klasörünün alt klasör sayısı {int} olmalıdır")
    public void altKlasorSayisi(String folderAlias, int expected) {
        int n = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/children")
                .expect(200, "Alt klasörler").json().getAsJsonArray("items").size();
        if (n != expected) {
            throw new AssertionError("'" + folderAlias + "' altında " + expected + " klasör olmalıydı, " + n + " var.");
        }
    }

    @Then("{string} belgesinin başlığı {string} olmalıdır")
    public void belgeBasligi(String documentAlias, String expected) {
        String actual = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias)).expect(200, "Belge")
                .json().getAsJsonObject("document").get("title").getAsString();
        if (!expand(expected).equals(actual)) {
            throw new AssertionError("Başlık '" + expand(expected) + "' olmalıydı, '" + actual + "'");
        }
    }

    @Then("{string} sınıfının kuralı olmamalıdır")
    public void sinifinKuraliOlmamali(String classAlias) {
        for (JsonElement p : EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias)).expect(200, "Sınıf")
                .json().getAsJsonArray("properties")) {
            String pid = p.getAsJsonObject().get("id").getAsString();
            int n = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/" + pid + "/rules")
                    .expect(200, "Kurallar").json().getAsJsonArray("items").size();
            if (n != 0) {
                throw new AssertionError("Reddedilen kural oluşmamalıydı (özellik " + pid + ": " + n + ").");
            }
        }
    }

    @Then("{string} belgesinin güncel sürüm etiketleri {string} olmalıdır")
    public void guncelSurumEtiketleri(String documentAlias, String csv) {
        List<String> actual = new ArrayList<>();
        for (JsonElement l : VersionSteps.versionsAsc(documentAlias).get(0).getAsJsonArray("labels")) {
            actual.add(l.getAsJsonObject().get("label").getAsString());
        }
        List<String> expected = new ArrayList<>();
        for (String s : csv.split(",")) {
            expected.add(expand(s.trim()));
        }
        if (!new java.util.TreeSet<>(expected).equals(new java.util.TreeSet<>(actual))) {
            throw new AssertionError("Sürüm etiketleri " + expected + " olmalıydı, " + actual);
        }
    }

    @Then("{string} belgesinin etiketleri tam olarak {string} olmalıdır")
    public void belgeEtiketleriTam(String documentAlias, String csv) {
        List<String> actual = new ArrayList<>();
        for (JsonElement t : EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags")
                .expect(200, "Etiketler").json().getAsJsonArray("tags")) {
            actual.add(t.getAsJsonObject().get("name").getAsString());
        }
        List<String> expected = new ArrayList<>();
        for (String s : csv.split(",")) {
            expected.add(expand(s.trim()));
        }
        if (!expected.equals(actual)) {
            throw new AssertionError("Etiketler " + expected + " olmalıydı, " + actual);
        }
    }

    @Then("etiket kataloğunda {string} için oluşturma isteği mevcut etiketi döndürmeli ve yinelenen etiket oluşmamalıdır")
    public void katalogMevcutEtiketiDondurmeli(String name) {
        String n = expand(name);
        JsonObject a = EcmApiClient.post(TestDataFactory.admin(), "/v1/tags", Map.of("name", n)).expect(201, "Etiket oluşturma").json();
        String original = a.get("name").getAsString();
        if (original.equals(n)) {
            throw new AssertionError("Harf varyantı için mevcut etiket dönmeliydi, yeni etiket '" + n + "' oluştu.");
        }
    }

    // ---------------------------------------------------------------------
    // JSON yükleme sınırı (DOC-06)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasörüne {int} baytlık içerikle JSON yüklemesi yapar")
    public void jsonYuklemesi(String folderAlias, int size) {
        uploaded = new byte[size];
        java.util.Arrays.fill(uploaded, (byte) 'q');
        lastUpload = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(marker + " json " + size, ApiTestContext.id(folderAlias), "text/plain", "qa-size.txt", uploaded));
        ApiTestContext.lastResponse(lastUpload);
        if (lastUpload.status() == 201) {
            TestDataFactory.registerDocument("boyut belgesi " + size, lastUpload.json().get("document_id").getAsString(), marker);
        }
    }

    @Then("yükleme 201 dönmeli ve indirilen içerik yüklenen baytlarla aynı olmalıdır")
    public void yuklemeVeIndirmeAyni() {
        lastUpload.expect(201, "Yükleme");
        byte[] down = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + lastUpload.json().get("document_id").getAsString() + "/content");
        if (!sha(down).equals(sha(uploaded))) {
            throw new AssertionError("İndirilen içerik farklı (SHA-256 " + sha(down) + " / " + sha(uploaded) + ", " + down.length + " bayt).");
        }
    }

    // ---------------------------------------------------------------------
    // Akış (multipart) yükleme (DOC-07)
    // ---------------------------------------------------------------------

    @When("yönetici arayüzde {string} klasörüne {int} MB'lık dosya yükler")
    public void arayuzdeBuyukDosyaYukler(String folderAlias, int mb) throws Exception {
        uploaded = new byte[mb * 1024 * 1024];
        new Random(42).nextBytes(uploaded);
        Path file = Files.createTempFile("qa-stream-", ".bin");
        Files.write(file, uploaded);
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page p = BrowserManager.page;
        p.navigate(BrowserManager.getBaseUrl() + "/documents/new/unclassified?folder_id=" + ApiTestContext.id(folderAlias));
        Locator title = p.getByPlaceholder("e.g. Q3 Budget Spreadsheet");
        title.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        String t = marker + " akış";
        title.fill(t);
        p.locator("input[type='file']").setInputFiles(file);
        Response r = p.waitForResponse(resp -> "POST".equals(resp.request().method()) && resp.url().contains("/v1/documents/unclassified"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> p.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Upload document").setExact(true)).click());
        if (r.status() != 201 || !r.url().endsWith("/stream")) {
            throw new AssertionError("2 MB üstü dosya akış ucuyla (…/stream) 201 yüklenmeliydi: " + r.url() + " HTTP " + r.status());
        }
        String id = JsonParser.parseString(r.text()).getAsJsonObject().get("document_id").getAsString();
        TestDataFactory.registerDocument("akış belgesi", id, t);
        p.waitForURL("**/documents/" + id, new Page.WaitForURLOptions().setTimeout(UI_WAIT_MS));
    }

    @Then("{string} indirildiğinde yüklenen dosyayla bayt bayt aynı olmalıdır")
    public void indirildigindeAyni(String documentAlias) {
        byte[] down = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content");
        if (!sha(down).equals(sha(uploaded))) {
            throw new AssertionError("Akışla yüklenen dosya bozulmuş: " + down.length + " / " + uploaded.length + " bayt.");
        }
    }

    @When("yönetici akış ucuna {string} gönderir")
    public void akisUcunaGonderir(String kind) {
        String meta = "{\"title\":\"" + marker + " akış red\",\"metadata\":{},\"mime_type\":\"text/plain\",\"original_filename\":\"a.txt\",\"folder_id\":\""
                + BrowserManager.ROOT_FOLDER_ID + "\"}";
        EcmApiClient.Part metaPart = new EcmApiClient.Part("metadata", null, "application/json", meta.getBytes(StandardCharsets.UTF_8));
        EcmApiClient.Part filePart = new EcmApiClient.Part("file", "a.txt", "text/plain", "QA akış".getBytes(StandardCharsets.UTF_8));
        List<EcmApiClient.Part> parts = switch (kind) {
            case "yalnız dosya parçası" -> List.of(filePart);
            case "yalnız metadata parçası" -> List.of(metaPart);
            case "dosyadan sonra metadata" -> List.of(filePart, metaPart);
            default -> throw new IllegalArgumentException(kind);
        };
        EcmApiClient.ApiResponse r = EcmApiClient.postMultipart(TestDataFactory.admin(), "/v1/documents/unclassified/stream", parts);
        ApiTestContext.lastResponse(r);
        registerCleanup("POST", "/v1/documents/unclassified", r);
    }

    // ---------------------------------------------------------------------
    // Unicode / Türkçe (DOC-08, DATA-02)
    // ---------------------------------------------------------------------

    @When("yönetici {string} klasörüne {string} başlığı ve {string} dosya adıyla Türkçe içerikli belge yükler")
    public void unicodeBelgeYukler(String folderAlias, String title, String filename) {
        uploaded = "İçerik: çğıöşü ÇĞİÖŞÜ".getBytes(StandardCharsets.UTF_8);
        String t = expand(title);
        lastUpload = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(t, ApiTestContext.id(folderAlias), "text/plain", filename, uploaded));
        ApiTestContext.lastResponse(lastUpload);
        if (lastUpload.status() >= 500) {
            throw new AssertionError("Unicode başlık/dosya adıyla yükleme sunucu hatası verdi: HTTP " + lastUpload.status() + " "
                    + lastUpload.body() + " (dosya adı: '" + filename + "')");
        }
        lastUpload.expect(201, "Unicode yükleme");
        TestDataFactory.registerDocument("unicode belge", lastUpload.json().get("document_id").getAsString(), t);
        ApiTestContext.putName("unicode belge#filename", filename);
    }

    @Then("unicode belgenin başlığı, dosya adı ve içeriği korunmalıdır")
    public void unicodeBelgeKorunmali() {
        JsonObject d = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id("unicode belge")).expect(200, "Belge").json();
        String title = d.getAsJsonObject("document").get("title").getAsString();
        String fn = d.getAsJsonObject("current_version").get("original_filename").getAsString();
        if (!ApiTestContext.name("unicode belge").equals(title) || !ApiTestContext.name("unicode belge#filename").equals(fn)) {
            throw new AssertionError("Başlık/dosya adı korunmadı: '" + title + "' / '" + fn + "'");
        }
        byte[] down = EcmApiClient.getBytes(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id("unicode belge") + "/content");
        if (!sha(down).equals(sha(uploaded))) {
            throw new AssertionError("UTF-8 içerik bozuldu.");
        }
    }

    @When("yönetici Türkçe adlarla klasör, sınıf özellik değeri ve kayıtlı arama oluşturur")
    public void turkceAdlarlaOlusturur() {
        EcmApiClient.Session a = TestDataFactory.admin();
        String folderName = "Çağrı Şube " + marker;
        JsonObject f = EcmApiClient.post(a, "/v1/folders", Map.of("name", folderName, "parent_id", BrowserManager.ROOT_FOLDER_ID, "inherit_permissions", false))
                .expect(200, "Türkçe klasör").json();
        String fid = f.get("id").getAsString();
        ApiTestContext.putId("türkçe klasör", fid);
        ApiTestContext.putName("türkçe klasör", folderName);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/folders/" + fid + "/active", Map.of("is_active", false)));
        JsonObject c = EcmApiClient.post(a, "/v1/classes", Map.of("name", "QA Türkçe Sınıf " + marker, "properties",
                List.of(Map.of("key", "konu", "display_name", "Konu", "data_type", "text", "is_multi_value", false, "required", true)))).expect(200, "Sınıf").json();
        ApiTestContext.putId("türkçe sınıf", c.get("id").getAsString());
        ApiTestContext.putName("türkçe sınıf#version", c.get("version").getAsString());
        String cid = c.get("id").getAsString();
        ApiTestContext.onCleanup(() -> {
            long v = ClassAdminSteps.classVersion("türkçe sınıf");
            EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + cid + "/active", Map.of("expected_version", v, "is_active", false));
        });
        EcmApiClient.ApiResponse d = EcmApiClient.post(a, "/v1/documents", TestDataFactory.classifiedDocumentBody("QA Türkçe " + marker, cid,
                c.get("version").getAsLong(), Map.of("konu", "İş Güvenliği Şartnamesi"), "türkçe klasör"));
        d.expect(201, "Türkçe özellikli belge");
        TestDataFactory.registerDocument("türkçe belge", d.json().get("document_id").getAsString(), "QA Türkçe " + marker);
        EcmApiClient.ApiResponse s = EcmApiClient.post(a, "/v1/saved-searches", Map.of("name", "Günlük Özet " + marker, "filter", Map.of("title", marker)));
        s.expect(201, "Türkçe kayıtlı arama");
        String sid = s.json().get("id").getAsString();
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/saved-searches/" + sid));
    }

    @Then("Türkçe değerler bozulmadan okunmalı ve aranabilmelidir")
    public void turkceDegerlerKorunmali() {
        EcmApiClient.Session a = TestDataFactory.admin();
        String folderName = a == null ? "" : EcmApiClient.get(a, "/v1/folders/" + ApiTestContext.id("türkçe klasör")).expect(200, "Klasör")
                .json().getAsJsonObject("folder").get("name").getAsString();
        if (!ApiTestContext.name("türkçe klasör").equals(folderName)) {
            throw new AssertionError("Türkçe klasör adı bozuldu: " + folderName);
        }
        String value = EcmApiClient.get(a, "/v1/documents/" + ApiTestContext.id("türkçe belge")).expect(200, "Belge")
                .json().getAsJsonObject("document").getAsJsonObject("metadata").get("konu").getAsString();
        if (!"İş Güvenliği Şartnamesi".equals(value)) {
            throw new AssertionError("Türkçe özellik değeri bozuldu: " + value);
        }
        String props = "[{\"key\":\"konu\",\"op\":\"contains\",\"value\":\"güvenliği\"}]";
        String found = EcmApiClient.get(a, "/v1/documents?class_id=" + ApiTestContext.id("türkçe sınıf") + "&props=" + enc(props)).expect(200, "Özellik araması").body();
        if (!found.contains(ApiTestContext.id("türkçe belge"))) {
            throw new AssertionError("Türkçe özellik değeriyle (küçük harf 'güvenliği') arama belgeyi bulmadı.");
        }
        if (!EcmApiClient.get(a, "/v1/saved-searches").expect(200, "Kayıtlı aramalar").body().contains("Günlük Özet " + marker)) {
            throw new AssertionError("Türkçe kayıtlı arama adı listede yok/bozuk.");
        }
        EcmApiClient.ApiResponse tag = EcmApiClient.post(a, "/v1/documents/" + ApiTestContext.id("türkçe belge") + "/tags",
                Map.of("tag_ids", List.of(), "tag_name", "öncelikli"));
        String evidence = "RQ-22 Türkçe etiket 'öncelikli': HTTP " + tag.status() + " " + tag.errorMessage()
                + " (domain/tag.rs is_ascii_alphanumeric; mesaj 'letters' diyor)";
        System.out.println("[RQ-22] " + evidence);
        Allure.addAttachment("RQ-22 evidence — Turkish tag name", "text/plain", evidence);
    }

    // ---------------------------------------------------------------------

    private String expand(String text) {
        if (text == null) {
            return "";
        }
        String out = text.replace("{M}", marker == null ? "" : marker);
        Matcher m = REPEAT.matcher(out);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1).repeat(Integer.parseInt(m.group(2)))));
        }
        m.appendTail(sb);
        out = sb.toString();
        Matcher rnd = RANDOM.matcher(out);
        StringBuilder sbr = new StringBuilder();
        while (rnd.find()) {
            rnd.appendReplacement(sbr, randomText(Integer.parseInt(rnd.group(1))));
        }
        rnd.appendTail(sbr);
        out = sbr.toString();
        Matcher e = EMAIL.matcher(out);
        StringBuilder sb2 = new StringBuilder();
        while (e.find()) {
            e.appendReplacement(sb2, Matcher.quoteReplacement(ApiTestContext.name(e.group(1))));
        }
        e.appendTail(sb2);
        return ApiTestContext.resolve(sb2.toString());
    }

    private static String randomText(int n) {
        String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(abc.charAt(random.nextInt(abc.length())));
        }
        return sb.toString();
    }

    private static void registerCleanup(String method, String path, EcmApiClient.ApiResponse r) {
        if (!"POST".equals(method) || r.status() < 200 || r.status() >= 300) {
            return;
        }
        try {
            JsonObject j = r.json();
            if (path.equals("/v1/users") || path.equals("/v1/auth/register")) {
                String id = j.has("id") ? j.get("id").getAsString() : j.getAsJsonObject("user").get("id").getAsString();
                ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + id, Map.of("is_active", false)));
            } else if (path.equals("/v1/folders")) {
                String id = j.get("id").getAsString();
                ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/folders/" + id + "/active", Map.of("is_active", false)));
            } else if (path.startsWith("/v1/documents/unclassified") || path.equals("/v1/documents")) {
                String id = j.get("document_id").getAsString();
                ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + id));
            } else if (path.equals("/v1/classes")) {
                String id = j.get("id").getAsString();
                long v = j.get("version").getAsLong();
                ApiTestContext.onCleanup(() -> EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + id + "/active",
                        Map.of("expected_version", v, "is_active", false)));
            } else if (path.equals("/v1/groups")) {
                String id = j.get("id").getAsString();
                ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/groups/" + id));
            }
        } catch (RuntimeException ignored) {
            // Temizlik kaydı testi düşürmemeli.
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "…(" + s.length() + ")" : s;
    }

    private static String sha(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
