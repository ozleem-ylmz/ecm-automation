@phase2 @phase2-classes
Feature: Phase 2 — Sınıflar, özellikler, doğrulama kuralları ve şablonlar kapsamı

  # Kaynak: src/engine/classes/{read,write,validation,templates}.rs, custom_fields.rs,
  # property_value_codec.rs, api/rest/{classes,custom_fields,property_templates,validation_rules}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @classes @boundary @defect-candidate
  Scenario: Boş özellik anahtarıyla sınıf oluşturma reddedilmelidir
    # add_property boş anahtarı reddederken create yolu kabul ediyor olabilir.
    When "yönetici" şu isteği gönderir: POST "/v1/classes" gövde '{"name":"QA BosKey {UNIQUE}","properties":[{"key":"","display_name":"Bos","data_type":"text","is_multi_value":false,"required":false}]}'
    Then yanıt durumu 4xx istemci hatası olmalıdır
