@coverage-gap @second-pass
Feature: İkinci geçiş kapsama boşlukları (Phase C, 2026-09-28)

  # ---------------------------------------------------------------------
  # reports/ECM_SECOND_PASS_COVERAGE_GAPS.md — READY TO IMPLEMENT (işlevsel).
  # Her senaryonun beklentisi yorum satırındaki ECM kaynağından gelir.
  # ---------------------------------------------------------------------

  # ======================= Batch C2 (P1) ===============================

  # ECM kaynağı: README `q` (full-text, snippet **word**); db/migrations/0029_fts.sql search_vector (C = content_text);
  # engine/documents/content_extraction.rs text/plain; read.rs include_deleted yalnız admin; ACL filtrelemesi.
  @phaseC @C2 @NEW-06
  Scenario: NEW-06 Tam metin araması yalnız içerikte geçen kelimeyi bulur, vurgulu parça verir, okunamayan ve silinmiş belgeyi döndürmez
    Given yönetici API oturumu açmıştır
    And tam metin araması için benzersiz bir "anahtar" kelimesi belirlenmiştir
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "açık klasör" oluşturulmuştur
    And yetki devralması kapalı "kapalı klasör" oluşturulmuştur
    And "açık klasör" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yönetici" "açık klasör" klasörüne içeriğinde "anahtar" kelimesi geçen ama başlığında geçmeyen "görünür belge" belgesini yüklemiştir
    And "yönetici" "kapalı klasör" klasörüne içeriğinde "anahtar" kelimesi geçen ama başlığında geçmeyen "gizli belge" belgesini yüklemiştir
    And "yönetici" "açık klasör" klasörüne içeriğinde "anahtar" kelimesi geçen ama başlığında geçmeyen "silinecek belge" belgesini yüklemiştir
    And "yönetici" "silinecek belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | soft delete |
    Then "okur" tam metin aramasında "anahtar" kelimesiyle yalnız "görünür belge" belgelerini vurgulu parçayla bulmalıdır
    And "yönetici" tam metin aramasında "anahtar" kelimesiyle yalnız "görünür belge, gizli belge" belgelerini vurgulu parçayla bulmalıdır

  # ECM kaynağı: README "Duplicate-title → new version … enforced in the save path … can't be bypassed by an API client";
  # engine/documents/save.rs find_versionable_duplicate (aynı klasör + birebir kırpılmış başlık + aynı sınıf bağlamı +
  # çağıranın Write hakkı; varsayılan Patch). Yazma hakkı olmayan kullanıcı için katlama yapılmaz (varlık sızdırılmaz).
  @phaseC @C2 @NEW-08
  Scenario: NEW-08 API istemcisinin aynı başlıkla oluşturması yeni sürüme katlanır; harf farkı, başka klasör ve yazma hakkı yoksa yeni belge oluşur
    Given yönetici API oturumu açmıştır
    And "katkıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "katkıcı" API ile giriş yapmıştır
    And yetki devralması kapalı "rapor klasörü" oluşturulmuştur
    And yetki devralması kapalı "başka klasör" oluşturulmuştur
    And "rapor klasörü" klasöründe "katkıcı" kullanıcısına "manager" rolü verilmiştir
    And yönetici "rapor klasörü" klasörüne "rapor" belgesini yüklemiştir
    When "yönetici" "rapor klasörü" klasörüne "rapor" belgesinin başlığını "birebir" biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç "ilk tekrar" olarak kaydedilir
    Then "ilk tekrar" sonucu "rapor" belgesine "1.0.1" sürümü olarak katlanmış olmalı ve klasörde o başlıkta tek belge kalmalıdır
    When "yönetici" "rapor klasörü" klasörüne "rapor" belgesinin başlığını "boşluklu" biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç "boşluklu tekrar" olarak kaydedilir
    Then "boşluklu tekrar" sonucu "rapor" belgesine "1.0.2" sürümü olarak katlanmış olmalı ve klasörde o başlıkta tek belge kalmalıdır
    When "yönetici" "rapor klasörü" klasörüne "rapor" belgesinin başlığını "küçük harf" biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç "küçük harfli" olarak kaydedilir
    Then "küçük harfli" sonucu "rapor" belgesinden farklı yeni bir belge olmalıdır
    When "yönetici" "başka klasör" klasörüne "rapor" belgesinin başlığını "birebir" biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç "başka klasördeki" olarak kaydedilir
    Then "başka klasördeki" sonucu "rapor" belgesinden farklı yeni bir belge olmalıdır
    When "katkıcı" "rapor klasörü" klasörüne "rapor" belgesinin başlığını "birebir" biçiminde kullanarak document_id olmadan yeni belge oluşturur ve sonuç "yazamayanın" olarak kaydedilir
    Then "yazamayanın" sonucu "rapor" belgesinden farklı yeni bir belge olmalıdır
    And "rapor" belgesinin sürüm sayısı 3 olmalıdır

  # ECM kaynağı: README allowed-class policy "enforced on every path … move, classify, reclassify, and version rollback";
  # folders/write.rs:161 (move), copy.rs:84 (copy), reclassify.rs:56/74, rollback.rs:78/86 (inaktif + izinli sınıf).
  @phaseC @C2 @NEW-09
  Scenario: NEW-09 İzinli sınıf ve pasif sınıf kuralı taşıma, kopyalama, yeniden sınıflandırma ve geri almada da uygulanır; reddedilen işlem belgeyi değiştirmez
    Given yönetici API oturumu açmıştır
    And yönetici "ref:text:opsiyonel" özellikleriyle "izinli sınıf" sınıfını oluşturmuştur
    And yönetici "ref:text:opsiyonel" özellikleriyle "yasak sınıf" sınıfını oluşturmuştur
    And yönetici "ref:text:opsiyonel" özellikleriyle "emekli sınıf" sınıfını oluşturmuştur
    And yetki devralması kapalı "kısıtlı klasör" oluşturulmuştur
    And yetki devralması kapalı "serbest klasör" oluşturulmuştur
    And yönetici "kısıtlı klasör" klasörünün izinli sınıflarını yalnız "izinli sınıf" olarak ayarlamıştır
    And yönetici "yasak sınıf" sınıfıyla "serbest klasör" klasörüne sınıflı "yasak belge" belgesini oluşturmuştur
    And yönetici "izinli sınıf" sınıfıyla "kısıtlı klasör" klasörüne sınıflı "izinli belge" belgesini oluşturmuştur
    And yönetici "yasak sınıf" sınıfıyla "serbest klasör" klasörüne sınıflı "geçmişli belge" belgesini oluşturmuştur
    And yönetici "geçmişli belge" belgesine "izinli sınıf" sınıfıyla yeni bir sürüm kaydetmiştir
    And yönetici "emekli sınıf" sınıfıyla "serbest klasör" klasörüne sınıflı "emekli geçmişli belge" belgesini oluşturmuştur
    And yönetici "emekli geçmişli belge" belgesine "izinli sınıf" sınıfıyla yeni bir sürüm kaydetmiştir
    # taşıma
    And "yasak belge" belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm
    When yönetici "yasak belge" belgesini "kısıtlı klasör" klasörüne taşımayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This folder only accepts" içermelidir
    And "yasak belge" belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır
    # kopyalama
    When yönetici "yasak belge" belgesini "kısıtlı klasör" klasörüne kopyalamayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This folder only accepts" içermelidir
    And "kısıtlı klasör" klasöründeki belge sayısı değişmemiş olmalıdır
    # yeniden sınıflandırma: izinli olmayan sınıf
    And "izinli belge" belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm
    When yönetici "izinli belge" belgesini "yasak sınıf" sınıfına yeniden sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This folder only accepts" içermelidir
    And "izinli belge" belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır
    # geri alma: izinli olmayan sınıfa dönen sürüm
    When yönetici "geçmişli belge" belgesini "kısıtlı klasör" klasörüne taşımayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "geçmişli belge" belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm
    When yönetici "geçmişli belge" belgesini ilk sürümüne geri almayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This folder only accepts" içermelidir
    And "geçmişli belge" belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır
    # pasif sınıf: yeniden sınıflandırma ve geri alma
    Given yönetici "emekli sınıf" sınıfını pasifleştirmiştir
    And "yasak belge" belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm
    When yönetici "yasak belge" belgesini "emekli sınıf" sınıfına yeniden sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "inactive" içermelidir
    And "yasak belge" belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır
    And "emekli geçmişli belge" belgesinin durumu kaydedilmiştir: klasör, sınıf ve güncel sürüm
    When yönetici "emekli geçmişli belge" belgesini ilk sürümüne geri almayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "inactive" içermelidir
    And "emekli geçmişli belge" belgesinin klasörü, sınıfı ve güncel sürümü kaydedilen durumla aynı olmalıdır

  # ECM kaynağı: kullanıcı kılavuzu §6 (ACL yalnız aktif grup üyelikleri üzerinden); postgres_repository/mod.rs:1147-1156
  # list_user_groups "Only active memberships count" (g.is_active = TRUE); UpdateGroupRequest.is_active.
  @phaseC @C2 @NEW-10
  Scenario: NEW-10 Pasifleştirilen grup üyelerine erişim vermez, üyelik korunur; yeniden aktifleştirilince erişim aynı oturumda geri gelir
    Given yönetici API oturumu açmıştır
    And "ekip üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip üyesi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    And "ekip klasörü" klasöründe "ekip" grubuna "viewer" rolü verilmiştir
    And yönetici "ekip klasörü" klasörüne "ekip belgesi" belgesini yüklemiştir
    When yönetici "ekip üyesi" kullanıcısını "ekip" grubuna ekler
    Then "ekip üyesi" "ekip belgesi" belgesini okuyabilmeli ve belge listesinde görmelidir
    When yönetici "ekip" grubunu "pasif" yapar
    Then "ekip üyesi" "ekip belgesi" belgesini okuyamamalı ve belge listesinde görmemelidir
    And "ekip" grubunun üyeleri arasında "ekip üyesi" tam bir kez listelenmelidir
    When yönetici "ekip" grubunu "aktif" yapar
    Then "ekip üyesi" "ekip belgesi" belgesini okuyabilmeli ve belge listesinde görmelidir

  # ECM kaynağı: kullanıcı kılavuzu 2.6; frontend pages/DocumentNew.tsx (sınıf seçimi, zorunlu özellik girişinde HTML
  # `required`, başarıda /documents/{id} yönlendirmesi); sunucu tarafı zorunluluk CLS-01/DOC-30 ile ayrıca kanıtlı.
  @phaseC @C2 @NEW-20 @ui
  Scenario: NEW-20 Kullanıcı arayüzden sınıflı belge yükler; zorunlu özellik boşken gönderim olmaz, doldurunca sınıf, özellik ve içerik kalıcıdır
    Given yönetici API oturumu açmıştır
    And yönetici "ref:text:zorunlu" özellikleriyle "arayüz sınıfı" sınıfını oluşturmuştur
    And yetki devralması kapalı "arayüz klasörü" oluşturulmuştur
    When yönetici arayüzde "arayüz klasörü" klasörüne "arayüz sınıfı" sınıfını seçip zorunlu "ref" özelliğini boş bırakarak "arayüz belgesi" belgesini yüklemeyi dener
    And yönetici arayüzde "ref" özelliğine "QA-ARAYUZ-REF" yazıp yüklemeyi tamamlar
    Then arayüzde yüklenen "arayüz belgesi" belgesi API'de "arayüz klasörü" klasöründe, "arayüz sınıfı" sınıfında, "ref"="QA-ARAYUZ-REF" özelliğiyle ve seçilen dosyanın baytlarıyla bulunmalıdır

  # ======================= Batch C3 (P2) ===============================

  # ECM kaynağı: README GET /documents `meta` ([{key, value?, op?}], eq varsayılan | contains, sınıftan bağımsız) ve
  # created_/updated_ after|before (ISO-8601); postgres documents.rs:1162-1220 (eq birebir, contains ILIKE, değersiz = anahtar varlığı);
  # documents_query.rs (meta/props JSON ve boş anahtar 422); OpenAPI list_documents 422 doğrulama yanıtı; saved_searches.rs:35.
  @phaseC @C3 @NEW-07
  Scenario: NEW-07 Serbest metadata ve tarih filtreleri tam doğru belgeleri döndürür, hatalı filtreler sunucu hatası olmadan reddedilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "metadata klasörü" oluşturulmuştur
    And yönetici "metadata klasörü" klasörüne "{\"proje\": \"alfa\", \"bolum\": \"satis\"}" serbest metadatasıyla "alfa belgesi" belgesini yüklemiştir
    And yönetici "metadata klasörü" klasörüne "{\"proje\": \"alfabet\"}" serbest metadatasıyla "alfabet belgesi" belgesini yüklemiştir
    And yönetici "metadata klasörü" klasörüne "{\"konu\": \"diger\"}" serbest metadatasıyla "diğer belge" belgesini yüklemiştir
    Then "metadata klasörü" klasöründe metadata filtreleri tam olarak şu belgeleri döndürmelidir:
      | meta filtresi                                         | beklenen                      |
      | [{"key":"proje","value":"alfa"}]                      | alfa belgesi                  |
      | [{"key":"proje","value":"alfa","op":"contains"}]      | alfa belgesi, alfabet belgesi |
      | [{"key":"proje","value":"ALFA","op":"contains"}]      | alfa belgesi, alfabet belgesi |
      | [{"key":"proje"}]                                     | alfa belgesi, alfabet belgesi |
      | [{"key":"proje"},{"key":"bolum","value":"satis"}]     | alfa belgesi                  |
      | [{"key":"yok"}]                                       | -                             |
    When "yönetici" "diğer belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | statü under_review |
    Then "metadata klasörü" klasöründe tarih filtreleri "alfa belgesi" belgesinin oluşturulma ve "diğer belge" belgesinin güncellenme zamanına göre doğru sonuç vermelidir
    And hatalı liste filtreleri doğrulama hatasıyla reddedilmeli ve sunucu hatası vermemelidir:
      | sorgu                                   | mesaj                                              |
      | meta=bozuk                              | invalid meta filter JSON                           |
      | meta=%5B%7B%22key%22%3A%22%20%22%7D%5D  | metadata filter entries must have a non-empty key. |
      | props=bozuk                             | invalid props filter JSON                          |
    And geçersiz durum içeren kayıtlı arama filtresi 422 ile reddedilmeli ve kayıtlı arama oluşmamalıdır

  # ECM kaynağı: OpenAPI list_documents created_/updated_ after|before "ISO-8601 inclusive bound"; aynı uç diğer tüm hatalı
  # parametreleri 422 ile reddeder (status, order, limit, sort_by, meta, props); error.rs F3 (22P02 → 422 eşlenir).
  # CONFIRMED BUG-21 (2026-09-28): postgres documents.rs:1162-1180 değeri doğrulamadan ::timestamptz'e çevirir, 22007/22008
  # eşlenmez → 500. Beklenti doğru olarak korunur.
  @phaseC @C3 @NEW-07 @NEW-07B @known-bug @BUG-21
  Scenario: NEW-07B Biçimi bozuk ISO-8601 tarih filtresi sunucu hatası yerine doğrulama hatasıyla reddedilir
    Given yönetici API oturumu açmıştır
    Then hatalı liste filtreleri doğrulama hatasıyla reddedilmeli ve sunucu hatası vermemelidir:
      | sorgu                                     | mesaj |
      | created_after=tarih-degil                 | -     |
      | created_before=2026-13-45                 | -     |
      | updated_before=2026-02-30T00%3A00%3A00Z   | -     |

  # ECM kaynağı: engine/property_value_codec.rs ("property '{key}' {msg}": must be a number / boolean / valid date string);
  # doc_management_engine.rs:280 "metadata must be a JSON object"; api/rest/documents.rs:71 "invalid metadata JSON".
  @phaseC @C3 @NEW-14
  Scenario: NEW-14 Türüne uymayan özellik değerleri, nesne olmayan metadata ve bozuk akış metadatası reddedilir, belge oluşmaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir yükleme işareti belirlenmiştir
    And yönetici "sayi:numeric:opsiyonel,evet:boolean:opsiyonel,tarih:date:opsiyonel" özellikleriyle "tipli sınıf" sınıfını oluşturmuştur
    Then "tipli sınıf" sınıfıyla şu metadata gövdeleriyle yükleme reddedilmeli ve hiçbir belge oluşmamalıdır:
      | metadata                | durum | mesaj                          |
      | {"sayi": "on iki"}      | 422   | property 'sayi'                |
      | {"evet": "belki"}       | 422   | property 'evet'                |
      | {"tarih": "31/12/2026"} | 422   | property 'tarih'               |
      | {"tarih": 20261231}     | 422   | property 'tarih'               |
      | ["sayi", 1]             | 422   | metadata must be a JSON object |
    And akış yüklemesinde bozuk metadata JSON 422 ve "invalid metadata JSON" ile reddedilmeli ve belge oluşmamalıdır

  # ECM kaynağı: README ACL (class ACL denetimi class.acl_grant/class.acl_revoke, ayrı eylemler);
  # engine/acl/management/document.rs:92/135 ve class.rs:90/134 (hedef türü acl, hedef = ACL satırı, after_state sahibi taşır).
  @phaseC @C3 @NEW-17
  Scenario: NEW-17 Belge ve sınıf izinlerinin verilmesi ve kaldırılması doğru eylem, aktör ve bağlamla denetime yazılır
    Given yönetici API oturumu açmıştır
    And "izinli" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "izin klasörü" oluşturulmuştur
    And yönetici "izin klasörü" klasörüne "izinli belge" belgesini yüklemiştir
    # Ürün kuralı: belge izni için kullanıcı önce klasörü görebilmeli (FOLDER_ACCESS_REQUIRED).
    And "izin klasörü" klasöründe "izinli" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "ref:text:opsiyonel" özellikleriyle "izinli sınıf" sınıfını oluşturmuştur
    When yönetici "belge" "izinli belge" üzerinde "izinli" kullanıcısına okuma izni verip kaldırır
    And yönetici "sınıf" "izinli sınıf" üzerinde "izinli" kullanıcısına okuma izni verip kaldırır
    Then "izinli belge" izni için "document.acl_grant" ve "document.acl_revoke" denetim kayıtları yönetici aktörüyle ve "document_id" bağlamıyla yazılmış olmalıdır
    And "izinli sınıf" izni için "class.acl_grant" ve "class.acl_revoke" denetim kayıtları yönetici aktörüyle ve "class_id" bağlamıyla yazılmış olmalıdır

  # ECM kaynağı: README Relationships (admin tanımlı türler, sistem türleri salt okunur); frontend pages/RelationshipTypes.tsx
  # (New type formu, Edit/Save, sistem türünde "Read-only"); App.tsx AdminGuard. Yeniden adlandırmanın mevcut bağlara
  # yansıması REL-05 ile API'de kanıtlı olduğu için burada tekrar edilmez.
  @phaseC @C3 @NEW-19 @ui
  Scenario: NEW-19 İlişki türleri yönetim sayfasında tür oluşturma ve yeniden adlandırma kalıcıdır, sistem türü salt okunurdur, admin olmayan yönlendirilir
    Given yönetici API oturumu açmıştır
    And "meraklı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    # AdminGuard.tsx: admin olmayan /classes adresine yönlendirilir (SYS-01 ile aynı hedef).
    Then "meraklı" arayüzde "/admin/relationship-types" adresine gittiğinde yönetim verisi görmeden "/classes" adresine yönlendirilmelidir
    When yönetici arayüzde ilişki türleri sayfasında "arayüz türü" türünü yönlü olarak oluşturur
    And yönetici arayüzde "arayüz türü" türünü yeni bir adla yeniden adlandırır
    Then "arayüz türü" türü API'de yeni adıyla ve yönlü olarak kayıtlı olmalı, eski adla kayıt kalmamalıdır
    And sistem ilişki türü "copied_from" arayüzde salt okunur görünmeli ve düzenleme kontrolü sunulmamalıdır

  # ECM kaynağı: frontend api/documents.ts:88 validateUploadFile ("The selected file is empty.", "File type '.x' is not allowed
  # for upload.") + DocumentNewUnclassified.tsx:100 (hata varsa gönderim yok); sunucu kuralları DOC-04/05 ile ayrıca kanıtlı.
  @phaseC @C3 @NEW-22 @ui
  Scenario: NEW-22 Yükleme formu boş ve engelli dosyada hatayı gösterir, istek göndermez, sayfada kalır ve belge oluşmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "form klasörü" oluşturulmuştur
    Then yönetici arayüzde "form klasörü" klasörüne "kurulum.exe" dosyasını "MZ" içerikle yüklemeyi denediğinde "File type '.exe' is not allowed for upload." mesajını görmeli, istek gönderilmemeli ve belge oluşmamalıdır
    And yönetici arayüzde "form klasörü" klasörüne "bos.txt" dosyasını "(boş)" içerikle yüklemeyi denediğinde "The selected file is empty." mesajını görmeli, istek gönderilmemeli ve belge oluşmamalıdır

  # ECM kaynağı: README ACL "mutating endpoints honor the same computed RBAC permissions the effective-access engine reports";
  # ACL-12 (kullanıcı DENY grup ALLOW'u ezer); oluşturucunun belge düzeyi yönetici hakkı (auto_grant_document_creator).
  # Klasör "editor" rolü BUG-10 (ACL-16) regresyonu olduğu için bilerek hariç tutuldu.
  @phaseC @C3 @NEW-24
  Scenario: NEW-24 Etkin erişim raporu viewer, DENY ve belge oluşturucusu için gerçek okuma ve yazma sonucuyla uyuşur
    Given yönetici API oturumu açmıştır
    And "okuyan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "engelli" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okuyan" API ile giriş yapmıştır
    And "engelli" API ile giriş yapmıştır
    And "yazar" API ile giriş yapmıştır
    And "okuma ekibi" isimli yeni bir grup oluşturulmuştur
    And yönetici "engelli" kullanıcısını "okuma ekibi" grubuna ekler
    And yetki devralması kapalı "erişim klasörü" oluşturulmuştur
    And "erişim klasörü" klasöründe "okuyan" kullanıcısına "viewer" rolü verilmiştir
    And "erişim klasörü" klasöründe "okuma ekibi" grubuna "viewer" rolü verilmiştir
    And "erişim klasörü" klasöründe "engelli" kullanıcısına DENY okuma kuralı verilmiştir
    And "erişim klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "erişim klasörü" klasörüne "yazarın belgesi" belgesini yüklemiştir
    Then "okuyan" için "yazarın belgesi" belgesinde etkin erişim okuma="var" yazma="yok" bildirmeli ve gerçek okuma ile yeniden adlandırma buna uymalıdır
    And "engelli" için "yazarın belgesi" belgesinde etkin erişim okuma="yok" yazma="yok" bildirmeli ve gerçek okuma ile yeniden adlandırma buna uymalıdır
    And "yazar" için "yazarın belgesi" belgesinde etkin erişim okuma="var" yazma="var" bildirmeli ve gerçek okuma ile yeniden adlandırma buna uymalıdır

  # ECM kaynağı: kullanıcı kılavuzu 7.1 (geri yükleme belgeyi aktif listeye döndürür); erişim klasör zincirinden gelir;
  # belge geçmişi silme ve geri yüklemeyi korur (AUD-05 ile aynı eylem adları).
  @phaseC @C3 @NEW-26
  Scenario: NEW-26 Klasörü yeniden adlandırılıp izinleri değiştirilen belge geri yüklenince aynı klasöre döner ve güncel izinler uygulanır
    Given yönetici API oturumu açmıştır
    And "eski okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yeni okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "eski okur" API ile giriş yapmıştır
    And "yeni okur" API ile giriş yapmıştır
    And yetki devralması kapalı "geri yükleme klasörü" oluşturulmuştur
    And "geri yükleme klasörü" klasöründe "eski okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "geri yükleme klasörü" klasörüne "dönen belge" belgesini yüklemiştir
    And "yönetici" "dönen belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | soft delete |
    When yönetici "geri yükleme klasörü" klasörünü yeniden adlandırır
    And yönetici "geri yükleme klasörü" klasöründe "eski okur" kullanıcısının iznini kaldırır
    And "geri yükleme klasörü" klasöründe "yeni okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "dönen belge" belgesini geri yükler
    Then geri yüklenen "dönen belge" belgesi yeniden adlandırılan "geri yükleme klasörü" klasöründe olmalı, geçmişi silme ve geri yüklemeyi içermelidir
    And "yeni okur" "dönen belge" belgesini okuyabilmeli ve belge listesinde görmelidir
    And "eski okur" "dönen belge" belgesini okuyamamalı ve belge listesinde görmemelidir

  # ======================= Batch C4 (P3) ===============================

  # ECM kaynağı: engine/documents/classify.rs:40, reclassify.rs:44, copy.rs:44 (Validation → 422, mesajlar birebir).
  @phaseC @C4 @NEW-15
  Scenario: NEW-15 Sınıflı belgeye sınıflandırma, sınıfsız belgeye yeniden sınıflandırma ve desteklenmeyen kopya kipi reddedilir, hiçbir şey değişmez
    Given yönetici API oturumu açmıştır
    And yönetici "ref:text:opsiyonel" özellikleriyle "durum sınıfı" sınıfını oluşturmuştur
    And yetki devralması kapalı "durum klasörü" oluşturulmuştur
    And yönetici "durum sınıfı" sınıfıyla "durum klasörü" klasörüne sınıflı "sınıflı belge" belgesini oluşturmuştur
    And yönetici "durum klasörü" klasörüne "sınıfsız belge" belgesini yüklemiştir
    Then durum dışı sınıflandırma, yeniden sınıflandırma ve kopyalama istekleri reddedilmeli ve hiçbir şey değişmemelidir: sınıflı "sınıflı belge", sınıfsız "sınıfsız belge", sınıf "durum sınıfı"

  # ECM kaynağı: engine/folders/write.rs:275 "Provide at least one of name or inherit_permissions."
  @phaseC @C4 @NEW-16
  Scenario: NEW-16 Alan içermeyen klasör güncellemesi reddedilir ve klasör değişmez
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "boş güncelleme klasörü" oluşturulmuştur
    Then "boş güncelleme klasörü" klasörüne alansız PATCH isteği 422 ve "Provide at least one of name or inherit_permissions." ile reddedilmeli, klasör değişmemelidir

  # ECM kaynağı: kullanıcı kılavuzu 1.18 (denetim günlüğü değiştirilemez kayıt); routes.rs /admin/audit-logs/{id} yalnız GET.
  @phaseC @C4 @NEW-18
  Scenario: NEW-18 Denetim kaydı yönetici tarafından da değiştirilemez ve silinemez
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yönetici "denetim klasörü" klasörüne "denetlenen belge" belgesini yüklemiştir
    And "denetlenen belge" belgesinin bir denetim kaydı seçilmiştir
    Then yönetici denetim kaydını değiştirme ve silme isteklerinde reddedilmeli ve kayıt aynen kalmalıdır
