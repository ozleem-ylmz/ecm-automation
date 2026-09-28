@coverage-gap @auth
Feature: ECM oturum güvenliği

  # ECM kaynağı: src/api/rest/auth.rs:281-285 (login), :469-474 (refresh), :591-594 (require_auth)
  @AUTH-01 @critical
  Scenario: AUTH-01 Pasifleştirilen kullanıcının açık oturumu, token yenilemesi ve yeni girişi reddedilmelidir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And "test kullanıcısı" kendi profil bilgisini alabilmelidir
    When yönetici "test kullanıcısı" hesabını pasifleştirir
    Then "test kullanıcısı" mevcut access token ile profil isteğinde pasif hesap hatası almalıdır
    And "test kullanıcısı" refresh token ile oturumunu yenileyememelidir
    And "test kullanıcısı" yeniden giriş yapmaya çalıştığında pasif hesap hatası almalıdır
    When yönetici "test kullanıcısı" hesabını tekrar aktifleştirir
    Then "test kullanıcısı" yeniden giriş yapabilmelidir

  # ---------------------------------------------------------------------
  # Batch 21 — Auth / session (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: src/api/rest/auth.rs refresh() — UPDATE ... revoked_at IS NULL (tek kullanımlık rotasyon)
  @batch21 @AUTH-02 @critical
  Scenario: AUTH-02 Kullanılmış refresh token ikinci kez oturum yenileyememelidir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    When "test kullanıcısı" refresh token ile oturumunu yeniler ve yeni bir refresh token alır
    Then "test kullanıcısı" kullanılmış eski refresh token ile oturum yenileyememelidir
    And "test kullanıcısı" yeni refresh token ile oturumunu yenileyebilmeli ve profilini alabilmelidir

  # ECM kaynağı: auth.rs logout() + frontend Layout.tsx handleLogout / AuthGuard.tsx
  @batch21 @AUTH-03 @critical
  Scenario: AUTH-03 Arayüzden çıkış refresh token'ı iptal etmeli ve korumalı sayfalar açılmamalıdır
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" tarayıcıda oturum açmış olarak "/folders" sayfasını açar
    When kullanıcı arayüzde kullanıcı menüsünden çıkış yapar
    Then çıkış isteği sunucuda başarıyla tamamlanmalı ve login sayfası görüntülenmelidir
    And tarayıcıda refresh token cookie'si kalmamalıdır
    And tarayıcıda "/documents" adresine gidildiğinde login sayfasına yönlendirilmeli ve veri gösterilmemelidir
    And çıkıştan önceki refresh token ile oturum yenilenememelidir

  # ECM kaynağı: admin.rs update_user (refresh token iptali) + auth.rs refresh() stale_password
  @batch21 @AUTH-04 @critical
  Scenario: AUTH-04 Yönetici parolayı değiştirdiğinde eski parola ve eski refresh token geçersiz olmalıdır
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    When yönetici "test kullanıcısı" hesabının parolasını "YeniParola!234" olarak değiştirir
    Then "test kullanıcısı" eski parolasıyla giriş yapamamalıdır
    And "test kullanıcısı" "YeniParola!234" parolasıyla giriş yapabilmelidir
    And "test kullanıcısı" parola değişikliğinden önceki refresh token ile oturum yenileyememelidir

  # PUT /v1/users/{id} yalnız admin'e açıktır (users.rs update_user require_admin_role); "kendi parolası"
  # kuralı (admin.rs verify_current_password) bu yüzden geçici bir admin hesabıyla sınanır. Seed admin değişmez.
  @batch21 @AUTH-05
  Scenario: AUTH-05 Kendi parolasını değiştiren kullanıcı mevcut parolasını doğru vermelidir
    Given yönetici API oturumu açmıştır
    And "geçici yönetici" isimli "admin" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "geçici yönetici" API ile giriş yapmıştır
    When "geçici yönetici" mevcut parolasını göndermeden kendi parolasını "Yeni!2345" yapmaya çalışır
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "current_password is required to change your own password." içermelidir
    When "geçici yönetici" yanlış mevcut parolayla kendi parolasını "Yeni!2345" yapmaya çalışır
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "current_password is incorrect." içermelidir
    And "geçici yönetici" reddedilen denemelerden sonra eski parolasıyla giriş yapabilmelidir
    When "geçici yönetici" doğru mevcut parolayla kendi parolasını "Yeni!2345" yapar
    Then "geçici yönetici" eski parolasıyla giriş yapamamalıdır
    And "geçici yönetici" "Yeni!2345" parolasıyla giriş yapabilmelidir

  # ECM kaynağı: auth.rs register() rol sunucuda sabit "user"; frontend pages/SignUp.tsx
  @batch21 @AUTH-07
  Scenario: AUTH-07 Kayıt olan hesap her zaman aktif user rolünde oluşturulmalı, istenen rol yok sayılmalıdır
    Given yönetici API oturumu açmıştır
    When anonim ziyaretçi arayüzdeki kayıt formunu "arayüz kaydı" için benzersiz bir e-posta ile doldurup gönderir
    Then kayıt sonrası uygulamaya oturum açmış olarak yönlendirilmelidir
    When anonim ziyaretçi "API kaydı" için API ile "admin" rolü isteyerek kayıt olur
    Then kayıt yanıtında "API kaydı" hesabının rolü "user" olmalıdır
    And yönetici "arayüz kaydı" hesabını aktif ve "user" rolünde görmelidir
    And yönetici "API kaydı" hesabını aktif ve "user" rolünde görmelidir
    And "arayüz kaydı" yalnız yöneticiye açık kullanıcı listesine erişememelidir
    And "API kaydı" yalnız yöneticiye açık kullanıcı listesine erişememelidir

  # ECM kaynağı: frontend components/AuthGuard.tsx; backend auth.rs require_auth
  @batch21 @AUTH-09
  Scenario: AUTH-09 Oturumsuz kullanıcı korumalı sayfalara doğrudan adresle girememelidir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "gizli klasör" oluşturulmuştur
    And yönetici "gizli klasör" klasörüne "gizli belge" belgesini yüklemiştir
    Then oturumsuz tarayıcıda şu adresler login sayfasına yönlendirilmeli ve veri göstermemelidir
      | /documents               |
      | /folders/{gizli klasör}  |
      | /admin/users             |
      | /documents/{gizli belge} |
    And oturumsuz "GET" "/v1/documents" isteği 401 almalıdır
    And oturumsuz "GET" "/v1/auth/me" isteği 401 almalıdır
    And oturumsuz "GET" "/v1/documents/{gizli belge}" isteği 401 almalıdır

  # ECM kaynağı: auth.rs logout() yalnız sunulan refresh token'ı iptal eder
  @batch21 @AUTH-10
  Scenario: AUTH-10 Bir oturumun çıkışı aynı kullanıcının diğer oturumunu sonlandırmamalıdır
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" iki ayrı oturumla API ile giriş yapmıştır
    When "test kullanıcısı" birinci oturumdan çıkış yapar
    Then çıkış yanıtı 204 olmalı ve refresh cookie'sini temizlemelidir
    And "test kullanıcısı" ikinci oturumunu yenileyebilmeli ve profilini alabilmelidir
    And "test kullanıcısı" birinci oturumun refresh token'ı ile oturum yenileyememelidir

  # ECM kaynağı: frontend api/client.ts api() 401 -> tryRefresh() -> tek tekrar; access token yalnız bellekte.
  # jwt ttl 8 saat ve test ortamında kısaltılamaz: istek başlığına geçersiz token enjekte edilir.
  @batch21 @AUTH-11
  Scenario: AUTH-11 Geçersiz access token ile yapılan arayüz işlemi sessiz yenilemeyle tamamlanmalı, refresh token yoksa kaydedilmemelidir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "çalışma klasörü" oluşturulmuştur
    And "çalışma klasörü" klasöründe "test kullanıcısı" kullanıcısına "manager" rolü verilmiştir
    And "test kullanıcısı" API ile giriş yapmıştır
    And "test kullanıcısı" "çalışma klasörü" klasörüne "çalışma belgesi" belgesini yüklemiştir
    And "test kullanıcısı" tarayıcıda oturum açmış olarak "çalışma belgesi" belgesinin detayını açar
    When sonraki etiket ekleme isteği geçersiz bir access token ile gönderilecek şekilde ayarlanır
    And kullanıcı arayüzde belgeye "birinci etiket" etiketini ekler
    Then etiket isteği önce 401 almalı, oturum sessizce yenilenmeli ve istek bir kez başarıyla tekrarlanmalıdır
    And "birinci etiket" etiketi arayüzde ve API'de "çalışma belgesi" belgesinde görünmelidir
    When tarayıcıdaki refresh token cookie'si silinir
    And sonraki etiket ekleme isteği geçersiz bir access token ile gönderilecek şekilde ayarlanır
    And kullanıcı arayüzde belgeye "ikinci etiket" etiketini ekler
    Then oturum yenileme 401 almalı ve etiket isteği tekrarlanmamalıdır
    And "ikinci etiket" etiketi API'de "çalışma belgesi" belgesinde bulunmamalıdır
    And sayfa yenilendiğinde login sayfası görüntülenmeli ve veri gösterilmemelidir

  # ECM kaynağı: auth.rs logout() refresh cookie'yi iptal eder; access JWT durumsuzdur (rest.yaml jwt.ttl_hours: 8).
  # Diğer sekmede sayfa yenilenmeden yapılan işlemin kabul edilip edilmeyeceği ürün kararıdır (RQ-15):
  # sonucu kanıt olarak eklenir; sözleşmesi net kısımlar doğrulanır.
  @batch21 @AUTH-12
  Scenario: AUTH-12 Bir sekmede çıkış yapıldığında diğer sekme oturumu yenileyememeli ve yenilemede login'e düşmelidir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "çalışma klasörü" oluşturulmuştur
    And "çalışma klasörü" klasöründe "test kullanıcısı" kullanıcısına "manager" rolü verilmiştir
    And "test kullanıcısı" API ile giriş yapmıştır
    And "test kullanıcısı" "çalışma klasörü" klasörüne "çalışma belgesi" belgesini yüklemiştir
    And "test kullanıcısı" tarayıcıda oturum açmış olarak "çalışma belgesi" belgesinin detayını açar
    And aynı tarayıcıda ikinci sekmede "çalışma belgesi" belgesinin detayı açılır
    When kullanıcı arayüzde kullanıcı menüsünden çıkış yapar
    Then çıkış isteği sunucuda başarıyla tamamlanmalı ve login sayfası görüntülenmelidir
    When ikinci sekmede kullanıcı belgeye "sekme etiketi" etiketini eklemeyi dener
    Then ikinci sekmedeki işlemin sonucu RQ-15 kararı için kanıt olarak kaydedilmelidir
    And ikinci sekme yenilendiğinde login sayfası görüntülenmeli ve belge verisi gösterilmemelidir
    And ikinci sekmede oturum yenileme başarısız olmalıdır
