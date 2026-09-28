@coverage-gap @folders @ownership
Feature: Klasör sahipliği (Linux benzeri)

  # ECM kaynağı:
  #  - src/engine/folders/write.rs:74-95 alt klasör oluşturma + kökte yalnız admin
  #  - src/engine/folders/write.rs:365-380 yeniden adlandırma: sahip / manager / admin
  #  - ROLES.md "Folder ownership"
  @OWN-01 @critical
  Scenario: OWN-01 Yazma yetkili normal kullanıcı alt klasörün sahibi olur ancak kök klasör oluşturamaz
    Given yönetici API oturumu açmıştır
    And "katkıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "katkıcı" API ile giriş yapmıştır
    And yetki devralması kapalı "proje klasörü" oluşturulmuştur
    And "proje klasörü" klasöründe "katkıcı" kullanıcısına "manager" rolü verilmiştir
    When "katkıcı" "proje klasörü" altında "katkıcı klasörü" alt klasörünü oluşturur
    Then son API yanıtının durum kodu 200 olmalıdır
    And "katkıcı" "katkıcı klasörü" klasörünü yeniden adlandırabilmelidir
    And "katkıcı" sahibi olmadığı "proje klasörü" klasörünü yeniden adlandıramamalıdır
    When "katkıcı" sistem kökü altında klasör oluşturmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "Only an administrator can create a top-level folder." içermelidir
    And reddedilen kök klasör denemesi sunucuda hiçbir klasör oluşturmamış olmalıdır

  # ---------------------------------------------------------------------
  # Batch 23 — Ownership (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: runtime.rs can_manage_folder_scoped (sahip = folder.created_by, devirle değişir)
  @batch23 @OWN-02
  Scenario: OWN-02 Sahiplik devrinden sonra eski sahip klasörü yönetemez, yeni sahip yönetir
    Given yönetici API oturumu açmıştır
    And "eski sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yeni sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "paylaşılan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "eski sahip" API ile giriş yapmıştır
    And "yeni sahip" API ile giriş yapmıştır
    And yetki devralması kapalı "devir klasörü" oluşturulmuştur
    And yönetici "devir klasörü" klasörünün sahipliğini "eski sahip" kullanıcısına devretmiştir
    And "eski sahip" "devir klasörü" klasörünü yeniden adlandırabilmelidir
    When yönetici "devir klasörü" klasörünün sahipliğini "yeni sahip" kullanıcısına devretmiştir
    And "devir klasörü" klasörünün mevcut durumu kaydedilmiştir
    Then "eski sahip" "devir klasörü" klasöründe şu yönetim işlemlerinin her biri 403 ile reddedilmelidir
      | yeniden adlandırma |
      | yetki verme        |
      | pasifleştirme      |
    And "devir klasörü" klasörü kaydedilen durumundan değişmemiş olmalıdır
    And "yeni sahip" "devir klasörü" klasörünü yeniden adlandırabilmelidir
    And "yeni sahip" "devir klasörü" klasöründe "paylaşılan" kullanıcısına "viewer" rolü verebilmelidir

  # ECM kaynağı: runtime.rs assert_document_access (sahip, klasördeki başka kullanıcının belgesini yönetir).
  # Geri yükleme: ROLES.md / REST yorumu / motor çelişkisi (RQ-18) — sonucu kanıt olarak eklenir.
  @batch23 @OWN-03
  Scenario: OWN-03 Klasör sahibi klasöründeki başka kullanıcının belgesini yeniden adlandırır, statüsünü değiştirir ve soft delete eder
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "sahip klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "sahip klasörü" klasörüne "yazarın belgesi" belgesini yüklemiştir
    And "yazarın belgesi" belgesinin mevcut durumu kaydedilmiştir
    When "klasör sahibi" "yazarın belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
      | statü under_review |
    Then "yazarın belgesi" belgesinin başlığı yeniden adlandırılmış olmalıdır
    And "yazarın belgesi" belgesinin durumu "under_review" olmalıdır
    When "klasör sahibi" "yazarın belgesi" belgesinde "soft delete" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "yazarın belgesi" belgesi soft delete edilmiş olmalıdır
    And "klasör sahibi" kullanıcısının "yazarın belgesi" belgesini geri yükleme sonucu RQ-18 kanıtı olarak kaydedilmelidir

  # ECM kaynağı: api/rest/folders.rs set_folder_active -> require_manage_folder
  @batch23 @OWN-04
  Scenario: OWN-04 Normal kullanıcı sahibi olduğu klasörü pasifleştirip aktifleştirebilir, sahip olmayan yazar yapamaz
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "sahip klasörü" klasöründe "yazar" kullanıcısına "editor" rolü verilmiştir
    When "yazar" "sahip klasörü" klasörünü pasifleştirmeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "sahip klasörü" klasörü aktif olmalıdır
    When "klasör sahibi" "sahip klasörü" klasörünü pasifleştirmeyi dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "sahip klasörü" klasörü pasif olmalıdır
    When "klasör sahibi" "sahip klasörü" klasörünü aktifleştirmeyi dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "sahip klasörü" klasörü aktif olmalıdır

  # ECM kaynağı: folders/write.rs transfer ownership ("The new owner is not an active user.")
  @batch23 @USER-05
  Scenario: USER-05 Klasör sahibi pasifleştirilince klasör erişilebilir kalır, sahiplik pasif kullanıcıya devredilemez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yeni sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And "yeni sahip" API ile giriş yapmıştır
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "klasör sahibi" "sahip klasörü" klasörüne "sahip belgesi" belgesini yüklemiştir
    When yönetici "klasör sahibi" hesabını pasifleştirir
    Then yönetici "sahip klasörü" klasörünü ve "sahip belgesi" belgesini okuyabilmelidir
    When yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The new owner is not an active user." içermelidir
    When yönetici "sahip klasörü" klasörünün sahipliğini "yeni sahip" kullanıcısına devretmiştir
    Then "yeni sahip" "sahip klasörü" klasörünü yeniden adlandırabilmelidir
    And "yeni sahip" "sahip belgesi" belgesinde "okuma" işlemini dener
    And son API yanıtının durum kodu 200 olmalıdır

  # ECM kaynağı: folders/write.rs patch_folder (kalıtım değişikliği yalnız admin)
  @batch23 @FLD-09
  Scenario: FLD-09 İzin kalıtımını yalnız admin değiştirebilir, klasör sahibi değiştiremez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör sahibi" API ile giriş yapmıştır
    And yetki devralması açık "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    When "klasör sahibi" "sahip klasörü" klasörünün yetki devralmasını kapatmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "Administrator privileges are required to change folder permission inheritance." içermelidir
    And "sahip klasörü" klasörünün yetki devralması açık kalmalıdır
