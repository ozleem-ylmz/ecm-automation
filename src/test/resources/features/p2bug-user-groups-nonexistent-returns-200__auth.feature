@phase2 @phase2-auth
Feature: Phase 2 — Kimlik doğrulama, oturum ve kullanıcı yönetimi kapsamı

  # Kaynak: src/api/rest/auth.rs, users.rs, engine/admin.rs.
  # Beklentiler ECM sözleşmesinden türetilmiştir; bir senaryo başarısız olursa
  # bu bir ürün defekti ADAYIdır (ECM salt okunur; assertion zayıflatılmaz).

  @users @defect-candidate
  Scenario: Var olmayan kullanıcının grup listesi 404 döndürmelidir
    # Beklenti: GET /users/{id} 404 verirken /users/{id}/groups da 404 vermeli (kapalı #58 deseni).
    When "yönetici" şu isteği gönderir: GET "/v1/users/{RANDOM_ID}/groups"
    Then yanıt durumu 404 olmalıdır
