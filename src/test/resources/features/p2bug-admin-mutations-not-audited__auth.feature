@phase2 @phase2-auth
Feature: Phase 2 — Kimlik doğrulama, oturum ve kullanıcı yönetimi kapsamı

  # Kaynak: src/api/rest/auth.rs, users.rs, engine/admin.rs.
  # Beklentiler ECM sözleşmesinden türetilmiştir; bir senaryo başarısız olursa
  # bu bir ürün defekti ADAYIdır (ECM salt okunur; assertion zayıflatılmaz).

  @users @defect-candidate
  Scenario: Kullanıcı oluşturma bir audit kaydı yazmalıdır
    # Beklenti: yönetici işlemleri audit'lenir (README). Bugün yalnızca activate/deactivate/login yazılıyor.
    Given "denetimli" aktörü "user" hesap rolüyle hazırlanmıştır
    Then "denetimli" hedefi için "user.create" audit kaydı bulunmalıdır
