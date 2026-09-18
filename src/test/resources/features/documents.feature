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

    # =========================
# Documents Batch 4
# Check-out / Check-in / Lock
# =========================

  Scenario: Yeni yüklenen belge checkout durumda olmamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then belge checkout durumda olmamalıdır

  Scenario: Admin kullanıcı belgeyi checkout yapabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  Scenario: Checkout yapılan belgede Check in butonu görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then Check in butonu görüntülenmelidir

  Scenario: Admin kullanıcı checkout yaptığı belgeyi check in yapabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumda olmamalıdır

  Scenario: Check in sonrasında Check out butonu tekrar görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Check out butonu görüntülenmelidir

    # =========================
# Multi-user Document Lock
# =========================

  Scenario: Manager admin tarafından checkout edilen belgenin lock bilgisini görebilmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then belge "Checked out by Admin" olarak görüntülenmelidir
  @multiuser
  Scenario: Manager admin tarafından checkout edilen belgeyi read-only görmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then belge read-only olarak görüntülenmelidir
  @multiuser
  Scenario: Manager admin tarafından checkout edilen belgede yeni versiyon oluşturamamalıdır
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then New version butonu görüntülenmemelidir
  @multiuser
  Scenario: Manager admin tarafından checkout edilen belgeyi indirebilmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then Download butonu görüntülenmelidir
  @multiuser
  Scenario: Manager admin tarafından checkout edilen belgenin önizlemesini açabilmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then Preview butonu görüntülenmelidir
  @critical @lock
  Scenario: Manager admin tarafından checkout edilen belgede Move işlemi yapamamalıdır
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then Move butonu görüntülenmemelidir

  @critical @lock
  Scenario: Manager admin tarafından checkout edilen belgede Classify işlemi yapamamalıdır
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then Classify butonu görüntülenmemelidir

  @critical @lock
  Scenario: Manager admin tarafından checkout edilen belgenin tag bilgisini değiştirememelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then tag değiştirme işlemi kullanılamamalıdır

  @critical @lock
  Scenario: Checkout durumu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When admin belge detay sayfasını yeniler
    Then belge checkout durumda olmalıdır

  @critical @lock
  Scenario: Check in sonrasında belge değişiklik işlemleri tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then New version butonu görüntülenmelidir

  @critical @lock
  Scenario: İki kullanıcı aynı belgeyi aynı anda checkout yapamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belgeyi açar
    Then manager kullanıcı belgeyi checkout yapamamalıdır
  @critical @integrity
  Scenario: Checkout edilmiş belge silinememelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    Then Delete butonu görüntülenmemelidir
  @critical @integrity
  Scenario: Checkout edilmiş belge Delete işlemi ile gerçekten silinememelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When kullanıcı checkout edilmiş belgeyi silmeyi dener
    Then checkout edilmiş belge silinmemelidir

  @critical @integrity
  Scenario: Checkout edilmiş belge check in sonrasında silinebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Delete butonu görüntülenmelidir

  @critical @integrity
  Scenario: Silinen belge eski detay URL üzerinden Deleted olarak görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı eski belge detay adresini açar
    Then belge eski detay adresinde Deleted olarak görüntülenmelidir

  @critical @integrity
  Scenario: Restore edilen belgenin başlığı korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    Then restore edilen belgenin başlığı değişmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin versiyonu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin mevcut versiyonu kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    Then restore edilen belgenin versiyonu değişmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin içeriği korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    And kullanıcı restore edilen belgenin önizlemesini açar
    Then restore edilen belge orijinal TXT içeriğini göstermelidir
    # =========================
  # Documents Batch 5
  # Status / Version / Restore Integrity
  # =========================

  @documents @integrity @batch5
  Scenario: Under review yapılan belge sayfa yenilendiğinde durumunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Soft delete edilen Under review belgenin statusu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    Then soft delete edilen belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Restore edilen Under review belgenin statusu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Patch versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @integrity @batch5
  Scenario: Minor versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.1.0" olmalıdır

  @documents @integrity @batch5
  Scenario: Major versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v2.0.0" olmalıdır

  @documents @integrity @batch5
  Scenario: Patch versiyonlu belge restore sonrasında versiyonunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @integrity @batch5
  Scenario: Restore sonrasında Deleted bilgisi kaybolmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  @documents @integrity @batch5
  Scenario: Restore sonrasında Preview tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge önizlemesi görüntülenmelidir

  @documents @integrity @batch5
  Scenario: Restore sonrasında Download tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir
      # =========================
  # Documents Batch 6
  # Version / Delete Integrity
  # =========================

  @documents @integrity @batch6
  Scenario: Patch versiyonu oluşturulduğunda belge başlığı değişmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6 @minorTitle
  Scenario: Minor versiyonu oluşturulduğunda belge başlığı değişmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6
  Scenario: Major versiyonu oluşturulduğunda belge başlığı değişmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6
  Scenario: Patch versiyonundan sonra ikinci Patch doğru artırılmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.2" olmalıdır

  @documents @integrity @batch6
  Scenario: Minor versiyonundan sonra Patch doğru artırılmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.1" olmalıdır

  @documents @integrity @batch6
  Scenario: Major versiyonundan sonra Minor doğru artırılmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.1.0" olmalıdır

  @documents @integrity @batch6
  Scenario: Birden fazla versiyon oluşturulan belge restore sonrasında son versiyonunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge versiyonu "v1.1.1" olmalıdır
    # =========================
  # Batch 6 - Hard Delete
  # =========================

  @documents @integrity @batch6 @harddelete
  Scenario: Hard delete penceresi DELETE onayı istemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı Hard delete işlemini açar
    Then hard delete onay penceresi görüntülenmelidir
    And Permanently delete butonu disabled olmalıdır

  @documents @integrity @batch6 @harddelete
  Scenario: DELETE yazıldığında Permanently delete butonu aktif olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı Hard delete işlemini açar
    And kullanıcı hard delete onay alanına "DELETE" yazar
    Then Permanently delete butonu aktif olmalıdır

  @documents @integrity @batch6 @harddelete
  Scenario: Hard delete edilen belge eski detay URL üzerinden açılamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı Hard delete işlemini açar
    And kullanıcı hard delete onay alanına "DELETE" yazar
    And kullanıcı belgeyi kalıcı olarak siler
    And kullanıcı eski belge detay adresini açar
    Then hard delete edilen belge detay sayfası görüntülenmemelidir
      # =========================
  # Documents Batch 7
  # Version / Status / Delete Bug Hunt
  # =========================

  @documents @bughunt @batch7
  Scenario: Under review belgeye Patch versiyonu eklendiğinde status korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch7
  Scenario: Under review belgeye Minor versiyonu eklendiğinde status korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch7
  Scenario: Under review belgeye Major versiyonu eklendiğinde status korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch7
  Scenario: Patch versiyonlu belgenin başlığı restore sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @bughunt @batch7
  Scenario: Minor versiyonlu belgenin başlığı restore sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @bughunt @batch7
  Scenario: Major versiyonlu belgenin başlığı restore sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @bughunt @batch7
  Scenario: Restore edilen çok versiyonlu belgenin Preview özelliği çalışmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge önizlemesi görüntülenmelidir

  @documents @bughunt @batch7
  Scenario: Restore edilen çok versiyonlu belge indirilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir

  @documents @bughunt @batch7
  Scenario: Check in yapılan belgenin versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @bughunt @batch7
  Scenario: Restore edilen belge tekrar checkout yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
      # =========================
  # Documents Batch 8
  # Negative / State Bug Hunt
  # =========================

  @documents @bughunt @batch8
  Scenario: Checkout edilen belgede New version işlemi kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then New version butonu görüntülenmelidir

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasında belge statusu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasında Under review statusu refresh ile korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Patch versiyonu oluşturulduktan sonra checkout yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch8
  Scenario: Checkout ve check in Patch versiyonunu değiştirmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @bughunt @batch8
  Scenario: Restore edilen Under review belge checkout yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
    And belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Restore edilen Patch versiyonlu belge checkout yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
    And belge versiyonu "v1.0.1" olmalıdır


  @documents @bughunt @batch8
  Scenario: Soft delete edilen belgede Check out işlemi kullanılamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    Then Check out butonu görüntülenmemelidir


  @documents @bughunt @batch8
  Scenario: Soft delete edilen belge Deleted durumunu refresh sonrasında korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı belge detay sayfasını yeniler
    Then belge eski detay adresinde Deleted olarak görüntülenmelidir

      # =========================
  # Documents Batch 9
  # Checkout Lock / Mutation Rules
  # =========================

  @documents @bughunt @batch9
  Scenario: Aynı kullanıcı checkout yaptıktan sonra belge checkout durumunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout edilmiş belgeyi checkout yapamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belgeyi checkout yapmayı dener
    Then manager checkout işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken yeni versiyon oluşturamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belge için New version işlemini dener
    Then manager New version işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belgeyi yeniden adlandıramamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belgeyi yeniden adlandırmayı dener
    Then manager rename işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belge statusunu değiştirememelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belge statusunu değiştirmeyi dener
    Then manager status değişikliği engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belgeyi soft delete yapamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belgeyi soft delete yapmayı dener
    Then manager soft delete işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Checkout kilidi check in sonrasında kaldırılmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumda olmamalıdır

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge başlığını değiştirmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge versiyonunu değiştirmemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @bughunt @batch9
  Scenario: Checkout durumu refresh sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch9
  Scenario: Check in durumu refresh sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge checkout durumda olmamalıdır

  @documents @bughunt @batch9
  Scenario: Patch versiyonlu belge checkout ve check in sonrasında aynı versiyonda kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @bughunt @batch9
  Scenario: Under review belge checkout ve check in sonrasında aynı statüde kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch9
  Scenario: Checkout kilidi kaldırıldıktan sonra New version kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then New version butonu görüntülenmelidir
    # =====================================================
