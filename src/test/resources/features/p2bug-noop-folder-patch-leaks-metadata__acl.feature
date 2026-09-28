@phase2 @phase2-acl
Feature: Phase 2 — ACL, etkin erişim ve miras kapsamı

  # Kaynak: src/engine/acl/{runtime,service,management}.rs, api/rest folder/document/class ACL.
  # #106, #107, #110 dondurulmuş defektlerdir; burada tekrar edilmez.

  Background:
    Given yönetici API oturumu açmıştır

  @acl-guard @defect-candidate
  Scenario: Değişiklik yapmayan klasör PATCH'i yetkisiz okuyucuya veri sızdırmamalıdır
    # Beklenti: yetkisiz kullanıcı 403/404 almalı; bugün no-op PATCH klasör özetini döndürüyor olabilir.
    Given "sizanuser" aktörü "user" hesap rolüyle hazırlanmıştır
    And "sfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur
    When "sizanuser" şu isteği gönderir: PATCH "/v1/folders/{sfolder}" gövde '{"inherit_permissions":false}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
