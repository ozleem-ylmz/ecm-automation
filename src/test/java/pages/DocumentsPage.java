package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.Download;

import java.nio.file.Paths;

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
}