@coverage-gap @validation
Feature: Doğrulama kuralları ve sınır değerleri

  # ---------------------------------------------------------------------
  # Batch 32 — Validation & boundaries (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # Her satır: istek -> beklenen durum ve hata mesajı; ardından kalıcı durum doğrulanır.
  # ---------------------------------------------------------------------

  # ECM kaynağı: acl grant doğrulaması (tam olarak bir ilke; var olan rol)
  @batch32 @ACL-08
  Scenario: ACL-08 ACL isteğinde tam olarak bir ilke olmalı ve rol var olmalıdır
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And "hedef kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "acl klasörü" oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                         | gövde                                                                                                                     | durum | mesaj                                         |
      | POST /v1/folders/{acl klasörü}/acl | {"role_id":"22222222-0000-0000-0000-000000000004","user_id":"{hedef kullanıcı}","group_id":"44444444-0000-0000-0000-000000000001","deny":false} | 422   | exactly one of user_id or group_id must be set |
      | POST /v1/folders/{acl klasörü}/acl | {"role_id":"22222222-0000-0000-0000-000000000004","deny":false}                                                       | 422   | exactly one of user_id or group_id must be set |
      | POST /v1/folders/{acl klasörü}/acl | {"role_id":"{RANDOM_ID}","user_id":"{hedef kullanıcı}","deny":false}                                                  | 422   | does not exist                                 |
    And "acl klasörü" klasöründe hiç izin satırı bulunmamalıdır

  # ECM kaynağı: engine/admin.rs hash_password (MIN_PASSWORD_LEN = 8)
  @batch32 @AUTH-06
  Scenario: AUTH-06 8 karakterden kısa parola ile kullanıcı oluşturulamaz, 8 karakter kabul edilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek          | gövde                                                                                                 | durum | mesaj                                   |
      | POST /v1/users | {"email":"qa.{M}@example.com","password":"Kisa1!x","full_name":"QA","role":"user","is_active":true}  | 422   | Password must be at least 8 characters. |
    And işaretli e-postayla aktif kullanıcı bulunmamalıdır
    And aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek          | gövde                                                                                                 | durum | mesaj |
      | POST /v1/users | {"email":"qa.{M}@example.com","password":"Kisa1!xy","full_name":"QA","role":"user","is_active":true} | 200   | -     |

  # ECM kaynağı: rename_document (1..255, kırpma)
  @batch32 @DOC-14
  Scenario: DOC-14 Başlık 255 karakteri aşamaz, 255 kabul edilir, baş ve son boşluklar kırpılır
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "başlık klasörü" oluşturulmuştur
    And yönetici "başlık klasörü" klasörüne "başlık belgesi" belgesini yüklemiştir
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                   | gövde                          | durum | mesaj                                 |
      | PATCH /v1/documents/{başlık belgesi}/title | {"title":"{TEKRAR:a:256}"}  | 422   | Title must be at most 255 characters. |
      | PATCH /v1/documents/{başlık belgesi}/title | {"title":"   "}             | 422   | Title must not be empty.              |
      | PATCH /v1/documents/{başlık belgesi}/title | {"title":"{TEKRAR:b:255}"}  | 200   | -                                     |
    And "başlık belgesi" belgesinin başlığı "{TEKRAR:b:255}" olmalıdır
    And aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                   | gövde                          | durum | mesaj |
      | PATCH /v1/documents/{başlık belgesi}/title | {"title":"  Başlık {M}  "}  | 200   | -     |
    And "başlık belgesi" belgesinin başlığı "Başlık {M}" olmalıdır

  # ECM kaynağı: validate_folder_name ('.', '..', '/', boş, NUL, 255)
  @batch32 @FLD-02
  Scenario: FLD-02 Klasör adı nokta, iki nokta, eğik çizgi, boşluk, NUL karakteri içeremez ve 255 karakteri aşamaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "üst klasör" oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek            | gövde                                                                  | durum | mesaj                                  |
      | POST /v1/folders | {"name":".","parent_id":"{üst klasör}","inherit_permissions":true}     | 422   | Folder name cannot be '.' or '..'.     |
      | POST /v1/folders | {"name":"..","parent_id":"{üst klasör}","inherit_permissions":true}    | 422   | Folder name cannot be '.' or '..'.     |
      | POST /v1/folders | {"name":"a/b","parent_id":"{üst klasör}","inherit_permissions":true}   | 422   | Folder name cannot contain '/'.        |
      | POST /v1/folders | {"name":"   ","parent_id":"{üst klasör}","inherit_permissions":true}   | 422   | Folder name cannot be empty.           |
      | POST /v1/folders | {"name":"a\\u0000b","parent_id":"{üst klasör}","inherit_permissions":true} | 422 | Folder name cannot contain null characters. |
      | POST /v1/folders | {"name":"{TEKRAR:x:256}","parent_id":"{üst klasör}","inherit_permissions":true} | 422 | Folder name cannot exceed 255 characters |
    And "üst klasör" klasörünün alt klasör sayısı 0 olmalıdır
    And aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek            | gövde                                                                         | durum | mesaj |
      | POST /v1/folders | {"name":"{TEKRAR:y:255}","parent_id":"{üst klasör}","inherit_permissions":true} | 200 | -     |
    And "üst klasör" klasörünün alt klasör sayısı 1 olmalıdır

  # ECM kaynağı: auth.rs register + engine create_user_inner
  @batch32 @AUTH-08
  Scenario: AUTH-08 Kayıtta boş alan, geçersiz e-posta, mevcut e-posta ve kısa parola reddedilir, hesap oluşmaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And "mevcut kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | oturum | istek                  | gövde                                                        | durum | mesaj                                   |
      | anonim | POST /v1/auth/register | {"email":"","password":"Passw0rd!"}                          | 422   | email and password are required         |
      | anonim | POST /v1/auth/register | {"email":"a@b","password":"Passw0rd!"}                       | 422   | is not a valid email address            |
      | anonim | POST /v1/auth/register | {"email":"{EPOSTA:mevcut kullanıcı}","password":"Passw0rd!"} | 422   | A user with this email already exists.  |
      | anonim | POST /v1/auth/register | {"email":"qa.{M}@example.com","password":"kisa"}             | 422   | Password must be at least 8 characters. |
    And işaretli e-postayla aktif kullanıcı bulunmamalıdır

  # ECM kaynağı: engine/classes/write.rs create_class / add_property (boş ad, tekil ad, yinelenen anahtar).
  # Sınıf adı büyük/küçük harfe duyarlı tekildir (yalnız grup/rol adları 0039 ile duyarsız) — RQ-24.
  @batch32 @CLS-06
  Scenario: CLS-06 Sınıf adı boş olamaz ve tekildir, yinelenen, boş anahtar ve boş görünen ad reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yönetici "t:text:opsiyonel" özellikleriyle "mevcut sınıf" sınıfını oluşturmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                       | gövde                                                                                                                                                                                                  | durum | mesaj                                   |
      | POST /v1/classes                            | {"name":"","properties":[]}                                                                                                                                                                            | 422   | class name cannot be empty              |
      | POST /v1/classes                            | {"name":"{AD:mevcut sınıf}","properties":[]}                                                                                                                                                           | 409   | A class with this name already exists.  |
      | POST /v1/classes                            | {"name":"QA {M}","properties":[{"key":"ref","display_name":"R","data_type":"text","is_multi_value":false,"required":false},{"key":"ref","display_name":"R2","data_type":"text","is_multi_value":false,"required":false}]} | 422 | Duplicate property key 'ref' |
      | POST /v1/classes/{mevcut sınıf}/properties  | {"key":"","display_name":"X","data_type":"text","is_multi_value":false,"required":false}                                                                                                              | 422   | property key cannot be empty            |
      | POST /v1/classes/{mevcut sınıf}/properties  | {"key":"k","display_name":"","data_type":"text","is_multi_value":false,"required":false}                                                                                                              | 422   | property display_name cannot be empty   |
      | POST /v1/classes/{mevcut sınıf}/properties  | {"key":"t","display_name":"T2","data_type":"text","is_multi_value":false,"required":false}                                                                                                            | 422   | Duplicate property key 't'              |

  # ECM kaynağı: validation rule doğrulamaları (tip, türe uygunluk, mesaj, regex derleme/uzunluk)
  @batch32 @CLS-10
  Scenario: CLS-10 Geçersiz kural tipi, türe uymayan kural, boş hata mesajı ve geçersiz ya da uzun regex reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yönetici "t:text:opsiyonel,n:numeric:opsiyonel" özellikleriyle "kural sınıfı" sınıfını oluşturmuştur
    And "kural sınıfı" sınıfının özellikleri "metin özelliği" ve "sayı özelliği" olarak adlandırılmıştır
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                                                  | gövde                                                                          | durum | mesaj                                               |
      | POST /v1/classes/{kural sınıfı}/properties/{metin özelliği}/rules      | {"constraint_type":"foo","constraint_value":1,"error_message":"m"}             | 422   | Invalid constraint_type 'foo'.                      |
      | POST /v1/classes/{kural sınıfı}/properties/{sayı özelliği}/rules       | {"constraint_type":"regex","constraint_value":"^a","error_message":"m"}        | 422   | Constraint 'regex' is not available for numeric     |
      | POST /v1/classes/{kural sınıfı}/properties/{metin özelliği}/rules      | {"constraint_type":"max_length","constraint_value":3,"error_message":""}       | 422   | error_message is required.                          |
      | POST /v1/classes/{kural sınıfı}/properties/{metin özelliği}/rules      | {"constraint_type":"regex","constraint_value":"(","error_message":"m"}         | 422   | Invalid regular expression                          |
      | POST /v1/classes/{kural sınıfı}/properties/{metin özelliği}/rules      | {"constraint_type":"regex","constraint_value":"{TEKRAR:a:513}","error_message":"m"} | 422 | Regex pattern must be at most 512 characters.  |
    And "kural sınıfı" sınıfının kuralı olmamalıdır

  # ECM kaynağı: klasör kardeş adı tekilliği (0017 idx_folder_name_parent; büyük/küçük harfe duyarlı — RQ-24)
  @batch32 @FLD-06
  Scenario: FLD-06 Klasör aynı üst altında var olan bir adla yeniden adlandırılamaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "üst klasör" oluşturulmuştur
    And "üst klasör" altında yetki devralması açık "alfa klasörü" oluşturulmuştur
    And "üst klasör" altında yetki devralması açık "beta klasörü" oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                            | gövde                         | durum | mesaj                                    |
      | PATCH /v1/folders/{beta klasörü} | {"name":"{AD:alfa klasörü}"} | 409   | A folder with this name already exists. |
    And "üst klasör" klasörünün alt klasör sayısı 2 olmalıdır

  # ECM kaynağı: ValidPath (bozuk UUID 422), NotFound 404; ham hata metni yok
  @batch32 @FLD-07
  Scenario: FLD-07 Var olmayan kaynak 404, bozuk kimlik 422 döner, sunucu hatası olmaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                          | gövde | durum | mesaj                             |
      | GET /v1/folders/{RANDOM_ID}    |       | 404   | Not found                         |
      | GET /v1/folders/abc            |       | 422   | The resource ID in the URL is not valid |
      | GET /v1/documents/{RANDOM_ID}  |       | 404   | Not found                         |
      | GET /v1/documents/abc          |       | 422   | The resource ID in the URL is not valid |
      | GET /v1/classes/abc            |       | 422   | The resource ID in the URL is not valid |
      | GET /v1/groups/{RANDOM_ID}     |       | 404   | -                                 |

  # ECM kaynağı: tag atama (en az bir tanımlayıcı; normalized_name ile harf duyarsız tekil)
  @batch32 @TAG-03
  Scenario: TAG-03 Etiket isteğinde tanımlayıcı zorunludur, aynı etiket harf farkıyla ikinci kez eklenmez
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And yönetici "etiket klasörü" klasörüne "etiketli belge" belgesini yüklemiştir
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                  | gövde                                   | durum | mesaj                                                   |
      | POST /v1/documents/{etiketli belge}/tags | {}                                    | 422   | at least one of tag_ids, tag_id, or tag_name must be set |
      | POST /v1/documents/{etiketli belge}/tags | {"tag_ids":[],"tag_name":"Proje{M}"}  | 201   | -                                                       |
      | POST /v1/documents/{etiketli belge}/tags | {"tag_ids":[],"tag_name":"proje{M}"}  | 201\|409 | -                                                    |
    And "etiketli belge" belgesinin etiketleri tam olarak "Proje{M}" olmalıdır
    And etiket kataloğunda "PROJE{M}" için oluşturma isteği mevcut etiketi döndürmeli ve yinelenen etiket oluşmamalıdır

  # ECM kaynağı: domain/document.rs label kuralı (1..100, ASCII harf/rakam/boşluk/-/_/.), tekrar idempotent
  @batch32 @VER-06
  Scenario: VER-06 Sürüm etiketi 1-100 karakter ve izinli karakterlerle sınırlıdır, aynı etiket ikinci kez eklenmez
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And yönetici "etiket klasörü" klasörüne "sürüm belgesi" belgesini yüklemiştir
    And "sürüm belgesi" belgesinin tek sürümü "ilk sürüm" olarak adlandırılmıştır
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                                                              | gövde                          | durum | mesaj                                  |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":""}                   | 422   | label cannot be empty                  |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":"{TEKRAR:a:101}"}     | 422   | label cannot exceed 100 characters     |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":"a/b"}                | 422   | label may only contain                 |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":"v1.0-rc_1"}          | 201   | -                                      |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":"v1.0-rc_1"}          | 2xx   | -                                      |
      | POST /v1/documents/{sürüm belgesi}/versions/{ilk sürüm}/labels     | {"label":"{TEKRAR:c:100}"}     | 201   | -                                      |
    And "sürüm belgesi" belgesinin güncel sürüm etiketleri "v1.0-rc_1, {TEKRAR:c:100}" olmalıdır

  # ECM kaynağı: UTF-8 saklama ve arama; etiket adı ASCII sınırı RQ-22 kanıtı
  @batch32 @DATA-02
  Scenario: DATA-02 Türkçe karakterli klasör adı, özellik değeri ve kayıtlı arama adı korunur ve aranabilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    When yönetici Türkçe adlarla klasör, sınıf özellik değeri ve kayıtlı arama oluşturur
    Then Türkçe değerler bozulmadan okunmalı ve aranabilmelidir
