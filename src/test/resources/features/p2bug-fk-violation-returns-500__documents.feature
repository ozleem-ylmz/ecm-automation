@phase2 @phase2-documents
Feature: Phase 2 — Belgeler, sınıflandırma, özel alanlar ve durum yaşam döngüsü kapsamı

  # Kaynak: src/engine/documents/{save,admin,copy,reclassify,classify}.rs, custom_fields.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "dfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @documents @move @defect-candidate
  Scenario: Var olmayan klasöre taşıma 404 döndürmelidir
    # Beklenti: 404 (bugün FK -> 500 olabilir; #59 ailesi).
    Given "yönetici" aktörü "dfolder" klasörüne "mbelge2" sınıfsız belgesini yüklemiştir
    When "yönetici" şu isteği gönderir: PUT "/v1/documents/{mbelge2}/folder" gövde '{"folder_id":"{RANDOM_ID}"}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
