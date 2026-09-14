Feature: ECM kullanıcı girişi

  Scenario: Admin kullanıcı başarılı şekilde giriş yapabilmelidir
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "admin@local" ve "123" ile giriş yapar
    Then klasörler sayfası görüntülenmelidir

  Scenario: Kullanıcı yanlış şifre ile giriş yapamamalıdır
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "admin@local" ve "yanlis123" ile giriş yapar
    Then kullanıcı login sayfasında kalmalıdır

  Scenario: Kullanıcı yanlış email ile giriş yapamamalıdır
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "yanlis@local" ve "123" ile giriş yapar
    Then kullanıcı login sayfasında kalmalıdır

  Scenario: Kullanıcı email alanını boş bırakarak giriş yapamamalıdır
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "" ve "123" ile giriş yapar
    Then kullanıcı login sayfasında kalmalıdır

  Scenario: Kullanıcı şifre alanını boş bırakarak giriş yapamamalıdır
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "admin@local" ve "" ile giriş yapar
    Then kullanıcı login sayfasında kalmalıdır

  Scenario: Kullanıcı email ve şifre alanlarını boş bırakarak giriş yapamamalıdır
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "" ve "" ile giriş yapar
    Then kullanıcı login sayfasında kalmalıdır

  Scenario: Manager kullanıcı başarılı şekilde giriş yapabilmelidir
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "manager@local" ve "123" ile giriş yapar
    Then klasörler sayfası görüntülenmelidir

  Scenario: Normal kullanıcı başarılı şekilde giriş yapabilmelidir
    Given kullanıcı ECM login sayfasındadır
    When kullanıcı "user@local" ve "123" ile giriş yapar
    Then klasörler sayfası görüntülenmelidir