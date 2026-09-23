package pages;

import utils.BrowserManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.PlaywrightException;


import java.nio.file.Paths;

import java.util.regex.Pattern;
public class DocumentsPage {

    private final Page page;

    public DocumentsPage(Page page) {
        this.page = page;
    }

    // =========================
    // DOCUMENTS SAYFASI
    // =========================

    public void documentsSayfasiniAc() {

        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("Documents")
                        .setExact(true)
        ).first().click();

        page.waitForURL("**/documents");
    }

    public boolean documentsSayfasindaMi() {
        return page.url().endsWith("/documents");
    }

    // =========================
    // UPLOAD WITHOUT CLASS
    // =========================

    public void uploadWithoutClassSayfasiniAc() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Upload without class")
                        .setExact(true)
        ).click();

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Upload without class")
                        .setExact(true)
        ).waitFor();
    }

    public boolean uploadWithoutClassSayfasindaMi() {

        try {

            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName("Upload without class")
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // DOCUMENT UPLOAD
    // =========================

    public void belgeBasligiGir(String title) {

        page.getByPlaceholder(
                "e.g. Q3 Budget Spreadsheet"
        ).fill(title);
    }

    public void testDosyasiSec() {

        page.locator("input[type='file']")
                .setInputFiles(
                        Paths.get(
                                "src/test/resources/test-files/test-document.txt"
                        )
                );
    }

    public void uploadDocumentButonunaTikla() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Upload document")
                        .setExact(true)
        ).click();
    }

    public void belgeYukle(String title) {

        belgeBasligiGir(title);
        testDosyasiSec();
        uploadDocumentButonunaTikla();

        /*
         * /documents/new/unclassified adresi de
         * documents wildcard desenine uyduğu için
         * doğrudan UUID'li gerçek detail URL'sini bekliyoruz.
         */

        long bitisZamani =
                System.currentTimeMillis() + 20000;

        while (System.currentTimeMillis() < bitisZamani) {

            if (belgeDetaySayfasindaMi()) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Belge yüklendikten sonra detay sayfası açılmadı! URL: "
                        + page.url()
        );
    }

    // =========================
    // DOCUMENT DETAIL
    // =========================

    public boolean belgeDetaySayfasindaMi() {

        return page.url().matches(
                ".*/documents/[0-9a-fA-F-]{36}.*"
        );
    }

    public boolean belgeBasligiGorunuyorMu(String title) {

        try {

            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(title)
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // DOCUMENT LIST
    // =========================

    public boolean belgeListedeGorunuyorMu(String title) {

        try {

            page.getByText(
                    title,
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // FORM VALIDATION
    // =========================

    public boolean uploadDocumentButonuDisabledMi() {

        Locator uploadButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Upload document")
                        .setExact(true)
        );

        uploadButton.waitFor();

        return uploadButton.isDisabled();
    }

    // =========================
    // SOFT DELETE
    // =========================

    public void belgeyiSoftDeleteYap() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        ).click();

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Delete this document?")
                        .setExact(true)
        ).waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        ).last().click();

        page.waitForURL(
                "**/documents",
                new Page.WaitForURLOptions()
                        .setTimeout(10000)
        );
    }

    public boolean belgeDeletedMi(String title) {

        try {

            Locator titleLocator = page.getByText(
                    title,
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first();

            titleLocator.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            Locator documentRow =
                    titleLocator.locator("xpath=ancestor::tr[1]");

            documentRow.getByText(
                    "Deleted",
                    new Locator.GetByTextOptions()
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // RESTORE
    // =========================

    public void belgeyiRestoreEt(String title) {

        /*
         * Soft delete sonrasında Documents listesindeyiz.
         * Önce kendi oluşturduğumuz belgeyi tekrar açıyoruz.
         */

        Locator titleLocator = page.getByText(
                title,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first();

        titleLocator.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        titleLocator.click();

        long detailBitisZamani =
                System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < detailBitisZamani) {

            if (belgeDetaySayfasindaMi()) {
                break;
            }

            page.waitForTimeout(250);
        }

        if (!belgeDetaySayfasindaMi()) {

            throw new AssertionError(
                    "Soft delete yapılan belgenin detay sayfası açılamadı! URL: "
                            + page.url()
            );
        }

        Locator restoreButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Restore")
                        .setExact(true)
        );

        restoreButton.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        restoreButton.click();

        long restoreBitisZamani =
                System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < restoreBitisZamani) {

            if (!detailSayfasindaDeletedGorunuyorMu()) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Restore işlemi sonrasında Deleted durumu kaybolmadı!"
        );
    }

    // =========================
    // RESTORE KONTROLLERİ
    // =========================

    public boolean detailSayfasindaDeletedGorunuyorMu() {

        return page.getByText(
                "Deleted",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).count() > 0;
    }

    public boolean belgeDeletedDegilMi() {

        return belgeDetaySayfasindaMi()
                && !detailSayfasindaDeletedGorunuyorMu();
    }

    public boolean belgeDetaySayfasiAktifMi() {

        return belgeDetaySayfasindaMi()
                && !detailSayfasindaDeletedGorunuyorMu();
    }

    // =========================
    // DOCUMENT LIST -> DETAIL
    // =========================

    public void belgeyiListedenAc(String title) {

        Locator belge = page.getByText(
                title,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first();

        belge.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        belge.click();

        long bitisZamani =
                System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < bitisZamani) {

            if (belgeDetaySayfasindaMi()) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Belge listeden açıldı ancak detay sayfasına gidilemedi! URL: "
                        + page.url()
        );
    }

    // =========================
    // DOCUMENT STATUS
    // =========================

    public boolean statusBilgisiGorunuyorMu() {

        try {

            Locator updateStatusButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Update status")
                            .setExact(true)
            );

            updateStatusButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            Locator statusSelect = page.locator("select").filter(
                    new Locator.FilterOptions()
                            .setHasText("Draft")
            ).first();

            statusSelect.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return updateStatusButton.isVisible()
                    && statusSelect.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Status kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public void statusuUnderReviewYap() {

        Locator statusSelect = page.locator("select").filter(
                new Locator.FilterOptions()
                        .setHasText("Draft")
        ).first();

        statusSelect.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        /*
         * ECM'deki gerçek option value:
         * Draft        -> draft
         * Under review -> under_review
         * Published    -> published
         * Archived     -> archived
         */
        statusSelect.selectOption("under_review");

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Update status")
                        .setExact(true)
        ).click();

        long bitisZamani =
                System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < bitisZamani) {

            if (statusUnderReviewMu()) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Belge statusu Under review olarak güncellenmedi!"
        );
    }

    public boolean statusUnderReviewMu() {

        try {

            /*
             * Status değiştikten sonra artık locator'ı "Draft"
             * üzerinden aramıyoruz.
             *
             * Status alanını, aynı Admin actions bölümündeki
             * Update status butonuna göre buluyoruz.
             */
            Locator updateStatusButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Update status")
                            .setExact(true)
            );

            updateStatusButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            Locator statusSelect = page.locator("select").filter(
                    new Locator.FilterOptions()
                            .setHas(
                                    page.locator(
                                            "option[value='under_review']"
                                    )
                            )
            ).first();

            statusSelect.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return statusSelect.inputValue()
                    .equals("under_review");

        } catch (Exception e) {

            System.out.println(
                    "Under review status kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean statusDraftMi() {

        try {

            Locator updateStatusButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Update status")
                            .setExact(true)
            );

            updateStatusButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            Locator statusSelect = page.locator("select").filter(
                    new Locator.FilterOptions()
                            .setHas(
                                    page.locator(
                                            "option[value='draft']"
                                    )
                            )
            ).first();

            statusSelect.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return statusSelect.inputValue().equals("draft");

        } catch (Exception e) {

            System.out.println(
                    "Draft status kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    // =========================
// DOCUMENT DOWNLOAD
// =========================

    public Download belgeyiIndir() {

        Download download = page.waitForDownload(
                () -> page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                                .setName("Download")
                                .setExact(true)
                ).click()
        );

        return download;
    }

    public boolean downloadBasariliMi(Download download) {

        if (download == null) {
            return false;
        }

        try {

            // Download sırasında Playwright bir hata bildirmiş mi?
            if (download.failure() != null) {
                System.out.println(
                        "Download hatası: " + download.failure()
                );
                return false;
            }

            // Playwright'ın indirdiği geçici dosya gerçekten var mı?
            return java.nio.file.Files.exists(download.path())
                    && java.nio.file.Files.size(download.path()) > 0;

        } catch (Exception e) {

            System.out.println(
                    "Download kontrol hatası: " + e.getMessage()
            );

            return false;
        }
    }
    // =========================
// DOCUMENT PREVIEW
// =========================

    // =========================
// DOCUMENT PREVIEW
// =========================

    // =========================
// DOCUMENT PREVIEW
// =========================

    public boolean txtPreviewGorunuyorMu() {

        try {

            Locator previewButton = page.getByText(
                    "Preview",
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first();

            previewButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            previewButton.click();

            Locator previewContent = page.locator("pre").filter(
                    new Locator.FilterOptions()
                            .setHasText(
                                    "ECM UI automation test document."
                            )
            );

            previewContent.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return previewContent.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "TXT preview kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }
    // =========================
// DOCUMENT NEW VERSION
// =========================

    public void yeniVersiyonSayfasiniAc() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("New version")
                        .setExact(true)
        ).click();

        // "Save new version" hem heading hem button olduğu için
        // özellikle heading'i seçiyoruz.
        Locator heading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Save new version")
                        .setExact(true)
        );

        heading.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
        );
    }

    public void versionTipiniSec(String versionType) {

        Locator radio = page.getByRole(
                AriaRole.RADIO,
                new Page.GetByRoleOptions()
                        .setName(versionType)
        );

        radio.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        radio.check();
    }

    public void yeniVersiyonDosyasiniSec() {

        page.locator("input[type='file']")
                .setInputFiles(
                        java.nio.file.Paths.get(
                                "src/test/resources/test-files/test-document.txt"
                        )
                );
    }

    public void yeniVersiyonuKaydet() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Save new version")
                        .setExact(true)
        ).click();

        // Kayıt tamamlanınca tekrar document detail'e
        // dönmesini bekliyoruz.
        page.waitForURL(
                url -> url.matches(
                        ".*/documents/[0-9a-fA-F-]{36}$"
                ),
                new Page.WaitForURLOptions()
                        .setTimeout(20000)
        );
    }

    public boolean belgeVersiyonuMu(String expectedVersion) {

        try {

            // Yeni version kaydedildikten sonra
            // document detail sayfasındaki Current version
            // bölümünü buluyoruz.
            Locator currentVersionHeading = page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName("Current version")
                            .setExact(true)
            );

            currentVersionHeading.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            // Current version başlığının bulunduğu container içerisinde
            // beklenen version değerini arıyoruz.
            Locator currentVersionContainer =
                    currentVersionHeading.locator("xpath=..");

            Locator version = currentVersionContainer.getByText(
                    expectedVersion,
                    new Locator.GetByTextOptions()
                            .setExact(true)
            );

            version.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return version.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Current version kontrol hatası: "
                            + expectedVersion
                            + " | "
                            + e.getMessage()
            );

            return false;
        }
    }
    // =========================
