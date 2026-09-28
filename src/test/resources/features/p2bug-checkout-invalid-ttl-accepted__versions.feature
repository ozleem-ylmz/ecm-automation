@phase2 @phase2-versions
Feature: Phase 2 — Sürümler, checkout/checkin ve etiketler kapsamı

  # Kaynak: src/engine/documents/{locks,labels,save,rollback}.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "vfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @versions @lock @defect-candidate
  Scenario Outline: Geçersiz kilit süresi denetimli reddedilmelidir
    # Beklenti: <=0 ve aşırı büyük değerler 422 (bugün süresi dolmuş kilit ya da 500 olabilir).
    Given "yönetici" aktörü "vfolder" klasörüne "surebelge-<id>" sınıfsız belgesini yüklemiştir
    When "yönetici" şu isteği gönderir: POST "/v1/documents/{surebelge-<id>}/checkout" gövde '<govde>'
    Then yanıt durumu 4xx istemci hatası olmalıdır

    Examples:
      | id | govde |
      | a | {"expires_in_secs":0} |
      | b | {"expires_in_secs":-60} |
