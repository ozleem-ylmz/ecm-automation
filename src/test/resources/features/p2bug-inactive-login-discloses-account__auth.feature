@phase2 @phase2-auth
Feature: Phase 2 — Kimlik doğrulama, oturum ve kullanıcı yönetimi kapsamı

  # Kaynak: src/api/rest/auth.rs, users.rs, engine/admin.rs.
  # Beklentiler ECM sözleşmesinden türetilmiştir; bir senaryo başarısız olursa
  # bu bir ürün defekti ADAYIdır (ECM salt okunur; assertion zayıflatılmaz).

  @auth-login @defect-candidate
  Scenario: Pasif hesaba yanlış parolayla giriş hesabın varlığını sızdırmamalıdır
    # Beklenti: yanlış parola her zaman 401 invalid_credentials (pasiflik ancak parola doğrulandıktan sonra).
    Given "pasifaday" aktörü "user" hesap rolüyle hazırlanmıştır
    And "yönetici" şu isteği göndermiş ve 200 almıştır: PUT "/v1/users/{pasifaday}" gövde '{"is_active":false}'
    When oturumsuz şu istek gönderilir: POST "/v1/auth/login" gövde '{"email":"{name:pasifaday}","password":"yanlis-parola"}'
    Then yanıt hata kodu "invalid_credentials" olmalıdır
