@coverage-gap @custom-fields
Feature: Sınıf özel alanları

  # ---------------------------------------------------------------------
  # Batch 27 — Custom fields (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: engine/custom_fields.rs (tip kontrolü, bilinmeyen alan, sınıfsız belge) + DocumentCustomFieldsCard.tsx
  @batch27 @CF-01
  Scenario: CF-01 Sınıflı belgenin özel alanları tip kuralıyla kaydedilir ve yenilemeden sonra korunur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "alan klasörü" oluşturulmuştur
    And yönetici "ref:text:opsiyonel" özellikleriyle "alanlı sınıf" sınıfını oluşturmuştur
    And yönetici "alanlı sınıf" sınıfına "oncelik" adlı "numeric" türünde "zorunlu" özel alan eklemiştir
    And "yönetici" "alanlı sınıf" sınıfıyla "alan klasörü" klasörüne "{}" metadatasıyla "alanlı belge" belgesini yüklemiştir
    And yönetici "alan klasörü" klasörüne "sınıfsız belge" belgesini yüklemiştir
    When yönetici "alanlı belge" belgesinin özel alanlarını "{\"oncelik\": \"abc\"}" olarak kaydetmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Custom field 'oncelik' must be a number." içermelidir
    When yönetici arayüzde "alanlı belge" belgesinin özel alanlar kartında "oncelik" alanına "3" girip kaydeder
    Then "alanlı belge" belgesinin "oncelik" özel alan değeri "3" olmalıdır
    And yönetici sayfayı yenilediğinde özel alanlar kartında "3" değerini görmelidir
    When yönetici "alanlı belge" belgesinin özel alanlarını "{\"yok\": \"1\"}" olarak kaydetmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Unknown custom field 'yok'." içermelidir
    When yönetici "sınıfsız belge" belgesinin özel alanlarını "{\"oncelik\": \"1\"}" olarak kaydetmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This document has no class, so it has no custom fields." içermelidir

  # ECM kaynağı: engine/custom_fields.rs (ad tekilliği, değerli alan silinemez)
  @batch27 @CF-02
  Scenario: CF-02 Özel alan adı tekildir, değeri olan özel alan silinemez, değeri olmayan silinir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "alan klasörü" oluşturulmuştur
    And yönetici "" özellikleriyle "alanlı sınıf" sınıfını oluşturmuştur
    And yönetici "alanlı sınıf" sınıfına "oncelik" adlı "numeric" türünde "opsiyonel" özel alan eklemiştir
    And yönetici "alanlı sınıf" sınıfına "notlar" adlı "text" türünde "opsiyonel" özel alan eklemiştir
    And "yönetici" "alanlı sınıf" sınıfıyla "alan klasörü" klasörüne "{}" metadatasıyla "alanlı belge" belgesini yüklemiştir
    And yönetici "alanlı belge" belgesinin özel alanlarını "{\"oncelik\": \"5\"}" olarak kaydetmeyi dener
    When yönetici "alanlı sınıf" sınıfına aynı "oncelik" adıyla özel alan eklemeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "A custom field with this name already exists on this class." içermelidir
    When yönetici "alanlı sınıf" sınıfındaki "oncelik" özel alanını silmeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "has 1 stored value(s) and cannot be deleted" içermelidir
    When yönetici "alanlı sınıf" sınıfındaki "notlar" özel alanının adını "oncelik" yapmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    When yönetici "alanlı sınıf" sınıfındaki "notlar" özel alanını silmeyi dener
    Then son API yanıtı başarılı olmalıdır
    And "alanlı sınıf" sınıfının özel alanları tam olarak "oncelik" olmalıdır
    And "alanlı belge" belgesinin "oncelik" özel alan değeri "5" olmalıdır
