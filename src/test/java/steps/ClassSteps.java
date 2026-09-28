package steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ApiTestContext;
import utils.EcmApiClient;
import utils.TestDataFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * CLS-01: sınıf + zorunlu özellik + sınıflı yükleme.
 * Takma adlar: "<sınıf>#text" ve "<sınıf>#numeric" özellik anahtarları,
 * "<sınıf>#<anahtar>" özellik tanım id'leri, "<sınıf>#version" optimistic
 * lock sürümü (DocManagementClass.version, src/domain/class.rs:17-21).
 */
public class ClassSteps {

    @Given("yönetici zorunlu {string} metin ve {string} sayısal özellikleri olan {string} sınıfını oluşturmuştur")
    public void yoneticiZorunluOzellikliSinifOlusturmustur(String textKey, String numericKey, String classAlias) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", TestDataFactory.unique("QA Sınıf"));
        body.put("description", "QA otomasyon - CLS-01");
        body.put("properties", List.of(
                property(textKey, "Sözleşme No", "text"),
                property(numericKey, "Tutar", "numeric")
        ));

        JsonObject created = EcmApiClient.post(TestDataFactory.admin(), "/v1/classes", body)
                .expect(200, "Sınıf oluşturma").json();
        String classId = created.get("id").getAsString();

        ApiTestContext.putId(classAlias, classId);
        ApiTestContext.putName(classAlias + "#version", created.get("version").getAsString());
        ApiTestContext.putName(classAlias + "#text", textKey);
        ApiTestContext.putName(classAlias + "#numeric", numericKey);
        for (JsonElement p : created.getAsJsonArray("properties")) {
            JsonObject prop = p.getAsJsonObject();
            ApiTestContext.putId(classAlias + "#" + prop.get("key").getAsString(), prop.get("id").getAsString());
        }

        ApiTestContext.onCleanup(() -> {
            long version = EcmApiClient.get(TestDataFactory.admin(), "/v1/classes/" + classId)
                    .expect(200, "Sınıf okuma (temizlik)").json().get("version").getAsLong();
            EcmApiClient.post(TestDataFactory.admin(), "/v1/classes/" + classId + "/active",
                    Map.of("expected_version", version, "is_active", false));
        });
    }

    @When("yönetici {string} sınıfıyla {string} klasörüne özellik vermeden belge yüklemeyi dener")
    public void ozellikVermedenBelgeYuklemeyiDener(String classAlias, String folderAlias) {
        upload(classAlias, folderAlias, TestDataFactory.unique("QA Eksik Özellik"), Map.of(), null);
    }

    @When("yönetici {string} sınıfıyla {string} klasörüne {string} özelliği sayı olmayan belge yüklemeyi dener")
    public void sayiOlmayanOzellikleBelgeYuklemeyiDener(String classAlias, String folderAlias, String numericKey) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(ApiTestContext.name(classAlias + "#text"), "SZ-ERR");
        props.put(numericKey, "yüz");
        upload(classAlias, folderAlias, TestDataFactory.unique("QA Hatalı Tip"), props, null);
    }

    @When("yönetici {string} sınıfıyla {string} klasörüne geçerli özelliklerle {string} belgesini yükler")
    public void gecerliOzelliklerleBelgeYukler(String classAlias, String folderAlias, String documentAlias) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(ApiTestContext.name(classAlias + "#text"), "SZ-001");
        props.put(ApiTestContext.name(classAlias + "#numeric"), 1500);
        upload(classAlias, folderAlias, TestDataFactory.unique("QA Sözleşme"), props, documentAlias);
    }

    @Then("{string} belgesi {string} sınıfına bağlı olmalıdır")
    public void belgesiSinifinaBagliOlmalidir(String documentAlias, String classAlias) {
        JsonObject document = readDocument(documentAlias).getAsJsonObject("document");
        String actual = document.has("class_id") && !document.get("class_id").isJsonNull()
                ? document.get("class_id").getAsString() : null;
        if (!ApiTestContext.id(classAlias).equals(actual)) {
            throw new AssertionError("'" + documentAlias + "' class_id '" + ApiTestContext.id(classAlias)
                    + "' olmalıydı, '" + actual + "' bulundu.");
        }
    }

    @Then("{string} belgesinin {string} özelliği {string} olmalıdır")
    public void belgeninOzelligiOlmalidir(String documentAlias, String propertyKey, String expected) {
        String classAlias = ApiTestContext.name(documentAlias + "#class");
        String propertyDefId = ApiTestContext.id(classAlias + "#" + propertyKey);
        for (JsonElement e : readDocument(documentAlias).getAsJsonArray("property_values")) {
            JsonObject v = e.getAsJsonObject();
            if (propertyDefId.equals(v.get("property_def_id").getAsString())) {
                String actual = v.get("value").isJsonNull() ? null : v.get("value").getAsString();
                if (!expected.equals(actual)) {
                    throw new AssertionError("'" + propertyKey + "' değeri '" + expected + "' olmalıydı, '"
                            + actual + "' bulundu.");
                }
                return;
            }
        }
        throw new AssertionError("'" + documentAlias + "' belgesinde '" + propertyKey + "' özellik değeri yok.");
    }

    private static Map<String, Object> property(String key, String displayName, String dataType) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("key", key);
        p.put("display_name", displayName);
        p.put("data_type", dataType);
        p.put("is_multi_value", false);
        p.put("required", true);
        return p;
    }

    private static void upload(String classAlias, String folderAlias, String title,
                               Map<String, Object> properties, String documentAlias) {
        Map<String, Object> body = TestDataFactory.classifiedDocumentBody(
                title,
                ApiTestContext.id(classAlias),
                Long.parseLong(ApiTestContext.name(classAlias + "#version")),
                properties,
                folderAlias
        );
        EcmApiClient.ApiResponse r = EcmApiClient.post(TestDataFactory.admin(), "/v1/documents", body);
        ApiTestContext.lastResponse(r);
        if (r.status() == 201) {
            String documentId = r.json().get("document_id").getAsString();
            String alias = documentAlias != null ? documentAlias : "beklenmeyen belge " + title;
            TestDataFactory.registerDocument(alias, documentId, title);
            ApiTestContext.putName(alias + "#class", classAlias);
        }
    }

    private static JsonObject readDocument(String documentAlias) {
        return EcmApiClient.get(TestDataFactory.admin(), "/v1/documents/" + ApiTestContext.id(documentAlias))
                .expect(200, "Belge okuma").json();
    }
}
