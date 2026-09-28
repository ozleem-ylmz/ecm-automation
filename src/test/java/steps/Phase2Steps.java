package steps;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Phase 2 (kaynak kodu güdümlü kapsam) ortak API adımları.
 *
 * Senaryolar aktör takma adlarıyla ("yönetici", "ayşe" ...) istek gönderir;
 * gövde ve yol içindeki yer tutucular ApiTestContext.resolve() ve aşağıdaki
 * dinamik belirteçlerle çözülür:
 *   {ALIAS}              kayıtlı id (ApiTestContext)
 *   {name:ALIAS}         kayıtlı ad / e-posta
 *   {classver:ALIAS}     sınıfın güncel version değeri (canlı okunur)
 *   {curver:ALIAS}       belgenin güncel sürüm id'si (canlı okunur)
 *   {bytes:metin}        UTF-8 bayt dizisi (JSON int array)
 *   {repeat:N:c}         c karakterinin N kez tekrarı
 *   {ROOT} {ALL_USERS} {ROLE_VIEWER} {ROLE_EDITOR} {ROLE_MANAGER} {ADMIN_ID}
 * Kalıcılık yönetici gözüyle yeniden okunarak doğrulanır (DB sürücüsü yok).
 */
public class Phase2Steps {

    private static final Gson GSON = new Gson();
    private static final Pattern NAME = Pattern.compile("\\{name:([^}]+)}");
    private static final Pattern CLASSVER = Pattern.compile("\\{classver:([^}]+)}");
    private static final Pattern CURVER = Pattern.compile("\\{curver:([^}]+)}");
    private static final Pattern BYTES = Pattern.compile("\\{bytes:([^}]*)}");
    private static final Pattern REPEAT = Pattern.compile("\\{repeat:(\\d+):([^}])}");

    private static final Map<String, String> SNAPSHOTS = new LinkedHashMap<>();
    private static List<EcmApiClient.ApiResponse> concurrent = List.of();

    // ------------------------------------------------------------------ çözümleme

    static EcmApiClient.Session actor(String alias) {
        if ("yönetici".equals(alias)) {
            return TestDataFactory.admin();
        }
        return ApiTestContext.session(alias);
    }

    static String resolve(String text) {
        if (text == null) {
            return null;
        }
        String out = text;
        out = replaceAll(out, REPEAT, m -> String.valueOf(m.group(2)).repeat(Integer.parseInt(m.group(1))));
        out = replaceAll(out, BYTES, m -> bytes(m.group(1)));
        out = replaceAll(out, NAME, m -> ApiTestContext.name(m.group(1)));
        out = replaceAll(out, CLASSVER, m -> classVersion(m.group(1)));
        out = replaceAll(out, CURVER, m -> currentVersion(m.group(1)));
        out = out.replace("{ROOT}", BrowserManager.ROOT_FOLDER_ID)
                .replace("{ALL_USERS}", TestDataFactory.ALL_USERS_GROUP)
                .replace("{ROLE_VIEWER}", TestDataFactory.ROLE_VIEWER)
                .replace("{ROLE_EDITOR}", TestDataFactory.ROLE_EDITOR)
                .replace("{ROLE_MANAGER}", TestDataFactory.ROLE_MANAGER);
        if (out.contains("{ADMIN_ID}")) {
            out = out.replace("{ADMIN_ID}", TestDataFactory.admin().userId);
        }
        return ApiTestContext.resolve(out);
    }

    private interface Repl {
        String apply(Matcher m);
    }

    private static String replaceAll(String in, Pattern p, Repl r) {
        Matcher m = p.matcher(in);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(r.apply(m)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /* Yol ve sorgu değerlerini güvenli biçimde percent-encode eder (boşluk, Unicode). */
    static String encodeUrl(String p) {
        if (p == null) {
            return null;
        }
        int q = p.indexOf('?');
        String path = q < 0 ? p : p.substring(0, q);
        String query = q < 0 ? null : p.substring(q + 1);
        StringBuilder sb = new StringBuilder();
        String[] segs = path.split("/", -1);
        for (int i = 0; i < segs.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(enc(segs[i]));
        }
        if (query != null) {
            sb.append('?');
            String[] pairs = query.split("&", -1);
            for (int i = 0; i < pairs.length; i++) {
                if (i > 0) {
                    sb.append('&');
                }
                int eq = pairs[i].indexOf('=');
                if (eq < 0) {
                    sb.append(enc(pairs[i]));
                } else {
                    sb.append(enc(pairs[i].substring(0, eq))).append('=').append(enc(pairs[i].substring(eq + 1)));
                }
            }
        }
        return sb.toString();
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String bytes(String text) {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < b.length; i++) {
            sb.append(i == 0 ? "" : ",").append(b[i] & 0xff);
        }
        return sb.append("]").toString();
    }

    private static String classVersion(String alias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(alias))
                .expect(200, "Sınıf okuma (" + alias + ")").json().get("version").getAsString();
    }

    private static String currentVersion(String alias) {
        JsonObject d = EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(alias))
                .expect(200, "Belge okuma (" + alias + ")").json();
        return d.getAsJsonObject("current_version").get("id").getAsString();
    }

