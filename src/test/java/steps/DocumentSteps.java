package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DocumentsPage;
import pages.LoginPage;
import utils.BrowserManager;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;


public class DocumentSteps {

    private DocumentsPage documentsPage;
    private String documentTitle;
    private Download downloadedFile;
    private Page managerPage;
    private String documentDetailUrl;
    private String savedDocumentTitle;
    private String savedDocumentVersion;
    private String batch12TargetFolder;
    private String batch12DetailUrl;
    private String batch13SourceTitle;
    private String batch13SourceUrl;
    private String batch13CopyTitle;
    private String batch13CopyUrl;
    private String batch13TargetFolder;
    private String batch14Label;
    private String batch14SecondLabel;
    private String batch14Version = "v1.0.0";
    private String batch15OriginalTitle;
    private String batch15RenamedTitle;
    private String batch15DocumentUrl;
    private String batch16OriginalTitle;
    private String batch16RenamedTitle;
    private String batch16FirstRenamedTitle;
    private String batch16SecondRenamedTitle;
    private String batch17OriginalTitle;
    private String batch17VersionComment;
    private String batch17ExpectedVersion = "v1.0.1";

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

        String activeTitle = batch13CopyTitle != null ? batch13CopyTitle : documentTitle;

        if (!documentsPage.belgeDeletedMi(activeTitle)) {
            throw new AssertionError(
                    "Belge soft delete sonrasında Deleted durumunda görüntülenmedi: "
                            + activeTitle
            );
        }
    }

    // =========================
    // RESTORE
    // =========================

    @When("kullanıcı belgeyi restore eder")
    public void kullaniciBelgeyiRestoreEder() {

        String activeTitle = batch13CopyTitle != null ? batch13CopyTitle : documentTitle;
        documentsPage.belgeyiRestoreEt(activeTitle);
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
    // =========================
// DOCUMENT CHECKOUT / LOCK
// =========================

    @Then("belge checkout durumda olmamalıdır")
    public void belgeCheckoutDurumundaOlmamalidir() {

        if (!documentsPage.checkoutDurumundaDegilMi()) {
            throw new AssertionError(
                    "Belge checkout yapılmamış durumda değil!"
            );
        }
    }

    @When("kullanıcı belgeyi checkout yapar")
    public void kullaniciBelgeyiCheckoutYapar() {

        documentsPage.belgeyiCheckoutYap();
    }

    @Then("belge checkout durumda olmalıdır")
    public void belgeCheckoutDurumundaOlmalidir() {

        if (!documentsPage.checkoutDurumundaMi()) {
            throw new AssertionError(
                    "Belge checkout durumuna geçmedi!"
            );
        }
    }

    @Then("belge checkout durumunda olmalıdır")
    public void belgeCheckoutDurumundaOlmalidirBatch12() {
        if (!documentsPage.checkoutDurumundaMi()) {
            throw new AssertionError("Belge checkout durumuna geçmedi!");
        }
    }

    @Then("belge checkout durumunda olmamalıdır")
    public void belgeCheckoutDurumundaOlmamalidirBatch12() {
        if (!documentsPage.checkoutDurumundaDegilMi()) {
            throw new AssertionError("Belge hâlâ checkout durumunda!");
        }
    }

    @Then("Check in butonu görüntülenmelidir")
    public void checkInButonuGoruntulenmelidir() {

        if (!documentsPage.checkInButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Check in butonu görüntülenmedi!"
            );
        }
    }
    @When("kullanıcı belgeyi check in yapar")
    public void kullaniciBelgeyiCheckInYapar() {

        documentsPage.belgeyiCheckInYap();
    }

    @Then("Check out butonu görüntülenmelidir")
    public void checkOutButonuGoruntulenmelidir() {

        if (!documentsPage.checkOutButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Check out butonu görüntülenmedi!"
            );
        }
    }
    @Then("Not checked out bilgisi görüntülenmemelidir")
    public void notCheckedOutBilgisiGoruntulenmemelidir() {

        if (!documentsPage.notCheckedOutGorunmuyorMu()) {
            throw new AssertionError(
                    "Checkout sonrasında Not checked out bilgisi hâlâ görüntüleniyor!"
            );
        }
    }

    @Then("Check out butonu görüntülenmemelidir")
    public void checkOutButonuGoruntulenmemelidir() {

        if (!documentsPage.checkOutButonuGorunmuyorMu()) {
            throw new AssertionError(
                    "Checkout sonrasında Check out butonu hâlâ görüntüleniyor!"
            );
        }
    }

    @Then("{string} bilgisi görüntülenmelidir")
    public void bilgiGoruntulenmelidir(String expectedText) {

        if (expectedText.equals("Checked out by you")) {

            if (!documentsPage.checkedOutByYouGorunuyorMu()) {
                throw new AssertionError(
                        "Checked out by you bilgisi görüntülenmedi!"
                );
            }
        }
    }
    // =========================
    // MULTI-USER DOCUMENT LOCK
    // =========================

    @Given("admin kullanıcı bir belgeyi checkout yapmıştır")
    public void adminKullaniciBirBelgeyiCheckoutYapmistir() {

        adminLogin();
        uploadWithoutClassSayfasinaGit();
        benzersizBelgeYukle();

        // Admin'in oluşturduğu belgenin gerçek UUID'li URL'sini sakla
        documentDetailUrl = BrowserManager.page.url();

        documentsPage.belgeyiCheckoutYap();

        if (!documentsPage.checkoutDurumundaMi()) {
            throw new AssertionError(
                    "Admin kullanıcı belgeyi checkout yapamadı!"
            );
        }
    }

    @When("manager kullanıcı aynı belgeyi açar")
    public void managerKullaniciAyniBelgeyiAcar() {

        managerPage = BrowserManager.createManagerPage();

        documentsPage.managerLogin(managerPage);

        managerPage.navigate(documentDetailUrl);

        managerPage.waitForURL(
                documentDetailUrl,
                new Page.WaitForURLOptions()
                        .setTimeout(15000)
        );
    }

    @Then("belge {string} olarak görüntülenmelidir")
    public void belgeLockBilgisiOlarakGoruntulenmelidir(
            String expectedLock
    ) {

        if (expectedLock.equals("Checked out by Admin")
                && !documentsPage.checkedOutByAdminGorunuyorMu(
                managerPage
        )) {

            throw new AssertionError(
                    "Manager belgeyi Checked out by Admin olarak görmedi!"
            );
        }
    }

    @Then("belge read-only olarak görüntülenmelidir")
    public void belgeReadOnlyOlarakGoruntulenmelidir() {

        if (!documentsPage.readOnlyGorunuyorMu(managerPage)) {
            throw new AssertionError(
                    "Manager belgeyi read-only olarak görmedi!"
            );
        }
    }

    @Then("New version butonu görüntülenmemelidir")
    public void newVersionButonuGoruntulenmemelidir() {

        if (!documentsPage.newVersionButonuGorunmuyorMu(managerPage)) {
            throw new AssertionError(
                    "Manager için New version butonu görüntüleniyor!"
            );
        }
    }

    @Then("Download butonu görüntülenmelidir")
    public void downloadButonuGoruntulenmelidir() {

        Page targetPage = managerPage != null ? managerPage : BrowserManager.page;

        if (!documentsPage.downloadButonuGorunuyorMu(targetPage)) {
            throw new AssertionError(
                    "Download butonu görüntülenmedi!"
            );
        }
    }

    @Then("Preview butonu görüntülenmelidir")
    public void previewButonuGoruntulenmelidir() {

        if (!documentsPage.previewButonuGorunuyorMu(managerPage)) {
            throw new AssertionError(
                    "Manager için Preview butonu görüntülenmedi!"
            );
        }

    }
    @Then("admin için Preview butonu görüntülenmelidir")
    public void adminIcinPreviewButonuGoruntulenmelidir() {

        if (!documentsPage.previewButonuGorunuyorMu(BrowserManager.page)) {
            throw new AssertionError(
                    "Admin için Preview butonu görüntülenmedi!"
            );
        }
    }

    // =========================
