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

  @batch34
  Scenario: Admin kullanıcı belgeyi soft delete yapabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    Then belge Deleted durumunda görüntülenmelidir

  Scenario: Admin kullanıcı soft delete yapılan belgeyi restore edebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  Scenario: Restore edilen belge tekrar kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge detay sayfası aktif olarak görüntülenmelidir
  # Documents Batch 2
  # Status işlemleri
  # =========================

  Scenario: Yüklenen belgenin mevcut durumu görüntülenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then belgenin status bilgisi görüntülenmelidir

  Scenario: Admin kullanıcı belgenin status bilgisini değiştirebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin yeni status bilgisi görüntülenmelidir

  Scenario: Admin kullanıcı Documents listesinden belge detayına gidebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Documents sayfasına döner
    And kullanıcı yüklenen belgeyi listeden açar
    Then yüklenen belgenin detay sayfası görüntülenmelidir

  Scenario: Belge detay sayfasından Documents listesine dönülebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Documents sayfasına döner
    Then Documents sayfası görüntülenmelidir
      # =========================
  # Documents Batch 3
  # Download / Preview / Version
  # =========================

  Scenario: Admin kullanıcı yüklenen belgeyi indirebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir

  # ECM kaynağı: api/rest/documents.rs build_attachment_response (Content-Disposition: özgün ad) +
  # frontend api/documents.ts downloadFromUrl (sunucu adını tercih eder) + routes.rs CORS expose_headers
  # (yalnız x-total-count). UI API'yi farklı kökenden çağırdığı için ad okunamıyor -> belge kimliği.
  @batch35 @DL-01 @known-bug
  Scenario: DL-01 Arayüzden indirilen dosya özgün dosya adını korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then arayüzden indirilen dosya güncel sürümün özgün dosya adını taşımalıdır

  Scenario: Admin kullanıcı TXT belgenin önizlemesini görüntüleyebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then belge önizlemesi görüntülenmelidir

  Scenario Outline: Admin kullanıcı belgeye yeni <versionType> versiyonu ekleyebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  @batch34
  Scenario: Checkout yapılan belgede Check in butonu görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then Check in butonu görüntülenmelidir
    And "Check in" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  Scenario: Admin kullanıcı checkout yaptığı belgeyi check in yapabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumda olmamalıdır

  @batch34
  Scenario: Check in sonrasında Check out butonu tekrar görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Check out butonu görüntülenmelidir
    And "Check out" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

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
  @multiuser @batch34
  Scenario: Manager admin tarafından checkout edilen belgeyi indirebilmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then Download butonu görüntülenmelidir
    And manager "Download" işlemini açık belgede gerçekten yapabilmeli ve sonucu API ile doğrulanmalıdır
  @multiuser @batch34
  Scenario: Manager admin tarafından checkout edilen belgenin önizlemesini açabilmelidir
    Given admin kullanıcı bir belgeyi checkout yapmıştır
    When manager kullanıcı aynı belgeyi açar
    Then admin için Preview butonu görüntülenmelidir
    And manager "Preview" işlemini açık belgede gerçekten yapabilmeli ve sonucu API ile doğrulanmalıdır
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

  @critical @lock @batch34
  Scenario: Check in sonrasında belge değişiklik işlemleri tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @critical @lock
  Scenario: İki kullanıcı aynı belgeyi aynı anda checkout yapamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belgeyi açar
    Then manager kullanıcı belgeyi checkout yapamamalıdır

  @critical @integrity @batch34
  Scenario: Checkout edilmiş belge check in sonrasında silinebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Delete butonu görüntülenmelidir
    And "Delete" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @critical @integrity
  Scenario: Silinen belge eski detay URL üzerinden Deleted olarak görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı eski belge detay adresini açar
    Then belge eski detay adresinde Deleted olarak görüntülenmelidir

  @critical @integrity
  Scenario: Restore edilen belgenin başlığı korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    Then restore edilen belgenin başlığı değişmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin versiyonu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    And belgenin mevcut versiyonu kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    Then restore edilen belgenin versiyonu değişmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin içeriği korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yüklenen belgeyi listeden siler
    And kullanıcı silinen belgeyi restore eder
    And kullanıcı restore edilen belgenin önizlemesini açar
    Then restore edilen belge orijinal TXT içeriğini göstermelidir
  # Documents Batch 5
  # Status / Version / Restore Integrity
  # =========================

  @documents @integrity @batch5
  Scenario: Under review yapılan belge sayfa yenilendiğinde durumunu korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Soft delete edilen Under review belgenin statusu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    Then soft delete edilen belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Restore edilen Under review belgenin statusu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belgenin statusu Under review olmalıdır

  @documents @integrity @batch5
  Scenario: Patch versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @integrity @batch5
  Scenario: Minor versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.1.0" olmalıdır

  @documents @integrity @batch5
  Scenario: Major versiyonu sayfa yenilendiğinde korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v2.0.0" olmalıdır

  @documents @integrity @batch5
  Scenario: Patch versiyonlu belge restore sonrasında versiyonunu korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @integrity @batch5
  Scenario: Restore sonrasında Deleted bilgisi kaybolmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  @documents @integrity @batch5
  Scenario: Restore sonrasında Preview tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge önizlemesi görüntülenmelidir

  @documents @integrity @batch5
  Scenario: Restore sonrasında Download tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6 @minorTitle
  Scenario: Minor versiyonu oluşturulduğunda belge başlığı değişmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6
  Scenario: Major versiyonu oluşturulduğunda belge başlığı değişmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @integrity @batch6
  Scenario: Patch versiyonundan sonra ikinci Patch doğru artırılmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı Hard delete işlemini açar
    Then hard delete onay penceresi görüntülenmelidir
    And Permanently delete butonu disabled olmalıdır

  @documents @integrity @batch6 @harddelete
  Scenario: DELETE yazıldığında Permanently delete butonu aktif olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    And kullanıcı Hard delete işlemini açar
    And kullanıcı hard delete onay alanına "DELETE" yazar
    Then Permanently delete butonu aktif olmalıdır

  @documents @integrity @batch6 @harddelete
  Scenario: Hard delete edilen belge eski detay URL üzerinden açılamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
  # ECM'de status belge seviyesindedir (document.status); versiyonlarda status yoktur.
  # Yeni versiyon kaydı belgeyi Draft'a çeker ve önceki status'u audit geçmişine yazar
  # (save.rs -> DocumentStatus::Draft, audit: document.new_version before/after).
  Scenario: Under review belgeye eklenen yeni Patch versiyonu Draft olarak başlamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalıdır
    And belgenin statusu Draft olmalıdır
    And belge geçmişinde "v1.0.0" versiyonunun Under review statusundayken yerini Draft olarak oluşturulan "v1.0.1" versiyonuna bıraktığı görülmelidir

  @documents @bughunt @batch7
  # ECM'de status belge seviyesindedir (document.status); versiyonlarda status yoktur.
  # Yeni versiyon kaydı belgeyi Draft'a çeker ve önceki status'u audit geçmişine yazar
  # (save.rs -> DocumentStatus::Draft, audit: document.new_version before/after).
  Scenario: Under review belgeye eklenen yeni Minor versiyonu Draft olarak başlamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalıdır
    And belgenin statusu Draft olmalıdır
    And belge geçmişinde "v1.0.0" versiyonunun Under review statusundayken yerini Draft olarak oluşturulan "v1.1.0" versiyonuna bıraktığı görülmelidir

  @documents @bughunt @batch7
  # ECM'de status belge seviyesindedir (document.status); versiyonlarda status yoktur.
  # Yeni versiyon kaydı belgeyi Draft'a çeker ve önceki status'u audit geçmişine yazar
  # (save.rs -> DocumentStatus::Draft, audit: document.new_version before/after).
  Scenario: Under review belgeye eklenen yeni Major versiyonu Draft olarak başlamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalıdır
    And belgenin statusu Draft olmalıdır
    And belge geçmişinde "v1.0.0" versiyonunun Under review statusundayken yerini Draft olarak oluşturulan "v2.0.0" versiyonuna bıraktığı görülmelidir

  @documents @bughunt @batch7
  Scenario: Patch versiyonlu belgenin başlığı restore sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge önizlemesi görüntülenmelidir

  @documents @bughunt @batch7
  Scenario: Restore edilen çok versiyonlu belge indirilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @bughunt @batch7
  Scenario: Restore edilen belge tekrar checkout yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
      # =========================
  # Documents Batch 8
  # Negative / State Bug Hunt
  # =========================

  @documents @bughunt @batch8 @batch34
  Scenario: Checkout edilen belgede New version işlemi kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasında belge statusu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasında Under review statusu refresh ile korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Patch versiyonu oluşturulduktan sonra checkout yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch8
  Scenario: Checkout ve check in Patch versiyonunu değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @bughunt @batch8
  Scenario: Restore edilen Under review belge checkout yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır
    And belgenin statusu Under review olmalıdır

  @documents @bughunt @batch8
  Scenario: Restore edilen Patch versiyonlu belge checkout yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı silinen belgeyi tekrar açar
    Then Check out butonu görüntülenmemelidir


  @documents @bughunt @batch8
  Scenario: Soft delete edilen belge Deleted durumunu refresh sonrasında korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout edilmiş belgeyi checkout yapamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belgeyi checkout yapmayı dener
    Then manager checkout işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken yeni versiyon oluşturamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı aynı belge için New version işlemini dener
    Then manager New version işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belgeyi yeniden adlandıramamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belgeyi yeniden adlandırmayı dener
    Then manager rename işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belge statusunu değiştirememelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belge statusunu değiştirmeyi dener
    Then manager status değişikliği engellenmelidir

  @documents @bughunt @batch9
  Scenario: Başka kullanıcı checkout kilidi varken belgeyi soft delete yapamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And manager kullanıcı ayrı tarayıcı oturumunda giriş yapmıştır
    When admin kullanıcı belgeyi checkout yapar
    And manager kullanıcı belgeyi soft delete yapmayı dener
    Then manager soft delete işlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Checkout kilidi check in sonrasında kaldırılmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumda olmamalıdır

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge başlığını değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then yüklenen belgenin başlığı değişmemelidir

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge versiyonunu değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @bughunt @batch9
  Scenario: Checkout durumu refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge checkout durumda olmalıdır

  @documents @bughunt @batch9
  Scenario: Check in durumu refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    And kullanıcı belge detay sayfasını yeniler
    Then belge checkout durumda olmamalıdır

  @documents @bughunt @batch9
  Scenario: Patch versiyonlu belge checkout ve check in sonrasında aynı versiyonda kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalıdır

  @documents @bughunt @batch9
  Scenario: Under review belge checkout ve check in sonrasında aynı statüde kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belgenin statusu Under review olmalıdır

  @documents @bughunt @batch9 @batch34
  Scenario: Checkout kilidi kaldırıldıktan sonra New version kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    # =====================================================
