package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import utils.BrowserManager;

import java.util.concurrent.atomic.AtomicInteger;

public class StorageGcPage {

    private final Page page;
    private final AtomicInteger versionDeleteRequestCount = new AtomicInteger(0);

    public StorageGcPage(Page page) {
        this.page = page;

        page.onRequest(request -> {
            if ("POST".equals(request.method())
                    && request.url().contains("/v1/admin/storage-gc/versions/delete")) {
                versionDeleteRequestCount.incrementAndGet();
            }
        });
    }

    public void sayfayiAc() {
        page.navigate(BrowserManager.getBaseUrl() + "/admin/storage-gc");

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Storage Garbage Collector")
        ).waitFor();
    }

    public boolean baslikGorunuyorMu() {
        return page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Storage Garbage Collector")
        ).isVisible();
    }

    public boolean dryRunSeciliMi() {
        Locator dryRun = page.getByLabel("Dry run");
        return dryRun.isVisible() && dryRun.isChecked();
    }

    public boolean previewButonuGorunuyorMu() {
        return page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Preview (dry run)")
        ).isVisible();
    }

    public Response previewCalistir() {
        Locator previewButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Preview (dry run)")
        );

        return page.waitForResponse(
                response ->
                        response.url().contains("/v1/admin/storage-gc")
                                && !response.url().contains("/versions")
                                && "POST".equals(response.request().method()),
                previewButton::click
        );
    }

    public boolean requestDryRunTrueMu(Response response) {
        if (response == null) {
            return false;
        }

        String body = response.request().postData();

        if (body == null) {
            return false;
        }

        String normalized = body.replaceAll("\\s+", "");

        return normalized.contains("\"dry_run\":true");
    }

    public boolean responseBasariliMi(Response response) {
        if (response == null) {
            return false;
        }

        return response.status() >= 200 && response.status() < 300;
    }

    public boolean sonucAlaniGorunuyorMu() {
        Locator orphansScanned = page.getByText(
                "Orphans scanned",
                new Page.GetByTextOptions().setExact(true)
        ).first();

        Locator wouldDelete = page.getByText(
                "Would delete",
                new Page.GetByTextOptions().setExact(true)
        ).first();

        Locator failed = page.getByText(
                "Failed",
                new Page.GetByTextOptions().setExact(true)
        ).first();

        try {
            orphansScanned.waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );

            wouldDelete.waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );

            failed.waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );

            return orphansScanned.isVisible()
                    && wouldDelete.isVisible()
                    && failed.isVisible();

        } catch (Exception e) {
            System.out.println(
                    "Storage GC sonuç kartları beklenirken hata: "
                            + e.getMessage()
            );

            return false;
        }
    }

    public void ilkSurumuSec() {
        Locator checkboxes = page.locator(
                "input[aria-label^='Select version ']"
        );

        checkboxes.first().waitFor();

        Locator first = checkboxes.first();

        if (!first.isChecked()) {
            first.check();
        }
    }

    public boolean ilkSurumSeciliMi() {
        Locator checkboxes = page.locator(
                "input[aria-label^='Select version ']"
        );

        return checkboxes.count() > 0
                && checkboxes.first().isChecked();
    }

    public void versionDeleteRequestSayaciniSifirla() {
        versionDeleteRequestCount.set(0);
    }

    public void deleteSelectedButonunaTikla() {
        Locator button = page.locator("button")
                .filter(
                        new Locator.FilterOptions()
                                .setHasText("Delete selected")
                )
                .first();

        button.click();
    }

    public boolean ilkOnayPenceresiGorunuyorMu() {
        return page.getByText(
                "Delete selected versions?",
                new Page.GetByTextOptions().setExact(true)
        ).isVisible();
    }

    public void ilkOnaydaCancelTikla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Cancel")
                        .setExact(true)
        ).click();
    }

    public void ilkOnaydaContinueTikla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Continue")
                        .setExact(true)
        ).click();
    }

    public boolean ikinciOnayPenceresiGorunuyorMu() {
        return page.getByText(
                "This cannot be undone",
                new Page.GetByTextOptions().setExact(true)
        ).isVisible();
    }

    public void ikinciOnaydaCancelTikla() {
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Cancel")
                        .setExact(true)
        ).click();
    }

    public boolean versionDeleteRequestGonderilmediMi() {
        page.waitForTimeout(500);
        return versionDeleteRequestCount.get() == 0;
    }

    private Locator testBelgesiSurumCheckboxlari(String documentTitle) {
        return page.locator(
                "input[aria-label^=\"Select version \"][aria-label$=\" of "
                        + documentTitle
                        + "\"]"
        );
    }

    public Response seciliSurumuIkiOnaylaSil() {
        versionDeleteRequestSayaciniSifirla();

        deleteSelectedButonunaTikla();

        if (!ilkOnayPenceresiGorunuyorMu()) {
            throw new AssertionError(
                    "lk Storage GC silme onayÄ± gÃ¶rÃ¼ntÃ¼lenmedi!"
            );
        }

        ilkOnaydaContinueTikla();

        if (!ikinciOnayPenceresiGorunuyorMu()) {
            throw new AssertionError(
                    "kinci Storage GC silme onayÄ± gÃ¶rÃ¼ntÃ¼lenmedi!"
            );
        }

        Locator deleteButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Delete 1")
                        .setExact(true)
        );

        return page.waitForResponse(
                response ->
                        response.url().contains(
                                "/v1/admin/storage-gc/versions/delete"
                        )
                                && "POST".equals(
                                        response.request().method()
                                ),
                deleteButton::click
        );
    }

    public boolean versionDeleteRequestBirKezGonderildiMi() {
        return versionDeleteRequestCount.get() == 1;
    }
    public void testBelgesiSurumuOlanaKadarBekle(
            String documentTitle
    ) {
        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {
            if (testBelgesiSurumCheckboxlari(documentTitle).count() >= 1) {
                return;
            }

            page.waitForTimeout(250);
        }

        throw new AssertionError(
                "Storage GC listesinde test belgesinin sürümü bulunamadı: "
                        + documentTitle
        );
    }

    public void testBelgesininSurumunuSec(
            String documentTitle
    ) {
        Locator versions =
                testBelgesiSurumCheckboxlari(documentTitle);

        if (versions.count() != 1) {
            throw new AssertionError(
                    "Test belgesi için tam 1 sürüm bekleniyordu. Bulunan: "
                            + versions.count()
            );
        }

        Locator version = versions.first();

        if (!version.isChecked()) {
            version.check();
        }

        if (!version.isChecked()) {
            throw new AssertionError(
                    "Storage GC test sürümü seçilemedi!"
            );
        }
    }

    public void testBelgesiSilineneKadarBekle(
            String documentTitle
    ) {
        long deadline = System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < deadline) {
            if (testBelgesiSurumCheckboxlari(documentTitle).count() == 0) {
                return;
            }

            page.waitForTimeout(250);
        }
    }

    public boolean testBelgesiArtikYokMu(
            String documentTitle
    ) {
        return testBelgesiSurumCheckboxlari(documentTitle).count() == 0;
    }
}