// CRITICAL LOCK STEPS
// =========================

    @Then("Move butonu görüntülenmemelidir")
    public void moveButonuGoruntulenmemelidir() {

        Page targetPage = managerPage != null ? managerPage : BrowserManager.page;

        if (!documentsPage.moveButonuGorunmuyorMu(targetPage)) {
            throw new AssertionError(
                    "Move butonu görüntülenmemesi gerekirken görüntüleniyor!"
            );
        }
    }

    @Then("Classify butonu görüntülenmemelidir")
    public void classifyButonuGoruntulenmemelidir() {

        Page targetPage = managerPage != null ? managerPage : BrowserManager.page;

        if (!documentsPage.classifyButonuGorunmuyorMu(targetPage)) {
            throw new AssertionError(
                    "Classify butonu görüntülenmemesi gerekirken görüntüleniyor!"
            );
        }
    }

    @Then("tag değiştirme işlemi kullanılamamalıdır")
    public void tagDegistirmeIslemiKullanilamamali() {

        if (!documentsPage.tagDegistirmeKullanilamiyorMu(managerPage)) {
            throw new AssertionError(
                    "BUG ADAYI: Manager kilitli belgenin tag bilgisini değiştirebiliyor!"
            );
        }
    }

    @When("admin belge detay sayfasını yeniler")
    public void adminBelgeDetaySayfasiniYeniler() {
        documentsPage.belgeDetaySayfasiniYenile();
    }

    @Then("New version butonu görüntülenmelidir")
    public void newVersionButonuGoruntulenmelidir() {

        if (!documentsPage.newVersionButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Check-in sonrasında New version butonu geri gelmedi!"
            );
        }
    }

    @When("admin kullanıcı belgeyi checkout yapar")
    public void adminKullaniciBelgeyiCheckoutYapar() {
        documentsPage.belgeyiCheckoutYap();

        // Manager'ın daha sonra aynı belgeyi doğrudan açabilmesi için
        // gerçek document detail URL'sini saklıyoruz.
        documentDetailUrl = BrowserManager.page.url();
    }

    @Then("manager kullanıcı belgeyi checkout yapamamalıdır")
    public void managerKullaniciBelgeyiCheckoutYapamamali() {

        if (documentsPage.checkOutButonuGorunuyorMu(managerPage)) {
            throw new AssertionError(
                    "BUG ADAYI: Manager admin tarafından kilitlenmiş belgeyi checkout yapabiliyor!"
            );
        }
    }
    // =========================