# BATCH 10 - VERSION / STATUS / DELETE-RESTORE INTEGRITY
# =====================================================

  @documents @regression @batch10
  Scenario: Patch versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Minor versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Major versiyon oluşturulduktan sonra status Draft olarak korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalıdır
    And belgenin statusu Draft olmalıdır

  @documents @regression @batch10
  Scenario: Under review statusu soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır

  @documents @regression @batch10
  Scenario: Patch versiyon soft delete ve restore sonrasında refresh ile korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
  Scenario: Under review belgeye eklenen Patch versiyonunun Draft statusu restore sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belgenin statusu Draft olmalıdır
    And belge versiyonu "v1.0.1" olmalıdır
    And belge geçmişinde "v1.0.0" versiyonunun Under review statusundayken yerini Draft olarak oluşturulan "v1.0.1" versiyonuna bıraktığı görülmelidir

  @documents @regression @batch10
  Scenario: Restore edilen çok versiyonlu belge checkout ve check in yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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

  @documents @regression @batch11 @batch34
  Scenario: Unclassified belge Classify işlemini göstermelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11
  Scenario: Unclassified belge sınıflandırma ekranını açabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Classify butonuna tıklar
    Then belge sınıflandırma ekranı görüntülenmelidir

  @documents @regression @batch11
  Scenario: Classify ekranından vazgeçilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Classify butonuna tıklar
    And kullanıcı sınıflandırma işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır

  @documents @regression @batch11
  Scenario: Classify iptal edildiğinde belge unclassified kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Classify butonuna tıklar
    And kullanıcı sınıflandırma işlemini iptal eder
    Then belge unclassified durumda olmalıdır

  @documents @regression @batch11
  Scenario: Unclassified belge refresh sonrasında unclassified kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belge detay sayfasını yeniler
    Then belge unclassified durumda olmalıdır