// DOCUMENT CHECKOUT / LOCK
// =========================

    public boolean checkoutDurumundaDegilMi() {

        try {

            Locator notCheckedOut = page.getByText(
                    "Not checked out.",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            notCheckedOut.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return notCheckedOut.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Checkout durum kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public void belgeyiCheckoutYap() {

        Locator checkoutButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Check out")
                        .setExact(true)
        );

        checkoutButton.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
        );

        checkoutButton.click();
    }

    public boolean checkInButonuGorunuyorMu() {

        try {

            Locator checkInButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Check in")
                            .setExact(true)
            );

            checkInButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return checkInButton.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Check in butonu kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean checkoutDurumundaMi() {

        return checkInButonuGorunuyorMu();
    }
    public void belgeyiCheckInYap() {

        Locator checkInButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Check in")
                        .setExact(true)
        );

        checkInButton.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
        );

        checkInButton.click();

        // Check-in tamamlanınca tekrar
        // "Not checked out." durumuna dönmesini bekliyoruz.
        Locator notCheckedOut = page.getByText(
                "Not checked out.",
                new Page.GetByTextOptions()
                        .setExact(true)
        );

        notCheckedOut.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
        );
    }

    public boolean checkOutButonuGorunuyorMu() {

        try {

            Locator checkOutButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Check out")
                            .setExact(true)
            );

            checkOutButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return checkOutButton.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Check out butonu kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }
    public boolean notCheckedOutGorunmuyorMu() {

        Locator notCheckedOut = page.getByText(
                "Not checked out.",
                new Page.GetByTextOptions()
                        .setExact(true)
        );

        return notCheckedOut.count() == 0
                || !notCheckedOut.isVisible();
    }

    public boolean checkOutButonuGorunmuyorMu() {

        Locator checkOutButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Check out")
                        .setExact(true)
        );

        return checkOutButton.count() == 0
                || !checkOutButton.isVisible();
    }

    public boolean checkedOutByYouGorunuyorMu() {

        try {

            Locator checkedOutByYou = page.getByText(
                    "Checked out by you",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            checkedOutByYou.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return checkedOutByYou.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Checked out by you kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }
    // =========================
    // MULTI-USER DOCUMENT LOCK
    // =========================

    public void managerLogin(Page managerPage) {

        managerPage.navigate(
                BrowserManager.getBaseUrl() + "/login"
        );

        managerPage.locator("input[type='email']")
                .fill("manager@local");

        managerPage.locator("input[type='password']")
                .fill("123");

        managerPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Sign in")
                        .setExact(true)
        ).click();

        managerPage.waitForURL(
                "**/folders",
                new Page.WaitForURLOptions()
                        .setTimeout(15000)
        );
    }

    public void belgeyiBasligaGoreAraVeAc(
            Page managerPage,
            String title
    ) {

        managerPage.navigate(
                BrowserManager.getBaseUrl() + "/documents"
        );

        // Documents sayfasının yüklenmesini bekle
        managerPage.waitForURL(
                "**/documents",
                new Page.WaitForURLOptions()
                        .setTimeout(15000)
        );

        Locator documentLink = managerPage.getByText(
                title,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first();

        documentLink.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(30000)
        );

        documentLink.click();

        managerPage.waitForURL(
                url -> url.matches(
                        ".*/documents/[0-9a-fA-F-]{36}$"
                ),
                new Page.WaitForURLOptions()
                        .setTimeout(15000)
        );
    }

    public boolean checkedOutByAdminGorunuyorMu(
            Page managerPage
    ) {

        try {

            Locator lockInfo = managerPage.getByText(
                    "Checked out by Admin",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            lockInfo.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return lockInfo.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Manager lock kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean readOnlyGorunuyorMu(
            Page managerPage
    ) {

        try {

            Locator readOnly = managerPage.getByText(
                    "Read-only",
                    new Page.GetByTextOptions()
                            .setExact(false)
            ).first();

            readOnly.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return readOnly.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Read-only kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean newVersionButonuGorunmuyorMu(
            Page managerPage
    ) {

        Locator button = managerPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("New version")
                        .setExact(true)
        );

        return button.count() == 0
                || !button.isVisible();
    }

    public boolean downloadButonuGorunuyorMu(
            Page managerPage
    ) {

        try {

            Locator button = managerPage.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Download")
                            .setExact(true)
            );

            button.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return button.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Manager Download butonu kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public boolean previewButonuGorunuyorMu(
            Page managerPage
    ) {

        try {

            Locator button = managerPage.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Preview")
                            .setExact(true)
            );

            button.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );

            return button.isVisible();

        } catch (Exception e) {

            System.out.println(
                    "Manager Preview butonu kontrol hatası: "
                            + e.getMessage()
            );

            return false;
        }
    }
    // =========================
// CRITICAL LOCK TESTS
// =========================
    public boolean classifyButonuGorunmuyorMu(Page targetPage) {
        return targetPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Classify")
                        .setExact(true)
        ).count() == 0;
    }

    public boolean tagDegistirmeKullanilamiyorMu(Page targetPage) {

        Locator tagButtons = targetPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Add tag")
                        .setExact(true)
        );

        return tagButtons.count() == 0
                || !tagButtons.first().isVisible()
                || tagButtons.first().isDisabled();
    }

    public void belgeDetaySayfasiniYenile() {
        page.reload();
        page.waitForLoadState();

        long end = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < end) {
            if (belgeDetaySayfasindaMi()
                    && page.getByRole(AriaRole.HEADING).count() > 0) {
                return;
            }
            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Refresh sonrasında belge detay sayfası hazır olmadı! URL: " + page.url()
        );
    }

    public boolean newVersionButonuGorunuyorMu() {
        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("New version")
                        .setExact(true)
        ).isVisible();
    }
    public boolean checkOutButonuGorunuyorMu(Page targetPage) {

        Locator checkOutButton = targetPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Check out")
                        .setExact(true)
        );

        return checkOutButton.count() > 0
                && checkOutButton.first().isVisible();
    }
    // =========================
