package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.FolderPage;
import pages.LoginPage;
import utils.BrowserManager;

public class FolderSteps {

    private FolderPage folderPage;
    private LoginPage loginPage;

    private String renameSourceName;
    private String renameTargetName;

    private void adminGirisYap() {

        BrowserManager.startBrowser();

        loginPage = new LoginPage(BrowserManager.page);
        folderPage = new FolderPage(BrowserManager.page);

        loginPage.loginSayfasiniAc();
        loginPage.emailGir("admin@local");
        loginPage.sifreGir("123");
        loginPage.girisYapButonunaTikla();

        BrowserManager.page.waitForURL("**/folders");
    }

    @Given("admin kullanıcı ECM sistemine giriş yapmıştır")
    public void adminKullaniciEcmSistemineGirisYapmistir() {

        adminGirisYap();
    }

    @Given("admin kullanıcı klasörler sayfasındadır")
    public void adminKullaniciKlasorlerSayfasindadir() {

        adminGirisYap();
        folderPage.klasorlerSayfasiniAc();
    }

    @When("kullanıcı klasörler sayfasını açar")
    public void kullaniciKlasorlerSayfasiniAcar() {

        folderPage.klasorlerSayfasiniAc();
    }

    @Then("ECM klasörler sayfası görüntülenmelidir")
    public void ecmKlasorlerSayfasiGoruntulenmelidir() {

        if (!folderPage.klasorlerSayfasindaMi()) {
            throw new AssertionError(
                    "ECM klasörler sayfası görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı {string} isimli yeni klasör oluşturur")
    public void kullaniciYeniKlasorOlusturur(String folderName) {

        folderPage.klasorlerSayfasiniAc();

        if (!folderPage.klasorGorunuyorMu(folderName)) {
            folderPage.klasorOlustur(folderName);
        }
    }

    @Then("{string} klasörü görüntülenmelidir")
    public void klasorGoruntulenmelidir(String folderName) {

        folderPage.klasorlerSayfasiniAc();

        if (!folderPage.klasorGorunuyorMu(folderName)) {
            throw new AssertionError(
                    folderName + " klasörü görüntülenmedi!"
            );
        }
    }

    @Given("{string} klasörü mevcuttur")
    public void klasorMevcuttur(String folderName) {

        folderPage.klasorlerSayfasiniAc();

        if (!folderPage.klasorGorunuyorMu(folderName)) {

            folderPage.klasorOlustur(folderName);
            folderPage.klasorlerSayfasiniAc();
        }
    }

    @When("kullanıcı {string} klasörünü açar")
    public void kullaniciKlasoruAcar(String folderName) {

        folderPage.klasoruAc(folderName);
    }

    @Then("klasör detay sayfası görüntülenmelidir")
    public void klasorDetaySayfasiGoruntulenmelidir() {

        if (!folderPage.klasorDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Klasör detay sayfası görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı {string} altında {string} klasörü oluşturur")
    public void kullaniciAltindaKlasorOlusturur(
            String parentFolder,
            String childFolder
    ) {

        folderPage.altKlasorOlustur(
                parentFolder,
                childFolder
        );
    }

    @Then("{string} alt klasörü görüntülenmelidir")
    public void altKlasorGoruntulenmelidir(String childFolder) {

        if (!folderPage.altKlasorGorunuyorMu(childFolder)) {
            throw new AssertionError(
                    childFolder + " alt klasörü görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı yeniden adlandırma için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciYenidenAdlandirmaIcinBenzersizBirKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        renameSourceName =
                "QA Rename Source " + System.currentTimeMillis();

        folderPage.klasorOlustur(renameSourceName);
    }

    @When("kullanıcı klasörü benzersiz yeni bir adla yeniden adlandırır")
    public void kullaniciKlasoruBenzersizYeniBirAdlaYenidenAdlandirir() {

        renameTargetName =
                "QA Rename Target " + System.currentTimeMillis();

        folderPage.klasoruAc(renameSourceName);
        folderPage.klasoruYenidenAdlandir(renameTargetName);
    }

    @Then("klasör yeni adıyla görüntülenmelidir")
    public void klasorYeniAdiylaGoruntulenmelidir() {

        if (!folderPage.klasorBasligiMi(renameTargetName)) {
            throw new AssertionError(
                    renameTargetName
                            + " klasör adı detay sayfasında görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı klasörü pasif hale getirir")
    public void kullaniciKlasoruPasifHaleGetirir() {

        folderPage.klasorlerSayfasiniAc();
        folderPage.klasoruAc("QA Folder Renamed");

        if (folderPage.klasorAktifMi()) {
            folderPage.klasoruPasifYap();
        }
    }

    @Then("klasör pasif olarak görüntülenmelidir")
    public void klasorPasifGoruntulenmelidir() {

        if (!folderPage.klasorPasifMi()) {
            throw new AssertionError(
                    "Klasör pasif olarak görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı pasif bir klasörün detay sayfasındadır")
    public void adminKullaniciPasifKlasorDetaySayfasindadir() {

        adminKullaniciKlasorlerSayfasindadir();

        if (!folderPage.klasorGorunuyorMu("QA Folder Renamed")) {

            folderPage.klasorOlustur("QA Folder Renamed");
            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc("QA Folder Renamed");

        if (folderPage.klasorAktifMi()) {
            folderPage.klasoruPasifYap();
        }
    }

    @When("kullanıcı klasörü aktif hale getirir")
    public void kullaniciKlasoruAktifHaleGetirir() {

        folderPage.klasoruAktifYap();
    }

    @Then("klasör aktif olarak görüntülenmelidir")
    public void klasorAktifGoruntulenmelidir() {

        if (!folderPage.klasorAktifMi()) {
            throw new AssertionError(
                    "Klasör aktif olarak görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı klasör detay sayfasındadır")
    public void adminKullaniciKlasorDetaySayfasindadir() {

        adminKullaniciKlasorlerSayfasindadir();

        if (!folderPage.klasorGorunuyorMu("QA Folder 01")) {

            folderPage.klasorOlustur("QA Folder 01");
            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc("QA Folder 01");

        if (!folderPage.altKlasorGorunuyorMu("QA Child Folder")) {

            folderPage.altKlasorOlusturDetaySayfasinda(
                    "QA Child Folder"
            );
        }
    }

    @When("kullanıcı alt klasörleri görüntüler")
    public void kullaniciAltKlasorleriGoruntuler() {

        // Alt klasörler detay sayfasında doğrudan gösteriliyor.
    }

    @Then("mevcut alt klasörler listelenmelidir")
    public void mevcutAltKlasorlerListelenmelidir() {

        if (!folderPage.altKlasorGorunuyorMu("QA Child Folder")) {
            throw new AssertionError(
                    "Alt klasörler görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı klasör geçmişini açar")
    public void kullaniciKlasorGecmisiniAcar() {

        folderPage.historyAc();
    }

    @Then("klasör geçmişi görüntülenmelidir")
    public void klasorGecmisiGoruntulenmelidir() {

        if (!folderPage.historyGorunuyorMu()) {
            throw new AssertionError(
                    "Klasör geçmişi görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı klasör izinleri bölümünü açar")
    public void kullaniciKlasorIzinleriBolumunuAcar() {

        // Folder access kartı detay sayfasında doğrudan bulunuyor.
    }

    @Then("klasör ACL bilgileri görüntülenmelidir")
    public void klasorAclBilgileriGoruntulenmelidir() {

        if (!folderPage.aclAlaniGorunuyorMu()) {
            throw new AssertionError(
                    "Folder access alanı görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı allowed classes bölümünü açar")
    public void kullaniciAllowedClassesBolumunuAcar() {

        // Allowed classes kartı detay sayfasında doğrudan bulunuyor.
    }

    @Then("izin verilen class bilgileri görüntülenmelidir")
    public void izinVerilenClassBilgileriGoruntulenmelidir() {

        if (!folderPage.allowedClassesAlaniGorunuyorMu()) {
            throw new AssertionError(
                    "Allowed document classes alanı görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı bir alt klasörün detay sayfasındadır")
    public void adminKullaniciAltKlasorDetaySayfasindadir() {

        adminKullaniciKlasorlerSayfasindadir();

        if (!folderPage.klasorGorunuyorMu("QA Folder 01")) {

            folderPage.klasorOlustur("QA Folder 01");
            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc("QA Folder 01");

        if (!folderPage.altKlasorGorunuyorMu("QA Child Folder")) {

            folderPage.altKlasorOlusturDetaySayfasinda(
                    "QA Child Folder"
            );
        }

        folderPage.klasoruAc("QA Child Folder");
    }

    @Then("klasör breadcrumb bilgisi görüntülenmelidir")
    public void klasorBreadcrumbBilgisiGoruntulenmelidir() {

        if (!folderPage.breadcrumbGorunuyorMu()) {
            throw new AssertionError(
                    "Klasör breadcrumb bilgisi görüntülenmedi!"
            );
        }
    }
}