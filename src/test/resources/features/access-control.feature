@coverage-gap @acl
Feature: Klasör erişim kontrolü (ACL, grup, devralma)

  # Seed: kök klasör All Users grubuna viewer verir (db/migrations/0021_acl.sql:78-93).
  # Bu yüzden izole senaryolar "yetki devralması kapalı" kök seviye klasörle başlar;
  # aksi halde her kullanıcı kökten devralınan viewer ile zaten okuyabilir.
  # Değerlendirme: src/engine/acl/service.rs:315-370, grup üyeliği: runtime.rs:18-33.

  @ACL-01 @critical
  Scenario: ACL-01 Grup üyeliği eklenip çıkarıldığında klasör erişimi anında değişmelidir
    Given yönetici API oturumu açmıştır
    And "okuyucu" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okuyucu" API ile giriş yapmıştır
    And "okuma grubu" isimli yeni bir grup oluşturulmuştur
    And yetki devralması kapalı "gizli klasör" oluşturulmuştur
    And "gizli klasör" klasöründe "okuma grubu" grubuna "viewer" rolü verilmiştir
    Then "okuyucu" "gizli klasör" klasörünü görüntüleyememelidir
    When yönetici "okuyucu" kullanıcısını "okuma grubu" grubuna ekler
    Then "okuyucu" "gizli klasör" klasörünü görüntüleyebilmelidir
    When yönetici "okuyucu" kullanıcısını "okuma grubu" grubundan çıkarır
    Then "okuyucu" "gizli klasör" klasörünü görüntüleyememelidir
    When "okuyucu" API ile yeniden giriş yapar
    Then "okuyucu" "gizli klasör" klasörünü görüntüleyememelidir

  @ACL-02 @critical
  Scenario: ACL-02 Alt klasördeki açık deny kuralı üst klasörden devralınan izni geçersiz kılmalıdır
    Given yönetici API oturumu açmıştır
    And "okuyucu" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okuyucu" API ile giriş yapmıştır
    And yetki devralması kapalı "üst klasör" oluşturulmuştur
    And "üst klasör" altında yetki devralması açık "alt klasör" oluşturulmuştur
    And "üst klasör" klasöründe "okuyucu" kullanıcısına "viewer" rolü verilmiştir
    Then "okuyucu" "alt klasör" klasörünü görüntüleyebilmelidir
    When "alt klasör" klasöründe "okuyucu" kullanıcısı için "viewer" rolü reddedilir
    Then "okuyucu" "alt klasör" klasörünü görüntüleyememelidir
    And "okuyucu" "üst klasör" klasörünü görüntüleyebilmelidir
    And "okuyucu" tarayıcıda "alt klasör" klasörünü açtığında okuma erişimi olmadığı mesajını görmelidir

  @ACL-06 @critical
  Scenario: ACL-06 Yetki devralması kapatıldığında üst klasör izni alt klasöre uygulanmamalıdır
    Given yönetici API oturumu açmıştır
    And "okuyucu" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okuyucu" API ile giriş yapmıştır
    And yetki devralması kapalı "üst klasör" oluşturulmuştur
    And "üst klasör" altında yetki devralması açık "alt klasör" oluşturulmuştur
    And "üst klasör" klasöründe "okuyucu" kullanıcısına "viewer" rolü verilmiştir
    Then "okuyucu" "alt klasör" klasörünü görüntüleyebilmelidir
    And "alt klasör" klasörünün yetki durumu "inherited" olmalıdır
    When admin kullanıcı tarayıcıda "alt klasör" klasörünün yetki devralmasını kapatır
    Then "okuyucu" "alt klasör" klasörünü görüntüleyememelidir
    And "alt klasör" klasörünün yetki durumu "default_deny" olmalıdır
    When admin kullanıcı tarayıcıda "alt klasör" klasörünün yetki devralmasını tekrar açar
    Then "okuyucu" "alt klasör" klasörünü görüntüleyebilmelidir
    And "alt klasör" klasörünün yetki durumu "inherited" olmalıdır

  # ---------------------------------------------------------------------
  # Batch 23 — Folder ACL (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: runtime.rs assert_document_access (klasör zinciri belgenin güncel klasöründen okunur)
  @batch23 @ACL-03 @critical
  Scenario: ACL-03 Belge izole klasörden herkese açık klasöre taşınınca görünür, geri taşınınca aynı oturumda tekrar gizlenir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And yetki devralması kapalı "izole klasör" oluşturulmuştur
    And yetki devralması kapalı "açık klasör" oluşturulmuştur
    And "açık klasör" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "izole klasör" klasörüne "taşınan belge" belgesini yüklemiştir
    When "test kullanıcısı" "taşınan belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "test kullanıcısı" belge aramasında "taşınan belge" belgesini görmemelidir
    When yönetici "taşınan belge" belgesini "açık klasör" klasörüne taşır
    And "test kullanıcısı" "taşınan belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "test kullanıcısı" belge aramasında "taşınan belge" belgesini görmelidir
    When yönetici "taşınan belge" belgesini "izole klasör" klasörüne taşır
    And "test kullanıcısı" "taşınan belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "test kullanıcısı" belge aramasında "taşınan belge" belgesini görmemelidir

  # ECM kaynağı: service.rs caller_has_direct_document_allow — doğrudan belge izni admin-authored korumasını aşar
  @batch23 @ACL-04 @critical
  Scenario: ACL-04 Admin belgesine doğrudan yazma izni verilen kullanıcı belgeyi değiştirebilir, yalnız klasör izni olan değiştiremez
    Given yönetici API oturumu açmıştır
    And "doğrudan yetkili" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör yetkili" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "doğrudan yetkili" API ile giriş yapmıştır
    And "klasör yetkili" API ile giriş yapmıştır
    And yetki devralması kapalı "paylaşım klasörü" oluşturulmuştur
    And "paylaşım klasörü" klasöründe "doğrudan yetkili" kullanıcısına "editor" rolü verilmiştir
    And "paylaşım klasörü" klasöründe "klasör yetkili" kullanıcısına "editor" rolü verilmiştir
    And yönetici "paylaşım klasörü" klasörüne "admin belgesi" belgesini yüklemiştir
    And "admin belgesi" belgesinde "doğrudan yetkili" kullanıcısına doğrudan "editor" rolü verilmiştir
    And "admin belgesi" belgesinin mevcut durumu kaydedilmiştir
    Then "klasör yetkili" "admin belgesi" belgesinde şu işlemlerin her biri 403 ile reddedilmelidir
      | yeniden adlandırma |
      | etiket ekleme      |
    And "admin belgesi" belgesi kaydedilen durumundan değişmemiş olmalıdır
    And "doğrudan yetkili" "admin belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
      | etiket ekleme      |
    And "admin belgesi" belgesinin başlığı yeniden adlandırılmış olmalıdır

  # ECM kaynağı: engine/acl/management/document.rs assert_can_manage_document_acl (yalnız admin)
  @batch23 @ACL-09 @critical
  Scenario: ACL-09 Belge erişim istisnalarını yalnız admin yönetebilir, klasör sahibi ve kapsamlı manager yönetemez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "klasör sahibi" kullanıcısını "ekip" grubuna ekler
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And "sahip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "klasör sahibi" "sahip klasörü" klasörüne "sahip belgesi" belgesini yüklemiştir
    And "sahip belgesi" belgesinin doğrudan erişim listesi kaydedilmiştir
    When "klasör sahibi" "sahip belgesi" belgesinde All Users grubuna doğrudan okuma erişimi vermeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "Only administrators can manage document access exceptions." içermelidir
    When "ekip yöneticisi" "sahip belgesi" belgesinde All Users grubuna doğrudan okuma erişimi vermeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "Only administrators can manage document access exceptions." içermelidir
    And "sahip belgesi" belgesinin doğrudan erişim listesi değişmemiş olmalıdır

  # ECM kaynağı: service.rs — doğrudan belge DENY en üst katmandır; kaldırılınca klasör izni geçerli olur
  @batch23 @ACL-05
  Scenario: ACL-05 Belge düzeyindeki DENY kaldırılınca klasörden gelen okuma erişimi aynı oturumda geri gelir
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And yetki devralması kapalı "okuma klasörü" oluşturulmuştur
    And "okuma klasörü" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "okuma klasörü" klasörüne "engelli belge" belgesini yüklemiştir
    And "engelli belge" belgesinde "test kullanıcısı" kullanıcısı için "viewer" rolü doğrudan reddedilmiştir
    When "test kullanıcısı" "engelli belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When yönetici "engelli belge" belgesindeki "test kullanıcısı" için reddetme kuralını kaldırır
    Then "test kullanıcısı" "engelli belge" belgesini okuyup içeriğini indirebilmelidir

  # ECM kaynağı: api/rest/error.rs DuplicateAclGrant -> 409
  @batch23 @ACL-07
  Scenario: ACL-07 Aynı ilke için aynı klasör ACL kuralı ikinci kez eklenemez
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "kural klasörü" oluşturulmuştur
    And "kural klasörü" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    When yönetici "kural klasörü" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolünü tekrar vermeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "An identical access rule already exists for this principal." içermelidir
    And "kural klasörü" klasöründe "test kullanıcısı" kullanıcısı için tek izin satırı bulunmalıdır

  # ECM kaynağı: FolderAclCard.tsx + EmailUserPicker.tsx (sahip yalnız tam e-posta ile kullanıcı seçer)
  @batch23 @ACL-10
  Scenario: ACL-10 Klasör sahibi arayüzde e-posta ile kullanıcıya okuma verir, kaldırınca kullanıcı belgeyi göremez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "paylaşılan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "paylaşılan" API ile giriş yapmıştır
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "klasör sahibi" "sahip klasörü" klasörüne "sahip belgesi" belgesini yüklemiştir
    When "paylaşılan" "sahip belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "klasör sahibi" tarayıcıda "sahip klasörü" klasörünün yetki kartından "paylaşılan" kullanıcısına e-posta ile okuma yetkisi verir
    Then "sahip klasörü" klasörünün yetki listesinde "paylaşılan" için okuma satırı bulunmalıdır
    And "paylaşılan" "sahip belgesi" belgesini okuyup içeriğini indirebilmelidir
    When "klasör sahibi" tarayıcıda "sahip klasörü" klasörünün yetki kartından "paylaşılan" satırını kaldırır
    And "paylaşılan" "sahip belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır

  # ECM kaynağı: api/rest/folders.rs require_manage_folder (ACL yönetimi = sahip/kapsamlı manager/admin)
  @batch23 @ACL-11
  Scenario: ACL-11 Klasörde yazma yetkisi olan ama sahip ya da kapsamlı manager olmayan kullanıcı ACL yönetemez
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "diğer kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "admin klasörü" oluşturulmuştur
    And "admin klasörü" klasöründe "yazar" kullanıcısına "editor" rolü verilmiştir
    And "admin klasörü" klasöründe "diğer kullanıcı" kullanıcısına "viewer" rolü verilmiştir
    And "admin klasörü" klasörünün mevcut durumu kaydedilmiştir
    Then "yazar" "admin klasörü" klasöründe şu yönetim işlemlerinin her biri 403 ile reddedilmelidir
      | yetki verme    |
      | yetki kaldırma |
    And "admin klasörü" klasörü kaydedilen durumundan değişmemiş olmalıdır

  # ECM kaynağı: service.rs evaluate_folder_chain (aynı seviyede kullanıcı kuralı grup kuralından önce gelir)
  @batch23 @ACL-12
  Scenario: ACL-12 Aynı seviyede kullanıcı ALLOW grup DENY'ı ezer, kullanıcı DENY grup ALLOW'u ezer
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And "karar grubu" isimli yeni bir grup oluşturulmuştur
    And yönetici "test kullanıcısı" kullanıcısını "karar grubu" grubuna ekler
    And yetki devralması kapalı "birinci klasör" oluşturulmuştur
    And yönetici "birinci klasör" klasörüne "birinci belge" belgesini yüklemiştir
    And "birinci klasör" klasöründe "karar grubu" grubu için "viewer" rolü reddedilmiştir
    And "birinci klasör" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    When "test kullanıcısı" "birinci belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır
    When yönetici "birinci klasör" klasöründe "test kullanıcısı" kullanıcısının izin kuralını kaldırır
    And "test kullanıcısı" "birinci belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    Given yetki devralması kapalı "ikinci klasör" oluşturulmuştur
    And yönetici "ikinci klasör" klasörüne "ikinci belge" belgesini yüklemiştir
    And "ikinci klasör" klasöründe "karar grubu" grubuna "viewer" rolü verilmiştir
    And "ikinci klasör" klasöründe "test kullanıcısı" kullanıcısı için "viewer" rolü reddedilir
    When "test kullanıcısı" "ikinci belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır

  # ECM kaynağı: runtime.rs caller_created_document (yaratıcının okuma tutamacı) + ROLES.md:80.
  # Yaratıcının yazma hakkı (auto_grant_document_creator) ROLES.md ile çelişir: RQ-19 kanıtı eklenir.
  @batch23 @ACL-14
  Scenario: ACL-14 Belge yaratıcısı klasör devredilse ve kalıtım kapatılsa da belgesini okur, yönetim yeni sahibe geçer
    Given yönetici API oturumu açmıştır
    And "eski sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yeni sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "eski sahip" API ile giriş yapmıştır
    And "yeni sahip" API ile giriş yapmıştır
    And yetki devralması açık "devir klasörü" oluşturulmuştur
    And yönetici "devir klasörü" klasörünün sahipliğini "eski sahip" kullanıcısına devretmiştir
    And "eski sahip" "devir klasörü" klasörüne "yaratıcının belgesi" belgesini yüklemiştir
    When yönetici "devir klasörü" klasörünün sahipliğini "yeni sahip" kullanıcısına devretmiştir
    And yönetici "devir klasörü" klasörünün yetki devralmasını kapatır
    Then "devir klasörü" klasöründe hiç izin satırı bulunmamalıdır
    And "eski sahip" "yaratıcının belgesi" belgesini okuyup içeriğini indirebilmelidir
    And "eski sahip" belge aramasında "yaratıcının belgesi" belgesini görmelidir
    And "yeni sahip" "yaratıcının belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
      | statü under_review |
    And "eski sahip" kullanıcısının "yaratıcının belgesi" belgesindeki yazma sonucu RQ-19 kanıtı olarak kaydedilmelidir

  # ECM kaynağı: runtime.rs get_effective_access (sınıf ACL'i gerçek kararın parçası) + EffectiveAccessCard.tsx
  @batch23 @ACL-15
  Scenario: ACL-15 Admin etkin erişim kartı sınıf kısıtını gerçek erişim kararıyla uyumlu gösterir
    Given yönetici API oturumu açmıştır
    And "sınıf izinli" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "sınıf izinsiz" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "sınıf izinli" API ile giriş yapmıştır
    And "sınıf izinsiz" API ile giriş yapmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And "sınıf klasörü" klasöründe "sınıf izinli" kullanıcısına "viewer" rolü verilmiştir
    And "sınıf klasörü" klasöründe "sınıf izinsiz" kullanıcısına "viewer" rolü verilmiştir
    And yönetici erişimi "sınıf izinli" kullanıcısıyla sınırlı "kısıtlı sınıf" sınıfını oluşturmuştur
    And yönetici "kısıtlı sınıf" sınıfıyla "sınıf klasörü" klasörüne "sınıflı belge" belgesini yüklemiştir
    Then "sınıflı belge" belgesinin etkin erişim raporu sınıf iznini yalnız "sınıf izinli" için göstermelidir
    When "sınıf izinsiz" "sınıflı belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "sınıf izinli" "sınıflı belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And yönetici tarayıcıda "sınıflı belge" belgesinin etkin erişim kartında sınıf kaynağını görmelidir

  # ECM kaynağı: src/api/rest/auth.rs lookup_user (tam eşleşme, yalnız aktif kullanıcı)
  @batch23 @ACL-17
  Scenario: ACL-17 Kullanıcı arama yalnız tam e-posta ile aktif kullanıcıyı döndürür
    Given yönetici API oturumu açmıştır
    And "arayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "aranan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "pasif kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "arayan" API ile giriş yapmıştır
    And yönetici "pasif kullanıcı" hesabını pasifleştirir
    Then "arayan" kullanıcı aramasında "aranan" e-postasının tamamıyla 200 almalıdır
    And "arayan" kullanıcı aramasında "aranan" e-postasının ön ekiyle 404 almalıdır
    And "arayan" kullanıcı aramasında "pasif kullanıcı" e-postasının tamamıyla 404 almalıdır
    And "arayan" kullanıcı aramasında boş e-postayla 400 almalıdır
    And son API yanıtının hata mesajı "email is required" içermelidir

  # ECM kaynağı: admin.rs delete_group + ACL satırlarının grup silinince kaldırılması (cascade) — RQ-8
  @batch23 @GRP-02 @critical
  Scenario: GRP-02 ACL'de kullanılan grup silinince üyeler erişimi kaybeder ve yetim ACL satırı kalmaz
    Given yönetici API oturumu açmıştır
    And "grup üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "grup üyesi" API ile giriş yapmıştır
    And "silinecek grup" isimli yeni bir grup oluşturulmuştur
    And yönetici "grup üyesi" kullanıcısını "silinecek grup" grubuna ekler
    And yetki devralması kapalı "grup klasörü" oluşturulmuştur
    And "grup klasörü" klasöründe "silinecek grup" grubuna "viewer" rolü verilmiştir
    And yönetici "grup klasörü" klasörüne "grup belgesi" belgesini yüklemiştir
    And "grup üyesi" "grup belgesi" belgesini okuyup içeriğini indirebilmelidir
    When yönetici "silinecek grup" grubunu siler
    Then grup silme reddedilmediyse "grup klasörü" klasöründe "silinecek grup" grubuna ait izin satırı kalmamalıdır
    And "grup belgesi" belgesinin etkin erişim raporunda "silinecek grup" grubu görünmemelidir
    When "grup üyesi" "grup belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
