package steps;

import com.microsoft.playwright.Response;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DocumentsPage;
import pages.LoginPage;
import pages.StorageGcPage;
import utils.BrowserManager;

public class StorageGcSteps {

    private StorageGcPage storageGcPage;
    private Response lastStorageGcResponse;
    private Response lastVersionDeleteResponse;
    private DocumentsPage documentsPage;
    private String storageGcTestDocumentTitle;

    private void adminLogin() {
        BrowserManager.startBrowser();

        LoginPage loginPage = new LoginPage(BrowserManager.page);

        loginPage.loginSayfasiniAc();
        loginPage.emailGir("admin@local");
        loginPage.sifreGir("123");
        loginPage.girisYapButonunaTikla();

        if (!loginPage.klasorlerSayfasindaMi()) {
            throw new AssertionError(
                    "Admin kullanıcı ECM sistemine giriş yapamadı!"
            );
        }

        storageGcPage = new StorageGcPage(BrowserManager.page);
        documentsPage = new DocumentsPage(BrowserManager.page);
    }

    @Given("admin kullanıcı Storage GC sayfasındadır")
    public void adminKullaniciStorageGcSayfasindadir() {
        adminLogin();

        storageGcPage.sayfayiAc();

        if (!storageGcPage.baslikGorunuyorMu()) {
            throw new AssertionError(
                    "Storage Garbage Collector sayfası görüntülenemedi!"
            );
        }
    }

    @Then("Dry run seçeneği varsayılan olarak açık olmalıdır")
    public void dryRunSecenegiVarsayilanOlarakAcikOlmalidir() {
        if (!storageGcPage.dryRunSeciliMi()) {
            throw new AssertionError(
                    "Dry run varsayılan olarak açık değil!"
            );
        }
    }

