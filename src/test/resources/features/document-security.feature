@coverage-gap @documents @security
Feature: Admin tarafından oluşturulan belgelerin korunması

  # ECM kaynağı:
  #  - src/engine/acl/runtime.rs:353-360 can_manage_document_scoped: admin belgesi hiçbir
  #    admin olmayan için yönetilebilir değildir (rename/status/delete/restore)
  #  - src/engine/acl/runtime.rs:461-470, :845-852 assert_caller_may_modify: yazma
  #    işlemleri (etiket, taşıma, yeni versiyon) "created by an administrator" ile reddedilir
  #  - src/engine/documents/save.rs:1405-1415 rename kuralı
  #  - ROLES.md "Admin-authored documents stay read-only to a non-admin owner"
  @DOC-01 @critical
  Scenario: DOC-01 Admin belgesi, klasör sahibi normal kullanıcı için salt okunur olmalıdır
    Given yönetici API oturumu açmıştır
    And "sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "sahip" API ile giriş yapmıştır
    And yetki devralması kapalı "çalışma alanı" oluşturulmuştur
    And "çalışma alanı" klasöründe "sahip" kullanıcısına "manager" rolü verilmiştir
    And "sahip" "çalışma alanı" altında "sahip klasörü" alt klasörünü oluşturmuştur
    And yönetici "sahip klasörü" klasörüne "admin belgesi" belgesini yüklemiştir
    And "sahip" "sahip klasörü" klasörüne "kendi belgesi" belgesini yüklemiştir
    Then "sahip" "admin belgesi" belgesini görüntüleyebilmelidir
    And "sahip" için "admin belgesi" belgesi admin tarafından oluşturulmuş ve yönetilemez görünmelidir
    When "sahip" "admin belgesi" belgesine "qa-etiket" etiketini eklemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "created by an administrator" içermelidir
    When "sahip" "admin belgesi" belgesini yeniden adlandırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "sahip" "admin belgesi" belgesinin durumunu "under_review" yapmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "admin belgesi" belgesinin durumu "draft" olmalıdır
    When "sahip" "kendi belgesi" belgesini yeniden adlandırmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır

  # ---------------------------------------------------------------------
  # Batch 24 — Document security (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: engine/documents move (hedefte Write + etkin klasör gerekir)
  @batch24 @DOC-11 @critical
  Scenario: DOC-11 Hedef klasörde yazma yetkisi yoksa ya da hedef pasifse belge taşınamaz ve yerinde kalır
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kaynak klasör" oluşturulmuştur
    And yetki devralması kapalı "okunur klasör" oluşturulmuştur
    And yetki devralması kapalı "pasif hedef" oluşturulmuştur
    And "kaynak klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "okunur klasör" klasöründe "yazar" kullanıcısına "viewer" rolü verilmiştir
    And "pasif hedef" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "kaynak klasör" klasörüne "taşınacak belge" belgesini yüklemiştir
    And yönetici "pasif hedef" klasörünü pasifleştirir
    When "yazar" "taşınacak belge" belgesini "okunur klasör" klasörüne taşımayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "You do not have permission to upload to this folder." içermelidir
    When "yazar" "taşınacak belge" belgesini "pasif hedef" klasörüne taşımayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata kodu "folder_inactive" olmalıdır
    And "taşınacak belge" belgesi hâlâ "kaynak klasör" klasöründe olmalıdır

  # ECM kaynağı: engine/documents/save.rs (hedef klasörde Write + etkin klasör)
  @batch24 @DOC-03
  Scenario: DOC-03 Yazma yetkisi olmayan klasöre ve pasif klasöre belge yüklenemez
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "okunur klasör" oluşturulmuştur
    And yetki devralması kapalı "pasif klasör" oluşturulmuştur
    And "okunur klasör" klasöründe "yazar" kullanıcısına "viewer" rolü verilmiştir
    And "pasif klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And yönetici "pasif klasör" klasörünü pasifleştirir
    When "yazar" "okunur klasör" klasörüne "okunur deneme" başlıklı belge yüklemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "You do not have permission to upload to this folder." içermelidir
    And "okunur deneme" başlığıyla hiçbir belge oluşmamış olmalıdır
    When "yazar" "pasif klasör" klasörüne "pasif deneme" başlıklı belge yüklemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata kodu "folder_inactive" olmalıdır
    And "pasif deneme" başlığıyla hiçbir belge oluşmamış olmalıdır

  # ECM kaynağı: documents.rs soft_delete_document -> can_delete_document (editor: document:delete yok)
  @batch24 @DOC-09
  Scenario: DOC-09 Klasörde editor olan ama sahip, kapsam ya da silme yetkisi olmayan kullanıcı başkasının belgesini silemez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "düzenleyici" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "düzenleyici" API ile giriş yapmıştır
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "sahip klasörü" klasöründe "düzenleyici" kullanıcısına "editor" rolü verilmiştir
    And "klasör sahibi" "sahip klasörü" klasörüne "sahip belgesi" belgesini yüklemiştir
    When "düzenleyici" "sahip belgesi" belgesinde "soft delete" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "sahip belgesi" belgesinin silinme durumu "false" olmalıdır

  # ECM kaynağı: engine/documents/copy.rs (hedefte Write; kaynak admin belgesiyse assert_caller_may_modify)
  @batch24 @DOC-12
  Scenario: DOC-12 Hedefte yazma yetkisi olmadan kopyalanamaz, admin belgesi admin olmayan tarafından kopyalanamaz
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "yazılabilir klasör" oluşturulmuştur
    And yetki devralması kapalı "okunur klasör" oluşturulmuştur
    And "yazılabilir klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "okunur klasör" klasöründe "yazar" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "yazılabilir klasör" klasörüne "kendi belgesi" belgesini yüklemiştir
    And yönetici "yazılabilir klasör" klasörüne "admin belgesi" belgesini yüklemiştir
    When "yazar" "kendi belgesi" belgesini "okunur klasör" klasörüne kopyalamayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "kendi belgesi" belgesinin kopyası oluşmamış olmalıdır
    When "yazar" "admin belgesi" belgesini "yazılabilir klasör" klasörüne kopyalamayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "created by an administrator" içermelidir
    And "admin belgesi" belgesinin kopyası oluşmamış olmalıdır
