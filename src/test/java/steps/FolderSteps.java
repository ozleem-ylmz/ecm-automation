package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.FolderPage;
import pages.LoginPage;
import utils.BrowserManager;
import utils.TestData;

import java.util.HashMap;
import java.util.Map;

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

    /*
     * Feature dosyasındaki sabit klasör isimleri ("QA Folder 01",
     * "QA Child Folder", "QA Folder Renamed") senaryoya özel benzersiz
     * isimlere eşlenir. Eskiden bu sabit isimler koşular arasında paylaşılıyor
     * ve oluşturma/pasifleştirme "yoksa oluştur / aktifse pasifleştir"
     * şeklinde koşullu yapılıyordu: ilk koşudan sonra test edilen işlem hiç
     * yapılmadan senaryo geçiyordu (false positive). Step sınıfı Cucumber'da
     * senaryo başına yeniden oluşturulduğu için eşleme senaryoya özeldir.
     */
    private final Map<String, String> folderNames = new HashMap<>();
    private final Map<String, String> folderIds = new HashMap<>();

    private String isim(String featureName) {
        return folderNames.computeIfAbsent(featureName, TestData::unique);
    }

    /*
     * Doğrulama adımları için: bu senaryoda eşlenmiş bir isim varsa onu,
     * yoksa feature'daki ismi olduğu gibi kullanır (ör. tek karakterli "A"
     * alt klasörü bilinçli olarak gerçek adıyla oluşturuluyor).
     */
    private String beklenenIsim(String featureName) {
        return folderNames.getOrDefault(featureName, featureName);
    }

    /*
     * Ön koşul oturumu: login UI'ı login.feature'da test ediliyor. Yeni ve
     * izole context'e rol havuzundan oturum eklenir (BrowserManager.apiLogin).
     * Sayfa açılmaz; çağıran adım gideceği sayfaya doğrudan gider.
     */
    private void adminOturumuAc() {

        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext("admin@local", "123");

        loginPage = new LoginPage(BrowserManager.page);
        folderPage = new FolderPage(BrowserManager.page);
    }

    // Ön koşul klasörü API ile (klasör oluşturma UI'ı ayrı senaryolarda test ediliyor).
    private String apiKlasorOlustur(String name, String parentId) {

        String id = BrowserManager.apiCreateFolder(
                BrowserManager.context,
                BrowserManager.getAccessToken(),
                name,
                parentId
        );

        folderIds.put(name, id);
        return id;
    }

    private String klasorId(String name) {

        String id = folderIds.get(name);

        if (id == null) {
            throw new IllegalStateException(
                    "Klasör bu senaryoda API ile oluşturulmadı: " + name
            );
        }

        return id;
    }

    private void apiKlasorOlusturVeDetayinaGit(String name) {

        String id = apiKlasorOlustur(name, null);
        folderPage.klasorDetayinaGit(id, name);
    }

    @Given("admin kullanıcı ECM sistemine giriş yapmıştır")
    public void adminKullaniciEcmSistemineGirisYapmistir() {

        adminOturumuAc();

        // Login sonrası uygulamanın açtığı yer: "/" -> /folders (App.tsx).
        BrowserManager.page.navigate(BrowserManager.getBaseUrl() + "/");
        BrowserManager.page.waitForURL("**/folders");
    }

    @Given("admin kullanıcı klasörler sayfasındadır")
    public void adminKullaniciKlasorlerSayfasindadir() {

        adminOturumuAc();
        folderPage.klasorlerSayfasinaDogrudanGit();
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

        // Test edilen işlem: klasör oluşturma UI'ı. Artık koşulsuz ve
        // benzersiz isimle (eskiden isim zaten varsa oluşturma atlanıyordu).
        folderPage.klasorOlustur(isim(folderName));
    }

    @Then("{string} klasörü görüntülenmelidir")
    public void klasorGoruntulenmelidir(String folderName) {

        folderPage.klasorlerSayfasiniAc();

        String expected = beklenenIsim(folderName);

        if (!folderPage.klasorGorunuyorMu(expected)) {
            throw new AssertionError(
                    expected + " klasörü görüntülenmedi!"
            );
        }
    }

    @Given("{string} klasörü mevcuttur")
    public void klasorMevcuttur(String folderName) {

        apiKlasorOlustur(isim(folderName), null);
    }

    @When("kullanıcı {string} klasörünü açar")
    public void kullaniciKlasoruAcar(String folderName) {

        // Test edilen işlem: klasörler listesinden klasörü açmak (UI).
        // Klasör, sayfa açıldıktan sonra API ile oluşturulduğu için listeyi
        // güncel haliyle yeniden açıyoruz.
        folderPage.klasorlerSayfasinaDogrudanGit();
        folderPage.klasoruAc(isim(folderName));
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

        // Test edilen işlem alt klasör oluşturma; parent'a ID ile gidiyoruz.
        String parent = isim(parentFolder);
        folderPage.klasorDetayinaGit(klasorId(parent), parent);

        folderPage.altKlasorOlusturDetaySayfasinda(isim(childFolder));
    }

    @Then("{string} alt klasörü görüntülenmelidir")
    public void altKlasorGoruntulenmelidir(String childFolder) {

        String expected = beklenenIsim(childFolder);

        if (!folderPage.altKlasorGorunuyorMu(expected)) {
            throw new AssertionError(
                    expected + " alt klasörü görüntülenmedi!"
            );
        }
    }

    @Given("admin kullanıcı yeniden adlandırma için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciYenidenAdlandirmaIcinBenzersizBirKlasorOlusturmustur() {

        adminOturumuAc();

        renameSourceName = TestData.unique("QA Rename Source");

        apiKlasorOlusturVeDetayinaGit(renameSourceName);
    }

    @When("kullanıcı klasörü benzersiz yeni bir adla yeniden adlandırır")
    public void kullaniciKlasoruBenzersizYeniBirAdlaYenidenAdlandirir() {

        renameTargetName = TestData.unique("QA Rename Target");

        // Kaynak klasörün detay sayfasındayız (Given); test edilen işlem rename UI'ı.
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

        String name = isim("QA Folder Renamed");
        folderPage.klasorDetayinaGit(klasorId(name), name);

        // Test edilen işlem pasifleştirme; yeni klasör aktif olmalı ve
        // pasifleştirme her zaman gerçekten yapılır (eskiden koşulluydu).
        if (!folderPage.klasorSuAnAktifMi()) {
            throw new AssertionError(
                    "Ön koşul bozuk: yeni oluşturulan klasör aktif değil: " + name
            );
        }

        folderPage.klasoruPasifYap();
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

        adminOturumuAc();

        // Ön koşul: pasif klasör (oluşturma + pasifleştirme API ile; pasifleştirme
        // UI'ı "klasörü pasif hale getirebilmelidir" senaryosunda test ediliyor).
        String name = isim("QA Folder Renamed");
        String id = apiKlasorOlustur(name, null);

        BrowserManager.apiSetFolderActive(
                BrowserManager.context,
                BrowserManager.getAccessToken(),
                id,
                false
        );

        folderPage.klasorDetayinaGit(id, name);

        if (!folderPage.klasorPasifMi()) {
            throw new AssertionError(
                    "Ön koşul bozuk: klasör pasif olarak açılmadı: " + name
            );
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

        adminOturumuAc();

        String parent = isim("QA Folder 01");
        String parentId = apiKlasorOlustur(parent, null);
        apiKlasorOlustur(isim("QA Child Folder"), parentId);

        folderPage.klasorDetayinaGit(parentId, parent);
    }

    @When("kullanıcı alt klasörleri görüntüler")
    public void kullaniciAltKlasorleriGoruntuler() {

        // Alt klasörler detay sayfasında doğrudan gösteriliyor.
    }

    @Then("mevcut alt klasörler listelenmelidir")
    public void mevcutAltKlasorlerListelenmelidir() {

        if (!folderPage.altKlasorGorunuyorMu(isim("QA Child Folder"))) {

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

        adminOturumuAc();

        String parent = isim("QA Folder 01");
        String child = isim("QA Child Folder");

        String parentId = apiKlasorOlustur(parent, null);
        String childId = apiKlasorOlustur(child, parentId);

        folderPage.klasorDetayinaGit(childId, child);
    }

    @Then("klasör breadcrumb bilgisi görüntülenmelidir")
    public void klasorBreadcrumbBilgisiGoruntulenmelidir() {

        if (!folderPage.breadcrumbGorunuyorMu(isim("QA Folder 01"))) {

            throw new AssertionError(
                    "Klasör breadcrumb bilgisi görüntülenmedi! Beklenen parent: "
                            + isim("QA Folder 01")
            );
        }
    }

    // ---------------------------------------------------------
    // FOLDER BATCH 2
    // ---------------------------------------------------------

    @Given("admin kullanıcı duplicate testi için benzersiz bir klasör oluşturmuştur")
    public void adminKullaniciDuplicateTestiIcinBenzersizKlasorOlusturmustur() {

        adminOturumuAc();

        testFolderName = TestData.unique("QA Duplicate Root");

        apiKlasorOlustur(testFolderName, null);
    }

    @When("kullanıcı aynı isimde klasörü tekrar oluşturmaya çalışır")
    public void kullaniciAyniIsimdeKlasoruTekrarOlusturmayaCalisir() {

        // Test edilen işlem: /folders sayfasındaki oluşturma formu (UI).
        folderPage.klasorlerSayfasinaDogrudanGit();

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

        adminOturumuAc();

        testFolderName = TestData.unique("QA Duplicate Parent");
        testChildName = TestData.unique("QA Duplicate Child");

        String parentId = apiKlasorOlustur(testFolderName, null);
        apiKlasorOlustur(testChildName, parentId);

        folderPage.klasorDetayinaGit(parentId, testFolderName);

        if (!folderPage.altKlasorGorunuyorMu(testChildName)) {
            throw new AssertionError(
                    "Ön koşul bozuk: child klasör parent detayında görünmüyor: " + testChildName
            );
        }
    }

    @When("kullanıcı aynı child klasörü tekrar oluşturmaya çalışır")
    public void kullaniciAyniChildKlasoruTekrarOlusturmayaCalisir() {

        folderPage.altKlasorOlusturBeklemeden(
                testChildName
        );
    }

    @Given("admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır")
    public void adminKullaniciBenzersizBirParentKlasorunDetaySayfasindadir() {

        adminOturumuAc();

        testFolderName = TestData.unique("QA Parent");

        apiKlasorOlusturVeDetayinaGit(testFolderName);
    }

    @When("kullanıcı {string} isimli tek karakterli alt klasör oluşturur")
    public void kullaniciTekKarakterliAltKlasorOlusturur(
            String childFolder
    ) {

        // Parent benzersiz olduğu için tek karakterli isim parent içinde benzersiz.
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

        adminOturumuAc();

        testFolderName = TestData.unique("QA Breadcrumb Parent");
        testChildName = TestData.unique("QA Breadcrumb Child");

        String parentId = apiKlasorOlustur(testFolderName, null);
        String childId = apiKlasorOlustur(testChildName, parentId);

        // Doğrudan navigasyon tam sayfa yüklemesidir; eski akıştaki ayrı
        // reload() bu yüzden gerekmiyor.
        folderPage.klasorDetayinaGit(childId, testChildName);
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

        // Yeni oluşturulan klasör aktif olmalı; pasifleştirme koşulsuz yapılır.
        if (!folderPage.klasorSuAnAktifMi()) {
            throw new AssertionError(
                    "Ön koşul bozuk: yeni oluşturulan klasör aktif değil: " + testFolderName
            );
        }

        folderPage.klasoruPasifYap();
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

        testChildName = TestData.unique("QA Inactive Child");

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

        if (!folderPage.klasorSuAnAktifMi()) {
            throw new AssertionError(
                    "Ön koşul bozuk: yeni oluşturulan klasör aktif değil: " + testFolderName
            );
        }

        folderPage.klasoruPasifYap();

        folderPage.klasoruAktifYap();
    }

    @Then("aktif klasör altında yeni alt klasör oluşturulabilmelidir")
    public void aktifKlasorAltindaYeniAltKlasorOlusturulabilmelidir() {

        testChildName = TestData.unique("QA Reactivated Child");

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

        adminOturumuAc();

        originalFolderName = TestData.unique("QA Rename Cancel");

        temporaryFolderName = TestData.unique("QA Should Not Save");

        apiKlasorOlusturVeDetayinaGit(originalFolderName);
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

        adminOturumuAc();

        originalFolderName = TestData.unique("QA Same Rename");

        apiKlasorOlusturVeDetayinaGit(originalFolderName);
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

        adminOturumuAc();

        testFolderName = TestData.unique("QA Trim Parent");

        apiKlasorOlusturVeDetayinaGit(testFolderName);
    }

    @When("kullanıcı başında ve sonunda boşluk olan benzersiz bir alt klasör oluşturur")
    public void kullaniciBoslukOlanBenzersizAltKlasorOlusturur() {

        expectedTrimmedName = TestData.unique("QA Trim Child");

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

        adminOturumuAc();

        originalFolderName = TestData.unique("QA Rename Trim Source");

        apiKlasorOlusturVeDetayinaGit(originalFolderName);
    }

    @When("kullanıcı klasörü başında ve sonunda boşluk olan yeni isimle yeniden adlandırır")
    public void kullaniciKlasoruBoslukOlanYeniIsimleYenidenAdlandirir() {

        expectedTrimmedName = TestData.unique("QA Rename Trim Target");

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

        adminOturumuAc();

        originalFolderName = TestData.unique("QA Rename Input");

        apiKlasorOlusturVeDetayinaGit(originalFolderName);
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
