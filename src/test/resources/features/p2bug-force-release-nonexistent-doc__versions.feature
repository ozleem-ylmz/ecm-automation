@phase2 @phase2-versions
Feature: Phase 2 — Sürümler, checkout/checkin ve etiketler kapsamı

  # Kaynak: src/engine/documents/{locks,labels,save,rollback}.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "vfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @versions @force-release @defect-candidate
  Scenario: Var olmayan belgede zorla serbest bırakma 404 döndürmelidir
    # Beklenti: admin varlık kontrolü atlıyor olabilir -> 200 + audit satırı.
    When "yönetici" şu isteği gönderir: DELETE "/v1/documents/{RANDOM_ID}/lock"
    Then yanıt durumu 404 olmalıdır
