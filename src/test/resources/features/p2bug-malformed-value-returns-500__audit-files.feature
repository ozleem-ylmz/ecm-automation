@phase2 @phase2-auditfiles
Feature: Phase 2 — Denetim, geçmiş, dosya ve sistem uçları kapsamı

  # Kaynak: src/api/rest/{audit,maintenance,health}.rs, engine/{audit_service,history,storage_gc}.rs.

  Background:
    Given yönetici API oturumu açmıştır

  @audit @defect-candidate
  Scenario Outline: Hatalı denetim tarih filtresi denetimli 4xx döndürmelidir
    # Beklenti: 422 (#105 deseni, farklı uç); bugün cast -> 500 olabilir.
    When "yönetici" şu isteği gönderir: GET "/v1/admin/audit-logs?<sorgu>"
    Then yanıt durumu 4xx istemci hatası olmalıdır

    Examples:
      | sorgu |
      | from=tarih-degil |
      | to=2026-13-45 |