# -------------------------
# MOVE
# -------------------------

  @documents @regression @batch11
  Scenario: Aktif belge Move işlemini göstermelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11 @batch34
  Scenario: Move penceresi açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Move to folder bağlantısına tıklar
    Then Move penceresi görüntülenmelidir
    And "Move onay" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11
  Scenario: Move işlemi iptal edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge başlığı korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge başlığı değişmemelidir

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge versiyonu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin mevcut versiyonu kaydedilmiştir
    When kullanıcı Move to folder bağlantısına tıklar
    And kullanıcı Move işlemini iptal eder
    Then belge versiyonu değişmemelidir


# -------------------------
# TAGS
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede Classify kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Classify butonu görüntülenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede Move kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Move işlemi görüntülenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede tag giriş alanı kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    Then belgenin statusu Under review olmalıdır
    And Tag giriş alanı görüntülenmelidir


# -------------------------
# REFRESH INTEGRITY
# -------------------------

  @documents @regression @batch11 @batch34
  Scenario: Refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11
  Scenario: Refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11 @batch34
  Scenario: Refresh sonrasında tag giriş alanı kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belge detay sayfasını yeniler
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır


# -------------------------
# SOFT DELETE
# -------------------------

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Move kullanılamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Move butonu görüntülenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Classify kullanılamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Classify butonu görüntülenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiş belgede Add tag kullanılamamalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı eski belge detay adresini açar
    Then Tag giriş alanı görüntülenmemelidir