// DELETE / RESTORE INTEGRITY STEPS
// =========================

    @Then("Delete butonu görüntülenmemelidir")
    public void deleteButonuGoruntulenmemelidir() {

        if (!documentsPage.deleteButonuGorunmuyorMu()) {
            throw new AssertionError(
                    "BUG ADAYI: Checkout edilmiş belgede Delete butonu görüntüleniyor!"
            );
        }
    }

    @Then("Delete butonu görüntülenmelidir")
    public void deleteButonuGoruntulenmelidir() {

        if (!documentsPage.deleteButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Check-in sonrasında Delete butonu görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı checkout edilmiş belgeyi silmeyi dener")
    public void kullaniciCheckoutEdilmisBelgeyiSilmeyiDener() {

        documentsPage.checkoutEdilmisBelgeyiSilmeyiDene();
    }

    @Then("checkout edilmiş belge silinmemelidir")
    public void checkoutEdilmisBelgeSilinmemelidir() {

        if (!documentsPage.checkoutEdilmisBelgeHalaMevcutMu()) {
            throw new AssertionError(
                    "BUG ADAYI: Checkout edilmiş belge Delete işlemiyle silinebildi!"
            );
        }
    }

































































































    // =========================
// // BATCH 5 - STATUS / REFRESH
// // =========================

    @When("kullanıcı belge detay sayfasını yeniler")
    public void kullaniciBelgeDetaySayfasiniYeniler() {
        documentsPage.belgeDetaySayfasiniYenile();
    }

    @Then("belgenin statusu Under review olmalıdır")
    public void belgeninStatusuUnderReviewOlmalidir() {

        if (!documentsPage.statusUnderReviewGorunuyorMu()) {
            throw new AssertionError(
                    "Belgenin statusu Under review olarak korunmadı!"
            );
        }
    }

    @Then("belgenin statusu Draft olmalıdır")
    public void belgeninStatusuDraftOlmalidir() {

        if (!documentsPage.statusDraftMi()) {
            throw new AssertionError(
                    "Belgenin statusu Draft olarak görüntülenmedi!"
            );
        }
    }

    @Then("soft delete edilen belgenin statusu Under review olmalıdır")
    public void softDeleteEdilenBelgeninStatusuUnderReviewOlmalidir() {

        if (!documentsPage.deletedBelgedeStatusUnderReviewGorunuyorMu()) {
            throw new AssertionError(
                    "Soft delete sonrasında belgenin Under review statusu korunmadı!"
            );
        }
    }


    // =========================
// BATCH 6 - HARD DELETE
// =========================

    @When("kullanıcı Hard delete işlemini açar")
    public void kullaniciHardDeleteIsleminiAcar() {
        documentsPage.hardDeletePenceresiniAc();
    }

    @Then("hard delete onay penceresi görüntülenmelidir")
    public void hardDeleteOnayPenceresiGoruntulenmelidir() {
        if (!documentsPage.hardDeletePenceresiGorunuyorMu()) {
            throw new AssertionError(
                    "Hard delete onay penceresi görüntülenmedi!"
            );
        }
    }

    @Then("Permanently delete butonu disabled olmalıdır")
    public void permanentlyDeleteButonuDisabledOlmalidir() {
        if (!documentsPage.permanentlyDeleteButonuDisabledMi()) {
            throw new AssertionError(
                    "GÜVENLİK BUG ADAYI: DELETE yazılmadan Permanently delete butonu aktif!"
            );
        }
    }

    @When("kullanıcı hard delete onay alanına {string} yazar")
    public void kullaniciHardDeleteOnayAlaninaYazar(String confirmation) {
        documentsPage.hardDeleteOnayMetniGir(confirmation);
    }

    @Then("Permanently delete butonu aktif olmalıdır")
    public void permanentlyDeleteButonuAktifOlmalidir() {
        if (!documentsPage.permanentlyDeleteButonuAktifMi()) {
            throw new AssertionError(
                    "DELETE yazılmasına rağmen Permanently delete butonu aktif olmadı!"
            );
        }
    }

    @When("kullanıcı belgeyi kalıcı olarak siler")
    public void kullaniciBelgeyiKaliciOlarakSiler() {
        documentsPage.permanentlyDeleteYap();
    }

    @Then("hard delete edilen belge detay sayfası görüntülenmemelidir")
    public void hardDeleteEdilenBelgeDetaySayfasiGoruntulenmemelidir() {
        if (!documentsPage.hardDeleteEdilmisBelgeDetaydaGorunmuyorMu()) {
            throw new AssertionError(
                    "KRİTİK DATA BUG ADAYI: Hard delete edilen belge eski detay URL üzerinden hâlâ erişilebilir!"
            );
        }
    }
    @When("kullanıcı silinen belgeyi tekrar açar")
    public void kullaniciSilinenBelgeyiTekrarAcar() {

        if (documentDetailUrl == null || documentDetailUrl.isBlank()) {
            throw new AssertionError(
                    "Silinen belge tekrar açılamadı: belge detay URL'si kaydedilmemiş!"
            );
        }

        BrowserManager.page.navigate(documentDetailUrl);
        BrowserManager.page.waitForLoadState();
    }

    // =========================
    // BATCH 8 - STATE BUG HUNT
    // =========================

    @Then("silinen belgede New version butonu görüntülenmemelidir")
    public void silinenBelgedeNewVersionButonuGoruntulenmemelidir() {

        if (documentsPage.newVersionButonuGorunuyorMu()) {
            throw new AssertionError(
                    "STATE BUG ADAYI: Soft delete edilmiş belgede New version butonu görüntüleniyor!"
            );
        }
    }

    @Then("Hard delete butonu görüntülenmemelidir")
    public void hardDeleteButonuGoruntulenmemelidir() {

        if (documentsPage.hardDeleteButonuGorunuyorMu()) {
            throw new AssertionError(
                    "STATE BUG ADAYI: Restore edilmiş aktif belgede Hard delete butonu görüntüleniyor!"
            );
        }
    }
    @Then("Hard delete butonu görüntülenmelidir")
    public void hardDeleteButonuGoruntulenmelidir() {

        if (!documentsPage.hardDeleteButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Restore sonrasında Hard delete butonu görüntülenmedi!"
            );
        }
    }
    // =====================================================
    // BATCH 9 - CHECKOUT LOCK / MULTI USER
    // =====================================================

    @Given("manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır")
    public void managerKullaniciAyriTarayiciOturumundaGirisYapmistir() {

        managerPage = BrowserManager.createManagerPage();
        documentsPage.managerLogin(managerPage);

        documentDetailUrl = BrowserManager.page.url();
    }


    @When("manager kullanıcı aynı belgeyi checkout yapmayı dener")
    public void managerKullaniciAyniBelgeyiCheckoutYapmayiDener() {

        managerPage.navigate(documentDetailUrl);
        managerPage.waitForLoadState();
    }


    @Then("manager checkout işlemi engellenmelidir")
    public void managerCheckoutIslemiEngellenmelidir() {

        if (documentsPage.checkOutButonuGorunuyorMu(managerPage)) {
            throw new AssertionError(
                    "LOCK BUG ADAYI: Manager kilitli belgeyi checkout yapabiliyor!"
            );
        }
    }


    @When("manager kullanıcı aynı belge için New version işlemini dener")
    public void managerKullaniciAyniBelgeIcinNewVersionIsleminiDener() {

        managerPage.navigate(documentDetailUrl);
        managerPage.waitForLoadState();
    }


    @Then("manager New version işlemi engellenmelidir")
    public void managerNewVersionIslemiEngellenmelidir() {

        if (!documentsPage.newVersionButonuGorunmuyorMu(managerPage)) {
            throw new AssertionError(
                    "LOCK BUG ADAYI: Manager kilitli belgede New version kullanabiliyor!"
            );
        }
    }


    @When("manager kullanıcı belgeyi yeniden adlandırmayı dener")
    public void managerKullaniciBelgeyiYenidenAdlandirmayiDener() {

        managerPage.navigate(documentDetailUrl);
        managerPage.waitForLoadState();
    }


    @Then("manager rename işlemi engellenmelidir")
    public void managerRenameIslemiEngellenmelidir() {

        if (!documentsPage.managerRenameYapamiyorMu(managerPage)) {
            throw new AssertionError(
                    "LOCK BUG ADAYI: Manager kilitli belgeyi yeniden adlandırabiliyor!"
            );
        }
    }


    @When("manager kullanıcı belge statusunu değiştirmeyi dener")
    public void managerKullaniciBelgeStatusunuDegistirmeyiDener() {

        managerPage.navigate(documentDetailUrl);
        managerPage.waitForLoadState();
    }


    @Then("manager status değişikliği engellenmelidir")
    public void managerStatusDegisikligiEngellenmelidir() {

        if (!documentsPage.managerStatusDegistiremiyorMu(managerPage)) {
            throw new AssertionError(
                    "LOCK BUG ADAYI: Manager kilitli belgenin statusunu değiştirebiliyor!"
            );
        }
    }


    @When("manager kullanıcı belgeyi soft delete yapmayı dener")
    public void managerKullaniciBelgeyiSoftDeleteYapmayiDener() {

        managerPage.navigate(documentDetailUrl);
        managerPage.waitForLoadState();
    }


    @Then("manager soft delete işlemi engellenmelidir")
    public void managerSoftDeleteIslemiEngellenmelidir() {

        if (!documentsPage.managerSoftDeleteYapamiyorMu(managerPage)) {
            throw new AssertionError(
                    "LOCK BUG ADAYI: Manager kilitli belgeyi soft delete yapabiliyor!"
            );
        }
    }


    @When("admin kullanıcı belgeyi check in yapar")
    public void adminKullaniciBelgeyiCheckInYapar() {

        documentsPage.belgeyiCheckInYap();
    }

    // =====================================================
    // RESTORE DATA INTEGRITY STEPS
    // =====================================================

    @Given("yüklenen belgenin başlığı kaydedilmiştir")
    public void yuklenenBelgeninBasligiKaydedilmistir() {
        savedDocumentTitle = documentsPage.mevcutBelgeBasliginiAl();
        if (savedDocumentTitle == null || savedDocumentTitle.isBlank()) {
            throw new AssertionError("Belge başlığı kaydedilemedi!");
        }
    }

    @Given("belgenin mevcut versiyonu kaydedilmiştir")
    public void belgeninMevcutVersiyonuKaydedilmistir() {
        savedDocumentVersion = documentsPage.mevcutBelgeVersiyonunuAl();
        if (savedDocumentVersion == null || savedDocumentVersion.isBlank()) {
            throw new AssertionError("Belgenin mevcut versiyonu kaydedilemedi!");
        }
    }

    @Then("belge başlığı değişmemelidir")
    public void belgeBasligiDegismemelidir() {
        if (!documentsPage.belgeBasligiEsitMi(savedDocumentTitle)) {
            throw new AssertionError("Belge başlığı değişti! Beklenen: " + savedDocumentTitle);
        }
    }

    @Then("belge versiyonu değişmemelidir")
    public void belgeVersiyonuDegismemelidir() {
        if (!documentsPage.belgeVersiyonuKorunduMu(savedDocumentVersion)) {
            throw new AssertionError("Belge versiyonu değişti! Beklenen: " + savedDocumentVersion);
        }
    }

    // =========================
    // DOCUMENT DETAIL URL STEPS
    // =========================

    @Given("belgenin detay adresi kaydedilmiştir")
    public void belgeninDetayAdresiKaydedilmistir() {
        documentDetailUrl = BrowserManager.page.url();
    }

    @When("kullanıcı eski belge detay adresini açar")
    public void kullaniciEskiBelgeDetayAdresiniAcar() {
        BrowserManager.page.navigate(documentDetailUrl);
        BrowserManager.page.waitForLoadState();
    }


// =====================================================
    // BATCH 11 - UI VERIFIED: CLASSIFY / MOVE / TAG
    // =====================================================

    @Then("Classify butonu görüntülenmelidir")
    public void classifyButonuGoruntulenmelidir() {
        if (!documentsPage.classifyButonuGorunuyorMu()) {
            throw new AssertionError("Classify butonu görüntülenmedi!");
        }
    }

    @When("kullanıcı Classify butonuna tıklar")
    public void kullaniciClassifyButonunaTiklar() {
        documentsPage.classifyButonunaTikla();
    }

    @Then("belge sınıflandırma ekranı görüntülenmelidir")
    public void belgeSiniflandirmaEkraniGoruntulenmelidir() {
        if (!documentsPage.classifyEkraniGorunuyorMu()) {
            throw new AssertionError("Belge sınıflandırma ekranı görüntülenmedi!");
        }
    }

    @When("kullanıcı sınıflandırma işlemini iptal eder")
    public void kullaniciSiniflandirmaIsleminiIptalEder() {
        documentsPage.classifyIsleminiIptalEt();
    }

    @Then("belge unclassified durumda olmalıdır")
    public void belgeUnclassifiedDurumdaOlmalidir() {
        if (!documentsPage.belgeUnclassifiedMi()) {
            throw new AssertionError("Belge Unclassified durumda görüntülenmedi!");
        }
    }

    @Then("Move işlemi görüntülenmelidir")
    public void moveIslemiGoruntulenmelidir() {
        if (!documentsPage.moveButonuGorunuyorMu()) {
            throw new AssertionError("Move to folder... linki görüntülenmedi!");
        }
    }

    @When("kullanıcı Move to folder bağlantısına tıklar")
    public void kullaniciMoveToFolderBaglantisinaTiklar() {
        documentsPage.moveButonunaTikla();
    }

    @Then("Move penceresi görüntülenmelidir")
    public void movePenceresiGoruntulenmelidir() {
        if (!documentsPage.movePenceresiGorunuyorMu()) {
            throw new AssertionError("Move penceresi görüntülenmedi!");
        }
    }

    @When("kullanıcı Move işlemini iptal eder")
    public void kullaniciMoveIsleminiIptalEder() {
        documentsPage.moveIsleminiIptalEt();
    }

    @Then("Tag giriş alanı görüntülenmelidir")
    public void tagGirisAlaniGoruntulenmelidir() {
        if (!documentsPage.tagGirisAlaniGorunuyorMu()) {
            throw new AssertionError("Type to add a tag... alanı görüntülenmedi!");
        }
    }

    @Then("Tag giriş alanı görüntülenmemelidir")
    public void tagGirisAlaniGoruntulenmemelidir() {
        Page targetPage = managerPage != null ? managerPage : BrowserManager.page;
        if (!documentsPage.tagGirisAlaniGorunmuyorMu(targetPage)) {
            throw new AssertionError("Tag giriş alanı görüntülenmemesi gerekirken görüntüleniyor!");
        }
    }

    @Then("belge detay sayfasında kalınmalıdır")
    public void belgeDetaySayfasindaKalinmalidir() {
        if (!documentsPage.belgeDetaySayfasindaKalindiMi()) {
            throw new AssertionError("Belge detay sayfasında kalınmadı!");
        }
    }

    @Then("Download butonu admin için görüntülenmelidir")
    public void downloadButonuAdminIcinGoruntulenmelidir() {
        if (!documentsPage.downloadButonuGorunuyorMu()) {
            throw new AssertionError("Download butonu görüntülenmedi!");
        }
    }



    @When("kullanıcı Move bağlantısına tıklar")
    public void kullaniciMoveBaglantisinaTiklar() {
        documentsPage.moveButonunaTikla();
    }

    // =========================
    // DOCUMENTS BATCH 12
    // Real Move execution / persistence
    // =========================

    @Given("benzersiz bir Move hedef klasörü oluşturulmuştur")
    public void benzersizBirMoveHedefKlasoruOlusturulmustur() {
        batch12DetailUrl = BrowserManager.page.url();
        batch12TargetFolder = "QA Move Folder " + System.currentTimeMillis();

        documentsPage.hedefKlasorOlustur(batch12TargetFolder);
        documentsPage.belgeDetayAdresiniAc(batch12DetailUrl);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError("Hedef klasör oluşturulduktan sonra belge detayına dönülemedi!");
        }
    }

    @When("kullanıcı Move penceresinde hedef klasörü seçer")
    public void kullaniciMovePenceresindeHedefKlasoruSecer() {
        documentsPage.moveHedefKlasorSec(batch12TargetFolder);
    }

    @Then("Move penceresinde hedef klasör seçili olmalıdır")
    public void movePenceresindeHedefKlasorSeciliOlmalidir() {
        if (!documentsPage.moveHedefKlasorSeciliMi(batch12TargetFolder)) {
            throw new AssertionError("Move hedef klasörü seçili değil: " + batch12TargetFolder);
        }
    }

    @When("kullanıcı Move işlemini onaylar")
    public void kullaniciMoveIsleminiOnaylar() {
        documentsPage.moveIsleminiOnayla();
    }

    @Then("Move penceresi kapanmalıdır")
    public void movePenceresiKapanmalidir() {
        if (!documentsPage.movePenceresiKapandiMi()) {
            throw new AssertionError("Move işleminden sonra pencere kapanmadı!");
        }
    }

    @Then("belge hedef klasörde olmalıdır")
    public void belgeHedefKlasordeOlmalidir() {
        // UI üzerinden kalıcılığı doğrula: Move ekranını yeniden aç ve
        // mevcut Destination değerinin oluşturduğumuz klasör olduğunu kontrol et.
        documentsPage.moveButonunaTikla();

        if (!documentsPage.moveHedefKlasorSeciliMi(batch12TargetFolder)) {
            throw new AssertionError(
                    "Belge beklenen hedef klasörde değil: " + batch12TargetFolder
            );
        }

        documentsPage.moveIsleminiIptalEt();
    }

    @Then("belge detay adresi Move sonrasında değişmemelidir")
    public void belgeDetayAdresiMoveSonrasindaDegismemelidir() {
        if (!documentsPage.belgeDetayUrlAyniMi(batch12DetailUrl)) {
            throw new AssertionError(
                    "Move sonrasında belge detay URL'i değişti! Beklenen: "
                            + batch12DetailUrl + " Gerçek: " + BrowserManager.page.url()
            );
        }
    }

    @Then("hedef klasör adı belge detayında görüntülenmelidir")
    public void hedefKlasorAdiBelgeDetayindaGoruntulenmelidir() {
        if (!documentsPage.mevcutKlasorAdiGorunuyorMu(batch12TargetFolder)) {
            throw new AssertionError(
                    "Hedef klasör adı belge detayında görüntülenmedi: " + batch12TargetFolder
            );
        }
    }



    // =========================
    // DOCUMENTS BATCH 13
    // COPY / DESTINATION / INDEPENDENCE
    // =========================

    @Given("Batch 13 kaynak belge bilgileri kaydedilmiştir")
    public void batch13KaynakBelgeBilgileriKaydedilmistir() {
        batch13SourceTitle = documentTitle;
        batch13SourceUrl = BrowserManager.page.url();
    }

    @Given("benzersiz bir Copy hedef klasörü oluşturulmuştur")
    public void benzersizBirCopyHedefKlasoruOlusturulmustur() {
        if (batch13SourceUrl == null) {
            batch13SourceUrl = BrowserManager.page.url();
        }
        batch13TargetFolder = "QA Copy Folder " + System.currentTimeMillis();
        documentsPage.hedefKlasorOlustur(batch13TargetFolder);
        documentsPage.belgeDetayAdresiniAcBatch13(batch13SourceUrl);
    }

    @When("kullanıcı Copy penceresini açar")
    public void kullaniciCopyPenceresiniAcar() {
        documentsPage.copyPenceresiniAc();
    }

    @Then("Copy penceresi görüntülenmelidir")
    public void copyPenceresiGoruntulenmelidir() {
        if (!documentsPage.copyPenceresiGorunuyorMu()) {
            throw new AssertionError("Copy document penceresi görüntülenmedi!");
        }
    }

    @Then("Copy açıklaması görüntülenmelidir")
    public void copyAciklamasiGoruntulenmelidir() {
        if (!documentsPage.copyAciklamasiGorunuyorMu()) {
            throw new AssertionError("Copy açıklaması görüntülenmedi!");
        }
    }

    @Then("Copy varsayılan başlığı kaynak belge başlığı ve copy eki olmalıdır")
    public void copyVarsayilanBasligiOlmalidir() {
        String source = batch13SourceTitle != null ? batch13SourceTitle : documentTitle;
        if (!documentsPage.copyBasligiVarsayilanMi(source)) {
            throw new AssertionError("Copy varsayılan başlığı beklenen değerde değil!");
        }
    }

    @Then("Copy varsayılan hedef klasörü Root olmalıdır")
    public void copyVarsayilanHedefRootOlmalidir() {
        if (!documentsPage.copyDestinationRootMu()) {
            throw new AssertionError("Copy varsayılan hedef klasörü Root değil!");
        }
    }

    @When("kullanıcı Copy başlığına benzersiz bir değer girer")
    public void kullaniciCopyBasliginaBenzersizDegerGirer() {
        batch13CopyTitle = "QA Copy " + System.currentTimeMillis();
        documentsPage.copyBasligiGir(batch13CopyTitle);
    }

    @When("kullanıcı Copy başlığını boş bırakır")
    public void kullaniciCopyBasliginiBosBirakir() {
        // ECM boş bırakıldığında modal açılışındaki varsayılan "(copy)" başlığını kullanıyor.
        batch13CopyTitle = documentsPage.copyBaslangicBasligi();
        documentsPage.copyBasligiGir("");
    }

    @Then("Copy başlığı girilen değer olmalıdır")
    public void copyBasligiGirilenDegerOlmalidir() {
        if (!documentsPage.copyBasligiMi(batch13CopyTitle)) {
            throw new AssertionError("Copy başlığı girilen değerle eşleşmiyor!");
        }
    }

    @Then("kopyalanan belge varsayılan copy başlığıyla görüntülenmelidir")
    public void kopyalananBelgeVarsayilanCopyBasligiylaGoruntulenmelidir() {
        if (!documentsPage.belgeBasligiGorunuyorMu(batch13CopyTitle)) {
            throw new AssertionError(
                    "Boş başlık sonrası varsayılan Copy başlığı görüntülenmedi: "
                            + batch13CopyTitle
            );
        }
    }

    @When("kullanıcı Copy hedef klasörünü seçer")
    public void kullaniciCopyHedefKlasorunuSecer() {
        documentsPage.copyHedefKlasorSec(batch13TargetFolder);
    }

    @Then("Copy hedef klasörü seçili olmalıdır")
    public void copyHedefKlasoruSeciliOlmalidir() {
        if (!documentsPage.copyHedefKlasorSeciliMi(batch13TargetFolder)) {
            throw new AssertionError("Copy hedef klasörü seçili değil: " + batch13TargetFolder);
        }
    }

    @When("kullanıcı Copy işlemini iptal eder")
    public void kullaniciCopyIsleminiIptalEder() {
        documentsPage.copyIsleminiIptalEt();
    }

    @When("kullanıcı Copy penceresini X ile kapatır")
    public void kullaniciCopyPenceresiniXileKapatir() {
        documentsPage.copyPenceresiniXileKapat();
    }

    @Then("Copy penceresi kapanmalıdır")
    public void copyPenceresiKapanmalidir() {
        if (!documentsPage.copyPenceresiKapandiMi()) {
            throw new AssertionError("Copy penceresi kapanmadı!");
        }
    }

    @When("kullanıcı Copy işlemini onaylar")
    public void kullaniciCopyIsleminiOnaylar() {
        if (batch13CopyTitle == null) {
            batch13CopyTitle = documentsPage.copyBaslangicBasligi();
        }

        documentsPage.copyIsleminiOnayla();
        documentsPage.kopyaBelgeDetayiniBekle(batch13CopyTitle, batch13SourceUrl);

        batch13CopyUrl = BrowserManager.page.url();
    }

    @Then("kopyalanan belge başlığı görüntülenmelidir")
    public void kopyalananBelgeBasligiGoruntulenmelidir() {
        if (!documentsPage.belgeBasligiGorunuyorMu(batch13CopyTitle)) {
            throw new AssertionError("Kopyalanan belge başlığı görüntülenmedi: " + batch13CopyTitle);
        }
    }

    @Then("kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır")
    public void kopyalananBelgeFarkliDetayAdresineSahipOlmalidir() {
        if (!documentsPage.belgeDetayUrlFarkliMi(batch13SourceUrl)) {
            throw new AssertionError("Copy yeni bir document detail URL oluşturmadı!");
        }
        batch13CopyUrl = BrowserManager.page.url();
    }

    @When("kullanıcı kaynak belge detayına döner")
    public void kullaniciKaynakBelgeDetayinaDoner() {
        documentsPage.belgeDetayAdresiniAcBatch13(batch13SourceUrl);
    }

    @When("kullanıcı kopya belge detayına döner")
    public void kullaniciKopyaBelgeDetayinaDoner() {
        if (batch13CopyUrl == null) {
            throw new AssertionError("Kopya belge URL'i henüz kaydedilmedi!");
        }
        documentsPage.belgeDetayAdresiniAcBatch13(batch13CopyUrl);
    }

    @Then("kaynak belge başlığı korunmalıdır")
    public void kaynakBelgeBasligiKorunmalidir() {
        if (!documentsPage.belgeBasligiGorunuyorMu(batch13SourceTitle)) {
            throw new AssertionError("Kaynak belge başlığı değişti: " + batch13SourceTitle);
        }
    }

    @Then("Copy butonu görüntülenmelidir")
    public void copyButonuGoruntulenmelidir() {
        if (!documentsPage.copyButonuGorunuyorMu()) {
            throw new AssertionError("Copy butonu görüntülenmedi!");
        }
    }


    // =========================
    // Batch 14 - Version Labels
    // =========================

    @Then("Version history görüntülenmelidir")
    public void versionHistoryGoruntulenmelidir() {
        if (!documentsPage.versionHistoryGorunuyorMu()) {
            throw new AssertionError("Version history görüntülenmedi!");
        }
    }

    @Then("{string} version history satırı görüntülenmelidir")
    public void versionHistorySatiriGoruntulenmelidir(String version) {
        if (!documentsPage.versionHistorySatiriGorunuyorMu(version)) {
            throw new AssertionError("Version history satırı görüntülenmedi: " + version);
        }
    }

    @When("kullanıcı {string} versiyonu için label ekleme alanını açar")
    public void kullaniciVersiyonIcinLabelEklemeAlaniniAcar(String version) {
        batch14Version = version;
        documentsPage.versionLabelEditorunuAc(version);
    }

    @Then("version label ekleme alanı açık olmalıdır")
    public void versionLabelEklemeAlaniAcikOlmalidir() {
        if (!documentsPage.versionLabelEditoruAcikMi(batch14Version)) {
            throw new AssertionError("Version label ekleme alanı açık değil: " + batch14Version);
        }
    }

    @When("kullanıcı benzersiz bir version label girer")
    public void kullaniciBenzersizBirVersionLabelGirer() {
        batch14Label = "release-" + System.currentTimeMillis();
        documentsPage.versionLabelGir(batch14Version, batch14Label);
    }

    @When("kullanıcı version label olarak {string} girer")
    public void kullaniciVersionLabelOlarakGirer(String label) {
        batch14Label = label;
        documentsPage.versionLabelGir(batch14Version, label);
    }

    @When("kullanıcı ikinci benzersiz version label girer")
    public void kullaniciIkinciBenzersizVersionLabelGirer() {
        batch14SecondLabel = "stable-" + System.currentTimeMillis();
        documentsPage.versionLabelGir(batch14Version, batch14SecondLabel);
    }

    @Then("version label input değeri girilen değer olmalıdır")
    public void versionLabelInputDegeriGirilenDegerOlmalidir() {
        String actual = documentsPage.versionLabelInputDegeri(batch14Version);
        if (!actual.equals(batch14Label)) {
            throw new AssertionError(
                    "Version label input değeri farklı. Beklenen: "
                            + batch14Label + ", actual: " + actual
            );
        }
    }

    @When("kullanıcı version label Add bağlantısına tıklar")
    public void kullaniciVersionLabelAddBaglantisinaTiklar() {
        documentsPage.versionLabelEkle(batch14Version);
    }

    @When("kullanıcı version label işlemini iptal eder")
    public void kullaniciVersionLabelIsleminiIptalEder() {
        documentsPage.versionLabelIptalEt(batch14Version);
    }

    @Then("eklenen version label görüntülenmelidir")
    public void eklenenVersionLabelGoruntulenmelidir() {
        if (!documentsPage.versionLabelGorunuyorMu(batch14Version, batch14Label)) {
            throw new AssertionError(
                    "Eklenen version label görüntülenmedi: "
                            + batch14Version + " / " + batch14Label
            );
        }
    }

    @Then("ikinci version label görüntülenmelidir")
    public void ikinciVersionLabelGoruntulenmelidir() {
        if (!documentsPage.versionLabelGorunuyorMu(batch14Version, batch14SecondLabel)) {
            throw new AssertionError(
                    "İkinci version label görüntülenmedi: "
                            + batch14Version + " / " + batch14SecondLabel
            );
        }
    }

    @Then("iptal edilen version label görüntülenmemelidir")
    public void iptalEdilenVersionLabelGoruntulenmemelidir() {
        if (!documentsPage.versionLabelGorunmuyorMu(batch14Version, batch14Label)) {
            throw new AssertionError("İptal edilen version label görüntüleniyor: " + batch14Label);
        }
    }

    @When("kullanıcı eklenen version labelı siler")
    public void kullaniciEklenenVersionLabeliSiler() {
        documentsPage.versionLabelSil(batch14Version, batch14Label);
    }

    @Then("eklenen version label görüntülenmemelidir")
    public void eklenenVersionLabelGoruntulenmemelidir() {
        if (!documentsPage.versionLabelGorunmuyorMu(batch14Version, batch14Label)) {
            throw new AssertionError("Silinen version label hâlâ görüntüleniyor: " + batch14Label);
        }
    }

    @When("kullanıcı ikinci version labelı ekler")
    public void kullaniciIkinciVersionLabeliEkler() {
        documentsPage.versionLabelEditorunuAc(batch14Version);
        batch14SecondLabel = "stable-" + System.currentTimeMillis();
        documentsPage.versionLabelGir(batch14Version, batch14SecondLabel);
        documentsPage.versionLabelEkle(batch14Version);
    }

    @When("kullanıcı {string} versiyonuna benzersiz label ekler")
    public void kullaniciVersiyonunaBenzersizLabelEkler(String version) {
        batch14Version = version;
        batch14Label = "label-" + version.replace(".", "-") + "-" + System.currentTimeMillis();
        documentsPage.versionLabelEditorunuAc(version);
        documentsPage.versionLabelGir(version, batch14Label);
        documentsPage.versionLabelEkle(version);
    }

    @Then("eklenen label {string} versiyonunda görüntülenmelidir")
    public void eklenenLabelVersiyonundaGoruntulenmelidir(String version) {
        if (!documentsPage.versionLabelGorunuyorMu(version, batch14Label)) {
            throw new AssertionError("Label beklenen versiyonda yok: " + version + " / " + batch14Label);
        }
    }

    @Then("eklenen label {string} versiyonunda görüntülenmemelidir")
    public void eklenenLabelVersiyonundaGoruntulenmemelidir(String version) {
        if (!documentsPage.versionLabelGorunmuyorMu(version, batch14Label)) {
            throw new AssertionError("Label yanlış versiyonda görüntüleniyor: " + version + " / " + batch14Label);
        }
    }
    // =====================================================
