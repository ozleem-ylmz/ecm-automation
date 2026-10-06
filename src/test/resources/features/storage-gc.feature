@storage-gc @admin @maintenance @ui
Feature: Storage Garbage Collector UI güvenliği

  Storage GC kalıcı veri silme yeteneğine sahip kritik bir admin ekranıdır.
  Tarayıcıdaki güvenlik kontrollerinin Playwright-Java ile doğrulanması gerekir.

  Scenario: Storage GC dry run varsayılan olarak güvenli preview modunda çalışır
    Given admin kullanıcı Storage GC sayfasındadır
    Then Dry run seçeneği varsayılan olarak açık olmalıdır
    And Storage GC Preview dry run butonu görüntülenmelidir
    When admin Storage GC preview işlemini çalıştırır
    Then Storage GC isteği dry run true ile gönderilmelidir
    And Storage GC preview isteği başarılı olmalıdır
    And Storage GC sonuç alanı görüntülenmelidir

  Scenario: Storage GC ilk silme onayı iptal edilirse hiçbir sürüm silinmez
    Given admin kullanıcı Storage GC sayfasındadır
    When admin listeden ilk sürümü seçer
    And admin seçili sürümü silmeyi dener
    Then ilk Storage GC silme onayı görüntülenmelidir
    When admin ilk Storage GC silme onayını iptal eder
    Then Storage GC sürüm silme isteği gönderilmemelidir
    And seçilen Storage GC sürümü seçili kalmalıdır

  Scenario: Storage GC ikinci silme onayı iptal edilirse hiçbir sürüm silinmez
    Given admin kullanıcı Storage GC sayfasındadır
    When admin listeden ilk sürümü seçer
    And admin seçili sürümü silmeyi dener
    Then ilk Storage GC silme onayı görüntülenmelidir
    When admin ilk Storage GC silme onayında devam eder
    Then ikinci Storage GC silme onayı görüntülenmelidir
    When admin ikinci Storage GC silme onayını iptal eder
    Then Storage GC sürüm silme isteği gönderilmemelidir
    And seçilen Storage GC sürümü seçili kalmalıdır

  @storage-gc-destructive
  Scenario: Storage GC iki onaydan sonra teste ait sürümü kalıcı olarak siler
    Given admin kullanıcının Storage GC için test belgesi vardır
    When admin test belgesinin sürümünü seçer
    And admin iki onayı da vererek seçili sürümü siler
    Then Storage GC gerçek sürüm silme isteği başarılı olmalıdır
    And Storage GC test belgesi artık sürüm listesinde görünmemelidir