    /* "a.b[0].c" biçiminde basit yol; "" kök. Bulunamazsa null. */
    static JsonElement at(JsonElement root, String path) {
        if (path == null || path.isBlank()) {
            return root;
        }
        JsonElement cur = root;
        for (String seg : path.split("\\.")) {
            String key = seg;
            List<Integer> idx = new ArrayList<>();
            Matcher m = Pattern.compile("\\[(\\d+)]").matcher(seg);
            int cut = seg.indexOf('[');
            if (cut >= 0) {
                key = seg.substring(0, cut);
                while (m.find()) {
                    idx.add(Integer.parseInt(m.group(1)));
                }
            }
            if (!key.isEmpty()) {
                if (cur == null || !cur.isJsonObject() || !cur.getAsJsonObject().has(key)) {
                    return null;
                }
                cur = cur.getAsJsonObject().get(key);
            }
            for (int i : idx) {
                if (cur == null || !cur.isJsonArray() || cur.getAsJsonArray().size() <= i) {
                    return null;
                }
                cur = cur.getAsJsonArray().get(i);
            }
        }
        return cur;
    }

    static String text(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return "null";
        }
        if (e.isJsonPrimitive()) {
            return e.getAsString();
        }
        return e.toString();
    }

    private static JsonElement lastJson() {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        try {
            return JsonParser.parseString(r.body());
        } catch (RuntimeException e) {
            throw new AssertionError("Yanıt JSON değil (HTTP " + r.status() + "): " + r.body());
        }
    }

    private static JsonArray array(String path) {
        JsonElement e = at(lastJson(), path);
        if (e == null || !e.isJsonArray()) {
            throw new AssertionError("'" + path + "' bir liste değil. Yanıt: " + EcmApiClient.redact(ApiTestContext.lastResponse().body()));
        }
        return e.getAsJsonArray();
    }

    // ------------------------------------------------------------------ kurulum

    @Given("{string} aktörü {string} hesap rolüyle hazırlanmıştır")
    public void aktorHazirlanmistir(String alias, String accountRole) {
        TestDataFactory.createUser(alias, accountRole);
        TestDataFactory.loginAs(alias);
    }

    @Given("{string} aktörü {string} grubuna üye yapılmıştır")
    public void aktorGrubaUye(String userAlias, String groupAlias) {
        TestDataFactory.addMember(groupAlias, userAlias);
    }

    @Given("{string} klasörü {string} altında {string} tarafından miras {string} olarak oluşturulmuştur")
    public void klasorOlusturulmustur(String alias, String parentAlias, String creator, String inherit) {
        TestDataFactory.createFolder(actor(creator), alias, "kök".equals(parentAlias) ? null : parentAlias,
                "açık".equals(inherit));
    }

    @Given("{string} klasöründe {string} için {string} rolü {string} olarak verilmiştir")
    public void klasorYetkisi(String folderAlias, String principal, String role, String kind) {
        boolean group = kind.contains("grup");
        boolean deny = kind.contains("ret");
        TestDataFactory.grantFolder(folderAlias, TestDataFactory.roleId(role), principal, group, deny);
    }

    @Given("{string} aktörü {string} klasörüne {string} sınıfsız belgesini yüklemiştir")
    public void siniflizBelge(String actorAlias, String folderAlias, String docAlias) {
        TestDataFactory.createUnclassifiedDocument(actor(actorAlias), docAlias, "kök".equals(folderAlias) ? null : folderAlias);
    }

    @Given("{string} aktörü {string} klasörüne {string} sınıfından {string} belgesini {string} metadatasıyla yüklemiştir")
    public void sinifliBelge(String actorAlias, String folderAlias, String classAlias, String docAlias, String metadataJson) {
        String title = TestDataFactory.unique("QA P2 Belge");
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(title, ApiTestContext.id(classAlias),
                Long.parseLong(classVersion(classAlias)),
                GSON.fromJson(resolve(metadataJson), Map.class),
                "kök".equals(folderAlias) ? null : folderAlias);
        EcmApiClient.ApiResponse r = EcmApiClient.post(actor(actorAlias), "/v1/documents", body);
        r.expect(201, "Sınıflı belge oluşturma (" + docAlias + ")");
        TestDataFactory.registerDocument(docAlias, r.json().get("document_id").getAsString(), title);
    }

    @Given("{string} belgesi yönetici tarafından çöp kutusuna taşınmıştır")
    public void belgeSilinmis(String docAlias) {
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(docAlias))
                .expect(200, "Soft delete (" + docAlias + ")");
    }

    @Given("{string} klasörü yönetici tarafından pasifleştirilmiştir")
    public void klasorPasif(String folderAlias) {
        EcmApiClient.put(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/active",
                Map.of("is_active", false)).expect(200, "Klasör pasifleştirme (" + folderAlias + ")");
    }

    @Given("{string} belgesi {string} tarafından kilitlenmiştir")
    public void belgeKilitli(String docAlias, String actorAlias) {
        EcmApiClient.post(actor(actorAlias), "/v1/documents/" + ApiTestContext.id(docAlias) + "/checkout", Map.of())
                .expect(200, "Checkout (" + docAlias + ")");
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(),
                "/v1/documents/" + ApiTestContext.id(docAlias) + "/lock"));
    }

    @Given("{string} değeri {string} kimliği olarak saklanmıştır")
    public void degerSaklanmis(String value, String alias) {
        ApiTestContext.putId(alias, resolve(value));
    }

    @Given("yönetici {string} sınıfının {string} özelliğinin kimliğini {string} olarak saklamıştır")
    public void ozellikKimligiSaklanmis(String classAlias, String key, String alias) {
        JsonObject cls = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias))
                .expect(200, "Sınıf okuma (özellik kimliği)").json();
        for (JsonElement p : cls.getAsJsonArray("properties")) {
            if (key.equals(p.getAsJsonObject().get("key").getAsString())) {
                ApiTestContext.putId(alias, p.getAsJsonObject().get("id").getAsString());
                return;
            }
        }
        throw new AssertionError("'" + classAlias + "' sınıfında '" + key + "' özelliği yok.");
    }

    // ------------------------------------------------------------------ istek

    private static EcmApiClient.ApiResponse send(String actorAlias, String method, String path, String body) {
        String p = encodeUrl(resolve(path));
        // GET'ler uzun zaman aşımıyla gider: admin olmayan klasör seçici/ağaç BUG-03 nedeniyle yavaştır.
        EcmApiClient.ApiResponse r = "GET".equalsIgnoreCase(method)
                ? EcmApiClient.getSlow(actor(actorAlias), p)
                : EcmApiClient.sendRawJson(actor(actorAlias), method, p, body == null ? null : resolve(body));
        ApiTestContext.lastResponse(r);
        return r;
    }

    @When("{string} şu isteği gönderir: {word} {string}")
    public void istekGonderir(String actorAlias, String method, String path) {
        send(actorAlias, method, path, null);
    }

    @When("{string} şu isteği gönderir: {word} {string} gövde {string}")
    public void istekGovdeyleGonderir(String actorAlias, String method, String path, String body) {
        send(actorAlias, method, path, body);
    }

    @When("{string} {string} idempotency anahtarıyla şu isteği gönderir: POST {string} gövde {string}")
    public void idempotentIstek(String actorAlias, String key, String path, String body) {
        // JsonParser bytes/int'leri metin olarak korur (GSON.fromJson->Object float'a çevirir, content u8 doğrulamasını bozar).
        Object b = body == null || body.isBlank() ? null : JsonParser.parseString(resolve(body));
        EcmApiClient.ApiResponse r = EcmApiClient.postWithHeaders(actor(actorAlias), encodeUrl(resolve(path)), b,
                Map.of("Idempotency-Key", resolve(key)));
        ApiTestContext.lastResponse(r);
    }

    @When("oturumsuz şu istek gönderilir: {word} {string} gövde {string}")
    public void oturumsuzIstek(String method, String path, String body) {
        EcmApiClient.ApiResponse r = EcmApiClient.anonymous(method, encodeUrl(resolve(path)),
                body == null || body.isBlank() ? null : GSON.fromJson(resolve(body), Object.class));
        ApiTestContext.lastResponse(r);
    }

    @Given("{string} şu isteği göndermiş ve {int} almıştır: {word} {string} gövde {string}")
    public void istekOnkosul(String actorAlias, int expected, String method, String path, String body) {
        send(actorAlias, method, path, body.isBlank() ? null : body).expect(expected, method + " " + path);
    }

    @When("{string} şu isteği {int} kez eşzamanlı gönderir: {word} {string} gövde {string}")
    public void eszamanli(String actorAlias, int times, String method, String path, String body) throws Exception {
        EcmApiClient.Session s = actor(actorAlias);
        String p = encodeUrl(resolve(path));
        String b = body.isBlank() ? null : resolve(body);
        ExecutorService pool = Executors.newFixedThreadPool(times);
        try {
            List<Callable<EcmApiClient.ApiResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < times; i++) {
                tasks.add(() -> EcmApiClient.sendRawJson(s, method, p, b));
            }
            List<EcmApiClient.ApiResponse> out = new ArrayList<>();
            for (Future<EcmApiClient.ApiResponse> f : pool.invokeAll(tasks)) {
                out.add(f.get());
            }
            concurrent = out;
            ApiTestContext.lastResponse(out.get(0));
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ doğrulama

    @Then("yanıt durumu {int} olmalıdır")
    public void yanitDurumu(int expected) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() != expected) {
            throw new AssertionError("HTTP " + expected + " bekleniyordu, " + r.status() + " geldi. Yanıt: "
                    + EcmApiClient.redact(r.body()));
        }
    }

    @Then("yanıt durumu 4xx istemci hatası olmalıdır")
    public void yanitDurumu4xx() {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() < 400 || r.status() >= 500) {
            throw new AssertionError("Denetimli 4xx bekleniyordu, " + r.status() + " geldi. Yanıt: "
                    + EcmApiClient.redact(r.body()));
        }
    }

    @Then("yanıt durumu sunucu hatası olmamalıdır")
    public void yanitDurumu5xxDegil() {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() >= 500) {
            throw new AssertionError("Sunucu hatası beklenmiyordu, " + r.status() + " geldi. Yanıt: "
                    + EcmApiClient.redact(r.body()));
        }
    }

    @Then("yanıt hata kodu {string} olmalıdır")
    public void yanitHataKodu(String code) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (!code.equals(r.errorCode())) {
            throw new AssertionError("Hata kodu '" + code + "' bekleniyordu, '" + r.errorCode() + "' geldi (HTTP "
                    + r.status() + "). Yanıt: " + EcmApiClient.redact(r.body()));
        }
    }

    @Then("yanıt hata mesajı {string} içermelidir")
    public void yanitHataMesaji(String fragment) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (!r.errorMessage().contains(resolve(fragment))) {
            throw new AssertionError("Hata mesajı '" + fragment + "' içermeliydi: '" + r.errorMessage() + "' (HTTP "
                    + r.status() + ")");
        }
    }

    @Then("yanıt hata mesajı {string} içermemelidir")
    public void yanitHataMesajiIcermemeli(String fragment) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.errorMessage().toLowerCase().contains(resolve(fragment).toLowerCase())) {
            throw new AssertionError("Hata mesajı '" + fragment + "' içermemeliydi: '" + r.errorMessage() + "'");
        }
    }

    @Then("yanıt alanı {string} değeri {string} olmalıdır")
    public void alanDegeri(String path, String expected) {
        String actual = text(at(lastJson(), path));
        String want = resolve(expected);
        if (!want.equals(actual)) {
            throw new AssertionError("'" + path + "' = '" + want + "' bekleniyordu, '" + actual + "' geldi. Yanıt: "
                    + EcmApiClient.redact(ApiTestContext.lastResponse().body()));
        }
    }

    @Then("yanıt alanı {string} değeri {string} içermelidir")
    public void alanDegeriIcermeli(String path, String fragment) {
        String actual = text(at(lastJson(), path));
        String want = resolve(fragment);
        if (!actual.contains(want)) {
            throw new AssertionError("'" + path + "' değeri '" + want + "' içermeliydi, '" + actual + "' geldi.");
        }
    }

    @Then("yanıt alanı {string} değeri {string} olmamalıdır")
    public void alanDegeriOlmamali(String path, String unexpected) {
        String actual = text(at(lastJson(), path));
        if (resolve(unexpected).equals(actual)) {
            throw new AssertionError("'" + path + "' = '" + actual + "' olmamalıydı.");
        }
    }

    @Then("yanıt alanı {string} bulunmalıdır")
    public void alanBulunmali(String path) {
        JsonElement e = at(lastJson(), path);
        if (e == null || e.isJsonNull()) {
            throw new AssertionError("'" + path + "' alanı bulunmalıydı. Yanıt: "
                    + EcmApiClient.redact(ApiTestContext.lastResponse().body()));
        }
    }

    @Then("yanıt alanı {string} bulunmamalı veya boş olmalıdır")
    public void alanBulunmamali(String path) {
        JsonElement e = at(lastJson(), path);
        if (e != null && !e.isJsonNull() && !(e.isJsonArray() && e.getAsJsonArray().isEmpty())
                && !(e.isJsonPrimitive() && e.getAsString().isEmpty())) {
            throw new AssertionError("'" + path + "' boş olmalıydı, '" + text(e) + "' geldi.");
        }
    }

    @Then("yanıt listesi {string} içinde {string} değeri {string} olan öğe bulunmalıdır")
    public void listeOgeBulunmali(String path, String field, String value) {
        String want = resolve(value);
        for (JsonElement el : array(path)) {
            if (want.equals(text(at(el, field)))) {
                return;
            }
        }
        throw new AssertionError("'" + path + "' içinde " + field + "='" + want + "' bulunmalıydı. Yanıt: "
                + EcmApiClient.redact(ApiTestContext.lastResponse().body()));
    }

    @Then("yanıt listesi {string} içinde {string} değeri {string} olan öğe bulunmamalıdır")
    public void listeOgeBulunmamali(String path, String field, String value) {
        String want = resolve(value);
        for (JsonElement el : array(path)) {
            if (want.equals(text(at(el, field)))) {
                throw new AssertionError("'" + path + "' içinde " + field + "='" + want + "' bulunmamalıydı.");
            }
        }
    }

    @Then("yanıt listesi {string} uzunluğu {int} olmalıdır")
    public void listeUzunlugu(String path, int size) {
        int actual = array(path).size();
        if (actual != size) {
            throw new AssertionError("'" + path + "' uzunluğu " + size + " bekleniyordu, " + actual + " geldi. Yanıt: "
                    + EcmApiClient.redact(ApiTestContext.lastResponse().body()));
        }
    }

    @Then("yanıt gövdesi {string} içermemelidir")
    public void govdeIcermemeli(String fragment) {
        String body = ApiTestContext.lastResponse().body();
        if (body != null && body.contains(resolve(fragment))) {
            throw new AssertionError("Yanıt gövdesi '" + fragment + "' içermemeliydi: " + EcmApiClient.redact(body));
        }
    }

    @Then("yanıt gövdesi {string} içermelidir")
    public void govdeIcermeli(String fragment) {
        String body = ApiTestContext.lastResponse().body();
        if (body == null || !body.contains(resolve(fragment))) {
            throw new AssertionError("Yanıt gövdesi '" + fragment + "' içermeliydi: " + EcmApiClient.redact(body));
        }
    }

    @Then("yanıt alanı {string} {string} kimliği olarak saklanır")
    public void alanSaklanir(String path, String alias) {
        String v = text(at(lastJson(), path));
        if ("null".equals(v)) {
            throw new AssertionError("'" + path + "' saklanamadı: yanıtta yok. " + ApiTestContext.lastResponse().body());
        }
        ApiTestContext.putId(alias, v);
    }

    @Then("yanıt alanı {string} {string} adı olarak saklanır")
    public void alanAdSaklanir(String path, String alias) {
        String v = text(at(lastJson(), path));
        if ("null".equals(v)) {
            throw new AssertionError("'" + path + "' saklanamadı: yanıtta yok. " + ApiTestContext.lastResponse().body());
        }
        ApiTestContext.putName(alias, v);
    }

    @Then("yanıt Set-Cookie başlığı {string} içermelidir")
    public void cookieIcermeli(String fragment) {
        String c = ApiTestContext.lastResponse().setCookie();
        if (c == null || !c.contains(fragment)) {
            throw new AssertionError("Set-Cookie '" + fragment + "' içermeliydi: " + (c == null ? "<yok>" : c.replaceAll("refresh_token=[^;]*", "refresh_token=<redacted>")));
        }
    }

    // ------------------------------------------------------------------ anlık görüntü

    @Given("yönetici {string} yanıtının anlık görüntüsünü {string} olarak almıştır")
    public void anlikGoruntu(String path, String key) {
        SNAPSHOTS.put(key, EcmApiClient.get(TestDataFactory.admin(), encodeUrl(resolve(path))).body());
    }

    @Then("yönetici {string} yanıtı {string} anlık görüntüsüyle aynı olmalıdır")
    public void anlikGoruntuAyni(String path, String key) {
        String now = EcmApiClient.get(TestDataFactory.admin(), encodeUrl(resolve(path))).body();
        String before = SNAPSHOTS.get(key);
        if (!JsonParser.parseString(before).equals(JsonParser.parseString(now))) {
            throw new AssertionError("Reddedilen/başarısız istek kalıcı durumu değiştirdi (" + path + ")\nÖnce: "
                    + EcmApiClient.redact(before) + "\nSonra: " + EcmApiClient.redact(now));
        }
        io.qameta.allure.Allure.addAttachment("Değişmeyen durum " + key, "application/json", EcmApiClient.redact(now));
    }

    // ------------------------------------------------------------------ audit

    private static int auditCount(String action, String targetId) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(),
                "/v1/admin/audit-logs?limit=200&action=" + action + "&target_id=" + targetId);
        r.expect(200, "Audit listesi");
        JsonElement items = r.json().get("items");
        return items == null ? 0 : items.getAsJsonArray().size();
    }

    @Then("{string} yolunda {string} eylemi bulunmalıdır")
    public void yolEyleminiIcerir(String path, String action) throws InterruptedException {
        String p = encodeUrl(resolve(path));
        for (int i = 0; i < 40; i++) {
            EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), p);
            if (r.status() == 200) {
                JsonElement items = at(JsonParser.parseString(r.body()), "items");
                if (items != null && items.isJsonArray()) {
                    for (JsonElement e : items.getAsJsonArray()) {
                        if (action.equals(text(at(e, "action")))) {
                            return;
                        }
                    }
                }
            }
            Thread.sleep(250);
        }
        throw new AssertionError("'" + action + "' eylemi " + path + " geçmişinde bulunmalıydı.");
    }

    @Then("{string} yolunda {string} eylemi bulunmalı ama {string} eylemi bulunmamalıdır")
    public void yolEyleminiIcerirDigeriniIcermez(String path, String present, String absent) throws InterruptedException {
        yolEyleminiIcerir(path, present);
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), encodeUrl(resolve(path)));
        for (JsonElement e : r.json().getAsJsonArray("items")) {
            if (absent.equals(text(at(e, "action")))) {
                throw new AssertionError("'" + absent + "' eylemi " + path + " geçmişinde bulunmamalıydı.");
            }
        }
    }

    @Then("{string} hedefi için {string} audit kaydı bulunmalıdır")
    public void auditBulunmali(String targetAlias, String action) throws InterruptedException {
        String target = resolve("{" + targetAlias + "}");
        for (int i = 0; i < 40; i++) {
            if (auditCount(action, target) > 0) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("'" + action + "' audit kaydı " + targetAlias + " (" + target + ") için bulunmalıydı.");
    }

    @Then("{string} hedefi için {string} audit kaydı bulunmamalıdır")
    public void auditBulunmamali(String targetAlias, String action) throws InterruptedException {
        String target = resolve("{" + targetAlias + "}");
        // audit_service toplu yazar (≈500 ms pencere); gecikmeli bir kaydı da yakalamak için bekle.
        Thread.sleep(2000);
        int n = auditCount(action, target);
        if (n != 0) {
            throw new AssertionError("'" + action + "' audit kaydı " + targetAlias + " için bulunmamalıydı; " + n + " adet var.");
        }
    }

    // ------------------------------------------------------------------ eşzamanlılık

    @Then("eşzamanlı yanıtlarda sunucu hatası bulunmamalıdır")
    public void eszamanliNo5xx() {
        List<Integer> statuses = concurrent.stream().map(EcmApiClient.ApiResponse::status).toList();
        if (statuses.stream().anyMatch(s -> s >= 500)) {
            throw new AssertionError("Eşzamanlı yanıtlarda 5xx olmamalıydı: " + statuses);
        }
    }

    @Then("eşzamanlı yanıtlarda {int} durumunda tam {int} yanıt olmalıdır")
    public void eszamanliSayim(int status, int count) {
        List<Integer> statuses = concurrent.stream().map(EcmApiClient.ApiResponse::status).toList();
        long n = statuses.stream().filter(s -> s == status).count();
        if (n != count) {
            throw new AssertionError(status + " durumunda " + count + " yanıt bekleniyordu: " + statuses);
        }
    }
}
