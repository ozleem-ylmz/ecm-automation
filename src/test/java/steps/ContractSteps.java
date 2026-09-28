package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.qameta.allure.Allure;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Batch 35 — plan dışı yeni testler: indirme sözleşmesi (Range, bütünlük,
 * güvenlik başlıkları, dosya adı) ve denetim kaydı ayrıntısı.
 *
 * ECM kaynakları: api/rest/documents.rs parse_range_header + build_attachment_response,
 * engine/documents/filename.rs build_download_filename, api/rest/routes.rs
 * with_security_headers, api/rest/audit.rs get_audit_log.
 */
public class ContractSteps {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    record Raw(int status, java.net.http.HttpHeaders headers, byte[] body) {
        String header(String name) {
            return headers.firstValue(name).orElse(null);
        }
    }

    /* İstemcinin aldığı ham yanıt: başlıklar ve baytlar (EcmApiClient gövdeyi metne çevirir). */
    static Raw rawGet(EcmApiClient.Session s, String path, Map<String, String> headers) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BrowserManager.getApiBaseUrl() + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + s.accessToken())
                .GET();
        headers.forEach(b::header);
        try {
            HttpResponse<byte[]> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Raw(r.statusCode(), r.headers(), r.body());
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("GET " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GET " + path + " kesildi", e);
        }
    }

    @Given("yönetici {string} klasörüne {string} adlı {string} türünde {string} içerikli {string} belgesini yüklemiştir")
    public void ozelDosyaYuklemistir(String folderAlias, String filename, String mime, String text, String documentAlias) {
        String title = TestDataFactory.unique("QA İndirme " + documentAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/unclassified",
                DocumentOperationsSteps.unclassifiedBody(title, ApiTestContext.id(folderAlias), mime, filename,
                        text.getBytes(StandardCharsets.UTF_8)));
        r.expect(201, "Belge yükleme (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), title);
        ApiTestContext.putName(documentAlias + "#v1", r.json().get("version_id").getAsString());
    }

    // ---------------------------------------------------------------------
    // DL-02 — HTTP Range sözleşmesi
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin içeriği şu bayt aralıklarıyla doğru indirilmelidir:")
    public void aralikliIndirme(String documentAlias, DataTable table) {
        rangeTable("/v1/documents/" + ApiTestContext.id(documentAlias) + "/content", table);
    }

    @Then("{string} belgesinin ilk sürüm içeriği şu bayt aralıklarıyla doğru indirilmelidir:")
    public void ilkSurumAralikliIndirme(String documentAlias, DataTable table) {
        rangeTable("/v1/documents/versions/" + ApiTestContext.name(documentAlias + "#v1") + "/content", table);
    }

    private void rangeTable(String path, DataTable table) {
        List<String> failures = new ArrayList<>();
        StringBuilder ev = new StringBuilder(path.replaceAll("[0-9a-f-]{36}", "{id}") + "\n");
        for (Map<String, String> row : table.asMaps()) {
            String range = row.get("aralık").trim();
            Raw r = rawGet(TestDataFactory.admin(), path, range.equals("(yok)") ? Map.of() : Map.of("Range", range));
            int expected = Integer.parseInt(row.get("durum").trim());
            String expectedRange = row.get("içerik aralığı").trim();
            String expectedBody = row.get("gövde").trim();
            String body = new String(r.body(), StandardCharsets.UTF_8);
            String contentRange = r.header("content-range");
            ev.append(range).append(" -> ").append(r.status()).append(" | Content-Range=").append(contentRange)
                    .append(" | Accept-Ranges=").append(r.header("accept-ranges")).append(" | ").append(body).append('\n');
            if (r.status() != expected) {
                failures.add(range + ": durum " + r.status() + " (beklenen " + expected + ")");
                continue;
            }
            if (expected == 416) {
                if (!body.contains("range_not_satisfiable")) {
                    failures.add(range + ": 416 gövdesi range_not_satisfiable olmalıydı: " + body);
                }
                continue;
            }
            if (!"bytes".equals(r.header("accept-ranges"))) {
                failures.add(range + ": Accept-Ranges: bytes her yanıtta olmalıydı");
            }
            if (!body.equals(expectedBody)) {
                failures.add(range + ": gövde '" + body + "' (beklenen '" + expectedBody + "')");
            }
            String cl = r.header("content-length");
            if (cl != null && Integer.parseInt(cl) != r.body().length) {
                failures.add(range + ": Content-Length " + cl + " gövde uzunluğuyla uyuşmuyor");
            }
            boolean noRange = expectedRange.equals("-");
            if (noRange ? contentRange != null : !expectedRange.equals(contentRange)) {
                failures.add(range + ": Content-Range '" + contentRange + "' (beklenen " + (noRange ? "yok" : expectedRange) + ")");
            }
        }
        Allure.addAttachment("DL-02 kanıtı — Range yanıtları", "text/plain", ev.toString());
        if (!failures.isEmpty()) {
            throw new AssertionError("Range sözleşmesi ihlalleri:\n" + String.join("\n", failures));
        }
    }

    // ---------------------------------------------------------------------
    // DL-03 — her sürümün sağlama toplamı ve boyutu
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin her sürümünün sağlama toplamı ve boyutu indirilen baytlarla aynı olmalıdır")
    public void surumButunlugu(String documentAlias) {
        String id = ApiTestContext.id(documentAlias);
        List<JsonObject> versions = VersionSteps.versionsAsc(documentAlias);
        if (versions.size() < 2) {
            throw new AssertionError("Bu kontrol en az iki sürüm gerektirir, bulunan: " + versions.size());
        }
        StringBuilder ev = new StringBuilder();
        List<String> failures = new ArrayList<>();
        String currentSha = null;
        for (JsonObject v : versions) {
            String vid = v.get("id").getAsString();
            JsonObject detail = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/versions/" + vid)
                    .expect(200, "Sürüm ayrıntısı").json().getAsJsonObject("version");
            byte[] bytes = rawGet(TestDataFactory.admin(), "/v1/documents/versions/" + vid + "/content", Map.of()).body();
            String sha = sha256(bytes);
            String declared = detail.get("checksum_sha256").getAsString();
            long size = detail.get("file_size_bytes").getAsLong();
            ev.append(label(v)).append(": sha=").append(sha.equals(declared) ? "eşleşiyor" : "FARKLI")
                    .append(", boyut ").append(size).append('/').append(bytes.length).append('\n');
            if (!sha.equals(declared)) {
                failures.add(label(v) + " checksum_sha256 " + declared + " != indirilen " + sha);
            }
            if (size != bytes.length) {
                failures.add(label(v) + " file_size_bytes " + size + " != indirilen " + bytes.length);
            }
            if (v.get("is_current").getAsBoolean()) {
                currentSha = sha;
            }
        }
        byte[] current = rawGet(TestDataFactory.admin(), "/v1/documents/" + id + "/content", Map.of()).body();
        if (!sha256(current).equals(currentSha)) {
            failures.add("/content güncel sürümün baytlarını vermeli");
        }
        JsonObject cv = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id).expect(200, "Belge").json()
                .getAsJsonObject("current_version");
        ev.append("RQ-27 kanıtı: okuma yanıtı depolama ayrıntılarını içeriyor: s3_bucket=")
                .append(cv.has("s3_bucket")).append(", s3_key=").append(cv.has("s3_key")).append('\n');
        Allure.addAttachment("DL-03 kanıtı — sürüm bütünlüğü", "text/plain", ev.toString());
        if (!failures.isEmpty()) {
            throw new AssertionError("Sürüm bütünlüğü ihlalleri:\n" + String.join("\n", failures));
        }
    }

    // ---------------------------------------------------------------------
    // DL-04 — etkin içerik güvenli sunulur
    // ---------------------------------------------------------------------

    @Then("{string} belgesi indirildiğinde etkin içerik güvenlik başlıklarıyla ek olarak sunulmalıdır")
    public void etkinIcerikGuvenli(String documentAlias) {
        Raw r = rawGet(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content", Map.of());
        String cd = r.header("content-disposition");
        String csp = r.header("content-security-policy");
        String ev = "Content-Type=" + r.header("content-type") + "\nContent-Disposition=" + cd
                + "\nX-Content-Type-Options=" + r.header("x-content-type-options") + "\nX-Frame-Options=" + r.header("x-frame-options")
                + "\nContent-Security-Policy=" + csp;
        Allure.addAttachment("DL-04 kanıtı — indirme güvenlik başlıkları", "text/plain", ev);
        List<String> failures = new ArrayList<>();
        if (r.status() != 200) {
            failures.add("HTTP " + r.status());
        }
        if (cd == null || !cd.startsWith("attachment")) {
            failures.add("Content-Disposition 'attachment' olmalı (satır içi gösterim yok)");
        }
        if (!"nosniff".equals(r.header("x-content-type-options"))) {
            failures.add("X-Content-Type-Options: nosniff olmalı");
        }
        if (!"DENY".equals(r.header("x-frame-options"))) {
            failures.add("X-Frame-Options: DENY olmalı");
        }
        if (csp == null || !csp.contains("default-src 'self'") || !csp.contains("script-src 'self'")) {
            failures.add("CSP satır içi betikleri engellemeli (default-src/script-src 'self')");
        }
        if (!failures.isEmpty()) {
            throw new AssertionError("Etkin içerik güvenli sunulmuyor:\n" + String.join("\n", failures) + "\n" + ev);
        }
    }

    // ---------------------------------------------------------------------
    // DL-05 — ASCII dışı dosya adı standart biçimde
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin indirme başlığı {string} dosya adını standart HTTP istemcisinin çözebileceği biçimde taşımalıdır")
    public void asciiDisiDosyaAdi(String documentAlias, String expectedName) {
        Raw r = rawGet(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/content", Map.of());
        String cd = r.header("content-disposition");
        String viaUtf8 = cd == null ? null : new String(cd.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        String decoded = null;
        if (cd != null) {
            Matcher star = Pattern.compile("filename\\*\\s*=\\s*UTF-8''([^;]+)", Pattern.CASE_INSENSITIVE).matcher(cd);
            Matcher plain = Pattern.compile("filename\\s*=\\s*\"([^\"]*)\"").matcher(cd);
            if (star.find()) {
                decoded = URLDecoder.decode(star.group(1).trim().replace("+", "%2B"), StandardCharsets.UTF_8);
            } else if (plain.find()) {
                decoded = plain.group(1);
            }
        }
        Allure.addAttachment("DL-05 kanıtı — Content-Disposition", "text/plain",
                "Beklenen ad: " + expectedName
                        + "\nİstemcinin gördüğü başlık (RFC 9110: ISO-8859-1): " + cd
                        + "\nİstemcinin çözdüğü ad: " + decoded
                        + "\nAynı baytlar UTF-8 okunursa: " + viaUtf8
                        + "\nfilename* (RFC 6266/8187) var mı: " + (cd != null && cd.toLowerCase().contains("filename*")));
        if (!expectedName.equals(decoded)) {
            throw new AssertionError("İndirme başlığındaki dosya adı standart istemcide '" + decoded + "' olarak çözülüyor; '"
                    + expectedName + "' olmalıydı. Sunucu ham UTF-8 baytları filename=\"…\" içine koyuyor, "
                    + "RFC 6266 filename*=UTF-8''… biçimi yok (BUG-18).");
        }
    }

    // ---------------------------------------------------------------------
    // AUD-10 — denetim kaydı ayrıntısı
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin denetim kaydı ayrıntısı listedeki kayıtla aynı olmalı, bilinmeyen kimlik 404 ve {string} için 403 dönmelidir")
    public void denetimAyrintisi(String documentAlias, String userAlias) {
        String id = ApiTestContext.id(documentAlias);
        JsonObject item = null;
        long deadline = System.currentTimeMillis() + 10000;
        while (item == null) {
            JsonObject list = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs?target_id=" + id + "&limit=50")
                    .expect(200, "Denetim listesi").json();
            for (JsonElement e : list.getAsJsonArray("items")) {
                if (!e.getAsJsonObject().get("action").getAsString().startsWith("document.read")) {
                    item = e.getAsJsonObject();
                    break;
                }
            }
            if (item == null && System.currentTimeMillis() > deadline) {
                throw new AssertionError("Belge için denetim kaydı yazılmadı");
            }
            sleep();
        }
        String auditId = item.get("id").getAsString();
        JsonObject detail = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs/" + auditId)
                .expect(200, "Denetim ayrıntısı").json();
        if (!JsonParser.parseString(detail.toString()).equals(item)) {
            throw new AssertionError("Ayrıntı listedeki kayıtla aynı olmalıydı.\nListe: " + item + "\nAyrıntı: " + detail);
        }
        int missing = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs/" + java.util.UUID.randomUUID()).status();
        int malformed = EcmApiClient.get(TestDataFactory.admin(), "/v1/admin/audit-logs/not-a-uuid").status();
        int nonAdmin = EcmApiClient.get(ApiTestContext.session(userAlias), "/v1/admin/audit-logs/" + auditId).status();
        Allure.addAttachment("AUD-10 kanıtı", "text/plain", "Kayıt " + item.get("action") + " ayrıntı == liste\nBilinmeyen kimlik -> "
                + missing + "\nBozuk kimlik -> " + malformed + "\nYönetici olmayan -> " + nonAdmin);
        if (missing != 404 || nonAdmin != 403 || malformed != 422) {
            throw new AssertionError("Beklenen: bilinmeyen 404, bozuk kimlik 422, yönetici olmayan 403; gelen " + missing + ", "
                    + malformed + ", " + nonAdmin);
        }
    }

    // ---------------------------------------------------------------------
    // CLS-20 — sınıf güncelleme (PATCH /classes/{id}, iyimser kilit)
    // ---------------------------------------------------------------------

    @Then("yönetici {string} sınıfının adını ve açıklamasını güncellediğinde kalıcı olmalı; eski sürüm, {string} adı, boş ad, yönetici olmayan {string} ve bilinmeyen sınıf reddedilmelidir")
    public void sinifGuncelleme(String classAlias, String otherClassAlias, String userAlias) {
        String id = ApiTestContext.id(classAlias);
        String path = "/v1/classes/" + id;
        long v = ClassAdminSteps.classVersion(classAlias);
        String newName = TestDataFactory.unique("QA Güncel Sınıf");
        JsonObject updated = EcmApiClient.patch(TestDataFactory.admin(), path,
                Map.of("expected_version", v, "name", newName, "description", "QA güncel açıklama")).expect(200, "Sınıf güncelleme").json();
        JsonObject read = EcmApiClient.get(TestDataFactory.admin(), path).expect(200, "Sınıf okuma").json();
        List<String> failures = new ArrayList<>();
        if (!newName.equals(read.get("name").getAsString()) || !"QA güncel açıklama".equals(read.get("description").getAsString())
                || read.get("version").getAsLong() <= v || updated.get("version").getAsLong() != read.get("version").getAsLong()) {
            failures.add("Güncelleme kalıcı değil ya da sürüm artmadı: " + read);
        }
        long now = read.get("version").getAsLong();
        EcmApiClient.ApiResponse stale = EcmApiClient.patch(TestDataFactory.admin(), path, Map.of("expected_version", v, "name", newName + " eski"));
        EcmApiClient.ApiResponse dup = EcmApiClient.patch(TestDataFactory.admin(), path,
                Map.of("expected_version", now, "name", ApiTestContext.name(otherClassAlias)));
        EcmApiClient.ApiResponse empty = EcmApiClient.patch(TestDataFactory.admin(), path, Map.of("expected_version", now, "name", "  "));
        EcmApiClient.ApiResponse nonAdmin = EcmApiClient.patch(ApiTestContext.session(userAlias), path, Map.of("expected_version", now, "name", "QA izinsiz"));
        EcmApiClient.ApiResponse unknown = EcmApiClient.patch(TestDataFactory.admin(), "/v1/classes/" + java.util.UUID.randomUUID(),
                Map.of("expected_version", 1, "name", "QA yok"));
        check(failures, "eski expected_version", stale, 409, "just updated by someone else");
        check(failures, "yinelenen ad", dup, 409, "A class with this name already exists");
        check(failures, "boş ad", empty, 422, "class name cannot be empty");
        check(failures, "yönetici olmayan", nonAdmin, 403, "");
        check(failures, "bilinmeyen sınıf", unknown, 404, "");
        JsonObject after = EcmApiClient.get(TestDataFactory.admin(), path).expect(200, "Sınıf okuma").json();
        if (!newName.equals(after.get("name").getAsString()) || after.get("version").getAsLong() != now) {
            failures.add("Reddedilen güncellemeler sınıfı değiştirmemeliydi: " + after.get("name") + " v" + after.get("version"));
        }
        // Denetim kaydı eşzamansız yazılır (AUD-01): class.update görünene kadar yoklanır.
        List<String> actions = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10000;
        do {
            actions.clear();
            JsonElement history = JsonParser.parseString(EcmApiClient.get(TestDataFactory.admin(), path + "/history").expect(200, "Sınıf geçmişi").body());
            (history.isJsonArray() ? history.getAsJsonArray() : history.getAsJsonObject().getAsJsonArray("items"))
                    .forEach(e -> actions.add(e.getAsJsonObject().get("action").getAsString()));
            if (actions.contains("class.update")) {
                break;
            }
            sleep();
        } while (System.currentTimeMillis() < deadline);
        if (actions.stream().filter("class.update"::equals).count() != 1) {
            failures.add("Geçmişte tam olarak bir class.update olmalıydı: " + actions);
        }
        Allure.addAttachment("CLS-20 kanıtı", "text/plain", "Güncelleme v" + v + " -> v" + now + "\neski: " + stale.status()
                + ", yinelenen: " + dup.status() + ", boş: " + empty.status() + ", yönetici olmayan: " + nonAdmin.status()
                + ", bilinmeyen: " + unknown.status() + "\nGeçmiş: " + actions);
        if (!failures.isEmpty()) {
            throw new AssertionError("Sınıf güncelleme sözleşmesi ihlalleri:\n" + String.join("\n", failures));
        }
    }

    private static void check(List<String> failures, String what, EcmApiClient.ApiResponse r, int status, String fragment) {
        if (r.status() != status || !r.errorMessage().contains(fragment)) {
            failures.add(what + ": beklenen " + status + " '" + fragment + "', gelen " + r.status() + " '" + r.errorMessage() + "'");
        }
    }

    // ---------------------------------------------------------------------
    // DOC-30 — sınıflı akış yükleme (POST /documents/stream)
    // ---------------------------------------------------------------------

    @Then("yönetici {string} sınıfıyla {string} klasörüne {int} MB'lık dosyayı akışla yüklediğinde içerik, sınıf ve {string} özelliği korunmalı; zorunlu özellik eksikse belge oluşmamalıdır")
    public void siniflAkisYukleme(String classAlias, String folderAlias, int mb, String propertyKey) {
        byte[] data = new byte[mb * 1024 * 1024];
        new java.util.Random().nextBytes(data);
        String title = TestDataFactory.unique("QA Akış Sınıflı");
        String value = "QA-" + System.nanoTime();
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        meta.put("class_id", ApiTestContext.id(classAlias));
        meta.put("class_version", ClassAdminSteps.classVersion(classAlias));
        meta.put("title", title);
        meta.put("metadata", Map.of(propertyKey, value));
        meta.put("mime_type", "application/octet-stream");
        meta.put("original_filename", "qa-buyuk.bin");
        meta.put("folder_id", ApiTestContext.id(folderAlias));
        EcmApiClient.ApiResponse r = EcmApiClient.postMultipart(TestDataFactory.admin(), "/v1/documents/stream", List.of(
                new EcmApiClient.Part("metadata", null, "application/json", new com.google.gson.Gson().toJson(meta).getBytes(StandardCharsets.UTF_8)),
                new EcmApiClient.Part("file", "qa-buyuk.bin", "application/octet-stream", data)));
        r.expect(201, "Sınıflı akış yükleme");
        String id = r.json().get("document_id").getAsString();
        TestDataFactory.registerDocument("akış belgesi", id, title);
        JsonObject doc = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id).expect(200, "Belge").json();
        List<String> failures = new ArrayList<>();
        if (!ApiTestContext.id(classAlias).equals(doc.getAsJsonObject("document").get("class_id").getAsString())) {
            failures.add("Belge sınıfı eşleşmiyor");
        }
        boolean propOk = false;
        for (JsonElement e : doc.getAsJsonArray("property_values")) {
            JsonElement v = e.getAsJsonObject().get("value");
            propOk |= v != null && !v.isJsonNull() && value.equals(v.getAsString());
        }
        if (!propOk) {
            failures.add("'" + propertyKey + "' özellik değeri saklanmadı: " + doc.getAsJsonArray("property_values"));
        }
        byte[] got = rawGet(TestDataFactory.admin(), "/v1/documents/" + id + "/content", Map.of()).body();
        JsonObject cv = doc.getAsJsonObject("current_version");
        if (!sha256(got).equals(sha256(data)) || !sha256(data).equals(cv.get("checksum_sha256").getAsString())
                || cv.get("file_size_bytes").getAsLong() != data.length) {
            failures.add("İçerik/sağlama toplamı/boyut yüklenen dosyayla aynı değil");
        }
        meta.put("title", title + " eksik");
        meta.put("metadata", Map.of());
        EcmApiClient.ApiResponse missing = EcmApiClient.postMultipart(TestDataFactory.admin(), "/v1/documents/stream", List.of(
                new EcmApiClient.Part("metadata", null, "application/json", new com.google.gson.Gson().toJson(meta).getBytes(StandardCharsets.UTF_8)),
                new EcmApiClient.Part("file", "qa-buyuk.bin", "application/octet-stream", data)));
        check(failures, "zorunlu özellik eksik", missing, 422, "missing required property '" + propertyKey + "'");
        int leaked = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents?limit=200&title="
                + java.net.URLEncoder.encode(title + " eksik", StandardCharsets.UTF_8)).json().getAsJsonArray("items").size();
        if (leaked != 0) {
            failures.add("Reddedilen akış yüklemesi belge oluşturdu");
        }
        Allure.addAttachment("DOC-30 kanıtı", "text/plain", mb + " MB akış yüklemesi -> 201, SHA-256 eşleşiyor, sınıf ve '" + propertyKey
                + "'=" + value + " saklandı; zorunlu özellik eksik -> " + missing.status() + " '" + missing.errorMessage() + "'");
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures));
        }
    }

    // ---------------------------------------------------------------------
    // VER-12 — sürüm etiketi listeleri (sürüm bazlı ve belge bazlı) tutarlı
    // ---------------------------------------------------------------------

    @Then("{string} belgesinin sürüm bazlı ve belge bazlı etiket listeleri her sürüm için aynı olmalıdır")
    public void surumEtiketListeleriTutarli(String documentAlias) {
        String id = ApiTestContext.id(documentAlias);
        JsonObject byVersion = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/version-labels")
                .expect(200, "Belge sürüm etiketleri").json().getAsJsonObject("labels_by_version");
        StringBuilder ev = new StringBuilder();
        List<String> failures = new ArrayList<>();
        int total = 0;
        for (JsonObject v : VersionSteps.versionsAsc(documentAlias)) {
            String vid = v.get("id").getAsString();
            java.util.TreeSet<String> per = new java.util.TreeSet<>();
            EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + id + "/versions/" + vid + "/labels").expect(200, "Sürüm etiketleri")
                    .json().getAsJsonArray("labels").forEach(e -> per.add(e.getAsJsonObject().get("label").getAsString()));
            java.util.TreeSet<String> map = new java.util.TreeSet<>();
            if (byVersion.has(vid)) {
                byVersion.getAsJsonArray(vid).forEach(e -> map.add(e.getAsJsonObject().get("label").getAsString()));
            }
            java.util.TreeSet<String> inList = new java.util.TreeSet<>();
            v.getAsJsonArray("labels").forEach(e -> inList.add(e.isJsonObject() ? e.getAsJsonObject().get("label").getAsString() : e.getAsString()));
            ev.append(label(v)).append(": sürüm ucu ").append(per).append(", belge ucu ").append(map).append(", sürüm listesi ").append(inList).append('\n');
            if (!per.equals(map) || !per.equals(inList)) {
                failures.add(label(v) + " etiketleri uçlar arasında farklı");
            }
            total += per.size();
        }
        if (total == 0) {
            failures.add("Kontrol anlamsız: hiçbir sürümde etiket yok");
        }
        Allure.addAttachment("VER-12 kanıtı", "text/plain", ev.toString());
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures) + "\n" + ev);
        }
    }

    @Given("{string} belgesinin {int}. sürümüne {string} sürüm etiketi eklenmiştir")
    public void nSurumeEtiket(String documentAlias, int ordinal, String label) {
        String vid = VersionSteps.versionsAsc(documentAlias).get(ordinal - 1).get("id").getAsString();
        EcmApiClient.post(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/versions/" + vid + "/labels",
                Map.of("label", label)).expect(201, "Sürüm etiketi");
    }

    // ---------------------------------------------------------------------
    // PT-10 — özellik şablonu ayrıntısı
    // ---------------------------------------------------------------------

    @Then("{string} özellik şablonunun ayrıntısı listedeki kayıtla aynı olmalı, bilinmeyen şablon 404 ve {string} için 403 dönmelidir")
    public void sablonAyrintisi(String templateAlias, String userAlias) {
        String id = ApiTestContext.id(templateAlias);
        JsonObject listed = null;
        JsonElement all = JsonParser.parseString(EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates").expect(200, "Şablonlar").body());
        for (JsonElement e : all.isJsonArray() ? all.getAsJsonArray() : all.getAsJsonObject().getAsJsonArray("items")) {
            if (id.equals(e.getAsJsonObject().get("id").getAsString())) {
                listed = e.getAsJsonObject();
            }
        }
        JsonObject detail = EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates/" + id).expect(200, "Şablon ayrıntısı").json();
        int unknown = EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates/" + java.util.UUID.randomUUID()).status();
        int nonAdmin = EcmApiClient.get(ApiTestContext.session(userAlias), "/v1/property-templates/" + id).status();
        Allure.addAttachment("PT-10 kanıtı", "text/plain", "Ayrıntı: " + detail + "\nListe: " + listed + "\nBilinmeyen: " + unknown
                + ", yönetici olmayan: " + nonAdmin);
        if (listed == null || !listed.equals(detail) || unknown != 404 || nonAdmin != 403) {
            throw new AssertionError("Şablon ayrıntısı sözleşmesi: liste==ayrıntı " + detail.equals(listed) + ", bilinmeyen " + unknown
                    + " (404), yönetici olmayan " + nonAdmin + " (403)");
        }
    }

    // ---------------------------------------------------------------------
    // OPS-01 — işletim uçları
    // ---------------------------------------------------------------------

    @Then("işletim uçları kimliksiz erişimde yalnız durum bilgisi vermeli, sistem bilgisi kimlik istemeli ve metrik erişimi RQ-28 kanıtı olarak kaydedilmelidir")
    public void isletimUclari() {
        StringBuilder ev = new StringBuilder();
        List<String> failures = new ArrayList<>();
        for (String p : List.of("/v1/health", "/v1/startup", "/v1/ready", "/v1/info", "/v1/metrics")) {
            EcmApiClient.ApiResponse r = EcmApiClient.anonymous("GET", p, null);
            String body = r.body() == null ? "" : r.body();
            ev.append(p).append(" -> ").append(r.status()).append(" ").append(body.length() > 160 ? body.substring(0, 160) + "…" : body).append('\n');
            switch (p) {
                case "/v1/health" -> { if (r.status() != 200 || !body.contains("\"ok\"")) failures.add("health 200 ok olmalı"); }
                case "/v1/startup" -> { if (r.status() != 200 || !body.contains("started")) failures.add("startup 200 started olmalı"); }
                case "/v1/ready" -> { if (r.status() != 200 || !body.contains("\"postgres\":\"ok\"") || !body.contains("\"storage\":\"ok\"")) failures.add("ready bağımlılıkları ok olmalı"); }
                case "/v1/info" -> { if (r.status() != 401) failures.add("info kimliksiz 401 olmalı"); }
                // Metrik adları "token" kelimesini içerebilir (ecm_auth_token_refresh_total); aranan, gerçek sır/kişisel veri değerleridir.
                default -> {
                    if (r.status() != 200 || Pattern.compile("eyJ[A-Za-z0-9_-]{10,}|Bearer\\s|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+|password\\s*[=:]", Pattern.CASE_INSENSITIVE)
                            .matcher(body).find()) {
                        failures.add("metrikler 200 dönmeli ve sır/kişisel veri değeri içermemeli");
                    }
                }
            }
        }
        Allure.addAttachment("OPS-01 kanıtı (RQ-28: /v1/metrics ve /v1/ready kimliksiz açık)", "text/plain", ev.toString());
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures) + "\n" + ev);
        }
    }

    // ---------------------------------------------------------------------

    private static void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String label(JsonObject v) {
        return "v" + v.get("version_major").getAsInt() + "." + v.get("version_minor").getAsInt() + "." + v.get("version_patch").getAsInt();
    }
}
