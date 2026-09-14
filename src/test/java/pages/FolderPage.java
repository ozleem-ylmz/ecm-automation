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
}