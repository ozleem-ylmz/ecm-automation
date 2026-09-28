package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.Map;

/*
 * STAT-01: belge durum geçiş matrisi (src/engine/documents/admin.rs:26-37).
 * Her başlangıç durumuna draft'tan tek bir izinli geçişle ulaşılır; draft
 * yeni belgenin varsayılan durumudur (db/migrations/0008_document.sql:19).
 */
public class DocumentLifecycleSteps {

    @Given("yönetici API ile yeni bir sınıfsız {string} oluşturmuştur")
    public void yoneticiApiIleYeniSinifsizBelgeOlusturmustur(String documentAlias) {
        TestDataFactory.createUnclassifiedDocument(TestDataFactory.admin(), documentAlias, null);
    }

    @Given("{string} belgesi API ile {string} durumuna getirilmiştir")
    public void belgesiApiIleDurumunaGetirilmistir(String documentAlias, String status) {
        if (!"draft".equals(status)) {
            changeStatus(TestDataFactory.admin(), documentAlias, status)
                    .expect(200, "Ön koşul durum geçişi draft -> " + status);
        }
        String actual = TestDataFactory.documentStatus(TestDataFactory.admin(), documentAlias);
        if (!status.equals(actual)) {
            throw new AssertionError("Ön koşul: durum '" + status + "' olmalıydı, '" + actual + "' bulundu.");
        }
    }

    @When("yönetici {string} belgesinin durumunu {string} yapmayı dener")
    public void yoneticiBelgeninDurumunuYapmayiDener(String documentAlias, String status) {
        ApiTestContext.lastResponse(changeStatus(TestDataFactory.admin(), documentAlias, status));
    }

    static EcmApiClient.ApiResponse changeStatus(EcmApiClient.Session as, String documentAlias, String status) {
        return EcmApiClient.patch(as, "/v1/documents/" + ApiTestContext.id(documentAlias) + "/status",
                Map.of("status", status));
    }
}
