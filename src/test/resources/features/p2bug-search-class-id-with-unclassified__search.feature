@phase2 @phase2-search
Feature: Phase 2 — Arama, facet ve kayıtlı arama kapsamı

  # Kaynak: src/api/rest/documents_query.rs, query.rs, saved_searches.rs, engine/documents/read.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @search @defect-candidate
  Scenario: class_id ve unclassified birlikte tutarlı davranmalıdır
    # documents_query yorumu unclassified'ı yok sayacağını söylüyor; SQL ikisini AND'liyor -> her zaman boş olabilir.
    Given yönetici "ref:text:opsiyonel" özellikleriyle "aramasinif" sınıfını oluşturmuştur
    And "csfolder" klasörü "kök" altında "yönetici" tarafından miras "kapalı" olarak oluşturulmuştur
    And "yönetici" aktörü "csfolder" klasörüne "aramasinif" sınıfından "csbelge" belgesini "{}" metadatasıyla yüklemiştir
    When "yönetici" şu isteği gönderir: GET "/v1/documents?class_id={aramasinif}&unclassified=true&limit=10"
    Then yanıt listesi "items" içinde "id" değeri "{csbelge}" olan öğe bulunmalıdır
