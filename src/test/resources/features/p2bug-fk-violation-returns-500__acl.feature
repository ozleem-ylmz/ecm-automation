@phase2 @phase2-acl
Feature: Phase 2 — ACL, etkin erişim ve miras kapsamı

  # Kaynak: src/engine/acl/{runtime,service,management}.rs, api/rest folder/document/class ACL.
  # #106, #107, #110 dondurulmuş defektlerdir; burada tekrar edilmez.

  Background:
    Given yönetici API oturumu açmıştır

  @acl-guard @defect-candidate
  Scenario: FK ihlali olan ACL isteği denetimli 4xx döndürmelidir
    # Beklenti: bilinmeyen principal 404/422 (bugün RepositoryError::Internal -> 500).
    Given "fkfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur
    When "yönetici" şu isteği gönderir: POST "/v1/folders/{fkfolder}/acl" gövde '{"role_id":"{ROLE_VIEWER}","user_id":"{RANDOM_ID}","deny":false}'
    Then yanıt durumu 4xx istemci hatası olmalıdır

  @acl-guard @defect-candidate
  Scenario: Var olmayan klasöre admin ACL isteği 404 döndürmelidir
    When "yönetici" şu isteği gönderir: POST "/v1/folders/{RANDOM_ID}/acl" gövde '{"role_id":"{ROLE_VIEWER}","user_id":"{ADMIN_ID}","deny":false}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
