package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

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

        return page.getByText(
                childFolder,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
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

    public boolean historyGorunuyorMu() {

        return page.getByText(
                "History",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
    }

    public boolean aclAlaniGorunuyorMu() {

        return page.getByText(
                "Folder access",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
    }

    public boolean allowedClassesAlaniGorunuyorMu() {

        return page.getByText(
                "Allowed document classes",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
    }

    public boolean breadcrumbGorunuyorMu() {

        return page.getByText(
                "Folders",
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
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

        if (input.isVisible()) {

            input.fill(childFolder);

            page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions()
                            .setName("Create")
                            .setExact(true)
            ).click();
        }
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

            page.getByText(
                    parentName,
                    new Page.GetByTextOptions()
                            .setExact(true)
            ).first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(5000)
            );

            page.getByText(
                    childName,
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

    public boolean inactiveEtiketiGorunuyorMu() {

        try {

            Locator inactiveLabels = page.getByText(
                    "Inactive",
                    new Page.GetByTextOptions()
                            .setExact(true)
            );

            if (inactiveLabels.count() == 0) {
                return false;
            }

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

        Locator input = page.locator(
                "input[placeholder='Subfolder name']"
        );

        if (!input.isVisible()) {
            return true;
        }

        input.fill(childFolder);

        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Create")
                        .setExact(true)
        ).click();

        page.waitForTimeout(1500);

        return !page.getByText(
                childFolder,
                new Page.GetByTextOptions()
                        .setExact(true)
        ).first().isVisible();
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

        boolean eskiDurum = checkbox.isChecked();
        boolean beklenenDurum = !eskiDurum;

        checkbox.click(
                new Locator.ClickOptions()
                        .setForce(true)
        );

        long bitisZamani =
                System.currentTimeMillis() + 15000;

        while (System.currentTimeMillis() < bitisZamani) {

            if (checkbox.isChecked() == beklenenDurum) {
                return;
            }

            page.waitForTimeout(250);
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

            return !renameInput.isVisible();

        } catch (Exception e) {

            return true;
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