# -------------------------
# RESTORE
# -------------------------

  @documents @regression @batch11
  Scenario: Restore sonrasında Move tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11 @batch34
  Scenario: Restore sonrasında Classify tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11 @batch34
  Scenario: Restore sonrasında tag giriş alanı tekrar kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır


# -------------------------
# CHECKOUT
# -------------------------

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede Move işlemini görmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11 @batch34
  Scenario: Checkout sahibi belgede Classify işlemini görmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11 @batch34
  Scenario: Checkout sahibi belgede tag giriş alanını görmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır


# -------------------------
# CHECK-IN INTEGRITY
# -------------------------

  @documents @regression @batch11
  Scenario: Check in sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch11 @batch34
  Scenario: Check in sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch11 @batch34
  Scenario: Check in sonrasında tag giriş alanı kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır


# -------------------------
# VERSION + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Yeni versiyon refresh sonrasında belge işlemlerini korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı belge detay sayfasını yeniler
    Then belgenin statusu Under review olmalıdır
    And Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir


# -------------------------
# RESTORE + REFRESH
# -------------------------

  @documents @regression @batch11 @batch34
  Scenario: Restore ve refresh sonrasında belge işlemleri kullanılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir
    And Classify butonu görüntülenmelidir
    And Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Move" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