# BATCH 10 - VERSION / STATUS / DELETE-RESTORE INTEGRITY
# =====================================================

  @documents @regression @batch10
  Scenario: Patch versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Minor versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Major versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Under review statusu soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @regression @batch10
  Scenario: Patch versiyon soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @regression @batch10
  Scenario: Minor versiyon soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.1.0" olmalıdır

  @documents @regression @batch10
  Scenario: Major versiyon soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v2.0.0" olmalıdır

  @documents @regression @batch10
  Scenario: Birden fazla Patch sonrasında soft delete ve restore en son versiyonu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge versiyonu "v1.0.2" olmalıdır

  @documents @regression @batch10
  Scenario: Under review ve Patch kombinasyonu restore sonrasında korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belgenin statusu Under review olmalıdır
    And belge versiyonu "v1.0.1" olmalıdır

  @documents @regression @batch10
  Scenario: Restore edilen çok versiyonlu belge checkout ve check in yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
    When kullanıcı belgeyi check in yapar
    Then belge checkout durumda olmamalıdır
    And belge versiyonu "v1.0.1" olmalıdır

# =========================================================
# BATCH 11 - DOCUMENT CLASSIFICATION / MOVE / TAGS
# =========================================================

  @documents @regression @batch11
  Scenario: Unclassified belge Classify işlemini göstermelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Unclassified belge sınıflandırma ekranını açabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Classify butonuna tıklar
    Then belge sınıflandırma ekranı görüntülenmelidir

  @documents @regression @batch11
  Scenario: Classify ekranından vazgeçilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Classify butonuna tıklar
    And kullanıcı sınıflandırma işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır

  @documents @regression @batch11
  Scenario: Classify iptal edildiğinde belge unclassified kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Classify butonuna tıklar
    And kullanıcı sınıflandırma işlemini iptal eder
    Then belge unclassified durumda olmalıdır

  @documents @regression @batch11
  Scenario: Unclassified belge refresh sonrasında unclassified kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belge detay sayfasını yeniler
    Then belge unclassified durumda olmalıdır


