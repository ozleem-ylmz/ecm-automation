@batch20
Feature: Silinmiş belge bütünlüğü ve belge ilişkileri (Batch 20)

  # Ürün kuralı (ECM, salt okunur): soft delete edilmiş belge salt okunurdur.
  # ECM bunu etiket (engine/documents/tags.rs document_exists(id, false) -> 404),
  # yeniden adlandırma, durum, taşıma, yeni sürüm ve kopyalamada uygular; ECM
  # ekibinin kendi e2e senaryosu tc146 da "backend intentionally rejects tag
  # mutations for deleted documents" der. Aşağıdaki senaryolar AYNI kuralın
  # henüz test edilmemiş mutasyonlarını (sürüm etiketi, ilişki, checkout) ölçer.
  # İlişki kuralları: engine/relationships/mod.rs create_relationship.

  # ---------------------------------------------------------------------------
  # Silinmiş belge: backend mutasyonları
  # ---------------------------------------------------------------------------

  @integrity @deleted
  Scenario: B20-01 Soft delete edilmiş belgenin sürüm etiketleri eklenemez ve kaldırılamaz
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "belge" oluşturmuştur
    And "belge" belgesinin güncel sürümüne "onayli" etiketi eklenmiştir
    And "belge" belgesi API ile soft delete edilmiştir
    When yönetici "belge" belgesinin güncel sürümüne "yeni" etiketini eklemeyi dener
    Then son API yanıtı başarısız olmalıdır
    And "belge" belgesinin güncel sürüm etiketleri yalnızca "onayli" olmalıdır
    When yönetici "belge" belgesinin güncel sürümünden "onayli" etiketini kaldırmayı dener
    Then son API yanıtı başarısız olmalıdır
    And "belge" belgesinin güncel sürüm etiketleri yalnızca "onayli" olmalıdır

  # REQUIREMENT REVIEW (RQ-30, 2026-09-27 yeniden doğrulama): soft delete sonrası mevcut bir ilişkinin
  # kaldırılmasını yasaklayan açık bir ürün kuralı yok; bu bir temizlik işlemi olarak kasıtlı olabilir.
  # Aşağıdaki beklenti ÖNERİLEN kuraldır, doğrulanmış ürün kuralı değildir (eski sınıflandırma: BUG-05 CONFIRMED).
  @integrity @deleted @relationships @requirement-review @RQ-30
  Scenario: B20-02 Soft delete edilmiş kaynak belgenin ilişkisi silinemez
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "kaynak" oluşturmuştur
    And yönetici API ile yeni bir sınıfsız "hedef" oluşturmuştur
    And yönlü "referans" ilişki türü oluşturulmuştur
    And "kaynak" belgesinden "hedef" belgesine "referans" ilişkisi oluşturulmuştur
    And "kaynak" belgesi API ile soft delete edilmiştir
    When yönetici "kaynak" belgesinin "referans" ilişkisini silmeyi dener
    Then son API yanıtı başarısız olmalıdır
    And "hedef" belgesinin gelen ilişkileri arasında "kaynak" belgesi bulunmalıdır

  @integrity @deleted @lock
  Scenario: B20-03 Soft delete edilmiş belge checkout edilemez ve kilit oluşmaz
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "belge" oluşturmuştur
    And "belge" belgesi API ile soft delete edilmiştir
    When yönetici "belge" belgesini checkout yapmayı dener
    Then son API yanıtı başarısız olmalıdır
    And "belge" belgesinde aktif kilit bulunmamalıdır

  # ---------------------------------------------------------------------------
  # Silinmiş belge: arayüz (F1/F8 ile aynı sınıf, FARKLI kontroller)
  # ---------------------------------------------------------------------------

  @integrity @deleted @ui
  Scenario: B20-04 Soft delete edilmiş belgede sürüm etiketi ekleme kontrolü sunulmamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then silinmiş belgenin sürüm geçmişinde etiket ekleme kontrolü görüntülenmemelidir

  @integrity @deleted @ui @relationships
  Scenario: B20-05 Soft delete edilmiş belgede Link document kontrolü sunulmamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then silinmiş belgenin ilişkiler kartında Link document kontrolü görüntülenmemelidir

  # ---------------------------------------------------------------------------
  # İlişkiler: yaşam döngüsü, kurallar, yetki
  # ---------------------------------------------------------------------------

  @relationships
  Scenario: B20-06 İlişki kaynakta giden, hedefte gelen olarak görünür ve silinince ikisinden de kalkar
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "kaynak" oluşturmuştur
    And yönetici API ile yeni bir sınıfsız "hedef" oluşturmuştur
    And yönlü "referans" ilişki türü oluşturulmuştur
    When yönetici "kaynak" belgesinden "hedef" belgesine "referans" ilişkisi oluşturur
    Then son API yanıtının durum kodu 201 olmalıdır
    And "kaynak" belgesinin giden ilişkileri arasında "hedef" belgesi bulunmalıdır
    And "hedef" belgesinin gelen ilişkileri arasında "kaynak" belgesi bulunmalıdır
    When yönetici "kaynak" belgesinin "referans" ilişkisini silmeyi dener
    Then son API yanıtı başarılı olmalıdır
    And "kaynak" belgesinin hiç ilişkisi olmamalıdır
    And "hedef" belgesinin hiç ilişkisi olmamalıdır

  @relationships @validation
  Scenario: B20-07 İlişki kuralları kendine bağlantıyı, tekrarı, döngüyü, bilinmeyen türü ve yabancı sürümü reddeder
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "A" oluşturmuştur
    And yönetici API ile yeni bir sınıfsız "B" oluşturmuştur
    And yönlü "referans" ilişki türü oluşturulmuştur
    And "A" belgesinden "B" belgesine "referans" ilişkisi oluşturulmuştur
    Then aşağıdaki ilişki denemeleri beklenen sonuçları vermeli ve yeni ilişki oluşmamalıdır:
      | kaynak | hedef | tür           | bağlama                | beklenen |
      | A      | A     | referans      | LATEST                 | 422      |
      | A      | B     | referans      | LATEST                 | 409      |
      | B      | A     | referans      | LATEST                 | 409      |
      | A      | B     | bilinmeyen    | LATEST                 | 422      |
      | B      | A     | referans      | SPECIFIC:B'nin sürümü  | 422      |

  @relationships @acl @security
  Scenario: B20-08 Hedef belgeyi okuyamayan kullanıcı ilişki oluşturamaz
    Given yönetici API oturumu açmıştır
    And "bağlayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "bağlayan" API ile giriş yapmıştır
    And yetki devralması kapalı "açık alan" oluşturulmuştur
    And yetki devralması kapalı "gizli alan" oluşturulmuştur
    And "açık alan" klasöründe "bağlayan" kullanıcısına "manager" rolü verilmiştir
    And "bağlayan" "açık alan" klasörüne "kendi belgesi" belgesini yüklemiştir
    And yönetici "gizli alan" klasörüne "gizli belge" belgesini yüklemiştir
    And yönlü "referans" ilişki türü oluşturulmuştur
    When "bağlayan" "kendi belgesi" belgesinden "gizli belge" belgesine "referans" ilişkisi oluşturmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "kendi belgesi" belgesinin hiç ilişkisi olmamalıdır
    And "gizli belge" belgesinin hiç ilişkisi olmamalıdır

  # ---------------------------------------------------------------------
  # Batch 28 — Relationships & referential integrity (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: documents.rs hard_delete_document ("removes the document, every version row, ACLs, tags ...")
  @batch28 @DOC-02 @critical
  Scenario: DOC-02 Etiketli, sürüm etiketli ve ilişkili belge kalıcı silinince kayıt, içerik, sürüm ve ilişki izi kalmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "silme klasörü" oluşturulmuştur
    And yönetici "silme klasörü" klasörüne "silinecek belge" belgesini yüklemiştir
    And yönetici "silme klasörü" klasörüne "karşı belge" belgesini yüklemiştir
    And yönlü "referans" ilişki türü oluşturulmuştur
    And "silinecek belge" belgesine "qa-hard" etiketi eklenmiştir
    And "silinecek belge" belgesinin güncel sürümüne "onayli" etiketi eklenmiştir
    And yönlü "geri referans" ilişki türü oluşturulmuştur
    And "silinecek belge" belgesinden "karşı belge" belgesine "referans" ilişkisi oluşturulmuştur
    And "karşı belge" belgesinden "silinecek belge" belgesine "geri referans" ilişkisi oluşturulmuştur
    And "silinecek belge" belgesi API ile soft delete edilmiştir
    When yönetici "silinecek belge" belgesini kalıcı olarak siler
    Then "silinecek belge" belgesinin kaydı, içeriği, sürümleri ve eski sürüm içeriği 404 dönmelidir
    And "karşı belge" belgesinin ilişkilerinde "silinecek belge" belgesine ait kayıt kalmamalıdır
    And "qa-hard" etiketiyle yapılan aramada "silinecek belge" belgesi bulunmamalıdır

  # ECM kaynağı: engine/relationships/mod.rs list_relationships_for_document (okunamayan karşı belge filtrelenir)
  @batch28 @REL-01 @critical
  Scenario: REL-01 İlişkinin karşı belgesini okuyamayan kullanıcı o belgenin kimliğini ve başlığını göremez
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "açık klasör" oluşturulmuştur
    And yetki devralması kapalı "gizli klasör" oluşturulmuştur
    And "açık klasör" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "açık klasör" klasörüne "görünen belge" belgesini yüklemiştir
    And yönetici "açık klasör" klasörüne "görünen hedef" belgesini yüklemiştir
    And yönetici "gizli klasör" klasörüne "gizli hedef" belgesini yüklemiştir
    And yönlü "referans" ilişki türü oluşturulmuştur
    And "görünen belge" belgesinden "gizli hedef" belgesine "referans" ilişkisi oluşturulmuştur
    And yönlü "bağlantı" ilişki türü oluşturulmuştur
    And "görünen belge" belgesinden "görünen hedef" belgesine "bağlantı" ilişkisi oluşturulmuştur
    Then "okur" "görünen belge" belgesinin ilişkilerinde "gizli hedef" belgesini ve başlığını görmemelidir
    And "okur" "görünen belge" belgesinin ilişkilerinde "görünen hedef" belgesini görmelidir

  # ECM kaynağı: engine/documents custom fields (silinmiş belge üzerinde yazma kabul ediliyor)
  @batch28 @CF-03 @known-bug
  Scenario: CF-03 Soft delete edilmiş belgenin özel alan değerleri değiştirilemez
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "alan klasörü" oluşturulmuştur
    And yönetici "" özellikleriyle "alanlı sınıf" sınıfını oluşturmuştur
    And yönetici "alanlı sınıf" sınıfına "alan" adlı "text" türünde "opsiyonel" özel alan eklemiştir
    And "yönetici" "alanlı sınıf" sınıfıyla "alan klasörü" klasörüne "{}" metadatasıyla "alanlı belge" belgesini yüklemiştir
    And yönetici "alanlı belge" belgesinin özel alanlarını "{\"alan\": \"önce\"}" olarak kaydetmeyi dener
    And "alanlı belge" belgesi API ile soft delete edilmiştir
    When yönetici "alanlı belge" belgesinin özel alanlarını "{\"alan\": \"sonra\"}" olarak kaydetmeyi dener
    Then son API yanıtı başarısız olmalıdır
    And "alanlı belge" belgesinin "alan" özel alan değeri "önce" olmalıdır

  # ECM kaynağı: engine/relationships/mod.rs (yönsüz türde hedefte de Write gerekir; yönlü türde okuma yeter)
  @batch28 @REL-02
  Scenario: REL-02 Yönsüz ilişki oluşturmak için hedefte de yazma yetkisi gerekir
    Given yönetici API oturumu açmıştır
    And "bağlayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "hedef yazarı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "bağlayan" API ile giriş yapmıştır
    And "hedef yazarı" API ile giriş yapmıştır
    And yetki devralması kapalı "kaynak klasör" oluşturulmuştur
    And yetki devralması kapalı "hedef klasör" oluşturulmuştur
    And "kaynak klasör" klasöründe "bağlayan" kullanıcısına "manager" rolü verilmiştir
    And "hedef klasör" klasöründe "bağlayan" kullanıcısına "viewer" rolü verilmiştir
    And "hedef klasör" klasöründe "hedef yazarı" kullanıcısına "manager" rolü verilmiştir
    And "bağlayan" "kaynak klasör" klasörüne "kaynak belge" belgesini yüklemiştir
    And "hedef yazarı" "hedef klasör" klasörüne "hedef belge" belgesini yüklemiştir
    And yönsüz "karşılıklı" ilişki türü oluşturulmuştur
    And yönlü "tek yönlü" ilişki türü yönetici tarafından oluşturulmuştur
    When "bağlayan" "kaynak belge" belgesinden "hedef belge" belgesine "karşılıklı" türünde ilişki oluşturmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "bağlayan" "kaynak belge" belgesinden "hedef belge" belgesine "tek yönlü" türünde ilişki oluşturmayı dener
    Then son API yanıtının durum kodu 201 olmalıdır
    Given "hedef klasör" klasöründe "bağlayan" kullanıcısına "manager" rolü verilmiştir
    When "bağlayan" "kaynak belge" belgesinden "hedef belge" belgesine "karşılıklı" türünde ilişki oluşturmayı dener
    Then son API yanıtının durum kodu 201 olmalıdır

  # ECM kaynağı: relationships binding_policy SPECIFIC (sabit sürüm) / LATEST (güncel sürüm)
  @batch28 @REL-03
  Scenario: REL-03 Belirli sürüme bağlı ilişki hedefe yeni sürüm eklense de o sürümde kalır, LATEST güncel sürümü gösterir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "ilişki klasörü" oluşturulmuştur
    And yönetici "ilişki klasörü" klasörüne "kaynak belge" belgesini yüklemiştir
    And yönetici "ilişki klasörü" klasörüne "hedef belge" belgesini yüklemiştir
    And yönlü "referans" ilişki türü yönetici tarafından oluşturulmuştur
    And "yönetici" "kaynak belge" belgesinden "hedef belge" belgesine "referans" türünde "SPECIFIC" bağlamalı ilişki oluşturur
    And yönlü "güncel referans" ilişki türü yönetici tarafından oluşturulmuştur
    And "yönetici" "kaynak belge" belgesinden "hedef belge" belgesine "güncel referans" türünde "LATEST" bağlamalı ilişki oluşturur
    When "yönetici" "hedef belge" belgesine "QA hedef v2" içerikli yeni bir yama sürümü eklemiştir
    Then "kaynak belge" belgesinin "SPECIFIC" bağlamalı ilişkisi hedefin ilk sürümüne bağlı kalmalıdır
    And "kaynak belge" belgesinin "LATEST" bağlamalı ilişkisi hedefin güncel sürümünü göstermelidir

  # ECM kaynağı: relationships/mod.rs delete_relationship (oluşturucu, kaynakta Write ya da admin)
  @batch28 @REL-04
  Scenario: REL-04 İlişkiyi yalnız oluşturucu, kaynak belgede yazma yetkisi olan ya da admin silebilir
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "ilişki klasörü" oluşturulmuştur
    And "ilişki klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "ilişki klasörü" klasörüne "kaynak belge" belgesini yüklemiştir
    And yönetici "ilişki klasörü" klasörüne "hedef belge" belgesini yüklemiştir
    And yönetici "ilişki klasörü" klasörüne "ilgisiz belge" belgesini yüklemiştir
    And yönlü "referans" ilişki türü oluşturulmuştur
    And "kaynak belge" belgesinden "hedef belge" belgesine "referans" ilişkisi oluşturulmuştur
    When "okur" "kaynak belge" belgesinin "referans" ilişkisini silmeyi dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "Only the relationship creator, a document writer, or an admin may delete this relationship." içermelidir
    And "kaynak belge" belgesinin "referans" ilişkisi hâlâ bulunmalıdır
    And "kaynak belge" belgesinin "referans" ilişkisini ilgisiz "ilgisiz belge" belgesinin yolundan silme sonucu RQ-21 kanıtı olarak kaydedilmelidir

  # ECM kaynağı: LinkDocumentModal.tsx + DocumentRelationshipsCard.tsx (Outbound/Inbound, Remove relationship)
  @batch28 @REL-06
  Scenario: REL-06 Kullanıcı Link document penceresiyle ilişki kurar, iki belgede de görünür ve Unlink ile kaldırılır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "ilişki klasörü" oluşturulmuştur
    And yönetici "ilişki klasörü" klasörüne "kaynak belge" belgesini yüklemiştir
    And yönetici "ilişki klasörü" klasörüne "hedef belge" belgesini yüklemiştir
    And yönlü "arayüz referansı" ilişki türü yönetici tarafından oluşturulmuştur
    When yönetici arayüzde "kaynak belge" belgesinden "hedef belge" belgesine "arayüz referansı" türünde Link document ile ilişki kurar
    Then "kaynak belge" belgesinin giden ilişkileri arasında "hedef belge" belgesi bulunmalıdır
    And "hedef belge" belgesinin gelen ilişkileri arasında "kaynak belge" belgesi bulunmalıdır
    And yönetici arayüzde "kaynak belge" belgesinin ilişkiler kartında "hedef belge" başlığını görmelidir
    And yönetici arayüzde "hedef belge" belgesinin ilişkiler kartında "kaynak belge" başlığını görmelidir
    When yönetici arayüzde "kaynak belge" belgesinin ilişkisini Unlink ile kaldırır
    Then "kaynak belge" belgesinin hiç ilişkisi olmamalıdır
    And "hedef belge" belgesinin hiç ilişkisi olmamalıdır

  # ECM kaynağı: relationship types (ad tekil ve zorunlu; sistem türleri salt okunur; ilişki tür adını taşır)
  @batch28 @REL-05
  Scenario: REL-05 İlişki türü adı tekil ve zorunludur, tür yeniden adlandırılınca mevcut ilişki yeni adla görünür
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "ilişki klasörü" oluşturulmuştur
    And yönetici "ilişki klasörü" klasörüne "kaynak belge" belgesini yüklemiştir
    And yönetici "ilişki klasörü" klasörüne "hedef belge" belgesini yüklemiştir
    And yönlü "adlandırılan" ilişki türü yönetici tarafından oluşturulmuştur
    And "yönetici" "kaynak belge" belgesinden "hedef belge" belgesine "adlandırılan" türünde ilişki oluşturmayı dener
    And son API yanıtının durum kodu 201 olmalıdır
    When yönetici "adlandırılan" ilişki türüyle aynı adda tür oluşturmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    When yönetici boş adla ilişki türü oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    When yönetici bir sistem ilişki türünü düzenlemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "System relationship types are read-only and cannot be edited." içermelidir
    And sistem ilişki türünün adı değişmemiş olmalıdır
    When yönetici "adlandırılan" ilişki türünü yeniden adlandırır
    Then "kaynak belge" belgesinin giden ilişkisi "adlandırılan" türünün güncel adıyla görünmelidir
