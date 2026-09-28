@phase2 @phase2-folders
Feature: Phase 2 — Klasörler, ağaç/seçici ve klasör izinleri kapsamı

  # Kaynak: src/engine/folders/{read,write}.rs, api/rest/folders.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @folders @defect-candidate
  Scenario: Var olmayan klasörün belgeleri ve izinli sınıfları 404 döndürmelidir
    # Beklenti: kapalı #58 deseni; admin bugün 200 alıyor olabilir.
    When "yönetici" şu isteği gönderir: GET "/v1/folders/{RANDOM_ID}/documents"
    Then yanıt durumu 404 olmalıdır
