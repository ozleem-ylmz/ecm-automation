@phase2 @phase2-auditfiles
Feature: Phase 2 — Denetim, geçmiş, dosya ve sistem uçları kapsamı

  # Kaynak: src/api/rest/{audit,maintenance,health}.rs, engine/{audit_service,history,storage_gc}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @audit @defect-candidate
  Scenario: İlişki türü oluşturma denetime yazılmalıdır
    # AuditAction::RelationshipTypeCreate tanımlı ama hiç emit edilmiyor olabilir.
    When "yönetici" şu isteği gönderir: POST "/v1/relationship-types" gövde '{"name":"QA Denetim Tür {UNIQUE}","is_directed":true}'
    Then yanıt durumu 201 olmalıdır
    And yanıt alanı "id" "denetimtur" kimliği olarak saklanır
    And "denetimtur" hedefi için "relationship_type.create" audit kaydı bulunmalıdır
