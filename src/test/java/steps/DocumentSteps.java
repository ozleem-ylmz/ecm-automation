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

        if (!documentsPage.downloadButonuGorunuyorMu(managerPage)) {
            throw new AssertionError(
                    "Manager için Download butonu görüntülenmedi!"
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


}
