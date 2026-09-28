package steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import pages.LoginPage;
import io.cucumber.java.en.Then;
import utils.BrowserManager;

public class LoginSteps {

    private LoginPage loginPage;

    @Given("kullanıcı ECM login sayfasındadır")
    public void kullaniciEcmLoginSayfasindadir() {

        BrowserManager.startBrowser();

        loginPage = new LoginPage(BrowserManager.page);

        loginPage.loginSayfasiniAc();
    }

    // Strengthen S-11: the login response is recorded (listener only; nothing waits here), so a
    // negative scenario can assert AFTER the server answered. Empty-field scenarios send no request
    // and are unaffected.
    private final java.util.List<com.microsoft.playwright.Response> loginResponses =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    @When("kullanıcı {string} ve {string} ile giriş yapar")
    public void kullaniciBilgileriyleGirisYapar(String email, String password) {

        BrowserManager.page.onResponse(r -> {
            if ("POST".equals(r.request().method()) && r.url().endsWith("/v1/auth/login")) {
                loginResponses.add(r);
            }
        });
        loginPage.emailGir(email);
        loginPage.sifreGir(password);
        loginPage.girisYapButonunaTikla();
    }
    @Then("klasörler sayfası görüntülenmelidir")
    public void klasorlerSayfasiGoruntulenmelidir() {

        if (!loginPage.klasorlerSayfasindaMi()) {
            throw new AssertionError("Klasörler sayfası görüntülenmedi!");
        }
    }
    /*
     * Strengthen S-11 (login.feature wrong password / wrong email): waits for the actual
     * POST /v1/auth/login response (api/rest/auth.rs login → InvalidCredentials = 401), then asserts
     * the browser is still on /login, the server's error message is rendered (Login.tsx shows
     * err.message) and no refresh_token session cookie was issued.
     */
    @Then("giriş isteği sunucudan 401 ile reddedilmeli ve sunucunun hata mesajı login sayfasında görünmelidir")
    public void girisIstegi401IleReddedilmeli() {
        com.microsoft.playwright.Page page = BrowserManager.page;
        page.waitForCondition(() -> !loginResponses.isEmpty(),
                new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(15000));
        com.microsoft.playwright.Response r = loginResponses.get(loginResponses.size() - 1);
        String body = r.text();
        if (r.status() != 401) {
            throw new AssertionError("Hatalı giriş 401 ile reddedilmeliydi, HTTP " + r.status());
        }
        String message = com.google.gson.JsonParser.parseString(body).getAsJsonObject()
                .getAsJsonObject("error").get("message").getAsString();
        page.getByText(message, new com.microsoft.playwright.Page.GetByTextOptions().setExact(true)).first()
                .waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(15000));
        boolean sessionCookie = page.context().cookies().stream().anyMatch(c -> "refresh_token".equals(c.name));
        io.qameta.allure.Allure.addAttachment("Giriş reddi kanıtı", "text/plain",
                "POST /v1/auth/login -> " + r.status() + " '" + message + "'\nURL (yanıttan sonra): " + page.url()
                        + "\nrefresh_token çerezi: " + sessionCookie);
        if (!page.url().contains("/login") || sessionCookie) {
            throw new AssertionError("Reddedilen girişten sonra login sayfasında kalınmalı ve oturum çerezi oluşmamalıydı (URL "
                    + page.url() + ", çerez " + sessionCookie + ")");
        }
    }

    @Then("kullanıcı login sayfasında kalmalıdır")
    public void kullaniciLoginSayfasindaKalmalidir() {

        if (!loginPage.loginSayfasindaMi()) {
            throw new AssertionError("Kullanıcı login sayfasında kalmadı!");
        }
    }
}