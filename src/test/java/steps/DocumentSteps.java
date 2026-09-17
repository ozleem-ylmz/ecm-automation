package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DocumentsPage;
import pages.LoginPage;
import utils.BrowserManager;
import com.microsoft.playwright.Download;

public class DocumentSteps {

    private DocumentsPage documentsPage;
    private String documentTitle;
    private Download downloadedFile;

    // =========================
    // HELPER METHODS
    // =========================

    private void adminLogin() {

        BrowserManager.startBrowser();

        LoginPage loginPage =
                new LoginPage(BrowserManager.page);

        loginPage.loginSayfasiniAc();
        loginPage.emailGir("admin@local");
        loginPage.sifreGir("123");
        loginPage.girisYapButonunaTikla();

        if (!loginPage.klasorlerSayfasindaMi()) {
            throw new AssertionError(
                    "Admin kullanıcı ECM sistemine giriş yapamadı!"
            );
        }

        documentsPage =
                new DocumentsPage(BrowserManager.page);
    }

    private void documentsSayfasinaGit() {

        documentsPage.documentsSayfasiniAc();

        if (!documentsPage.documentsSayfasindaMi()) {
            throw new AssertionError(
                    "Documents sayfası açılamadı!"
            );
        }
    }

    private void uploadWithoutClassSayfasinaGit() {

        documentsSayfasinaGit();

        documentsPage.uploadWithoutClassSayfasiniAc();

        if (!documentsPage.uploadWithoutClassSayfasindaMi()) {
            throw new AssertionError(
                    "Upload without class sayfası açılamadı!"
            );
        }
    }