// BATCH 15 - DOCUMENT RENAME
// =====================================================

    @Given("Batch 15 için yeni bir document oluşturulur")
    public void batch15IcinYeniBirDocumentOlusturulur() {

        // Login + DocumentsPage initialization
        adminLogin();

        batch15OriginalTitle =
                "Batch15 Rename " + System.currentTimeMillis();

        // Mevcut helper'ları kullanıyoruz
        uploadWithoutClassSayfasinaGit();

        documentsPage.belgeYukle(batch15OriginalTitle);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Batch 15 document yüklendikten sonra detail sayfası açılmadı!"
            );
        }

        batch15DocumentUrl = BrowserManager.page.url();

        if (!documentsPage.belgeBasligiGorunuyorMu(batch15OriginalTitle)) {
            throw new AssertionError(
                    "Batch 15 document oluşturulamadı: "
                            + batch15OriginalTitle
            );
        }
    }

    @Then("document rename edit butonu görüntülenmelidir")
    public void documentRenameEditButonuGoruntulenmelidir() {

        if (!documentsPage.renameEditButonuGorunuyorMu()) {
            throw new AssertionError(
                    "Document rename edit butonu görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı document rename editorunu açar")
    public void kullaniciDocumentRenameEditorunuAcar() {

        documentsPage.renameEditorunuAc();
    }

    @Then("document rename editoru görüntülenmelidir")
    public void documentRenameEditoruGoruntulenmelidir() {

        if (!documentsPage.renameEditoruAcikMi()) {
            throw new AssertionError(
                    "Document rename editoru açılmadı!"
            );
        }
    }

    @Then("rename alanında mevcut document başlığı bulunmalıdır")
    public void renameAlanindaMevcutDocumentBasligiBulunmalidir() {

        String actual =
                documentsPage.renameInputDegeri();

        if (!actual.equals(batch15OriginalTitle)) {
            throw new AssertionError(
                    "Rename input mevcut başlığı içermiyor. "
                            + "Beklenen: " + batch15OriginalTitle
                            + " Actual: " + actual
            );
        }
    }

    @Then("rename confirm butonu görüntülenmelidir")
    public void renameConfirmButonuGoruntulenmelidir() {

        if (!documentsPage.renameEditoruAcikMi()) {
            throw new AssertionError(
                    "Confirm rename butonu görüntülenmedi!"
            );
        }
    }

    @Then("rename cancel butonu görüntülenmelidir")
    public void renameCancelButonuGoruntulenmelidir() {

        if (!documentsPage.renameEditoruAcikMi()) {
            throw new AssertionError(
                    "Rename Cancel butonu görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir")
    public void kullaniciDocumentBasliginiYeniBenzersizBirBasliklaDegistirir() {

        batch15RenamedTitle =
                "Batch15 Renamed " + System.currentTimeMillis();

        documentsPage.renameBasligiGir(
                batch15RenamedTitle
        );
    }

    @Then("rename alanında yeni document başlığı bulunmalıdır")
    public void renameAlanindaYeniDocumentBasligiBulunmalidir() {

        String actual =
                documentsPage.renameInputDegeri();

        if (!actual.equals(batch15RenamedTitle)) {
            throw new AssertionError(
                    "Rename input değeri yanlış. Beklenen: "
                            + batch15RenamedTitle
                            + " Actual: "
                            + actual
            );
        }
    }

    @When("kullanıcı document rename işlemini kaydeder")
    public void kullaniciDocumentRenameIsleminiKaydeder() {

        documentsPage.renameKaydet();
        documentsPage.renameSonucunuBekle(
                batch15RenamedTitle
        );
    }

    @Then("document yeni başlığı ile görüntülenmelidir")
    public void documentYeniBasligiIleGoruntulenmelidir() {

        if (!documentsPage.belgeBasligiDegistiMi(
                batch15RenamedTitle)) {

            throw new AssertionError(
                    "Yeni document başlığı görüntülenmedi: "
                            + batch15RenamedTitle
            );
        }
    }

    @Then("eski document başlığı artık görüntülenmemelidir")
    public void eskiDocumentBasligiArtikGoruntulenmemelidir() {

        if (!documentsPage.eskiBelgeBasligiGorunmuyorMu(
                batch15OriginalTitle)) {

            throw new AssertionError(
                    "Eski document başlığı hâlâ görüntüleniyor: "
                            + batch15OriginalTitle
            );
        }
    }

    @Then("rename sonrasında document URL değişmemelidir")
    public void renameSonrasindaDocumentUrlDegismemelidir() {

        if (!documentsPage.renameSonrasiUrlAyniMi(
                batch15DocumentUrl)) {

            throw new AssertionError(
                    "Rename document ID/URL değiştirdi! "
                            + "Önce: " + batch15DocumentUrl
                            + " Sonra: "
                            + BrowserManager.page.url()
            );
        }
    }

    @When("kullanıcı document rename işlemini iptal eder")
    public void kullaniciDocumentRenameIsleminiIptalEder() {

        documentsPage.renameIptalEt();
    }

    @Then("document rename editoru kapanmalıdır")
    public void documentRenameEditoruKapanmalidir() {

        if (!documentsPage.renameEditoruKapandiMi()) {
            throw new AssertionError(
                    "Rename editoru kapanmadı!"
            );
        }
    }

    @Then("document başlığı değişmeden kalmalıdır")
    public void documentBasligiDegismedenKalmalidir() {

        if (!documentsPage.belgeBasligiGorunuyorMu(
                batch15OriginalTitle)) {

            throw new AssertionError(
                    "Cancel sonrasında document başlığı değişti!"
            );
        }
    }

    @When("kullanıcı document detay sayfasını yeniler")
    public void kullaniciDocumentDetaySayfasiniYeniler() {

        documentsPage.belgeDetayiniRefreshEt();
    }

    @Then("rename edilen document başlığı refresh sonrasında korunmalıdır")
    public void renameEdilenDocumentBasligiRefreshSonrasindaKorunmalidir() {

        if (!documentsPage.belgeBasligiDegistiMi(
                batch15RenamedTitle)) {

            throw new AssertionError(
                    "Rename edilen başlık refresh sonrasında korunmadı: "
                            + batch15RenamedTitle
            );
        }
    }
    // =====================================================
    // BATCH 16 - DOCUMENT RENAME VALIDATION / EDGE CASES
    // =====================================================

    @Given("Batch 16 için yeni bir document oluşturulur")
    public void batch16IcinYeniBirDocumentOlusturulur() {

        adminLogin();

        batch16OriginalTitle =
                "Batch16 Rename " + System.currentTimeMillis();

        uploadWithoutClassSayfasinaGit();

        documentsPage.belgeYukle(batch16OriginalTitle);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Batch 16 document yüklendikten sonra detail sayfası açılmadı!"
            );
        }

        if (!documentsPage.belgeBasligiGorunuyorMu(batch16OriginalTitle)) {
            throw new AssertionError(
                    "Batch 16 document oluşturulamadı: "
                            + batch16OriginalTitle
            );
        }
    }

    @When("kullanıcı document rename alanını boş bırakır")
    public void kullaniciDocumentRenameAlaniniBosBirakir() {

        documentsPage.renameBasligiGir("");
    }

    @Then("document rename işlemi boş başlıkla kaydedilememelidir")
    public void documentRenameIslemiBosBasliklaKaydedilememelidir() {

        documentsPage.renameKaydet();

        if (!documentsPage.renameBasligiKaydedilmediMi(
                batch16OriginalTitle)) {

            throw new AssertionError(
                    "Boş document başlığı sistem tarafından kaydedildi! "
                            + "Original başlık korunmadı: "
                            + batch16OriginalTitle
            );
        }
    }

    @When("kullanıcı document rename alanına sadece boşluk girer")
    public void kullaniciDocumentRenameAlaninaSadeceBoslukGirer() {

        documentsPage.renameBasligiGir("   ");
    }

    @Then("document rename işlemi boşluk başlıkla kaydedilememelidir")
    public void documentRenameIslemiBoslukBasliklaKaydedilememelidir() {

        documentsPage.renameKaydet();

        if (!documentsPage.renameBasligiKaydedilmediMi(
                batch16OriginalTitle)) {

            throw new AssertionError(
                    "Sadece boşluk içeren document başlığı sistem tarafından kaydedildi! "
                            + "Original başlık korunmadı: "
                            + batch16OriginalTitle
            );
        }
    }

    @When("kullanıcı document başlığını Batch 16 için yeni benzersiz bir başlıkla değiştirir")
    public void kullaniciDocumentBasliginiBatch16IcinYeniBenzersizBirBasliklaDegistirir() {

        batch16RenamedTitle =
                "Batch16 Renamed " + System.currentTimeMillis();

        documentsPage.renameBasligiGir(
                batch16RenamedTitle
        );
    }

    @Then("Batch 16 original document başlığı görüntülenmelidir")
    public void batch16OriginalDocumentBasligiGoruntulenmelidir() {

        if (!documentsPage.belgeBasligiGorunuyorMu(
                batch16OriginalTitle)) {

            throw new AssertionError(
                    "Cancel + refresh sonrasında original document başlığı korunmadı: "
                            + batch16OriginalTitle
            );
        }
    }

    @When("kullanıcı document başlığını Batch 16 için birinci kez değiştirip kaydeder")
    public void kullaniciDocumentBasliginiBatch16IcinBirinciKezDegistiripKaydeder() {

        batch16FirstRenamedTitle =
                "Batch16 First Rename " + System.currentTimeMillis();

        documentsPage.renameEditorunuAc();

        documentsPage.renameBasligiGir(
                batch16FirstRenamedTitle
        );

        documentsPage.renameKaydet();

        documentsPage.renameSonucunuBekle(
                batch16FirstRenamedTitle
        );
    }

    @When("kullanıcı document başlığını Batch 16 için ikinci kez değiştirip kaydeder")
    public void kullaniciDocumentBasliginiBatch16IcinIkinciKezDegistiripKaydeder() {

        batch16SecondRenamedTitle =
                "Batch16 Second Rename " + System.currentTimeMillis();

        documentsPage.renameEditorunuAc();

        documentsPage.renameBasligiGir(
                batch16SecondRenamedTitle
        );

        documentsPage.renameKaydet();

        documentsPage.renameSonucunuBekle(
                batch16SecondRenamedTitle
        );
    }

    @Then("document ikinci Batch 16 başlığı ile görüntülenmelidir")
    public void documentIkinciBatch16BasligiIleGoruntulenmelidir() {

        if (!documentsPage.belgeBasligiDegistiMi(
                batch16SecondRenamedTitle)) {

            throw new AssertionError(
                    "Document ikinci rename başlığı ile görüntülenmedi: "
                            + batch16SecondRenamedTitle
            );
        }
    }

    @Then("birinci Batch 16 rename başlığı artık görüntülenmemelidir")
    public void birinciBatch16RenameBasligiArtikGoruntulenmemelidir() {

        if (!documentsPage.eskiBelgeBasligiGorunmuyorMu(
                batch16FirstRenamedTitle)) {

            throw new AssertionError(
                    "Birinci rename başlığı ikinci rename sonrasında hâlâ görüntüleniyor: "
                            + batch16FirstRenamedTitle
            );
        }
    }
    // =====================================================
// BATCH 17 - VERSION CHANGE COMMENT
// =====================================================

    @Given("Batch 17 için yeni bir document oluşturulur")
    public void batch17IcinYeniBirDocumentOlusturulur() {

        adminLogin();

        batch17OriginalTitle =
                "Batch17 Version Comment " + System.currentTimeMillis();

        uploadWithoutClassSayfasinaGit();

        documentsPage.belgeYukle(batch17OriginalTitle);

        if (!documentsPage.belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Batch 17 document oluşturulduktan sonra detail sayfası açılmadı!"
            );
        }

        if (!documentsPage.belgeBasligiGorunuyorMu(batch17OriginalTitle)) {
            throw new AssertionError(
                    "Batch 17 document başlığı görüntülenmedi: "
                            + batch17OriginalTitle
            );
        }
    }

    @When("kullanıcı Batch 17 document için yeni version oluşturmayı açar")
    public void kullaniciBatch17DocumentIcinYeniVersionOlusturmayiAcar() {

        documentsPage.yeniVersiyonSayfasiniAc();
    }

    @Then("Version comment alanı görüntülenmelidir")
    public void versionCommentAlaniGoruntulenmelidir() {

        if (!documentsPage.versionCommentAlaniGorunuyorMu()) {
            throw new AssertionError(
                    "Version comment alanı görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı benzersiz bir Batch 17 version comment girer")
    public void kullaniciBenzersizBirBatch17VersionCommentGirer() {

        batch17VersionComment =
                "Batch17 change comment " + System.currentTimeMillis();

        documentsPage.versionCommentGir(batch17VersionComment);
    }

    @When("kullanıcı Version comment alanını boş bırakır")
    public void kullaniciVersionCommentAlaniniBosBirakir() {

        documentsPage.versionCommentBosBirak();
    }

    @When("kullanıcı Batch 17 yeni version işlemini kaydeder")
    public void kullaniciBatch17YeniVersionIsleminiKaydeder() {

        documentsPage.versionTipiniSec("Patch");

        documentsPage.yeniVersiyonDosyasiniSec();

        documentsPage.yeniVersiyonuKaydet();
    }

    @Then("Batch 17 version comment document detayında görüntülenmelidir")
    public void batch17VersionCommentDocumentDetayindaGoruntulenmelidir() {

        if (
                batch17VersionComment == null
                        || batch17VersionComment.isBlank()
        ) {
            throw new AssertionError(
                    "Batch 17 version comment oluşturulmamış!"
            );
        }

        if (!documentsPage.versionCommentGorunuyorMu(batch17VersionComment)) {
            throw new AssertionError(
                    "Batch 17 version comment document detail üzerinde görüntülenmedi: "
                            + batch17VersionComment
            );
        }
    }

    @Then("Batch 17 yeni version history kaydı görüntülenmelidir")
    public void batch17YeniVersionHistoryKaydiGoruntulenmelidir() {

        if (!documentsPage.versionHistorySatiriGorunuyorMu(batch17ExpectedVersion)) {
            throw new AssertionError(
                    "Batch 17 yeni version history kaydı görüntülenmedi: "
                            + batch17ExpectedVersion
            );
        }
    }

    @Then("Batch 17 version comment yeni version kaydında görüntülenmelidir")
    public void batch17VersionCommentYeniVersionKaydindaGoruntulenmelidir() {

        if (!documentsPage.versionCommentGorunuyorMu(batch17VersionComment)) {
            throw new AssertionError(
                    "Batch 17 version comment yeni version kaydında görüntülenmedi: "
                            + batch17VersionComment
            );
        }
    }

    @Then("Batch 17 yeni version başarıyla oluşturulmalıdır")
    public void batch17YeniVersionBasariylaOlusturulmalidir() {

        if (!documentsPage.versionHistorySatiriGorunuyorMu(batch17ExpectedVersion)) {
            throw new AssertionError(
                    "Version comment boşken yeni version oluşturulamadı. Beklenen: "
                            + batch17ExpectedVersion
            );
        }
    }

}