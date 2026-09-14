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

    @When("kullanıcı {string} ve {string} ile giriş yapar")
    public void kullaniciBilgileriyleGirisYapar(String email, String password) {

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
    @Then("kullanıcı login sayfasında kalmalıdır")
    public void kullaniciLoginSayfasindaKalmalidir() {

        if (!loginPage.loginSayfasindaMi()) {
            throw new AssertionError("Kullanıcı login sayfasında kalmadı!");
        }
    }
}