# ============================================================
# BATCH 12 - MOVE / DESTINATION / STATE COMBINATIONS
# ============================================================

  @documents @regression @batch12
  Scenario: Move penceresinde hedef klasör seçilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    Then Move penceresinde hedef klasör seçili olmalıdır
    And kullanıcı Move işlemini iptal eder

  @documents @regression @batch12
  Scenario: Seçilen Move işlemi iptal edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini iptal eder
    Then belge detay sayfasında kalınmalıdır
    And Move işlemi görüntülenmelidir

  @documents @regression @batch12
  Scenario: Belge başka klasöre taşınabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Move penceresi kapanmalıdır
    And belge hedef klasörde olmalıdır

  @documents @regression @batch12
  Scenario: Move sonrasında hedef klasör belge detayında görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge detay adresi korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin detay adresi kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge detay adresi Move sonrasında değişmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge başlığı korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge başlığı değişmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasında belge versiyonu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin mevcut versiyonu kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then belge versiyonu değişmemelidir

  @documents @regression @batch12 @batch34
  Scenario: Move sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Download butonu görüntülenmelidir
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move sonrasında Tag girişi kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move sonrasında New version kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move sonrasında Check out kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then Check out butonu görüntülenmelidir
    And "Check out" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında hedef klasör korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında belge başlığı korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And yüklenen belgenin başlığı kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belge başlığı değişmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında belge versiyonu korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And belgenin mevcut versiyonu kaydedilmiştir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then belge versiyonu değişmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch12 @batch34
  Scenario: Move ve refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move ve refresh sonrasında Tag girişi kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move ve refresh sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Download butonu görüntülenmelidir
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12 @batch34
  Scenario: Move ve refresh sonrasında New version kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    When kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch12
  Scenario Outline: Yeni versiyonlu belge Move sonrasında versiyonunu korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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

  @documents @regression @batch12 @batch34
  Scenario Outline: Yeni versiyonlu belge Move sonrasında işlemlerini korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    Examples:
      | tip   |
      | Patch |
      | Minor |
      | Major |

  @documents @regression @batch12
  Scenario: Under review belge başka klasöre taşınabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgenin status bilgisini değiştirir
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir
    And belgenin statusu Under review olmalıdır

  @documents @regression @batch12
  Scenario: Under review belge Move ve refresh sonrasında statusunu korumalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi checkout yapar
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir
    And belge checkout durumunda olmalıdır

  @documents @regression @batch12
  Scenario: Checkout sahibi Move sonrasında Check in yapabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    And benzersiz bir Move hedef klasörü oluşturulmuştur
    And kullanıcı Move bağlantısına tıklar
    And kullanıcı Move penceresinde hedef klasörü seçer
    And kullanıcı Move işlemini onaylar
    Then hedef klasör adı belge detayında görüntülenmelidir

  @documents @regression @batch12
  Scenario: Restore edilen belge Move ve refresh sonrasında aktif kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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

  @documents @regression @batch13 @batch34
  Scenario: Copy penceresi açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir
    And "Copy onay" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13
  Scenario: Copy penceresinde açıklama görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    Then Copy açıklaması görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayılan başlık doğru oluşturulmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    Then Copy varsayılan başlığı kaynak belge başlığı ve copy eki olmalıdır

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayılan hedef Root olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    Then Copy varsayılan hedef klasörü Root olmalıdır

  @documents @regression @batch13
  Scenario: Copy başlığı değiştirilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    Then Copy başlığı girilen değer olmalıdır

  @documents @regression @batch13
  Scenario: Copy başlığı boş bırakıldığında varsayılan başlık kullanılmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığını boş bırakır
    And kullanıcı Copy işlemini onaylar
    Then Copy penceresi kapanmalıdır
    And kopyalanan belge varsayılan copy başlığıyla görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy işlemi Cancel ile iptal edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini iptal eder
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13
  Scenario: Copy penceresi X ile kapatılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy penceresini X ile kapatır
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Cancel sonrasında Copy tekrar açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini iptal eder
    And kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir
    And "Copy onay" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13 @batch34
  Scenario: X ile kapatma sonrasında Copy tekrar açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy penceresini X ile kapatır
    And kullanıcı Copy penceresini açar
    Then Copy penceresi görüntülenmelidir
    And "Copy onay" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13
  Scenario: Copy hedef klasörü seçilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy hedef klasörünü seçer
    Then Copy hedef klasörü seçili olmalıdır

  @documents @regression @batch13
  Scenario: Hedef klasör seçildikten sonra Copy iptal edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    And benzersiz bir Copy hedef klasörü oluşturulmuştur
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy hedef klasörünü seçer
    And kullanıcı Copy işlemini iptal eder
    Then Copy penceresi kapanmalıdır
    And belge detay sayfasında kalınmalıdır

  @documents @regression @batch13
  Scenario: Özel başlıkla Root klasörüne Copy oluşturulabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Copy penceresi kapanmalıdır
    And kopyalanan belge başlığı görüntülenmelidir
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır

  @documents @regression @batch13
  Scenario: Özel başlıkla farklı klasöre Copy oluşturulabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir
    And kopyalanan belge kaynak belgeden farklı detay adresine sahip olmalıdır

  @documents @regression @batch13
  Scenario: Copy oluşturulduktan sonra kaynak belge erişilebilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı kaynak belge detayına döner
    Then kaynak belge başlığı korunmalıdır

  @documents @regression @batch13
  Scenario: Copy oluşturulduktan sonra kopya belge tekrar açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge Download işlemini desteklemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi indirir
    Then belge dosyası başarıyla indirilmelidir

  @documents @regression @batch13
  Scenario: Copy belge Preview işlemini desteklemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then belge önizlemesi görüntülenmelidir

  @documents @regression @batch13 @batch34
  Scenario: Copy belgede New version kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13
  Scenario: Copy belgede Move kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch13 @batch34
  Scenario: Copy belgede Classify kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Copy belgede Tag girişi kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Tag giriş alanı görüntülenmelidir
    And "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Copy belgede Check out kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Check out butonu görüntülenmelidir
    And "Check out" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Copy belgede tekrar Copy kullanılabilir olmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then Copy butonu görüntülenmelidir
    And "Copy" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Copy belge soft delete yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    Then belge Deleted durumunda görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge soft delete sonrası restore edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge artık Deleted durumunda olmamalıdır

  @documents @regression @batch13
  Scenario: Restore edilen Copy belge aktif kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi soft delete yapar
    And kullanıcı belgeyi restore eder
    Then belge detay sayfası aktif olarak görüntülenmelidir

  @documents @regression @batch13
  Scenario: Copy belge checkout yapılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi checkout yapar
    Then belge checkout durumunda olmalıdır

  @documents @regression @batch13
  Scenario: Copy belge checkout ve check in döngüsünü desteklemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belgeyi checkout yapar
    And kullanıcı belgeyi check in yapar
    Then belge checkout durumunda olmamalıdır

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden Copy oluşturabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı belgeyi checkout yapar
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden farklı klasöre Copy oluşturabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı belgenin status bilgisini değiştirir
    And kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    Then kopyalanan belge başlığı görüntülenmelidir

  @documents @regression @batch13
  Scenario: Patch versiyonlu kaynak belgeden Copy oluşturulabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
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
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalıdır

  @documents @regression @batch13 @batch34
  Scenario: Copy belge refresh sonrasında Download kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Download butonu görüntülenmelidir
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasında Move kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch13 @batch34
  Scenario: Copy belge refresh sonrasında Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    And Batch 13 kaynak belge bilgileri kaydedilmiştir
    When kullanıcı Copy penceresini açar
    And kullanıcı Copy başlığına benzersiz bir değer girer
    And kullanıcı Copy işlemini onaylar
    And kullanıcı belge detay sayfasını yeniler
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır


  # ============================================================
  # Batch 14 - Version Labels / Version History
  # ============================================================

  @documents @regression @batch14
  Scenario: Version history yeni belgede görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then Version history görüntülenmelidir

  @documents @regression @batch14 @batch34
  Scenario: Yeni belgede v1.0.0 version history satırı görüntülenmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then "v1.0.0" version history satırı görüntülenmelidir
    And "Sürüm geçmişi v1.0.0" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: v1.0.0 için label ekleme alanı açılabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    Then version label ekleme alanı açık olmalıdır

  @documents @regression @batch14
  Scenario: Version label input girilen değeri göstermelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    Then version label input değeri girilen değer olmalıdır

  @documents @regression @batch14
  Scenario: v1.0.0 versiyonuna label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Eklenen version label refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Version label ekleme işlemi iptal edilebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label işlemini iptal eder
    Then iptal edilen version label görüntülenmemelidir

  @documents @regression @batch14
  Scenario: Eklenen version label silinebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı eklenen version labelı siler
    Then eklenen version label görüntülenmemelidir

  @documents @regression @batch14
  Scenario: Silinen version label refresh sonrasında geri gelmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı eklenen version labelı siler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen version label görüntülenmemelidir

  @documents @regression @batch14
  Scenario: Aynı versiyona iki farklı label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı ikinci version labelı ekler
    Then eklenen version label görüntülenmelidir
    And ikinci version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: İki labeldan biri silindiğinde diğeri korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı ikinci version labelı ekler
    And kullanıcı eklenen version labelı siler
    Then eklenen version label görüntülenmemelidir
    And ikinci version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Tire içeren version label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı version label olarak "release-candidate" girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Alt çizgi içeren version label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı version label olarak "release_candidate" girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Sayısal version label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı version label olarak "2026" girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Büyük küçük harf içeren version label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı version label olarak "ReleaseCandidate" girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Nokta içeren version label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı version label olarak "release.1" girer
    And kullanıcı version label Add bağlantısına tıklar
    Then eklenen version label görüntülenmelidir

  @documents @regression @batch14 @batch34
  Scenario: Patch sonrası v1.0.1 history satırı oluşmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then "v1.0.1" version history satırı görüntülenmelidir
    And "Sürüm geçmişi v1.0.1" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: Patch versiyonuna label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.1" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.1" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Patch label refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.1" versiyonuna benzersiz label ekler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen label "v1.0.1" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Patch label eski v1.0.0 satırında görünmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.1" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda görüntülenmemelidir

  @documents @regression @batch14
  Scenario: Eski v1.0.0 versiyonuna Patch sonrasında label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Eski version label yeni Patch satırında görünmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.1" versiyonunda görüntülenmemelidir

  @documents @regression @batch14 @batch34
  Scenario: Minor sonrası v1.1.0 history satırı oluşmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then "v1.1.0" version history satırı görüntülenmelidir
    And "Sürüm geçmişi v1.1.0" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: Minor versiyonuna label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.1.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.1.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Minor label refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.1.0" versiyonuna benzersiz label ekler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen label "v1.1.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Minor label eski v1.0.0 satırında görünmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.1.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda görüntülenmemelidir

  @documents @regression @batch14 @batch34
  Scenario: Major sonrası v2.0.0 history satırı oluşmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then "v2.0.0" version history satırı görüntülenmelidir
    And "Sürüm geçmişi v2.0.0" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: Major versiyonuna label eklenebilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v2.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v2.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Major label refresh sonrasında korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v2.0.0" versiyonuna benzersiz label ekler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen label "v2.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Major label eski v1.0.0 satırında görünmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v2.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda görüntülenmemelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Patch oluşturulduktan sonra korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Minor oluşturulduktan sonra korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Minor" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Major oluşturulduktan sonra korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Major" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda görüntülenmelidir

  @documents @regression @batch14
  Scenario: Label eklemek mevcut document version değerini değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @regression @batch14
  Scenario: Label silmek mevcut document version değerini değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı eklenen version labelı siler
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @regression @batch14
  Scenario: Label ekleme iptali mevcut version değerini değiştirmemelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label işlemini iptal eder
    Then belge versiyonu "v1.0.0" olmalıdır

  @documents @regression @batch14 @batch34
  Scenario: Label eklendikten sonra Download kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then Download butonu görüntülenmelidir
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14 @batch34
  Scenario: Label eklendikten sonra Preview kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then admin için Preview butonu görüntülenmelidir
    And "Preview" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Move kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then Move işlemi görüntülenmelidir

  @documents @regression @batch14 @batch34
  Scenario: Label eklendikten sonra Copy kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then Copy butonu görüntülenmelidir
    And "Copy" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14 @batch34
  Scenario: Label eklendikten sonra New version kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then New version butonu görüntülenmelidir
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14 @batch34
  Scenario: Label eklendikten sonra Classify kullanılabilir kalmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then Classify butonu görüntülenmelidir
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @documents @regression @batch14
  Scenario: İki label refresh sonrasında birlikte korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı ikinci version labelı ekler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen version label görüntülenmelidir
    And ikinci version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: İlk label silinip refresh yapıldığında ikinci label korunmalıdır
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı "v1.0.0" versiyonu için label ekleme alanını açar
    And kullanıcı benzersiz bir version label girer
    And kullanıcı version label Add bağlantısına tıklar
    When kullanıcı ikinci version labelı ekler
    And kullanıcı eklenen version labelı siler
    And kullanıcı belge detay sayfasını yeniler
    Then eklenen version label görüntülenmemelidir
    And ikinci version label görüntülenmelidir

  @documents @regression @batch14
  Scenario: Patch ve eski versiyon farklı label taşıyabilmelidir
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    When kullanıcı yeni versiyon sayfasını açar
    And kullanıcı "Patch" versiyon tipini seçer
    And kullanıcı yeni versiyon dosyasını yükler
    And kullanıcı yeni versiyonu kaydeder
    When kullanıcı "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda görüntülenmelidir
    When kullanıcı "v1.0.1" versiyonu için label ekleme alanını açar
    And kullanıcı ikinci benzersiz version label girer
    And kullanıcı version label Add bağlantısına tıklar
    Then ikinci version label görüntülenmelidir
 # =====================================================