# -------------------------
# MOVE
# -------------------------

  @documents @regression @batch11
  Scenario: Aktif belge Move işlemini göstermelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Move penceresi açılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Move to folder bağlantısına tıklar
    Then Move penceresi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Move işlemi iptal edilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge başlığı korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge başlığı değişmemelidir

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge versiyonu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin mevcut versiyonu kaydedilmiştir
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge versiyonu değişmemelidir


# -------------------------
# TAGS
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede Classify kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalıdır
    And Classify butonu görüntülenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede Move kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalıdır
    And Move işlemi görüntülenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede tag giriş alanı kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalıdır
    And Tag giriş alanı görüntülenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


# -------------------------
# STATUS + OPERATIONS
# -------------------------

  @documents @regression @batch11
  Scenario: Under review belgede Classify kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede Move kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede tag giriş alanı kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Tag giriş alanı görüntülenmelidir


# -------------------------
# REFRESH INTEGRITY
# -------------------------

  @documents @regression @batch11
  Scenario: Refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Refresh sonrasında tag giriş alanı kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belge detay sayfasını yeniler
    Then Tag giriş alanı görüntülenmelidir


# -------------------------
# SOFT DELETE
# -------------------------

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Move kullanılamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Move butonu görüntülenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Classify kullanılamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Classify butonu görüntülenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Add tag kullanılamamalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Tag giriş alanı görüntülenmemelidir


