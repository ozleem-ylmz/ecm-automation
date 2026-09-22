package steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import utils.BrowserManager;

public class Hooks {

    @Before
    public void beforeScenario(Scenario scenario) {
        BrowserManager.startScenario(
                scenario.getName(),
                scenario.getUri().toString(),
                scenario.getId()
        );
    }

    @After
    public void tearDown() {
        BrowserManager.finishScenario();
        BrowserManager.closeBrowser();
    }
}