# BATCH 15 - DOCUMENT RENAME
# PATCH /v1/documents/{id}/title
# =====================================================

@batch15 @documents @rename @endpoint_PATCH_v1_documents_id_title @batch34
Scenario: B15-01 Admin document detail sayfasında rename edit kontrolünü görebilir
  Given Batch 15 için yeni bir document oluşturulur
  Then document rename edit butonu görüntülenmelidir
  And "Rename" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

@batch15 @documents @rename @batch34
Scenario: B15-02 Rename edit kontrolü inline rename editorunu açar
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then document rename editoru görüntülenmelidir
  And "Rename" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

@batch15 @documents @rename
Scenario: B15-03 Rename editoru mevcut document başlığını input içinde gösterir
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename alanında mevcut document başlığı bulunmalıdır

@batch15 @documents @rename @batch34
Scenario: B15-04 Rename editorunda confirm kontrolü bulunur
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename confirm butonu görüntülenmelidir
  And "Rename" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

@batch15 @documents @rename @batch34
Scenario: B15-05 Rename editorunda cancel kontrolü bulunur
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename cancel butonu görüntülenmelidir
  And "Rename iptal" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

@batch15 @documents @rename @endpoint_PATCH_v1_documents_id_title
Scenario: B15-06 Admin document başlığını değiştirebilir
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  And kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir
  And kullanıcı document rename işlemini kaydeder
  Then document yeni başlığı ile görüntülenmelidir

