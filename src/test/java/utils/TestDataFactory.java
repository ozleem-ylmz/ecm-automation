package utils;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * API ile benzersiz test verisi üretir ve her kaynak için ApiTestContext'e
 * geri alma adımı kaydeder. ECM'de kullanıcı silme audit geçmişi olan
 * kullanıcıda 409 döndüğü için (users.rs delete_user) test kullanıcıları
 * silinmez, pasifleştirilir; klasörler de içerik barındırabildiği için
 * pasifleştirilir.
 */
public final class TestDataFactory {

    public static final String ADMIN_EMAIL = "admin@local";
    public static final String DEFAULT_PASSWORD = "123";
    public static final String TEST_USER_PASSWORD = "QaAuto!2345";

    /* db/migrations/0019_role_permission.sql seed id'leri */
    public static final String ROLE_MANAGER = "22222222-0000-0000-0000-000000000002";
    public static final String ROLE_EDITOR = "22222222-0000-0000-0000-000000000003";
    public static final String ROLE_VIEWER = "22222222-0000-0000-0000-000000000004";

    /* src/domain/access_control.rs ALL_USERS_GROUP_ID */
    public static final String ALL_USERS_GROUP = "44444444-0000-0000-0000-000000000001";

    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestDataFactory() {
    }

    public static String unique(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + SEQ.incrementAndGet();
    }

    /*
     * Benzersiz ve ECM'in kabul ettiği test e-postası.
     * ECM (src/engine/admin.rs validate_email) alan adında en az bir nokta
     * ister; "@local" gibi noktasız alan adları 422 validation_error alır
     * (ECM birim testi: "user@domain" reddedilmeli). example.com, RFC 2606
     * ile test için ayrılmış alan adıdır.
     * Paralel/shard koşularında ayrı JVM'ler aynı milisaniyede aynı sayacı
     * üretebileceği için JVM başına runId ve worker id de eklenir.
     * Biçim: qa.auto-<runId>-<worker>-<millis>-<sayaç>@example.com
     */
    public static String uniqueEmail() {
        return ("qa.auto-" + TestData.RUN_ID + "-" + TestData.WORKER_ID + "-"
                + System.currentTimeMillis() + "-" + SEQ.incrementAndGet() + "@example.com")
                .toLowerCase();
    }

    public static EcmApiClient.Session admin() {
        if (!sessionExists("yönetici")) {
            ApiTestContext.putSession("yönetici", EcmApiClient.login(ADMIN_EMAIL, DEFAULT_PASSWORD));
        }
        return ApiTestContext.session("yönetici");
    }

    private static boolean sessionExists(String alias) {
        try {
            ApiTestContext.session(alias);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /* Hesap rolü: admin | manager | user. Döner: yeni kullanıcının id'si. */
    public static String createUser(String alias, String accountRole) {
        String email = uniqueEmail();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", TEST_USER_PASSWORD);
        body.put("full_name", "QA Auto " + alias);
        body.put("role", accountRole);
        body.put("is_active", true);

        JsonObject created = EcmApiClient.post(admin(), "/v1/users", body)
                .expect(200, "Test kullanıcısı oluşturma").json();
        String userId = created.get("id").getAsString();

        ApiTestContext.putId(alias, userId);
        ApiTestContext.putName(alias, email);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(admin(), "/v1/users/" + userId,
                Map.of("is_active", false)));
        return userId;
    }

    public static EcmApiClient.Session loginAs(String alias) {
        EcmApiClient.Session s = EcmApiClient.login(ApiTestContext.name(alias), TEST_USER_PASSWORD);
        ApiTestContext.putSession(alias, s);
        return s;
    }

    public static String createGroup(String alias) {
        String name = unique("QA Grup");
        JsonObject created = EcmApiClient.post(admin(), "/v1/groups", Map.of("name", name))
                .expect(200, "Grup oluşturma").json();
        String groupId = created.get("id").getAsString();
        ApiTestContext.putId(alias, groupId);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(admin(), "/v1/groups/" + groupId));
        return groupId;
    }

    public static void addMember(String groupAlias, String userAlias) {
        EcmApiClient.post(admin(), "/v1/groups/" + ApiTestContext.id(groupAlias) + "/members",
                        Map.of("user_id", ApiTestContext.id(userAlias)))
                .expect(204, "Gruba üye ekleme");
    }

    public static void removeMember(String groupAlias, String userAlias) {
        EcmApiClient.delete(admin(), "/v1/groups/" + ApiTestContext.id(groupAlias)
                        + "/members/" + ApiTestContext.id(userAlias))
                .expect(204, "Gruptan üye çıkarma");
    }

