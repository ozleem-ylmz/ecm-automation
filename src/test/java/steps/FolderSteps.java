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

    private String testFolderName;
    private String testChildName;

    private String originalFolderName;
    private String temporaryFolderName;
    private String expectedTrimmedName;

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

        folderPage.klasoruYenidenAdlandir(
                renameTargetName
        );
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

        folderPage.klasoruAc(
                "QA Folder Renamed"
        );

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

            folderPage.klasorOlustur(
                    "QA Folder Renamed"
            );

            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc(
                "QA Folder Renamed"
        );

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

            folderPage.klasorOlustur(
                    "QA Folder 01"
            );

            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc(
                "QA Folder 01"
        );

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

            folderPage.klasorOlustur(
                    "QA Folder 01"
            );

            folderPage.klasorlerSayfasiniAc();
        }

        folderPage.klasoruAc(
                "QA Folder 01"
        );

        if (!folderPage.altKlasorGorunuyorMu("QA Child Folder")) {

            folderPage.altKlasorOlusturDetaySayfasinda(
                    "QA Child Folder"
            );
        }

        folderPage.klasoruAc(
                "QA Child Folder"
        );
    }

    @Then("klasör breadcrumb bilgisi görüntülenmelidir")
    public void klasorBreadcrumbBilgisiGoruntulenmelidir() {

        if (!folderPage.breadcrumbGorunuyorMu()) {

            throw new AssertionError(
                    "Klasör breadcrumb bilgisi görüntülenmedi!"
            );
        }
    }

    // ---------------------------------------------------------
    // FOLDER BATCH 2
    // ---------------------------------------------------------

    @Given("admin kullanıcı duplicate testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciDuplicateTestiIcinBenzersizKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        testFolderName =
                "QA Duplicate Root " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                testFolderName
        );
    }

    @When("kullanıcı aynı isimde klasörü tekrar oluşturmaya çalışır")
    public void kullaniciAyniIsimdeKlasoruTekrarOlusturmayaCalisir() {

        folderPage.klasorlerSayfasiniAc();

        folderPage.klasorOlusturBeklemeden(
                testFolderName
        );
    }

    @Then("duplicate klasör hatası görüntülenmelidir")
    public void duplicateKlasorHatasiGoruntulenmelidir() {

        if (!folderPage.duplicateKlasorHatasiGorunuyorMu()) {

            throw new AssertionError(
                    "Duplicate klasör hatası görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı duplicate alt klasör testi için benzersiz bir parent ve child oluşturmuştur")
    public void adminKullaniciDuplicateAltKlasorTestiIcinBenzersizParentVeChildOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        long timestamp =
                System.currentTimeMillis();

        testFolderName =
                "QA Duplicate Parent " + timestamp;

        testChildName =
                "QA Duplicate Child " + timestamp;

        folderPage.klasorOlustur(
                testFolderName
        );

        folderPage.klasoruAc(
                testFolderName
        );

        folderPage.altKlasorOlusturDetaySayfasinda(
                testChildName
        );
    }

    @When("kullanıcı aynı child klasörü tekrar oluşturmaya çalışır")
    public void kullaniciAyniChildKlasoruTekrarOlusturmayaCalisir() {

        folderPage.altKlasorOlusturBeklemeden(
                testChildName
        );
    }

    @Given("admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır")
    public void adminKullaniciBenzersizBirParentKlasorunDetaySayfasindadir() {

        adminKullaniciKlasorlerSayfasindadir();

        testFolderName =
                "QA Parent " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                testFolderName
        );

        folderPage.klasoruAc(
                testFolderName
        );
    }

    @When("kullanıcı {string} isimli tek karakterli alt klasör oluşturur")
    public void kullaniciTekKarakterliAltKlasorOlusturur(
            String childFolder
    ) {

        folderPage.altKlasorOlusturDetaySayfasinda(
                childFolder
        );
    }

    @Then("alt klasör adı alanının maksimum uzunluğu 255 olmalıdır")
    public void altKlasorAdiAlanininMaksimumUzunlugu255Olmalidir() {

        if (!folderPage.altKlasorMaxLength255Mi()) {

            throw new AssertionError(
                    "Alt klasör input maxlength değeri 255 değil!"
            );
        }
    }

    @When("kullanıcı rename formunu açar")
    public void kullaniciRenameFormunuAcar() {

        folderPage.renameFormunuAc();
    }

    @Then("rename alanının maksimum uzunluğu 255 olmalıdır")
    public void renameAlanininMaksimumUzunlugu255Olmalidir() {

        if (!folderPage.renameMaxLength255Mi()) {

            throw new AssertionError(
                    "Rename input maxlength değeri 255 değil!"
            );
        }
    }

    @Then("Explorer bölümü görüntülenmelidir")
    public void explorerBolumuGoruntulenmelidir() {

        if (!folderPage.explorerGorunuyorMu()) {

            throw new AssertionError(
                    "Explorer bölümü görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı benzersiz parent ve child klasör oluşturup child detayını açmıştır")
    public void adminKullaniciBenzersizParentVeChildKlasorOlusturupChildDetayiniAcmistir() {

        adminKullaniciKlasorlerSayfasindadir();

        long timestamp =
                System.currentTimeMillis();

        testFolderName =
                "QA Breadcrumb Parent " + timestamp;

        testChildName =
                "QA Breadcrumb Child " + timestamp;

        folderPage.klasorOlustur(
                testFolderName
        );

        folderPage.klasoruAc(
                testFolderName
        );

        folderPage.altKlasorOlusturDetaySayfasinda(
                testChildName
        );

        folderPage.klasoruAc(
                testChildName
        );
    }

    @Then("breadcrumb alanında parent ve child klasör adları görüntülenmelidir")
    public void breadcrumbAlanindaParentVeChildKlasorAdlariGoruntulenmelidir() {

        if (!folderPage.breadcrumbKlasorleriGorunuyorMu(
                testFolderName,
                testChildName
        )) {

            throw new AssertionError(
                    "Breadcrumb içinde parent ve child klasör adları görüntülenmedi!"
            );
        }
    }

    @When("kullanıcı test klasörünü pasif hale getirir")
    public void kullaniciTestKlasorunuPasifHaleGetirir() {

        if (folderPage.klasorAktifMi()) {

            folderPage.klasoruPasifYap();
        }
    }

    @Then("Inactive etiketi görüntülenmelidir")
    public void inactiveEtiketiGoruntulenmelidir() {

        if (!folderPage.inactiveEtiketiGorunuyorMu()) {

            throw new AssertionError(
                    "Inactive etiketi görüntülenmedi!"
            );
        }
    }

    @Then("pasif klasör altında yeni alt klasör oluşturulamamalıdır")
    public void pasifKlasorAltindaYeniAltKlasorOlusturulamamalidir() {

        testChildName =
                "QA Inactive Child " + System.currentTimeMillis();

        if (!folderPage.pasifKlasordeAltKlasorOlusturulamiyorMu(
                testChildName
        )) {

            throw new AssertionError(
                    "Pasif klasör altında alt klasör oluşturuldu!"
            );
        }
    }

    @When("kullanıcı test klasörünü pasif hale getirip tekrar aktif eder")
    public void kullaniciTestKlasorunuPasifHaleGetiripTekrarAktifEder() {

        if (folderPage.klasorAktifMi()) {

            folderPage.klasoruPasifYap();
        }

        folderPage.klasoruAktifYap();
    }

    @Then("aktif klasör altında yeni alt klasör oluşturulabilmelidir")
    public void aktifKlasorAltindaYeniAltKlasorOlusturulabilmelidir() {

        testChildName =
                "QA Reactivated Child " + System.currentTimeMillis();

        folderPage.altKlasorOlusturDetaySayfasinda(
                testChildName
        );

        if (!folderPage.altKlasorGorunuyorMu(testChildName)) {

            throw new AssertionError(
                    "Tekrar aktif edilen klasörde alt klasör oluşturulamadı!"
            );
        }
    }

    // ---------------------------------------------------------
    // FOLDER BATCH 3
    // ---------------------------------------------------------

    @Then("permission inheritance açık olmalıdır")
    public void permissionInheritanceAcikOlmalidir() {

        if (!folderPage.permissionInheritanceAcikMi()) {

            throw new AssertionError(
                    "Permission inheritance açık değil!"
            );
        }
    }

    @When("kullanıcı permission inheritance özelliğini kapatır")
    public void kullaniciPermissionInheritanceOzelliginiKapatir() {

        if (folderPage.permissionInheritanceAcikMi()) {

            folderPage.permissionInheritanceDegistir();
        }
    }

    @Then("permission inheritance kapalı olmalıdır")
    public void permissionInheritanceKapaliOlmalidir() {

        if (folderPage.permissionInheritanceAcikMi()) {

            throw new AssertionError(
                    "Permission inheritance kapanmadı!"
            );
        }
    }

    @When("kullanıcı permission inheritance özelliğini kapatıp tekrar açar")
    public void kullaniciPermissionInheritanceOzelliginiKapatipTekrarAcar() {

        if (folderPage.permissionInheritanceAcikMi()) {
            folderPage.permissionInheritanceDegistir();
        }

        if (!folderPage.permissionInheritanceAcikMi()) {
            folderPage.permissionInheritanceDegistir();
        }
    }

    @Given("admin kullanıcı rename cancel testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciRenameCancelTestiIcinBenzersizKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        originalFolderName =
                "QA Rename Cancel " + System.currentTimeMillis();

        temporaryFolderName =
                "QA Should Not Save " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                originalFolderName
        );

        folderPage.klasoruAc(
                originalFolderName
        );
    }

    @When("kullanıcı rename formunu açıp yeni isim girer ve Cancel butonuna basar")
    public void kullaniciRenameFormunuAcipYeniIsimGirerVeCancelButonunaBasar() {

        folderPage.renameFormunuAc();

        folderPage.renameInputDoldur(
                temporaryFolderName
        );

        folderPage.renameIptalEt();
    }

    @Then("klasörün eski adı korunmalıdır")
    public void klasorunEskiAdiKorunmalidir() {

        if (!folderPage.klasorBasligiMi(
                originalFolderName
        )) {

            throw new AssertionError(
                    "Cancel sonrası klasörün eski adı korunmadı!"
            );
        }

        if (!folderPage.renameFormuKapaliMi()) {

            throw new AssertionError(
                    "Cancel sonrası rename formu kapanmadı!"
            );
        }
    }

    @Given("admin kullanıcı same name rename testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciSameNameRenameTestiIcinBenzersizKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        originalFolderName =
                "QA Same Rename " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                originalFolderName
        );

        folderPage.klasoruAc(
                originalFolderName
        );
    }

    @When("kullanıcı klasörü mevcut adıyla yeniden kaydeder")
    public void kullaniciKlasoruMevcutAdiylaYenidenKaydeder() {

        folderPage.renameFormunuAc();

        folderPage.renameInputDoldur(
                originalFolderName
        );

        folderPage.renameKaydet();
    }

    @Then("klasörün mevcut adı korunmalıdır")
    public void klasorunMevcutAdiKorunmalidir() {

        if (!folderPage.klasorBasligiMi(
                originalFolderName
        )) {

            throw new AssertionError(
                    "Aynı isimle rename sonrası klasör adı değişti!"
            );
        }
    }

    @Given("admin kullanıcı trim testi için benzersiz bir parent klasörün detay sayfasındadır")
    public void adminKullaniciTrimTestiIcinBenzersizParentKlasorDetaySayfasindadir() {

        adminKullaniciKlasorlerSayfasindadir();

        testFolderName =
                "QA Trim Parent " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                testFolderName
        );

        folderPage.klasoruAc(
                testFolderName
        );
    }

    @When("kullanıcı başında ve sonunda boşluk olan benzersiz bir alt klasör oluşturur")
    public void kullaniciBoslukOlanBenzersizAltKlasorOlusturur() {

        expectedTrimmedName =
                "QA Trim Child " + System.currentTimeMillis();

        String nameWithSpaces =
                "   " + expectedTrimmedName + "   ";

        folderPage.altKlasorInputunaYaz(
                nameWithSpaces
        );

        folderPage.altKlasorCreateButonunaTikla();
    }

    @Then("alt klasör adı boşluklar olmadan görüntülenmelidir")
    public void altKlasorAdiBosluklarOlmadanGoruntulenmelidir() {

        if (!folderPage.klasorLinkiGorunuyorMu(
                expectedTrimmedName
        )) {

            throw new AssertionError(
                    "Alt klasör adı trim edilmedi!"
            );
        }
    }

    @Given("admin kullanıcı rename trim testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciRenameTrimTestiIcinBenzersizKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        originalFolderName =
                "QA Rename Trim Source " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                originalFolderName
        );

        folderPage.klasoruAc(
                originalFolderName
        );
    }

    @When("kullanıcı klasörü başında ve sonunda boşluk olan yeni isimle yeniden adlandırır")
    public void kullaniciKlasoruBoslukOlanYeniIsimleYenidenAdlandirir() {

        expectedTrimmedName =
                "QA Rename Trim Target " + System.currentTimeMillis();

        folderPage.renameFormunuAc();

        folderPage.renameInputDoldur(
                "   " + expectedTrimmedName + "   "
        );

        folderPage.renameKaydet();
    }

    @Then("klasör adı boşluklar olmadan görüntülenmelidir")
    public void klasorAdiBosluklarOlmadanGoruntulenmelidir() {

        if (!folderPage.klasorBasligiMi(
                expectedTrimmedName
        )) {

            throw new AssertionError(
                    "Rename sonrası klasör adı trim edilmedi!"
            );
        }
    }

    @When("kullanıcı boş isimle alt klasör oluşturmaya çalışır")
    public void kullaniciBosIsimleAltKlasorOlusturmayaCalisir() {

        folderPage.altKlasorInputunaYaz("");

        if (!folderPage.altKlasorCreateButonuDisabledMi()) {

            throw new AssertionError(
                    "Boş isim girildiğinde Create butonu disabled olmadı!"
            );
        }
    }

    @Then("yeni alt klasör oluşturulmamalıdır")
    public void yeniAltKlasorOlusturulmamali() {

        if (!folderPage.altKlasorCreateButonuDisabledMi()) {

            throw new AssertionError(
                    "Geçersiz klasör isminde Create butonu aktif!"
            );
        }
    }

    @When("kullanıcı sadece boşluk içeren isimle alt klasör oluşturmaya çalışır")
    public void kullaniciSadeceBoslukIcerenIsimleAltKlasorOlusturmayaCalisir() {

        folderPage.altKlasorInputunaYaz(
                "     "
        );

        if (!folderPage.altKlasorCreateButonuDisabledMi()) {

            throw new AssertionError(
                    "Sadece boşluk girildiğinde Create butonu disabled olmadı!"
            );
        }
    }

    @Given("admin kullanıcı rename input testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciRenameInputTestiIcinBenzersizKlasorOlusturmustur() {

        adminKullaniciKlasorlerSayfasindadir();

        originalFolderName =
                "QA Rename Input " + System.currentTimeMillis();

        folderPage.klasorOlustur(
                originalFolderName
        );

        folderPage.klasoruAc(
                originalFolderName
        );
    }

    @Then("rename alanında mevcut klasör adı bulunmalıdır")
    public void renameAlanindaMevcutKlasorAdiBulunmalidir() {

        String inputValue =
                folderPage.renameInputDegeri();

        if (!originalFolderName.equals(inputValue)) {

            throw new AssertionError(
                    "Rename input değeri beklenen klasör adı değil! "
                            + "Beklenen: "
                            + originalFolderName
                            + " | Gelen: "
                            + inputValue
            );
        }
    }
}