@phase2 @phase2-classes
Feature: Phase 2 — Sınıflar, özellikler, doğrulama kuralları ve şablonlar kapsamı

  # Kaynak: src/engine/classes/{read,write,validation,templates}.rs, custom_fields.rs,
  # property_value_codec.rs, api/rest/{classes,custom_fields,property_templates,validation_rules}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @classes @defect-candidate
  Scenario: Sınıf yeniden adlandırmada ad boşlukları kırpılmalıdır
    # create adı kırparken update kırpmıyor olabilir; " X " ile "X" çakışmalı.
    When "yönetici" şu isteği gönderir: POST "/v1/classes" gövde '{"name":"QA Kirp Sinif {UNIQUE}","properties":[]}'
    Then yanıt alanı "name" "kirpsinifadi" kimliği olarak saklanır
    Given yönetici "ref:text:opsiyonel" özellikleriyle "digersinif" sınıfını oluşturmuştur
    When "yönetici" şu isteği gönderir: PATCH "/v1/classes/{digersinif}" gövde '{"expected_version":{classver:digersinif},"name":"  {kirpsinifadi}  "}'
    Then yanıt durumu 409 olmalıdır
