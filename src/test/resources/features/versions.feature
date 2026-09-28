@coverage-gap @versions
Feature: Belge sürümleri, geri alma ve sürüm etiketleri

  # ---------------------------------------------------------------------
  # Batch 25 — Versions (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: rollback -> yeni güncel sürüm (geçmiş korunur); DocumentVersionDetail.tsx "Restore this version"
  @batch25 @VER-01 @critical
  Scenario: VER-01 Eski sürüme geri alma yeni bir güncel sürüm oluşturur, eski içeriği geri getirir ve geçmiş korunur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And yönetici "sürüm klasörü" klasörüne "sürümlü belge" belgesini yüklemiştir
    And "yönetici" "sürümlü belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "sürümlü belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    When yönetici arayüzde "sürümlü belge" belgesinin "ilk sürüm" sürüm sayfasından geri alma yapar
    Then "sürümlü belge" belgesinde 3 sürüm olmalı ve yeni güncel sürüm "ilk içerik:sürümlü belge" içeriğini taşımalıdır
    And "yönetici" "ikinci sürüm" sürümünün içeriğini indirdiğinde "QA içerik B" almalıdır
    And sayfa yenilendiğinde "sürümlü belge" belgesinin güncel sürümü aynı kalmalı ve arayüzde gösterilmelidir

  # ECM kaynağı: GET /documents/versions/{id}/content (sürüm baytları) + assert_version_access
  @batch25 @VER-02 @critical
  Scenario: VER-02 Eski sürüm indirildiğinde o sürümün baytları döner, başka belgenin sürüm kimliği kullanılamaz
    Given yönetici API oturumu açmıştır
    And "yetkisiz" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yetkisiz" API ile giriş yapmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And yönetici "sürüm klasörü" klasörüne "sürümlü belge" belgesini yüklemiştir
    And yönetici "sürüm klasörü" klasörüne "başka belge" belgesini yüklemiştir
    And "yönetici" "sürümlü belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "sürümlü belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    Then "yönetici" "ilk sürüm" sürümünün içeriğini indirdiğinde "ilk içerik:sürümlü belge" almalıdır
    And "yönetici" "ikinci sürüm" sürümünün içeriğini indirdiğinde "QA içerik B" almalıdır
    When "yönetici" "başka belge" belgesinin "ilk sürüm" sürüm detayını istemeyi dener
    Then son API yanıtının durum kodu 404 olmalıdır
    When "yetkisiz" "ilk sürüm" sürümünün içeriğini indirmeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır

  # ECM kaynağı: runtime.rs assert_version_access — sürümün KENDİ sınıfı değerlendirilir (sınıf sürüm başına).
  # Plan RQ-1'e bağlamıştı; farklı sınıflı yeni sürüm yolu reclassify kararından bağımsızdır.
  @batch25 @VER-03 @critical
  Scenario: VER-03 Daha kısıtlı sınıftaki eski sürüm yalnız güncel sınıfa erişimi olan kullanıcıya verilmez
    Given yönetici API oturumu açmıştır
    And "güncel sınıf okuru" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "eski sınıf okuru" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "güncel sınıf okuru" API ile giriş yapmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And "sınıf klasörü" klasöründe "güncel sınıf okuru" kullanıcısına "viewer" rolü verilmiştir
    And yönetici erişimi "eski sınıf okuru" kullanıcısıyla sınırlı "kısıtlı sınıf" sınıfını oluşturmuştur
    And yönetici erişimi "güncel sınıf okuru" kullanıcısıyla sınırlı "açık sınıf" sınıfını da oluşturmuştur
    And yönetici "kısıtlı sınıf" sınıfıyla "sınıf klasörü" klasörüne "sınıf değiştiren belge" belgesini yüklemiştir
    And yönetici "sınıf değiştiren belge" belgesine "açık sınıf" sınıfıyla yeni bir sürüm eklemiştir
    And "sınıf değiştiren belge" belgesinin sürümleri "kısıtlı sürüm" ve "açık sürüm" olarak adlandırılmıştır
    Then "güncel sınıf okuru" "açık sürüm" sürümünün içeriğini indirdiğinde "QA açık sınıf sürümü" almalıdır
    When "güncel sınıf okuru" "kısıtlı sürüm" sürümünün içeriğini indirmeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "güncel sınıf okuru" "sınıf değiştiren belge" belgesinin "kısıtlı sürüm" sürüm detayını istemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "güncel sınıf okuru" "sınıf değiştiren belge" belgesinin sürüm listesinde "kısıtlı sürüm" sürümünü görmemelidir

  # ECM kaynağı: rollback doğrulamaları ("That version is already the current version."), kilit 409, viewer 403
  @batch25 @VER-04
  Scenario: VER-04 Güncel sürüme geri alma reddedilir, başka kullanıcının kilidi varken 409, viewer geri alamaz
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilitleyen" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And "kilitleyen" API ile giriş yapmıştır
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And "sürüm klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "sürüm klasörü" klasöründe "kilitleyen" kullanıcısına "manager" rolü verilmiştir
    And "sürüm klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "sürüm klasörü" klasörüne "sürümlü belge" belgesini yüklemiştir
    And "yazar" "sürümlü belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "sürümlü belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    When "yönetici" "sürümlü belge" belgesini "ikinci sürüm" sürümüne geri almayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "That version is already the current version." içermelidir
    And "sürümlü belge" belgesinin sürüm sayısı değişmemiş olmalıdır
    When "kilitleyen" "sürümlü belge" belgesinde "checkout" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    When "yönetici" "sürümlü belge" belgesini "ilk sürüm" sürümüne geri almayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And "sürümlü belge" belgesinin sürüm sayısı değişmemiş olmalıdır
    When "kilitleyen" "sürümlü belge" belgesinde "checkin" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    When "okur" "sürümlü belge" belgesini "ilk sürüm" sürümüne geri almayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "sürümlü belge" belgesinin sürüm sayısı değişmemiş olmalıdır

  # ECM kaynağı: DocumentVersionDetail.tsx (canWrite && !isCurrent -> Restore; Current rozeti; Download).
  # Sürüm detay sayfası yükleyeni göstermez; yükleyen belge detayındaki sürüm geçmişinde doğrulanır.
  @batch25 @VER-05
  Scenario: VER-05 Sürüm detay sayfası sürüm numarası ve dosya adını gösterir, güncel sürümde ve viewer için geri alma yoktur
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And "sürüm klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "sürüm klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "sürüm klasörü" klasörüne "sürümlü belge" belgesini yüklemiştir
    And "yazar" "sürümlü belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "sürümlü belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    Then "yönetici" arayüzde "sürümlü belge" belgesinin "ilk sürüm" sürüm sayfasında sürüm numarasını ve dosya adını görmelidir
    And sürüm sayfasında Restore this version butonu görünmelidir
    And sürüm sayfasında Download butonu ve Current rozeti "görünmez" olmalıdır
    And yönetici belge detayındaki sürüm geçmişinde "ilk sürüm" sürümünü yükleyen olarak "yazar" e-postasını görmelidir
    Then "yönetici" arayüzde "sürümlü belge" belgesinin "ikinci sürüm" sürüm sayfasında sürüm numarasını ve dosya adını görmelidir
    And sürüm sayfasında Restore this version butonu görünmemelidir
    And sürüm sayfasında Download butonu ve Current rozeti "görünür" olmalıdır
    Then "okur" arayüzde "sürümlü belge" belgesinin "ilk sürüm" sürüm sayfasında sürüm numarasını ve dosya adını görmelidir
    And sürüm sayfasında Restore this version butonu görünmemelidir
    And sürüm sayfasında Download butonu ve Current rozeti "görünmez" olmalıdır

  # ECM kaynağı: engine/documents/labels.rs (Write gerekir; kilit başka kullanıcıdaysa 409)
  @batch25 @VER-07
  Scenario: VER-07 Viewer sürüm etiketi ekleyemez ve kaldıramaz, kilit başka kullanıcıdayken etiket eklenemez
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilitleyen" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And "kilitleyen" API ile giriş yapmıştır
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And "sürüm klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "sürüm klasörü" klasöründe "kilitleyen" kullanıcısına "manager" rolü verilmiştir
    And "sürüm klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "sürüm klasörü" klasörüne "etiketli belge" belgesini yüklemiştir
    And "yazar" "etiketli belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "etiketli belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    And yönetici "ilk sürüm" sürümüne "qa-onayli" etiketini eklemiştir
    When "okur" "ilk sürüm" sürümüne "qa-viewer" etiketini eklemeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "okur" "ilk sürüm" sürümünden "qa-onayli" etiketini kaldırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "kilitleyen" "etiketli belge" belgesinde "checkout" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    When "yönetici" "ilk sürüm" sürümüne "qa-kilitli" etiketini eklemeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And "etiketli belge" belgesinin sürüm etiketleri tam olarak "qa-onayli=v1.0.0" olmalıdır
    When "kilitleyen" "etiketli belge" belgesinde "checkin" işlemini dener
    Then son API yanıtı başarılı olmalıdır

  # ECM kaynağı: engine/documents/labels.rs get_document_version_labels
  @batch25 @VER-09
  Scenario: VER-09 Belgenin tüm sürüm etiketleri tek uçtan doğru sürüm eşleşmesiyle listelenir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And yönetici "sürüm klasörü" klasörüne "etiketli belge" belgesini yüklemiştir
    And "yönetici" "etiketli belge" belgesine "QA içerik B" içerikli yeni bir yama sürümü eklemiştir
    And "etiketli belge" belgesinin sürümleri "ilk sürüm" ve "ikinci sürüm" olarak adlandırılmıştır
    And yönetici "ilk sürüm" sürümüne "qa-a" etiketini eklemiştir
    And yönetici "ikinci sürüm" sürümüne "qa-b" etiketini eklemiştir
    Then "etiketli belge" belgesinin toplu sürüm etiketi listesi "qa-a" etiketini "ilk sürüm" sürümüne, "qa-b" etiketini "ikinci sürüm" sürümüne eşlemelidir
    And var olmayan bir belgenin toplu sürüm etiketi listesi 404 dönmelidir
