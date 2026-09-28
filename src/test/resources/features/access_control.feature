Feature: ECM yetkilendirme, kullanıcı yaşam döngüsü, sınıf ve statü kuralları (Batch 19)

  # Kaynak kurallar (ECM, salt okunur): ROLES.md, engine/acl/runtime.rs,
  # engine/acl/service.rs, api/rest/auth.rs (require_auth), engine/documents/admin.rs
  # (statü durum makinesi), engine/classes/validation.rs.
  #
  # İzolasyon: root klasör "All Users" grubuna viewer verir (seed). Bu yüzden
  # yetki senaryoları kalıtımı KAPALI klasörler ve senaryoya özel kullanıcılarla
  # kurulur; her isim/e-posta TestData ile koşuya ve worker'a özgüdür.

  # ---------------------------------------------------------------------------
  # ACL - izolasyon ve sızıntı
  # ---------------------------------------------------------------------------

  @batch19 @acl @security
  Scenario: B19-01 Kalıtımı kapalı klasördeki belge izinsiz kullanıcıya hiçbir belge API'si ile açılmaz
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    Then "ayse" kullanıcısı "Sozlesme" belgesine hiçbir belge API'si ile erişememelidir
    And "ayse" kullanıcısı "Gizli" klasörüne erişememelidir

  @batch19 @acl @security
  Scenario: B19-02 İzinsiz kullanıcı gizli belgeyi arayüzde açamaz ve oturumu düşmez
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    When "ayse" kullanıcısı arayüzde "Sozlesme" belgesinin detay adresini açar
    Then arayüzde erişim reddi mesajı görüntülenmeli ve "Sozlesme" başlığı gösterilmemelidir

  @batch19 @acl @security
  Scenario: B19-03 Gizli belge liste, facet, etiket araması, başlık eşleşmesi ve klasör seçicilerinde sızmaz
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    And admin "Sozlesme" belgesine benzersiz bir etiket eklemiştir
    Then "Sozlesme" belgesi "ayse" kullanıcısının hiçbir liste ve arama sonucunda görünmemelidir
    And "Sozlesme" belgesi admin kullanıcısının liste ve etiket araması sonuçlarında görünmelidir

  @batch19 @acl
  Scenario: B19-04 Klasör okuma izni geri alındığında erişim aynı oturumda kapanır
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    When admin "Gizli" klasöründe "ayse" kullanıcısına "viewer" rolüyle izin verir
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyabilmelidir
    When admin "Gizli" klasöründeki "ayse" kullanıcısının iznini geri alır
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyamamalıdır

  @batch19 @acl @groups @b19-duplicate
  Scenario: B19-05 Grup üzerinden gelen erişim üyelik kaldırılınca aynı oturumda kapanır
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin "Hukuk" grubunu oluşturmuştur
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    And admin "Gizli" klasöründe "Hukuk" grubuna "viewer" rolüyle izin vermiştir
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyamamalıdır
    When admin "ayse" kullanıcısını "Hukuk" grubuna ekler
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyabilmelidir
    When admin "ayse" kullanıcısını "Hukuk" grubundan çıkarır
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyamamalıdır

  @batch19 @acl
  Scenario: B19-06 Belge seviyesindeki DENY klasör iznini ezer, klasördeki diğer belge okunabilir kalır
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    And admin "Gizli" klasörüne "Rapor" belgesini yüklemiştir
    And admin "Gizli" klasöründe "ayse" kullanıcısına "viewer" rolüyle izin vermiştir
    When admin "Sozlesme" belgesinde "ayse" kullanıcısına "viewer" rolüyle DENY tanımlar
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyamamalıdır
    And "ayse" kullanıcısı "Rapor" belgesini okuyabilmelidir

  @batch19 @acl @b19-duplicate
  Scenario: B19-07 Alt klasördeki DENY üst klasörden miras gelen izni ezer
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörü altında kalıtımı açık "Alt" klasörünü oluşturmuştur
    And admin "Gizli" klasörüne "Rapor" belgesini yüklemiştir
    And admin "Alt" klasörüne "AltBelge" belgesini yüklemiştir
    And admin "Gizli" klasöründe "ayse" kullanıcısına "viewer" rolüyle izin vermiştir
    Then "ayse" kullanıcısı "AltBelge" belgesini okuyabilmelidir
    When admin "Alt" klasöründe "ayse" kullanıcısına "viewer" rolüyle DENY tanımlar
    Then "ayse" kullanıcısı "AltBelge" belgesini okuyamamalıdır
    And "ayse" kullanıcısı "Rapor" belgesini okuyabilmelidir

  @batch19 @acl @b19-duplicate
  Scenario: B19-08 Alt klasörde izin kalıtımı kapatılınca üst izin düşer, açılınca geri gelir
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin kalıtımı kapalı "Gizli" klasörünü oluşturmuştur
    And admin "Gizli" klasörü altında kalıtımı açık "Alt" klasörünü oluşturmuştur
    And admin "Alt" klasörüne "AltBelge" belgesini yüklemiştir
    And admin "Gizli" klasöründe "ayse" kullanıcısına "viewer" rolüyle izin vermiştir
    Then "ayse" kullanıcısı "AltBelge" belgesini okuyabilmelidir
    When admin "Alt" klasörünün izin kalıtımını kapatır
    Then "ayse" kullanıcısı "AltBelge" belgesini okuyamamalıdır
    When admin "Alt" klasörünün izin kalıtımını açar
    Then "ayse" kullanıcısı "AltBelge" belgesini okuyabilmelidir

  @batch19 @acl @integrity
  # Belgeyi admin DEĞİL, klasör sahibi normal kullanıcı yazar: aksi halde ret
  # "admin-authored" korumasından gelir (DOC-01 bunu zaten kanıtlıyor) ve viewer
  # rolünün yazma izni vermediği kuralı hiç test edilmezdi.
  Scenario: B19-09 Viewer izni yazma işlemlerine izin vermez ve belge değişmeden kalır
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And "yazar" adında normal bir test kullanıcısı vardır
    And admin "Gizli" klasörünü oluşturup sahipliğini "yazar" kullanıcısına devretmiştir
    And "yazar" kullanıcısı "Gizli" klasörüne "Sozlesme" belgesini yüklemiştir
    And admin "Gizli" klasöründe "ayse" kullanıcısına "viewer" rolüyle izin vermiştir
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyabilmelidir
    And "ayse" kullanıcısının "Sozlesme" belgesindeki tüm yazma denemeleri reddedilmeli ve belge değişmemelidir

  @batch19 @acl @classes @security
  Scenario: B19-10 Sınıf ACL'i sert sınırdır: klasörü okuyabilen kullanıcı sınıf izni olmadan sınıflı belgeyi göremez
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin zorunlu metin alanı "ref" olan "Gizlilik" sınıfını oluşturmuştur
    And admin Root klasörüne "Sozlesme" belgesini yüklemiştir
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyabilmelidir
    When admin "Sozlesme" belgesini "Gizlilik" sınıfına geçerli metadata ile sınıflandırır
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyamamalıdır
    And "Sozlesme" belgesi "ayse" kullanıcısının belge listesinde görünmemelidir
    When admin "Gizlilik" sınıfında "ayse" kullanıcısına "viewer" rolüyle izin verir
    Then "ayse" kullanıcısı "Sozlesme" belgesini okuyabilmelidir

  # ---------------------------------------------------------------------------
  # Kullanıcı yaşam döngüsü ve yetki yükseltme
  # ---------------------------------------------------------------------------

  @batch19 @auth @security @b19-duplicate
  Scenario: B19-11 Pasifleştirilen kullanıcının açık oturumu anında reddedilir ve tekrar giriş yapamaz
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    Then "ayse" kullanıcısının açık oturumu geçerli olmalıdır
    When admin "ayse" kullanıcısını pasifleştirir
    Then "ayse" kullanıcısının açık oturumu hesap pasif hatasıyla reddedilmelidir
    And "ayse" kullanıcısı giriş yapamamalıdır
    When admin "ayse" kullanıcısını aktifleştirir
    Then "ayse" kullanıcısı giriş yapabilmelidir

  @batch19 @auth @security
  Scenario: B19-12 Normal kullanıcı yönetim işlemleriyle yetkisini yükseltemez
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And "mehmet" adında normal bir test kullanıcısı vardır
    And admin "Hukuk" grubunu oluşturmuştur
    Then "ayse" kullanıcısının tüm yönetici işlemi denemeleri reddedilmelidir
    And "ayse" kullanıcısının rolü "user" kalmalı ve "Hukuk" grubunda üye olmamalıdır
    And "mehmet" kullanıcısı aktif kalmalıdır

  @batch19 @auth @security @b19-duplicate
  Scenario: B19-13 Admin rolü geri alınan kullanıcı açık oturumunda admin yetkisini anında kaybeder
    Given admin API oturumu açılmıştır
    And "yonetici2" adında "admin" rolünde bir test kullanıcısı vardır
    Then "yonetici2" kullanıcısının açık oturumu yönetici işlemi yapabilmelidir
    When admin "yonetici2" kullanıcısının rolünü "user" yapar
    Then "yonetici2" kullanıcısının açık oturumu yönetici işlemi yapamamalıdır

  # ---------------------------------------------------------------------------
  # Checkout kilidi ve klasör sahipliği
  # ---------------------------------------------------------------------------

  @batch19 @lock @multiuser
  Scenario: B19-14 Başka kullanıcının checkout kilidi admin değişikliklerini de engeller, Copy serbesttir
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin "AyseKlasor" klasörünü oluşturup sahipliğini "ayse" kullanıcısına devretmiştir
    And "ayse" kullanıcısı "AyseKlasor" klasörüne "AyseBelge" belgesini yüklemiştir
    When "ayse" kullanıcısı "AyseBelge" belgesini checkout yapar
    Then admin'in "AyseBelge" belgesindeki tüm değişiklik denemeleri 409 ile reddedilmeli ve belge değişmemelidir
    And admin "AyseBelge" belgesinin kopyasını oluşturabilmelidir
    When admin "AyseBelge" belgesinin kilidini zorla kaldırır
    Then admin "AyseBelge" belgesini yeniden adlandırabilmelidir

  @batch19 @ownership @acl
  Scenario: B19-15 Klasör sahibi admin belgesini okuyabilir ama değiştiremez, kendi belgesini yönetir, hard delete yapamaz
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin "AyseKlasor" klasörünü oluşturup sahipliğini "ayse" kullanıcısına devretmiştir
    And admin "AyseKlasor" klasörüne "AdminBelgesi" belgesini yüklemiştir
    And "ayse" kullanıcısı "AyseKlasor" klasörüne "AyseBelge" belgesini yüklemiştir
    Then "ayse" kullanıcısı "AdminBelgesi" belgesini okuyabilmelidir
    And "ayse" kullanıcısı "AdminBelgesi" belgesini yeniden adlandıramamalıdır
    And "ayse" kullanıcısı "AyseBelge" belgesini yeniden adlandırabilmelidir
    And "ayse" kullanıcısı "AyseBelge" belgesini kalıcı olarak silememeli ve belge mevcut kalmalıdır

  @batch19 @ownership @groups
  Scenario: B19-16 Klasör sahibi yalnızca üyesi olduğu gruba izin verebilir
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin "Hukuk" grubunu oluşturmuştur
    And admin "Finans" grubunu oluşturmuştur
    And admin "ayse" kullanıcısını "Hukuk" grubuna ekler
    And admin "AyseKlasor" klasörünü oluşturup sahipliğini "ayse" kullanıcısına devretmiştir
    When "ayse" kullanıcısı "AyseKlasor" klasöründe "Finans" grubuna "viewer" rolüyle izin vermeyi dener
    Then istek 403 ile reddedilmelidir
    When "ayse" kullanıcısı "AyseKlasor" klasöründe "Hukuk" grubuna "viewer" rolüyle izin vermeyi dener
    Then istek başarılı olmalıdır
    And "AyseKlasor" klasörünün ACL listesinde "Finans" grubu bulunmamalıdır

  @batch19 @ownership @spec
  Scenario: B19-17 Klasör sahibi bireysel izni admin kullanıcısına veremez
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And admin "AyseKlasor" klasörünü oluşturup sahipliğini "ayse" kullanıcısına devretmiştir
    When "ayse" kullanıcısı "AyseKlasor" klasöründe admin kullanıcısına "viewer" rolüyle izin vermeyi dener
    Then istek reddedilmelidir
    And "AyseKlasor" klasörünün ACL listesinde admin kullanıcısı bulunmamalıdır

  # ---------------------------------------------------------------------------
  # Statü yaşam döngüsü
  # ---------------------------------------------------------------------------

  @batch19 @status @lifecycle @b19-duplicate
  Scenario: B19-18 Statü yaşam döngüsü geçiş kuralları uygulanır ve reddedilen geçiş statüyü değiştirmez
    Given admin API oturumu açılmıştır
    Then belge statü geçişleri aşağıdaki sonuçları vermelidir:
      | başlangıç    | hedef        | sonuç |
      | archived     | published    | 422   |
      | archived     | under_review | 422   |
      | published    | draft        | 422   |
      | archived     | draft        | 200   |
      | published    | under_review | 200   |
      | under_review | archived     | 200   |
      | draft        | bogus        | 422   |

  @batch19 @status @lifecycle
  Scenario: B19-19 Arayüzden yapılan geçersiz statü geçişi hata gösterir ve belge arşivde kalır
    Given admin arşivlenmiş yeni bir belgenin detay sayfasındadır
    When admin arayüzde statüyü "Published" olarak güncellemeyi dener
    Then arayüzde geçersiz statü geçişi hatası görüntülenmelidir
    And belgenin API statüsü "archived" kalmalıdır

  @batch19 @status @lifecycle @ui
  Scenario: B19-20 Arşivlenmiş belgenin statü seçiminde yalnızca geçerli geçişler sunulmalıdır
    Given admin arşivlenmiş yeni bir belgenin detay sayfasındadır
    Then statü seçiminde yalnızca "Draft,Archived" seçenekleri sunulmalıdır

  # ---------------------------------------------------------------------------
  # Sınıf, özellik ve doğrulama kuralları
  # ---------------------------------------------------------------------------

  @batch19 @classes @validation
  Scenario: B19-21 Sınıflandırmada zorunlu, regex ve en küçük değer kuralları uygulanır
    Given admin API oturumu açılmıştır
    And admin "Sozlesme" sınıfını zorunlu metin alanı "contract_no" ve sayısal alan "amount" ile oluşturmuştur
    And admin "Sozlesme" sınıfının "contract_no" alanına "^C-\d{4}$" regex kuralı eklemiştir
    And admin "Sozlesme" sınıfının "amount" alanına en küçük değer 0 kuralı eklemiştir
    And admin Root klasörüne "Belge" belgesini yüklemiştir
    Then "Belge" belgesinin "Sozlesme" sınıfına sınıflandırması aşağıdaki metadata ile reddedilmelidir:
      | metadata                                   | hata kodu                 |
      | {"amount": 5}                              | validation_error          |
      | {"contract_no": "X-1", "amount": 5}        | property_validation_error |
      | {"contract_no": "C-1234", "amount": -1}    | property_validation_error |
      | {"contract_no": "C-1234", "amount": "abc"} | validation_error          |
    And "Belge" belgesi sınıfsız kalmalıdır
    When admin "Belge" belgesini "Sozlesme" sınıfına {"contract_no": "C-1234", "amount": 10} metadata ile sınıflandırır
    Then "Belge" belgesi "Sozlesme" sınıfında ve "contract_no" değeri "C-1234" olarak kalıcı olmalıdır

  @batch19 @classes @lifecycle
  Scenario: B19-22 Pasif sınıfa sınıflandırma yapılamaz, belgesi olan sınıf silinemez
    Given admin API oturumu açılmıştır
    And admin zorunlu metin alanı "ref" olan "Pasif" sınıfını oluşturmuştur
    And admin zorunlu metin alanı "ref" olan "Dolu" sınıfını oluşturmuştur
    And admin Root klasörüne "Belge" belgesini yüklemiştir
    And admin Root klasörüne "DoluBelge" belgesini yüklemiştir
    And admin "DoluBelge" belgesini "Dolu" sınıfına geçerli metadata ile sınıflandırmıştır
    When admin "Pasif" sınıfını pasifleştirir
    Then "Belge" belgesi "Pasif" sınıfına sınıflandırılamamalı ve sınıfsız kalmalıdır
    And "Dolu" sınıfı silinememeli ve 409 çakışma hatası dönmelidir
    And "Pasif" sınıfı silinebilmeli ve sonrasında bulunamamalıdır

  # ---------------------------------------------------------------------------
  # Kaydedilmiş aramalar
  # ---------------------------------------------------------------------------

  @batch19 @search @security
  Scenario: B19-23 Kaydedilmiş arama sahibine özeldir
    Given admin API oturumu açılmıştır
    And "ayse" adında normal bir test kullanıcısı vardır
    And "mehmet" adında normal bir test kullanıcısı vardır
    When "ayse" kullanıcısı "Aylik Rapor" adlı kaydedilmiş aramayı oluşturur
    Then "mehmet" kullanıcısı "Aylik Rapor" aramasını göremez, listesinde bulamaz ve silemez
    And "ayse" kullanıcısı "Aylik Rapor" aramasını hâlâ görebilmelidir