    /* parentAlias null ise sistem kökü altında (yalnız admin oluşturabilir). */
    public static String createFolder(EcmApiClient.Session as, String alias, String parentAlias, boolean inherit) {
        String name = unique("QA Klasör");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("parent_id", parentAlias == null
                ? BrowserManager.ROOT_FOLDER_ID
                : ApiTestContext.id(parentAlias));
        body.put("inherit_permissions", inherit);

        JsonObject created = EcmApiClient.post(as, "/v1/folders", body)
                .expect(200, "Klasör oluşturma (" + alias + ")").json();
        String folderId = created.get("id").getAsString();
        ApiTestContext.putId(alias, folderId);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(admin(), "/v1/folders/" + folderId + "/active",
                Map.of("is_active", false)));
        return folderId;
    }

    /* principalAlias bir kullanıcı ya da grup takma adı olabilir. */
    public static String grantFolder(String folderAlias, String roleId, String principalAlias,
                                     boolean principalIsGroup, boolean deny) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", roleId);
        body.put(principalIsGroup ? "group_id" : "user_id", ApiTestContext.id(principalAlias));
        body.put("deny", deny);
        JsonObject grant = EcmApiClient.post(admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl", body)
                .expect(200, "Klasör yetkisi verme").json();
        return grant.get("id").getAsString();
    }

    public static String createUnclassifiedDocument(EcmApiClient.Session as, String alias, String folderAlias) {
        String title = unique("QA Belge");
        EcmApiClient.ApiResponse r = EcmApiClient.post(as, "/v1/documents/unclassified",
                documentBody(title, folderAlias, null));
        // save_doc_unclassified -> 201 Created (src/api/rest/documents.rs:168)
        r.expect(201, "Sınıfsız belge oluşturma (" + alias + ")");
        String documentId = r.json().get("document_id").getAsString();
        registerDocument(alias, documentId, title);
        return documentId;
    }

    /* Temizlikte belge soft delete edilir (restore edilebilir, iz bırakmaz). */
    public static void registerDocument(String alias, String documentId, String title) {
        ApiTestContext.putId(alias, documentId);
        ApiTestContext.putName(alias, title);
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(admin(), "/v1/documents/" + documentId));
    }

    public static String roleId(String aclRoleName) {
        return switch (aclRoleName) {
            case "viewer" -> ROLE_VIEWER;
            case "editor" -> ROLE_EDITOR;
            case "manager" -> ROLE_MANAGER;
            default -> throw new IllegalArgumentException("Bilinmeyen ACL rolü: " + aclRoleName);
        };
    }

    /* Sınıflı yükleme gövdesi (SaveDocumentRequest, application/dto/document.rs:11-30). */
    public static Map<String, Object> classifiedDocumentBody(String title, String classId, long classVersion,
                                                             Map<String, Object> properties, String folderAlias) {
        Map<String, Object> body = documentBody(title, folderAlias, properties);
        body.put("class_id", classId);
        body.put("class_version", classVersion);
        return body;
    }

    private static Map<String, Object> documentBody(String title, String folderAlias, Map<String, Object> metadata) {
        byte[] bytes = ("QA otomasyon içeriği " + title).getBytes(StandardCharsets.UTF_8);
        List<Integer> content = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            content.add(b & 0xff);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("metadata", metadata == null ? Map.of() : metadata);
        body.put("mime_type", "text/plain");
        body.put("original_filename", "qa-auto.txt");
        body.put("folder_id", folderAlias == null
                ? BrowserManager.ROOT_FOLDER_ID
                : ApiTestContext.id(folderAlias));
        body.put("content", content);
        return body;
    }

    /*
     * Mevcut belgeye yeni sürüm gövdesi: POST /v1/documents/unclassified gövdesinde
     * document_id bulunursa yeni sürüm oluşturulur (save_doc_unclassified).
     */
    public static Map<String, Object> newVersionBody(String documentAlias, String versionType, String text) {
        JsonObject doc = EcmApiClient.get(admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma (yeni sürüm gövdesi)").json();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        List<Integer> content = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            content.add(b & 0xff);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", doc.getAsJsonObject("document").get("title").getAsString());
        body.put("metadata", Map.of());
        body.put("mime_type", "text/plain");
        body.put("original_filename", "qa-auto-v2.txt");
        body.put("document_id", ApiTestContext.id(documentAlias));
        body.put("version_type", versionType);
        if (doc.has("folder") && doc.get("folder").isJsonObject()) {
            body.put("folder_id", doc.getAsJsonObject("folder").get("id").getAsString());
        }
        body.put("content", content);
        return body;
    }

    public static String documentStatus(EcmApiClient.Session as, String documentAlias) {
        return EcmApiClient.get(as, "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma").json()
                .getAsJsonObject("document").get("status").getAsString();
    }
}
