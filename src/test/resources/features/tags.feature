@coverage-gap @tags
Feature: Belge etiketleri

  # ---------------------------------------------------------------------
  # Batch 29 — Tags (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: TagManager.tsx (Enter + Add tags) + etiket araması (tag_names)
  @batch29 @TAG-01
  Scenario: TAG-01 Kullanıcı etiket ekler, çip yenilemeden sonra kalır ve etiket araması belgeyi bulur
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And "yönetici" "etiket klasörü" klasörüne önekli "etiketlenecek" başlıklı "etiketlenecek belge" belgesini yüklemiştir
    When yönetici arayüzde "etiketlenecek belge" belgesine "arayüz" etiketini ekler
    Then yönetici sayfayı yenilediğinde "arayüz" etiket çipini görmelidir
    And "arayüz" etiket aramasında "etiketlenecek belge" belgesi "bulunmalıdır"

  # ECM kaynağı: TagChip.tsx "Remove <ad>" + DELETE /documents/{id}/tags/{tag_id}
  @batch29 @TAG-02
  Scenario: TAG-02 Kullanıcı etiket çipini kaldırır, etiket belgeden ve etiket aramasından kalkar
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And "yönetici" "etiket klasörü" klasörüne önekli "etiketli" başlıklı "etiketli belge" belgesini yüklemiştir
    And "etiketli belge" belgesine "kaldırılacak" arama etiketi eklenmiştir
    When yönetici arayüzde "etiketli belge" belgesindeki "kaldırılacak" etiket çipini kaldırır
    Then yönetici sayfayı yenilediğinde "kaldırılacak" etiket çipini görmemelidir
    And "kaldırılacak" etiket aramasında "etiketli belge" belgesi "bulunmamalıdır"