# -------------------------
# RESTORE
# -------------------------

  @documents @regression @batch11
  Scenario: Restore sonrasında Move tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Restore sonrasında Classify tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Restore sonrasında tag giriş alanı tekrar kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Tag giriş alanı görüntülenmelidir


# -------------------------
# CHECKOUT
# -------------------------

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede Move işlemini görmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede Classify işlemini görmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede tag giriş alanını görmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    Then Tag giriş alanı görüntülenmelidir


# -------------------------
# CHECK-IN INTEGRITY
# -------------------------

  @documents @regression @batch11
  Scenario: Check in sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Check in sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Check in sonrasında tag giriş alanı kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Tag giriş alanı görüntülenmelidir


# -------------------------
# VERSION + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Yeni versiyon refresh sonrasında belge işlemlerini korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "<versiyon>" olmalıdır
    And Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


# -------------------------
# STATUS + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario: Under review refresh sonrasında belge işlemlerini korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır
    And Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir


# -------------------------
# RESTORE + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario: Restore ve refresh sonrasında belge işlemleri kullanılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir

# ============================================================
# BATCH 12 - MOVE / DESTINATION / STATE COMBINATIONS
# ============================================================

  @documents @regression @batch12
  Scenario: Move penceresinde hedef klasör seçilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    Then Move penceresinde hedef klasör seçili olmalıdır
    And kullanıcı Move işlemini iptal eder

  @documents @regression @batch12
  Scenario: Seçilen Move işlemi iptal edilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır
    And Move işlemi görüntülenmelidir

  @documents @regression @batch12
  Scenario: Belge başka klasöre taşınabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Move penceresi kapanmalıdır
    And belge hedef klasörde olmalıdır

  @documents @regression @batch12
  Scenario: Move sonrasında hedef klasör belge detayında görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge detay adresi korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin detay adresi kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge detay adresi Move sonrasında değişmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge başlığı korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge başlığı değişmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge versiyonu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin mevcut versiyonu kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge versiyonu değişmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Download butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında Tag girişi kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Tag giriş alanı görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında New version kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then New version butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında Check out kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Check out butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında hedef klasör korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında belge başlığı korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And yüklenen belgenin başlığı kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belge başlığı değişmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında belge versiyonu korunmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And belgenin mevcut versiyonu kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu değişmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında Tag girişi kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Tag giriş alanı görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Download butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında New version kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then New version butonu görüntülenmelidir

  @documents @regression @batch12
  Scenario Outline: Yeni versiyonlu belge Move sonrasında versiyonunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And belgenin mevcut versiyonu kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge versiyonu değişmemelidir
    And hedef klasör adı belge detayında görüntülenmelidir
    Examples:
      | tip   |
      | Patch |
      | Minor |
      | Major |

  @documents @regression @batch12
  Scenario Outline: Yeni versiyonlu belge Move sonrasında işlemlerini korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "<tip>" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir
    Examples:
      | tip   |
      | Patch |
      | Minor |
      | Major |

  @documents @regression @batch12
  Scenario: Under review belge başka klasöre taşınabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir
    And belgenin statusu Under review olmalıdır

  @documents @regression @batch12
  Scenario: Under review belge Move ve refresh sonrasında statusunu korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır
    And hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Checkout sahibi belgeyi başka klasöre taşıyabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir
    And belge checkout durumunda olmalıdır

  @documents @regression @batch12
  Scenario: Checkout sahibi Move sonrasında Check in yapabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi checkout yapar
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumunda olmamalıdır
    And hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Restore edilen belge başka klasöre taşınabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Restore edilen belge Move ve refresh sonrasında aktif kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belge detay sayfası aktif olarak görüntülenmelidir
    And hedef klasör adı belge detayında görüntülenmelidir

