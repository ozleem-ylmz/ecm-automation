package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Batch 22 — grup yönetimi (src/engine/admin.rs create_group / update_group /
 * delete_group / add_group_member) ve kullanıcının kendi grupları
 * (auth.rs me_groups). All Users sistem grubunu değiştiren bir istek beklenmedik
 * şekilde kabul edilirse paylaşılan ortam hemen eski haline getirilir.
 */
public class GroupSteps {

    private static final String ALL_USERS_NAME = "All Users";

    private final Map<String, String> renamedTo = new HashMap<>();

    @When("yönetici boş adla grup oluşturmayı dener")
    public void yoneticiBosAdlaGrupOlusturmayiDener() {
        tryCreateGroup("");
    }

    @When("yönetici {string} adının küçük harfli haliyle grup oluşturmayı dener")
    public void yoneticiKucukHarfliAdlaGrupOlusturmayiDener(String groupAlias) {
        tryCreateGroup(ApiTestContext.name(groupAlias).toLowerCase());
    }

    @Then("{string} adında yalnız bir grup bulunmalıdır")
    public void adindaYalnizBirGrupBulunmali(String groupAlias) {
        String name = ApiTestContext.name(groupAlias);
        List<String> matches = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/groups").expect(200, "Grup listesi")
                .json().getAsJsonArray("items")) {
            String n = e.getAsJsonObject().get("name").getAsString();
            if (n.equalsIgnoreCase(name)) {
                matches.add(n);
            }
        }
        if (matches.size() != 1) {
            throw new AssertionError("'" + name + "' adında (harf duyarsız) tek grup olmalıydı: " + matches);
        }
    }

    @When("yönetici {string} grubunu yeniden adlandırır")
    public void yoneticiGrubuYenidenAdlandirir(String groupAlias) {
        String newName = TestDataFactory.unique("QA Grup Yeni");
        String returned = EcmApiClient.put(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias),
                        Map.of("name", newName))
                .expect(200, "Grup yeniden adlandırma").json().get("name").getAsString();
        if (!newName.equals(returned)) {
            throw new AssertionError("Yanıttaki grup adı '" + newName + "' olmalıydı, '" + returned + "' geldi.");
        }
        renamedTo.put(groupAlias, newName);
    }

    @Then("{string} grubunun adı yeni adı olmalıdır")
    public void grubunAdiYeniAdiOlmali(String groupAlias) {
        String expected = renamedTo.get(groupAlias);
        String actual = group(ApiTestContext.id(groupAlias)).get("name").getAsString();
        if (!expected.equals(actual)) {
            throw new AssertionError("GET /v1/groups/{id} adı '" + expected + "' olmalıydı, '" + actual + "' geldi.");
        }
    }

    @When("yönetici All Users grubunu silmeyi dener")
    public void yoneticiAllUsersGrubunuSilmeyiDener() {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/groups/" + TestDataFactory.ALL_USERS_GROUP));
    }

    @When("yönetici All Users grubunu yeniden adlandırmayı dener")
    public void yoneticiAllUsersGrubunuYenidenAdlandirmayiDener() {
        mutateAllUsers(Map.of("name", "QA All Users " + System.nanoTime()));
    }

    @When("yönetici All Users grubunu pasifleştirmeyi dener")
    public void yoneticiAllUsersGrubunuPasiflestirmeyiDener() {
        mutateAllUsers(Map.of("is_active", false));
    }

    @Then("All Users grubu {string} adıyla aktif kalmalıdır")
    public void allUsersGrubuAktifKalmali(String expectedName) {
        JsonObject g = group(TestDataFactory.ALL_USERS_GROUP);
        if (!expectedName.equals(g.get("name").getAsString()) || !g.get("is_active").getAsBoolean()) {
            throw new AssertionError("All Users grubu değişmemeliydi: " + g);
        }
    }

    @When("yönetici {string} kullanıcısını {string} grubuna tekrar eklemeyi dener")
    public void yoneticiTekrarEklemeyiDener(String userAlias, String groupAlias) {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(),
                "/v1/groups/" + ApiTestContext.id(groupAlias) + "/members",
                Map.of("user_id", ApiTestContext.id(userAlias))));
    }

    @Then("tekrar ekleme isteği 204 ya da 409 ile sonuçlanmalıdır")
    public void tekrarEklemeIstegi204Ya409() {
        int s = ApiTestContext.lastResponse().status();
        if (s != 204 && s != 409) {
            throw new AssertionError("Aynı üyeyi tekrar ekleme 204 (idempotent) ya da 409 dönmeliydi, HTTP " + s + " "
                    + ApiTestContext.lastResponse().body());
        }
    }

    @Then("{string} grubunun üyeleri arasında {string} tam bir kez listelenmelidir")
    public void grubunUyeleriArasindaTamBirKez(String groupAlias, String userAlias) {
        String userId = ApiTestContext.id(userAlias);
        int count = 0;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/groups/" + ApiTestContext.id(groupAlias) + "/members")
                .expect(200, "Grup üyeleri").json().getAsJsonArray("items")) {
            if (userId.equals(e.getAsJsonObject().get("user_id").getAsString())) {
                count++;
            }
        }
        if (count != 1) {
            throw new AssertionError("'" + userAlias + "' grupta tam bir kez listelenmeliydi, " + count + " kez listelendi.");
        }
    }

    @Then("{string} klasörünün yetki listesinde {string} grubunun izni korunmuş olmalıdır")
    public void klasorYetkiListesindeGrupIzniKorunmus(String folderAlias, String groupAlias) {
        String groupId = ApiTestContext.id(groupAlias);
        boolean aclKept = false;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl")
                .expect(200, "Klasör ACL").json().getAsJsonArray("items")) {
            JsonObject acl = e.getAsJsonObject();
            aclKept |= acl.has("group_id") && !acl.get("group_id").isJsonNull()
                    && groupId.equals(acl.get("group_id").getAsString()) && !acl.get("deny").getAsBoolean();
        }
        if (!aclKept) {
            throw new AssertionError("Yeniden adlandırılan grubun klasör izni (group_id=" + groupId + ") listede yok.");
        }
        // Arayüzün yetki listesinde grup adını çözdüğü kaynak (GET /v1/rbac/groups) yeni adı göstermeli.
        String shown = null;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/rbac/groups").expect(200, "RBAC grup listesi")
                .json().getAsJsonArray("items")) {
            JsonObject g = e.getAsJsonObject();
            if (groupId.equals(g.get("id").getAsString())) {
                shown = g.get("name").getAsString();
            }
        }
        if (!renamedTo.get(groupAlias).equals(shown)) {
            throw new AssertionError("GET /v1/rbac/groups grubu yeni adıyla göstermeli: '" + renamedTo.get(groupAlias)
                    + "', gelen: '" + shown + "'");
        }
    }

    @Then("{string} kendi grup listesinde {string} grubunu görmelidir")
    public void kendiGrupListesindeGormeli(String userAlias, String groupAlias) {
        if (!myGroupIds(userAlias).contains(ApiTestContext.id(groupAlias))) {
            throw new AssertionError("'" + userAlias + "' GET /v1/auth/me/groups listesinde '" + groupAlias + "' olmalıydı.");
        }
    }

    @Then("{string} kendi grup listesinde {string} grubunu görmemelidir")
    public void kendiGrupListesindeGormemeli(String userAlias, String groupAlias) {
        if (myGroupIds(userAlias).contains(ApiTestContext.id(groupAlias))) {
            throw new AssertionError("'" + userAlias + "' GET /v1/auth/me/groups listesinde '" + groupAlias + "' olmamalıydı.");
        }
    }

    // ---------------------------------------------------------------------

    private static List<String> myGroupIds(String userAlias) {
        List<String> ids = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(ApiTestContext.session(userAlias), "/v1/auth/me/groups")
                .expect(200, "Kendi grupları").json().getAsJsonArray("items")) {
            ids.add(e.getAsJsonObject().get("id").getAsString());
        }
        if (ids.contains(TestDataFactory.ALL_USERS_GROUP)) {
            throw new AssertionError("GET /v1/auth/me/groups All Users sistem grubunu listelememeli: " + ids);
        }
        return ids;
    }

    private static JsonObject group(String groupId) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/groups/" + groupId).expect(200, "Grup okuma").json();
    }

    private static void tryCreateGroup(String name) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/groups", Map.of("name", name));
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            String id = r.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/groups/" + id));
        }
    }

    private static void mutateAllUsers(Map<String, Object> body) {
        EcmApiClient.ApiResponse r = EcmApiClient.put(TestDataFactory.admin(), "/v1/groups/" + TestDataFactory.ALL_USERS_GROUP, body);
        ApiTestContext.lastResponse(r);
        if (r.status() >= 200 && r.status() < 300) {
            EcmApiClient.put(TestDataFactory.admin(), "/v1/groups/" + TestDataFactory.ALL_USERS_GROUP,
                    Map.of("name", ALL_USERS_NAME, "is_active", true));
            System.out.println("[GRP-03] All Users grubu değiştirildi ve geri yüklendi: " + body);
        }
    }
}
