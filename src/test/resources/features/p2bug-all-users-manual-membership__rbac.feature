@phase2 @phase2-rbac
Feature: Phase 2 — Gruplar, roller ve RBAC kapsamı

  # Kaynak: src/api/rest/groups.rs, rbac.rs, engine/admin.rs, engine/rbac/mod.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @groups @allusers @defect-candidate
  Scenario: All Users grubundan aktif kullanıcı elle çıkarılamamalıdır
    # Beklenti: sistem grubu üyeliği otomatik yönetilir; elle çıkarma reddedilmeli.
    Given "sistemuye" aktörü "user" hesap rolüyle hazırlanmıştır
    When "yönetici" şu isteği gönderir: DELETE "/v1/groups/{ALL_USERS}/members/{sistemuye}"
    Then yanıt durumu 4xx istemci hatası olmalıdır
