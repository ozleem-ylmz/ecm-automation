@phase2 @phase2-rbac
Feature: Phase 2 — Gruplar, roller ve RBAC kapsamı

  # Kaynak: src/api/rest/groups.rs, rbac.rs, engine/admin.rs, engine/rbac/mod.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @rbac-guard @defect-candidate
  Scenario: /rbac/roles yalnızca yetkili hesaplara açık olmalıdır
    # Beklenti belirsiz (README "ACL read"); sıradan kullanıcı bugün 200 alıyor olabilir.
    Given "meraklı2" aktörü "user" hesap rolüyle hazırlanmıştır
    When "meraklı2" şu isteği gönderir: GET "/v1/rbac/roles"
    Then yanıt durumu 403 olmalıdır
