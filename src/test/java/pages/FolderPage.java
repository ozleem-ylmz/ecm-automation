package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import utils.BrowserManager;

public class FolderPage {

    private final Page page;

    public FolderPage(Page page) {
        this.page = page;
    }

    public void klasorlerSayfasiniAc() {

        page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("Folders")
                        .setExact(true)
        ).first().click();
    }

    public boolean klasorlerSayfasindaMi() {
        return page.url().contains("/folders");
    }

    /*
     * /folders sayfasına doğrudan gider (login sonrası uygulamanın açtığı
     * sayfa) ve "Create a new folder" formunun render edilmesini bekler.
     */
    public void klasorlerSayfasinaDogrudanGit() {

        page.navigate(BrowserManager.getBaseUrl() + "/folders");

        page.getByText("Create a new folder").waitFor(
                new Locator.WaitForOptions().setTimeout(10000)
        );
    }

    /*
     * ID'si bilinen klasörün detayına doğrudan gider (tüm klasör ağacında
     * isim aramadan) ve h1'de klasör adının render edilmesini bekler.
     */
    public void klasorDetayinaGit(String folderId, String folderName) {

        page.navigate(BrowserManager.getBaseUrl() + "/folders/" + folderId);

        try {
            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(folderName)
                            .setExact(true)
                            .setLevel(1)
            ).waitFor(
                    new Locator.WaitForOptions().setTimeout(10000)
            );
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Klasör detay sayfası açılamadı: " + folderName
                            + " (" + folderId + ") URL: " + page.url(),
                    e
            );
        }
    }

    public void klasorOlustur(String folderName) {

        page.getByText("Create a new folder").waitFor();

        page.locator("input[placeholder='e.g. Finance']")
                .fill(folderName);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create folder")
                        .setExact(true)
        ).click();

        page.getByText(
                folderName,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().waitFor();
    }

    public boolean klasorGorunuyorMu(String folderName) {

        try {

            page.getByText(
                    folderName,
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void klasoruAc(String folderName) {

        page.getByText(
                folderName,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().click();
    }

    public boolean klasorDetaySayfasindaMi() {

        return page.url().matches(".*/folders/[^/]+.*");
    }

    public void altKlasorOlustur(
            String parentFolder,
            String childFolder
    ) {

        klasoruAc(parentFolder);

        page.locator("input[placeholder='Subfolder name']")
                .fill(childFolder);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        ).click();

        page.getByText(
                childFolder,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().waitFor();
    }

    public void altKlasorOlusturDetaySayfasinda(String childFolder) {

        page.locator("input[placeholder='Subfolder name']")
                .fill(childFolder);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        ).click();

        page.getByText(
                childFolder,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().waitFor();
    }

    public boolean altKlasorGorunuyorMu(String childFolder) {

        // Anlık isVisible() yerine (sayfa henüz render edilmemiş olabilir)
        // pozitif bekleme.
        try {
            page.getByText(
                    childFolder,
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public void klasoruYenidenAdlandir(String newName) {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Rename")
                        .setExact(true)
        ).click();

        Locator renameInput = page.locator(
                "input[maxlength='255']:not([placeholder='Subfolder name'])"
        );

        renameInput.waitFor();
        renameInput.fill(newName);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Save")
                        .setExact(true)
        ).click();

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName(newName)
                        .setExact(true)
        ).waitFor();
    }

    public boolean klasorBasligiMi(String folderName) {

        try {

            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName(folderName)
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void klasoruPasifYap() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Inactivate")
                        .setExact(true)
        ).click();

        page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Inactivate folder?")
                        .setExact(true)
        ).waitFor();

        page.getByRole(AriaRole.ALERTDIALOG)
                .getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions()
                                .setName("Inactivate")
                                .setExact(true)
                )
                .click();

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Re-activate")
                        .setExact(true)
        ).waitFor();
    }

    public void klasoruAktifYap() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Re-activate")
                        .setExact(true)
        ).click();

        page.getByRole(AriaRole.ALERTDIALOG)
                .getByRole(
                        AriaRole.BUTTON,
                        new Locator.GetByRoleOptions()
                                .setName("Re-activate")
                                .setExact(true)
                )
                .click();

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Inactivate")
                        .setExact(true)
        ).waitFor();
    }

    public boolean klasorPasifMi() {

        try {

            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Re-activate")
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /*
     * Ön koşul dallanması için: klasörün mevcut durumunu, "Inactivate" veya
     * "Re-activate" butonundan HANGİSİ render edilirse ona göre döner.
     * klasorAktifMi() pasif klasörde 5 sn timeout'a kadar bekliyordu.
     */
    public boolean klasorSuAnAktifMi() {

        Locator inactivate = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Inactivate")
                        .setExact(true)
        );

        Locator reactivate = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Re-activate")
                        .setExact(true)
        );

        inactivate.or(reactivate).first().waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(10000)
        );

        return inactivate.count() > 0;
    }

    public boolean klasorAktifMi() {

        try {

            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Inactivate")
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void historyAc() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("History")
                        .setExact(true)
        ).click();
    }

    /*
     * HistoryTimeline.tsx: "History" <h2>'si toggle butonunun İÇİNDE; eski
     * kontrol (getByText("History").isVisible()) panel açılmasa da true
     * dönüyordu. Panel açıldığında timeline girdileri (ol > li) veya
     * "No activity recorded yet." görünür; onu bekliyoruz.
     */
    public boolean historyGorunuyorMu() {

        Locator section = page.locator("section").filter(
                new Locator.FilterOptions().setHas(
                        page.getByRole(
                                AriaRole.HEADING,
                                new Page.GetByRoleOptions().setName("History").setExact(true)
                        )
                )
        );

        Locator content = section.locator("ol > li")
                .or(section.getByText("No activity recorded yet.", new Locator.GetByTextOptions().setExact(true)));

        try {
            content.first().waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public boolean aclAlaniGorunuyorMu() {
        return metinGorunurMu("Folder access");
    }

    public boolean allowedClassesAlaniGorunuyorMu() {
        return metinGorunurMu("Allowed document classes");
    }

    private boolean metinGorunurMu(String text) {
        // Anlık isVisible() yerine pozitif bekleme (kartlar async render oluyor).
        try {
            page.getByText(
                    text,
                    new Page.GetByTextOptions().setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions().setTimeout(5000)
            );
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    /*
     * Alt klasörün breadcrumb'ı (BreadcrumbNav.tsx: nav[aria-label=Breadcrumb],
     * öğeler title=<ad>) parent klasörü göstermeli. Eski kontrol
     * getByText("Folders") idi; sol menüdeki "Folders" linki her sayfada
     * olduğu için breadcrumb olmasa da true dönüyordu.
     */
    public boolean breadcrumbGorunuyorMu(String parentName) {
        try {
            page.locator("nav[aria-label='Breadcrumb']")
                    .locator("[title=\"" + parentName + "\"]")
                    .first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    public void klasorOlusturBeklemeden(String folderName) {

        page.locator("input[placeholder='e.g. Finance']")
                .fill(folderName);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create folder")
                        .setExact(true)
        ).click();
    }

    public void altKlasorOlusturBeklemeden(String childFolder) {

        Locator input = page.locator(
                "input[placeholder='Subfolder name']"
        );

        // Eskiden anlık isVisible() false ise (sayfa henüz render olmamışsa)
        // test edilen işlem sessizce atlanıyordu. İnput'u bekliyoruz.
        input.waitFor(new Locator.WaitForOptions().setTimeout(10000));

        input.fill(childFolder);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        ).click();
    }

    public boolean duplicateKlasorHatasiGorunuyorMu() {

        try {

            page.getByText(
                    "A folder with this name already exists.",
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean altKlasorMaxLength255Mi() {

        Locator input = page.locator(
                "input[placeholder='Subfolder name']"
        );

        String maxLength = input.getAttribute("maxlength");

        return "255".equals(maxLength);
    }

    public void renameFormunuAc() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Rename")
                        .setExact(true)
        ).click();

        page.locator(
                "input[maxlength='255']:not([placeholder='Subfolder name'])"
        ).waitFor();
    }

    public boolean renameMaxLength255Mi() {

        Locator input = page.locator(
                "input[maxlength='255']:not([placeholder='Subfolder name'])"
        );

        String maxLength = input.getAttribute("maxlength");

        return "255".equals(maxLength);
    }

    public boolean explorerGorunuyorMu() {

        try {

            page.getByRole(
                    AriaRole.HEADING,
                    new Page.GetByRoleOptions()
                            .setName("Explorer")
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public boolean breadcrumbKlasorleriGorunuyorMu(
            String parentName,
            String childName
    ) {
        try {
            Locator breadcrumb = page.locator("nav[aria-label='Breadcrumb']");

            breadcrumb.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            Locator parent = breadcrumb.locator(
                    "[title=\"" + parentName + "\"]"
            );

            Locator child = breadcrumb.locator(
                    "[title=\"" + childName + "\"]"
            );

            parent.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            child.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return parent.isVisible() && child.isVisible();

        } catch (Exception e) {
            return false;
        }
    }

    public boolean inactiveEtiketiGorunuyorMu() {

        try {

            Locator inactiveLabels = page.getByText(
                    "Inactive",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            inactiveLabels.first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return inactiveLabels.first().isVisible();

        } catch (Exception e) {

            return false;
        }
    }

    public boolean pasifKlasordeAltKlasorOlusturulamiyorMu(
            String childFolder
    ) {

        /*
         * Önce pasif durumun render edildiğini pozitif olarak doğrula
         * (FolderDetail.tsx inactive notice); anlık negatif isVisible()
         * sayfa henüz güncellenmeden true dönebilirdi.
         */
        try {
            page.getByText(
                    "This folder is inactive — regular users cannot see it or its documents.",
                    new Page.GetByTextOptions().setExact(true)
            ).waitFor(new Locator.WaitForOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            throw new AssertionError(
                    "Pasif klasör uyarısı görüntülenmedi; klasör pasif durumda değil. URL: "
                            + page.url(),
                    e
            );
        }

        Locator input = page.locator(
                "input[placeholder='Subfolder name']"
        );

        // Aynı render'da (canUploadHere = folderActive && ...) form yok.
        if (input.count() == 0 || !input.first().isVisible()) {
            return true;
        }

        // Form görünüyorsa gerçekten dene: sunucu isteği reddetmeli ve
        // klasör oluşmamalı. Sabit 1.5 sn yerine API yanıtını bekliyoruz.
        input.first().fill(childFolder);

        Response response = page.waitForResponse(
                r -> r.url().endsWith("/v1/folders") && "POST".equals(r.request().method()),
                new Page.WaitForResponseOptions().setTimeout(10000),
                () -> page.getByRole(
                        AriaRole.BUTTON,
                        new Page.GetByRoleOptions()
                                .setName("Create")
                                .setExact(true)
                ).click()
        );

        return !response.ok();
    }

    public boolean permissionInheritanceAcikMi() {

        try {

            Locator checkbox = page.getByRole(
                    AriaRole.CHECKBOX,
                    new Page.GetByRoleOptions()
                            .setName("Inherit access from parent folders")
                            .setExact(true)
            );

            checkbox.waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return checkbox.isChecked();

        } catch (Exception e) {

            return false;
        }
    }

    public void permissionInheritanceDegistir() {

        Locator checkbox = page.getByRole(
                AriaRole.CHECKBOX,
                new Page.GetByRoleOptions()
                        .setName("Inherit access from parent folders")
                        .setExact(true)
        );

        checkbox.waitFor(
                new Locator.WaitForOptions()
                        .setTimeout(5000)
        );

        // FolderDetail.tsx: input, PATCH + refresh() süresince disabled
        // (inheritBusy). setForce(true) disabled input'a tıklamayı sessizce
        // yutar; önceki değişiklik bitmeden gelen ikinci toggle kayboluyordu
        // (paralel koşuda tek PATCH görüldü). Tıklamadan önce etkin olmalı.
        com.microsoft.playwright.assertions.PlaywrightAssertions
                .assertThat(checkbox)
                .isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions()
                        .setTimeout(15000));

        boolean eskiDurum = checkbox.isChecked();
        boolean beklenenDurum = !eskiDurum;

        checkbox.click(
                new Locator.ClickOptions()
                        .setForce(true)
        );

        try {
            com.microsoft.playwright.assertions.PlaywrightAssertions
                    .assertThat(checkbox)
                    .isChecked(
                            new com.microsoft.playwright.assertions.LocatorAssertions.IsCheckedOptions()
                                    .setChecked(beklenenDurum)
                                    .setTimeout(15000)
                    );
            // checked durumu refresh() ile değişir; inheritBusy bundan SONRA
            // false olur. Sonraki adım işlemi bitmiş kabul edebilsin diye bekle.
            com.microsoft.playwright.assertions.PlaywrightAssertions
                    .assertThat(checkbox)
                    .isEnabled(new com.microsoft.playwright.assertions.LocatorAssertions.IsEnabledOptions()
                            .setTimeout(15000));
            return;
        } catch (AssertionError | com.microsoft.playwright.PlaywrightException ignored) {
            // Aşağıda kanıtlı hata mesajıyla düşüyoruz.
        }

        throw new AssertionError(
                "Permission inheritance değişmedi! "
                        + "Eski durum: "
                        + eskiDurum
                        + " | Beklenen durum: "
                        + beklenenDurum
                        + " | Son durum: "
                        + checkbox.isChecked()
        );
    }

    public void renameInputDoldur(String newName) {

        Locator renameInput = page.locator(
                "input[maxlength='255']:not([placeholder='Subfolder name'])"
        );

        renameInput.waitFor();
        renameInput.fill(newName);
    }

    public void renameKaydet() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Save")
                        .setExact(true)
        ).click();
    }

    public void renameIptalEt() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Cancel")
                        .setExact(true)
        ).click();
    }

    public boolean renameFormuKapaliMi() {

        try {

            Locator renameInput = page.locator(
                    "input[maxlength='255']:not([placeholder='Subfolder name'])"
            );

            // Anlık negatif kontrol yerine gizlenmesini bekle (en fazla 5 sn).
            renameInput.first().waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.HIDDEN)
                            .setTimeout(5000)
            );
            return true;

        } catch (PlaywrightException e) {

            return false;
        }
    }

    public String renameInputDegeri() {

        Locator renameInput = page.locator(
                "input[maxlength='255']:not([placeholder='Subfolder name'])"
        );

        renameInput.waitFor();

        return renameInput.inputValue();
    }

    public boolean klasorLinkiGorunuyorMu(String folderName) {

        try {

            page.getByRole(
                    AriaRole.LINK,
                    new Page.GetByRoleOptions()
                            .setName(folderName)
                            .setExact(true)
            ).waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    public void altKlasorInputunaYaz(String folderName) {

        page.locator(
                "input[placeholder='Subfolder name']"
        ).fill(folderName);
    }

    public void altKlasorCreateButonunaTikla() {

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        ).click();
    }

    public boolean altKlasorCreateButonuDisabledMi() {

        Locator createButton = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        );

        createButton.waitFor();

        return createButton.isDisabled();
    }

    public int altKlasorSayisi(String folderName) {

        return page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName(folderName)
                        .setExact(true)
        ).count();
    }
}