package steps;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import utils.BrowserManager;
import utils.ScenarioLedger;

public class Hooks {

    // Fail-fast: yanlış uygulamaya/porta karşı koşuyu senaryolar başlamadan durdurur.
    @BeforeAll
    public static void verifyEnvironment() {
        BrowserManager.verifyEnvironment();
    }

    private long scenarioStart;

    @Before
    public void beforeScenario(Scenario scenario) {
        scenarioStart = System.nanoTime();
        BrowserManager.startScenario(
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getId()
        );
    }

    @After
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            attachFailureEvidence(scenario);
        }
        BrowserManager.finishScenario();
        // Sadece senaryonun context'lerini kapatır; browser process'i
        // sonraki senaryolar için açık kalır.
        BrowserManager.closeBrowser();
        ScenarioLedger.record(
                scenario.getUri().toString(),
                scenario.getLine(),
                scenario.getName(),
                scenario.getStatus().name(),
                (System.nanoTime() - scenarioStart) / 1_000_000
        );
    }

    /*
     * Başarısız senaryonun kanıtı context kapanmadan alınır: ana (ve varsa
     * manager) sayfanın URL'i + tam sayfa ekran görüntüsü. Cucumber eki olarak
     * Allure raporuna düşer; paralel koşuda da shard'lar arası karışmaz.
     */
    private static void attachFailureEvidence(Scenario scenario) {
        attachPage(scenario, BrowserManager.page, "admin");
        attachPage(scenario, BrowserManager.managerPage, "manager");
    }

    private static void attachPage(Scenario scenario, com.microsoft.playwright.Page page, String role) {
        if (page == null || page.isClosed()) {
            return;
        }
        try {
            scenario.log("[" + role + "] URL at failure: " + page.url());
            scenario.attach(
                    page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setFullPage(true)),
                    "image/png",
                    "failure-" + role
            );
        } catch (Exception e) {
            scenario.log("[" + role + "] failure screenshot alınamadı: " + e.getMessage());
        }
    }

    @AfterAll
    public static void afterAll() {
        BrowserManager.shutdown();
    }
}