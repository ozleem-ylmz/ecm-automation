Feature: ECM klasör işlemleri

  Scenario: Admin kullanıcı klasörler sayfasını açabilmelidir
    Given admin kullanıcı ECM sistemine giriş yapmıştır
    When kullanıcı klasörler sayfasını açar
    Then ECM klasörler sayfası görüntülenmelidir

  Scenario: Admin kullanıcı yeni klasör oluşturabilmelidir
    Given admin kullanıcı klasörler sayfasındadır
    When kullanıcı "QA Folder 01" isimli yeni klasör oluşturur
    Then "QA Folder 01" klasörü görüntülenmelidir

  Scenario: Admin kullanıcı alt klasör oluşturabilmelidir
    Given admin kullanıcı klasörler sayfasındadır
    And "QA Folder 01" klasörü mevcuttur
    When kullanıcı "QA Folder 01" altında "QA Child Folder" klasörü oluşturur
    Then "QA Child Folder" alt klasörü görüntülenmelidir

  Scenario: Kullanıcı oluşturulan klasörü açabilmelidir
    Given admin kullanıcı klasörler sayfasındadır
    And "QA Folder 01" klasörü mevcuttur
    When kullanıcı "QA Folder 01" klasörünü açar
    Then klasör detay sayfası görüntülenmelidir

  Scenario: Admin kullanıcı klasörü yeniden adlandırabilmelidir
    Given admin kullanıcı yeniden adlandırma için benzersiz bir klasör oluşturmuştur
    When kullanıcı klasörü benzersiz yeni bir adla yeniden adlandırır
    Then klasör yeni adıyla görüntülenmelidir

  Scenario: Admin kullanıcı klasörü pasif hale getirebilmelidir
    Given admin kullanıcı klasörler sayfasındadır
    And "QA Folder Renamed" klasörü mevcuttur
    When kullanıcı klasörü pasif hale getirir
    Then klasör pasif olarak görüntülenmelidir

  Scenario: Admin kullanıcı pasif klasörü tekrar aktif hale getirebilmelidir
    Given admin kullanıcı pasif bir klasörün detay sayfasındadır
    When kullanıcı klasörü aktif hale getirir
    Then klasör aktif olarak görüntülenmelidir

  Scenario: Klasör detayında alt klasörler görüntülenmelidir
    Given admin kullanıcı klasör detay sayfasındadır
    When kullanıcı alt klasörleri görüntüler
    Then mevcut alt klasörler listelenmelidir

  Scenario: Klasör geçmişi görüntülenebilmelidir
    Given admin kullanıcı klasör detay sayfasındadır
    When kullanıcı klasör geçmişini açar
    Then klasör geçmişi görüntülenmelidir

  Scenario: Klasör ACL alanı görüntülenebilmelidir
    Given admin kullanıcı klasör detay sayfasındadır
    When kullanıcı klasör izinleri bölümünü açar
    Then klasör ACL bilgileri görüntülenmelidir

  Scenario: Klasör allowed classes alanı görüntülenebilmelidir
    Given admin kullanıcı klasör detay sayfasındadır
    When kullanıcı allowed classes bölümünü açar
    Then izin verilen class bilgileri görüntülenmelidir

  Scenario: Klasör breadcrumb bilgisi görüntülenmelidir
    Given admin kullanıcı bir alt klasörün detay sayfasındadır
    Then klasör breadcrumb bilgisi görüntülenmelidir

  Scenario: Aynı isimde root klasör ikinci kez oluşturulamamalıdır
    Given admin kullanıcı duplicate testi için benzersiz bir klasör oluşturmuştur
    When kullanıcı aynı isimde klasörü tekrar oluşturmaya çalışır
    Then duplicate klasör hatası görüntülenmelidir

  Scenario: Aynı parent altında aynı isimde alt klasör oluşturulamamalıdır
    Given admin kullanıcı duplicate alt klasör testi için benzersiz bir parent ve child oluşturmuştur
    When kullanıcı aynı child klasörü tekrar oluşturmaya çalışır
    Then duplicate klasör hatası görüntülenmelidir

  Scenario: Tek karakterli alt klasör oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı "A" isimli tek karakterli alt klasör oluşturur
    Then "A" alt klasörü görüntülenmelidir

  Scenario: Alt klasör adı alanının maksimum uzunluğu 255 olmalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    Then alt klasör adı alanının maksimum uzunluğu 255 olmalıdır

  Scenario: Rename alanının maksimum uzunluğu 255 olmalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı rename formunu açar
    Then rename alanının maksimum uzunluğu 255 olmalıdır

  Scenario: Klasör detayında Explorer bölümü görüntülenmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    Then Explorer bölümü görüntülenmelidir

  Scenario: Alt klasör breadcrumb alanında parent ve child görünmelidir
    Given admin kullanıcı benzersiz parent ve child klasör oluşturup child detayını açmıştır
    Then breadcrumb alanında parent ve child klasör adları görüntülenmelidir

  Scenario: Pasif klasörde Inactive etiketi görüntülenmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı test klasörünü pasif hale getirir
    Then Inactive etiketi görüntülenmelidir

  Scenario: Pasif klasör altında yeni alt klasör oluşturulamamalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı test klasörünü pasif hale getirir
    Then pasif klasör altında yeni alt klasör oluşturulamamalıdır

  Scenario: Tekrar aktif edilen klasörde alt klasör oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı test klasörünü pasif hale getirip tekrar aktif eder
    Then aktif klasör altında yeni alt klasör oluşturulabilmelidir

  Scenario: Yeni klasörde permission inheritance varsayılan olarak açık olmalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    Then permission inheritance açık olmalıdır

  Scenario: Admin permission inheritance özelliğini kapatabilmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı permission inheritance özelliğini kapatır
    Then permission inheritance kapalı olmalıdır

  Scenario: Admin kapatılan permission inheritance özelliğini tekrar açabilmelidir
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı permission inheritance özelliğini kapatıp tekrar açar
    Then permission inheritance açık olmalıdır

  Scenario: Rename işlemi Cancel ile iptal edilebilmelidir
    Given admin kullanıcı rename cancel testi için benzersiz bir klasör oluşturmuştur
    When kullanıcı rename formunu açıp yeni isim girer ve Cancel butonuna basar
    Then klasörün eski adı korunmalıdır

  Scenario: Klasör aynı isimle yeniden adlandırıldığında adı değişmemelidir
    Given admin kullanıcı same name rename testi için benzersiz bir klasör oluşturmuştur
    When kullanıcı klasörü mevcut adıyla yeniden kaydeder
    Then klasörün mevcut adı korunmalıdır

  Scenario: Klasör adı başındaki ve sonundaki boşluklar trim edilmelidir
    Given admin kullanıcı trim testi için benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı başında ve sonunda boşluk olan benzersiz bir alt klasör oluşturur
    Then alt klasör adı boşluklar olmadan görüntülenmelidir

  Scenario: Rename sırasında başındaki ve sonundaki boşluklar trim edilmelidir
    Given admin kullanıcı rename trim testi için benzersiz bir klasör oluşturmuştur
    When kullanıcı klasörü başında ve sonunda boşluk olan yeni isimle yeniden adlandırır
    Then klasör adı boşluklar olmadan görüntülenmelidir

  Scenario: Boş isim ile alt klasör oluşturulamamalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı boş isimle alt klasör oluşturmaya çalışır
    Then yeni alt klasör oluşturulmamalıdır

  Scenario: Sadece boşluk içeren isim ile alt klasör oluşturulamamalıdır
    Given admin kullanıcı benzersiz bir parent klasörün detay sayfasındadır
    When kullanıcı sadece boşluk içeren isimle alt klasör oluşturmaya çalışır
    Then yeni alt klasör oluşturulmamalıdır

  Scenario: Rename formu açıldığında mevcut klasör adı input içinde görünmelidir
    Given admin kullanıcı rename input testi için benzersiz bir klasör oluşturmuştur
    When kullanıcı rename formunu açar
    Then rename alanında mevcut klasör adı bulunmalıdır