package steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.FolderAccessPage;
import pages.FolderPage;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

/*
 * ACL-01 / ACL-02 / ACL-06. Erişim kararları API ile (GET /v1/folders/{id}),
 * yetki devralması değişikliği ise mevcut FolderPage adımlarıyla UI'dan
 * yapılır. FolderPage değiştirilmez; yalnızca çağrılır.
 */
public class AccessControlSteps {

    private static final String VIEW_DENIED = "You do not have permission to view this folder";

    @Then("{string} {string} klasörünü görüntüleyebilmelidir")
    public void klasoruGoruntuleyebilmelidir(String userAlias, String folderAlias) {
        EcmApiClient.ApiResponse r = readFolder(userAlias, folderAlias);
        if (r.status() != 200) {
            throw new AssertionError("'" + userAlias + "' '" + folderAlias + "' klasörünü okuyabilmeliydi. HTTP "
                    + r.status() + " " + r.body());
        }
    }

    @Then("{string} {string} klasörünü görüntüleyememelidir")
    public void klasoruGoruntuleyememelidir(String userAlias, String folderAlias) {
        EcmApiClient.ApiResponse r = readFolder(userAlias, folderAlias);
        if (r.status() != 403 || !r.errorMessage().contains(VIEW_DENIED)) {
            throw new AssertionError("'" + userAlias + "' '" + folderAlias + "' klasörünü okuyamamalıydı "
                    + "(HTTP 403 '" + VIEW_DENIED + "'). Gelen: HTTP " + r.status() + " " + r.body());
        }
    }

    @When("yönetici {string} kullanıcısını {string} grubuna ekler")
    public void yoneticiKullaniciyiGrubaEkler(String userAlias, String groupAlias) {
        TestDataFactory.addMember(groupAlias, userAlias);
    }

    @When("yönetici {string} kullanıcısını {string} grubundan çıkarır")
    public void yoneticiKullaniciyiGruptanCikarir(String userAlias, String groupAlias) {
        TestDataFactory.removeMember(groupAlias, userAlias);
    }

    @When("{string} klasöründe {string} kullanıcısı için {string} rolü reddedilir")
    public void klasordeKullaniciIcinRolReddedilir(String folderAlias, String userAlias, String aclRole) {
        TestDataFactory.grantFolder(folderAlias, TestDataFactory.roleId(aclRole), userAlias, false, true);
    }

    @Then("{string} klasörünün yetki durumu {string} olmalıdır")
    public void klasorunYetkiDurumuOlmalidir(String folderAlias, String expectedState) {
        String actual = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/acl")
                .expect(200, "Klasör ACL listesi").json()
                .get("effective_state").getAsString();
        if (!expectedState.equals(actual)) {
            throw new AssertionError("'" + folderAlias + "' effective_state '" + expectedState
                    + "' olmalıydı, '" + actual + "' geldi.");
        }
    }

    @When("admin kullanıcı tarayıcıda {string} klasörünün yetki devralmasını kapatır")
    public void adminTarayicidaYetkiDevralmasiniKapatir(String folderAlias) {
        toggleInheritanceInUi(folderAlias, false);
    }

    @When("admin kullanıcı tarayıcıda {string} klasörünün yetki devralmasını tekrar açar")
    public void adminTarayicidaYetkiDevralmasiniTekrarAcar(String folderAlias) {
        toggleInheritanceInUi(folderAlias, true);
    }

    @Then("{string} tarayıcıda {string} klasörünü açtığında okuma erişimi olmadığı mesajını görmelidir")
    public void tarayicidaOkumaErisimiYokMesajiniGormelidir(String userAlias, String folderAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLogin(BrowserManager.context, ApiTestContext.name(userAlias), TestDataFactory.TEST_USER_PASSWORD);

        FolderAccessPage accessPage = new FolderAccessPage(BrowserManager.page);
        // Klasörün kendisi reddedildiğinde FolderDetail, GET /folders/{id} 403
        // yanıtının mesajını gösterir (bkz. FolderAccessPage dal 1). Hem gerçek
        // reddi (403 access_denied + okuma reddi mesajı) hem UI mesajını doğrularız.
        com.microsoft.playwright.Response folderResponse =
                accessPage.klasorDetayiniAcVeKlasorYanitiniAl(ApiTestContext.id(folderAlias));
        String body = folderResponse.text();

        if (folderResponse.status() != 403
                || !body.contains("\"code\":\"access_denied\"")
                || !body.contains(VIEW_DENIED)) {
            throw new AssertionError("'" + userAlias + "' tarayıcıda '" + folderAlias
                    + "' klasörünü açtığında erişim reddedilmedi: GET /v1/folders/{id} HTTP "
                    + folderResponse.status() + " " + body);
        }

        if (!accessPage.klasorGoruntulemeReddiMesajiGorunuyorMu()) {
            throw new AssertionError("'" + userAlias + "' için '" + folderAlias
                    + "' klasöründe okuma erişimi olmadığı mesajı görünmedi (beklenen: \""
                    + FolderAccessPage.FOLDER_VIEW_DENIED + "\"). URL: " + BrowserManager.page.url());
        }
    }

    private static EcmApiClient.ApiResponse readFolder(String userAlias, String folderAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(ApiTestContext.session(userAlias),
                "/v1/folders/" + ApiTestContext.id(folderAlias));
        ApiTestContext.lastResponse(r);
        return r;
    }

    private static void toggleInheritanceInUi(String folderAlias, boolean expectedAfter) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);

        new FolderAccessPage(BrowserManager.page).klasorDetayiniAc(ApiTestContext.id(folderAlias));

        FolderPage folderPage = new FolderPage(BrowserManager.page);
        if (folderPage.permissionInheritanceAcikMi() == expectedAfter) {
            throw new AssertionError("'" + folderAlias + "' yetki devralması zaten "
                    + (expectedAfter ? "açık" : "kapalı") + "; ön koşul hatalı.");
        }
        folderPage.permissionInheritanceDegistir();

        boolean persisted = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias))
                .expect(200, "Klasör detayı").json()
                .getAsJsonObject("folder").get("inherit_permissions").getAsBoolean();
        if (persisted != expectedAfter) {
            throw new AssertionError("UI değişikliği kaydedilmedi: inherit_permissions=" + persisted);
        }
    }
}
