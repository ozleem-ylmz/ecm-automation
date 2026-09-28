@coverage-gap @bulk
Feature: Toplu belge işlemleri

  # ---------------------------------------------------------------------
  # Batch 30 — Bulk operations (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: bulk_move_documents (öğe bazlı yetki: admin belgesi, okunamayan belge, başkasının kilidi)
  @batch30 @BULK-01 @critical
  Scenario: BULK-01 Toplu taşımada yalnız izinli belgeler taşınır, izinsiz, admin belgesi ve kilitli olanlar öğe bazında hata döner
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilitleyen" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And "kilitleyen" API ile giriş yapmıştır
    And yetki devralması kapalı "kaynak klasör" oluşturulmuştur
    And yetki devralması kapalı "hedef klasör" oluşturulmuştur
    And yetki devralması kapalı "izole klasör" oluşturulmuştur
    And "kaynak klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "hedef klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "kaynak klasör" klasöründe "kilitleyen" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "kaynak klasör" klasörüne "kendi belgesi" belgesini yüklemiştir
    And yönetici "kaynak klasör" klasörüne "admin belgesi" belgesini yüklemiştir
    And yönetici "izole klasör" klasörüne "izole belge" belgesini yüklemiştir
    And "yazar" "kaynak klasör" klasörüne "kilitli belge" belgesini yüklemiştir
    And "kilitleyen" "kilitli belge" belgesini checkout yapmıştır
    When "yazar" "kendi belgesi, admin belgesi, izole belge, kilitli belge" belgelerini "hedef klasör" klasörüne toplu taşır
    Then toplu işlem 207 dönmeli ve "kendi belgesi" başarılı, "admin belgesi, izole belge, kilitli belge" başarısız olmalıdır
    And toplu işlemde "admin belgesi" öğesinin hata kodu "FORBIDDEN" olmalıdır
    And "kendi belgesi" belgeleri "hedef klasör" klasöründe olmalıdır
    And "admin belgesi, kilitli belge" belgeleri "kaynak klasör" klasöründe olmalıdır
    And "izole belge" belgeleri "izole klasör" klasöründe olmalıdır

  # ECM kaynağı: bulk_assign_tag / bulk_remove_tag (öğe bazlı Write kontrolü)
  @batch30 @BULK-02 @critical
  Scenario: BULK-02 Toplu etiketleme ve etiket kaldırma yalnız yazılabilir belgelere uygulanır
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kaynak klasör" oluşturulmuştur
    And yetki devralması kapalı "izole klasör" oluşturulmuştur
    And "kaynak klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "kaynak klasör" klasörüne "kendi belgesi" belgesini yüklemiştir
    And yönetici "kaynak klasör" klasörüne "admin belgesi" belgesini yüklemiştir
    And yönetici "izole klasör" klasörüne "izole belge" belgesini yüklemiştir
    When "yazar" "kendi belgesi, admin belgesi, izole belge" belgelerine toplu etiket ekler
    Then toplu işlem 207 dönmeli ve "kendi belgesi" başarılı, "admin belgesi, izole belge" başarısız olmalıdır
    And toplu etiket yalnız "kendi belgesi" belgelerinde bulunmalıdır, "admin belgesi, izole belge" belgelerinde bulunmamalıdır
    When yönetici "admin belgesi" belgesine toplu etiketi ayrıca ekler
    And "yazar" "kendi belgesi, admin belgesi" belgelerinden toplu etiketi kaldırır
    Then toplu işlem 207 dönmeli ve "kendi belgesi" başarılı, "admin belgesi" başarısız olmalıdır
    And toplu etiket yalnız "admin belgesi" belgelerinde bulunmalıdır, "kendi belgesi" belgelerinde bulunmamalıdır

  # ECM kaynağı: bulk_delete_documents (admin; geçerli / zaten silinmiş / var olmayan)
  @batch30 @BULK-03
  Scenario: BULK-03 Admin toplu silmede geçerli, zaten silinmiş ve var olmayan kimlikler için öğe bazlı sonuç alır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "silme klasörü" oluşturulmuştur
    And yönetici "silme klasörü" klasörüne "canlı belge" belgesini yüklemiştir
    And yönetici "silme klasörü" klasörüne "silinmiş belge" belgesini yüklemiştir
    And "silinmiş belge" belgesi API ile soft delete edilmiştir
    When yönetici "canlı belge, silinmiş belge" belgelerini ve rastgele bir kimliği toplu siler
    Then toplu işlem 207 dönmeli ve "canlı belge" başarılı, "silinmiş belge, rastgele kimlik" başarısız olmalıdır
    And toplu işlemde "silinmiş belge" öğesinin hata kodu "NOT_FOUND" olmalıdır
    And toplu işlemde "rastgele kimlik" öğesinin hata kodu "NOT_FOUND" olmalıdır
    And "canlı belge" belgesinin silinme durumu "true" olmalıdır

  # ECM kaynağı: bulk_* "document_ids must contain 1–100 items" (400; plan 422 bekliyordu)
  @batch30 @BULK-04
  Scenario: BULK-04 Toplu işlem 1 ile 100 öğe arasıyla sınırlıdır
    Given yönetici API oturumu açmıştır
    When yönetici 0 öğeli toplu etiketleme göndermeyi dener
    Then son API yanıtı başarısız olmalıdır
    And son API yanıtının hata mesajı "document_ids must contain 1–100 items" içermelidir
    When yönetici 101 öğeli toplu etiketleme göndermeyi dener
    Then son API yanıtı başarısız olmalıdır
    And son API yanıtının hata mesajı "document_ids must contain 1–100 items" içermelidir
    When yönetici 100 öğeli toplu etiketleme göndermeyi dener
    Then son API yanıtının durum kodu 207 olmalıdır

  # ECM kaynağı: Documents.tsx seçim kutuları + BulkActionsBar.tsx (Add tag -> sonuç özeti "N tagged / M failed")
  @batch30 @BULK-05
  Scenario: BULK-05 Belge listesinde çoklu seçimle toplu etiketleme yapılır ve sonuç özeti API durumuyla uyumludur
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "toplu klasör" oluşturulmuştur
    And "toplu klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "toplu klasör" klasörüne önekli "bir" başlıklı "birinci belge" belgesini yüklemiştir
    And "yazar" "toplu klasör" klasörüne önekli "iki" başlıklı "ikinci belge" belgesini yüklemiştir
    And "yazar" "toplu klasör" klasörüne önekli "üç" başlıklı "kilitli belge" belgesini yüklemiştir
    And "yazar" "kilitli belge" belgesini checkout yapmıştır
    When yönetici arayüzde "birinci belge, ikinci belge, kilitli belge" belgelerini seçip toplu etiket ekler
    Then toplu işlem 207 dönmeli ve "birinci belge, ikinci belge" başarılı, "kilitli belge" başarısız olmalıdır
    And arayüz toplu işlem özetinde "2 tagged" göstermelidir
    And arayüz toplu işlem özetinde "1 failed" göstermelidir
    And toplu etiket yalnız "birinci belge, ikinci belge" belgelerinde bulunmalıdır, "kilitli belge" belgelerinde bulunmamalıdır
