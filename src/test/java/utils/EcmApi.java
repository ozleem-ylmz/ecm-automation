package utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Çok kullanıcılı API test istemcisi (batch19: yetki / ACL / yaşam döngüsü).
 *
 * BrowserManager.apiPostJson yalnızca başarılı setup çağrıları içindir (2xx
 * değilse düşer). Buradaki çağrılar ise NEGATİF sonuçları da (403/404/409/422)
 * kanıt olarak döndürür; beklenen durumu adım tanımı doğrular.
 *
 *  - Her kullanıcının kendi access token'ı ile çağrı yapılır (tarayıcı yok).
 *  - Çağrılar runtime endpoint coverage'a kaydedilir (BrowserManager.recordApiCall).
 *  - Her senaryonun istek/yanıt özeti tutulur; başarısız senaryoda Allure'a
 *    kanıt olarak eklenir (AccessSteps @After).
 *  - HTTP/1.1: SessionPool ile aynı gerekçe (Vite/axum h2c upgrade sorunu).
 */
public final class EcmApi {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Gson GSON = new Gson();

    private static final List<String> EXCHANGES = Collections.synchronizedList(new ArrayList<>());

    private EcmApi() {
    }

    /** Bir API çağrısının sonucu: HTTP durum kodu + (varsa) JSON gövde. */
    public record Response(int status, String body, String method, String path) {

        public JsonElement json() {
            if (body == null || body.isBlank()) {
                return JsonNull.INSTANCE;
            }
            try {
                return JsonParser.parseString(body);
            } catch (RuntimeException e) {
                return JsonNull.INSTANCE;
            }
        }

        public JsonObject obj() {
            JsonElement e = json();
            if (!e.isJsonObject()) {
                throw new AssertionError("JSON nesne bekleniyordu: " + describe());
            }
            return e.getAsJsonObject();
        }

        public String str(String field) {
            JsonObject o = obj();
            if (!o.has(field) || o.get(field).isJsonNull()) {
                throw new AssertionError("Yanıtta '" + field + "' yok: " + describe());
            }
            return o.get(field).getAsString();
        }

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public String errorCode() {
            JsonElement e = json();
            if (e.isJsonObject() && e.getAsJsonObject().has("error")) {
                JsonElement err = e.getAsJsonObject().get("error");
                if (err.isJsonObject() && err.getAsJsonObject().has("code")) {
                    return err.getAsJsonObject().get("code").getAsString();
                }
            }
            return null;
        }

        public String describe() {
            String b = body == null ? "" : body.replaceAll("\\s+", " ");
            return method + " " + path + " -> HTTP " + status + " " + (b.length() > 400 ? b.substring(0, 400) + "…" : b);
        }

        /** Başarılı olması gereken setup çağrıları için. */
        public Response expectOk() {
            if (!ok()) {
                throw new AssertionError("API setup çağrısı başarısız: " + describe());
            }
            return this;
        }
    }

    // ---------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------

    public static Response get(String token, String path) {
        return send(token, "GET", path, null);
    }

    public static Response post(String token, String path, Object body) {
        return send(token, "POST", path, body);
    }

    public static Response put(String token, String path, Object body) {
        return send(token, "PUT", path, body);
    }

    public static Response patch(String token, String path, Object body) {
        return send(token, "PATCH", path, body);
    }

    public static Response delete(String token, String path) {
        return send(token, "DELETE", path, null);
    }

    public static Response send(String token, String method, String path, Object body) {
        return send(token, method, path, body, Duration.ofSeconds(30));
    }

    /* STR-10: klasör seçici/ağaç çağrıları BUG-03 nedeniyle yavaş; sızıntı testi zaman aşımıyla maskelenmesin. */
    public static Response getSlow(String token, String path) {
        return send(token, "GET", path, null, Duration.ofSeconds(120));
    }

    private static Response send(String token, String method, String path, Object body, Duration timeout) {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(BrowserManager.getApiBaseUrl() + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .method(method, publisher);
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> r;
        try {
            r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(method + " " + path + " gönderilemedi", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(method + " " + path + " kesildi", e);
        }

        String pathOnly = path.contains("?") ? path.substring(0, path.indexOf('?')) : path;
        BrowserManager.recordApiCall(method, pathOnly);

        Response response = new Response(r.statusCode(), r.body(), method, path);
        EXCHANGES.add(response.describe());
        return response;
    }

    // ---------------------------------------------------------------------
    // Senaryo kanıtı
    // ---------------------------------------------------------------------

    public static void resetExchanges() {
        EXCHANGES.clear();
    }

    public static String exchangeLog() {
        synchronized (EXCHANGES) {
            return String.join(System.lineSeparator(), EXCHANGES);
        }
    }

    // ---------------------------------------------------------------------
    // Oturum ve setup yardımcıları
    // ---------------------------------------------------------------------

    /** POST /v1/auth/login -> access token (başarısızsa düşer). */
    public static String login(String email, String password) {
        Response r = post(null, "/v1/auth/login", Map.of("email", email, "password", password));
        return r.expectOk().str("token");
    }

    /** POST /v1/auth/login yanıtının kendisi (negatif senaryolar için). */
    public static Response tryLogin(String email, String password) {
        return post(null, "/v1/auth/login", Map.of("email", email, "password", password));
    }

    /** Admin olarak test kullanıcısı oluşturur; dönüş: kullanıcı id'si. */
    public static String createUser(String adminToken, String email, String password, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("full_name", "QA B19 " + role);
        body.put("role", role);
        return post(adminToken, "/v1/users", body).expectOk().str("id");
    }

    /** Admin olarak klasör oluşturur; dönüş: klasör id'si. */
    public static String createFolder(String token, String name, String parentId, boolean inheritPermissions) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("parent_id", parentId == null ? BrowserManager.ROOT_FOLDER_ID : parentId);
        body.put("inherit_permissions", inheritPermissions);
        return post(token, "/v1/folders", body).expectOk().str("id");
    }

    /**
     * UI'ın gönderdiği payload ile (POST /documents/unclassified, içerik bayt
     * dizisi) sınıfsız TXT belge oluşturur. Başarısız olursa yanıtı döndürür.
     */
    public static Response tryCreateDocument(String token, String title, String folderId, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        List<Integer> content = new ArrayList<>(bytes.length);
        for (byte x : bytes) {
            content.add(x & 0xff);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("metadata", Map.of());
        body.put("mime_type", "text/plain");
        body.put("original_filename", "b19.txt");
        body.put("folder_id", folderId == null ? BrowserManager.ROOT_FOLDER_ID : folderId);
        body.put("content", content);
        return post(token, "/v1/documents/unclassified", body);
    }

    public static String createDocument(String token, String title, String folderId, String text) {
        return tryCreateDocument(token, title, folderId, text).expectOk().str("document_id");
    }

    /** ACL satırı ekler (folders|documents|classes). Dönüş: yanıt. */
    public static Response grant(String token, String scope, String resourceId, String roleId,
                                 String userId, String groupId, boolean deny) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", roleId);
        if (userId != null) {
            body.put("user_id", userId);
        }
        if (groupId != null) {
            body.put("group_id", groupId);
        }
        body.put("deny", deny);
        return post(token, "/v1/" + scope + "/" + resourceId + "/acl", body);
    }
}
