package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class BrowserManager {

    public static Playwright playwright;
    public static Browser browser;
    public static Page page;

    // Manager için admin'den bağımsız ikinci oturum
    public static BrowserContext managerContext;
    public static Page managerPage;

    private static final String DEFAULT_BASE_URL = "http://localhost:5173";

    public static String getBaseUrl() {
        String envBaseUrl = System.getenv("ECM_BASE_URL");

        if (envBaseUrl == null || envBaseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }

        return envBaseUrl.replaceAll("/+$", "");
    }

    public static boolean isCi() {
        String ci = System.getenv("CI");
        return ci != null && ci.equalsIgnoreCase("true");
    }

    public static void startBrowser() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(isCi())
                        .setSlowMo(isCi() ? 0 : 500)
        );

        page = browser.newPage();
    }

    public static Page createManagerPage() {

        managerContext = browser.newContext();
        managerPage = managerContext.newPage();

        return managerPage;
    }

    public static void closeBrowser() {

        // Önce manager oturumunu kapat
        try {
            if (managerContext != null) {
                managerContext.close();
            }
        } catch (Exception ignored) {
        } finally {
            managerContext = null;
            managerPage = null;
        }

        // Sonra browser'ı kapat
        try {
            if (browser != null && browser.isConnected()) {
                browser.close();
            }
        } catch (Exception ignored) {
        } finally {
            browser = null;
            page = null;
        }

        // En son Playwright'ı kapat
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        } finally {
            playwright = null;
        }
    }
}