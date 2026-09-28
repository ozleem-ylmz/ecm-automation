@coverage-gap @folders-api
Feature: Klasör yaşam döngüsü ve bütünlüğü (API + arayüz)

  # ---------------------------------------------------------------------
  # Batch 23 — Folder lifecycle & integrity (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: folders/write.rs hard_delete_folder; FolderDetail.tsx Delete butonu koşulu
  @batch23 @FLD-01 @critical
  Scenario: FLD-01 Alt klasörü ya da belgesi olan klasör silinemez, boş klasör silinir, kök klasör silinemez
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "alt klasörlü" oluşturulmuştur
    And "alt klasörlü" altında yetki devralması açık "iç klasör" oluşturulmuştur
    And yetki devralması kapalı "belgeli klasör" oluşturulmuştur
    And yönetici "belgeli klasör" klasörüne "klasör belgesi" belgesini yüklemiştir
    And yetki devralması kapalı "boş klasör" oluşturulmuştur
    Then yönetici tarayıcıda "alt klasörlü" klasörünü açtığında Delete butonu görünmemelidir
    And yönetici tarayıcıda "boş klasör" klasörünü açtığında Delete butonu görünmelidir
    When yönetici "alt klasörlü" klasörünü silmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Cannot delete a folder that still has subfolders." içermelidir
    And "alt klasörlü" klasörü hâlâ bulunmalıdır
    When yönetici "belgeli klasör" klasörünü silmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Cannot delete a folder that still has documents." içermelidir
    And "belgeli klasör" klasörü hâlâ bulunmalıdır
    When yönetici "boş klasör" klasörünü silmeyi dener
    Then son API yanıtının durum kodu 204 olmalıdır
    And "boş klasör" klasörü artık bulunmamalıdır
    When yönetici kök klasörü silmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The system root folder cannot be deleted." içermelidir

  # ECM kaynağı: runtime.rs assert_document_access (etkin olmayan klasör -> FolderInactive, admin hariç)
  @batch23 @FLD-03
  Scenario: FLD-03 Pasif klasördeki belgeye admin olmayan erişemez, admin erişir, yeniden aktifleştirince erişim döner
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "pasifleşen klasör" oluşturulmuştur
    And "pasifleşen klasör" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "pasifleşen klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And yönetici "pasifleşen klasör" klasörüne "pasif klasör belgesi" belgesini yüklemiştir
    And "okur" "pasif klasör belgesi" belgesini okuyup içeriğini indirebilmelidir
    When yönetici "pasifleşen klasör" klasörünü pasifleştirir
    And "okur" "pasif klasör belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata kodu "folder_inactive" olmalıdır
    When "yönetici" "pasif klasör belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır
    When "yazar" "pasifleşen klasör" klasörüne belge yüklemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata kodu "folder_inactive" olmalıdır
    When yönetici "pasifleşen klasör" klasörünü aktifleştirir
    Then "okur" "pasif klasör belgesi" belgesini okuyup içeriğini indirebilmelidir

  # ECM kaynağı: folders/write.rs (kök için inactivate / inheritance / owner korumaları; admin dışı rename).
  # Admin'in kökü yeniden adlandırması motor tarafından izinli ama domain kuralı reddeder: RQ-17 (koşturulmaz).
  @batch23 @FLD-04
  Scenario: FLD-04 Kök klasör pasifleştirilemez, kalıtımı ve sahipliği değiştirilemez, admin olmayan tarafından yeniden adlandırılamaz
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    When yönetici kök klasörde "pasifleştirme" işlemini dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The system root folder cannot be inactivated." içermelidir
    When yönetici kök klasörde "kalıtım değiştirme" işlemini dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Permission inheritance cannot be changed on the system root folder." içermelidir
    When yönetici kök klasörde "sahiplik devri" işlemini dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The system root folder has no owner and cannot be transferred." içermelidir
    When "test kullanıcısı" kök klasörü yeniden adlandırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And kök klasör "Root" adıyla aktif ve kalıtımı açık kalmalıdır

  # ECM kaynağı: folders/read.rs list_folder_children / list_folder_documents (ACL filtreli)
  @batch23 @FLD-05
  Scenario: FLD-05 Klasör alt öğe ve belge listeleri yalnız kullanıcının erişebildiği öğeleri döndürür
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "üst klasör" oluşturulmuştur
    And "üst klasör" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "üst klasör" klasörüne "görünen belge" belgesini yüklemiştir
    And yönetici "izole alt klasör" alt klasörünü "üst klasör" altında kalıtım kapalı oluşturmuştur
    And yönetici "izole alt klasör" klasörüne "gizli belge" belgesini yüklemiştir
    Then "okur" "üst klasör" klasörünün alt klasörlerinde "izole alt klasör" klasörünü görmemelidir
    And "okur" "üst klasör" klasörünün belge listesinde "görünen belge" belgesini görmelidir
    And "okur" belge aramasında "gizli belge" belgesini görmemelidir
    When "okur" "gizli belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır

  # ECM kaynağı: api/rest/folders.rs list_folders_picker (?for=upload -> yalnız yazma ACL'i).
  # Yanıt süresi BUG-03 kapsamında ayrıca kanıt olarak eklenir (SLA kararı RQ-6 bekliyor).
  @batch23 @FLD-08
  Scenario: FLD-08 Yükleme klasör seçicisi normal kullanıcıya yalnız yazabildiği klasörleri listeler
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And yetki devralması kapalı "yazılabilir klasör" oluşturulmuştur
    And yetki devralması kapalı "salt okunur klasör" oluşturulmuştur
    And "yazılabilir klasör" klasöründe "test kullanıcısı" kullanıcısına "manager" rolü verilmiştir
    And "salt okunur klasör" klasöründe "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    Then "test kullanıcısı" yükleme klasör seçicisinde "yazılabilir klasör" klasörünü görmelidir
    And "test kullanıcısı" yükleme klasör seçicisinde "salt okunur klasör" klasörünü görmemelidir

  # ECM kaynağı: postgres_repository/folders.rs count_folder_documents (yalnız is_deleted=FALSE sayılır) +
  # runtime.rs can_manage_document_scoped ("Unfiled documents ... only an admin can manage them").
  # Beklenen: klasör silme engellenir YA DA belgeler geçerli bir klasörde korunur (RQ-14); her iki durumda da
  # geri yüklenen belge klasörsüz kalmamalı ve yetkili kullanıcı erişimini kaybetmemelidir.
  # REQUIREMENT REVIEW (RQ-14, 2026-09-27 yeniden doğrulama): kaynak kod (hard_delete_folder "active document",
  # hata metni "Move or delete them first.", desteklenen "unfiled" belgeler) davranışın kasıtlı olduğunu gösteriyor;
  # kullanıcı kılavuzu 3.7 "tamamen boş" diyor. Beklenti ÖNERİLEN kuraldır (eski sınıflandırma: BUG-09 CONFIRMED).
  @batch23 @FLD-10 @critical @requirement-review @RQ-14
  Scenario: FLD-10 Yalnız soft delete edilmiş belgesi olan klasör silindikten sonra geri yüklenen belge klasörsüz kalmamalıdır
    Given yönetici API oturumu açmıştır
    And "klasör okuru" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "klasör okuru" API ile giriş yapmıştır
    And yetki devralması kapalı "boş görünen klasör" oluşturulmuştur
    And "boş görünen klasör" klasöründe "klasör okuru" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "boş görünen klasör" klasörüne "çöpteki belge" belgesini yüklemiştir
    And "klasör okuru" "çöpteki belge" belgesini okuyup içeriğini indirebilmelidir
    And "çöpteki belge" belgesi API ile soft delete edilmiştir
    When yönetici "boş görünen klasör" klasörünü silmeyi dener
    And yönetici "çöpteki belge" belgesini geri yükler
    Then "çöpteki belge" belgesi geçerli bir klasörde bulunmalıdır
    And "klasör okuru" "çöpteki belge" belgesini okuyup içeriğini indirebilmelidir
