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

    // Pozitif (görünmesi beklenen) bekleme üst sınırı. Koşul sağlanınca hemen
    // döner; geçen testte maliyeti yoktur. 5 sn, paralel koşuda CPU doygunken
    // (SPA boot + render) yetmeyip sahte hata üretiyordu.
    private static final double UI_WAIT_MS = 15000;

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

    public void documentsSayfasinaDogrudanGit() {
        page.navigate(BrowserManager.getBaseUrl() + "/documents");
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

    public void uploadWithoutClassSayfasinaDogrudanGit() {

        page.navigate(
                BrowserManager.getBaseUrl() + "/documents/new/unclassified"
        );
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
        try {
            page.waitForURL(
                    url -> url.matches(".*/documents/[0-9a-fA-F-]{36}$"),
                    new Page.WaitForURLOptions()
                            .setTimeout(20000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Belge yüklendikten sonra detay sayfası açılmadı! URL: "
                            + page.url(),
                    e
            );
        }

        // URL değiştiği anda eski upload sayfasının h1'i ("Upload without class")
        // hâlâ DOM'da olabiliyor. Sonraki adımlar (ör. başlığı kaydetme) doğru
        // belgeyi okusun diye detay başlığı render edilene kadar bekliyoruz.
        belgeDetayBasliginiBekle(title);
    }

    private void belgeDetayBasliginiBekle(String title) {

        try {
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(title)
                            .setExact(true)
                            .setLevel(1)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Belge detay başlığı görüntülenmedi: " + title
                            + " | Görünen h1: " + page.locator("h1").allInnerTexts(),
                    e
            );
        }
    }

    /*
     * ID'si bilinen belgenin detay sayfasına doğrudan gider (listeden
     * aramadan) ve detay başlığının render edilmesini bekler.
     */
    public void belgeDetayinaGitVeBasligiBekle(String documentId, String title) {

        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + documentId);

        if (!page.url().endsWith("/documents/" + documentId)) {
            throw new AssertionError(
                    "Belge detay sayfası açılamadı. Beklenen id: " + documentId
                            + " URL: " + page.url()
            );
        }

        belgeDetayBasliginiBekle(title);
    }

    // =========================
    // DOCUMENT DETAIL
    // =========================

    /*
     * Negatif ("görünmemeli / yapamamalı") kontroller ancak belge verisi
     * render edildikten sonra anlamlıdır: DocumentDetail.tsx önce
     * "Loading document…" gösterir; butonlar ve lock durumu (aynı yüklemede
     * çekiliyor) h1 ile birlikte gelir. navigate()+waitForLoadState() sadece
     * "load" olayını bekler, SPA verisini beklemez; o anda hiçbir buton
     * olmadığı için negatif kontrol her durumda geçiyordu (false positive riski).
     * Sayfa hazır değilse sessizce geçmek yerine hata veriyoruz.
     */
    private void detayVerisiniBekle(Page targetPage) {
        try {
            targetPage.locator("h1").first().waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
            targetPage.getByText("Loading document…").waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Belge detay verisi yüklenmeden görünürlük kontrolü yapılamaz. URL: "
                            + targetPage.url(),
                    e
            );
        }

        // /login de bir h1 ("Welcome back") gösterir ve "Loading document…"
        // içermez: oturum düşmüşse yukarıdaki bekleme geçer, negatif kontroller
        // hiçbir şey test etmeden "görünmüyor" derdi. Detay URL'i zorunlu.
        if (!targetPage.url().matches(".*/documents/[0-9a-fA-F-]{36}.*")) {
            throw new AssertionError(
                    "Belge detay sayfasında değil (oturum düşmüş olabilir). URL: "
                            + targetPage.url()
            );
        }
    }

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
                            .setTimeout(UI_WAIT_MS)
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
                            .setTimeout(UI_WAIT_MS)
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

        detayVerisiniBekle(page);

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
                        .setTimeout(UI_WAIT_MS)
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

            // Soft delete sonrası uygulama global /documents listesine döner;
            // liste (8k+ belge) 3 JVM'li soğuk başlangıçta 5 sn'den geç render
            // edildi (belge 3. satırda "Deleted" rozetiyle vardı). Koşul-temelli
            // pozitif bekleme: başlık görünür görünmez döner.
            titleLocator.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(UI_WAIT_MS)
            );

            Locator documentRow =
                    titleLocator.locator("xpath=ancestor::tr[1]");

            documentRow.getByText(
                    "Deleted",
                    new Locator.GetByTextOptions()
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(UI_WAIT_MS)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // RESTORE
    // =========================

    /*
     * Belgenin detay adresi biliniyorsa Documents listesinde aramadan
     * doğrudan detay sayfasına gidip restore eder. Liste navigasyonu bu
     * adımın test ettiği şey değil; liste doğrulaması "belge Deleted
     * durumunda görüntülenmelidir" adımında yapılıyor.
     */
    public void belgeyiRestoreEt(String title, String detailUrl) {

        if (detailUrl == null || detailUrl.isBlank()) {
            belgeyiRestoreEt(title);
            return;
        }

        page.navigate(detailUrl);

        if (!page.url().equals(detailUrl)) {
            throw new AssertionError(
                    "Soft delete yapılan belgenin detay sayfası açılamadı! Beklenen: "
                            + detailUrl + " URL: " + page.url()
            );
        }

        restoreButonunaTiklaVeDogrula();
    }

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
                        .setTimeout(UI_WAIT_MS)
        );

        titleLocator.click();

        try {
            page.waitForURL(
                    url -> url.matches(".*/documents/[0-9a-fA-F-]{36}$"),
                    new Page.WaitForURLOptions()
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Soft delete yapılan belgenin detay sayfası açılamadı! URL: "
                            + page.url(),
                    e
            );
        }

        restoreButonunaTiklaVeDogrula();
    }

    private void restoreButonunaTiklaVeDogrula() {

        Locator restoreButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Restore")
                        .setExact(true)
        );

        restoreButton.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(UI_WAIT_MS)
        );

        // Restore'un tamamlandığını POST /restore yanıtı kanıtlar; ardından
        // header'daki Deleted badge'inin kalkmasını bekliyoruz.
        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/restore$")
                            && "POST".equals(r.request().method()),
                    new Page.WaitForResponseOptions()
                            .setTimeout(10000),
                    restoreButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Restore tıklandıktan sonra POST /restore isteği gönderilmedi!",
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Restore başarısız: HTTP " + response.status() + " " + response.text()
            );
        }

        try {
            page.locator(
                    "div:has(> span:text-matches('^Class:')) > span.rounded-full"
            ).filter(
                    new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^\\s*Deleted\\s*$"))
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.DETACHED)
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Restore işlemi sonrasında Deleted durumu kaybolmadı!",
                    e
            );
        }
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
                        .setTimeout(UI_WAIT_MS)
        );

        belge.click();

        try {
            page.waitForURL(
                    url -> url.matches(".*/documents/[0-9a-fA-F-]{36}.*"),
                    new Page.WaitForURLOptions().setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Belge listeden açıldı ancak detay sayfasına gidilemedi! URL: "
                            + page.url(),
                    e
            );
        }
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
                        .setHas(page.locator("option[value='under_review']"))
        ).first();

        statusSelect.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(UI_WAIT_MS)
        );

        /*
         * ECM'deki gerçek option value:
         * Draft        -> draft
         * Under review -> under_review
         * Published    -> published
         * Archived     -> archived
         */
        statusSelect.selectOption("under_review");

        Locator updateStatusButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Update status")
                        .setExact(true)
        );

        // Select'in değeri sadece kullanıcının seçimini gösterir; kaydın
        // gerçekleştiğini PATCH /status yanıtı ve header badge'i kanıtlar.
        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/status$")
                            && "PATCH".equals(r.request().method()),
                    new Page.WaitForResponseOptions()
                            .setTimeout(10000),
                    updateStatusButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Update status tıklandıktan sonra PATCH /status isteği gönderilmedi!",
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Status güncellenemedi! HTTP " + response.status()
                            + " " + response.text()
            );
        }

        if (!belgeStatusuMu("Under review")) {
            throw new AssertionError(
                    "Belge statusu Under review olarak güncellenmedi! Görünen: "
                            + mevcutStatusBadgeText()
            );
        }
    }

    public boolean statusUnderReviewMu() {
        return belgeStatusuMu("Under review");
    }

    /*
     * Belgenin kayıtlı (API'den gelen) status'u, detay header'ındaki
     * "Class: ..." satırının yanındaki badge'dir
     * (DocumentDetail.tsx -> statusLabel(data.document.status)).
     *
     * Admin actions içindeki <select> yalnızca düzenlenebilir taslak değerdir:
     * belge yüklenmeden önce varsayılanı "draft"tır ve kullanıcının seçimini
     * kaydedilmeden gösterir. Ayrıca getByText("Under review") dropdown
     * option'ını da yakalar. Bu yüzden status kontrolü için kullanılmamalı.
     */
    private Locator mevcutStatusBadge() {
        return page.locator(
                "div:has(> span:text-matches('^Class:')) > span.rounded-full"
        ).first();
    }

    public String mevcutStatusBadgeText() {
        try {
            return mevcutStatusBadge().innerText(
                    new Locator.InnerTextOptions().setTimeout(2000)
            ).trim();
        } catch (PlaywrightException e) {
            return "(status badge bulunamadı)";
        }
    }

    public boolean belgeStatusuMu(String expectedLabel) {
        try {
            com.microsoft.playwright.assertions.PlaywrightAssertions
                    .assertThat(mevcutStatusBadge())
                    // Badge "Under Review", dropdown "Under review" yazıyor;
                    // büyük/küçük harf farkını yok sayıp tam eşleşme arıyoruz.
                    .hasText(
                            Pattern.compile(
                                    // Regex tarayıcıda JS olarak çalışır; JS \Q..\E
                                    // desteklemediği için Pattern.quote kullanılmaz.
                                    "^\\s*"
                                            + expectedLabel.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0")
                                            + "\\s*$",
                                    Pattern.CASE_INSENSITIVE
                            ),
                            new com.microsoft.playwright.assertions.LocatorAssertions.HasTextOptions()
                                    .setTimeout(5000)
                    );
            return true;
        } catch (AssertionError | PlaywrightException e) {
            System.out.println(
                    "Status eşleşmedi. Beklenen: " + expectedLabel
                            + " | Görünen badge: " + mevcutStatusBadgeText()
                            + " | API: " + BrowserManager.getDocumentStatusEvidence()
            );
            return false;
        }
    }

    public boolean statusDraftMi() {
        return belgeStatusuMu("Draft");
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

        // copyPenceresiniAc ile aynı yarış (3 worker'lı koşuda görüldü):
        // tıklama, GET /documents/{id}/lock sonrası yeniden render sırasında
        // etkisiz kalıyordu (navigasyon/istek yok). Önce veri + kilit durumu.
        detayVerisiniBekle(page);
        kilitDurumunuBekle(page);

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
                        .setTimeout(UI_WAIT_MS)
        );

        // Re-version formu Title alanını GET /documents/{id} yanıtıyla async
        // dolduruyor (DocumentNew.tsx). Title input'u "required" olduğu için
        // dolmadan Save'e basılırsa tarayıcı form validasyonu submit'i sessizce
        // engelliyor: istek yok, hata mesajı yok. Title dolana kadar bekliyoruz.
        try {
            com.microsoft.playwright.assertions.PlaywrightAssertions
                    .assertThat(page.getByPlaceholder("e.g. Q3 Invoice — Acme Corp"))
                    .hasValue(
                            Pattern.compile("\\S"),
                            new com.microsoft.playwright.assertions.LocatorAssertions.HasValueOptions()
                                    .setTimeout(10000)
                    );
        } catch (AssertionError | PlaywrightException e) {
            throw new AssertionError(
                    "Yeni versiyon formunda Title alanı kaynak belgeden doldurulmadı. URL: "
                            + page.url(),
                    e
            );
        }
    }

    public void versionTipiniSec(String versionType) {

        Locator radio = page.getByRole(
                AriaRole.RADIO,
                new Page.GetByRoleOptions()
                        .setName(versionType)
        );

        radio.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(UI_WAIT_MS)
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

        Locator saveButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Save new version")
                        .setExact(true)
        );

        try {
            saveButton.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );
        } catch (Exception e) {
            throw new AssertionError(
                    "Save new version butonu bulunamadı. URL: " + page.url(),
                    e
            );
        }

        if (!saveButton.isEnabled()) {
            throw new AssertionError(
                    "Save new version butonu aktif değil. URL: " + page.url()
            );
        }

        try {
            page.waitForResponse(
                    response -> response.url().matches(".*/v1/documents/[^/]+/checkin$")
                            && "POST".equals(response.request().method()),
                    new Page.WaitForResponseOptions()
                            .setTimeout(20000),
                    saveButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Save new version tıklandıktan sonra checkin isteği gönderilmedi. URL: "
                            + page.url(),
                    e
            );
        }

        // Başarılı save sonrasında frontend document detail'e gider.
        try {
            page.waitForURL(
                    url -> url.matches(
                            ".*/documents/[0-9a-fA-F-]{36}$"
                    ),
                    new Page.WaitForURLOptions()
                            .setTimeout(20000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Yeni versiyon kaydedildikten sonra belge detay sayfasına dönülmedi. URL: "
                            + page.url(),
                    e
            );
        }
        // Detay sayfası h1'i belge verisi gelince render ediliyor; sonraki
        // adımların yüklenmiş detay sayfasında çalıştığından emin oluyoruz.
        // URL değiştiği anda eski form sayfasının h1'i DOM'da kalabildiği için
        // onları hariç tutuyoruz.
        page.locator("h1").filter(
                new Locator.FilterOptions()
                        .setHasNotText(Pattern.compile(
                                "^(Save new version|New document|Upload without class)$"
                        ))
        ).first().waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
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

        // Click sonrası hemen başka sayfaya gidilirse (ör. Move hedef klasörü
        // oluşturma) POST /checkout isteği navigasyonla iptal olabiliyordu.
        // Checkout'un tamamlandığını API yanıtıyla doğruluyoruz.
        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/checkout$")
                            && "POST".equals(r.request().method()),
                    new Page.WaitForResponseOptions()
                            .setTimeout(10000),
                    checkoutButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Check out tıklandıktan sonra POST /checkout isteği gönderilmedi!",
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Checkout başarısız: HTTP " + response.status() + " " + response.text()
            );
        }
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
        detayVerisiniBekle(page);


        Locator notCheckedOut = page.getByText(
                "Not checked out.",
                new Page.GetByTextOptions()
                        .setExact(true)
        );

        return notCheckedOut.count() == 0
                || !notCheckedOut.isVisible();
    }

    public boolean checkOutButonuGorunmuyorMu() {
        detayVerisiniBekle(page);


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
        // Ön koşul login'i: manager'ın kendi ayrı context'ine API ile giriş.
        // Admin ve manager oturumları ayrı cookie jar'larda kalır. Çağıran
        // adımlar ardından managerPage'i belge detayına yönlendiriyor.
        BrowserManager.apiLogin(managerPage.context(), "manager@local", "123");
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
        detayVerisiniBekle(managerPage);


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
        detayVerisiniBekle(targetPage);

        return targetPage.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Classify")
                        .setExact(true)
        ).count() == 0;
    }

    public boolean tagDegistirmeKullanilamiyorMu(Page targetPage) {
        detayVerisiniBekle(targetPage);


        // Detay sayfasında "Add tag" butonu yok (o metin yalnızca liste
        // sayfasının toplu işlem çubuğunda); eski kontrol her durumda
        // geçiyordu. Tag değiştirme giriş noktası tag input'udur.
        Locator input = tagInput(targetPage);

        return input.count() == 0
                || !input.first().isVisible()
                || input.first().isDisabled();
    }

    /*
     * Hard reload: sayfa baştan yüklenir, belge GET /documents/{id} ile
     * yeniden çekilir. 250 ms polling yerine o yanıtı ve ardından detay
     * başlığının render edilmesini bekliyoruz.
     */
    public void belgeDetaySayfasiniYenile() {

        if (!belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Refresh öncesinde belge detay sayfasında değiliz! URL: " + page.url()
            );
        }

        try {
            page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}$")
                            && "GET".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(10000),
                    () -> page.reload()
            );

            page.getByRole(AriaRole.HEADING).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Refresh sonrasında belge detay sayfası hazır olmadı! URL: " + page.url(),
                    e
            );
        }

        if (!belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Refresh sonrasında belge detay sayfasında kalınmadı! URL: " + page.url()
            );
        }
    }

    public boolean newVersionButonuGorunuyorMu() {
        detayVerisiniBekle(page);

        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("New version")
                        .setExact(true)
        ).isVisible();
    }
    public boolean checkOutButonuGorunuyorMu(Page targetPage) {
        detayVerisiniBekle(targetPage);


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
        detayVerisiniBekle(page);


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

        // Anlık count() yerine detay h1'inin render edilmesini bekliyoruz;
        // save sonrası navigation'da h1 kısa süre boş kalıyor.
        try {
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(expectedTitle)
                            .setExact(true)
                            .setLevel(1)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(UI_WAIT_MS)
            );
            return true;
        } catch (PlaywrightException e) {
            System.out.println(
                    "Belge başlığı eşleşmedi. Beklenen: " + expectedTitle
                            + " | Görünen h1: " + page.locator("h1").allInnerTexts()
            );
            return false;
        }
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
        // Preview içeriği tıklamadan sonra async yükleniyor; anlık
        // isVisible() yerine içeriğin görünmesini bekliyoruz.
        try {
            page.locator("pre")
                    .getByText(
                            "ECM UI automation test document.",
                            new Locator.GetByTextOptions()
                                    .setExact(false)
                    )
                    .first()
                    .waitFor(
                            new Locator.WaitForOptions()
                                    .setTimeout(10000)
                    );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }
    public boolean softDeleteEdilmisBelgeAktifDetayOlarakGoruntulenmiyorMu() {

        // Soft-delete edilmiş belge eski URL'den açıldığında header'da
        // "Deleted" badge'i görünmeli. Sayfa navigate sonrası async
        // render edildiği için anlık count() yerine badge'i bekliyoruz.
        try {
            page.locator(
                    "div:has(> span:text-matches('^Class:')) > span.rounded-full"
            ).filter(
                    new Locator.FilterOptions()
                            .setHasText(Pattern.compile("^\\s*Deleted\\s*$"))
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }
    // =========================
// STATUS INTEGRITY
// =========================

    /*
     * ECM'de status belge (document) seviyesindedir: document_version
     * tablosunda status kolonu yoktur. Yeni versiyon kaydedildiğinde ECM
     * belgenin status'unu Draft yapar ve bunu audit geçmişine
     * "document.new_version" olayı olarak before/after state ile yazar.
     * Önceki versiyonun hangi status ile devredildiği yalnızca bu geçmişte
     * görülebilir, bu yüzden history API yanıtını kontrol ediyoruz.
     *
     * Detay sayfası yenilenirken UI'ın kendi yaptığı
     * GET /documents/{id}/history yanıtını bekleyip okuyoruz.
     * Dönüş: bulunursa null, bulunamazsa açıklayıcı hata metni.
     */
    public String yeniVersiyonGecisiGecmisteHata(
            String oncekiVersiyon,
            String oncekiStatus,
            String yeniVersiyon,
            String yeniStatus
    ) {
        // Reload öncesi sayfadan kalan (navigasyonla gövdesi atılan) history
        // yanıtları da gelebildiği için gövdeyi yanıt gelir gelmez okuyoruz;
        // okunamayanları (eski sayfaya ait) atlıyoruz.
        String[] historyBody = new String[1];
        int[] historyStatus = new int[1];

        java.util.function.Consumer<com.microsoft.playwright.Response> listener = r -> {
            if (historyBody[0] == null
                    && "GET".equals(r.request().method())
                    && r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/history$")) {
                try {
                    historyStatus[0] = r.status();
                    historyBody[0] = r.text();
                } catch (PlaywrightException ignored) {
                    // Eski sayfaya ait yanıt: gövde artık yok.
                }
            }
        };

        page.onResponse(listener);
        try {
            page.reload();
            page.waitForCondition(
                    () -> historyBody[0] != null,
                    new Page.WaitForConditionOptions()
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            return "Belge geçmişi (GET /documents/{id}/history) yüklenmedi: " + e.getMessage();
        } finally {
            page.offResponse(listener);
        }

        if (historyStatus[0] < 200 || historyStatus[0] >= 300) {
            return "Belge geçmişi alınamadı: HTTP " + historyStatus[0];
        }

        String from = oncekiVersiyon.replaceFirst("^v", "");
        String to = yeniVersiyon.replaceFirst("^v", "");

        com.google.gson.JsonArray items = com.google.gson.JsonParser
                .parseString(historyBody[0])
                .getAsJsonObject()
                .getAsJsonArray("items");

        java.util.List<String> newVersionEvents = new java.util.ArrayList<>();

        for (com.google.gson.JsonElement el : items) {
            com.google.gson.JsonObject item = el.getAsJsonObject();

            if (!"document.new_version".equals(item.get("action").getAsString())) {
                continue;
            }

            String beforeStatus = jsonAlan(item, "before_state", "status");
            String beforeVersion = jsonAlan(item, "before_state", "version");
            String afterStatus = jsonAlan(item, "after_state", "status");
            String afterVersion = jsonAlan(item, "after_state", "version");

            newVersionEvents.add(
                    beforeVersion + "(" + beforeStatus + ") -> "
                            + afterVersion + "(" + afterStatus + ")"
            );

            if (from.equals(beforeVersion)
                    && oncekiStatus.equals(beforeStatus)
                    && to.equals(afterVersion)
                    && yeniStatus.equals(afterStatus)) {
                return null;
            }
        }

        return "Beklenen geçiş: " + from + "(" + oncekiStatus + ") -> "
                + to + "(" + yeniStatus + ") | Geçmişteki new_version olayları: "
                + newVersionEvents;
    }

    private static String jsonAlan(
            com.google.gson.JsonObject item,
            String state,
            String field
    ) {
        com.google.gson.JsonElement stateEl = item.get(state);
        if (stateEl == null || !stateEl.isJsonObject()) {
            return null;
        }
        com.google.gson.JsonElement value = stateEl.getAsJsonObject().get(field);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    public boolean statusUnderReviewGorunuyorMu() {
        // getByText("Under review") status dropdown'undaki <option>'ı da
        // yakaladığı için status Draft iken bile true dönüyordu (false positive).
        // Belgenin kayıtlı status'unu gösteren header badge'ini kontrol ediyoruz.
        return statusUnderReviewMu();
    }

    public boolean deletedBelgedeStatusUnderReviewGorunuyorMu() {
        // Header'daki "Deleted" badge'i (DocumentDetail.tsx, is_deleted) ve
        // yanındaki status badge'i birlikte kontrol ediyoruz.
        Locator deletedBadge = page.locator(
                "div:has(> span:text-matches('^Class:')) > span.rounded-full"
        ).filter(
                new Locator.FilterOptions()
                        .setHasText(Pattern.compile("^\\s*Deleted\\s*$"))
        );

        try {
            deletedBadge.first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );
        } catch (PlaywrightException e) {
            System.out.println("Deleted badge bulunamadı. URL: " + page.url());
            return false;
        }

        return belgeStatusuMu("Under review");
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

        // Tıklayıp dönmek yetmez: DELETE .../hard bitmeden yapılan sonraki
        // navigate'in GET'i silmeden önce işlenip belgeyi "hâlâ erişilebilir"
        // gösterebiliyordu (paralel koşuda görüldü). Yanıtı bekle.
        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/hard$")
                            && "DELETE".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                    () -> page.getByRole(
                            AriaRole.BUTTON,
                            new Page.GetByRoleOptions()
                                    .setName("Permanently delete")
                                    .setExact(true)
                    ).click()
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Permanently delete tıklandı ancak DELETE /documents/{id}/hard yanıtı gelmedi. URL: "
                            + page.url(),
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Hard delete başarısız: HTTP " + response.status() + " " + response.text()
            );
        }
    }

    public boolean hardDeleteEdilmisBelgeDetaydaGorunmuyorMu() {

        // Anlık count() sayfa verisi gelmeden de 0 döner; önce yükleme bitmeli.
        page.getByText("Loading document…").waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(UI_WAIT_MS)
        );

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
        detayVerisiniBekle(page);


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
        detayVerisiniBekle(managerPage);


        // Kalem butonunun erişilebilir adı title'dır: "Rename document"
        // (DocumentDetail.tsx). Tam eşleşen "Rename" adında buton yok; eski
        // kontrol her durumda geçiyordu.
        Locator button = managerPage.locator("button[title='Rename document']");

        return button.count() == 0
                || !button.first().isVisible()
                || button.first().isDisabled();
    }

    public boolean managerStatusDegistiremiyorMu(Page managerPage) {
        detayVerisiniBekle(managerPage);


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
        detayVerisiniBekle(managerPage);


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
        detayVerisiniBekle(page);

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

    /*
     * DocumentDetail.tsx: Classify -> navigate(/documents/{id}/classify),
     * DocumentClassify.tsx h1 = "Assign to a class". Eski kontrol anlık
     * count()'larla "herhangi bir dialog VEYA url'de 'classif' VEYA
     * 'Classify' metni > 1" arıyordu (hem zayıf hem yarışa açık).
     */
    public boolean classifyEkraniGorunuyorMu() {
        try {
            page.waitForURL(
                    url -> url.matches(".*/documents/[0-9a-fA-F-]{36}/classify$"),
                    new Page.WaitForURLOptions().setTimeout(10000)
            );
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Assign to a class").setExact(true)
            ).waitFor(new Locator.WaitForOptions().setTimeout(10000));
            return true;
        } catch (PlaywrightException e) {
            System.out.println("Classify ekranı açılmadı. URL: " + page.url());
            return false;
        }
    }

    /*
     * DocumentClassify.tsx: Cancel -> navigate(/documents/{id}). Eskiden anlık
     * count() ile Cancel aranıyor (sayfa render olmadıysa goBack) ve ardından
     * detay sayfası yüklenmeden "Unclassified" kontrol ediliyordu (yarış).
     */
    public void classifyIsleminiIptalEt() {
        Locator cancel = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Cancel").setExact(true)
        ).last();

        cancel.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        cancel.click();

        try {
            page.waitForURL(
                    url -> url.matches(".*/documents/[0-9a-fA-F-]{36}$"),
                    new Page.WaitForURLOptions().setTimeout(10000)
            );
            // URL client-side route değişiminde hemen güncelleniyor; eski
            // sayfanın h1'i ("Assign to a class") DOM'dan kalkana kadar bekle.
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Assign to a class").setExact(true)
            ).waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.DETACHED)
                    .setTimeout(10000));
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Classify iptalinden sonra belge detayına dönülmedi. URL: " + page.url(),
                    e
            );
        }
    }

    public boolean belgeUnclassifiedMi() {
        detayVerisiniBekle(page);
        // Detay header'ı: "Class: Unclassified" (DocumentDetail.tsx). Anlık
        // count() yerine pozitif bekleme.
        try {
            page.getByText(
                    "Unclassified",
                    new Page.GetByTextOptions().setExact(false)
            ).first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    // Ekran görüntüsünde doğrulandı: bu bir BUTTON değil LINK.
    public boolean moveButonuGorunuyorMu() {
        detayVerisiniBekle(page);

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
        detayVerisiniBekle(page);

        Locator tagInput = tagInput(page);
        return tagInput.count() > 0 && tagInput.first().isVisible();
    }

    // TagManager.tsx: yazma yetkisi (canWrite) varsa tek giriş noktası bu
    // input'tur; "Add tags" butonu yalnızca bekleyen etiket varken çıkar.
    private static Locator tagInput(Page targetPage) {
        return targetPage.locator("input[placeholder^='Type to add a tag']");
    }

    public boolean tagGirisAlaniGorunmuyorMu(Page targetPage) {
        detayVerisiniBekle(targetPage);

        // Negatif kontrol, Tags kartı (TagManager.tsx) kendi GET
        // /documents/{id}/tags isteğini bitirmeden yapılmamalı: "Loading tags…"
        // kaybolana kadar bekle. Önce kartın başlığı: kart henüz mount
        // edilmemişken "Loading tags…" de yoktur (HIDDEN anında geçerdi).
        try {
            targetPage.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions().setName("Tags").setExact(true)
            ).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
            targetPage.getByText("Loading tags…").waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(UI_WAIT_MS)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Tags kartı yüklenmeden tag giriş alanı kontrolü yapılamaz."
                            + detayDurumKaniti(targetPage), e);
        }

        // ECM placeholder'ı "Type to add a tag…" (tek karakter U+2026).
        // Eski getByPlaceholder("Type to add a tag...") (üç nokta) hiçbir
        // zaman eşleşmiyordu: kontrol, alan görünürken bile geçiyordu.
        Locator input = tagInput(targetPage);
        return input.count() == 0 || !input.first().isVisible();
    }

    public boolean downloadButonuGorunuyorMu() {
        detayVerisiniBekle(page);

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Download").setExact(true)
        );
        return button.count() > 0 && button.first().isVisible();
    }

    public boolean belgeDetaySayfasindaKalindiMi() {
        return belgeDetaySayfasindaMi();
    }


    // Assertion mesajı için kanıt: hangi sayfada, belge Deleted mı, kilit var mı.
    public String detayDurumKaniti(Page targetPage) {
        try {
            boolean deleted = targetPage.getByText(
                    "Deleted", new Page.GetByTextOptions().setExact(true)).first().isVisible();
            boolean lockedBy = targetPage.getByText(
                    Pattern.compile("^Checked out by ")).first().isVisible();
            return " [URL: " + targetPage.url() + ", Deleted rozeti: " + deleted
                    + ", 'Checked out by' görünür: " + lockedBy + "]";
        } catch (PlaywrightException e) {
            return " [durum kanıtı alınamadı: " + e.getMessage() + "]";
        }
    }

    public boolean moveButonuGorunmuyorMu(Page targetPage) {
        detayVerisiniBekle(targetPage);

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
        /*
         * Move/Copy hedef klasörü bir ön koşul; klasör oluşturma UI'ı
         * folders.feature'da test ediliyor. UI'ın gönderdiği payload'ın
         * aynısıyla (Folders.tsx handleCreate) API üzerinden oluşturuyoruz;
         * ağır /folders sayfasını açmıyoruz.
         */
        String response = BrowserManager.apiPostJson(
                page.context(),
                BrowserManager.getAccessToken(),
                "/v1/folders",
                java.util.Map.of(
                        "name", folderName,
                        "parent_id", "00000000-0000-0000-0000-000000000001",
                        "inherit_permissions", true
                )
        );

        if (!response.contains(folderName)) {
            throw new AssertionError(
                    "Hedef klasör oluşturulamadı: " + folderName + " | " + response
            );
        }
    }

    public void belgeDetayAdresiniAc(String url) {
        page.navigate(url);
        page.waitForLoadState();
    }

    /*
     * Move dialog'undaki Destination select'i (MoveDocumentDialog.tsx).
     * Sayfada status/ACL select'leri de olduğu için "select.last()" yerine
     * "Destination" etiketli form'a scope'luyoruz.
     */
    private Locator moveDestinationSelect() {
        return page.locator("form").filter(
                new Locator.FilterOptions()
                        .setHas(page.getByText(
                                "Destination",
                                new Page.GetByTextOptions().setExact(true)
                        ))
        ).locator("select");
    }

    public void moveHedefKlasorSec(String folderName) {
        klasorOptionSec(moveDestinationSelect(), folderName, "Move");
    }

    /*
     * FolderPicker select'inde hedef klasörü seçer.
     *
     * FolderPicker önce disabled bir "Loading folders…" select'i render
     * ediyor, GET /folders/picker yanıtı gelince gerçek listeyi çiziyor.
     * Hedef klasörün option'ı DOM'a gelene kadar bekliyoruz.
     * Seçenekler tam yol gösterebilir: "Root / Parent / QA Move Folder ..."
     *
     * Ortamda binlerce klasör olduğu için option'lar Java'dan tek tek
     * okunmaz (her biri bir round-trip'ti, ~30 sn); eşleşme tek bir
     * locator filtresiyle tarayıcıda yapılır.
     */
    private void klasorOptionSec(Locator select, String folderName, String islem) {

        Locator targetOption = select.locator("option").filter(
                new Locator.FilterOptions()
                        .setHasText(klasorOptionPattern(folderName))
        ).first();

        try {
            targetOption.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.ATTACHED)
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            String ozet;
            try {
                ozet = String.valueOf(select.first().evaluate(
                        "s => s.options.length + ' option, ilk: ' + Array.from(s.options).slice(0, 3).map(o => o.text).join(' | ')"
                ));
            } catch (PlaywrightException ignored) {
                ozet = "(select bulunamadı)";
            }
            throw new AssertionError(
                    islem + " hedef klasörü listede bulunamadı: " + folderName
                            + " | " + ozet,
                    e
            );
        }

        select.selectOption(targetOption.getAttribute("value"));
    }

    private static Pattern klasorOptionPattern(String folderName) {
        // Regex tarayıcıda JS olarak çalışır (Pattern.quote kullanılmaz).
        String escaped = folderName.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
        return Pattern.compile("(^|/)\\s*" + escaped + "\\s*$");
    }

    /*
     * Seçili option'ın metni, tek bir evaluate ile. Select henüz
     * "Loading folders…" durumundaysa (disabled) yüklenmesini bekler.
     */
    private String seciliKlasorOptionMetni(Locator select) {
        com.microsoft.playwright.assertions.PlaywrightAssertions
                .assertThat(select)
                .isEnabled(
                        new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions()
                                .setTimeout(10000)
                );

        Object text = select.evaluate(
                "s => s.selectedIndex >= 0 ? s.options[s.selectedIndex].text : ''"
        );
        return text == null ? "" : text.toString().trim();
    }

    private static boolean klasorOptionMetniEslesiyorMu(String optionText, String folderName) {
        return optionText.equals(folderName)
                || optionText.endsWith("/ " + folderName)
                || optionText.endsWith("/" + folderName);
    }

    public boolean moveHedefKlasorSeciliMi(String folderName) {
        try {
            return klasorOptionMetniEslesiyorMu(
                    seciliKlasorOptionMetni(moveDestinationSelect()),
                    folderName
            );
        } catch (AssertionError | PlaywrightException e) {
            System.out.println("Move seçili klasör okunamadı: " + e.getMessage());
            return false;
        }
    }

    public void moveIsleminiOnayla() {

        Locator moveButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Move").setExact(true)
        );

        /*
         * Move tamamlandığını PUT /documents/{id}/folder yanıtı kanıtlar.
         * YARIŞ DÜZELTMESİ: ECM başarılı PUT sonrası onMoved -> reload() ile
         * belgeyi async yeniden çekiyor; Move dialog'u açılışta hedefi
         * data.folder.id'den alıyor. Yeniden çekim bitmeden dialog tekrar
         * açılırsa eski klasör görünüyordu. Dış dinleyici tıklamadan ÖNCE
         * kurulduğu için PUT'tan sonraki GET /documents/{id} kaçırılmaz.
         */
        com.microsoft.playwright.Response[] put = new com.microsoft.playwright.Response[1];
        // put[0] iç bekleme DÖNDÜKTEN sonra atanır; GET o aralıkta gelirse dış
        // bekleme onu kaçırıp 15 sn sonra düşüyordu (yükte görüldü). PUT
        // yanıtı, predicate içinde görüldüğü anda işaretlenir.
        boolean[] putSeen = new boolean[1];
        com.microsoft.playwright.Response response;
        try {
            page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}$")
                            && "GET".equals(r.request().method())
                            && putSeen[0],
                    new Page.WaitForResponseOptions()
                            .setTimeout(UI_WAIT_MS),
                    () -> {
                        put[0] = page.waitForResponse(
                                r -> {
                                    boolean match = r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/folder$")
                                            && "PUT".equals(r.request().method());
                                    if (match) {
                                        putSeen[0] = true;
                                    }
                                    return match;
                                },
                                new Page.WaitForResponseOptions()
                                        .setTimeout(10000),
                                moveButton::click
                        );
                        if (!put[0].ok()) {
                            throw new AssertionError(
                                    "Move başarısız: HTTP " + put[0].status() + " " + put[0].text()
                            );
                        }
                    }
            );
            response = put[0];
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    putSeen[0]
                            ? "Move PUT tamamlandı ancak ardından GET /documents/{id} (yeniden yükleme) gelmedi!"
                            : "Move onaylandıktan sonra PUT /documents/{id}/folder yanıtı gelmedi!",
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Move başarısız: HTTP " + response.status() + " " + response.text()
            );
        }
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
        // Move sonrası detay sayfası reload() ile yeniden yükleniyor;
        // anlık count() yerine klasör adının görünmesini bekliyoruz.
        try {
            page.getByText(
                    folderName,
                    new Page.GetByTextOptions().setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(UI_WAIT_MS)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }


    // =========================
    // DOCUMENTS BATCH 13
    // COPY / DESTINATION / INDEPENDENCE
    // =========================

    public boolean copyButonuGorunuyorMu() {
        detayVerisiniBekle(page);

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        return button.count() > 0 && button.first().isVisible();
    }

    public void copyPenceresiniAc() {
        /*
         * YARIŞ DÜZELTMESİ (4 worker'lı koşuda 1 kez görüldü): Copy butonu
         * canEdit = can_write && !lockedByOther(lockStatus) ile render
         * ediliyor; lockStatus, başlık (h1) render edildikten SONRA ayrı bir
         * GET /documents/{id}/lock ile geliyor. Tıklamadan önce hem belge
         * verisinin hem kilit durumunun render edildiğini bekliyoruz.
         * Tıklama tekrarlanmaz: diyalog açılmazsa kanıtla birlikte düşer.
         */
        detayVerisiniBekle(page);
        kilitDurumunuBekle(page);

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Copy")
                        .setExact(true)
        ).first();

        try {
            button.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(UI_WAIT_MS)
            );
        } catch (Exception e) {
            throw new AssertionError("Copy butonu bulunamadı!" + detayDurumKaniti(page), e);
        }

        button.click();

        try {
            copyDialogHeading().first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Copy tıklandı ancak 'Copy document' penceresi açılmadı."
                            + detayDurumKaniti(page)
                            + " [Copy butonu hâlâ görünür: " + button.isVisible()
                            + ", 'Loading document…' görünür: "
                            + page.getByText("Loading document…").isVisible() + "]",
                    e
            );
        }
    }

    /*
     * Check-out kartı (DocumentLockCard.tsx) kilit durumu gelene kadar
     * "Loading…" gösterir; sonra "Not checked out." ya da "Checked out by …".
     * Silinmiş belgede kart hiç render edilmez; bu yardımcı yalnızca aktif
     * belgede kilide bağlı aksiyonlardan önce kullanılır.
     */
    private void kilitDurumunuBekle(Page targetPage) {
        try {
            targetPage.getByText(
                    Pattern.compile("^(Not checked out\\.|Checked out by .+)$")
            ).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Belgenin kilit (check-out) durumu yüklenmedi." + detayDurumKaniti(targetPage),
                    e
            );
        }
    }

    public boolean copyPenceresiGorunuyorMu() {
        Locator heading = copyDialogHeading();
        return heading.count() > 0 && heading.first().isVisible();
    }

    public boolean copyAciklamasiGorunuyorMu() {
        return page.getByText(
                "Creates an independent draft copy of the current version and records a copied_from lineage link.",
                new Page.GetByTextOptions().setExact(true)
        ).count() > 0;
    }

    /*
     * CopyDocumentDialog.tsx: <form> içinde "New title" ve
     * "Destination folder" label'lı alanlar. Önceki sürüm bu alanları
     * count() denemeleriyle (her biri ayrı bir round-trip) arıyordu.
     */
    private Locator copyForm() {
        return page.locator("form").filter(
                new Locator.FilterOptions().setHas(
                        page.locator("label", new Page.LocatorOptions().setHasText(Pattern.compile("^New title$")))
                )
        );
    }

    private Locator copyTitleInput() {
        return copyForm().locator("div:has(> label:text-is('New title')) > input");
    }

    private Locator copyDestinationSelect() {
        return copyForm().locator("div:has(> label:text-is('Destination folder')) select");
    }

    private Locator copyDialogHeading() {
        return page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Copy document").setExact(true)
        );
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
        // FolderPicker önce disabled "Loading folders…" select'i çiziyor;
        // seçili değeri ancak liste yüklendikten sonra okuyoruz.
        try {
            String text = seciliKlasorOptionMetni(copyDestinationSelect());
            return text.equals("Root") || text.endsWith("/ Root") || text.endsWith("/Root");
        } catch (AssertionError | PlaywrightException e) {
            System.out.println("Copy seçili hedef klasör okunamadı: " + e.getMessage());
            return false;
        }
    }

    public void copyHedefKlasorSec(String folderName) {
        klasorOptionSec(copyDestinationSelect(), folderName, "Copy");
    }

    public boolean copyHedefKlasorSeciliMi(String folderName) {
        try {
            return klasorOptionMetniEslesiyorMu(
                    seciliKlasorOptionMetni(copyDestinationSelect()),
                    folderName
            );
        } catch (AssertionError | PlaywrightException e) {
            System.out.println("Copy seçili klasör okunamadı: " + e.getMessage());
            return false;
        }
    }

    public void copyIsleminiIptalEt() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Cancel").setExact(true)
        ).click();
    }

    public void copyPenceresiniXileKapat() {
        // Dialog header'ındaki aria-label="Close" butonu.
        page.locator("header").filter(
                new Locator.FilterOptions().setHas(copyDialogHeading())
        ).getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Close").setExact(true)
        ).click();
    }

    public boolean copyPenceresiKapandiMi() {
        // Anlık count() yerine dialog'un DOM'dan kalkmasını (en fazla 5 sn) bekliyoruz.
        try {
            copyDialogHeading().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.DETACHED)
                            .setTimeout(5000)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    /*
     * Copy'yi onaylar ve tamamlandığını deterministik olarak doğrular:
     * POST /documents/{kaynak}/copy başarılı yanıtı + dialog'un kapanması.
     * 250 ms polling yerine gerçek API yanıtını bekliyoruz.
     * Dönüş: API'nin oluşturduğu kopya belgenin id'si.
     */
    public String copyIsleminiOnayla() {
        Locator submit = copyForm().getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Copy").setExact(true)
        );

        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/copy$")
                            && "POST".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(15000),
                    submit::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Copy onaylandıktan sonra POST /documents/{id}/copy isteği gönderilmedi!",
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Copy başarısız: HTTP " + response.status() + " " + response.text()
            );
        }

        com.google.gson.JsonElement idEl = com.google.gson.JsonParser
                .parseString(response.text())
                .getAsJsonObject()
                .get("document_id");
        String newId = idEl == null || idEl.isJsonNull() ? null : idEl.getAsString();
        if (newId == null || newId.isBlank()) {
            throw new AssertionError(
                    "Copy yanıtında document_id yok: " + response.text()
            );
        }

        if (!copyPenceresiKapandiMi()) {
            throw new AssertionError("Copy işlemi sonrasında pencere kapanmadı!");
        }

        return newId;
    }

    /*
     * Uygulama copy sonrası /documents/{yeniId} adresine gider. API'nin
     * döndürdüğü id'nin detay sayfasının açılmasını ve kopya başlığının
     * render edilmesini bekliyoruz.
     */
    public void kopyaBelgeDetayiniBekle(String copyTitle, String copyDocumentId) {
        try {
            page.waitForURL(
                    url -> url.endsWith("/documents/" + copyDocumentId),
                    new Page.WaitForURLOptions().setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Copy oluşturuldu ancak yeni kopyanın detay sayfasına gidilmedi. "
                            + "Beklenen id: " + copyDocumentId + ", URL: " + page.url(),
                    e
            );
        }

        belgeDetayBasliginiBekle(copyTitle);
    }

    public boolean copyBasligiBoskenButonDisabledMi() {
        Locator buttons = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Copy").setExact(true)
        );
        return buttons.last().isDisabled();
    }

    /*
     * Bilinen detay adresine tam sayfa yüklemesiyle gider ve belge verisi
     * gelene kadar bekler (gerekçe: belgeDetayAdresiniAcBatch13). Ardışık iki
     * navigate arasında SPA'nın refresh isteği yarıda kalmaz.
     */
    public void belgeDetayAdresineGit(String url) {
        belgeDetayAdresiniAcBatch13(url);
    }

    /*
     * Detay adresine gider ve SPA'nın GET /documents/{id} yanıtını (durum ne
     * olursa olsun) bekler: bu yanıt oturum yenilemesinin bittiğini kanıtlar.
     * Belge verisinin yüklenmesini ŞART KOŞMAZ; hard delete edilmiş belgede
     * de (404) kullanılabilir. Sonraki Then adımları durumu kendileri doğrular.
     */
    public void detayAdresiniAcVeYanitiBekle(String url) {
        try {
            page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}$")
                            && "GET".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                    () -> page.navigate(url)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Detay adresi açıldı ancak GET /documents/{id} yanıtı gelmedi "
                            + "(oturum düşmüş olabilir). URL: " + page.url(),
                    e
            );
        }
    }

    public void belgeDetayAdresiniAcBatch13(String url) {
        page.navigate(url);
        page.waitForLoadState();
        // "load" SPA'nın oturum yenilemesinden (GET /auth/me -> POST
        // /auth/refresh) ÖNCE gelir. ECM refresh token'ı tek kullanımlık
        // döndürür (auth.rs); hemen ardından yapılan ikinci navigate yarım
        // kalan refresh'i iptal edince yeni cookie kaybolup /login'e
        // düşülüyordu (paralel koşuda görüldü). Belge verisi yüklenene kadar bekle.
        detayVerisiniBekle(page);
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
                .setTimeout(UI_WAIT_MS));

        addLabelButton.click();

        // React yeniden render ettiği için row'u tekrar alıyoruz.
        row = versionHistoryRow(version);

        Locator input =
                row.locator("input[type='text']").first();

        input.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(UI_WAIT_MS));
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
    /*
     * YARIŞ DÜZELTMESİ: eskiden "Add" tıklanıp hemen dönülüyordu; ardından
     * gelen refresh / silme adımı POST isteği bitmeden çalışıp etiketi
     * bulamıyordu (ya da istek reload ile iptal oluyordu). Şimdi
     * POST /documents/{id}/versions/{vid}/labels yanıtını bekliyoruz;
     * başarılıysa etiketin satırda render edilmesini de bekliyoruz.
     * Başarısız yanıt (ör. doğrulama hatası) burada hata vermez: UI'ın hata
     * gösterdiği senaryolar sonraki Then adımlarıyla doğrulanıyor.
     */
    public void versionLabelEkle(String version) {
        Locator row = versionHistoryRow(version);

        String label = row.locator("input[type='text']").first().inputValue().trim();

        Locator addButton = row.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("Add")
                        .setExact(true)
        );

        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/versions/[0-9a-fA-F-]{36}/labels$")
                            && "POST".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(10000),
                    addButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Version label Add tıklandıktan sonra POST .../labels isteği gönderilmedi: " + label,
                    e
            );
        }

        if (response.ok() && !label.isEmpty()) {
            versionHistoryRow(version).locator(
                    "button[aria-label='Remove label " + label + "']"
            ).waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));
        }
    }

    public void versionLabelIptalEt(String version) {
        Locator row = versionHistoryRow(version);
        row.getByText("Cancel", new Locator.GetByTextOptions().setExact(true)).click();

        // Sabit 250 ms yerine label editörünün kapanmasını bekliyoruz.
        // Kapanmazsa sonraki Then adımları durumu kendi assertion'larıyla yakalar.
        try {
            row.locator("input[type='text']").first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5000)
            );
        } catch (PlaywrightException ignored) {
        }
    }

    public boolean versionLabelGorunuyorMu(String version, String label) {
        Locator row = versionHistoryRow(version);

        Locator removeButton = row.locator(
                "button[aria-label='Remove label " + label + "']"
        );

        try {
            removeButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(UI_WAIT_MS));

            return removeButton.isVisible();
        } catch (PlaywrightException e) {
            return false;
        }
    }
    /*
     * Eskiden !versionLabelGorunuyorMu(): etiketin GÖRÜNMESİNİ 5 sn bekliyordu.
     * Silme sonrası (DELETE bitmeden) etiket hâlâ ekrandaysa hemen "görünüyor"
     * dönüp yanlış hata veriyordu; doğru durumda ise her seferinde 5 sn
     * harcıyordu. Şimdi etiketin DOM'dan kalkmasını bekliyoruz.
     */
    public boolean versionLabelGorunmuyorMu(String version, String label) {
        try {
            versionHistoryRow(version).locator(
                    "button[aria-label='Remove label " + label + "']"
            ).waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.DETACHED)
                    .setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public void versionLabelSil(String version, String label) {
        Locator row = versionHistoryRow(version);

        Locator removeButton = row.locator(
                "button[aria-label='Remove label " + label + "']"
        );

        // Anlık count()==0 yerine etiketin render edilmesini bekle.
        try {
            removeButton.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Silinecek version label bulunamadı: " + label,
                    e
            );
        }

        com.microsoft.playwright.Response response;
        try {
            response = page.waitForResponse(
                    r -> r.url().matches(".*/v1/documents/[0-9a-fA-F-]{36}/versions/[0-9a-fA-F-]{36}/labels/.+")
                            && "DELETE".equals(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(10000),
                    removeButton::click
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Version label silme tıklandıktan sonra DELETE isteği gönderilmedi: " + label,
                    e
            );
        }

        if (!response.ok()) {
            throw new AssertionError(
                    "Version label silinemedi: HTTP " + response.status() + " " + response.text()
            );
        }
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
                        .setTimeout(UI_WAIT_MS)
        );

        confirm.click();
    }

    public void renameIptalEt() {

        Locator cancel =
                page.locator("button[title='Cancel']");

        cancel.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(UI_WAIT_MS)
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

        // Aynı iki koşul (yeni başlık görünür + rename editörü kapalı),
        // 250 ms polling yerine Playwright'ın auto-wait'iyle.
        try {
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(expectedTitle)
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            page.locator("button[title='Confirm rename']").first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Document rename tamamlanmadı. Beklenen title: "
                            + expectedTitle,
                    e
            );
        }
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
                        .setTimeout(UI_WAIT_MS)
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
                        .setTimeout(UI_WAIT_MS)
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
                            .setTimeout(UI_WAIT_MS)
            );

            return comments.first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }
    // =====================================================
    // BATCH 18 - DOCUMENT RELATIONSHIPS
    // =====================================================

    public boolean relationshipsAlaniGorunuyorMu() {

        try {
            Locator heading = page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName("Relationships")
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

    public void linkDocumentPenceresiniAc() {

        Locator button = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Link document")
                        .setExact(true)
        );

        button.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(UI_WAIT_MS)
        );

        button.click();
    }

    public boolean linkDocumentPenceresiGorunuyorMu() {

        try {
            Locator heading = page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName("Link document")
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

    public boolean targetDocumentIdAlaniGorunuyorMu() {

        try {
            Locator input = page.locator(
                    "input[placeholder='Paste the target document UUID']"
            );

            input.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000)
            );

            return input.isVisible();

        } catch (Exception e) {
            return false;
        }
    }

    public boolean floatToLatestSeciliMi() {

        try {
            Locator radio = page.locator(
                    "input[name='binding'][value='LATEST']"
            );

            radio.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(5000)
            );

            return radio.isChecked();

        } catch (Exception e) {
            return false;
        }
    }

    public String mevcutDocumentId() {

        String url = page.url();

        String marker = "/documents/";
        int start = url.indexOf(marker);

        if (start < 0) {
            throw new AssertionError(
                    "Document ID URL'den alınamadı. URL: " + url
            );
        }

        String id = url.substring(start + marker.length());

        int queryIndex = id.indexOf('?');
        if (queryIndex >= 0) {
            id = id.substring(0, queryIndex);
        }

        int hashIndex = id.indexOf('#');
        if (hashIndex >= 0) {
            id = id.substring(0, hashIndex);
        }

        int slashIndex = id.indexOf('/');
        if (slashIndex >= 0) {
            id = id.substring(0, slashIndex);
        }

        if (id.isBlank()) {
            throw new AssertionError(
                    "Document ID URL'de boş. URL: " + url
            );
        }

        return id;
    }

    public void documentDetayinaGit(String documentId) {

        if (documentId == null || documentId.isBlank()) {
            throw new AssertionError(
                    "Document detail açılamadı: document ID boş!"
            );
        }

        page.navigate(
                BrowserManager.getBaseUrl() + "/documents/" + documentId
        );

        page.waitForLoadState();

        if (!belgeDetaySayfasindaMi()) {
            throw new AssertionError(
                    "Document detail açılamadı. ID: " + documentId
                            + " URL: " + page.url()
            );
        }
    }

    public void targetDocumentIdGir(String documentId) {

        Locator input = page.locator(
                "input[placeholder='Paste the target document UUID']"
        );

        input.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(UI_WAIT_MS)
        );

        input.fill(documentId);
    }

    public void pinToSpecificVersionSec() {

        Locator radio = page.locator(
                "input[name='binding'][value='SPECIFIC']"
        );

        radio.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(UI_WAIT_MS)
        );

        radio.check();
    }

    public boolean targetVersionAlaniGorunuyorMu() {

        try {
            Locator label = page.getByText(
                    "Target version",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            label.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            return label.isVisible();

        } catch (Exception e) {
            return false;
        }
    }

}
