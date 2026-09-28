@phase2 @phase2-folders
Feature: Phase 2 — Klasörler, ağaç/seçici ve klasör izinleri kapsamı

  # Kaynak: src/engine/folders/{read,write}.rs, api/rest/folders.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @folders @allowed-classes @defect-candidate
  Scenario: Bilinmeyen sınıf id ile izinli sınıf ayarı denetimli reddedilmelidir
    # Beklenti: FK hatası 404/422 (bugün RepositoryError::Internal -> 500 olabilir).
    Given "izinklasor2" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur
    When "yönetici" şu isteği gönderir: PUT "/v1/folders/{izinklasor2}/allowed-classes" gövde '{"class_ids":["{RANDOM_ID}"]}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
