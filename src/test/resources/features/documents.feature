Feature: ECM Documents işlemleri

  Scenario: Admin kullanıcı Documents sayfasını açabilmelidir
    Given admin kullanıcı ECM sistemine giriş yapmıştır
    When kullanıcı Documents sayfasını açar
    Then Documents sayfası görüntülenmelidir

  Scenario: Admin kullanıcı Upload without class sayfasını açabilmelidir
    Given admin kullanıcı Documents sayfasındadır
    When kullanıcı Upload without class butonuna tıklar
    Then Upload without class sayfası görüntülenmelidir

  Scenario: Admin kullanıcı class seçmeden TXT belge yükleyebilmelidir
    Given admin kullanıcı Upload without class sayfasındadır
    When kullanıcı benzersiz başlıkla TXT belge yükler
    Then yüklenen belgenin detay sayfası görüntülenmelidir

  Scenario: Yüklenen belgenin başlığı detay sayfasında görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then yüklenen belgenin başlığı detay sayfasında görüntülenmelidir

  Scenario: Yüklenen belge Documents listesinde bulunabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Documents sayfasına döner
    Then yüklenen belge Documents listesinde görüntülenmelidir

  Scenario: Başlık girilmeden belge yüklenememelidir
    Given admin kullanıcı Upload without class sayfasındadır
    When kullanıcı dosya seçer ancak başlık girmez
    And kullanıcı Upload document butonuna tıklar
    Then kullanıcı Upload without class sayfasında kalmalıdır

  Scenario: Dosya seçilmeden belge yüklenememelidir
    Given admin kullanıcı Upload without class sayfasındadır
    When kullanıcı benzersiz belge başlığı girer
    And kullanıcı Upload document butonuna tıklar
    Then kullanıcı Upload without class sayfasında kalmalıdır

  Scenario: Admin kullanıcı belgeyi soft delete yapabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    Then belge Deleted durumunda görüntülenmelidir

  Scenario: Admin kullanıcı soft delete yapılan belgeyi restore edebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  Scenario: Restore edilen belge tekrar kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge detay sayfası aktif olarak görüntülenmelidir

  Scenario: Admin kullanıcı soft delete yapılan belgeyi restore edebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır
      # =========================
  # Documents Batch 2
  # Status işlemleri
  # =========================

  Scenario: Yüklenen belgenin mevcut durumu görüntülenebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then belgenin status bilgisi görüntülenmelidir

  Scenario: Admin kullanıcı belgenin status bilgisini değiştirebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin yeni status bilgisi görüntülenmelidir

  Scenario: Admin kullanıcı Documents listesinden belge detayına gidebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Documents sayfasına döner
    And kullanıcı yüklenen belgeyi listeden açar
    Then yüklenen belgenin detay sayfası görüntülenmelidir

  Scenario: Belge detay sayfasından Documents listesine dönülebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Documents sayfasına döner
    Then Documents sayfası görüntülenmelidir
      # =========================
  # Documents Batch 3
  # Download / Preview / Version
  # =========================

  Scenario: Admin kullanıcı yüklenen belgeyi indirebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir

  Scenario: Admin kullanıcı TXT belgenin önizlemesini görüntüleyebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then belge önizlemesi görüntülenmelidir

  Scenario Outline: Admin kullanıcı belgeye yeni <versionType> versiyonu ekleyebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<versionType>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "<expectedVersion>" olmalıdır

    Examples:
      | versionType | expectedVersion |
      | Patch       | v1.0.1          |
      | Minor       | v1.1.0          |
      | Major       | v2.0.0          |