@phase2 @phase2-search
Feature: Phase 2 — Arama, facet ve kayıtlı arama kapsamı

  # Kaynak: src/api/rest/documents_query.rs, query.rs, saved_searches.rs, engine/documents/read.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @search @defect-candidate
  Scenario: /documents/search geçersiz durumda denetimli 4xx döndürmelidir
    # Beklenti: 422 (/documents ile aynı); bugün cast hatası -> 500 olabilir.
    When "yönetici" şu isteği gönderir: GET "/v1/documents/search?status=bogus"
    Then yanıt durumu 4xx istemci hatası olmalıdır