@batch15 @documents @rename
Scenario: B15-07 Rename sonrasında eski document başlığı kaybolur
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  And kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir
  And kullanıcı document rename işlemini kaydeder
  Then eski document başlığı artık görüntülenmemelidir

@batch15 @documents @rename
Scenario: B15-08 Rename document kimliğini ve detail URL adresini değiştirmez
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  And kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir
  And kullanıcı document rename işlemini kaydeder
  Then rename sonrasında document URL değişmemelidir

@batch15 @documents @rename
Scenario: B15-09 Cancel document başlığındaki değişikliği kaydetmez
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  And kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir
  And kullanıcı document rename işlemini iptal eder
  Then document rename editoru kapanmalıdır
  And document başlığı değişmeden kalmalıdır

@batch15 @documents @rename
Scenario: B15-10 Rename edilen başlık refresh sonrasında kalıcıdır
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  And kullanıcı document başlığını yeni benzersiz bir başlıkla değiştirir
  And kullanıcı document rename işlemini kaydeder
  And kullanıcı document detay sayfasını yeniler
  Then rename edilen document başlığı refresh sonrasında korunmalıdır
 # =====================================================
# BATCH 16 - DOCUMENT RENAME VALIDATION / EDGE CASES
# PATCH /v1/documents/{id}/title
# =====================================================

  @batch16 @documents @rename @validation
  Scenario: B16-01 Document başlığı boş bırakılamaz
    Given Batch 16 için yeni bir document oluşturulur
    When kullanıcı document rename editorunu açar
    And kullanıcı document rename alanını boş bırakır
    Then document rename işlemi boş başlıkla kaydedilememelidir

  @batch16 @documents @rename @validation
  Scenario: B16-02 Sadece boşluk içeren document başlığı kaydedilemez
    Given Batch 16 için yeni bir document oluşturulur
    When kullanıcı document rename editorunu açar
    And kullanıcı document rename alanına sadece boşluk girer
    Then document rename işlemi boşluk başlıkla kaydedilememelidir

  @batch16 @documents @rename @validation
  Scenario: B16-03 Rename iptal edildiğinde original başlık refresh sonrasında korunur
    Given Batch 16 için yeni bir document oluşturulur
    When kullanıcı document rename editorunu açar
    And kullanıcı document başlığını Batch 16 için yeni benzersiz bir başlıkla değiştirir
    And kullanıcı document rename işlemini iptal eder
    And kullanıcı document detay sayfasını yeniler
    Then Batch 16 original document başlığı görüntülenmelidir

  @batch16 @documents @rename @validation
  Scenario: B16-04 Document art arda iki kez yeniden adlandırılabilir
    Given Batch 16 için yeni bir document oluşturulur
    When kullanıcı document başlığını Batch 16 için birinci kez değiştirip kaydeder
    And kullanıcı document başlığını Batch 16 için ikinci kez değiştirip kaydeder
    Then document ikinci Batch 16 başlığı ile görüntülenmelidir

  @batch16 @documents @rename @validation
  Scenario: B16-05 İkinci rename sonrasında birinci rename başlığı artık görüntülenmez
    Given Batch 16 için yeni bir document oluşturulur
    When kullanıcı document başlığını Batch 16 için birinci kez değiştirip kaydeder
    And kullanıcı document başlığını Batch 16 için ikinci kez değiştirip kaydeder
    Then birinci Batch 16 rename başlığı artık görüntülenmemelidir
    # =====================================================
