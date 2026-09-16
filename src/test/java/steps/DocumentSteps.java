package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DocumentsPage;
import pages.LoginPage;
import utils.BrowserManager;

public class DocumentSteps {

    private DocumentsPage documentsPage;
    private String documentTitle;

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

    @When("kullanıcı Documents sayfasını açar")
    public void kullaniciDocumentsSayfasiniAcar() {

        documentsPage =
                new DocumentsPage(BrowserManager.page);

        documentsSayfasinaGit();

    }

    @When("kullanıcı Upload without class butonuna tıklar")
    public void kullaniciUploadWithoutClassButonunaTiklar() {

        documentsPage.uploadWithoutClassSayfasiniAc();
    }

    @When("kullanıcı benzersiz başlıkla TXT belge yükler")
    public void kullaniciBenzersizBasliklaTxtBelgeYukler() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeYukle(documentTitle);
    }

    @When("kullanıcı Documents sayfasına döner")
    public void kullaniciDocumentsSayfasinaDoner() {

        documentsPage.documentsSayfasiniAc();
    }

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

    @Then("Documents sayfası görüntülenmelidir")
    public void documentsSayfasiGoruntulenmelidir() {

        if (!documentsPage.documentsSayfasindaMi()) {
            throw new AssertionError(
                    "Documents sayfası görüntülenmedi!"
            );
        }
    }

    @Then("Upload without class sayfası görüntülenmelidir")
    public void uploadWithoutClassSayfasiGoruntulenmelidir() {

        if (!documentsPage.uploadWithoutClassSayfasindaMi()) {
            throw new AssertionError(
                    "Upload without class sayfası görüntülenmedi!"
            );
        }
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

    @Then("Upload document butonu disabled olmalıdır")
    public void uploadDocumentButonuDisabledOlmalidir() {

        if (!documentsPage.uploadDocumentButonuDisabledMi()) {
            throw new AssertionError(
                    "Upload document butonu disabled değil!"
            );
        }
    }
    @When("kullanıcı Upload document butonuna tıklar")
    public void kullaniciUploadDocumentButonunaTiklar() {

        documentsPage.uploadDocumentButonunaTikla();
    }

    @Then("kullanıcı Upload without class sayfasında kalmalıdır")
    public void kullaniciUploadWithoutClassSayfasindaKalmalidir() {

        if (!documentsPage.uploadWithoutClassSayfasindaMi()) {
            throw new AssertionError(
                    "Başlık boş olmasına rağmen Upload without class sayfasından çıkıldı!"
            );
        }
    }

    @When("kullanıcı benzersiz belge başlığı girer")
    public void kullaniciBenzersizBelgeBasligiGirer() {

        documentTitle =
                "QA Document " + System.currentTimeMillis();

        documentsPage.belgeBasligiGir(documentTitle);
    }
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

}