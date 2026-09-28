@phase2 @phase2-reltags
Feature: Phase 2 — İlişkiler ve etiketler kapsamı

  # Kaynak: src/engine/relationships/mod.rs, engine/documents/tags.rs.

  Background:
    Given yönetici API oturumu açmıştır
    And "rtfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur

  @relationships @defect-candidate
  Scenario: İlişki türü yeniden adlandırmada çakışma doğru kaynak mesajı vermelidir
    # Beklenti: ilişki-türü mesajı; bugün "A class with this name..." dönüyor olabilir.
    Given "yönetici" şu isteği göndermiş ve 201 almıştır: POST "/v1/relationship-types" gövde '{"name":"QA T1 {UNIQUE}","is_directed":true}'
    And yanıt alanı "name" "t1" adı olarak saklanır
    And "yönetici" şu isteği göndermiş ve 201 almıştır: POST "/v1/relationship-types" gövde '{"name":"QA T2 {UNIQUE}","is_directed":true}'
    And yanıt alanı "id" "t2id" kimliği olarak saklanır
    When "yönetici" şu isteği gönderir: PUT "/v1/relationship-types/{t2id}" gövde '{"name":"{name:t1}"}'
    Then yanıt durumu 409 olmalıdır
    And yanıt hata mesajı "class" içermemelidir