    private void benzersizBelgeYukle() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeYukle(documentTitle);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Belge yüklendikten sonra detay sayfası açılmadı!"
            );
        }
    }

    // =========================
    // GIVEN
    // =========================

    @Given("admin kullanıcı Documents sayfasındadır")
    public void adminKullaniciDocumentsSayfasindadir() {

        adminLogin();
        documentsSayfasinaGit();
    }

    @Given("admin kullanıcı Upload without class sayfasındadır")
    public void adminKullaniciUploadWithoutClassSayfasindadir() {

        adminLogin();
        uploadWithoutClassSayfasinaGit();
    }

    @Given("admin kullanıcı benzersiz bir TXT belge yüklemiştir")
    public void adminKullaniciBenzersizBirTxtBelgeYuklemistir() {

        adminLogin();
        uploadWithoutClassSayfasinaGit();
        benzersizBelgeYukle();
    }

    // =========================
    // DOCUMENTS NAVIGATION
    // =========================

    @When("kullanıcı Documents sayfasını açar")
    public void kullaniciDocumentsSayfasiniAcar() {

        documentsPage =
                new DocumentsPage(BrowserManager.page);

        documentsSayfasinaGit();
    }

    @When("kullanıcı Documents sayfasına döner")
    public void kullaniciDocumentsSayfasinaDoner() {

        documentsPage.documentsSayfasiniAc();
    }

    @Then("Documents sayfası görüntülenmelidir")
    public void documentsSayfasiGoruntulenmelidir() {

        if (!documentsPage.documentsSayfasindaMi()) {
            throw new AssertionError(
                    "Documents sayfası görüntülenmedi!"
            );
        }
    }

    // =========================
    // UPLOAD WITHOUT CLASS
    // =========================

    @When("kullanıcı Upload without class butonuna tıklar")
    public void kullaniciUploadWithoutClassButonunaTiklar() {

        documentsPage.uploadWithoutClassSayfasiniAc();
    }

    @Then("Upload without class sayfası görüntülenmelidir")
    public void uploadWithoutClassSayfasiGoruntulenmelidir() {

        if (!documentsPage.uploadWithoutClassSayfasindaMi()) {
            throw new AssertionError(
                    "Upload without class sayfası görüntülenmedi!"
            );
        }
    }

    // =========================
    // DOCUMENT UPLOAD
    // =========================

    @When("kullanıcı benzersiz başlıkla TXT belge yükler")
    public void kullaniciBenzersizBasliklaTxtBelgeYukler() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeYukle(documentTitle);
    }

    @Then("yüklenen belgenin detay sayfası görüntülenmelidir")
    public void yuklenenBelgeninDetaySayfasiGoruntulenmelidir() {

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Yüklenen belgenin detay sayfası görüntülenmedi!"
            );
        }
    }

    @Then("yüklenen belgenin başlığı detay sayfasında görüntülenmelidir")
    public void yuklenenBelgeninBasligiDetaySayfasindaGoruntulenmelidir() {

        if (!documentsPage.belgeBasligiGorunuyorMu(documentTitle)) {
            throw new AssertionError(
                    "Belge başlığı detay sayfasında görüntülenmedi: "
                            + documentTitle
            );
        }
    }

    @Then("yüklenen belge Documents listesinde görüntülenmelidir")
    public void yuklenenBelgeDocumentsListesindeGoruntulenmelidir() {

        if (!documentsPage.belgeListedeGorunuyorMu(documentTitle)) {
            throw new AssertionError(
                    "Yüklenen belge Documents listesinde bulunamadı: "
                            + documentTitle
            );
        }
    }

    // =========================
    // FORM VALIDATION
    // =========================

    @When("kullanıcı dosya seçer ancak başlık girmez")
    public void kullaniciDosyaSecerAncakBaslikGirmez() {

        documentsPage.testDosyasiSec();
    }

    @When("kullanıcı belge başlığı girer ancak dosya seçmez")
    public void kullaniciBelgeBasligiGirerAncakDosyaSecmez() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeBasligiGir(documentTitle);
    }

    @When("kullanıcı benzersiz belge başlığı girer")
    public void kullaniciBenzersizBelgeBasligiGirer() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeBasligiGir(documentTitle);
    }

    @When("kullanıcı Upload document butonuna tıklar")
    public void kullaniciUploadDocumentButonunaTiklar() {

        documentsPage.uploadDocumentButonunaTikla();
    }

    @Then("Upload document butonu disabled olmalıdır")
    public void uploadDocumentButonuDisabledOlmalidir() {

        if (!documentsPage.uploadDocumentButonuDisabledMi()) {
            throw new AssertionError(
                    "Upload document butonu disabled değil!"
            );
        }
    }

    @Then("kullanıcı Upload without class sayfasında kalmalıdır")
    public void kullaniciUploadWithoutClassSayfasindaKalmalidir() {

        if (!documentsPage.uploadWithoutClassSayfasindaMi()) {
            throw new AssertionError(
                    "Upload without class sayfasında kalınmadı!"
            );
        }
    }

    // =========================
    // SOFT DELETE
    // =========================

    @When("kullanıcı belgeyi soft delete yapar")
    public void kullaniciBelgeyiSoftDeleteYapar() {

        documentsPage.belgeyiSoftDeleteYap();
    }

    @Then("belge Deleted durumunda görüntülenmelidir")
    public void belgeDeletedDurumundaGoruntulenmelidir() {

        if (!documentsPage.belgeDeletedMi(documentTitle)) {
            throw new AssertionError(
                    "Belge soft delete sonrasında Deleted durumunda görüntülenmedi: "
                            + documentTitle
            );
        }
    }

    // =========================
    // RESTORE
    // =========================

    @When("kullanıcı belgeyi restore eder")
    public void kullaniciBelgeyiRestoreEder() {

        documentsPage.belgeyiRestoreEt(documentTitle);
    }

    @Then("belge artık Deleted durumunda olmamalıdır")
    public void belgeArtikDeletedDurumundaOlmamalidir() {

        if (!documentsPage.belgeDeletedDegilMi()) {
            throw new AssertionError(
                    "Restore sonrasında belge hâlâ Deleted durumunda: "
                            + documentTitle
            );
        }
    }

    @Then("belge detay sayfası aktif olarak görüntülenmelidir")
    public void belgeDetaySayfasiAktifOlarakGoruntulenmelidir() {

        if (!documentsPage.belgeDetaySayfasiAktifMi()) {
            throw new AssertionError(
                    "Restore sonrasında belge detay sayfası aktif değil!"
            );
        }
    }

    // =========================
    // DOCUMENT LIST -> DETAIL
    // =========================

    @When("kullanıcı yüklenen belgeyi listeden açar")
    public void kullaniciYuklenenBelgeyiListedenAcar() {

        documentsPage.belgeyiListedenAc(documentTitle);
    }

    // =========================
    // DOCUMENT STATUS
    // =========================

    @Then("belgenin status bilgisi görüntülenmelidir")
    public void belgeninStatusBilgisiGoruntulenmelidir() {

        if (!documentsPage.statusBilgisiGorunuyorMu()) {
            throw new AssertionError(
                    "Belgenin status bilgisi görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı belgenin status bilgisini değiştirir")
    public void kullaniciBelgeninStatusBilgisiniDegistirir() {

        documentsPage.statusuUnderReviewYap();
    }

    @Then("belgenin yeni status bilgisi görüntülenmelidir")
    public void belgeninYeniStatusBilgisiGoruntulenmelidir() {

        if (!documentsPage.statusUnderReviewMu()) {
            throw new AssertionError(
                    "Belgenin yeni status bilgisi Under review olarak görüntülenmedi!"
            );
        }
    }
    // =========================
// DOCUMENT DOWNLOAD
// =========================

    @When("kullanıcı belgeyi indirir")
    public void kullaniciBelgeyiIndirir() {

        downloadedFile = documentsPage.belgeyiIndir();
    }

    @Then("belge dosyası başarıyla indirilmelidir")
    public void belgeDosyasiBasariylaIndirilmelidir() {

        if (!documentsPage.downloadBasariliMi(downloadedFile)) {
            throw new AssertionError(
                    "Belge dosyası başarıyla indirilemedi!"
            );
        }
    }
    // =========================
// DOCUMENT PREVIEW
// =========================

    @Then("belge önizlemesi görüntülenmelidir")
    public void belgeOnizlemesiGoruntulenmelidir() {

        if (!documentsPage.txtPreviewGorunuyorMu()) {
            throw new AssertionError(
                    "TXT belgenin önizlemesi görüntülenmedi!"
            );
        }
    }
    // =========================
// DOCUMENT NEW VERSION
// =========================

    @When("kullanıcı yeni versiyon sayfasını açar")
    public void kullaniciYeniVersiyonSayfasiniAcar() {

        documentsPage.yeniVersiyonSayfasiniAc();
    }

    @When("kullanıcı {string} versiyon tipini seçer")
    public void kullaniciVersiyonTipiniSecer(
            String versionType
    ) {

        documentsPage.versionTipiniSec(versionType);
    }

    @When("kullanıcı yeni versiyon dosyasını yükler")
    public void kullaniciYeniVersiyonDosyasiniYukler() {

        documentsPage.yeniVersiyonDosyasiniSec();
    }

    @When("kullanıcı yeni versiyonu kaydeder")
    public void kullaniciYeniVersiyonuKaydeder() {

        documentsPage.yeniVersiyonuKaydet();
    }

    @Then("belge versiyonu {string} olmalıdır")
    public void belgeVersiyonuOlmalidir(
            String expectedVersion
    ) {

        if (!documentsPage.belgeVersiyonuMu(expectedVersion)) {

            throw new AssertionError(
                    "Beklenen belge versiyonu görüntülenmedi: "
                            + expectedVersion
            );
        }
    }
}