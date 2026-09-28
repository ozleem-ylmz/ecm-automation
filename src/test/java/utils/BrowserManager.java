package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserManager {

    public static Playwright playwright;
    public static Browser browser;
    public static Page page;

    /*
     * Playwright driver'ı ve Chromium process'i tüm koşu boyunca bir kez
     * başlatılır (en pahalı kısım). İzolasyon her senaryoya açılan yeni
     * BrowserContext ile sağlanır: cookie, localStorage, sessionStorage ve
     * cache context'e özeldir ve senaryo sonunda context kapatılır.
     */
    public static BrowserContext context;

    // Senaryonun ana context'inde API ile login olan kullanıcının access
    // token'ı; context ile birlikte senaryo sonunda temizlenir.
    private static String accessToken;

    public static BrowserContext managerContext;
    public static Page managerPage;

    /*
     * Varsayılan adresler yalnızca yapılandırma verilmediğinde kullanılır.
     * Öncelik: -Decm.baseUrl / -Decm.apiUrl / -Decm.appApiUrl, sonra
     * ECM_BASE_URL / ECM_API_URL / ECM_APP_API_URL ortam değişkenleri.
     *
     * NOT: Bu makinede portlar zaman içinde değişti (bir dönem ECM 5174,
     * Test Platform 5173 idi; 2026-09-25 itibarıyla ECM 5173, Test Platform
     * 5174). Hangi portun doğru olduğu varsayılmaz: verifyEnvironment()
     * koşu başında adresin gerçekten ECM'i sunduğunu doğrular ve değilse
     * senaryolar başlamadan açık bir hatayla durur.
     */
    private static final String DEFAULT_BASE_URL = "http://localhost:5174";

    // Test setup API çağrıları IPv4 ile açıkça yapılır (backend 127.0.0.1'de dinliyor).
    private static final String DEFAULT_API_URL = "http://127.0.0.1:8080";

    // ECM frontend'inin tarayıcıdan çağırdığı API adresi (ECM .env ->
    // VITE_API_BASE_URL). Refresh cookie'si bu host için de yazılmalı ki
    // uygulama oturumu cookie ile geri yükleyebilsin.
    private static final String DEFAULT_APP_API_URL = "http://localhost:8080";

    private static String currentScenarioName;
    private static String currentScenarioUri;
    private static String currentScenarioId;

    private static final Set<String> scenarioEndpoints =
            new LinkedHashSet<>();

    /*
     * Assertion hata mesajlarında kanıt olarak kullanmak için senaryo boyunca
     * API'nin döndürdüğü belge status değerlerini ve status'u etkileyen
     * işlemleri (status PATCH, yeni versiyon POST) sırasıyla tutuyoruz.
     */
    private static final List<String> documentStatusEvidence =
            new ArrayList<>();

    private static final Pattern DOCUMENT_DETAIL_PATH =
            Pattern.compile("^/v1/documents/([0-9a-fA-F-]{36})$");

    private static final Pattern DOCUMENT_STATUS_JSON =
            Pattern.compile("\"document\":\\{[^}]*?\"status\":\"([a-z_]+)\"");

    private static final Pattern VERSION_JSON =
            Pattern.compile("\"version_major\":(\\d+),\"version_minor\":(\\d+),\"version_patch\":(\\d+)");

    private BrowserManager() {
    }

    private static String configuredUrl(String property, String envVar, String fallback) {
        String value = System.getProperty(property);

        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }

        if (value == null || value.isBlank()) {
            value = fallback;
        }

        return value.trim().replaceAll("/+$", "");
    }

    /*
     * FAIL-FAST ORTAM KONTROLÜ (koşu başında bir kez, @BeforeAll).
     *
     * Yanlış porta (ör. Test Platform) karşı koşulduğunda her senaryo
     * ~6 sn timeout ile düşüyor ve 43 senaryo 257 sn harcıyordu. Burada:
     *   1. UI adresinin döndürdüğü HTML <title> beklenen ECM başlığı mı?
     *      (-Decm.ui.expectedTitle, varsayılan "doc_sdk" = ECM frontend
     *      index.html <title>)
     *   2. ECM API'si ayakta mı? GET /v1/auth/me token'sız 401 (veya 200)
     *      dönmeli.
     * Sonuç cache'lenir: kontrol başarısızsa sonraki her senaryo aynı
     * mesajla anında düşer (tarayıcı hiç açılmaz).
     * Kapatmak için (önerilmez): -Decm.envGuard=false
     */
    private static volatile String environmentError;
    private static volatile boolean environmentVerified;

    public static synchronized void verifyEnvironment() {

        if (environmentVerified) {
            return;
        }

        if (environmentError != null) {
            throw new IllegalStateException(environmentError);
        }

        if (!Boolean.parseBoolean(System.getProperty("ecm.envGuard", "true"))) {
            environmentVerified = true;
            return;
        }

        String expectedTitle = System.getProperty("ecm.ui.expectedTitle", "doc_sdk");
        String ui = getBaseUrl();
        String api = getApiBaseUrl();

        // HTTP/1.1: varsayılan HTTP/2 "Upgrade: h2c" isteğini Vite dev server
        // HMR websocket upgrade'i sanıp yanıtlamıyor (istek asılı kalıyor).
        java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();

        String problem = null;

        try {
            java.net.http.HttpResponse<String> r = getAnyAddress(http, ui + "/");

            Matcher m = Pattern.compile("(?is)<title>(.*?)</title>").matcher(r.body());
            String title = m.find() ? m.group(1).trim() : "(title yok)";

            if (r.statusCode() != 200 || !title.equals(expectedTitle)) {
                problem = "ECM UI adresi ECM'i sunmuyor: " + ui
                        + " -> HTTP " + r.statusCode() + ", <title>" + title + "</title>"
                        + " (beklenen <title>" + expectedTitle + "</title>)."
                        + " Doğru adresi ECM_BASE_URL ortam değişkeni veya -Decm.baseUrl"
                        + " ile verin (ör. ECM_BASE_URL=http://localhost:5173).";
            }
        } catch (Exception e) {
            problem = "ECM UI adresine ulaşılamıyor: " + ui + " (" + e + ")."
                    + " ECM_BASE_URL / -Decm.baseUrl değerini kontrol edin.";
        }

        if (problem == null) {
            try {
                java.net.http.HttpResponse<String> r = getAnyAddress(http, api + "/v1/auth/me");

                if (r.statusCode() != 401 && r.statusCode() != 200) {
                    problem = "ECM API beklenen yanıtı vermiyor: GET " + api
                            + "/v1/auth/me -> HTTP " + r.statusCode()
                            + " (token'sız 401 bekleniyordu). ECM_API_URL / -Decm.apiUrl"
                            + " değerini kontrol edin.";
                }
            } catch (Exception e) {
                problem = "ECM API adresine ulaşılamıyor: " + api + " (" + e + ")."
                        + " ECM_API_URL / -Decm.apiUrl değerini kontrol edin.";
            }
        }

        if (problem != null) {
            environmentError = "[ENV GUARD] " + problem;
            System.out.println(environmentError);
            throw new IllegalStateException(environmentError);
        }

        environmentVerified = true;
        System.out.println("[ENV GUARD] OK: UI " + ui + " <title>" + expectedTitle
                + "</title>, API " + api);
    }

    /*
     * java.net.http.HttpClient yalnızca ilk çözülen adrese bağlanır; tarayıcı
     * gibi IPv6/IPv4 arasında geri düşmez. Bu makinede Vite UI yalnızca
     * [::1]'de, API yalnızca 127.0.0.1'de dinliyor. Bu yüzden host'un TÜM
     * adreslerini sırayla deniyoruz; hiçbiri bağlanamazsa son hata fırlar.
     */
    private static java.net.http.HttpResponse<String> getAnyAddress(
            java.net.http.HttpClient http,
            String url
    ) throws Exception {

        URI uri = URI.create(url);
        Exception last = null;

        for (java.net.InetAddress address : java.net.InetAddress.getAllByName(uri.getHost())) {

            String host = address instanceof java.net.Inet6Address
                    ? "[" + address.getHostAddress().replaceAll("%.*$", "") + "]"
                    : address.getHostAddress();

            URI target = new URI(uri.getScheme(), null, host.replace("[", "").replace("]", ""),
                    uri.getPort(), uri.getPath(), uri.getQuery(), null);

            try {
                return http.send(
                        java.net.http.HttpRequest.newBuilder(target)
                                .timeout(java.time.Duration.ofSeconds(10)).GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
            } catch (java.net.ConnectException e) {
                last = e;
            }
        }

        throw last != null ? last : new java.net.ConnectException("Adres çözülemedi: " + uri.getHost());
    }

    public static String getBaseUrl() {
        return configuredUrl("ecm.baseUrl", "ECM_BASE_URL", DEFAULT_BASE_URL);
    }

    public static String getAppApiBaseUrl() {
        return configuredUrl("ecm.appApiUrl", "ECM_APP_API_URL", DEFAULT_APP_API_URL);
    }

    public static String getApiBaseUrl() {
        return configuredUrl("ecm.apiUrl", "ECM_API_URL", DEFAULT_API_URL);
    }

    /*
     * ÖN KOŞUL (precondition) oturumu: login UI'ını test etmeyen senaryolarda
     * kullanılır. Login ekranı ve login sonrası açılan ağır /folders sayfası
     * yerine verilen (yeni ve boş) BrowserContext'e yalnızca refresh_token
     * cookie'si eklenir; sayfa ilk açıldığında uygulama hard reload'daki gibi
     * (client.ts) bu cookie ile access token alır. Login UI'ı login.feature
     * senaryolarında test edilmeye devam eder.
     *
     * Rol başına oturum havuzu (SessionPool):
     *   ECM refresh token'ları tek kullanımlıktır: POST /auth/refresh sunulan
     *   token'ı revoke eder ve yenisini verir (auth.rs refresh()). Bu yüzden
     *   bir storageState'i birden fazla context'e kopyalamak güvenli DEĞİLDİR
     *   (ikinci context 401 alır). Bunun yerine:
     *     1. Rol (email) başına koşuda bir kez POST /auth/login (~250 ms,
     *        parola hash'i; ECM varsayılanı login için 10 istek/dk limitli).
     *     2. Her senaryoda havuzdaki token POST /auth/refresh ile (~15 ms)
     *        yeni bir token'a çevrilir; yalnızca bu yeni token, senaryonun
     *        yeni context'ine cookie olarak eklenir. Başka hiçbir durum
     *        (localStorage, sessionStorage, diğer cookie'ler) taşınmaz.
     *     3. Senaryo sonunda context kapanmadan önce, uygulamanın rotate
     *        ettiği güncel refresh_token context'ten alınıp havuza geri
     *        konur (releaseSession).
     *   Token revoke edilmişse (ör. logout, parola değişimi) refresh 401
     *   döner ve gerçek login'e düşülür; senaryo asla ölü bir oturumla
     *   başlamaz. Havuz rol başına bir kuyruk olduğu için paralel worker'lar
     *   aynı token'ı asla paylaşmaz.
     *
     * Dönüş: API setup çağrıları için bu oturumun access token'ı.
     */
    public static String apiLogin(
            BrowserContext targetContext,
            String email,
            String password
    ) {
        AuthResult auth = Profiler.span(
                "api", "session for " + email,
                () -> SessionPool.acquire(email, password)
        );

        recordApiEndpoint("POST", auth.endpoint());

        targetContext.addCookies(java.util.List.of(
                new com.microsoft.playwright.options.Cookie("refresh_token", auth.refreshToken())
                        .setDomain(appApiHost())
                        .setPath("/")
                        .setExpires(auth.expiresEpochSeconds())
                        .setHttpOnly(true)
                        .setSecure(true)
                        .setSameSite(com.microsoft.playwright.options.SameSiteAttribute.STRICT)
        ));

        synchronized (contextRoles) {
            contextRoles.put(targetContext, email);
        }

        return auth.accessToken();
    }

    private static String appApiHost() {
        return java.net.URI.create(getAppApiBaseUrl()).getHost();
    }

    // Hangi context hangi rolün oturumunu taşıyor (senaryo sonunda token'ı
    // doğru rolün havuzuna geri koymak için).
    private static final java.util.Map<BrowserContext, String> contextRoles =
            new java.util.IdentityHashMap<>();

    /*
     * Context kapanmadan önce çağrılır: uygulamanın elindeki güncel
     * refresh_token'ı rolün havuzuna iade eder.
     */
    private static void releaseSession(BrowserContext target) {
        String email;
        synchronized (contextRoles) {
            email = contextRoles.remove(target);
        }
        if (email == null) {
            return;
        }
        try {
            String host = appApiHost();
            target.cookies().stream()
                    .filter(c -> "refresh_token".equals(c.name) && host.equals(c.domain))
                    .findFirst()
                    .ifPresent(c -> SessionPool.release(email, c.value));
        } catch (Exception ignored) {
            // Havuza iade edilemezse sonraki senaryo login'e düşer.
        }
    }

    record AuthResult(String accessToken, String refreshToken, double expiresEpochSeconds, String endpoint) {
    }

    static final class SessionPool {

        private static final java.net.http.HttpClient HTTP =
                java.net.http.HttpClient.newBuilder()
                        .version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();

        private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentLinkedDeque<String>> POOL =
                new java.util.concurrent.ConcurrentHashMap<>();

        private SessionPool() {
        }

        static AuthResult acquire(String email, String password) {
            java.util.Deque<String> tokens =
                    POOL.computeIfAbsent(email, k -> new java.util.concurrent.ConcurrentLinkedDeque<>());

            String pooled;
            while ((pooled = tokens.pollFirst()) != null) {
                java.net.http.HttpResponse<String> r =
                        send("/v1/auth/refresh", null, "refresh_token=" + pooled);
                if (r.statusCode() == 200) {
                    return toResult(r, email, "/v1/auth/refresh");
                }
                // 401: token revoke edilmiş/expire olmuş -> sıradakini dene.
            }

            java.net.http.HttpResponse<String> r = send(
                    "/v1/auth/login",
                    new com.google.gson.Gson().toJson(java.util.Map.of("email", email, "password", password)),
                    null
            );
            if (r.statusCode() != 200) {
                throw new AssertionError(
                        "API login başarısız (" + email + "): HTTP "
                                + r.statusCode() + " " + r.body()
                );
            }
            return toResult(r, email, "/v1/auth/login");
        }

        static void release(String email, String refreshToken) {
            if (refreshToken != null && !refreshToken.isBlank()) {
                POOL.computeIfAbsent(email, k -> new java.util.concurrent.ConcurrentLinkedDeque<>())
                        .addLast(refreshToken);
            }
        }

        private static java.net.http.HttpResponse<String> send(String path, String json, String cookie) {
            java.net.http.HttpRequest.Builder b = java.net.http.HttpRequest.newBuilder(
                            java.net.URI.create(getApiBaseUrl() + path))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(json == null
                            ? java.net.http.HttpRequest.BodyPublishers.noBody()
                            : java.net.http.HttpRequest.BodyPublishers.ofString(json));
            if (cookie != null) {
                b.header("Cookie", cookie);
            }
            try {
                return HTTP.send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException("Auth isteği başarısız: POST " + path, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        /*
         * refresh_token'ı Set-Cookie başlığından okuruz: yanıt düz http
         * 127.0.0.1'den geldiği için "Secure" cookie hiçbir cookie jar'a
         * otomatik yazılmaz. Backend'in verdiği Max-Age korunur.
         */
        private static AuthResult toResult(java.net.http.HttpResponse<String> r, String email, String endpoint) {
            String setCookie = r.headers().allValues("set-cookie").stream()
                    .filter(v -> v.startsWith("refresh_token="))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "POST " + endpoint + " yanıtında refresh_token Set-Cookie başlığı yok (" + email + ")"
                    ));

            String value = setCookie.substring(
                    "refresh_token=".length(),
                    setCookie.contains(";") ? setCookie.indexOf(';') : setCookie.length()
            );

            Matcher maxAge = Pattern.compile("(?i)max-age=(\\d+)").matcher(setCookie);
            double expires = maxAge.find()
                    ? System.currentTimeMillis() / 1000.0 + Long.parseLong(maxAge.group(1))
                    : -1;

            String token = com.google.gson.JsonParser.parseString(r.body())
                    .getAsJsonObject()
                    .get("token")
                    .getAsString();

            return new AuthResult(token, value, expires, endpoint);
        }
    }

    /*
     * Ön koşul verisi için authenticated JSON POST. UI davranışı test
     * edilmeyen setup adımlarında kullanılır (ör. Move/Copy hedef klasörü).
     */
    public static String apiPostJson(
            BrowserContext targetContext,
            String accessToken,
            String path,
            java.util.Map<String, Object> body
    ) {
        com.microsoft.playwright.APIResponse response = Profiler.span(
                "api", "POST " + path,
                () -> targetContext.request().post(
                        getApiBaseUrl() + path,
                        com.microsoft.playwright.options.RequestOptions.create()
                                .setHeader("Authorization", "Bearer " + accessToken)
                                .setData(body)
                ));

        recordApiEndpoint("POST", path);

        if (!response.ok()) {
            throw new AssertionError(
                    "API setup çağrısı başarısız: POST " + path + " -> HTTP "
                            + response.status() + " " + response.text()
            );
        }

        return response.text();
    }

    public static final String ROOT_FOLDER_ID = "00000000-0000-0000-0000-000000000001";

    /*
     * ÖN KOŞUL belgesi: Upload UI'ını test etmeyen senaryolar için belgeyi
     * UI'ın gönderdiği payload'ın aynısıyla (DocumentNewUnclassified.tsx,
     * < 2 MB dosya -> POST /documents/unclassified, content = byte dizisi)
     * API üzerinden oluşturur. Upload UI'ı "Upload without class sayfasındadır"
     * ve "benzersiz bir TXT belge yüklemiştir" adımlarıyla test edilmeye
     * devam eder. Dönüş: yeni belgenin id'si.
     */
    public static String apiCreateUnclassifiedDocument(
            BrowserContext targetContext,
            String accessToken,
            String title,
            java.nio.file.Path file
    ) {
        byte[] bytes;
        try {
            bytes = java.nio.file.Files.readAllBytes(file);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }

        java.util.List<Integer> content = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            content.add(b & 0xff);
        }

        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("title", title);
        body.put("metadata", java.util.Map.of());
        body.put("mime_type", "text/plain");
        body.put("original_filename", file.getFileName().toString());
        body.put("folder_id", ROOT_FOLDER_ID);
        body.put("content", content);

        String response = apiPostJson(targetContext, accessToken, "/v1/documents/unclassified", body);

        com.google.gson.JsonObject json =
                com.google.gson.JsonParser.parseString(response).getAsJsonObject();

        if (!json.has("document_id")) {
            throw new AssertionError(
                    "API ile belge oluşturuldu ancak document_id dönmedi: " + response
            );
        }

        return json.get("document_id").getAsString();
    }

    /*
     * ÖN KOŞUL klasörü: klasör oluşturma UI'ını test etmeyen senaryolar için
     * UI'ın gönderdiği payload'ın aynısıyla (Folders.tsx / FolderDetail.tsx ->
     * api/folders.ts createFolder) API üzerinden oluşturur.
     * Dönüş: yeni klasörün id'si.
     */
    public static String apiCreateFolder(
            BrowserContext targetContext,
            String accessToken,
            String name,
            String parentId
    ) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("parent_id", parentId == null ? ROOT_FOLDER_ID : parentId);
        body.put("inherit_permissions", true);

        String response = apiPostJson(targetContext, accessToken, "/v1/folders", body);

        com.google.gson.JsonObject json =
                com.google.gson.JsonParser.parseString(response).getAsJsonObject();

        if (!json.has("id") || !name.equals(json.get("name").getAsString())) {
            throw new AssertionError(
                    "API ile klasör oluşturuldu ancak beklenen id/name dönmedi: " + response
            );
        }

        return json.get("id").getAsString();
    }

    /*
     * ÖN KOŞUL durumu: klasörü aktif/pasif yapar (api/folders.ts
     * setFolderActive -> PUT /folders/{id}/active {is_active}).
     */
    public static void apiSetFolderActive(
            BrowserContext targetContext,
            String accessToken,
            String folderId,
            boolean active
    ) {
        String path = "/v1/folders/" + folderId + "/active";

        com.microsoft.playwright.APIResponse response = Profiler.span(
                "api", "PUT /v1/folders/{id}/active",
                () -> targetContext.request().put(
                        getApiBaseUrl() + path,
                        com.microsoft.playwright.options.RequestOptions.create()
                                .setHeader("Authorization", "Bearer " + accessToken)
                                .setData(java.util.Map.of("is_active", active))
                ));

        recordApiEndpoint("PUT", path);

        if (!response.ok()) {
            throw new AssertionError(
                    "API setup çağrısı başarısız: PUT " + path + " -> HTTP "
                            + response.status() + " " + response.text()
            );
        }
    }

    // utils.EcmApi (çok kullanıcılı API testleri) çağrılarının da runtime
    // endpoint coverage'a girmesi için dış erişim noktası.
    public static void recordApiCall(String method, String path) {
        recordApiEndpoint(method, path);
    }

    // API ile yapılan setup çağrıları da runtime endpoint coverage'a girsin.
    private static void recordApiEndpoint(String method, String path) {
        try {
            String endpoint = method + " "
                    + EndpointTemplateMatcher.match(method, path);
            scenarioEndpoints.add(endpoint);
            System.out.println("[ENDPOINT] " + endpoint);
        } catch (Exception ignored) {
            // Coverage collection must never fail the test.
        }
    }

    /*
     * Senaryonun ana context'inde API ile login olur ve access token'ı
     * setup çağrıları için saklar. Sayfa açılmaz; çağıran adım gideceği
     * sayfaya doğrudan navigate eder.
     */
    public static void apiLoginMainContext(String email, String password) {
        accessToken = apiLogin(context, email, password);
    }

    public static String getAccessToken() {
        if (accessToken == null) {
            throw new IllegalStateException(
                    "Access token yok: önce apiLoginMainContext çağrılmalı."
            );
        }
        return accessToken;
    }

    public static boolean isCi() {
        String ci = System.getenv("CI");
        return ci != null && ci.equalsIgnoreCase("true");
    }

    public static void startScenario(
            String scenarioName,
            String scenarioUri,
            String scenarioId
    ) {
        RuntimeEndpointCoverage.initializeRun();

        currentScenarioName = scenarioName;
        currentScenarioUri = scenarioUri;
        currentScenarioId = scenarioId;

        scenarioEndpoints.clear();

        System.out.println(
                "[SCENARIO] " + currentScenarioName
        );

        System.out.println(
                "[SCENARIO URI] " + currentScenarioUri
        );

        System.out.println(
                "[SCENARIO ID] " + currentScenarioId
        );
    }

    public static void finishScenario() {
        if (currentScenarioName == null) {
            return;
        }

        System.out.println(
                "[COVERAGE] Scenario: " + currentScenarioName
        );

        for (String endpoint : scenarioEndpoints) {
            System.out.println(
                    "[COVERAGE] " + endpoint
            );

            RuntimeEndpointCoverage.record(
                    currentScenarioId,
                    currentScenarioUri,
                    currentScenarioName,
                    endpoint
            );
        }

        currentScenarioName = null;
        currentScenarioUri = null;
        currentScenarioId = null;

        scenarioEndpoints.clear();

        synchronized (documentStatusEvidence) {
            documentStatusEvidence.clear();
        }
    }

    public static String getDocumentStatusEvidence() {
        synchronized (documentStatusEvidence) {
            return documentStatusEvidence.isEmpty()
                    ? "(API status kanıtı yok)"
                    : String.join(" -> ", documentStatusEvidence);
        }
    }

    private static void recordStatusEvidence(String entry) {
        synchronized (documentStatusEvidence) {
            if (documentStatusEvidence.isEmpty()
                    || !documentStatusEvidence.get(documentStatusEvidence.size() - 1).equals(entry)) {
                documentStatusEvidence.add(entry);
            }
        }
    }

    private static synchronized void ensureBrowser() {

        if (browser != null && browser.isConnected()) {
            return;
        }

        Profiler.span("browser", "Playwright.create + chromium.launch", () -> {
            playwright = Playwright.create();

            browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setSlowMo(0)
                            .setArgs(browserArgs())
            );
        });

    }

    /*
     * ECM backend yalnızca IPv4'te (127.0.0.1:8080) dinliyor; uygulama ise
     * API'yi "localhost:8080" üzerinden çağırıyor. Windows'ta localhost önce
     * ::1'e çözülüyor ve reddedilen IPv6 bağlantısı ~0.2-2 sn bekledikten
     * sonra IPv4'e düşüyor. Her senaryo yeni bir context (yeni bağlantı
     * havuzu) açtığı için bu gecikme her senaryoda ilk API çağrısına
     * ekleniyordu (ölçüm: detay sayfası 1.7 sn -> 0.6 sn).
     *
     * Kural yalnızca API host:port'unun DNS çözümlemesini değiştirir;
     * origin, Host başlığı, cookie domain'i ve CORS aynı kalır (hepsi hâlâ
     * "localhost"). UI (::1:5173) etkilenmez. Kapatmak için:
     * -Decm.browser.ipv4Api=false
     */
    private static List<String> browserArgs() {
        List<String> args = new ArrayList<>();

        URI appApi = URI.create(getAppApiBaseUrl());
        boolean enabled = Boolean.parseBoolean(
                System.getProperty("ecm.browser.ipv4Api", "true"));

        if (enabled && "localhost".equalsIgnoreCase(appApi.getHost()) && appApi.getPort() > 0) {
            args.add("--host-resolver-rules=MAP localhost:" + appApi.getPort()
                    + " 127.0.0.1:" + appApi.getPort());
        }

        return args;
    }

    public static void startBrowser() {

        verifyEnvironment();

        ensureBrowser();

        // Aynı senaryoda tekrar çağrılırsa önceki context'i kapatıp temiz
        // bir context açıyoruz (eskiden yeni bir Playwright process'i açılıyordu).
        closeContext();

        context = Profiler.span("browser", "newContext (main)", () -> browser.newContext());
        startTracing(context, "main");

        page = Profiler.span("browser", "newPage (main)", () -> context.newPage());

        attachEndpointListener(page);
    }

    private static int traceCounter = 0;

    // Opt-in profiling: senaryo başına hafif trace (snapshot/screenshot yok);
    // her Playwright çağrısının süresini kanıt olarak verir.
    private static void startTracing(BrowserContext target, String role) {
        if (!Profiler.ENABLED) {
            return;
        }
        target.tracing().start(
                new com.microsoft.playwright.Tracing.StartOptions()
                        .setScreenshots(false)
                        .setSnapshots(false)
                        .setSources(false)
                        .setTitle(Profiler.scenario() + " [" + role + "]")
        );
    }

    private static void stopTracing(BrowserContext target, String role) {
        if (!Profiler.ENABLED || target == null) {
            return;
        }
        try {
            traceCounter++;
            target.tracing().stop(
                    new com.microsoft.playwright.Tracing.StopOptions()
                            .setPath(Profiler.DIR.resolve("traces").resolve(
                                    String.format("%03d-%s.zip", traceCounter, role)))
            );
        } catch (Exception ignored) {
            // Profiling must never fail the test.
        }
    }

    public static Page createManagerPage() {

        managerContext = Profiler.span("browser", "newContext (manager)", () -> browser.newContext());
        startTracing(managerContext, "manager");

        managerPage = Profiler.span("browser", "newPage (manager)", () -> managerContext.newPage());

        attachEndpointListener(managerPage);

        return managerPage;
    }

    private static void attachEndpointListener(Page targetPage) {

        targetPage.onRequest(request -> {
            try {
                URI uri = URI.create(request.url());

                String path = uri.getPath();

                if (path == null || !path.startsWith("/v1/")) {
                    return;
                }

                String normalizedPath =
                        EndpointTemplateMatcher.match(
                                request.method(),
                                path
                        );

                String endpoint =
                        request.method().toUpperCase()
                                + " "
                                + normalizedPath;

                scenarioEndpoints.add(endpoint);

                System.out.println(
                        "[ENDPOINT] " + endpoint
                );

            } catch (Exception ignored) {
                // Coverage collection must never fail the test.
            }
        });

        targetPage.onResponse(response -> {
            try {
                String path = URI.create(response.url()).getPath();
                String method = response.request().method();

                if (path == null || !path.startsWith("/v1/documents")) {
                    return;
                }

                if ("GET".equals(method)
                        && DOCUMENT_DETAIL_PATH.matcher(path).matches()
                        && response.ok()) {

                    Matcher m = DOCUMENT_STATUS_JSON.matcher(response.text());
                    if (m.find()) {
                        recordStatusEvidence("GET status=" + m.group(1));
                    }

                } else if ("PATCH".equals(method) && path.endsWith("/status")) {

                    recordStatusEvidence(
                            "PATCH status " + response.request().postData()
                                    + " -> " + response.status()
                    );

                } else if ("POST".equals(method)
                        && path.startsWith("/v1/documents/unclassified")
                        && String.valueOf(response.request().postData())
                                .matches("(?s).*\"document_id\":\"[0-9a-fA-F-]{36}\".*")) {

                    Matcher v = VERSION_JSON.matcher(response.text());
                    recordStatusEvidence(
                            "POST new version -> " + response.status()
                                    + (v.find() ? " v" + v.group(1) + "." + v.group(2) + "." + v.group(3) : "")
                    );
                }
            } catch (Exception ignored) {
                // Evidence collection must never fail the test.
            }
        });
    }

    /*
     * Senaryo sonunda çağrılır: senaryoya ait tüm context'leri (ve dolayısıyla
     * cookie/storage/oturum durumunu) kapatır. Browser process'i bir sonraki
     * senaryo için açık kalır.
     */
    public static void closeBrowser() {

        try {
            if (managerContext != null) {
                releaseSession(managerContext);
                stopTracing(managerContext, "manager");
                Profiler.span("browser", "context.close (manager)", () -> managerContext.close());
            }
        } catch (Exception ignored) {
        } finally {
            managerContext = null;
            managerPage = null;
        }

        closeContext();
    }

    private static void closeContext() {

        try {
            if (context != null) {
                releaseSession(context);
                stopTracing(context, "main");
                Profiler.span("browser", "context.close (main)", () -> context.close());
            }
        } catch (Exception ignored) {
        } finally {
            context = null;
            page = null;
            accessToken = null;
        }
    }

    public static synchronized void shutdown() {

        closeBrowser();

        try {
            if (browser != null && browser.isConnected()) {
                browser.close();
            }
        } catch (Exception ignored) {
        } finally {
            browser = null;
        }

        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        } finally {
            playwright = null;
        }
    }
}
