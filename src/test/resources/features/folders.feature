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