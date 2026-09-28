@coverage-gap @api-contract
Feature: Kalan uç noktaların sözleşmesi

  # ---------------------------------------------------------------------
  # Batch 35 — Plan dışı yeni testler. Tam koşudaki çalışma zamanı uç nokta
  # kapsamında hiç çağrılmayan uçlar (140 rotadan 132'si çağrılıyordu) ve
  # uzun ad sınırı taraması.
  # ---------------------------------------------------------------------

  # ECM kaynağı: api/rest/classes.rs update_class (UpdateClassRequest.expected_version, name, description)
  @batch35 @CLS-20
  Scenario: CLS-20 Sınıf adı ve açıklaması iyimser kilitle güncellenir; eski sürüm, yinelenen ve boş ad, yetkisiz ve bilinmeyen istek reddedilir
    Given yönetici API oturumu açmıştır
    And "meraklı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "meraklı" API ile giriş yapmıştır
    And yönetici "ref:text:opsiyonel" özellikleriyle "güncellenen sınıf" sınıfını oluşturmuştur
    And yönetici "ref:text:opsiyonel" özellikleriyle "diğer sınıf" sınıfını oluşturmuştur
    Then yönetici "güncellenen sınıf" sınıfının adını ve açıklamasını güncellediğinde kalıcı olmalı; eski sürüm, "diğer sınıf" adı, boş ad, yönetici olmayan "meraklı" ve bilinmeyen sınıf reddedilmelidir

  # ECM kaynağı: api/rest/documents.rs save_doc_stream (metadata parçası dosyadan önce; zorunlu özellik doğrulaması)
  @batch35 @DOC-30
  Scenario: DOC-30 Sınıflı büyük dosya akışla yüklenir; içerik, sınıf ve özellik değeri korunur, zorunlu özellik eksikse belge oluşmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "akış klasörü" oluşturulmuştur
    And yönetici "ref:text:zorunlu" özellikleriyle "akış sınıfı" sınıfını oluşturmuştur
    Then yönetici "akış sınıfı" sınıfıyla "akış klasörü" klasörüne 3 MB'lık dosyayı akışla yüklediğinde içerik, sınıf ve "ref" özelliği korunmalı; zorunlu özellik eksikse belge oluşmamalıdır

  # ECM kaynağı: GET /documents/{id}/versions/{vid}/labels + GET /documents/{id}/version-labels + /versions labels alanı
  @batch35 @VER-12
  Scenario: VER-12 Sürüm etiketleri sürüm ucu, belge ucu ve sürüm listesinde tutarlıdır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And yönetici "etiket klasörü" klasörüne "etiketli belge" belgesini yüklemiştir
    And "yönetici" "etiketli belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeni sürüm |
    And "etiketli belge" belgesinin 1. sürümüne "rc-1" sürüm etiketi eklenmiştir
    And "etiketli belge" belgesinin 2. sürümüne "final" sürüm etiketi eklenmiştir
    And "etiketli belge" belgesinin 2. sürümüne "onay-2026" sürüm etiketi eklenmiştir
    Then "etiketli belge" belgesinin sürüm bazlı ve belge bazlı etiket listeleri her sürüm için aynı olmalıdır

  # ECM kaynağı: api/rest/property_templates.rs get_property_template (yönetici)
  @batch35 @PT-10
  Scenario: PT-10 Özellik şablonu ayrıntısı listedeki kayıtla aynıdır; bilinmeyen şablon 404, yönetici olmayan 403 alır
    Given yönetici API oturumu açmıştır
    And "meraklı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "meraklı" API ile giriş yapmıştır
    When yönetici "ayrıntı şablonu" adlı "text" türünde özellik şablonu oluşturur
    Then "ayrıntı şablonu" özellik şablonunun ayrıntısı listedeki kayıtla aynı olmalı, bilinmeyen şablon 404 ve "meraklı" için 403 dönmelidir

  # ECM kaynağı: api/rest/routes.rs kimliksiz rotalar (health, ready, startup, metrics) + kimlikli /info
  @batch35 @OPS-01 @requirement-review
  Scenario: OPS-01 Sağlık uçları kimliksiz çalışır, sistem bilgisi kimlik ister; metrik erişimi gereksinim kararına kaydedilir
    Then işletim uçları kimliksiz erişimde yalnız durum bilgisi vermeli, sistem bilgisi kimlik istemeli ve metrik erişimi RQ-28 kanıtı olarak kaydedilmelidir

  # ECM kaynağı: document_class.name / user_group.name / role.name TEXT + UNIQUE btree (satır ~2,7 KB sınırı);
  # uzunluk doğrulaması yok -> sıkıştırılamaz uzun ad PostgreSQL 54000 hatası -> HTTP 500. Sınır değeri RQ-29.
  @batch35 @BND-20 @known-bug
  Scenario: BND-20 Aşırı uzun sınıf, grup ve rol adları sunucu hatası yerine doğrulama hatasıyla reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek               | gövde                                                                                                                                              | durum           | mesaj |
      | POST /v1/classes    | {"name":"QA {M} {RASTGELE:3000}","description":"QA","properties":[{"key":"ref","display_name":"REF","data_type":"text","is_multi_value":false,"required":false}]} | 400\|409\|413\|422 | -     |
      | POST /v1/groups     | {"name":"QA {M} {RASTGELE:3000}"}                                                                                                                  | 400\|409\|413\|422 | -     |
      | POST /v1/rbac/roles | {"name":"QA {M} {RASTGELE:3000}","permission_ids":[]}                                                                                              | 400\|409\|413\|422 | -     |