# ============================================================
  # BATCH 13 - COPY / DESTINATION / INDEPENDENCE
  # ============================================================

  @documents @regression @batch13
  Scenario: Copy penceresi açılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy penceresinde açıklama görüntülenmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    Then Copy açıklaması görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayılan başlık doğru oluşturulmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    Then Copy varsayılan başlığı kaynak belge başlığı ve copy eki olmalıdır

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayılan hedef Root olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    Then Copy varsayılan hedef klasörü Root olmalıdır

  @documents @regression @batch13
  Scenario: Copy başlığı değiştirilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    Then Copy başlığı girilen değer olmalıdır

  @documents @regression @batch13
  Scenario: Copy başlığı boş bırakıldığında varsayılan başlık kullanılmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığını boş bırakır
    And kullanıcı Copy işlemini onaylar
    Then Copy penceresi kapanmalıdır
    And kopyalanan belge varsayılan copy başlığıyla görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy işlemi Cancel ile iptal edilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini iptal eder
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13
  Scenario: Copy penceresi X ile kapatılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy penceresini X ile kapatır
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13
  Scenario: Cancel sonrasında Copy tekrar açılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini iptal eder
    And kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir

  @documents @regression @batch13
  Scenario: X ile kapatma sonrasında Copy tekrar açılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy penceresini X ile kapatır
    And kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy hedef klasörü seçilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy hedef klasörünü seçer
    Then Copy hedef klasörü seçili olmalıdır

  @documents @regression @batch13
  Scenario: Hedef klasör seçildikten sonra Copy iptal edilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy hedef klasörünü seçer
    And kullanıcı Copy işlemini iptal eder
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13
  Scenario: Özel başlıkla Root klasörüne Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Copy penceresi kapanmalıdır
    And kopyalanan belge başlığı görüntülenmelidir
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır

  @documents @regression @batch13
  Scenario: Özel başlıkla farklı klasöre Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy hedef klasörünü seçer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır

  @documents @regression @batch13
  Scenario: Varsayılan başlıkla Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır

  @documents @regression @batch13
  Scenario: Copy oluşturulduktan sonra kaynak belge erişilebilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı kaynak belge detayına döner
    Then kaynak belge başlığı korunmalıdır

  @documents @regression @batch13
  Scenario: Copy oluşturulduktan sonra kopya belge tekrar açılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır
    And kullanıcı kaynak belge detayına döner
    And kullanıcı kopya belge detayına döner
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasında başlığını korumalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge Download işlemini desteklemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir

  @documents @regression @batch13
  Scenario: Copy belge Preview işlemini desteklemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then belge önizlemesi görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede New version kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then New version butonu görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Move kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Classify kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Classify butonu görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Tag girişi kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Tag giriş alanı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Check out kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Check out butonu görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede tekrar Copy kullanılabilir olmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Copy butonu görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge soft delete yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    Then belge Deleted durumunda görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge soft delete sonrası restore edilebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  @documents @regression @batch13
  Scenario: Restore edilen Copy belge aktif kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge detay sayfası aktif olarak görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge checkout yapılabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumunda olmalıdır

  @documents @regression @batch13
  Scenario: Copy belge checkout ve check in döngüsünü desteklemelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumunda olmamalıdır

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden Copy oluşturabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden farklı klasöre Copy oluşturabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı belgeyi checkout yapar
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy hedef klasörünü seçer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Under review kaynak belgeden Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Patch versiyonlu kaynak belgeden Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Minor versiyonlu kaynak belgeden Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Major versiyonlu kaynak belgeden Copy oluşturulabilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belgeye Patch versiyonu eklenebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @regression @batch13
  Scenario: Copy belgeye Minor versiyonu eklenebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalıdır

  @documents @regression @batch13
  Scenario: Copy belgeye Major versiyonu eklenebilmelidir
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalıdır

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Download butonu görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı benzersiz bir TXT belge yüklemiştir
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir
