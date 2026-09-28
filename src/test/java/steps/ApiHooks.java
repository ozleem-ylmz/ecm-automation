package steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import utils.ApiTestContext;

/*
 * API tabanlı senaryoların kendi yaşam döngüsü. Hooks.java'ya dokunmadan
 * çalışır: Cucumber glue tüm "steps" paketini taradığı için bu sınıf
 * otomatik bulunur.
 *
 * @After order: Cucumber'da yüksek order'lı After önce çalışır; temizlik,
 * Hooks.tearDown (varsayılan 10000) tarayıcıyı kapatmadan önce yapılır.
 */
public class ApiHooks {

    @Before(order = 5)
    public void apiSenaryoBaslat(Scenario scenario) {
        ApiTestContext.start(
                scenario.getId(),
                scenario.getUri().toString(),
                scenario.getName()
        );
    }

    @After(order = 20000)
    public void apiTestVerisiniTemizle() {
        ApiTestContext.runCleanup();
    }
}
