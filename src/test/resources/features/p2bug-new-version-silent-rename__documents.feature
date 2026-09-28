@phase2 @phase2-documents
Feature: Phase 2 — Belgeler, sınıflandırma, özel alanlar ve durum yaşam döngüsü kapsamı

  # Kaynak: src/engine/documents/{save,admin,copy,reclassify,classify}.rs, custom_fields.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "dfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @documents @reversion @defect-candidate
  Scenario: Yeni sürüm başlığı sessizce yeniden adlandırma yapmamalıdır
    # Beklenti: yeni sürümde farklı başlık yok sayılmalı ya da yeniden adlandırma kuralları uygulanmalı.
    Given "yönetici" aktörü "dfolder" klasörüne "rvbelge2" sınıfsız belgesini yüklemiştir
    And "yönetici" aktörü "dfolder" klasörüne "engelbelge" sınıfsız belgesini yüklemiştir
    When "yönetici" şu isteği gönderir: POST "/v1/documents/unclassified" gövde '{"document_id":"{rvbelge2}","title":"{name:engelbelge}","metadata":{},"mime_type":"text/plain","original_filename":"v2.txt","version_type":"Patch","content":{bytes:v2}}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
