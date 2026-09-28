@phase2 @phase2-documents
Feature: Phase 2 — Belgeler, sınıflandırma, özel alanlar ve durum yaşam döngüsü kapsamı

  # Kaynak: src/engine/documents/{save,admin,copy,reclassify,classify}.rs, custom_fields.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "dfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @documents @status @defect-candidate
  Scenario: Pasif klasördeki belgenin durumu değiştirilememelidir
    # Beklenti: pasif klasörde durum değişimi folder_inactive 403 (diğer mutasyonlarla tutarlı).
    Given "yönetici" aktörü "dfolder" klasörüne "pfbelge" sınıfsız belgesini yüklemiştir
    And "dfolder" klasörü yönetici tarafından pasifleştirilmiştir
    When "yönetici" şu isteği gönderir: PATCH "/v1/documents/{pfbelge}/status" gövde '{"status":"under_review"}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
