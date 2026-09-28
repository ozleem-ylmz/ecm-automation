package steps;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.BrowserManager;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Batch 27 — sınıflar, yeniden sınıflandırma, özellikler, doğrulama kuralları,
 * sınıf ACL'i, izinli sınıflar, özellik şablonları ve özel alanlar.
 * ECM kaynakları: engine/classes/write.rs, engine/documents/reclassify.rs,
 * engine/classes/templates.rs, engine/custom_fields.rs, README "Classes" tablosu.
 *
 * Özellik belirtimi: "anahtar:tür:zorunlu|opsiyonel" virgülle ayrılır; boş = özelliksiz.
 */
public class ClassAdminSteps {

    private static final Gson GSON = new Gson();
    private static final int UI_WAIT_MS = 20000;

    private final Map<String, String> ruleIds = new HashMap<>();
    private final Map<String, String> classAclIds = new HashMap<>();

    // ---------------------------------------------------------------------
    // Sınıf oluşturma / yaşam döngüsü
    // ---------------------------------------------------------------------

    @Given("yönetici {string} özellikleriyle {string} sınıfını oluşturmuştur")
    public void yoneticiOzellikleriyleSinifOlusturmustur(String spec, String classAlias) {
        List<Map<String, Object>> props = new ArrayList<>();
        if (!spec.isBlank()) {
            for (String p : spec.split(",")) {
                String[] parts = p.trim().split(":");
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("key", parts[0]);
                m.put("display_name", parts[0].toUpperCase());
                m.put("data_type", parts[1]);
                m.put("is_multi_value", false);
                m.put("required", "zorunlu".equals(parts[2]));
                props.add(m);
            }
        }
        JsonObject created = EcmApiClient.post(TestDataFactory.admin(), "/v1/classes",
                        Map.of("name", TestDataFactory.unique("QA Sınıf"), "description", "QA Batch 27", "properties", props))
                .expect(200, "Sınıf oluşturma (" + classAlias + ")").json();
        String classId = created.get("id").getAsString();
        ApiTestContext.putId(classAlias, classId);
        ApiTestContext.putName(classAlias, created.get("name").getAsString());
        ApiTestContext.onCleanup(() -> {
            JsonObject c = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + classId).json();
            if (c.has("version")) {
                EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + classId + "/active",
                        Map.of("expected_version", c.get("version").getAsLong(), "is_active", false));
            }
        });
    }

    @When("yönetici {string} sınıfını pasifleştirir")
    public void yoneticiSinifiPasiflestirir(String classAlias) {
        setClassActive(classAlias, false);
    }

    @When("yönetici {string} sınıfını tekrar aktifleştirir")
    public void yoneticiSinifiAktiflestirir(String classAlias) {
        setClassActive(classAlias, true);
    }

    @When("yönetici {string} sınıfını silmeyi dener")
    public void yoneticiSinifiSilmeyiDener(String classAlias) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(),
                "/v1/classes/" + ApiTestContext.id(classAlias) + "?expected_version=" + classVersion(classAlias)));
    }

    @Then("{string} sınıfı hâlâ bulunmalıdır")
    public void sinifHalaBulunmali(String classAlias) {
        EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias)).expect(200, "Sınıf hâlâ var");
    }

    @Then("yönetici arayüzde {string} sınıfının detayında Delete butonunu görmemelidir")
    public void sinifDetayindaDeleteGorunmemeli(String classAlias) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/classes/" + ApiTestContext.id(classAlias));
        page.getByText(ApiTestContext.name(classAlias)).first().waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        Locator del = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete").setExact(true));
        if (del.count() > 0 && del.first().isVisible() && del.first().isEnabled()) {
            throw new AssertionError("Silinemeyecek sınıf için arayüz etkin bir Delete butonu sunmamalıydı.");
        }
    }

    // ---------------------------------------------------------------------
    // Sınıflı yükleme / sınıflandırma / yeniden sınıflandırma
    // ---------------------------------------------------------------------

    @Given("{string} {string} sınıfıyla {string} klasörüne {string} metadatasıyla {string} belgesini yüklemiştir")
    public void sinifliBelgeYuklemistir(String userAlias, String classAlias, String folderAlias, String metaJson, String documentAlias) {
        EcmApiClient.ApiResponse r = uploadClassified(userAlias, classAlias, folderAlias, metaJson, classVersion(classAlias));
        r.expect(201, "Sınıflı yükleme (" + documentAlias + ")");
        TestDataFactory.registerDocument(documentAlias, r.json().get("document_id").getAsString(), lastTitle);
    }

    @When("{string} {string} sınıfıyla {string} klasörüne {string} metadatasıyla belge yüklemeyi dener")
    public void sinifliBelgeYuklemeyiDener(String userAlias, String classAlias, String folderAlias, String metaJson) {
        tryUpload(userAlias, classAlias, folderAlias, metaJson, classVersion(classAlias));
    }

    @When("yönetici {string} sınıfıyla {string} klasörüne eski sınıf sürümü ve {string} metadatasıyla belge yüklemeyi dener")
    public void eskiSurumleYuklemeyiDener(String classAlias, String folderAlias, String metaJson) {
        tryUpload("yönetici", classAlias, folderAlias, metaJson, Long.parseLong(ApiTestContext.name(classAlias + "#oldVersion")));
    }

    @When("{string} {string} belgesini {string} sınıfına {string} metadatasıyla sınıflandırmayı dener")
    public void siniflandirmayiDener(String userAlias, String documentAlias, String classAlias, String metaJson) {
        ApiTestContext.lastResponse(classify(userAlias, documentAlias, classAlias, metaJson, classVersion(classAlias), "class"));
    }

    @When("yönetici {string} belgesini {string} sınıfına eski sınıf sürümü ve {string} metadatasıyla sınıflandırmayı dener")
    public void eskiSurumleSiniflandirmayiDener(String documentAlias, String classAlias, String metaJson) {
        ApiTestContext.lastResponse(classify("yönetici", documentAlias, classAlias, metaJson,
                Long.parseLong(ApiTestContext.name(classAlias + "#oldVersion")), "class"));
    }

    @When("yönetici {string} belgesini {string} sınıfına {string} metadatasıyla yeniden sınıflandırmayı dener")
    public void yenidenSiniflandirmayiDener(String documentAlias, String classAlias, String metaJson) {
        ApiTestContext.lastResponse(classify("yönetici", documentAlias, classAlias, metaJson, classVersion(classAlias), "reclassify"));
    }

    @Then("{string} belgesi {string} sınıfında olmalı ve metadatası {string} olmalıdır")
    public void belgeSinifindaVeMetadata(String documentAlias, String classAlias, String metaJson) {
        JsonObject d = document(documentAlias);
        String classId = d.get("class_id").isJsonNull() ? null : d.get("class_id").getAsString();
        JsonObject expected = GSON.fromJson(metaJson, JsonObject.class);
        if (!ApiTestContext.id(classAlias).equals(classId) || !expected.equals(d.getAsJsonObject("metadata"))) {
            throw new AssertionError("'" + documentAlias + "' " + classAlias + " sınıfında ve metadata " + expected
                    + " olmalıydı. Gelen: class_id=" + classId + " metadata=" + d.get("metadata"));
        }
    }

    @Then("{string} belgesi sınıfsız olmalıdır")
    public void belgeSinifsizOlmali(String documentAlias) {
        JsonElement c = document(documentAlias).get("class_id");
        if (c != null && !c.isJsonNull()) {
            throw new AssertionError("'" + documentAlias + "' sınıfsız kalmalıydı, class_id=" + c);
        }
    }

    @Then("son API yanıtının ihlal listesi {string} özelliği için {string} kuralını içermelidir")
    public void ihlalListesiIcermeli(String key, String constraintType) {
        EcmApiClient.ApiResponse r = ApiTestContext.lastResponse();
        boolean found = false;
        try {
            for (JsonElement v : r.json().getAsJsonObject("error").getAsJsonObject("details").getAsJsonArray("violations")) {
                JsonObject o = v.getAsJsonObject();
                found |= key.equals(o.get("property_key").getAsString()) && constraintType.equals(o.get("constraint_type").getAsString());
            }
        } catch (RuntimeException e) {
            throw new AssertionError("Yanıtta ihlal listesi yok: " + r.body());
        }
        if (r.status() != 422 || !found) {
            throw new AssertionError("422 ve '" + key + "' için '" + constraintType + "' ihlali bekleniyordu: HTTP " + r.status() + " " + r.body());
        }
    }

    // ---------------------------------------------------------------------
    // Özellikler, kullanımdan kaldırma, kurallar
    // ---------------------------------------------------------------------

    @When("yönetici {string} sınıfına {string} özelliğini {string} türünde zorunlu olarak eklemeyi dener")
    public void sinifaZorunluOzellikEklemeyiDener(String classAlias, String key, String dataType) {
        ApiTestContext.putName(classAlias + "#oldVersion", String.valueOf(classVersion(classAlias)));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", key);
        body.put("display_name", key.toUpperCase());
        body.put("data_type", dataType);
        body.put("is_multi_value", false);
        body.put("required", true);
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties", body));
    }

    @Then("{string} sınıfının sürümü artmış olmalıdır")
    public void sinifSurumuArtmisOlmali(String classAlias) {
        long old = Long.parseLong(ApiTestContext.name(classAlias + "#oldVersion"));
        if (classVersion(classAlias) <= old) {
            throw new AssertionError("Sınıf sürümü artmalıydı (" + old + " -> " + classVersion(classAlias) + ").");
        }
    }

    @Then("{string} sınıfındaki {string} özelliği isteğe bağlı olarak kaydedilmiş olmalıdır")
    public void ozellikIstegeBagliKaydedilmis(String classAlias, String key) {
        JsonObject p = property(classAlias, key);
        if (p.get("required").getAsBoolean()) {
            throw new AssertionError("README: sonradan eklenen özellik her zaman isteğe bağlıdır; '" + key + "' zorunlu kaydedilmiş.");
        }
    }

    @When("yönetici {string} sınıfının {string} özelliğini kullanımdan kaldırır")
    public void ozellikKullanimdanKaldirir(String classAlias, String key) {
        deprecate(classAlias, key, true);
    }

    @When("yönetici {string} sınıfının {string} özelliğini tekrar kullanıma alır")
    public void ozellikTekrarKullanimaAlir(String classAlias, String key) {
        deprecate(classAlias, key, false);
    }

    @Given("yönetici {string} sınıfının {string} özelliğine {string} kuralını {string} değeriyle eklemiştir")
    public void ozelligeKuralEklemistir(String classAlias, String key, String type, String valueJson) {
        Object value = GSON.fromJson(valueJson, Object.class);
        String id = EcmApiClient.post(TestDataFactory.admin(),
                        "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/" + propertyId(classAlias, key) + "/rules",
                        Map.of("constraint_type", type, "constraint_value", value, "error_message", key + " " + type))
                .expect(200, "Kural ekleme (" + type + ")").json().get("id").getAsString();
        ruleIds.put(classAlias + "|" + key + "|" + type, id);
    }

    @When("yönetici {string} sınıfının {string} özelliğindeki {string} kuralını siler")
    public void kuraliSiler(String classAlias, String key, String type) {
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/"
                + propertyId(classAlias, key) + "/rules/" + ruleIds.get(classAlias + "|" + key + "|" + type)).expect(200, "Kural silme");
    }

    @When("yönetici {string} sınıfının {string} özelliğinin görünen adını {string} yapmayı dener")
    public void gorunenAdiYapmayiDener(String classAlias, String key, String displayName) {
        ApiTestContext.lastResponse(EcmApiClient.patch(TestDataFactory.admin(),
                "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/" + propertyId(classAlias, key),
                Map.of("display_name", displayName)));
    }

    @When("yönetici {string} sınıfında var olmayan bir özelliğin görünen adını değiştirmeyi dener")
    public void varOlmayanOzellikGorunenAd(String classAlias) {
        ApiTestContext.lastResponse(EcmApiClient.patch(TestDataFactory.admin(),
                "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/" + java.util.UUID.randomUUID(),
                Map.of("display_name", "QA")));
    }

    @Then("{string} sınıfının {string} özelliğinin görünen adı {string} olmalıdır")
    public void gorunenAdOlmali(String classAlias, String key, String expected) {
        String actual = property(classAlias, key).get("display_name").getAsString();
        if (!expected.equals(actual)) {
            throw new AssertionError("Görünen ad '" + expected + "' olmalıydı, '" + actual + "' bulundu.");
        }
    }

    // ---------------------------------------------------------------------
    // Sınıf ACL'i, izinli sınıflar, sınıf listeleri
    // ---------------------------------------------------------------------

    @Given("{string} sınıfında {string} kullanıcısına {string} rolü verilmiştir")
    public void sinifdaKullaniciyaRol(String classAlias, String userAlias, String role) {
        classAclIds.put(classAlias + "|" + userAlias, grantClass(classAlias, "user_id", ApiTestContext.id(userAlias), role, false));
    }

    @Given("{string} sınıfında {string} grubuna {string} rolü verilmiştir")
    public void sinifdaGrubaRol(String classAlias, String groupAlias, String role) {
        String groupId = groupAlias.equals("All Users") ? TestDataFactory.ALL_USERS_GROUP : ApiTestContext.id(groupAlias);
        grantClass(classAlias, "group_id", groupId, role, false);
    }

    @Given("{string} sınıfında {string} kullanıcısı için {string} rolü reddedilmiştir")
    public void sinifdaKullaniciReddedilmis(String classAlias, String userAlias, String role) {
        grantClass(classAlias, "user_id", ApiTestContext.id(userAlias), role, true);
    }

    @When("yönetici {string} sınıfındaki {string} kullanıcısının izin satırını kaldırır")
    public void sinifIzinSatiriniKaldirir(String classAlias, String userAlias) {
        EcmApiClient.delete(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/acl/"
                + classAclIds.get(classAlias + "|" + userAlias)).expect(200, "Sınıf ACL kaldırma");
    }

    @Given("yönetici {string} klasörünün izinli sınıflarını yalnız {string} olarak ayarlamıştır")
    public void izinliSiniflariAyarlamistir(String folderAlias, String classAlias) {
        EcmApiClient.put(TestDataFactory.admin(), "/v1/folders/" + ApiTestContext.id(folderAlias) + "/allowed-classes",
                Map.of("class_ids", List.of(ApiTestContext.id(classAlias)))).expect(200, "İzinli sınıflar");
    }

    @Then("{string} sınıf listesinde {string} sınıfını görmelidir")
    public void sinifListesindeGormeli(String userAlias, String classAlias) {
        assertInClassList(userAlias, classAlias, "/v1/classes", true);
    }

    @Then("{string} sınıf listesinde {string} sınıfını görmemelidir")
    public void sinifListesindeGormemeli(String userAlias, String classAlias) {
        assertInClassList(userAlias, classAlias, "/v1/classes", false);
    }

    @Then("{string} yükleme sınıf listesinde {string} sınıfını görmelidir")
    public void yuklemeSinifListesindeGormeli(String userAlias, String classAlias) {
        assertInClassList(userAlias, classAlias, "/v1/classes?for=upload", true);
    }

    @Then("{string} yükleme sınıf listesinde {string} sınıfını görmemelidir")
    public void yuklemeSinifListesindeGormemeli(String userAlias, String classAlias) {
        assertInClassList(userAlias, classAlias, "/v1/classes?for=upload", false);
    }

    // ---------------------------------------------------------------------
    // Özellik şablonları (PT-01, PT-02)
    // ---------------------------------------------------------------------

    @When("yönetici {string} adlı {string} türünde özellik şablonu oluşturur")
    public void yoneticiSablonOlusturur(String templateAlias, String type) {
        String name = TestDataFactory.unique("QA Şablon");
        JsonObject t = EcmApiClient.post(TestDataFactory.admin(), "/v1/property-templates",
                Map.of("name", name, "type", type, "description", "QA PT")).expect(200, "Şablon oluşturma").json();
        ApiTestContext.putId(templateAlias, t.get("id").getAsString());
        ApiTestContext.putName(templateAlias, name);
        String id = t.get("id").getAsString();
        ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/property-templates/" + id));
        if (!type.equals(t.get("data_type").getAsString()) || t.get("is_system").getAsBoolean()) {
            throw new AssertionError("Şablon '" + type + "' türünde ve sistem dışı olmalıydı: " + t);
        }
    }

    @When("yönetici {string} şablonuyla aynı adda şablon oluşturmayı dener")
    public void ayniAddaSablonOlusturmayiDener(String templateAlias) {
        tryTemplate(ApiTestContext.name(templateAlias), "text");
    }

    @When("yönetici {string} adı ve {string} türüyle şablon oluşturmayı dener")
    public void adVeTurleSablonOlusturmayiDener(String name, String type) {
        tryTemplate(name.equals("<benzersiz>") ? TestDataFactory.unique("QA Şablon") : name, type);
    }

    @When("yönetici {string} şablonunu yeniden adlandırır")
    public void sablonuYenidenAdlandirir(String templateAlias) {
        String newName = ApiTestContext.name(templateAlias) + " yeni";
        EcmApiClient.patch(TestDataFactory.admin(), "/v1/property-templates/" + ApiTestContext.id(templateAlias),
                Map.of("name", newName)).expect(200, "Şablon yeniden adlandırma");
        ApiTestContext.putName(templateAlias, newName);
    }

    @Then("{string} şablonu güncel adıyla listelenmelidir")
    public void sablonGuncelAdiylaListelenmeli(String templateAlias) {
        String id = ApiTestContext.id(templateAlias);
        String name = null;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates").expect(200, "Şablonlar").json().getAsJsonArray("items")) {
            if (id.equals(e.getAsJsonObject().get("id").getAsString())) {
                name = e.getAsJsonObject().get("name").getAsString();
            }
        }
        if (!ApiTestContext.name(templateAlias).equals(name)) {
            throw new AssertionError("Şablon listede güncel adıyla görünmeli: '" + ApiTestContext.name(templateAlias) + "', gelen: " + name);
        }
    }

    @When("yönetici {string} sistem şablonunu {string} işlemiyle değiştirmeyi dener")
    public void sistemSablonunuDegistirmeyiDener(String systemName, String action) {
        String id = systemTemplate(systemName).get("id").getAsString();
        EcmApiClient.ApiResponse r = "silme".equals(action)
                ? EcmApiClient.delete(TestDataFactory.admin(), "/v1/property-templates/" + id)
                : EcmApiClient.patch(TestDataFactory.admin(), "/v1/property-templates/" + id, Map.of("name", "QA hack " + System.nanoTime()));
        ApiTestContext.lastResponse(r);
    }

    @Then("{string} sistem şablonu değişmeden kalmalıdır")
    public void sistemSablonuDegismedenKalmali(String systemName) {
        JsonObject t = systemTemplate(systemName);
        if (!systemName.equals(t.get("name").getAsString()) || !t.get("is_system").getAsBoolean()) {
            throw new AssertionError("Sistem şablonu değişmemeliydi: " + t);
        }
    }

    // ---------------------------------------------------------------------
    // Özel alanlar (CF-01, CF-02)
    // ---------------------------------------------------------------------

    @Given("yönetici {string} sınıfına {string} adlı {string} türünde {string} özel alan eklemiştir")
    public void ozelAlanEklemistir(String classAlias, String fieldName, String type, String requiredWord) {
        JsonObject f = EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/custom-fields",
                        Map.of("field_name", fieldName, "data_type", type, "is_required", "zorunlu".equals(requiredWord)))
                .expect(200, "Özel alan ekleme").json();
        ApiTestContext.putId(classAlias + "#cf#" + fieldName, f.get("id").getAsString());
    }

    @When("yönetici {string} sınıfına aynı {string} adıyla özel alan eklemeyi dener")
    public void ayniAdlaOzelAlanEklemeyiDener(String classAlias, String fieldName) {
        ApiTestContext.lastResponse(EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/custom-fields",
                Map.of("field_name", fieldName, "data_type", "text", "is_required", false)));
    }

    @When("yönetici {string} belgesinin özel alanlarını {string} olarak kaydetmeyi dener")
    public void ozelAlanlariKaydetmeyiDener(String documentAlias, String valuesJson) {
        ApiTestContext.lastResponse(EcmApiClient.put(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/custom-fields",
                Map.of("values", GSON.fromJson(valuesJson, Map.class))));
    }

    @Then("{string} belgesinin {string} özel alan değeri {string} olmalıdır")
    public void ozelAlanDegeriOlmali(String documentAlias, String fieldName, String expected) {
        String actual = null;
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/custom-fields")
                .expect(200, "Özel alanlar").json().getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            if (fieldName.equals(o.get("field_name").getAsString())) {
                actual = o.get("value").isJsonNull() ? null : o.get("value").getAsString();
            }
        }
        if (!expected.equals(actual)) {
            throw new AssertionError("'" + fieldName + "' özel alan değeri '" + expected + "' olmalıydı, '" + actual + "' bulundu.");
        }
    }

    @When("yönetici {string} sınıfındaki {string} özel alanını silmeyi dener")
    public void ozelAlaniSilmeyiDener(String classAlias, String fieldName) {
        ApiTestContext.lastResponse(EcmApiClient.delete(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias)
                + "/custom-fields/" + ApiTestContext.id(classAlias + "#cf#" + fieldName)));
    }

    @When("yönetici {string} sınıfındaki {string} özel alanının adını {string} yapmayı dener")
    public void ozelAlanAdiniYapmayiDener(String classAlias, String fieldName, String newName) {
        ApiTestContext.lastResponse(EcmApiClient.patch(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias)
                + "/custom-fields/" + ApiTestContext.id(classAlias + "#cf#" + fieldName), Map.of("field_name", newName)));
    }

    @Then("{string} sınıfının özel alanları tam olarak {string} olmalıdır")
    public void ozelAlanlariTamOlarak(String classAlias, String namesCsv) {
        List<String> names = new ArrayList<>();
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/custom-fields")
                .expect(200, "Sınıf özel alanları").json().getAsJsonArray("items")) {
            names.add(e.getAsJsonObject().get("field_name").getAsString());
        }
        List<String> expected = namesCsv.isBlank() ? List.of() : List.of(namesCsv.split(","));
        if (!new java.util.TreeSet<>(names).equals(new java.util.TreeSet<>(expected))) {
            throw new AssertionError("Özel alanlar " + expected + " olmalıydı, " + names + " bulundu.");
        }
    }

    @When("yönetici arayüzde {string} belgesinin özel alanlar kartında {string} alanına {string} girip kaydeder")
    public void arayuzdeOzelAlanKaydeder(String documentAlias, String fieldName, String value) {
        BrowserManager.startBrowser();
        BrowserManager.apiLoginMainContext(TestDataFactory.ADMIN_EMAIL, TestDataFactory.DEFAULT_PASSWORD);
        Page page = BrowserManager.page;
        page.navigate(BrowserManager.getBaseUrl() + "/documents/" + ApiTestContext.id(documentAlias));
        // DocumentCustomFieldsCard.tsx: kart = h2'nin iki üst div'i (başlık satırı + içerik).
        Locator card = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Custom fields").setExact(true))
                .locator("xpath=../..");
        try {
            card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit").setExact(true))
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Custom fields kartında Edit butonu görünmedi.", e);
        }
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Edit").setExact(true)).click();
        Locator input = card.locator("label:has-text('" + fieldName + "') + input");
        input.waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        if (!"number".equals(input.getAttribute("type"))) {
            throw new AssertionError("Sayısal özel alan girişi type=number olmalıydı (istemci tarafı tip koruması).");
        }
        input.fill(value);
        Response r = page.waitForResponse(resp -> "PUT".equals(resp.request().method()) && resp.url().endsWith("/custom-fields"),
                new Page.WaitForResponseOptions().setTimeout(UI_WAIT_MS),
                () -> card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Save").setExact(true)).click());
        if (r.status() != 200) {
            throw new AssertionError("Arayüzden özel alan kaydı HTTP " + r.status() + ": " + r.text());
        }
    }

    @Then("yönetici sayfayı yenilediğinde özel alanlar kartında {string} değerini görmelidir")
    public void yenilemedeOzelAlanDegeri(String value) {
        Page page = BrowserManager.page;
        page.reload();
        // DocumentCustomFieldsCard.tsx: kart = h2'nin iki üst div'i (başlık satırı + içerik).
        Locator card = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Custom fields").setExact(true))
                .locator("xpath=../..");
        try {
            card.getByText(value, new Locator.GetByTextOptions().setExact(true)).first()
                    .waitFor(new Locator.WaitForOptions().setTimeout(UI_WAIT_MS));
        } catch (PlaywrightException e) {
            throw new AssertionError("Yenilemeden sonra özel alan değeri '" + value + "' kartta görünmedi.", e);
        }
    }

    // ---------------------------------------------------------------------

    private String lastTitle;

    private EcmApiClient.ApiResponse uploadClassified(String userAlias, String classAlias, String folderAlias, String metaJson, long version) {
        lastTitle = TestDataFactory.unique("QA Sınıflı");
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(lastTitle, ApiTestContext.id(classAlias), version,
                GSON.fromJson(metaJson, Map.class), folderAlias);
        return EcmApiClient.post(session(userAlias), "/v1/documents", body);
    }

    private void tryUpload(String userAlias, String classAlias, String folderAlias, String metaJson, long version) {
        EcmApiClient.ApiResponse r = uploadClassified(userAlias, classAlias, folderAlias, metaJson, version);
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            TestDataFactory.registerDocument("yükleme " + lastTitle, r.json().get("document_id").getAsString(), lastTitle);
        }
    }

    private static EcmApiClient.ApiResponse classify(String userAlias, String documentAlias, String classAlias, String metaJson,
                                                     long version, String endpoint) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("class_id", ApiTestContext.id(classAlias));
        body.put("class_version", version);
        body.put("metadata", GSON.fromJson(metaJson, Map.class));
        return EcmApiClient.patch(session(userAlias), "/v1/documents/" + ApiTestContext.id(documentAlias) + "/" + endpoint, body);
    }

    private static void setClassActive(String classAlias, boolean active) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/active",
                Map.of("expected_version", classVersion(classAlias), "is_active", active)).expect(200, "Sınıf aktiflik");
    }

    private static void deprecate(String classAlias, String key, boolean value) {
        EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/properties/"
                + propertyId(classAlias, key) + "/deprecation", Map.of("is_deprecated", value)).expect(200, "Kullanımdan kaldırma");
    }

    private static String grantClass(String classAlias, String principalKey, String principalId, String role, boolean deny) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role_id", TestDataFactory.roleId(role));
        body.put(principalKey, principalId);
        body.put("deny", deny);
        return EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias) + "/acl", body)
                .expect(200, "Sınıf ACL").json().get("id").getAsString();
    }

    private static void assertInClassList(String userAlias, String classAlias, String path, boolean expected) {
        boolean present = false;
        for (JsonElement e : EcmApiClient.get(session(userAlias), path).expect(200, "Sınıf listesi").json().getAsJsonArray("items")) {
            present |= ApiTestContext.id(classAlias).equals(e.getAsJsonObject().get("id").getAsString());
        }
        if (present != expected) {
            throw new AssertionError("'" + userAlias + "' " + path + " listesinde '" + classAlias + "' " + (expected ? "görmeliydi" : "görmemeliydi") + ".");
        }
    }

    private static void tryTemplate(String name, String type) {
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/property-templates", Map.of("name", name, "type", type));
        ApiTestContext.lastResponse(r);
        if (r.status() == 200) {
            String id = r.json().get("id").getAsString();
            ApiTestContext.onCleanup(() -> EcmApiClient.delete(TestDataFactory.admin(), "/v1/property-templates/" + id));
        }
    }

    private static JsonObject systemTemplate(String name) {
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/property-templates").expect(200, "Şablonlar").json().getAsJsonArray("items")) {
            JsonObject t = e.getAsJsonObject();
            if (t.get("is_system").getAsBoolean() && name.equals(t.get("name").getAsString())) {
                return t;
            }
        }
        throw new AssertionError("Sistem şablonu bulunamadı: " + name);
    }

    static long classVersion(String classAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias))
                .expect(200, "Sınıf okuma").json().get("version").getAsLong();
    }

    private static JsonObject property(String classAlias, String key) {
        for (JsonElement e : EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + ApiTestContext.id(classAlias))
                .expect(200, "Sınıf").json().getAsJsonArray("properties")) {
            if (key.equals(e.getAsJsonObject().get("key").getAsString())) {
                return e.getAsJsonObject();
            }
        }
        throw new AssertionError("Özellik bulunamadı: " + key);
    }

    private static String propertyId(String classAlias, String key) {
        return property(classAlias, key).get("id").getAsString();
    }

    private static JsonObject document(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge").json().getAsJsonObject("document");
    }

    private static EcmApiClient.Session session(String alias) {
        return alias.equals("yönetici") ? TestDataFactory.admin() : ApiTestContext.session(alias);
    }
}
