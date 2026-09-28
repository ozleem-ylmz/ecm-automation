package steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * OWN-01: folder:write yetkisi olan normal kullanıcı alt klasör oluşturur ve
 * sahibi olur (write.rs:74-95); sahip yeniden adlandırabilir, sahip olmayan
 * yazma yetkilisi adlandıramaz (write.rs:365-380); kökte yalnız admin
 * klasör oluşturabilir (write.rs:88-92).
 */
public class FolderOwnershipSteps {

    @When("{string} {string} altında {string} alt klasörünü oluşturur")
    public void altKlasorunuOlusturur(String userAlias, String parentAlias, String folderAlias) {
        String name = TestDataFactory.unique("QA Sahip Klasörü");
        EcmApiClient.ApiResponse r = createFolder(userAlias, name, ApiTestContext.id(parentAlias));
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            registerFolder(folderAlias, name, r.json().get("id").getAsString());
        }
    }

    @Then("{string} {string} klasörünü yeniden adlandırabilmelidir")
    public void klasoruYenidenAdlandirabilmelidir(String userAlias, String folderAlias) {
        String newName = TestDataFactory.unique("QA Yeniden Adlandırıldı");
        String actual = rename(userAlias, folderAlias, newName)
                .expect(200, "'" + userAlias + "' sahibi olduğu klasörü yeniden adlandırma")
                .json().get("name").getAsString();
        if (!newName.equals(actual)) {
            throw new AssertionError("Klasör adı '" + newName + "' olmalıydı, '" + actual + "' döndü.");
        }
    }

    @Then("{string} sahibi olmadığı {string} klasörünü yeniden adlandıramamalıdır")
    public void sahibiOlmadigiKlasoruYenidenAdlandiramamalidir(String userAlias, String folderAlias) {
        EcmApiClient.ApiResponse r = rename(userAlias, folderAlias, TestDataFactory.unique("QA İzinsiz"));
        ApiTestContext.lastResponse(r);
        if (r.status() != 403) {
            throw new AssertionError("Sahip olmayan kullanıcı için 403 bekleniyordu. Gelen: HTTP "
                    + r.status() + " " + r.body());
        }
    }

    @When("{string} sistem kökü altında klasör oluşturmayı dener")
    public void sistemKokuAltindaKlasorOlusturmayiDener(String userAlias) {
        String name = TestDataFactory.unique("QA Kök Denemesi");
        // Strengthen (S-03): the attempted name is kept so the no-mutation check can read server state.
        ApiTestContext.putName("kök denemesi#attempted", name);
        EcmApiClient.ApiResponse r = createFolder(userAlias, name, BrowserManager.ROOT_FOLDER_ID);
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            // Kural ihlali (bug) durumunda bile oluşan klasörü temizle.
            registerFolder("kök denemesi", name, r.json().get("id").getAsString());
        }
    }

    /*
     * Strengthen S-03 (OWN-01): a rejected top-level create must leave no persisted folder.
     * Reads the system root's children from the server as admin (GET /v1/folders/{root}).
     */
    @Then("reddedilen kök klasör denemesi sunucuda hiçbir klasör oluşturmamış olmalıdır")
    public void reddedilenKokDenemesiKlasorOlusturmamali() {
        String name = ApiTestContext.name("kök denemesi#attempted");
        com.google.gson.JsonArray children = EcmApiClient.get(TestDataFactory.admin(), "/v1/folders/" + BrowserManager.ROOT_FOLDER_ID)
                .expect(200, "Sistem kökü okuma (yönetici)").json().getAsJsonArray("children");
        long matches = 0;
        for (com.google.gson.JsonElement e : children) {
            if (name.equals(e.getAsJsonObject().get("name").getAsString())) {
                matches++;
            }
        }
        io.qameta.allure.Allure.addAttachment("OWN-01 kanıtı — reddedilen kök klasör", "text/plain",
                "Denenen ad: " + name + "\nSistem kökü alt klasör sayısı: " + children.size() + "\nAynı adlı klasör: " + matches);
        if (matches != 0) {
            throw new AssertionError("403 ile reddedilen kök klasör isteği sunucuda '" + name + "' klasörünü oluşturmamalıydı (" + matches + " kayıt)");
        }
    }

    private static EcmApiClient.ApiResponse createFolder(String userAlias, String name, String parentId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("parent_id", parentId);
        return EcmApiClient.post(ApiTestContext.session(userAlias), "/v1/folders", body);
    }

    private static EcmApiClient.ApiResponse rename(String userAlias, String folderAlias, String newName) {
        return EcmApiClient.patch(ApiTestContext.session(userAlias),
                "/v1/folders/" + ApiTestContext.id(folderAlias), Map.of("name", newName));
    }

    private static void registerFolder(String alias, String name, String folderId) {
        ApiTestContext.putId(alias, folderId);
        ApiTestContext.putName(alias, name);
        ApiTestContext.onCleanup(() -> EcmApiClient.put(TestDataFactory.admin(),
                "/v1/folders/" + folderId + "/active", Map.of("is_active", false)));
    }
}
