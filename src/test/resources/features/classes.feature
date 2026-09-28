@coverage-gap @classes
Feature: Belge sınıfları ve özellik doğrulaması

  # ECM kaynağı:
  #  - POST /v1/classes: src/engine/classes/write.rs, DTO application/dto/class.rs:14-60
  #  - POST /v1/documents (sınıflı): SaveDocumentRequest application/dto/document.rs:11-30
  #  - zorunlu özellik: src/engine/doc_management_engine.rs:289-296 "missing required property"
  #  - tip dönüşümü: src/engine/property_value_codec.rs:15-55
  @CLS-01 @critical
  Scenario: CLS-01 Zorunlu özellikli sınıfla yüklenen belge özellik doğrulamasından geçmelidir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sözleşme klasörü" oluşturulmuştur
    And yönetici zorunlu "sozlesme_no" metin ve "tutar" sayısal özellikleri olan "sözleşme sınıfı" sınıfını oluşturmuştur
    When yönetici "sözleşme sınıfı" sınıfıyla "sözleşme klasörü" klasörüne özellik vermeden belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "missing required property 'sozlesme_no'" içermelidir
    When yönetici "sözleşme sınıfı" sınıfıyla "sözleşme klasörü" klasörüne "tutar" özelliği sayı olmayan belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    When yönetici "sözleşme sınıfı" sınıfıyla "sözleşme klasörü" klasörüne geçerli özelliklerle "sözleşme belgesi" belgesini yükler
    Then son API yanıtının durum kodu 201 olmalıdır
    And "sözleşme belgesi" belgesi "sözleşme sınıfı" sınıfına bağlı olmalıdır
    And "sözleşme belgesi" belgesinin "sozlesme_no" özelliği "SZ-001" olmalıdır

  # ---------------------------------------------------------------------
  # Batch 27 — Classes, reclassification, properties, rules, class ACL
  # ---------------------------------------------------------------------

  # ECM kaynağı: service.rs check_class_access (aynı seviyede kullanıcı DENY grup ALLOW'u ezer)
  @batch27 @ACL-13
  Scenario: ACL-13 Sınıfta kullanıcı DENY grup ALLOW'u ezer ve sınıflı belge o kullanıcıdan gizlenir
    Given yönetici API oturumu açmıştır
    And "engelli" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "diğer okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "engelli" API ile giriş yapmıştır
    And "diğer okur" API ile giriş yapmıştır
    And yönetici "" özellikleriyle "herkese açık sınıf" sınıfını oluşturmuştur
    And "herkese açık sınıf" sınıfında "All Users" grubuna "viewer" rolü verilmiştir
    And "herkese açık sınıf" sınıfında "engelli" kullanıcısı için "viewer" rolü reddedilmiştir
    And yetki devralması açık "ortak klasör" oluşturulmuştur
    And "yönetici" "herkese açık sınıf" sınıfıyla "ortak klasör" klasörüne "{}" metadatasıyla "sınıflı belge" belgesini yüklemiştir
    When "engelli" "sınıflı belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "engelli" belge aramasında "sınıflı belge" belgesini görmemelidir
    When "diğer okur" "sınıflı belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 200 olmalıdır

  # ECM kaynağı: engine/documents/reclassify.rs (hedef sınıfın zorunlu alanları; sınıfsız belge -> classify)
  @batch27 @CLS-02
  Scenario: CLS-02 Sınıflı belge başka sınıfa yeniden sınıflandırılır ve hedef sınıfın zorunlu alanları uygulanır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "ref:text:zorunlu" özellikleriyle "kaynak sınıf" sınıfını oluşturmuştur
    And yönetici "no:text:zorunlu" özellikleriyle "hedef sınıf" sınıfını oluşturmuştur
    And "yönetici" "kaynak sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"R-1\"}" metadatasıyla "sınıflı belge" belgesini yüklemiştir
    And yönetici "sınıf klasörü" klasörüne "sınıfsız belge" belgesini yüklemiştir
    When yönetici "sınıflı belge" belgesini "hedef sınıf" sınıfına "{}" metadatasıyla yeniden sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "missing required property 'no'" içermelidir
    And "sınıflı belge" belgesi "kaynak sınıf" sınıfında olmalı ve metadatası "{\"ref\": \"R-1\"}" olmalıdır
    When yönetici "sınıflı belge" belgesini "hedef sınıf" sınıfına "{\"no\": \"B-1\"}" metadatasıyla yeniden sınıflandırmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "sınıflı belge" belgesi "hedef sınıf" sınıfında olmalı ve metadatası "{\"no\": \"B-1\"}" olmalıdır
    When yönetici "sınıfsız belge" belgesini "hedef sınıf" sınıfına "{\"no\": \"X\"}" metadatasıyla yeniden sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "use classify instead of reclassify" içermelidir
    And "sınıfsız belge" belgesi sınıfsız olmalıdır

  # ECM kaynağı: class_version iyimser kilidi ("This class was updated to version N since you opened the form")
  @batch27 @CLS-03
  Scenario: CLS-03 Sınıf tanımı güncellendikten sonra eski sınıf sürümüyle sınıflandırma ve yükleme reddedilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "ref:text:zorunlu" özellikleriyle "değişen sınıf" sınıfını oluşturmuştur
    And yönetici "sınıf klasörü" klasörüne "sınıfsız belge" belgesini yüklemiştir
    When yönetici "değişen sınıf" sınıfına "ek" özelliğini "text" türünde zorunlu olarak eklemeyi dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "değişen sınıf" sınıfının sürümü artmış olmalıdır
    When yönetici "sınıfsız belge" belgesini "değişen sınıf" sınıfına eski sınıf sürümü ve "{\"ref\": \"a\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "since you opened the form" içermelidir
    And "sınıfsız belge" belgesi sınıfsız olmalıdır
    When yönetici "değişen sınıf" sınıfıyla "sınıf klasörü" klasörüne eski sınıf sürümü ve "{\"ref\": \"a\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "since you opened the form" içermelidir

  # ECM kaynağı: pasif sınıf yeni kullanıma kapalı ("This class is inactive and cannot be used for new documents.")
  @batch27 @CLS-04
  Scenario: CLS-04 Sınıf pasifleştirilince mevcut belgeler kullanılabilir kalır, yeni yükleme engellenir, yeniden aktifleştirme çalışır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "ref:text:zorunlu" özellikleriyle "pasifleşen sınıf" sınıfını oluşturmuştur
    And "yönetici" "pasifleşen sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"x\"}" metadatasıyla "mevcut belge" belgesini yüklemiştir
    And "mevcut belge" belgesinin mevcut durumu kaydedilmiştir
    When yönetici "pasifleşen sınıf" sınıfını pasifleştirir
    Then "yönetici" "mevcut belge" belgesini okuyup içeriğini indirebilmelidir
    And "yönetici" "mevcut belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
    And "mevcut belge" belgesinin başlığı yeniden adlandırılmış olmalıdır
    When "yönetici" "pasifleşen sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"y\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This class is inactive and cannot be used for new documents." içermelidir
    When yönetici "pasifleşen sınıf" sınıfını tekrar aktifleştirir
    And "yönetici" "pasifleşen sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"z\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 201 olmalıdır

  # ECM kaynağı: folders allowed-classes (yükleme ve sınıflandırmada izinli sınıf kontrolü)
  @batch27 @CLS-05
  Scenario: CLS-05 Klasörün izinli sınıfları tanımlıysa izinli olmayan sınıfla yükleme ve sınıflandırma reddedilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "kısıtlı klasör" oluşturulmuştur
    And yönetici "" özellikleriyle "izinli sınıf" sınıfını oluşturmuştur
    And yönetici "" özellikleriyle "izinsiz sınıf" sınıfını oluşturmuştur
    And yönetici "kısıtlı klasör" klasörünün izinli sınıflarını yalnız "izinli sınıf" olarak ayarlamıştır
    And yönetici "kısıtlı klasör" klasörüne "sınıfsız belge" belgesini yüklemiştir
    When "yönetici" "izinsiz sınıf" sınıfıyla "kısıtlı klasör" klasörüne "{}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This folder only accepts" içermelidir
    When "yönetici" "sınıfsız belge" belgesini "izinsiz sınıf" sınıfına "{}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And "sınıfsız belge" belgesi sınıfsız olmalıdır
    When "yönetici" "izinli sınıf" sınıfıyla "kısıtlı klasör" klasörüne "{}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 201 olmalıdır

  # ECM kaynağı: README — "POST /classes/{id}/properties: always optional; never retroactively required";
  # engine/classes/write.rs add_property required=false. Plan beklentisi (yeni kayıt alanı ister) belgelenmiş kuralla çelişir.
  @batch27 @CLS-07
  Scenario: CLS-07 Kullanımdaki sınıfa sonradan eklenen özellik isteğe bağlı kaydedilir, mevcut ve yeni belgeler çalışmaya devam eder
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "ref:text:zorunlu" özellikleriyle "kullanılan sınıf" sınıfını oluşturmuştur
    And "yönetici" "kullanılan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"e\"}" metadatasıyla "eski belge" belgesini yüklemiştir
    When yönetici "kullanılan sınıf" sınıfına "tarih" özelliğini "date" türünde zorunlu olarak eklemeyi dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "kullanılan sınıf" sınıfındaki "tarih" özelliği isteğe bağlı olarak kaydedilmiş olmalıdır
    And "yönetici" "eski belge" belgesini okuyup içeriğini indirebilmelidir
    When "yönetici" "kullanılan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"e2\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 201 olmalıdır
    When "yönetici" "kullanılan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"e3\", \"tarih\": \"2026-13-40\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "must be a valid date" içermelidir
    When "yönetici" "kullanılan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{\"ref\": \"e4\", \"tarih\": \"2026-01-31\"}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 201 olmalıdır

  # ECM kaynağı: README deprecation (zorunluluk ve kurallar yeni kayıtta atlanır; mevcut değerler korunur; geri alınabilir)
  @batch27 @CLS-08
  Scenario: CLS-08 Kullanımdan kaldırılan zorunlu özellik yeni kayıtlarda istenmez, mevcut değer korunur ve geri alınabilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "no:text:zorunlu" özellikleriyle "emekli alan sınıfı" sınıfını oluşturmuştur
    And "yönetici" "emekli alan sınıfı" sınıfıyla "sınıf klasörü" klasörüne "{\"no\": \"X\"}" metadatasıyla "eski belge" belgesini yüklemiştir
    When yönetici "emekli alan sınıfı" sınıfının "no" özelliğini kullanımdan kaldırır
    And "yönetici" "emekli alan sınıfı" sınıfıyla "sınıf klasörü" klasörüne "{}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 201 olmalıdır
    And "eski belge" belgesi "emekli alan sınıfı" sınıfında olmalı ve metadatası "{\"no\": \"X\"}" olmalıdır
    When yönetici "emekli alan sınıfı" sınıfının "no" özelliğini tekrar kullanıma alır
    And "yönetici" "emekli alan sınıfı" sınıfıyla "sınıf klasörü" klasörüne "{}" metadatasıyla belge yüklemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "missing required property 'no'" içermelidir

  # ECM kaynağı: validation-constraints (min_length, max_length, allowed_values, max_value) + tarih biçimi
  @batch27 @CLS-09
  Scenario: CLS-09 En kısa, en uzun, izinli değerler, en büyük değer ve tarih kuralları sınıflandırmada uygulanır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "kod:text:zorunlu,adet:numeric:zorunlu,tarih:date:zorunlu" özellikleriyle "kurallı sınıf" sınıfını oluşturmuştur
    And yönetici "kurallı sınıf" sınıfının "kod" özelliğine "min_length" kuralını "3" değeriyle eklemiştir
    And yönetici "kurallı sınıf" sınıfının "kod" özelliğine "max_length" kuralını "5" değeriyle eklemiştir
    And yönetici "kurallı sınıf" sınıfının "kod" özelliğine "allowed_values" kuralını "[\"AAA\", \"BBB\", \"ABCDE\"]" değeriyle eklemiştir
    And yönetici "kurallı sınıf" sınıfının "adet" özelliğine "max_value" kuralını "10" değeriyle eklemiştir
    And yönetici "sınıf klasörü" klasörüne "sınıflandırılacak belge" belgesini yüklemiştir
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"AB\", \"adet\": 1, \"tarih\": \"2026-01-01\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının ihlal listesi "kod" özelliği için "min_length" kuralını içermelidir
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"ABCDEF\", \"adet\": 1, \"tarih\": \"2026-01-01\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının ihlal listesi "kod" özelliği için "max_length" kuralını içermelidir
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"CCC\", \"adet\": 1, \"tarih\": \"2026-01-01\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının ihlal listesi "kod" özelliği için "allowed_values" kuralını içermelidir
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"AAA\", \"adet\": 11, \"tarih\": \"2026-01-01\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının ihlal listesi "adet" özelliği için "max_value" kuralını içermelidir
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"AAA\", \"adet\": 1, \"tarih\": \"2026-13-01\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "must be a valid date string (YYYY-MM-DD)" içermelidir
    And "sınıflandırılacak belge" belgesi sınıfsız olmalıdır
    When "yönetici" "sınıflandırılacak belge" belgesini "kurallı sınıf" sınıfına "{\"kod\": \"AAA\", \"adet\": 10, \"tarih\": \"2026-01-31\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "sınıflandırılacak belge" belgesi "kurallı sınıf" sınıfında olmalı ve metadatası "{\"kod\": \"AAA\", \"adet\": 10, \"tarih\": \"2026-01-31\"}" olmalıdır

  # ECM kaynağı: sınıf ACL satırı kaldırılınca sınıf sert sınırı yeniden uygulanır
  @batch27 @CLS-12
  Scenario: CLS-12 Sınıf okuma izni kaldırılınca sınıflı belgeler kullanıcıdan yeniden gizlenir
    Given yönetici API oturumu açmıştır
    And "sınıf okuru" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "sınıf okuru" API ile giriş yapmıştır
    And yönetici "" özellikleriyle "kısıtlı sınıf" sınıfını oluşturmuştur
    And "kısıtlı sınıf" sınıfında "sınıf okuru" kullanıcısına "viewer" rolü verilmiştir
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And "sınıf klasörü" klasöründe "sınıf okuru" kullanıcısına "viewer" rolü verilmiştir
    And "yönetici" "kısıtlı sınıf" sınıfıyla "sınıf klasörü" klasörüne "{}" metadatasıyla "sınıflı belge" belgesini yüklemiştir
    And "sınıf okuru" belge aramasında "sınıflı belge" belgesini görmelidir
    When yönetici "kısıtlı sınıf" sınıfındaki "sınıf okuru" kullanıcısının izin satırını kaldırır
    And "sınıf okuru" "sınıflı belge" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "sınıf okuru" belge aramasında "sınıflı belge" belgesini görmemelidir

  # ECM kaynağı: engine/classes delete_class (yalnız çöpteki belgeler sınıfı kullanıyorsa FK hatası -> 500 internal_error);
  # ClassDetail.tsx Delete butonunu canlı belge listesi boşsa gösterir.
  @batch27 @CLS-15 @known-bug
  Scenario: CLS-15 Yalnız soft delete edilmiş belgelerin kullandığı sınıf silinmeye çalışılınca kontrollü 409 dönmeli, 500 değil
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "" özellikleriyle "çöpte kullanılan sınıf" sınıfını oluşturmuştur
    And "yönetici" "çöpte kullanılan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{}" metadatasıyla "çöpteki sınıflı belge" belgesini yüklemiştir
    And "çöpteki sınıflı belge" belgesi API ile soft delete edilmiştir
    When yönetici "çöpte kullanılan sınıf" sınıfını silmeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And "çöpte kullanılan sınıf" sınıfı hâlâ bulunmalıdır
    And yönetici arayüzde "çöpte kullanılan sınıf" sınıfının detayında Delete butonunu görmemelidir

  # ECM kaynağı: runtime.rs can_access_class (manager için bireysel class allow yazma saymaz; grup grant'i sayar)
  @batch27 @RBAC-07
  Scenario: RBAC-07 Manager'a bireysel verilen sınıf izni sınıflandırma için sayılmaz, ekip grubuna verilen izin sayılır
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yönetici "" özellikleriyle "ekip sınıfı" sınıfını oluşturmuştur
    And "ekip sınıfı" sınıfında "ekip yöneticisi" kullanıcısına "editor" rolü verilmiştir
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    And "ekip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And "ekip klasörü" klasöründe "ekip yöneticisi" kullanıcısına "manager" rolü verilmiştir
    And "ekip yöneticisi" "ekip klasörü" klasörüne "manager belgesi" belgesini yüklemiştir
    When "ekip yöneticisi" "manager belgesi" belgesini "ekip sınıfı" sınıfına "{}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "manager belgesi" belgesi sınıfsız olmalıdır
    Given "ekip sınıfı" sınıfında "ekip" grubuna "editor" rolü verilmiştir
    When "ekip yöneticisi" "manager belgesi" belgesini "ekip sınıfı" sınıfına "{}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "manager belgesi" belgesi "ekip sınıfı" sınıfında olmalı ve metadatası "{}" olmalıdır

  # ECM kaynağı: validation rule silme -> kural artık uygulanmaz
  @batch27 @CLS-11
  Scenario: CLS-11 Silinen doğrulama kuralı artık uygulanmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And yönetici "kod:text:zorunlu" özellikleriyle "regex sınıfı" sınıfını oluşturmuştur
    And yönetici "regex sınıfı" sınıfının "kod" özelliğine "regex" kuralını "{\"pattern\": \"^Z\", \"full_match\": false}" değeriyle eklemiştir
    And yönetici "sınıf klasörü" klasörüne "sınıflandırılacak belge" belgesini yüklemiştir
    When "yönetici" "sınıflandırılacak belge" belgesini "regex sınıfı" sınıfına "{\"kod\": \"A1\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının ihlal listesi "kod" özelliği için "regex" kuralını içermelidir
    When yönetici "regex sınıfı" sınıfının "kod" özelliğindeki "regex" kuralını siler
    And "yönetici" "sınıflandırılacak belge" belgesini "regex sınıfı" sınıfına "{\"kod\": \"A1\"}" metadatasıyla sınıflandırmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "sınıflandırılacak belge" belgesi "regex sınıfı" sınıfında olmalı ve metadatası "{\"kod\": \"A1\"}" olmalıdır

  # ECM kaynağı: list_classes (read) ve ?for=upload (write) ACL filtreleri
  @batch27 @CLS-13
  Scenario: CLS-13 Sınıf listesi normal kullanıcıya okuyabildiği sınıfları, yükleme listesi yalnız yazabildiklerini döndürür
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "başka kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    And yönetici "" özellikleriyle "okunur sınıf" sınıfını oluşturmuştur
    And yönetici "" özellikleriyle "yazılır sınıf" sınıfını oluşturmuştur
    And yönetici "" özellikleriyle "yabancı sınıf" sınıfını oluşturmuştur
    And "okunur sınıf" sınıfında "test kullanıcısı" kullanıcısına "viewer" rolü verilmiştir
    And "yazılır sınıf" sınıfında "test kullanıcısı" kullanıcısına "editor" rolü verilmiştir
    And "yabancı sınıf" sınıfında "başka kullanıcı" kullanıcısına "viewer" rolü verilmiştir
    Then "test kullanıcısı" sınıf listesinde "okunur sınıf" sınıfını görmelidir
    And "test kullanıcısı" sınıf listesinde "yazılır sınıf" sınıfını görmelidir
    And "test kullanıcısı" sınıf listesinde "yabancı sınıf" sınıfını görmemelidir
    And "test kullanıcısı" yükleme sınıf listesinde "yazılır sınıf" sınıfını görmelidir
    And "test kullanıcısı" yükleme sınıf listesinde "okunur sınıf" sınıfını görmemelidir
    And "test kullanıcısı" yükleme sınıf listesinde "yabancı sınıf" sınıfını görmemelidir

  # ECM kaynağı: engine/classes/write.rs update_property_display_name
  @batch27 @CLS-16
  Scenario: CLS-16 Özellik görünen adı güncellenir, boş ad reddedilir, var olmayan özellik 404 döner
    Given yönetici API oturumu açmıştır
    And yönetici "p:text:opsiyonel" özellikleriyle "etiketli sınıf" sınıfını oluşturmuştur
    When yönetici "etiketli sınıf" sınıfının "p" özelliğinin görünen adını "QA Yeni Ad" yapmayı dener
    Then son API yanıtının durum kodu 200 olmalıdır
    And "etiketli sınıf" sınıfının "p" özelliğinin görünen adı "QA Yeni Ad" olmalıdır
    When yönetici "etiketli sınıf" sınıfının "p" özelliğinin görünen adını "" yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "property display_name cannot be empty" içermelidir
    And "etiketli sınıf" sınıfının "p" özelliğinin görünen adı "QA Yeni Ad" olmalıdır
    When yönetici "etiketli sınıf" sınıfında var olmayan bir özelliğin görünen adını değiştirmeyi dener
    Then son API yanıtının durum kodu 404 olmalıdır
