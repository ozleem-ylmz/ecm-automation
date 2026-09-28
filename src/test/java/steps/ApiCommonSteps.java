package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

/*
 * Yeni API tabanlı feature dosyalarının ortak adımları: oturum, test
 * kullanıcısı, grup, klasör, ACL ve yanıt doğrulamaları.
 */
public class ApiCommonSteps {

    @Given("yönetici API oturumu açmıştır")
    public void yoneticiApiOturumuAcmistir() {
        TestDataFactory.admin();
    }

    @Given("{string} isimli {string} hesap rolünde yeni bir test kullanıcısı oluşturulmuştur")
    public void yeniTestKullanicisiOlusturulmustur(String alias, String accountRole) {
        TestDataFactory.createUser(alias, accountRole);
    }

    @Given("{string} API ile giriş yapmıştır")
    public void apiIleGirisYapmistir(String alias) {
        TestDataFactory.loginAs(alias);
    }

    @When("{string} API ile yeniden giriş yapar")
    public void apiIleYenidenGirisYapar(String alias) {
        TestDataFactory.loginAs(alias);
    }

    @Given("{string} isimli yeni bir grup oluşturulmuştur")
    public void yeniBirGrupOlusturulmustur(String alias) {
        TestDataFactory.createGroup(alias);
    }

    @Given("yetki devralması kapalı {string} oluşturulmuştur")
    public void yetkiDevralmasiKapaliKlasorOlusturulmustur(String alias) {
        TestDataFactory.createFolder(TestDataFactory.admin(), alias, null, false);
    }

    @Given("{string} altında yetki devralması açık {string} oluşturulmuştur")
    public void yetkiDevralmasiAcikAltKlasorOlusturulmustur(String parentAlias, String alias) {
        TestDataFactory.createFolder(TestDataFactory.admin(), alias, parentAlias, true);
    }

    @Given("{string} klasöründe {string} grubuna {string} rolü verilmiştir")
    public void klasordeGrubaRolVerilmistir(String folderAlias, String groupAlias, String aclRole) {
        TestDataFactory.grantFolder(folderAlias, TestDataFactory.roleId(aclRole), groupAlias, true, false);
    }

    @Given("{string} klasöründe {string} kullanıcısına {string} rolü verilmiştir")
    public void klasordeKullaniciyaRolVerilmistir(String folderAlias, String userAlias, String aclRole) {
        TestDataFactory.grantFolder(folderAlias, TestDataFactory.roleId(aclRole), userAlias, false, false);
    }

    @Then("son API yanıtının durum kodu {int} olmalıdır")
    public void sonApiYanitininDurumKodu(int expected) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (r.status() != expected) {
            throw new AssertionError(
                    "HTTP " + expected + " bekleniyordu, " + r.status() + " geldi. Yanıt: " + r.body()
            );
        }
    }

    @Then("son API yanıtının hata kodu {string} olmalıdır")
    public void sonApiYanitininHataKodu(String expectedCode) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (!expectedCode.equals(r.errorCode())) {
            throw new AssertionError(
                    "Hata kodu '" + expectedCode + "' bekleniyordu, '" + r.errorCode() + "' geldi. Yanıt: " + r.body()
            );
        }
    }

    @Then("son API yanıtının hata mesajı {string} içermelidir")
    public void sonApiYanitininHataMesajiIcermelidir(String expectedFragment) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        if (!r.errorMessage().contains(expectedFragment)) {
            throw new AssertionError(
                    "Hata mesajı '" + expectedFragment + "' içermeliydi. Gelen: '" + r.errorMessage()
                            + "' (HTTP " + r.status() + ")"
            );
        }
    }

    @Then("{string} belgesinin durumu {string} olmalıdır")
    public void belgeninDurumuOlmalidir(String documentAlias, String expectedStatus) {
        String actual = TestDataFactory.documentStatus(TestDataFactory.admin(), documentAlias);
        if (!expectedStatus.equals(actual)) {
            throw new AssertionError(
                    "'" + documentAlias + "' durumu '" + expectedStatus + "' olmalıydı, '" + actual + "' bulundu."
            );
        }
    }
}