    @Then("Storage GC Preview dry run butonu görüntülenmelidir")
    public void storageGcPreviewDryRunButonuGoruntulenmelidir() {
        if (!storageGcPage.previewButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Preview (dry run) butonu görüntülenmiyor!"
            );
        }
    }

    @When("admin Storage GC preview işlemini çalıştırır")
    public void adminStorageGcPreviewIsleminiCalistirir() {
        lastStorageGcResponse = storageGcPage.previewCalistir();
    }

    @Then("Storage GC isteği dry run true ile gönderilmelidir")
    public void storageGcIstegiDryRunTrueIleGonderilmelidir() {
        if (!storageGcPage.requestDryRunTrueMu(lastStorageGcResponse)) {
            throw new AssertionError(
                    "Storage GC isteği dry_run=true ile gönderilmedi!"
            );
        }
    }

    @Then("Storage GC preview isteği başarılı olmalıdır")
    public void storageGcPreviewIstegiBasariliOlmalidir() {
        if (!storageGcPage.responseBasariliMi(lastStorageGcResponse)) {
            int status = lastStorageGcResponse == null
                    ? -1
                    : lastStorageGcResponse.status();

            throw new AssertionError(
                    "Storage GC preview isteği başarısız. HTTP status: "
                            + status
            );
        }
    }

    @Then("Storage GC sonuç alanı görüntülenmelidir")
    public void storageGcSonucAlaniGoruntulenmelidir() {
        if (!storageGcPage.sonucAlaniGorunuyorMu()) {
            throw new AssertionError(
                    "Storage GC sonuç kartları görüntülenmedi!"
            );
        }
    }

    @When("admin listeden ilk sürümü seçer")
    public void adminListedenIlkSurumuSecer() {
        storageGcPage.ilkSurumuSec();

        if (!storageGcPage.ilkSurumSeciliMi()) {
            throw new AssertionError(
                    "Storage GC listesindeki ilk sürüm seçilemedi!"
            );
        }
    }

    @When("admin seçili sürümü silmeyi dener")
    public void adminSeciliSurumuSilmeyiDener() {
        storageGcPage.versionDeleteRequestSayaciniSifirla();
        storageGcPage.deleteSelectedButonunaTikla();
    }

    @Then("ilk Storage GC silme onayı görüntülenmelidir")
    public void ilkStorageGcSilmeOnayiGoruntulenmelidir() {
        if (!storageGcPage.ilkOnayPenceresiGorunuyorMu()) {
            throw new AssertionError(
                    "lk Storage GC silme onayı görüntülenmedi!"
            );
        }
    }

    @When("admin ilk Storage GC silme onayını iptal eder")
    public void adminIlkStorageGcSilmeOnayiniIptalEder() {
        storageGcPage.ilkOnaydaCancelTikla();
    }

    @When("admin ilk Storage GC silme onayında devam eder")
    public void adminIlkStorageGcSilmeOnayindaDevamEder() {
        storageGcPage.ilkOnaydaContinueTikla();
    }

    @Then("ikinci Storage GC silme onayı görüntülenmelidir")
    public void ikinciStorageGcSilmeOnayiGoruntulenmelidir() {
        if (!storageGcPage.ikinciOnayPenceresiGorunuyorMu()) {
            throw new AssertionError(
                    "kinci Storage GC silme onayı görüntülenmedi!"
            );
        }
    }

    @When("admin ikinci Storage GC silme onayını iptal eder")
    public void adminIkinciStorageGcSilmeOnayiniIptalEder() {
        storageGcPage.ikinciOnaydaCancelTikla();
    }

    @Then("Storage GC sürüm silme isteği gönderilmemelidir")
    public void storageGcSurumSilmeIstegiGonderilmemelidir() {
        if (!storageGcPage.versionDeleteRequestGonderilmediMi()) {
            throw new AssertionError(
                    "Cancel sonrasında /storage-gc/versions/delete isteği gönderildi!"
            );
        }
    }

    @Then("seçilen Storage GC sürümü seçili kalmalıdır")
    public void secilenStorageGcSurumuSeciliKalmalidir() {
        if (!storageGcPage.ilkSurumSeciliMi()) {
            throw new AssertionError(
                    "Cancel sonrasında seçilen sürüm beklenmedik şekilde seçimden çıktı!"
            );
        }
    }

    @Given("admin kullanıcının Storage GC için test belgesi vardır")
    public void adminKullanicininStorageGcIcinTestBelgesiVardir() {
        adminLogin();

        storageGcTestDocumentTitle =
                "storage-gc-e2e-" + System.currentTimeMillis();

        BrowserManager.page.navigate(
                BrowserManager.getBaseUrl() + "/documents"
        );

        documentsPage.uploadWithoutClassSayfasiniAc();
        documentsPage.belgeYukle(storageGcTestDocumentTitle);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Storage GC test belgesi oluşturulamadı!"
            );
        }

        storageGcPage.sayfayiAc();

        storageGcPage.testBelgesiSurumuOlanaKadarBekle(
                storageGcTestDocumentTitle
        );
    }

    @When("admin test belgesinin sürümünü seçer")
    public void adminTestBelgesininSurumunuSecer() {
        storageGcPage.testBelgesininSurumunuSec(
                storageGcTestDocumentTitle
        );
    }

    @When("admin iki onayı da vererek seçili sürümü siler")
    public void adminIkiOnayiDaVererekSeciliSurumuSiler() {
        lastVersionDeleteResponse =
                storageGcPage.seciliSurumuIkiOnaylaSil();
    }

    @Then("Storage GC gerçek sürüm silme isteği başarılı olmalıdır")
    public void storageGcGercekSurumSilmeIstegiBasariliOlmalidir() {
        if (!storageGcPage.responseBasariliMi(lastVersionDeleteResponse)) {
            int status = lastVersionDeleteResponse == null
                    ? -1
                    : lastVersionDeleteResponse.status();

            throw new AssertionError(
                    "Storage GC version delete başarısız. HTTP status: "
                            + status
            );
        }

        if (!storageGcPage.versionDeleteRequestBirKezGonderildiMi()) {
            throw new AssertionError(
                    "/storage-gc/versions/delete isteği tam bir kez gönderilmedi!"
            );
        }
    }

    @Then("Storage GC test belgesi artık sürüm listesinde görünmemelidir")
    public void storageGcTestBelgesiArtikSurumListesindeGorunmemelidir() {
        storageGcPage.testBelgesiSilineneKadarBekle(
                storageGcTestDocumentTitle
        );

        if (!storageGcPage.testBelgesiArtikYokMu(
                storageGcTestDocumentTitle)) {
            throw new AssertionError(
                    "Hard-delete edilen test belgesi Storage GC listesinde hâlâ görünüyor!"
            );
        }
    }
}