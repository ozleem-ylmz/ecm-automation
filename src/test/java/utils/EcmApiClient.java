package utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Negatif senaryolar için ham HTTP istemcisi: BrowserManager.apiPostJson 2xx
 * dışındaki her yanıtta AssertionError fırlatır; yetki/doğrulama testleri ise
 * 403/409/422 gibi durum kodlarını doğrulamak zorundadır.
 *
 * refresh_token cookie'si Secure işaretli olduğundan (auth.rs refresh_cookie)
 * http://127.0.0.1 üzerinde otomatik gönderilmez; BrowserManager.SessionPool
 * ile aynı şekilde Cookie başlığı elle taşınır.
 *
 * Her çağrı RuntimeEndpointCoverage'a yazılır; böylece bu testler de runtime
 * endpoint coverage raporunda görünür (BrowserManager'a dokunmadan).
 */
public final class EcmApiClient {

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    private static final Pattern REFRESH_COOKIE =
            Pattern.compile("refresh_token=([^;]*)");

    private EcmApiClient() {
    }

    public static final class Session {

        public final String email;
        public final String userId;
        public final String role;

        private String accessToken;
        private String refreshToken;

        private Session(String email, String userId, String role, String accessToken, String refreshToken) {
            this.email = email;
            this.userId = userId;
            this.role = role;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String accessToken() {
            return accessToken;
        }

        public String refreshToken() {
            return refreshToken;
        }
    }

    public record ApiResponse(int status, String body, String setCookie) {

        public JsonObject json() {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                throw new AssertionError("JSON nesnesi bekleniyordu: " + body);
            }
            return element.getAsJsonObject();
        }

        public String errorMessage() {
            try {
                JsonObject error = json().getAsJsonObject("error");
                return error == null ? "" : error.get("message").getAsString();
            } catch (RuntimeException e) {
                return "";
            }
        }

        public String errorCode() {
            try {
                JsonObject error = json().getAsJsonObject("error");
                return error == null ? "" : error.get("code").getAsString();
            } catch (RuntimeException e) {
                return "";
            }
        }

        public ApiResponse expect(int expectedStatus, String context) {
            if (status != expectedStatus) {
                throw new AssertionError(
                        context + ": HTTP " + expectedStatus + " bekleniyordu, "
                                + status + " geldi. Yanıt: " + redact(body)
                );
            }
            return this;
        }
    }

    private static final Pattern TOKEN_FIELD =
            Pattern.compile("\"(token|refresh_token|access_token)\"\\s*:\\s*\"[^\"]*\"");

    /* Hata mesajları ve Allure'a asla token değeri yazılmaz. */
    public static String redact(String text) {
        if (text == null) {
            return null;
        }
        return TOKEN_FIELD.matcher(text).replaceAll("\"$1\":\"<redacted>\"");
    }

    /* Verilen refresh token ile POST /auth/refresh; hiçbir session güncellenmez. */
    public static ApiResponse refreshWith(String refreshToken) {
        return send("POST", "/v1/auth/refresh", null, "refresh_token=" + refreshToken, null);
    }

    /* POST /auth/logout: yalnızca sunulan refresh cookie'si iptal edilir (auth.rs logout). */
    public static ApiResponse logoutWith(String refreshToken) {
        return send("POST", "/v1/auth/logout", null, "refresh_token=" + refreshToken, null);
    }

    public static ApiResponse logout(Session session) {
        return logoutWith(session.refreshToken);
    }

    /* Kimlik bilgisi taşımayan istekler (register, oturumsuz erişim denemeleri). */
    public static ApiResponse anonymous(String method, String path, Object body) {
        return send(method, path, null, null, body == null ? null : GSON.toJson(body));
    }

    /* Access token'ı bilinen oturum; register yanıtından kurulur. */
    public static Session sessionFrom(String email, ApiResponse loginLike) {
        JsonObject json = loginLike.json();
        JsonObject user = json.getAsJsonObject("user");
        return new Session(
                email,
                user.get("id").getAsString(),
                user.get("role").getAsString(),
                json.get("token").getAsString(),
                refreshTokenFrom(loginLike.setCookie()).orElse(null)
        );
    }

    public static Optional<String> refreshCookieValue(ApiResponse response) {
        return refreshTokenFrom(response.setCookie());
    }

    /* auth.rs clear_refresh_cookie: "refresh_token=; ... Max-Age=0" */
    public static boolean clearsRefreshCookie(ApiResponse response) {
        String c = response.setCookie();
        return c != null && c.startsWith("refresh_token=;") && c.contains("Max-Age=0");
    }

    public static ApiResponse rawLogin(String email, String password) {
        return send("POST", "/v1/auth/login", null, null,
                GSON.toJson(java.util.Map.of("email", email, "password", password)));
    }

    public static Session login(String email, String password) {
        ApiResponse r = rawLogin(email, password).expect(200, "Login " + email);
        JsonObject json = r.json();
        JsonObject user = json.getAsJsonObject("user");
        return new Session(
                email,
                user.get("id").getAsString(),
                user.get("role").getAsString(),
                json.get("token").getAsString(),
                refreshTokenFrom(r.setCookie()).orElseThrow(
                        () -> new AssertionError("Login yanıtında refresh_token cookie'si yok: " + email))
        );
    }

    /*
     * Session'ın refresh token'ı ile POST /auth/refresh. Başarılı yanıtta
     * rotate edilen token ve yeni access token session'a yazılır; başarısız
     * yanıtta session değişmez (tüketilen token tekrar kullanılmaz).
     */
    public static ApiResponse refresh(Session session) {
        ApiResponse r = send("POST", "/v1/auth/refresh", null,
                "refresh_token=" + session.refreshToken, null);
        if (r.status() == 200) {
            session.accessToken = r.json().get("token").getAsString();
            refreshTokenFrom(r.setCookie()).ifPresent(t -> session.refreshToken = t);
        }
        return r;
    }

    public static ApiResponse get(Session session, String path) {
        return send("GET", path, session.accessToken, null, null);
    }

    /* Klasör seçici/ağaç: admin olmayan kullanıcıda BUG-03 yavaşlığı; içerik testleri zaman aşımıyla maskelenmesin. */
    public static ApiResponse getSlow(Session session, String path) {
        return send("GET", path, session.accessToken, null, null, java.util.Map.of(), Duration.ofSeconds(120));
    }

    public static ApiResponse post(Session session, String path, Object body) {
        return send("POST", path, session.accessToken, null, body == null ? null : GSON.toJson(body));
    }

    public static ApiResponse put(Session session, String path, Object body) {
        return send("PUT", path, session.accessToken, null, body == null ? null : GSON.toJson(body));
    }

    public static ApiResponse patch(Session session, String path, Object body) {
        return send("PATCH", path, session.accessToken, null, body == null ? null : GSON.toJson(body));
    }

    public static ApiResponse delete(Session session, String path) {
        return send("DELETE", path, session.accessToken, null, null);
    }

    /* Gövdesi hazır JSON metni olan istekler (Scenario Outline tabloları). */
    public static ApiResponse sendRawJson(Session session, String method, String path, String jsonBody) {
        return send(method, path, session.accessToken, null,
                jsonBody == null || jsonBody.isBlank() ? null : jsonBody);
    }

    /* İkili içerik indirme (sürüm/belge içeriği bayt bayt karşılaştırma için). */
    public static byte[] getBytes(Session session, String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BrowserManager.getApiBaseUrl() + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + session.accessToken)
                .GET().build();
        try {
            HttpResponse<byte[]> r = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            ApiTestContext.recordEndpoint("GET", path);
            if (r.statusCode() != 200) {
                throw new AssertionError("GET " + path + " HTTP 200 bekleniyordu, " + r.statusCode());
            }
            return r.body();
        } catch (java.io.IOException e) {
            throw new AssertionError("ECM API'ye ulaşılamadı: GET " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /* multipart/form-data parçası (ad, isteğe bağlı dosya adı, içerik türü, baytlar). */
    public record Part(String name, String filename, String contentType, byte[] bytes) {
    }

    /* Akış yükleme uçları için multipart POST (src/api/rest/documents.rs *_stream). */
    public static ApiResponse postMultipart(Session session, String path, java.util.List<Part> parts) {
        String boundary = "qa-" + java.util.UUID.randomUUID();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            for (Part p : parts) {
                out.write(("--" + boundary + "\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String disposition = "Content-Disposition: form-data; name=\"" + p.name() + "\""
                        + (p.filename() == null ? "" : "; filename=\"" + p.filename() + "\"");
                out.write((disposition + "\r\nContent-Type: " + p.contentType() + "\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.write(p.bytes());
                out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(BrowserManager.getApiBaseUrl() + path))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + session.accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
        try {
            HttpResponse<String> r = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            ApiTestContext.recordEndpoint("POST", path);
            return new ApiResponse(r.statusCode(), r.body(), r.headers().firstValue("set-cookie").orElse(""));
        } catch (java.io.IOException e) {
            throw new AssertionError("ECM API'ye ulaşılamadı: POST " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /* Ek başlıklı JSON POST (ör. Idempotency-Key, src/api/rest/idempotency.rs). */
    public static ApiResponse postWithHeaders(Session session, String path, Object body, java.util.Map<String, String> headers) {
        return send("POST", path, session.accessToken, null, body == null ? null : GSON.toJson(body), headers);
    }

    private static ApiResponse send(String method, String path, String bearer, String cookie, String jsonBody) {
        return send(method, path, bearer, cookie, jsonBody, java.util.Map.of());
    }

    private static ApiResponse send(String method, String path, String bearer, String cookie, String jsonBody,
                                    java.util.Map<String, String> extraHeaders) {
        return send(method, path, bearer, cookie, jsonBody, extraHeaders, Duration.ofSeconds(30));
    }

    private static ApiResponse send(String method, String path, String bearer, String cookie, String jsonBody,
                                    java.util.Map<String, String> extraHeaders, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BrowserManager.getApiBaseUrl() + path))
                .timeout(timeout)
                .header("Accept", "application/json");
        extraHeaders.forEach(builder::header);
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        HttpRequest.BodyPublisher publisher = jsonBody == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);
        if (jsonBody != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, publisher);

        HttpResponse<String> response;
        try {
            response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new AssertionError("ECM API'ye ulaşılamadı: " + method + " " + path, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        ApiTestContext.recordEndpoint(method, path);

        return new ApiResponse(
                response.statusCode(),
                response.body(),
                response.headers().firstValue("set-cookie").orElse("")
        );
    }

    private static Optional<String> refreshTokenFrom(String setCookie) {
        if (setCookie == null) {
            return Optional.empty();
        }
        Matcher m = REFRESH_COOKIE.matcher(setCookie);
        if (m.find() && !m.group(1).isBlank()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }
}
