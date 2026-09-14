package steps;

import io.cucumber.java.After;
import utils.BrowserManager;

public class Hooks {

    @After
    public void tearDown() {
        BrowserManager.closeBrowser();
    }
}