// DELETE / RESTORE INTEGRITY
// =========================

    public boolean deleteButonuGorunmuyorMu() {

        Locator deleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        );

        return deleteButton.count() == 0
                || !deleteButton.first().isVisible();
    }

    public boolean deleteButonuGorunuyorMu() {

        Locator deleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        );

        return deleteButton.count() > 0
                && deleteButton.first().isVisible();
    }
    public void checkoutEdilmisBelgeyiSilmeyiDene() {

        Locator deleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        );

        deleteButton.click();

        // Confirmation ekranı/modalı açılırsa Delete'i onayla
        Locator confirmDelete = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        );

        if (confirmDelete.count() > 0 && confirmDelete.last().isVisible()) {
            confirmDelete.last().click();
        }

        page.waitForTimeout(1000);
    }

    public boolean checkoutEdilmisBelgeHalaMevcutMu() {

        // Silme reddedildiyse belge detay sayfasında kalmamız
        // ve checkout bilgisinin korunması bekleniyor.
        return page.url().contains("/documents/")
                && checkoutDurumundaMi();
    }
    // =========================
// RESTORE DATA INTEGRITY
// =========================

    public String mevcutBelgeBasliginiAl() {

        return page.locator("h1").first().innerText().trim();
    }

    public String mevcutBelgeVersiyonunuAl() {

        Locator versionContainer = page.getByText(
                "Current version",
                new Page.GetByTextOptions().setExact(true)
        ).locator("..");

        return versionContainer.innerText();
    }

    public boolean belgeBasligiEsitMi(String expectedTitle) {

        return page.getByText(
                expectedTitle,
                new Page.GetByTextOptions().setExact(true)
        ).count() > 0;
    }

    public boolean belgeVersiyonuKorunduMu(String expectedVersion) {

        Locator versionContainer = page.getByText(
                "Current version",
                new Page.GetByTextOptions().setExact(true)
        ).locator("..");

        return versionContainer.innerText().equals(expectedVersion);
    }

    public void restoreEdilenBelgeninPreviewiniAc() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Preview")
                        .setExact(true)
        ).click();
    }

    public boolean restoreEdilenTxtIcerigiDogruMu() {

        return page.locator("pre")
                .getByText(
                        "ECM UI automation test document.",
                        new Locator.GetByTextOptions()
                                .setExact(false)
                )
                .isVisible();
    }
    public boolean softDeleteEdilmisBelgeAktifDetayOlarakGoruntulenmiyorMu() {

        // Soft-delete edilmiş belge eski URL'den açıldığında
        // normal/aktif belge işlemlerinin kullanılabilir olmaması gerekir.
        Locator deletedBadge = page.getByText(
                "Deleted",
                new Page.GetByTextOptions().setExact(true)
        );

        return deletedBadge.count() > 0
                && deletedBadge.first().isVisible();
    }
    // =========================
