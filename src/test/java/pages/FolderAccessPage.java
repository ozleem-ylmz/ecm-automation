package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import utils.BrowserManager;

/*
 * Klasör detayının yetki odaklı görünümü. FolderDetail.tsx iki ayrı erişim
 * reddi dalı render eder:
 *
 *  1) KLASÖRÜN KENDİSİ reddedildiğinde (GET /folders/{id} -> 403):
 *     getFolder hatası setError(err.message) ile sayfa üstünde gösterilir
 *     (FolderDetail.tsx:111-119, 707-712). Metin backend'den gelir:
 *     src/engine/acl/runtime.rs:807 access_denied_folder(Read)
 *     "You do not have permission to view this folder. Ask an administrator
 *     if you need access." (assert_folder_access, runtime.rs:72-107).
 *
 *  2) Klasör açılabildiği hâlde yalnızca BELGE LİSTESİ reddedildiğinde
 *     (GET /folders/{id}/documents -> 403): FolderDetail.tsx:139-141
 *     "You do not have read access to this folder."
 */
public class FolderAccessPage {

    /* Dal 2: klasör görünür, belge listesi 403. */
    private static final String NO_READ_ACCESS =
            "You do not have read access to this folder.";

    /* Dal 1: klasörün kendisi 403 (runtime.rs:807, AclPermission::Read). */
    public static final String FOLDER_VIEW_DENIED =
            "You do not have permission to view this folder. Ask an administrator if you need access.";

    private final Page page;

    public FolderAccessPage(Page page) {
        this.page = page;
    }

    public void klasorDetayiniAc(String folderId) {
        page.navigate(BrowserManager.getBaseUrl() + "/folders/" + folderId);
        page.waitForLoadState();
    }

    /*
     * Klasör detayını açar ve UI'nin kendi GET /v1/folders/{id} yanıtını
     * yakalar (belge listesi isteği değil). Dinleyici navigasyondan önce
     * kurulur.
     */
    public Response klasorDetayiniAcVeKlasorYanitiniAl(String folderId) {
        String path = "/v1/folders/" + folderId;
        try {
            return page.waitForResponse(
                    r -> "GET".equals(r.request().method()) && r.url().endsWith(path),
                    new Page.WaitForResponseOptions().setTimeout(15000),
                    () -> page.navigate(BrowserManager.getBaseUrl() + "/folders/" + folderId)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError("Klasör detay sayfası GET " + path + " isteğini göndermedi. URL: "
                    + page.url(), e);
        }
    }

    /* Dal 2 kontrolü (klasör açılabiliyor, belge listesi reddediliyor). */
    public boolean okumaErisimiYokMesajiGorunuyorMu() {
        try {
            page.getByText(NO_READ_ACCESS).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
            return true;
        } catch (Exception e) {
            System.out.println("Okuma erişimi mesajı görünmedi. URL: " + page.url());
            return false;
        }
    }

    /* Dal 1 kontrolü (klasörün kendisi reddediliyor). */
    public boolean klasorGoruntulemeReddiMesajiGorunuyorMu() {
        try {
            page.getByText(FOLDER_VIEW_DENIED).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
            return true;
        } catch (Exception e) {
            System.out.println("Klasör görüntüleme reddi mesajı görünmedi. URL: " + page.url());
            return false;
        }
    }
}
