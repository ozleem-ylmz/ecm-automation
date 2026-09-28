@phase2 @phase2-classes
Feature: Phase 2 — Sınıflar, özellikler, doğrulama kuralları ve şablonlar kapsamı

  # Kaynak: src/engine/classes/{read,write,validation,templates}.rs, custom_fields.rs,
  # property_value_codec.rs, api/rest/{classes,custom_fields,property_templates,validation_rules}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @classes @defect-candidate
  Scenario: Sınıf açıklaması temizlenebilmelidir
    # Beklenti: description null gönderince temizlenmeli; COALESCE nedeniyle eski değer kalıyor olabilir.
    Given yönetici "ref:text:opsiyonel" özellikleriyle "aciklamali" sınıfını oluşturmuştur
    When "yönetici" şu isteği gönderir: PATCH "/v1/classes/{aciklamali}" gövde '{"expected_version":{classver:aciklamali},"name":"{name:aciklamali}","description":"ilk açıklama"}'
    Then yanıt durumu 200 olmalıdır
    When "yönetici" şu isteği gönderir: PATCH "/v1/classes/{aciklamali}" gövde '{"expected_version":{classver:aciklamali},"name":"{name:aciklamali}","description":null}'
    Then yanıt durumu 200 olmalıdır
    When "yönetici" şu isteği gönderir: GET "/v1/classes/{aciklamali}"
    Then yanıt alanı "description" bulunmamalı veya boş olmalıdır