// STATUS INTEGRITY
// =========================

    public boolean statusUnderReviewGorunuyorMu() {
        return page.getByText(
                "Under review",
                new Page.GetByTextOptions().setExact(true)
        ).count() > 0;
    }

    public boolean deletedBelgedeStatusUnderReviewGorunuyorMu() {
        Locator deleted = page.getByText(
                "Deleted",
                new Page.GetByTextOptions().setExact(true)
        );

        Locator underReview = page.getByText(
                "Under review",
                new Page.GetByTextOptions().setExact(true)
        );

        return deleted.count() > 0
                && deleted.first().isVisible()
                && underReview.count() > 0;
    }
// =========================
// HARD DELETE
// =========================

    // =========================
    // HARD DELETE
    // =========================

    public void hardDeletePenceresiniAc() {

        Locator hardDeleteButton = page.locator("button")
                .filter(
                        new Locator.FilterOptions()
                                .setHasText("Hard delete")
                );

        hardDeleteButton.first().click();

        page.getByText(
                "Permanently delete document?",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).waitFor();
    }

    public boolean hardDeletePenceresiGorunuyorMu() {

        Locator modalTitle = page.getByText(
                "Permanently delete document?",
                new Page.GetByTextOptions()
                        .setExact(true)
        );

        return modalTitle.count() > 0
                && modalTitle.first().isVisible();
    }

    public boolean permanentlyDeleteButonuDisabledMi() {

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Permanently delete")
                        .setExact(true)
        );

        return button.isDisabled();
    }

    public void hardDeleteOnayMetniGir(String confirmation) {

        Locator confirmationInput =
                page.locator("input:visible");

        if (confirmationInput.count() == 0) {
            throw new AssertionError(
                    "Hard delete modalındaki DELETE onay inputu bulunamadı!"
            );
        }

        confirmationInput.last().fill(confirmation);
    }

    public boolean permanentlyDeleteButonuAktifMi() {

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Permanently delete")
                        .setExact(true)
        );

        return button.isEnabled();
    }

    public void permanentlyDeleteYap() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Permanently delete")
                        .setExact(true)
        ).click();

        page.waitForLoadState();
    }

    public boolean hardDeleteEdilmisBelgeDetaydaGorunmuyorMu() {

        Locator deletedBadge = page.getByText(
                "Deleted",
                new Page.GetByTextOptions()
                        .setExact(true)
        );

        Locator hardDeleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Hard delete")
                        .setExact(false)
        );

        return deletedBadge.count() == 0
                && hardDeleteButton.count() == 0;
    }

    // =========================
    // BATCH 8 - STATE CONTROLS
    // =========================

    public boolean hardDeleteButonuGorunuyorMu() {

        Locator hardDeleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Hard delete")
                        .setExact(false)
        );

        if (hardDeleteButton.count() == 0) {
            System.out.println(
                    "DEBUG: Restore sonrası DOM'da Hard delete butonu bulunamadı."
            );
            return false;
        }

        boolean visible =
                hardDeleteButton.first().isVisible();

        System.out.println(
                "DEBUG: Restore sonrası Hard delete count = "
                        + hardDeleteButton.count()
                        + ", visible = "
                        + visible
        );

        return visible;
    }
    // =========================
    // BATCH 9 - LOCK CONTROLS
    // =========================

    public boolean managerRenameYapamiyorMu(Page managerPage) {

        Locator button = managerPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Rename")
                        .setExact(true)
        );

        return button.count() == 0
                || !button.first().isVisible()
                || button.first().isDisabled();
    }

    public boolean managerStatusDegistiremiyorMu(Page managerPage) {

        Locator button = managerPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Update status")
                        .setExact(true)
        );

        return button.count() == 0
                || !button.first().isVisible()
                || button.first().isDisabled();
    }

    public boolean managerSoftDeleteYapamiyorMu(Page managerPage) {

        Locator button = managerPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete")
                        .setExact(true)
        );

        return button.count() == 0
                || !button.first().isVisible()
                || button.first().isDisabled();
    }

// =====================================================
    // BATCH 11 - UI VERIFIED: CLASSIFY / MOVE / TAG
    // =====================================================

    public boolean classifyButonuGorunuyorMu() {
        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Classify").setExact(true)
        );
        return button.count() > 0 && button.first().isVisible();
    }

    public void classifyButonunaTikla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Classify").setExact(true)
        ).click();
    }

    public boolean classifyEkraniGorunuyorMu() {
        return page.getByRole(AriaRole.DIALOG).count() > 0
                || page.url().toLowerCase().contains("classif")
                || page.getByText("Classify", new Page.GetByTextOptions().setExact(true)).count() > 1;
    }

    public void classifyIsleminiIptalEt() {
        Locator cancel = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Cancel").setExact(true)
        );
        if (cancel.count() > 0) {
            cancel.last().click();
            return;
        }
        page.goBack();
    }

    public boolean belgeUnclassifiedMi() {
        Locator value = page.getByText(
                "Unclassified",
                new Page.GetByTextOptions().setExact(false)
        );
        return value.count() > 0 && value.first().isVisible();
    }

    // Ekran görüntüsünde doğrulandı: bu bir BUTTON değil LINK.
    public boolean moveButonuGorunuyorMu() {
        Locator moveButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(
                        Pattern.compile("^Move to folder")
                )
        );
        return moveButton.count() > 0 && moveButton.first().isVisible();
    }

    public void moveButonunaTikla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(
                        Pattern.compile("^Move to folder")
                )
        ).first().click();
    }

    public boolean movePenceresiGorunuyorMu() {
        Locator title = page.getByText(
                "Move to folder",
                new Page.GetByTextOptions().setExact(true)
        );
        return title.count() > 0 && title.first().isVisible();
    }

    public void moveIsleminiIptalEt() {
        Locator cancel = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Cancel").setExact(true)
        );
        if (cancel.count() == 0) {
            throw new AssertionError("Move ekranında Cancel butonu bulunamadı!");
        }
        cancel.last().click();
    }

    // Ekran görüntüsünde doğrulandı: Add tag button yok, doğrudan input var.
    public boolean tagGirisAlaniGorunuyorMu() {
        Locator tagInput = page.locator("input[placeholder^='Type to add a tag']");
        return tagInput.count() > 0 && tagInput.first().isVisible();
    }

    public boolean tagGirisAlaniGorunmuyorMu(Page targetPage) {
        Locator input = targetPage.getByPlaceholder("Type to add a tag...");
        return input.count() == 0 || !input.first().isVisible();
    }

    public boolean downloadButonuGorunuyorMu() {
        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Download").setExact(true)
        );
        return button.count() > 0 && button.first().isVisible();
    }

    public boolean belgeDetaySayfasindaKalindiMi() {
        return belgeDetaySayfasindaMi();
    }


    public boolean moveButonuGorunmuyorMu(Page targetPage) {
        Locator moveButton = targetPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(
                        Pattern.compile("^Move to folder")
                )
        );
        return moveButton.count() == 0 || !moveButton.first().isVisible();
    }


    // =========================
    // DOCUMENTS BATCH 12
    // Real Move execution / persistence
    // =========================

    public void hedefKlasorOlustur(String folderName) {
        page.navigate(BrowserManager.getBaseUrl() + "/folders");
        page.waitForLoadState();

        Locator nameInput = page.getByPlaceholder("e.g. Finance");
        if (nameInput.count() == 0) {
            throw new AssertionError("Folder name alanı bulunamadı!");
        }

        nameInput.fill(folderName);
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Create folder").setExact(true)
        ).click();

        Locator folderLink = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName(folderName).setExact(true)
        );
        folderLink.waitFor();
    }

    public void belgeDetayAdresiniAc(String url) {
        page.navigate(url);
        page.waitForLoadState();
    }

    public void moveHedefKlasorSec(String folderName) {
        Locator select = page.locator("select").last();
        if (select.count() == 0) {
            throw new AssertionError("Move ekranında Destination select bulunamadı!");
        }

        Locator options = select.locator("option");
        String optionValue = null;

        for (int i = 0; i < options.count(); i++) {
            Locator option = options.nth(i);
            String text = option.textContent();
            if (text != null) {
                String optionText = text.trim();

                // Destination seçenekleri tam yol gösterebilir:
                // "Root / Parent / QA Move Folder ..."
                if (optionText.equals(folderName)
                        || optionText.endsWith("/ " + folderName)
                        || optionText.endsWith("/" + folderName)) {
                    optionValue = option.getAttribute("value");
                    break;
                }
            }
        }

        if (optionValue == null) {
            throw new AssertionError("Move hedef klasörü listede bulunamadı: " + folderName);
        }

        select.selectOption(optionValue);
    }

    public boolean moveHedefKlasorSeciliMi(String folderName) {
        Locator select = page.locator("select").last();
        if (select.count() == 0) {
            return false;
        }

        String selectedValue = select.inputValue();
        Locator options = select.locator("option");

        for (int i = 0; i < options.count(); i++) {
            Locator option = options.nth(i);
            String value = option.getAttribute("value");
            String text = option.textContent();

            if (selectedValue.equals(value) && text != null) {
                String optionText = text.trim();

                // Seçili option metni de tam klasör yolu olabilir.
                if (optionText.equals(folderName)
                        || optionText.endsWith("/ " + folderName)
                        || optionText.endsWith("/" + folderName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void moveIsleminiOnayla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Move").setExact(true)
        ).click();
    }

    public boolean movePenceresiKapandiMi() {
        Locator title = page.getByText(
                "Move to folder",
                new Page.GetByTextOptions().setExact(true)
        );
        return title.count() == 0 || !title.first().isVisible();
    }

    public boolean belgeDetayUrlAyniMi(String expectedUrl) {
        return page.url().equals(expectedUrl);
    }

    public boolean mevcutKlasorAdiGorunuyorMu(String folderName) {
        Locator exactText = page.getByText(
                folderName,
                new Page.GetByTextOptions().setExact(true)
        );
        return exactText.count() > 0 && exactText.first().isVisible();
    }


    // =========================
    // DOCUMENTS BATCH 13
    // COPY / DESTINATION / INDEPENDENCE
    // =========================

    public boolean copyButonuGorunuyorMu() {
        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        return button.count() > 0 && button.first().isVisible();
    }

    public void copyPenceresiniAc() {
        Locator buttons = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        if (buttons.count() == 0) {
            throw new AssertionError("Copy butonu bulunamadı!");
        }
        buttons.first().click();

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        ).waitFor(new Locator.WaitForOptions().setTimeout(5000));
    }

    public boolean copyPenceresiGorunuyorMu() {
        Locator heading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        );
        return heading.count() > 0 && heading.first().isVisible();
    }

    public boolean copyAciklamasiGorunuyorMu() {
        return page.getByText(
                "Creates an independent draft copy of the current version and records a copied_from lineage link.",
                new Page.GetByTextOptions().setExact(true)
        ).count() > 0;
    }

    private Locator copyTitleInput() {
        Locator label = page.getByText(
                "New title",
                new Page.GetByTextOptions().setExact(true)
        );
        if (label.count() > 0) {
            Locator input = label.first().locator("xpath=following::input[1]");
            if (input.count() > 0) return input.first();
        }
        return page.locator("input").filter(
                new Locator.FilterOptions().setHasNot(page.locator("[type='hidden']"))
        ).last();
    }

    private Locator copyDestinationSelect() {
        Locator heading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        );
        Locator dialog = heading.locator("xpath=ancestor::*[self::div or self::section][.//select][1]");
        if (dialog.count() > 0 && dialog.locator("select").count() > 0) {
            return dialog.locator("select").first();
        }
        return page.locator("select").last();
    }

    public String copyBaslangicBasligi() {
        return copyTitleInput().inputValue();
    }

    public boolean copyBasligiVarsayilanMi(String sourceTitle) {
        return copyBaslangicBasligi().equals(sourceTitle + " (copy)");
    }

    public void copyBasligiGir(String title) {
        copyTitleInput().fill(title);
    }

    public boolean copyBasligiMi(String expected) {
        return copyTitleInput().inputValue().equals(expected);
    }

    public boolean copyDestinationRootMu() {
        Locator select = copyDestinationSelect();
        String text = select.locator("option:checked").innerText().trim();
        return text.equals("Root") || text.endsWith("/ Root") || text.endsWith("/Root");
    }

    public void copyHedefKlasorSec(String folderName) {
        Locator select = copyDestinationSelect();
        Locator options = select.locator("option");

        for (int i = 0; i < options.count(); i++) {
            Locator option = options.nth(i);
            String text = option.innerText().trim();
            if (text.equals(folderName)
                    || text.endsWith("/ " + folderName)
                    || text.endsWith("/" + folderName)) {
                select.selectOption(option.getAttribute("value"));
                return;
            }
        }
        throw new AssertionError("Copy hedef klasörü listede bulunamadı: " + folderName);
    }

    public boolean copyHedefKlasorSeciliMi(String folderName) {
        String text = copyDestinationSelect().locator("option:checked").innerText().trim();
        return text.equals(folderName)
                || text.endsWith("/ " + folderName)
                || text.endsWith("/" + folderName);
    }

    public void copyIsleminiIptalEt() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Cancel").setExact(true)
        ).click();
    }

    public void copyPenceresiniXileKapat() {
        Locator heading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        );
        Locator dialog = heading.locator("xpath=ancestor::*[self::div or self::section][.//button][1]");
        Locator close = dialog.locator("button").filter(
                new Locator.FilterOptions().setHas(page.locator("svg"))
        ).first();
        if (close.count() == 0) {
            close = page.locator("button").filter(
                    new Locator.FilterOptions().setHasNotText("Copy")
            ).first();
        }
        close.click();
    }

    public boolean copyPenceresiKapandiMi() {
        return page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        ).count() == 0;
    }

    public void copyIsleminiOnayla() {
        Locator buttons = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        buttons.last().click();

        long end = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < end) {
            if (!copyPenceresiGorunuyorMu()) return;
            page.waitForTimeout(250);
        }
        throw new AssertionError("Copy işlemi sonrasında pencere kapanmadı!");
    }

    public void kopyaBelgeDetayiniBekle(String copyTitle, String sourceUrl) {
        long end = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < end) {
            boolean differentUrl = sourceUrl == null || !page.url().equals(sourceUrl);

            if (differentUrl
                    && belgeDetaySayfasindaMi()
                    && belgeBasligiGorunuyorMu(copyTitle)) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Copy oluşturuldu ancak yeni kopyanın detay sayfası hazır olmadı. "
                        + "Beklenen başlık: " + copyTitle
                        + ", URL: " + page.url()
        );
    }

    public boolean copyBasligiBoskenButonDisabledMi() {
        Locator buttons = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        return buttons.last().isDisabled();
    }

    public void belgeDetayAdresiniAcBatch13(String url) {
        page.navigate(url);
        page.waitForLoadState();
    }

    public boolean belgeDetayUrlFarkliMi(String oldUrl) {
        return !page.url().equals(oldUrl);
    }

    public boolean belgeDetayUrlAyniMiBatch13(String expectedUrl) {
        return page.url().equals(expectedUrl);
    }


    // =========================
    // Batch 14 - Version Labels
    // =========================

    private Locator versionHistoryRow(String version) {
        Locator row = page.locator("tr").filter(
                new Locator.FilterOptions().setHasText(version)
        ).first();

        row.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));
        return row;
    }

    public boolean versionHistoryGorunuyorMu() {
        return page.getByText(
                "Version history",
                new Page.GetByTextOptions().setExact(true)
        ).count() > 0;
    }

    public boolean versionHistorySatiriGorunuyorMu(String version) {
        try {
            return versionHistoryRow(version).isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public void versionLabelEditorunuAc(String version) {
        Locator row = versionHistoryRow(version);

        Locator addLabelButton =
                row.locator("button[aria-label='Add label']");

        addLabelButton.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));

        addLabelButton.click();

        // React yeniden render ettiği için row'u tekrar alıyoruz.
        row = versionHistoryRow(version);

        Locator input =
                row.locator("input[type='text']").first();

        input.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
    }

    public boolean versionLabelEditoruAcikMi(String version) {
        Locator row = versionHistoryRow(version);

        return row.locator("input[type='text']").first().isVisible();
    }

    public void versionLabelGir(String version, String label) {
        Locator row = versionHistoryRow(version);

        Locator input =
                row.locator("input[type='text']").first();

        input.fill(label);
    }

    public String versionLabelInputDegeri(String version) {
        Locator row = versionHistoryRow(version);

        return row.locator("input[type='text']")
                .first()
                .inputValue();
    }
    public void versionLabelEkle(String version) {
        Locator row = versionHistoryRow(version);

        Locator addButton = row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("Add")
                        .setExact(true)
        );

        addButton.click();
    }

    public void versionLabelIptalEt(String version) {
        Locator row = versionHistoryRow(version);
        row.getByText("Cancel", new Locator.GetByTextOptions().setExact(true)).click();
        page.waitForTimeout(250);
    }

    public boolean versionLabelGorunuyorMu(String version, String label) {
        Locator row = versionHistoryRow(version);

        Locator removeButton = row.locator(
                "button[aria-label='Remove label " + label + "']"
        );

        try {
            removeButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            return removeButton.isVisible();
        } catch (PlaywrightException e) {
            return false;
        }
    }
    public boolean versionLabelGorunmuyorMu(String version, String label) {
        return !versionLabelGorunuyorMu(version, label);
    }

    public void versionLabelSil(String version, String label) {
        Locator row = versionHistoryRow(version);

        Locator removeButton = row.locator(
                "button[aria-label='Remove label " + label + "']"
        );

        if (removeButton.count() == 0) {
            throw new AssertionError(
                    "Silinecek version label bulunamadı: " + label
            );
        }

        removeButton.click();

    }

    public int versionLabelSayisi(String version) {
        Locator row = versionHistoryRow(version);
        // Label chip'leri, görünür label metinleri üzerinden testlerde doğrulanıyor.
        // Bu yardımcı sadece editor kapalıyken LABELS hücresindeki remove button sayısını döndürür.
        Locator buttons = row.locator("button");
        int count = 0;
        for (int i = 0; i < buttons.count(); i++) {
            Locator b = buttons.nth(i);
            String text = b.innerText().trim();
            String aria = b.getAttribute("aria-label");
            String title = b.getAttribute("title");
            if ((text.equals("×") || text.equals("x"))
                    || (aria != null && aria.toLowerCase().contains("remove"))
                    || (title != null && title.toLowerCase().contains("remove"))) {
                count++;
            }
        }
        return count;
    }
    // =========================
    // BATCH 15 - DOCUMENT RENAME
    // PATCH /v1/documents/{id}/title
    // =========================

    private Locator renameEditButton() {

        // Document title'ın hemen yanındaki kalem/edit butonu.
        // Edit modunda Confirm rename / Cancel butonları oluşuyor.
        Locator confirm =
                page.locator("button[title='Confirm rename']");

        if (confirm.count() > 0 && confirm.first().isVisible()) {
            throw new AssertionError(
                    "Rename edit modu zaten açık!"
            );
        }

        Locator titleHeading =
                page.locator("h1").first();

        if (titleHeading.count() == 0) {
            throw new AssertionError(
                    "Document title heading bulunamadı!"
            );
        }

        Locator container =
                titleHeading.locator("xpath=..");

        Locator buttons =
                container.locator("button");

        if (buttons.count() == 0) {
            throw new AssertionError(
                    "Document title yanındaki rename/edit butonu bulunamadı!"
            );
        }

        return buttons.first();
    }

    private Locator renameInput() {

        Locator confirm =
                page.locator("button[title='Confirm rename']");

        confirm.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        // DocumentDetail.tsx:
        // <span>
        //   <input ... />
        //   <button title="Confirm rename">...</button>
        //   <button title="Cancel">...</button>
        // </span>
        Locator renameContainer =
                confirm.locator("xpath=..");

        Locator input =
                renameContainer.locator("input").first();

        input.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        return input;
    }

    public boolean renameEditButonuGorunuyorMu() {

        try {
            return renameEditButton().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    public void renameEditorunuAc() {

        renameEditButton().click();

        renameInput();
    }

    public boolean renameEditoruAcikMi() {

        Locator confirm =
                page.locator("button[title='Confirm rename']");

        Locator cancel =
                page.locator("button[title='Cancel']");

        return confirm.count() > 0
                && confirm.first().isVisible()
                && cancel.count() > 0
                && cancel.first().isVisible();
    }

    public String renameInputDegeri() {

        return renameInput().inputValue();
    }

    public void renameBasligiGir(String newTitle) {

        renameInput().fill(newTitle);
    }

    // =========================
    // BATCH 16 - RENAME VALIDATION
    // =========================

    public boolean renameKaydetDisabledMi() {

        Locator confirm =
                page.locator("button[title='Confirm rename']");

        confirm.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        return confirm.isDisabled();
    }

    public void renameKaydet() {

        Locator confirm =
                page.locator("button[title='Confirm rename']");

        confirm.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        confirm.click();
    }

    public void renameIptalEt() {

        Locator cancel =
                page.locator("button[title='Cancel']");

        cancel.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        cancel.click();
    }

    public boolean renameEditoruKapandiMi() {

        Locator confirm =
                page.locator("button[title='Confirm rename']");

        return confirm.count() == 0
                || !confirm.first().isVisible();
    }

    public boolean belgeBasligiDegistiMi(String expectedTitle) {

        try {

            Locator heading = page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(expectedTitle)
                            .setExact(true)
            );

            heading.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            return heading.isVisible();

        } catch (Exception e) {

            return false;
        }
    }

    public boolean eskiBelgeBasligiGorunmuyorMu(String oldTitle) {

        Locator oldHeading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName(oldTitle)
                        .setExact(true)
        );

        return oldHeading.count() == 0
                || !oldHeading.first().isVisible();
    }

    public void renameSonucunuBekle(String expectedTitle) {

        long end =
                System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < end) {

            if (belgeBasligiDegistiMi(expectedTitle)
                    && renameEditoruKapandiMi()) {

                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Document rename tamamlanmadı. Beklenen title: "
                        + expectedTitle
        );
    }

    public boolean renameSonrasiUrlAyniMi(String oldUrl) {

        return page.url().equals(oldUrl);
    }

    public void belgeDetayiniRefreshEt() {

        page.reload();
        page.waitForLoadState();

        if (!belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Refresh sonrası document detail açılamadı! URL: "
                            + page.url()
            );
        }
    }
    public boolean renameBasligiKaydedilmediMi(String originalTitle) {

        try {

            Locator heading = page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(originalTitle)
                            .setExact(true)
            );

            heading.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000)
            );

            return heading.isVisible();

        } catch (Exception e) {

            return false;
        }
    }
    // =========================
// BATCH 17 - VERSION COMMENT
// =========================

    public boolean versionCommentAlaniGorunuyorMu() {

        try {

            Locator textarea = page.locator(
                    "textarea[placeholder^='Describe what changed in this version']"
            );

            textarea.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000)
            );

            return textarea.isVisible();

        } catch (Exception e) {

            return false;
        }
    }

    public void versionCommentGir(String comment) {

        Locator textarea = page.locator(
                "textarea[placeholder^='Describe what changed in this version']"
        );

        textarea.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        textarea.fill(comment);
    }

    public void versionCommentBosBirak() {

        Locator textarea = page.locator(
                "textarea[placeholder^='Describe what changed in this version']"
        );

        textarea.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000)
        );

        textarea.fill("");
    }

    public boolean versionCommentGorunuyorMu(String comment) {
        try {
            Locator comments = page.getByText(
                    comment,
                    new Page.GetByTextOptions().setExact(true)
            );

            comments.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000)
            );

            return comments.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

}