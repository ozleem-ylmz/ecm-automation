package steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/*
 * RBAC-01: seed hesaplarla (manager@local, user@local — db/migrations/0015)
 * admin-only uç noktalara istek. Yer tutucular ApiTestContext.resolve ile
 * çözülür: {SELF_ID} oturum sahibinin id'si, {RANDOM_ID} yeni UUID,
 * {UNIQUE} benzersiz sayı (guard hatalıysa bile çakışma olmasın diye).
 */
public class RbacSteps {

    private static final String ALIAS = "bu oturum";

    @Given("{string} hesabı ile API oturumu açılmıştır")
    public void hesabiIleApiOturumuAcilmistir(String email) {
        EcmApiClient.Session s = EcmApiClient.login(email, TestDataFactory.DEFAULT_PASSWORD);
        ApiTestContext.putSession(ALIAS, s);
        ApiTestContext.putId("SELF_ID", s.userId);
    }

    // STR-04 (Batch 34): reddedilen yazma isteğinden önce/sonra yönetici gözüyle alınan durum.
    private String rbacTarget;
    private String rbacStateBefore;
    private String rbacResolvedPath;

    @When("bu oturum {string} {string} isteğini {string} gövdesiyle gönderir")
    public void buOturumIstegiGonderir(String method, String endpoint, String body) {
        String path = ApiTestContext.resolve(endpoint);
        String resolvedBody = ApiTestContext.resolve(body);
        rbacTarget = method + " " + endpoint + (resolvedBody.isBlank() ? "" : " " + resolvedBody);
        rbacResolvedPath = path;
        rbacStateBefore = (endpoint + body).contains("{RANDOM_ID}") ? null
                : persistedState(method, endpoint, path, resolvedBody);
        EcmApiClient.ApiResponse r = EcmApiClient.sendRawJson(
                ApiTestContext.session(ALIAS),
                method,
                path,
                resolvedBody
        );
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            System.out.println("[RBAC-01] GÜVENLİK BUG ADAYI: " + method + " " + endpoint
                    + " admin olmayan hesapla başarılı oldu: " + r.body());
        }
    }

    @Then("isteğin kalıcı bir değişiklik yapmadığı yönetici gözüyle doğrulanmalıdır")
    public void istekKaliciDegisiklikYapmamali() {
        String[] parts = rbacTarget.split(" ", 3);
        String after = rbacStateBefore == null ? null
                : persistedState(parts[0], parts[1], rbacResolvedPath, parts.length > 2 ? parts[2] : "");
        String evidence;
        if (rbacStateBefore == null) {
            evidence = "N/A: " + (parts[0].equals("GET") ? "okuma isteği, kalıcı durum yok"
                    : "{RANDOM_ID} hedefi var olmayan bir kaynak; değişecek kalıcı durum yok");
        } else if (!rbacStateBefore.equals(after)) {
            throw new AssertionError("GÜVENLİK: reddedilen istek kalıcı durumu değiştirdi (" + rbacTarget + ")\nÖnce: "
                    + rbacStateBefore + "\nSonra: " + after);
        } else {
            evidence = "Durum değişmedi: " + after;
        }
        io.qameta.allure.Allure.addAttachment("STR-04 kanıtı — kalıcı durum", "text/plain", rbacTarget + "\n" + evidence);
    }

    /* null: kontrol edilecek kalıcı durum yok (GET veya var olmayan rastgele hedef). */
    private static String persistedState(String method, String template, String path, String body) {
        if (method.equals("GET") || template.contains("{RANDOM_ID}")) {
            return null;
        }
        EcmApiClient.Session admin = TestDataFactory.admin();
        JsonObject json = body.isBlank() ? new JsonObject() : com.google.gson.JsonParser.parseString(body).getAsJsonObject();
        if (method.equals("POST") && path.equals("/v1/users")) {
            String email = json.get("email").getAsString();
            return "lookup(" + email + ")=" + EcmApiClient.get(admin, "/v1/users/lookup?email="
                    + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)).status();
        }
        if (method.equals("PUT") && path.matches("/v1/users/[^/]+")) {
            JsonObject u = EcmApiClient.get(admin, path).expect(200, "Kullanıcı okuma").json();
            return "role=" + u.get("role") + ", is_active=" + u.get("is_active");
        }
        if (method.equals("POST") && path.matches("/v1/groups/[^/]+/members")) {
            TreeSet<String> ids = new TreeSet<>();
            String self = json.get("user_id").getAsString();
            EcmApiClient.get(admin, "/v1/users/" + self + "/groups").expect(200, "Kullanıcı grupları").json()
                    .getAsJsonArray("items").forEach(e -> ids.add(e.getAsJsonObject().get("id").getAsString()));
            return "groups(" + self + ")=" + ids;
        }
        if (method.equals("PUT") && path.matches("/v1/rbac/roles/[^/]+/permissions")) {
            return "permissions=" + rolePermissions(path.split("/")[4]);
        }
        String list = switch (path) {
            case "/v1/groups" -> "/v1/groups";
            case "/v1/classes" -> "/v1/classes";
            case "/v1/rbac/roles" -> "/v1/rbac/roles";
            default -> throw new IllegalStateException("STR-04: kalıcı durum kontrolü tanımlı değil: " + method + " " + path);
        };
        String name = json.get("name").getAsString();
        return list + " içinde '" + name + "' var mı=" + EcmApiClient.get(admin, list).expect(200, "Liste").body()
                .contains("\"" + name + "\"");
    }

    // =====================================================================
    // Batch 22 — özel roller, sistem rolleri, manager klasör kapsamı
    // =====================================================================

    private final Map<String, TreeSet<String>> systemRoleBefore = new HashMap<>();
    private final Map<String, FolderState> folderSnapshots = new HashMap<>();

    record FolderState(String name, boolean active, int aclCount) {
    }

    @Given("yönetici {string} izinlerine sahip {string} özel rolünü oluşturmuştur")
    public void yoneticiOzelRolOlusturmustur(String actionNames, String roleAlias) {
        List<String> ids = permissionIds(actionNames);
        String name = TestDataFactory.unique("QA Rol");
        JsonObject created = EcmApiClient.post(TestDataFactory.admin(), "/v1/rbac/roles",
                        Map.of("name", name, "description", "QA otomasyon özel rolü", "permission_ids", ids))
                .expect(200, "Özel rol oluşturma").json();
        String roleId = created.get("id").getAsString();
        ApiTestContext.putId(roleAlias, roleId);
        ApiTestContext.putName(roleAlias, name);
        // Özel rol silme ucu yok (routes.rs /rbac/roles yalnız GET/POST); benzersiz adla kalır.
        assertRolePermissions(roleId, new TreeSet<>(ids));
        boolean listed = false;
        for (var e : EcmApiClient.get(TestDataFactory.admin(), "/v1/rbac/roles").expect(200, "Rol listesi").json()
                .getAsJsonArray("items")) {
            listed |= roleId.equals(e.getAsJsonObject().get("id").getAsString());
        }
        if (!listed) {
            throw new AssertionError("Oluşturulan özel rol GET /v1/rbac/roles listesinde yok: " + name);
        }
    }

    @Given("{string} klasöründe {string} kullanıcısına {string} özel rolü verilmiştir")
    public void klasordeOzelRolVerilmistir(String folderAlias, String userAlias, String roleAlias) {
        TestDataFactory.grantFolder(folderAlias, ApiTestContext.id(roleAlias), userAlias, false, false);
    }

    @When("yönetici {string} rolünün izinlerini {string} olarak günceller")
    public void yoneticiRolIzinleriniGunceller(String roleAlias, String actionNames) {
        List<String> ids = permissionIds(actionNames);
        String roleId = ApiTestContext.id(roleAlias);
        EcmApiClient.put(TestDataFactory.admin(), "/v1/rbac/roles/" + roleId + "/permissions",
                        Map.of("permission_ids", ids))
                .expect(200, "Özel rol izin güncelleme");
        assertRolePermissions(roleId, new TreeSet<>(ids));
    }

    @When("yönetici {string} sistem rolünün izinlerini boşaltmayı dener")
    public void yoneticiSistemRolIzinleriniBosaltmayiDener(String aclRole) {
        String roleId = TestDataFactory.roleId(aclRole);
        TreeSet<String> before = rolePermissions(roleId);
        systemRoleBefore.put(aclRole, before);
        EcmApiClient.ApiResponse r = EcmApiClient.put(TestDataFactory.admin(),
                "/v1/rbac/roles/" + roleId + "/permissions", Map.of("permission_ids", List.of()));
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            // Paylaşılan ortam korunur: kabul edildiyse hemen eski izinler geri yazılır, test yine düşer.
            EcmApiClient.put(TestDataFactory.admin(), "/v1/rbac/roles/" + roleId + "/permissions",
                    Map.of("permission_ids", new ArrayList<>(before)));
            System.out.println("[RBAC-03] Sistem rolü izinleri değiştirildi ve geri yüklendi: " + aclRole);
        }
    }

    @Then("{string} sistem rolünün izinleri değişmemiş olmalıdır")
    public void sistemRolIzinleriDegismemisOlmali(String aclRole) {
        TreeSet<String> before = systemRoleBefore.get(aclRole);
        TreeSet<String> after = rolePermissions(TestDataFactory.roleId(aclRole));
        if (before.isEmpty() || !before.equals(after)) {
            throw new AssertionError("'" + aclRole + "' sistem rolünün izinleri değişmemeliydi. Önce: " + before
                    + " Sonra: " + after);
        }
    }

    @Given("{string} klasörünün mevcut durumu kaydedilmiştir")
    public void klasorunMevcutDurumuKaydedilmistir(String folderAlias) {
        folderSnapshots.put(folderAlias, folderState(folderAlias));
    }

    @Then("{string} klasörü kaydedilen durumundan değişmemiş olmalıdır")
    public void klasorKaydedilenDurumdanDegismemis(String folderAlias) {
        FolderState before = folderSnapshots.get(folderAlias);
        FolderState after = folderState(folderAlias);
        if (!after.equals(before)) {
            throw new AssertionError("'" + folderAlias + "' değişmemeliydi. Önce: " + before + " Sonra: " + after);
        }
    }

    /* İşlemler: pasifleştirme | yeniden adlandırma | yetki verme */
    @Then("{string} {string} klasöründe şu yönetim işlemlerinin her biri {int} ile reddedilmelidir")
    public void klasorYonetimIslemleriReddedilmeli(String userAlias, String folderAlias, int expected, DataTable table) {
        EcmApiClient.Session s = ApiTestContext.session(userAlias);
        String base = "/v1/folders/" + ApiTestContext.id(folderAlias);
        List<String> mismatches = new ArrayList<>();
        for (String action : table.asList()) {
            EcmApiClient.ApiResponse r = switch (action.trim()) {
                case "pasifleştirme" -> EcmApiClient.put(s, base + "/active", Map.of("is_active", false));
                case "yeniden adlandırma" -> EcmApiClient.patch(s, base, Map.of("name", TestDataFactory.unique("QA İzinsiz Ad")));
                case "yetki verme" -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("role_id", TestDataFactory.ROLE_VIEWER);
                    body.put("group_id", TestDataFactory.ALL_USERS_GROUP);
                    body.put("deny", false);
                    yield EcmApiClient.post(s, base + "/acl", body);
                }
                case "yetki kaldırma" -> {
                    String aclId = EcmApiClient.get(TestDataFactory.admin(), base + "/acl").expect(200, "Klasör ACL")
                            .json().getAsJsonArray("items").get(0).getAsJsonObject().get("id").getAsString();
                    yield EcmApiClient.delete(s, base + "/acl/" + aclId);
                }
                default -> throw new IllegalArgumentException("Bilinmeyen klasör işlemi: " + action);
            };
            if (r.status() != expected) {
                mismatches.add(action + " -> HTTP " + r.status() + " " + r.body());
            }
        }
        if (!mismatches.isEmpty()) {
            throw new AssertionError("'" + userAlias + "' için her klasör yönetim işlemi HTTP " + expected
                    + " ile reddedilmeliydi. Beklenmeyen: " + mismatches);
        }
    }

    private static FolderState folderState(String folderAlias) {
        String id = ApiTestContext.id(folderAlias);
        JsonObject folder = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + id)
                .expect(200, "Klasör okuma").json().getAsJsonObject("folder");
        int acl = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + id + "/acl")
                .expect(200, "Klasör ACL okuma").json().getAsJsonArray("items").size();
        return new FolderState(folder.get("name").getAsString(), folder.get("is_active").getAsBoolean(), acl);
    }

    private static List<String> permissionIds(String actionNames) {
        Map<String, String> byName = new HashMap<>();
        for (var e : EcmApiClient.get(TestDataFactory.admin(), "/v1/rbac/permissions").expect(200, "İzin listesi")
                .json().getAsJsonArray("items")) {
            JsonObject p = e.getAsJsonObject();
            byName.put(p.get("action_name").getAsString(), p.get("id").getAsString());
        }
        List<String> ids = new ArrayList<>();
        for (String n : actionNames.split(",")) {
            String id = byName.get(n.trim());
            if (id == null) {
                throw new IllegalArgumentException("ECM'de böyle bir izin yok: " + n + " (mevcut: " + byName.keySet() + ")");
            }
            ids.add(id);
        }
        return ids;
    }

    private static TreeSet<String> rolePermissions(String roleId) {
        JsonArray ids = EcmApiClient.get(TestDataFactory.admin(), "/v1/rbac/roles/" + roleId)
                .expect(200, "Rol detayı").json().getAsJsonArray("permission_ids");
        TreeSet<String> out = new TreeSet<>();
        ids.forEach(e -> out.add(e.getAsString()));
        return out;
    }

    private static void assertRolePermissions(String roleId, TreeSet<String> expected) {
        TreeSet<String> actual = rolePermissions(roleId);
        if (!expected.equals(actual)) {
            throw new AssertionError("Rol izinleri " + expected + " olmalıydı, GET " + actual + " döndü.");
        }
    }
}
