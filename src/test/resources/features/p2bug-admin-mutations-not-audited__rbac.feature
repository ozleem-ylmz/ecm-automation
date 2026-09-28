@phase2 @phase2-rbac
Feature: Phase 2 — Gruplar, roller ve RBAC kapsamı

  # Kaynak: src/api/rest/groups.rs, rbac.rs, engine/admin.rs, engine/rbac/mod.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @groups @defect-candidate
  Scenario: Grup silme bir audit kaydı yazmalıdır
    # Beklenti (README/#64 kapsamı): grup silme audit'lenir. Bugün yazılmıyor olabilir.
    Given "silinen" isimli yeni bir grup oluşturulmuştur
    When "yönetici" şu isteği gönderir: DELETE "/v1/groups/{silinen}"
    Then yanıt durumu 204 olmalıdır
    And "silinen" hedefi için "group.delete" audit kaydı bulunmalıdır

  @groups @defect-candidate
  Scenario: Üye çıkarma bir audit kaydı yazmalıdır
    Given "cuye" aktörü "user" hesap rolüyle hazırlanmıştır
    And "grupz" isimli yeni bir grup oluşturulmuştur
    And "cuye" aktörü "grupz" grubuna üye yapılmıştır
    When "yönetici" şu isteği gönderir: DELETE "/v1/groups/{grupz}/members/{cuye}"
    Then yanıt durumu 204 olmalıdır
    And "grupz" hedefi için "group.member_remove" audit kaydı bulunmalıdır
