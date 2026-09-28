package steps;

import com.google.gson.JsonObject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.Map;

/*
 * DOC-01: admin belgesi, admin olmayan herkes için salt okunur.
 *  - okuma serbest (created_by_admin=true, can_manage=false)
 *  - yazma (etiket): assert_caller_may_modify -> 403 "created by an administrator"
 *  - yeniden adlandırma / durum: can_manage_document_scoped=false -> 403
 * Karşılaştırma: kullanıcının kendi belgesi yeniden adlandırılabilir.
 */
public class DocumentSecuritySteps {

    @Given("{string} {string} altında {string} alt klasörünü oluşturmuştur")
    public void altKlasorunuOlusturmustur(String userAlias, String parentAlias, String folderAlias) {
        TestDataFactory.createFolder(ApiTestContext.session(userAlias), folderAlias, parentAlias, true);
    }

    @Given("yönetici {string} klasörüne {string} belgesini yüklemiştir")
    public void yoneticiKlasoreBelgeYuklemistir(String folderAlias, String documentAlias) {
        TestDataFactory.createUnclassifiedDocument(TestDataFactory.admin(), documentAlias, folderAlias);
    }

    @Given("{string} {string} klasörüne {string} belgesini yüklemiştir")
    public void kullaniciKlasoreBelgeYuklemistir(String userAlias, String folderAlias, String documentAlias) {
        TestDataFactory.createUnclassifiedDocument(ApiTestContext.session(userAlias), documentAlias, folderAlias);
    }

    @Then("{string} {string} belgesini görüntüleyebilmelidir")
    public void belgeyiGoruntuleyebilmelidir(String userAlias, String documentAlias) {
        readDocument(userAlias, documentAlias).expect(200, "'" + userAlias + "' belge okuma");
    }

    @Then("{string} için {string} belgesi admin tarafından oluşturulmuş ve yönetilemez görünmelidir")
    public void belgeAdminTarafindanOlusturulmusVeYonetilemez(String userAlias, String documentAlias) {
        JsonObject json = readDocument(userAlias, documentAlias).expect(200, "Belge okuma").json();
        boolean createdByAdmin = json.get("created_by_admin").getAsBoolean();
        boolean canManage = json.get("can_manage").getAsBoolean();
        boolean canRename = json.get("can_rename").getAsBoolean();
        if (!createdByAdmin || canManage || canRename) {
            throw new AssertionError("Beklenen: created_by_admin=true, can_manage=false, can_rename=false. Gelen: "
                    + "created_by_admin=" + createdByAdmin + ", can_manage=" + canManage + ", can_rename=" + canRename);
        }
    }

    @When("{string} {string} belgesine {string} etiketini eklemeyi dener")
    public void belgeyeEtiketEklemeyiDener(String userAlias, String documentAlias, String tagName) {
        ApiTestContext.lastResponse(EcmApiClient.post(ApiTestContext.session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/tags",
                Map.of("tag_name", tagName)));
    }

    @When("{string} {string} belgesini yeniden adlandırmayı dener")
    public void belgeyiYenidenAdlandirmayiDener(String userAlias, String documentAlias) {
        ApiTestContext.lastResponse(EcmApiClient.patch(ApiTestContext.session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias) + "/title",
                Map.of("title", TestDataFactory.unique("QA Yeni Başlık"))));
    }

    @When("{string} {string} belgesinin durumunu {string} yapmayı dener")
    public void belgeninDurumunuYapmayiDener(String userAlias, String documentAlias, String status) {
        ApiTestContext.lastResponse(DocumentLifecycleSteps.changeStatus(
                ApiTestContext.session(userAlias), documentAlias, status));
    }

    private static EcmApiClient.ApiResponse readDocument(String userAlias, String documentAlias) {
        EcmApiClient.ApiResponse r = EcmApiClient.get(ApiTestContext.session(userAlias),
                "/v1/documents/" + ApiTestContext.id(documentAlias));
        ApiTestContext.lastResponse(r);
        return r;
    }
}
