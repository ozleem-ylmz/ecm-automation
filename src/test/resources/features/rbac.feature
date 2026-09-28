@coverage-gap @rbac
Feature: Admin-only işlemler için hesap rolü yetkilendirmesi

  # ECM kaynağı: src/engine/doc_management_engine.rs:510-516 require_admin_role -> 403 "forbidden"
  # Guard, handler gövdesinde engine çağrısından önce çalışır; bu yüzden {RANDOM_ID}
  # ile 404 yerine 403 beklenir. JSON gövdeleri geçerli DTO şeklindedir çünkü
  # ValidJson çıkarımı guard'dan önce çalışır (geçersiz gövde 422 döndürürdü).
  @RBAC-01 @critical @batch34
  Scenario Outline: RBAC-01 <hesap> admin-only işlemi yapamamalıdır: <method> <endpoint>
    Given "<hesap>" hesabı ile API oturumu açılmıştır
    When bu oturum "<method>" "<endpoint>" isteğini '<gövde>' gövdesiyle gönderir
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata kodu "forbidden" olmalıdır
    And isteğin kalıcı bir değişiklik yapmadığı yönetici gözüyle doğrulanmalıdır

    @manager
    Examples: Manager hesap rolü
      | hesap         | method | endpoint                                                               | gövde                                                                  |
      | manager@local | GET    | /v1/users                                                              |                                                                        |
      | manager@local | POST   | /v1/users                                                              | {"email":"rbac.{UNIQUE}@local","password":"Probe12345","role":"admin"} |
      | manager@local | PUT    | /v1/users/{SELF_ID}                                                    | {"role":"admin"}                                                       |
      | manager@local | DELETE | /v1/users/{RANDOM_ID}                                                  |                                                                        |
      | manager@local | GET    | /v1/users/{SELF_ID}/groups                                             |                                                                        |
      | manager@local | GET    | /v1/groups                                                             |                                                                        |
      | manager@local | POST   | /v1/groups                                                             | {"name":"rbac-{UNIQUE}"}                                               |
      | manager@local | POST   | /v1/groups/44444444-0000-0000-0000-000000000001/members                | {"user_id":"{SELF_ID}"}                                                |
      | manager@local | POST   | /v1/classes                                                            | {"name":"rbac-{UNIQUE}"}                                               |
      | manager@local | GET    | /v1/property-templates                                                 |                                                                        |
      | manager@local | POST   | /v1/rbac/roles                                                         | {"name":"rbac-{UNIQUE}","permission_ids":[]}                           |
      | manager@local | PUT    | /v1/rbac/roles/22222222-0000-0000-0000-000000000004/permissions        | {"permission_ids":[]}                                                  |
      | manager@local | GET    | /v1/admin/audit-logs                                                   |                                                                        |
      | manager@local | GET    | /v1/admin/storage-gc/versions                                          |                                                                        |
      | manager@local | DELETE | /v1/documents/{RANDOM_ID}/hard                                         |                                                                        |
      | manager@local | POST   | /v1/documents/bulk/delete                                              | {"document_ids":["{RANDOM_ID}"]}                                       |
      | manager@local | DELETE | /v1/folders/{RANDOM_ID}                                                |                                                                        |
      | manager@local | PATCH  | /v1/folders/{RANDOM_ID}/owner                                          | {"new_owner_id":"{SELF_ID}"}                                           |
      | manager@local | GET    | /v1/documents/{RANDOM_ID}/effective-access                             |                                                                        |

    @user
    Examples: User hesap rolü
      | hesap      | method | endpoint                                                               | gövde                                                                  |
      | user@local | GET    | /v1/users                                                              |                                                                        |
      | user@local | POST   | /v1/users                                                              | {"email":"rbac.{UNIQUE}@local","password":"Probe12345","role":"admin"} |
      | user@local | PUT    | /v1/users/{SELF_ID}                                                    | {"role":"admin"}                                                       |
      | user@local | DELETE | /v1/users/{RANDOM_ID}                                                  |                                                                        |
      | user@local | GET    | /v1/users/{SELF_ID}/groups                                             |                                                                        |
      | user@local | GET    | /v1/groups                                                             |                                                                        |
      | user@local | POST   | /v1/groups                                                             | {"name":"rbac-{UNIQUE}"}                                               |
      | user@local | POST   | /v1/groups/44444444-0000-0000-0000-000000000001/members                | {"user_id":"{SELF_ID}"}                                                |
      | user@local | POST   | /v1/classes                                                            | {"name":"rbac-{UNIQUE}"}                                               |
      | user@local | GET    | /v1/property-templates                                                 |                                                                        |
      | user@local | POST   | /v1/rbac/roles                                                         | {"name":"rbac-{UNIQUE}","permission_ids":[]}                           |
      | user@local | PUT    | /v1/rbac/roles/22222222-0000-0000-0000-000000000004/permissions        | {"permission_ids":[]}                                                  |
      | user@local | GET    | /v1/admin/audit-logs                                                   |                                                                        |
      | user@local | GET    | /v1/admin/storage-gc/versions                                          |                                                                        |
      | user@local | DELETE | /v1/documents/{RANDOM_ID}/hard                                         |                                                                        |
      | user@local | POST   | /v1/documents/bulk/delete                                              | {"document_ids":["{RANDOM_ID}"]}                                       |
      | user@local | DELETE | /v1/folders/{RANDOM_ID}                                                |                                                                        |
      | user@local | PATCH  | /v1/folders/{RANDOM_ID}/owner                                          | {"new_owner_id":"{SELF_ID}"}                                           |
      | user@local | GET    | /v1/documents/{RANDOM_ID}/effective-access                             |                                                                        |

  # ---------------------------------------------------------------------
  # Batch 22 — Users, groups, RBAC, custom roles, manager scope
  # ---------------------------------------------------------------------

  # ECM kaynağı: src/engine/rbac/mod.rs create_role / update_role_permissions; acl/service.rs rol izinleri.
  # Belgeyi admin DEĞİL normal kullanıcı yazar: aksi halde ret admin-authored korumasından gelirdi.
  @batch22 @RBAC-02 @critical
  Scenario: RBAC-02 Yalnız okuma izinli özel rol yazmaya izin vermemeli, izin eklenince aynı oturumda yazma açılmalıdır
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "özel rol klasörü" oluşturulmuştur
    And "özel rol klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "özel rol klasörü" klasörüne "rol belgesi" belgesini yüklemiştir
    And yönetici "document:read,folder:read" izinlerine sahip "salt okur" özel rolünü oluşturmuştur
    And "özel rol klasörü" klasöründe "okur" kullanıcısına "salt okur" özel rolü verilmiştir
    And "rol belgesi" belgesinin mevcut durumu kaydedilmiştir
    Then "okur" "rol belgesi" belgesini okuyup içeriğini indirebilmelidir
    And "okur" "rol belgesi" belgesinde şu işlemlerin her biri 403 ile reddedilmelidir
      | yeniden adlandırma |
      | etiket ekleme      |
      | yeni sürüm         |
      | soft delete        |
    And "rol belgesi" belgesi kaydedilen durumundan değişmemiş olmalıdır
    # Klasör düzeyindeki izin klasör kapsamlı fiillerle değerlendirilir (service.rs: yalnız delete
    # document:* üzerinden kalıtılır); içerik yazma için folder:write gerekir.
    When yönetici "salt okur" rolünün izinlerini "document:read,folder:read,document:write,folder:write" olarak günceller
    And "okur" "rol belgesi" belgesinde "yeni sürüm" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "rol belgesi" belgesinin güncel sürümü değişmiş olmalıdır

  # ECM kaynağı: src/engine/rbac/mod.rs update_role_permissions (is_system_role -> Validation)
  @batch22 @RBAC-03
  Scenario: RBAC-03 Sistem rollerinin izinleri yönetici tarafından da değiştirilemez
    Given yönetici API oturumu açmıştır
    When yönetici "viewer" sistem rolünün izinlerini boşaltmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "System roles are read-only and cannot be modified." içermelidir
    And "viewer" sistem rolünün izinleri değişmemiş olmalıdır

  # ECM kaynağı: src/engine/acl/runtime.rs can_manage_document_scoped — ekip grubunun klasör zincirinde
  # belgeden önce verilmiş izni olmalı VE belgeyi yaratan kullanıcı da o ekipte olmalı; admin belgesi asla.
  @batch22 @RBAC-04 @critical
  Scenario: RBAC-04 Manager yalnız ekibinin erişimli olduğu klasörde ekip üyesinin belgesini yönetebilir
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip dışı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip üyesi" API ile giriş yapmıştır
    And "ekip dışı" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yönetici "ekip üyesi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    # Ekip editor: document:delete taşımaz; manager'ın silme/yönetim hakkı yalnız ekip kapsamından gelebilir.
    And "ekip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And "ekip klasörü" klasöründe "ekip üyesi" kullanıcısına "manager" rolü verilmiştir
    And "ekip klasörü" klasöründe "ekip dışı" kullanıcısına "manager" rolü verilmiştir
    And "ekip üyesi" "ekip klasörü" klasörüne "ekip belgesi" belgesini yüklemiştir
    And yönetici "ekip klasörü" klasörüne "yönetici belgesi" belgesini yüklemiştir
    And "ekip dışı" "ekip klasörü" klasörüne "dış belge" belgesini yüklemiştir
    And "yönetici belgesi" belgesinin mevcut durumu kaydedilmiştir
    And "dış belge" belgesinin mevcut durumu kaydedilmiştir
    And "ekip belgesi" belgesinin mevcut durumu kaydedilmiştir
    Then "ekip yöneticisi" "yönetici belgesi" belgesinde şu işlemlerin her biri 403 ile reddedilmelidir
      | statü under_review |
      | yeniden adlandırma |
      | soft delete        |
    And "ekip yöneticisi" "dış belge" belgesinde şu işlemlerin her biri 403 ile reddedilmelidir
      | statü under_review |
      | yeniden adlandırma |
      | soft delete        |
    And "yönetici belgesi" belgesi kaydedilen durumundan değişmemiş olmalıdır
    And "dış belge" belgesi kaydedilen durumundan değişmemiş olmalıdır
    And "ekip yöneticisi" "ekip belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | statü under_review |
      | yeniden adlandırma |
    And "ekip belgesi" belgesinin durumu "under_review" olmalıdır
    And "ekip belgesi" belgesinin başlığı yeniden adlandırılmış olmalıdır
    When "ekip yöneticisi" "ekip belgesi" belgesinde "soft delete" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "ekip belgesi" belgesi soft delete edilmiş olmalıdır

  # ECM kaynağı: runtime.rs team_groups_in_chain_since — belgeden SONRA verilen ekip izni geriye işlemez
  @batch22 @RBAC-05 @critical
  Scenario: RBAC-05 Ekip grubuna belge oluşturulduktan sonra verilen yetki manager'a o belgede yönetim vermez
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip üyesi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yönetici "ekip üyesi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    And "ekip klasörü" klasöründe "ekip üyesi" kullanıcısına "manager" rolü verilmiştir
    And "ekip üyesi" "ekip klasörü" klasörüne "eski belge" belgesini yüklemiştir
    When "ekip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And "ekip yöneticisi" "eski belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır
    When "ekip yöneticisi" "eski belge" belgesinde "statü under_review" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "eski belge" belgesinin durumu "draft" olmalıdır
    When "ekip üyesi" "ekip klasörü" klasörüne "yeni belge" belgesini yüklemiştir
    And "ekip yöneticisi" "yeni belge" belgesinde "statü under_review" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "yeni belge" belgesinin durumu "under_review" olmalıdır

  # ECM kaynağı: src/api/rest/folders.rs require_manage_folder; folders/write.rs patch_folder (rename = yönetim)
  @batch22 @RBAC-09
  Scenario: RBAC-09 Manager ekip kapsamı dışındaki klasörün durumunu, adını ve yetkilerini değiştiremez
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "kapsam dışı klasör" oluşturulmuştur
    And "kapsam dışı klasör" klasöründe "ekip yöneticisi" kullanıcısına "manager" rolü verilmiştir
    And "kapsam dışı klasör" klasörünün mevcut durumu kaydedilmiştir
    Then "ekip yöneticisi" "kapsam dışı klasör" klasöründe şu yönetim işlemlerinin her biri 403 ile reddedilmelidir
      | pasifleştirme      |
      | yeniden adlandırma |
      | yetki verme        |
    And "kapsam dışı klasör" klasörü kaydedilen durumundan değişmemiş olmalıdır
    When "kapsam dışı klasör" klasöründe "ekip" grubuna "viewer" rolü verilmiştir
    Then "ekip yöneticisi" "kapsam dışı klasör" klasörünü yeniden adlandırabilmelidir
