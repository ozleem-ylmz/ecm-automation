@phase2 @phase2-classes
Feature: Phase 2 — Sınıflar, özellikler, doğrulama kuralları ve şablonlar kapsamı

  # Kaynak: src/engine/classes/{read,write,validation,templates}.rs, custom_fields.rs,
  # property_value_codec.rs, api/rest/{classes,custom_fields,property_templates,validation_rules}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @classes @boundary @defect-candidate
  Scenario: Bilinmeyen veri tipli özellikle sınıf denetimli reddedilmelidir
    # Beklenti: 422 (bugün RepositoryError::Internal -> 500 olabilir).
    When "yönetici" şu isteği gönderir: POST "/v1/classes" gövde '{"name":"QA Tip {UNIQUE}","properties":[{"key":"alan","display_name":"Alan","data_type":"enum","is_multi_value":false,"required":false}]}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
