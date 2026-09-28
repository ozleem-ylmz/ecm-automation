@phase2 @phase2-acl
Feature: Phase 2 — ACL, etkin erişim ve miras kapsamı

  # Kaynak: src/engine/acl/{runtime,service,management}.rs, api/rest folder/document/class ACL.
  # #106, #107, #110 dondurulmuş defektlerdir; burada tekrar edilmez.

  Background:
    Given yönetici API oturumu açmıştır

  @acl-guard @defect-candidate
  Scenario: Var olmayan belgenin ACL listesi 404 döndürmelidir
    # Beklenti: kapalı #58/#60 deseni; bugün 200 boş dönebilir.
    When "yönetici" şu isteği gönderir: GET "/v1/documents/{RANDOM_ID}/acl"
    Then yanıt durumu 404 olmalıdır
