@phase2 @phase2-documents
Feature: Phase 2 — Belgeler, sınıflandırma, özel alanlar ve durum yaşam döngüsü kapsamı

  # Kaynak: src/engine/documents/{save,admin,copy,reclassify,classify}.rs, custom_fields.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "dfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @documents @reversion @defect-candidate
  Scenario: Yeni sürümde sınıf değişimi klasör izinli sınıf politikasını uygulamalıdır
    # save_doc yeni sürümde assert_class_allowed_in_folder çağırmıyor olabilir (README: her yolda uygulanır).
    Given yönetici "ref:text:opsiyonel" özellikleriyle "izinliA" sınıfını oluşturmuştur
    And yönetici "ref:text:opsiyonel" özellikleriyle "izinliB" sınıfını oluşturmuştur
    And "yönetici" şu isteği göndermiş ve 200 almıştır: PUT "/v1/folders/{dfolder}/allowed-classes" gövde '{"class_ids":["{izinliA}"]}'
    And "yönetici" aktörü "dfolder" klasörüne "izinliA" sınıfından "rvbelge" belgesini "{}" metadatasıyla yüklemiştir
    When "yönetici" şu isteği gönderir: POST "/v1/documents" gövde '{"document_id":"{rvbelge}","title":"{name:rvbelge}","class_id":"{izinliB}","class_version":{classver:izinliB},"metadata":{},"mime_type":"text/plain","original_filename":"v2.txt","folder_id":"{dfolder}","content":{bytes:v2}}'
    Then yanıt durumu 422 olmalıdır
