@coverage-gap @users
Feature: Kullanıcı rolü değişikliklerinin etkisi

  # ECM kaynağı: src/api/rest/auth.rs:570-597 — rol her istekte veritabanından okunur,
  # JWT içindeki rol kullanılmaz; bu yüzden yeniden giriş gerekmez.
  @USER-02 @critical
  Scenario: USER-02 Rol yükseltme ve düşürme aynı access token ile bir sonraki istekte geçerli olmalıdır
    Given yönetici API oturumu açmıştır
    And "rol test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "rol test kullanıcısı" API ile giriş yapmıştır
    When "rol test kullanıcısı" kullanıcı listesini ister
    Then son API yanıtının durum kodu 403 olmalıdır
    When yönetici "rol test kullanıcısı" hesabının rolünü "admin" yapar
    And "rol test kullanıcısı" kullanıcı listesini ister
    Then son API yanıtının durum kodu 200 olmalıdır
    When yönetici "rol test kullanıcısı" hesabının rolünü "user" yapar
    And "rol test kullanıcısı" kullanıcı listesini ister
    Then son API yanıtının durum kodu 403 olmalıdır

  # ---------------------------------------------------------------------
  # Batch 22 — Users
  # ---------------------------------------------------------------------

  # ECM kaynağı: src/engine/admin.rs create_user_inner (e-posta normalize + tekil), validate_user_role
  @batch22 @USER-01
  Scenario: USER-01 Aynı e-posta, geçersiz rol ve boş e-posta ile kullanıcı oluşturulamamalıdır
    Given yönetici API oturumu açmıştır
    And "mevcut kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    When yönetici "mevcut kullanıcı" e-postasıyla yeni kullanıcı oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "A user with this email already exists." içermelidir
    When yönetici "mevcut kullanıcı" e-postasının büyük harfli haliyle yeni kullanıcı oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "A user with this email already exists." içermelidir
    When yönetici "superadmin" rolüyle yeni kullanıcı oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Invalid role 'superadmin'. Allowed roles: admin, manager, user." içermelidir
    And reddedilen e-postayla aktif bir kullanıcı oluşmamış olmalıdır
    When yönetici boş e-postayla kullanıcı oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Email is required." içermelidir

  # ECM kaynağı: src/api/rest/users.rs update_user / delete_user (kendi hesabı korumaları)
  @batch22 @USER-03 @critical
  Scenario: USER-03 Yönetici kendi rolünü düşüremez, kendini pasifleştiremez ve silemez
    Given yönetici API oturumu açmıştır
    And "geçici yönetici" isimli "admin" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "geçici yönetici" API ile giriş yapmıştır
    When "geçici yönetici" kendi hesabının rolünü "user" yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "You cannot change your own account role." içermelidir
    When "geçici yönetici" kendi hesabını pasifleştirmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "You cannot deactivate your own account." içermelidir
    When "geçici yönetici" kendi hesabını silmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "You cannot delete your own account." içermelidir
    And yönetici "geçici yönetici" hesabını aktif ve "admin" rolünde görmelidir
    When "geçici yönetici" kullanıcı listesini ister
    Then son API yanıtının durum kodu 200 olmalıdır

  # ECM kaynağı: postgres_repository/users.rs delete_user (FK 23503 -> UserHasAuditHistory 409)
  @batch22 @USER-04
  Scenario: USER-04 Denetim geçmişi olan kullanıcı silinemez, geçmişi olmayan kullanıcı silinebilir
    Given yönetici API oturumu açmıştır
    And "işlem yapan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "işlem yapmayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "işlem yapan" API ile giriş yapmıştır
    And yetki devralması kapalı "silme klasörü" oluşturulmuştur
    And "silme klasörü" klasöründe "işlem yapan" kullanıcısına "manager" rolü verilmiştir
    And "işlem yapan" "silme klasörü" klasörüne "iz belgesi" belgesini yüklemiştir
    When yönetici "işlem yapan" kullanıcısını silmeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata kodu "user_has_audit_history" olmalıdır
    And yönetici "işlem yapan" hesabını aktif ve "user" rolünde görmelidir
    When yönetici "işlem yapmayan" kullanıcısını silmeyi dener
    Then son API yanıtının durum kodu 204 olmalıdır
    And "işlem yapmayan" kullanıcısı artık bulunmamalıdır

  # ECM kaynağı: auth.rs require_auth rolü her istekte yeniden okur; runtime.rs is_manager
  @batch22 @USER-07
  Scenario: USER-07 User rolüne düşürülen manager kapsamlı yönetim yetkisini aynı oturumda kaybeder
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip üyesi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yönetici "ekip üyesi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    And "ekip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And "ekip klasörü" klasöründe "ekip üyesi" kullanıcısına "manager" rolü verilmiştir
    And "ekip üyesi" "ekip klasörü" klasörüne "ekip belgesi" belgesini yüklemiştir
    When "ekip yöneticisi" "ekip belgesi" belgesinde "statü under_review" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    When yönetici "ekip yöneticisi" hesabının rolünü "user" yapar
    And "ekip yöneticisi" "ekip belgesi" belgesinde "statü published" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "ekip belgesi" belgesinin durumu "under_review" olmalıdır
