package utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/*
 * API tabanlı senaryoların paylaşılan durumu. Projede DI (picocontainer) yok;
 * adım sınıfları arasında veri bu senaryo kapsamlı statik tutucu üzerinden
 * paylaşılır ve steps.ApiHooks her senaryo başında sıfırlar.
 *
 * Takma adlar (alias) Gherkin'de geçen isimlerdir: "yönetici", "test
 * kullanıcısı", "üst klasör" vb.
 */
public final class ApiTestContext {

    private static String scenarioId;
    private static String scenarioUri;
    private static String scenarioName;

    private static final Set<String> recordedEndpoints = new LinkedHashSet<>();

    private static final Map<String, EcmApiClient.Session> sessions = new HashMap<>();
    private static final Map<String, String> ids = new HashMap<>();
    private static final Map<String, String> names = new HashMap<>();

    private static EcmApiClient.ApiResponse lastResponse;

    /* Temizlik: en son oluşturulan önce geri alınır. */
    private static final Deque<Runnable> cleanup = new ArrayDeque<>();

    private ApiTestContext() {
    }

    public static synchronized void start(String id, String uri, String name) {
        scenarioId = id;
        scenarioUri = uri;
        scenarioName = name;
        recordedEndpoints.clear();
        sessions.clear();
        ids.clear();
        names.clear();
        lastResponse = null;
        cleanup.clear();
    }

    public static synchronized void recordEndpoint(String method, String path) {
        if (scenarioName == null) {
            return;
        }
        try {
            String plainPath = path.contains("?") ? path.substring(0, path.indexOf('?')) : path;
            String endpoint = method + " " + EndpointTemplateMatcher.match(method, plainPath);
            if (recordedEndpoints.add(endpoint)) {
                RuntimeEndpointCoverage.record(scenarioId, scenarioUri, scenarioName, endpoint);
            }
        } catch (Exception ignored) {
            // Coverage toplama testi asla düşürmemeli.
        }
    }

    public static void putSession(String alias, EcmApiClient.Session session) {
        sessions.put(alias, session);
    }

    public static EcmApiClient.Session session(String alias) {
        EcmApiClient.Session s = sessions.get(alias);
        if (s == null) {
            throw new IllegalStateException("Oturum yok: '" + alias + "'. Önce giriş adımı çalışmalı.");
        }
        return s;
    }

    public static void putId(String alias, String id) {
        ids.put(alias, id);
    }

    public static String id(String alias) {
        String id = ids.get(alias);
        if (id == null) {
            throw new IllegalStateException("Kayıtlı kaynak yok: '" + alias + "'. Mevcut: " + ids.keySet());
        }
        return id;
    }

    /* Kimliğin senaryodaki takma adı (yoksa kimliğin kendisi); liste doğrulamalarında okunur rapor için. */
    public static String aliasOfId(String id) {
        for (Map.Entry<String, String> e : ids.entrySet()) {
            if (e.getValue().equals(id)) {
                return e.getKey();
            }
        }
        return id;
    }

    public static boolean hasId(String alias) {
        return ids.containsKey(alias);
    }

    public static void putName(String alias, String name) {
        names.put(alias, name);
    }

    public static String name(String alias) {
        String n = names.get(alias);
        if (n == null) {
            throw new IllegalStateException("Kayıtlı isim yok: '" + alias + "'.");
        }
        return n;
    }

    public static void lastResponse(EcmApiClient.ApiResponse response) {
        lastResponse = response;
    }

    public static EcmApiClient.ApiResponse lastResponse() {
        if (lastResponse == null) {
            throw new IllegalStateException("Henüz API yanıtı yok.");
        }
        return lastResponse;
    }

    public static void onCleanup(Runnable action) {
        cleanup.push(action);
    }

    public static void runCleanup() {
        while (!cleanup.isEmpty()) {
            try {
                cleanup.pop().run();
            } catch (Throwable t) {
                System.out.println("[CLEANUP] atlandı: " + t.getMessage());
            }
        }
    }

    /*
     * {DOC_ID}, {FOLDER_ID} gibi yer tutucuları kayıtlı id'lerle değiştirir;
     * {RANDOM_ID} her seferinde yeni bir UUID üretir.
     */
    public static String resolve(String text) {
        if (text == null) {
            return null;
        }
        String out = text;
        while (out.contains("{RANDOM_ID}")) {
            out = out.replaceFirst("\\{RANDOM_ID}", java.util.UUID.randomUUID().toString());
        }
        while (out.contains("{UNIQUE}")) {
            out = out.replaceFirst("\\{UNIQUE}", Long.toString(System.nanoTime()));
        }
        for (Map.Entry<String, String> e : ids.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }
}
