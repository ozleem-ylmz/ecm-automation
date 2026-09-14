package pages;

import com.microsoft.playwright.Page;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public void loginSayfasiniAc() {
        page.navigate("http://localhost:5173/login");
    }

    public void emailGir(String email) {
        page.locator("input[type='email']").fill(email);
    }

    public void sifreGir(String password) {
        page.locator("input[type='password']").fill(password);
    }

    public void girisYapButonunaTikla() {
        page.locator("button[type='submit']").click();
    }

    public boolean klasorlerSayfasindaMi() {
        return page.url().contains("/folders");
    }
    public boolean loginSayfasindaMi() {
        return page.url().contains("/login")
                && page.locator("input[type='password']").isVisible();
    }
}