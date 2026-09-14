package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class BrowserManager {

    public static Playwright playwright;
    public static Browser browser;
    public static Page page;

    public static void startBrowser() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(500)
        );

        page = browser.newPage();
    }

    public static void closeBrowser() {

        try {
            if (browser != null && browser.isConnected()) {
                browser.close();
            }
        } catch (Exception ignored) {
        } finally {
            browser = null;
            page = null;
        }

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