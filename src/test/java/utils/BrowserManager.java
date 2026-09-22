package utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

public class BrowserManager {

    public static Playwright playwright;
    public static Browser browser;
    public static Page page;

    public static BrowserContext managerContext;
    public static Page managerPage;

    private static final String DEFAULT_BASE_URL = "http://localhost:5173";

    private static String currentScenarioName;
    private static String currentScenarioUri;
    private static String currentScenarioId;

    private static final Set<String> scenarioEndpoints =
            new LinkedHashSet<>();

    private BrowserManager() {
    }

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

    public static void startScenario(
            String scenarioName,
            String scenarioUri,
            String scenarioId
    ) {
        RuntimeEndpointCoverage.initializeRun();

        currentScenarioName = scenarioName;
        currentScenarioUri = scenarioUri;
        currentScenarioId = scenarioId;

        scenarioEndpoints.clear();

        System.out.println(
                "[SCENARIO] " + currentScenarioName
        );

        System.out.println(
                "[SCENARIO URI] " + currentScenarioUri
        );

        System.out.println(
                "[SCENARIO ID] " + currentScenarioId
        );
    }

    public static void finishScenario() {
        if (currentScenarioName == null) {
            return;
        }

        System.out.println(
                "[COVERAGE] Scenario: " + currentScenarioName
        );

        for (String endpoint : scenarioEndpoints) {
            System.out.println(
                    "[COVERAGE] " + endpoint
            );

            RuntimeEndpointCoverage.record(
                    currentScenarioId,
                    currentScenarioUri,
                    currentScenarioName,
                    endpoint
            );
        }

        currentScenarioName = null;
        currentScenarioUri = null;
        currentScenarioId = null;

        scenarioEndpoints.clear();
    }

    public static void startBrowser() {

        playwright = Playwright.create();

        browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(isCi())
                        .setSlowMo(isCi() ? 0 : 500)
        );

        page = browser.newPage();

        attachEndpointListener(page);
    }

    public static Page createManagerPage() {

        managerContext = browser.newContext();

        managerPage = managerContext.newPage();

        attachEndpointListener(managerPage);

        return managerPage;
    }

    private static void attachEndpointListener(Page targetPage) {

        targetPage.onRequest(request -> {
            try {
                URI uri = URI.create(request.url());

                String path = uri.getPath();

                if (path == null || !path.startsWith("/v1/")) {
                    return;
                }

                String normalizedPath =
                        EndpointTemplateMatcher.match(
                                request.method(),
                                path
                        );

                String endpoint =
                        request.method().toUpperCase()
                                + " "
                                + normalizedPath;

                scenarioEndpoints.add(endpoint);

                System.out.println(
                        "[ENDPOINT] " + endpoint
                );

            } catch (Exception ignored) {
                // Coverage collection must never fail the test.
            }
        });
    }

    public static void closeBrowser() {

        try {
            if (managerContext != null) {
                managerContext.close();
            }
        } catch (Exception ignored) {
        } finally {
            managerContext = null;
            managerPage = null;
        }

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