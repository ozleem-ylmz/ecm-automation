package steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * USER-02: rol değişikliği. require_auth rolü her istekte DB'den okur
 * (auth.rs:588-597); aynı access token yeniden giriş olmadan yeni rolü taşır.
 */
public class AdminUserSteps {

    @When("{string} kullanıcı listesini ister")
    public void kullaniciListesiniIster(String alias) {
        ApiTestContext.lastResponse(EcmApiClient.get(ApiTestContext.session(alias), "/v1/users"));
    }

    @When("yönetici {string} hesabının rolünü {string} yapar")
    public void yoneticiHesabinRolunuYapar(String alias, String accountRole) {
        String role = EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias),
                        Map.of("role", accountRole))
                .expect(200, "Rol değişikliği (" + alias + " -> " + accountRole + ")")
                .json().get("role").getAsString();
        if (!accountRole.equals(role)) {
            throw new AssertionError("Rol '" + accountRole + "' olmalıydı, '" + role + "' döndü.");
        }
    }

    // =====================================================================
    // Batch 22 — kullanıcı oluşturma doğrulamaları, kendi hesabı korumaları, silme
    // =====================================================================

    private String rejectedEmail;

    @When("yönetici {string} e-postasıyla yeni kullanıcı oluşturmayı dener")
    public void yoneticiAyniEpostaylaOlusturmayiDener(String alias) {
        tryCreateUser(ApiTestContext.name(alias), "user");
    }

    @When("yönetici {string} e-postasının büyük harfli haliyle yeni kullanıcı oluşturmayı dener")
    public void yoneticiBuyukHarfliEpostaylaDener(String alias) {
        tryCreateUser(ApiTestContext.name(alias).toUpperCase(), "user");
    }

    @When("yönetici {string} rolüyle yeni kullanıcı oluşturmayı dener")
    public void yoneticiRolleOlusturmayiDener(String role) {
        rejectedEmail = TestDataFactory.uniqueEmail();
        tryCreateUser(rejectedEmail, role);
    }

    @When("yönetici boş e-postayla kullanıcı oluşturmayı dener")
    public void yoneticiBosEpostaylaDener() {
        tryCreateUser("", "user");
    }

    @Then("reddedilen e-postayla aktif bir kullanıcı oluşmamış olmalıdır")
    public void reddedilenEpostaylaKullaniciOlusmamis() {
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(),
                "/v1/users/lookup?email=" + URLEncoder.encode(rejectedEmail, StandardCharsets.UTF_8));
        if (r.status() != 404) {
            throw new AssertionError("Reddedilen istek kullanıcı oluşturmamalıydı; lookup HTTP " + r.status() + " " + r.body());
        }
    }

    @When("{string} kendi hesabının rolünü {string} yapmayı dener")
    public void kendiRolunuYapmayiDener(String alias, String role) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        ApiTestContext.lastResponse(EcmApiClient.put(s, "/v1/users/" + s.userId, Map.of("role", role)));
    }

    @When("{string} kendi hesabını pasifleştirmeyi dener")
    public void kendiHesabiniPasiflestirmeyiDener(String alias) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        ApiTestContext.lastResponse(EcmApiClient.put(s, "/v1/users/" + s.userId, Map.of("is_active", false)));
    }

    @When("{string} kendi hesabını silmeyi dener")
    public void kendiHesabiniSilmeyiDener(String alias) {
        EcmApiClient.Session s = ApiTestContext.session(alias);
        ApiTestContext.lastResponse(EcmApiClient.delete(s, "/v1/users/" + s.userId));
    }

    @When("yönetici {string} kullanıcısını silmeyi dener")
    public void yoneticiKullaniciyiSilmeyiDener(String alias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias)));
    }

    @Then("{string} kullanıcısı artık bulunmamalıdır")
    public void kullaniciArtikBulunmamali(String alias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(TestDataFactory.admin(), "/v1/users/" + ApiTestContext.id(alias));
        if (r.status() != 404) {
            throw new AssertionError("Silinen kullanıcı için GET /v1/users/{id} 404 olmalıydı, HTTP " + r.status());
        }
    }

    private static void tryCreateUser(String email, String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", TestDataFactory.TEST_USER_PASSWORD);
        body.put("full_name", "QA Auto reddedilmeli");
        body.put("role", role);
        body.put("is_active", true);
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/users", body);
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            // Beklenmeyen kayıt paylaşılan ortamda aktif kalmasın.
            String id = r.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(), "/v1/users/" + id,
                    Map.of("is_active", false)));
        }
    }
}