# BATCH 17 - VERSION CHANGE COMMENT
# =====================================================

  @batch17 @documents @version @comment @batch34
  Scenario: B17-01 Yeni version oluştururken version comment girilebilir
    Given Batch 17 için yeni bir document oluşturulur
    When kullanıcı Batch 17 document için yeni version oluşturmayı açar
    Then Version comment alanı görüntülenmelidir
    And "Version comment" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır

  @batch17 @documents @version @comment
  Scenario: B17-02 Version comment ile yeni version oluşturulabilir
    Given Batch 17 için yeni bir document oluşturulur
    When kullanıcı Batch 17 document için yeni version oluşturmayı açar
    And kullanıcı benzersiz bir Batch 17 version comment girer
    And kullanıcı Batch 17 yeni version işlemini kaydeder
    Then Batch 17 version comment document detayında görüntülenmelidir

  @batch17 @documents @version @comment
  Scenario: B17-03 Version comment refresh sonrasında korunur
    Given Batch 17 için yeni bir document oluşturulur
    When kullanıcı Batch 17 document için yeni version oluşturmayı açar
    And kullanıcı benzersiz bir Batch 17 version comment girer
    And kullanıcı Batch 17 yeni version işlemini kaydeder
    And kullanıcı document detay sayfasını yeniler
    Then Batch 17 version comment document detayında görüntülenmelidir

  @batch17 @documents @version @comment
  Scenario: B17-04 Version comment yeni version history kaydında görüntülenir
    Given Batch 17 için yeni bir document oluşturulur
    When kullanıcı Batch 17 document için yeni version oluşturmayı açar
    And kullanıcı benzersiz bir Batch 17 version comment girer
    And kullanıcı Batch 17 yeni version işlemini kaydeder
    Then Batch 17 yeni version history kaydı görüntülenmelidir
    And Batch 17 version comment yeni version kaydında görüntülenmelidir

  @batch17 @documents @version @comment
  Scenario: B17-05 Version comment olmadan da yeni version oluşturulabilir
    Given Batch 17 için yeni bir document oluşturulur
    When kullanıcı Batch 17 document için yeni version oluşturmayı açar
    And kullanıcı Version comment alanını boş bırakır
    And kullanıcı Batch 17 yeni version işlemini kaydeder
    Then Batch 17 yeni version başarıyla oluşturulmalıdır
    # =====================================================
# BATCH 18 - DOCUMENT RELATIONSHIPS
# =====================================================

  @batch18 @documents @relationships
  Scenario: B18-01 Document detail sayfasında Relationships alanı görüntülenir
    Given Batch 18 için yeni bir document oluşturulur
    Then Relationships alanı görüntülenmelidir

  @batch18 @documents @relationships
  Scenario: B18-02 Admin Link document penceresini açabilir
    Given Batch 18 için yeni bir document oluşturulur
    When kullanıcı Link document işlemini açar
    Then Link document penceresi görüntülenmelidir

  @batch18 @documents @relationships
  Scenario: B18-03 Link document penceresinde hedef document alanı görüntülenir
    Given Batch 18 için yeni bir document oluşturulur
    When kullanıcı Link document işlemini açar
    Then Target document ID alanı görüntülenmelidir

  @batch18 @documents @relationships
  Scenario: B18-04 Link document varsayılan olarak latest version binding kullanır
    Given Batch 18 için yeni bir document oluşturulur
    When kullanıcı Link document işlemini açar
    Then Float to latest seçili olmalıdır

  @batch18 @documents @relationships
  Scenario: B18-05 Specific version seçildiğinde Target version alanı görüntülenir
    Given Batch 18 için kaynak ve hedef document oluşturulur
    When kullanıcı kaynak document üzerinde Link document işlemini açar
    And kullanıcı hedef document ID bilgisini girer
    And kullanıcı Pin to specific version seçeneğini seçer
    Then Target version alanı görüntülenmelidir