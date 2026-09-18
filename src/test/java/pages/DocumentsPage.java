package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Download;


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
                "http://localhost:5173/login"
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
                "http://localhost:5173/documents"
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
        page.navigate("http://localhost:5173/folders");
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
            if (text != null && text.trim().equals(folderName)) {
                optionValue = option.getAttribute("value");
                break;
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

            if (selectedValue.equals(value)
                    && text != null
                    && text.trim().equals(folderName)) {
                return true;
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

}
