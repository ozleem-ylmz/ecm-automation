Feature: ECM Documents iÅŸlemleri

  Scenario: Admin kullanÄ±cÄ± Documents sayfasÄ±nÄ± aÃ§abilmelidir
    Given admin kullanÄ±cÄ± ECM sistemine giriÅŸ yapmÄ±ÅŸtÄ±r
    When kullanÄ±cÄ± Documents sayfasÄ±nÄ± aÃ§ar
    Then Documents sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± Upload without class sayfasÄ±nÄ± aÃ§abilmelidir
    Given admin kullanÄ±cÄ± Documents sayfasÄ±ndadÄ±r
    When kullanÄ±cÄ± Upload without class butonuna tÄ±klar
    Then Upload without class sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± class seÃ§meden TXT belge yÃ¼kleyebilmelidir
    Given admin kullanÄ±cÄ± Upload without class sayfasÄ±ndadÄ±r
    When kullanÄ±cÄ± benzersiz baÅŸlÄ±kla TXT belge yÃ¼kler
    Then yÃ¼klenen belgenin detay sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: YÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± detay sayfasÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± detay sayfasÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: YÃ¼klenen belge Documents listesinde bulunabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Documents sayfasÄ±na dÃ¶ner
    Then yÃ¼klenen belge Documents listesinde gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: BaÅŸlÄ±k girilmeden belge yÃ¼klenememelidir
    Given admin kullanÄ±cÄ± Upload without class sayfasÄ±ndadÄ±r
    When kullanÄ±cÄ± dosya seÃ§er ancak baÅŸlÄ±k girmez
    And kullanÄ±cÄ± Upload document butonuna tÄ±klar
    Then kullanÄ±cÄ± Upload without class sayfasÄ±nda kalmalÄ±dÄ±r

  Scenario: Dosya seÃ§ilmeden belge yÃ¼klenememelidir
    Given admin kullanÄ±cÄ± Upload without class sayfasÄ±ndadÄ±r
    When kullanÄ±cÄ± benzersiz belge baÅŸlÄ±ÄŸÄ± girer
    And kullanÄ±cÄ± Upload document butonuna tÄ±klar
    Then kullanÄ±cÄ± Upload without class sayfasÄ±nda kalmalÄ±dÄ±r

  Scenario: Admin kullanÄ±cÄ± belgeyi soft delete yapabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    Then belge Deleted durumunda gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± soft delete yapÄ±lan belgeyi restore edebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge artÄ±k Deleted durumunda olmamalÄ±dÄ±r

  Scenario: Restore edilen belge tekrar kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge detay sayfasÄ± aktif olarak gÃ¶rÃ¼ntÃ¼lenmelidir
  # Documents Batch 2
  # Status iÅŸlemleri
  # =========================

  Scenario: YÃ¼klenen belgenin mevcut durumu gÃ¶rÃ¼ntÃ¼lenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then belgenin status bilgisi gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    Then belgenin yeni status bilgisi gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± Documents listesinden belge detayÄ±na gidebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Documents sayfasÄ±na dÃ¶ner
    And kullanÄ±cÄ± yÃ¼klenen belgeyi listeden aÃ§ar
    Then yÃ¼klenen belgenin detay sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Belge detay sayfasÄ±ndan Documents listesine dÃ¶nÃ¼lebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Documents sayfasÄ±na dÃ¶ner
    Then Documents sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
      # =========================
  # Documents Batch 3
  # Download / Preview / Version
  # =========================

  Scenario: Admin kullanÄ±cÄ± yÃ¼klenen belgeyi indirebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi indirir
    Then belge dosyasÄ± baÅŸarÄ±yla indirilmelidir

  Scenario: Admin kullanÄ±cÄ± TXT belgenin Ã¶nizlemesini gÃ¶rÃ¼ntÃ¼leyebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then belge Ã¶nizlemesi gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario Outline: Admin kullanÄ±cÄ± belgeye yeni <versionType> versiyonu ekleyebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<versionType>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "<expectedVersion>" olmalÄ±dÄ±r

    Examples:
      | versionType | expectedVersion |
      | Patch       | v1.0.1          |
      | Minor       | v1.1.0          |
      | Major       | v2.0.0          |

    # =========================
# Documents Batch 4
# Check-out / Check-in / Lock
# =========================

  Scenario: Yeni yÃ¼klenen belge checkout durumda olmamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then belge checkout durumda olmamalÄ±dÄ±r

  Scenario: Admin kullanÄ±cÄ± belgeyi checkout yapabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r

  Scenario: Checkout yapÄ±lan belgede Check in butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then Check in butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  Scenario: Admin kullanÄ±cÄ± checkout yaptÄ±ÄŸÄ± belgeyi check in yapabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge checkout durumda olmamalÄ±dÄ±r

  Scenario: Check in sonrasÄ±nda Check out butonu tekrar gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then Check out butonu gÃ¶rÃ¼ntÃ¼lenmelidir

    # =========================
# Multi-user Document Lock
# =========================

  Scenario: Manager admin tarafÄ±ndan checkout edilen belgenin lock bilgisini gÃ¶rebilmelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then belge "Checked out by Admin" olarak gÃ¶rÃ¼ntÃ¼lenmelidir
  @multiuser
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgeyi read-only gÃ¶rmelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then belge read-only olarak gÃ¶rÃ¼ntÃ¼lenmelidir
  @multiuser
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgede yeni versiyon oluÅŸturamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmemelidir
  @multiuser
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgeyi indirebilmelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then Download butonu gÃ¶rÃ¼ntÃ¼lenmelidir
  @multiuser
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgenin Ã¶nizlemesini aÃ§abilmelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then admin iÃ§in Preview butonu gÃ¶rÃ¼ntÃ¼lenmelidir
  @critical @lock
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgede Move iÅŸlemi yapamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then Move butonu gÃ¶rÃ¼ntÃ¼lenmemelidir

  @critical @lock
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgede Classify iÅŸlemi yapamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmemelidir

  @critical @lock
  Scenario: Manager admin tarafÄ±ndan checkout edilen belgenin tag bilgisini deÄŸiÅŸtirememelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then tag deÄŸiÅŸtirme iÅŸlemi kullanÄ±lamamalÄ±dÄ±r

  @critical @lock
  Scenario: Checkout durumu sayfa yenilendiÄŸinde korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When admin belge detay sayfasÄ±nÄ± yeniler
    Then belge checkout durumda olmalÄ±dÄ±r

  @critical @lock
  Scenario: Check in sonrasÄ±nda belge deÄŸiÅŸiklik iÅŸlemleri tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @critical @lock
  Scenario: Ä°ki kullanÄ±cÄ± aynÄ± belgeyi aynÄ± anda checkout yapamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± aynÄ± belgeyi aÃ§ar
    Then manager kullanÄ±cÄ± belgeyi checkout yapamamalÄ±dÄ±r
  @critical @integrity
  Scenario: Checkout edilmiÅŸ belge silinememelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    Then Delete butonu gÃ¶rÃ¼ntÃ¼lenmemelidir
  @critical @integrity
  Scenario: Checkout edilmiÅŸ belge Delete iÅŸlemi ile gerÃ§ekten silinememelidir
    Given admin kullanÄ±cÄ± bir belgeyi checkout yapmÄ±ÅŸtÄ±r
    When kullanÄ±cÄ± checkout edilmiÅŸ belgeyi silmeyi dener
    Then checkout edilmiÅŸ belge silinmemelidir

  @critical @integrity
  Scenario: Checkout edilmiÅŸ belge check in sonrasÄ±nda silinebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then Delete butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @critical @integrity
  Scenario: Silinen belge eski detay URL Ã¼zerinden Deleted olarak gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± yÃ¼klenen belgeyi listeden siler
    And kullanÄ±cÄ± eski belge detay adresini aÃ§ar
    Then belge eski detay adresinde Deleted olarak gÃ¶rÃ¼ntÃ¼lenmelidir

  @critical @integrity
  Scenario: Restore edilen belgenin baÅŸlÄ±ÄŸÄ± korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yÃ¼klenen belgeyi listeden siler
    And kullanÄ±cÄ± silinen belgeyi restore eder
    Then restore edilen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin versiyonu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin mevcut versiyonu kaydedilmiÅŸtir
    When kullanÄ±cÄ± yÃ¼klenen belgeyi listeden siler
    And kullanÄ±cÄ± silinen belgeyi restore eder
    Then restore edilen belgenin versiyonu deÄŸiÅŸmemelidir

  @critical @integrity
  Scenario: Restore edilen belgenin iÃ§eriÄŸi korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yÃ¼klenen belgeyi listeden siler
    And kullanÄ±cÄ± silinen belgeyi restore eder
    And kullanÄ±cÄ± restore edilen belgenin Ã¶nizlemesini aÃ§ar
    Then restore edilen belge orijinal TXT iÃ§eriÄŸini gÃ¶stermelidir
    # =========================
  # Documents Batch 5
  # Status / Version / Restore Integrity
  # =========================

  @documents @integrity @batch5
  Scenario: Under review yapÄ±lan belge sayfa yenilendiÄŸinde durumunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Soft delete edilen Under review belgenin statusu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi soft delete yapar
    Then soft delete edilen belgenin statusu Under review olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Restore edilen Under review belgenin statusu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Patch versiyonu sayfa yenilendiÄŸinde korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Minor versiyonu sayfa yenilendiÄŸinde korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v1.1.0" olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Major versiyonu sayfa yenilendiÄŸinde korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v2.0.0" olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Patch versiyonlu belge restore sonrasÄ±nda versiyonunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Restore sonrasÄ±nda Deleted bilgisi kaybolmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge artÄ±k Deleted durumunda olmamalÄ±dÄ±r

  @documents @integrity @batch5
  Scenario: Restore sonrasÄ±nda Preview tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge Ã¶nizlemesi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @integrity @batch5
  Scenario: Restore sonrasÄ±nda Download tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi indirir
    Then belge dosyasÄ± baÅŸarÄ±yla indirilmelidir
      # =========================
  # Documents Batch 6
  # Version / Delete Integrity
  # =========================

  @documents @integrity @batch6
  Scenario: Patch versiyonu oluÅŸturulduÄŸunda belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @integrity @batch6 @minorTitle
  Scenario: Minor versiyonu oluÅŸturulduÄŸunda belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @integrity @batch6
  Scenario: Major versiyonu oluÅŸturulduÄŸunda belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @integrity @batch6
  Scenario: Patch versiyonundan sonra ikinci Patch doÄŸru artÄ±rÄ±lmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.2" olmalÄ±dÄ±r

  @documents @integrity @batch6
  Scenario: Minor versiyonundan sonra Patch doÄŸru artÄ±rÄ±lmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.1" olmalÄ±dÄ±r

  @documents @integrity @batch6
  Scenario: Major versiyonundan sonra Minor doÄŸru artÄ±rÄ±lmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v2.1.0" olmalÄ±dÄ±r

  @documents @integrity @batch6
  Scenario: Birden fazla versiyon oluÅŸturulan belge restore sonrasÄ±nda son versiyonunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge versiyonu "v1.1.1" olmalÄ±dÄ±r
    # =========================
  # Batch 6 - Hard Delete
  # =========================

  @documents @integrity @batch6 @harddelete
  Scenario: Hard delete penceresi DELETE onayÄ± istemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± silinen belgeyi tekrar aÃ§ar
    And kullanÄ±cÄ± Hard delete iÅŸlemini aÃ§ar
    Then hard delete onay penceresi gÃ¶rÃ¼ntÃ¼lenmelidir
    And Permanently delete butonu disabled olmalÄ±dÄ±r

  @documents @integrity @batch6 @harddelete
  Scenario: DELETE yazÄ±ldÄ±ÄŸÄ±nda Permanently delete butonu aktif olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± silinen belgeyi tekrar aÃ§ar
    And kullanÄ±cÄ± Hard delete iÅŸlemini aÃ§ar
    And kullanÄ±cÄ± hard delete onay alanÄ±na "DELETE" yazar
    Then Permanently delete butonu aktif olmalÄ±dÄ±r

  @documents @integrity @batch6 @harddelete
  Scenario: Hard delete edilen belge eski detay URL Ã¼zerinden aÃ§Ä±lamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± silinen belgeyi tekrar aÃ§ar
    And kullanÄ±cÄ± Hard delete iÅŸlemini aÃ§ar
    And kullanÄ±cÄ± hard delete onay alanÄ±na "DELETE" yazar
    And kullanÄ±cÄ± belgeyi kalÄ±cÄ± olarak siler
    And kullanÄ±cÄ± eski belge detay adresini aÃ§ar
    Then hard delete edilen belge detay sayfasÄ± gÃ¶rÃ¼ntÃ¼lenmemelidir
      # =========================
  # Documents Batch 7
  # Version / Status / Delete Bug Hunt
  # =========================

  @documents @bughunt @batch7
  Scenario: Under review belgeye Patch versiyonu eklendiÄŸinde status korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch7
  Scenario: Under review belgeye Minor versiyonu eklendiÄŸinde status korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch7
  Scenario: Under review belgeye Major versiyonu eklendiÄŸinde status korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch7
  Scenario: Patch versiyonlu belgenin baÅŸlÄ±ÄŸÄ± restore sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @bughunt @batch7
  Scenario: Minor versiyonlu belgenin baÅŸlÄ±ÄŸÄ± restore sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @bughunt @batch7
  Scenario: Major versiyonlu belgenin baÅŸlÄ±ÄŸÄ± restore sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @bughunt @batch7
  Scenario: Restore edilen Ã§ok versiyonlu belgenin Preview Ã¶zelliÄŸi Ã§alÄ±ÅŸmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge Ã¶nizlemesi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @bughunt @batch7
  Scenario: Restore edilen Ã§ok versiyonlu belge indirilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi indirir
    Then belge dosyasÄ± baÅŸarÄ±yla indirilmelidir

  @documents @bughunt @batch7
  Scenario: Check in yapÄ±lan belgenin versiyonu sayfa yenilendiÄŸinde korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v1.0.0" olmalÄ±dÄ±r

  @documents @bughunt @batch7
  Scenario: Restore edilen belge tekrar checkout yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r
      # =========================
  # Documents Batch 8
  # Negative / State Bug Hunt
  # =========================

  @documents @bughunt @batch8
  Scenario: Checkout edilen belgede New version iÅŸlemi kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasÄ±nda belge statusu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch8
  Scenario: Checkout ve check in sonrasÄ±nda Under review statusu refresh ile korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch8
  Scenario: Patch versiyonu oluÅŸturulduktan sonra checkout yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r

  @documents @bughunt @batch8
  Scenario: Checkout ve check in Patch versiyonunu deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @bughunt @batch8
  Scenario: Restore edilen Under review belge checkout yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r
    And belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch8
  Scenario: Restore edilen Patch versiyonlu belge checkout yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r
    And belge versiyonu "v1.0.1" olmalÄ±dÄ±r


  @documents @bughunt @batch8
  Scenario: Soft delete edilen belgede Check out iÅŸlemi kullanÄ±lamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± silinen belgeyi tekrar aÃ§ar
    Then Check out butonu gÃ¶rÃ¼ntÃ¼lenmemelidir


  @documents @bughunt @batch8
  Scenario: Soft delete edilen belge Deleted durumunu refresh sonrasÄ±nda korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± silinen belgeyi tekrar aÃ§ar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge eski detay adresinde Deleted olarak gÃ¶rÃ¼ntÃ¼lenmelidir

      # =========================
  # Documents Batch 9
  # Checkout Lock / Mutation Rules
  # =========================

  @documents @bughunt @batch9
  Scenario: AynÄ± kullanÄ±cÄ± checkout yaptÄ±ktan sonra belge checkout durumunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: BaÅŸka kullanÄ±cÄ± checkout edilmiÅŸ belgeyi checkout yapamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And manager kullanÄ±cÄ± ayrÄ± tarayÄ±cÄ± oturumunda giriÅŸ yapmÄ±ÅŸtÄ±r
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± aynÄ± belgeyi checkout yapmayÄ± dener
    Then manager checkout iÅŸlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: BaÅŸka kullanÄ±cÄ± checkout kilidi varken yeni versiyon oluÅŸturamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And manager kullanÄ±cÄ± ayrÄ± tarayÄ±cÄ± oturumunda giriÅŸ yapmÄ±ÅŸtÄ±r
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± aynÄ± belge iÃ§in New version iÅŸlemini dener
    Then manager New version iÅŸlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: BaÅŸka kullanÄ±cÄ± checkout kilidi varken belgeyi yeniden adlandÄ±ramamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And manager kullanÄ±cÄ± ayrÄ± tarayÄ±cÄ± oturumunda giriÅŸ yapmÄ±ÅŸtÄ±r
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± belgeyi yeniden adlandÄ±rmayÄ± dener
    Then manager rename iÅŸlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: BaÅŸka kullanÄ±cÄ± checkout kilidi varken belge statusunu deÄŸiÅŸtirememelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And manager kullanÄ±cÄ± ayrÄ± tarayÄ±cÄ± oturumunda giriÅŸ yapmÄ±ÅŸtÄ±r
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± belge statusunu deÄŸiÅŸtirmeyi dener
    Then manager status deÄŸiÅŸikliÄŸi engellenmelidir

  @documents @bughunt @batch9
  Scenario: BaÅŸka kullanÄ±cÄ± checkout kilidi varken belgeyi soft delete yapamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And manager kullanÄ±cÄ± ayrÄ± tarayÄ±cÄ± oturumunda giriÅŸ yapmÄ±ÅŸtÄ±r
    When admin kullanÄ±cÄ± belgeyi checkout yapar
    And manager kullanÄ±cÄ± belgeyi soft delete yapmayÄ± dener
    Then manager soft delete iÅŸlemi engellenmelidir

  @documents @bughunt @batch9
  Scenario: Checkout kilidi check in sonrasÄ±nda kaldÄ±rÄ±lmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge checkout durumda olmamalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge baÅŸlÄ±ÄŸÄ±nÄ± deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @bughunt @batch9
  Scenario: Checkout ve check in belge versiyonunu deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge versiyonu "v1.0.0" olmalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Checkout durumu refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge checkout durumda olmalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Check in durumu refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge checkout durumda olmamalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Patch versiyonlu belge checkout ve check in sonrasÄ±nda aynÄ± versiyonda kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Under review belge checkout ve check in sonrasÄ±nda aynÄ± statÃ¼de kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @bughunt @batch9
  Scenario: Checkout kilidi kaldÄ±rÄ±ldÄ±ktan sonra New version kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    # =====================================================
# BATCH 10 - VERSION / STATUS / DELETE-RESTORE INTEGRITY
# =====================================================

  @documents @regression @batch10
  Scenario: Patch versiyon oluÅŸturulduktan sonra status Draft olarak korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r
    And belgenin statusu Draft olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Minor versiyon oluÅŸturulduktan sonra status Draft olarak korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalÄ±dÄ±r
    And belgenin statusu Draft olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Major versiyon oluÅŸturulduktan sonra status Draft olarak korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalÄ±dÄ±r
    And belgenin statusu Draft olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Under review statusu soft delete ve restore sonrasÄ±nda refresh ile korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belgenin statusu Under review olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Patch versiyon soft delete ve restore sonrasÄ±nda refresh ile korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Minor versiyon soft delete ve restore sonrasÄ±nda refresh ile korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v1.1.0" olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Major versiyon soft delete ve restore sonrasÄ±nda refresh ile korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "v2.0.0" olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Birden fazla Patch sonrasÄ±nda soft delete ve restore en son versiyonu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge versiyonu "v1.0.2" olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Under review ve Patch kombinasyonu restore sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @regression @batch10
  Scenario: Restore edilen Ã§ok versiyonlu belge checkout ve check in yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumda olmalÄ±dÄ±r
    When kullanÄ±cÄ± belgeyi check in yapar
    Then belge checkout durumda olmamalÄ±dÄ±r
    And belge versiyonu "v1.0.1" olmalÄ±dÄ±r

# =========================================================
# BATCH 11 - DOCUMENT CLASSIFICATION / MOVE / TAGS
# =========================================================

  @documents @regression @batch11
  Scenario: Unclassified belge Classify iÅŸlemini gÃ¶stermelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Unclassified belge sÄ±nÄ±flandÄ±rma ekranÄ±nÄ± aÃ§abilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Classify butonuna tÄ±klar
    Then belge sÄ±nÄ±flandÄ±rma ekranÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Classify ekranÄ±ndan vazgeÃ§ilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Classify butonuna tÄ±klar
    And kullanÄ±cÄ± sÄ±nÄ±flandÄ±rma iÅŸlemini iptal eder
    Then belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r

  @documents @regression @batch11
  Scenario: Classify iptal edildiÄŸinde belge unclassified kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Classify butonuna tÄ±klar
    And kullanÄ±cÄ± sÄ±nÄ±flandÄ±rma iÅŸlemini iptal eder
    Then belge unclassified durumda olmalÄ±dÄ±r

  @documents @regression @batch11
  Scenario: Unclassified belge refresh sonrasÄ±nda unclassified kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge unclassified durumda olmalÄ±dÄ±r


# -------------------------
# MOVE
# -------------------------

  @documents @regression @batch11
  Scenario: Aktif belge Move iÅŸlemini gÃ¶stermelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Move penceresi aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Move to folder baÄŸlantÄ±sÄ±na tÄ±klar
    Then Move penceresi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Move iÅŸlemi iptal edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Move to folder baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move iÅŸlemini iptal eder
    Then belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge baÅŸlÄ±ÄŸÄ± korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    When kullanÄ±cÄ± Move to folder baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move iÅŸlemini iptal eder
    Then belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @regression @batch11
  Scenario: Move iptalinden sonra belge versiyonu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin mevcut versiyonu kaydedilmiÅŸtir
    When kullanÄ±cÄ± Move to folder baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move iÅŸlemini iptal eder
    Then belge versiyonu deÄŸiÅŸmemelidir


# -------------------------
# TAGS
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede Classify kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalÄ±dÄ±r
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede Move kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalÄ±dÄ±r
    And Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


  @documents @regression @batch11
  Scenario Outline: Versiyonlu belgede tag giriÅŸ alanÄ± kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "<versiyon>" olmalÄ±dÄ±r
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


# -------------------------
# STATUS + OPERATIONS
# -------------------------

  @documents @regression @batch11
  Scenario: Under review belgede Classify kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede Move kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Under review belgede tag giriÅŸ alanÄ± kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# REFRESH INTEGRITY
# -------------------------

  @documents @regression @batch11
  Scenario: Refresh sonrasÄ±nda Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Refresh sonrasÄ±nda Move kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Refresh sonrasÄ±nda tag giriÅŸ alanÄ± kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# SOFT DELETE
# -------------------------

  @documents @regression @batch11
  Scenario: Soft delete edilmiÅŸ belgede Move kullanÄ±lamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± eski belge detay adresini aÃ§ar
    Then Move butonu gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiÅŸ belgede Classify kullanÄ±lamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± eski belge detay adresini aÃ§ar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch11
  Scenario: Soft delete edilmiÅŸ belgede Add tag kullanÄ±lamamalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± eski belge detay adresini aÃ§ar
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmemelidir


# -------------------------
# RESTORE
# -------------------------

  @documents @regression @batch11
  Scenario: Restore sonrasÄ±nda Move tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Restore sonrasÄ±nda Classify tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Restore sonrasÄ±nda tag giriÅŸ alanÄ± tekrar kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# CHECKOUT
# -------------------------

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede Move iÅŸlemini gÃ¶rmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede Classify iÅŸlemini gÃ¶rmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Checkout sahibi belgede tag giriÅŸ alanÄ±nÄ± gÃ¶rmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# CHECK-IN INTEGRITY
# -------------------------

  @documents @regression @batch11
  Scenario: Check in sonrasÄ±nda Move kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Check in sonrasÄ±nda Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch11
  Scenario: Check in sonrasÄ±nda tag giriÅŸ alanÄ± kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# VERSION + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario Outline: Yeni versiyon refresh sonrasÄ±nda belge iÅŸlemlerini korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu "<versiyon>" olmalÄ±dÄ±r
    And Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

    Examples:
      | tip   | versiyon |
      | Patch | v1.0.1   |
      | Minor | v1.1.0   |
      | Major | v2.0.0   |


# -------------------------
# STATUS + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario: Under review refresh sonrasÄ±nda belge iÅŸlemlerini korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir


# -------------------------
# RESTORE + REFRESH
# -------------------------

  @documents @regression @batch11
  Scenario: Restore ve refresh sonrasÄ±nda belge iÅŸlemleri kullanÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

# ============================================================
# BATCH 12 - MOVE / DESTINATION / STATE COMBINATIONS
# ============================================================

  @documents @regression @batch12
  Scenario: Move penceresinde hedef klasÃ¶r seÃ§ilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    Then Move penceresinde hedef klasÃ¶r seÃ§ili olmalÄ±dÄ±r
    And kullanÄ±cÄ± Move iÅŸlemini iptal eder

  @documents @regression @batch12
  Scenario: SeÃ§ilen Move iÅŸlemi iptal edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini iptal eder
    Then belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r
    And Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Belge baÅŸka klasÃ¶re taÅŸÄ±nabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Move penceresi kapanmalÄ±dÄ±r
    And belge hedef klasÃ¶rde olmalÄ±dÄ±r

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda hedef klasÃ¶r belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda belge detay adresi korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin detay adresi kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then belge detay adresi Move sonrasÄ±nda deÄŸiÅŸmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda belge baÅŸlÄ±ÄŸÄ± korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda belge versiyonu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin mevcut versiyonu kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then belge versiyonu deÄŸiÅŸmemelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda Download kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Download butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda Tag giriÅŸi kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda New version kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move sonrasÄ±nda Check out kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Check out butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda hedef klasÃ¶r korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda belge baÅŸlÄ±ÄŸÄ± korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And yÃ¼klenen belgenin baÅŸlÄ±ÄŸÄ± kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge baÅŸlÄ±ÄŸÄ± deÄŸiÅŸmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda belge versiyonu korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And belgenin mevcut versiyonu kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge versiyonu deÄŸiÅŸmemelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda Move kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda Tag giriÅŸi kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda Download kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Download butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Move ve refresh sonrasÄ±nda New version kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario Outline: Yeni versiyonlu belge Move sonrasÄ±nda versiyonunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And belgenin mevcut versiyonu kaydedilmiÅŸtir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then belge versiyonu deÄŸiÅŸmemelidir
    And hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir
    Examples:
      | tip   |
      | Patch |
      | Minor |
      | Major |

  @documents @regression @batch12
  Scenario Outline: Yeni versiyonlu belge Move sonrasÄ±nda iÅŸlemlerini korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "<tip>" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir
    And Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir
    And Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
    Examples:
      | tip   |
      | Patch |
      | Minor |
      | Major |

  @documents @regression @batch12
  Scenario: Under review belge baÅŸka klasÃ¶re taÅŸÄ±nabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir
    And belgenin statusu Under review olmalÄ±dÄ±r

  @documents @regression @batch12
  Scenario: Under review belge Move ve refresh sonrasÄ±nda statusunu korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belgenin statusu Under review olmalÄ±dÄ±r
    And hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Checkout sahibi belgeyi baÅŸka klasÃ¶re taÅŸÄ±yabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir
    And belge checkout durumunda olmalÄ±dÄ±r

  @documents @regression @batch12
  Scenario: Checkout sahibi Move sonrasÄ±nda Check in yapabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge checkout durumunda olmamalÄ±dÄ±r
    And hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Restore edilen belge baÅŸka klasÃ¶re taÅŸÄ±nabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    Then hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch12
  Scenario: Restore edilen belge Move ve refresh sonrasÄ±nda aktif kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    And benzersiz bir Move hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    And kullanÄ±cÄ± Move baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± Move penceresinde hedef klasÃ¶rÃ¼ seÃ§er
    And kullanÄ±cÄ± Move iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then belge detay sayfasÄ± aktif olarak gÃ¶rÃ¼ntÃ¼lenmelidir
    And hedef klasÃ¶r adÄ± belge detayÄ±nda gÃ¶rÃ¼ntÃ¼lenmelidir

# ============================================================
  # BATCH 13 - COPY / DESTINATION / INDEPENDENCE
  # ============================================================

  @documents @regression @batch13
  Scenario: Copy penceresi aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy penceresi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy penceresinde aÃ§Ä±klama gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy aÃ§Ä±klamasÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayÄ±lan baÅŸlÄ±k doÄŸru oluÅŸturulmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy varsayÄ±lan baÅŸlÄ±ÄŸÄ± kaynak belge baÅŸlÄ±ÄŸÄ± ve copy eki olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy penceresinde varsayÄ±lan hedef Root olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy varsayÄ±lan hedef klasÃ¶rÃ¼ Root olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy baÅŸlÄ±ÄŸÄ± deÄŸiÅŸtirilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    Then Copy baÅŸlÄ±ÄŸÄ± girilen deÄŸer olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy baÅŸlÄ±ÄŸÄ± boÅŸ bÄ±rakÄ±ldÄ±ÄŸÄ±nda varsayÄ±lan baÅŸlÄ±k kullanÄ±lmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±nÄ± boÅŸ bÄ±rakÄ±r
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Copy penceresi kapanmalÄ±dÄ±r
    And kopyalanan belge varsayÄ±lan copy baÅŸlÄ±ÄŸÄ±yla gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy iÅŸlemi Cancel ile iptal edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy iÅŸlemini iptal eder
    Then Copy penceresi kapanmalÄ±dÄ±r
    And belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy penceresi X ile kapatÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy penceresini X ile kapatÄ±r
    Then Copy penceresi kapanmalÄ±dÄ±r
    And belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Cancel sonrasÄ±nda Copy tekrar aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy iÅŸlemini iptal eder
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy penceresi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: X ile kapatma sonrasÄ±nda Copy tekrar aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy penceresini X ile kapatÄ±r
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    Then Copy penceresi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy hedef klasÃ¶rÃ¼ seÃ§ilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    And benzersiz bir Copy hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy hedef klasÃ¶rÃ¼nÃ¼ seÃ§er
    Then Copy hedef klasÃ¶rÃ¼ seÃ§ili olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Hedef klasÃ¶r seÃ§ildikten sonra Copy iptal edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    And benzersiz bir Copy hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy hedef klasÃ¶rÃ¼nÃ¼ seÃ§er
    And kullanÄ±cÄ± Copy iÅŸlemini iptal eder
    Then Copy penceresi kapanmalÄ±dÄ±r
    And belge detay sayfasÄ±nda kalÄ±nmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Ã–zel baÅŸlÄ±kla Root klasÃ¶rÃ¼ne Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Copy penceresi kapanmalÄ±dÄ±r
    And kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
    And kopyalanan belge kaynak belgeden farklÄ± detay adresine sahip olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Ã–zel baÅŸlÄ±kla farklÄ± klasÃ¶re Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    And benzersiz bir Copy hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy hedef klasÃ¶rÃ¼nÃ¼ seÃ§er
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
    And kopyalanan belge kaynak belgeden farklÄ± detay adresine sahip olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: VarsayÄ±lan baÅŸlÄ±kla Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
    And kopyalanan belge kaynak belgeden farklÄ± detay adresine sahip olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy oluÅŸturulduktan sonra kaynak belge eriÅŸilebilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± kaynak belge detayÄ±na dÃ¶ner
    Then kaynak belge baÅŸlÄ±ÄŸÄ± korunmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy oluÅŸturulduktan sonra kopya belge tekrar aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kopyalanan belge kaynak belgeden farklÄ± detay adresine sahip olmalÄ±dÄ±r
    And kullanÄ±cÄ± kaynak belge detayÄ±na dÃ¶ner
    And kullanÄ±cÄ± kopya belge detayÄ±na dÃ¶ner
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasÄ±nda baÅŸlÄ±ÄŸÄ±nÄ± korumalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge Download iÅŸlemini desteklemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi indirir
    Then belge dosyasÄ± baÅŸarÄ±yla indirilmelidir

  @documents @regression @batch13
  Scenario: Copy belge Preview iÅŸlemini desteklemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then belge Ã¶nizlemesi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede New version kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Move kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Classify kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Tag giriÅŸi kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Tag giriÅŸ alanÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede Check out kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Check out butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgede tekrar Copy kullanÄ±labilir olmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then Copy butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge soft delete yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi soft delete yapar
    Then belge Deleted durumunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge soft delete sonrasÄ± restore edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge artÄ±k Deleted durumunda olmamalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Restore edilen Copy belge aktif kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi soft delete yapar
    And kullanÄ±cÄ± belgeyi restore eder
    Then belge detay sayfasÄ± aktif olarak gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge checkout yapÄ±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi checkout yapar
    Then belge checkout durumunda olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy belge checkout ve check in dÃ¶ngÃ¼sÃ¼nÃ¼ desteklemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± belgeyi check in yapar
    Then belge checkout durumunda olmamalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden Copy oluÅŸturabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Checkout sahibi kaynak belgeden farklÄ± klasÃ¶re Copy oluÅŸturabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    And benzersiz bir Copy hedef klasÃ¶rÃ¼ oluÅŸturulmuÅŸtur
    When kullanÄ±cÄ± belgeyi checkout yapar
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy hedef klasÃ¶rÃ¼nÃ¼ seÃ§er
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Under review kaynak belgeden Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± belgenin status bilgisini deÄŸiÅŸtirir
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Patch versiyonlu kaynak belgeden Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Minor versiyonlu kaynak belgeden Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Major versiyonlu kaynak belgeden Copy oluÅŸturulabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    And kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    Then kopyalanan belge baÅŸlÄ±ÄŸÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belgeye Patch versiyonu eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.0.1" olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy belgeye Minor versiyonu eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v1.1.0" olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy belgeye Major versiyonu eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then belge versiyonu "v2.0.0" olmalÄ±dÄ±r

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasÄ±nda Download kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Download butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasÄ±nda Move kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch13
  Scenario: Copy belge refresh sonrasÄ±nda Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    And Batch 13 kaynak belge bilgileri kaydedilmiÅŸtir
    When kullanÄ±cÄ± Copy penceresini aÃ§ar
    And kullanÄ±cÄ± Copy baÅŸlÄ±ÄŸÄ±na benzersiz bir deÄŸer girer
    And kullanÄ±cÄ± Copy iÅŸlemini onaylar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir


  # ============================================================
  # Batch 14 - Version Labels / Version History
  # ============================================================

  @documents @regression @batch14
  Scenario: Version history yeni belgede gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then Version history gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Yeni belgede v1.0.0 version history satÄ±rÄ± gÃ¶rÃ¼ntÃ¼lenmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    Then "v1.0.0" version history satÄ±rÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: v1.0.0 iÃ§in label ekleme alanÄ± aÃ§Ä±labilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    Then version label ekleme alanÄ± aÃ§Ä±k olmalÄ±dÄ±r

  @documents @regression @batch14
  Scenario: Version label input girilen deÄŸeri gÃ¶stermelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    Then version label input deÄŸeri girilen deÄŸer olmalÄ±dÄ±r

  @documents @regression @batch14
  Scenario: v1.0.0 versiyonuna label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Eklenen version label refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Version label ekleme iÅŸlemi iptal edilebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label iÅŸlemini iptal eder
    Then iptal edilen version label gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: Eklenen version label silinebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± eklenen version labelÄ± siler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: Silinen version label refresh sonrasÄ±nda geri gelmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± eklenen version labelÄ± siler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: AynÄ± versiyona iki farklÄ± label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± ikinci version labelÄ± ekler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir
    And ikinci version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Ä°ki labeldan biri silindiÄŸinde diÄŸeri korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± ikinci version labelÄ± ekler
    And kullanÄ±cÄ± eklenen version labelÄ± siler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmemelidir
    And ikinci version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Tire iÃ§eren version label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± version label olarak "release-candidate" girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Alt Ã§izgi iÃ§eren version label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± version label olarak "release_candidate" girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: SayÄ±sal version label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± version label olarak "2026" girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: BÃ¼yÃ¼k kÃ¼Ã§Ã¼k harf iÃ§eren version label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± version label olarak "ReleaseCandidate" girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Nokta iÃ§eren version label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± version label olarak "release.1" girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Patch sonrasÄ± v1.0.1 history satÄ±rÄ± oluÅŸmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then "v1.0.1" version history satÄ±rÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Patch versiyonuna label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.1" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.1" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Patch label refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.1" versiyonuna benzersiz label ekler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen label "v1.0.1" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Patch label eski v1.0.0 satÄ±rÄ±nda gÃ¶rÃ¼nmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.1" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: Eski v1.0.0 versiyonuna Patch sonrasÄ±nda label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Eski version label yeni Patch satÄ±rÄ±nda gÃ¶rÃ¼nmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.1" versiyonunda gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: Minor sonrasÄ± v1.1.0 history satÄ±rÄ± oluÅŸmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then "v1.1.0" version history satÄ±rÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Minor versiyonuna label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.1.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.1.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Minor label refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.1.0" versiyonuna benzersiz label ekler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen label "v1.1.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Minor label eski v1.0.0 satÄ±rÄ±nda gÃ¶rÃ¼nmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.1.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: Major sonrasÄ± v2.0.0 history satÄ±rÄ± oluÅŸmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then "v2.0.0" version history satÄ±rÄ± gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Major versiyonuna label eklenebilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v2.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v2.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Major label refresh sonrasÄ±nda korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v2.0.0" versiyonuna benzersiz label ekler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen label "v2.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Major label eski v1.0.0 satÄ±rÄ±nda gÃ¶rÃ¼nmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v2.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmemelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Patch oluÅŸturulduktan sonra korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Minor oluÅŸturulduktan sonra korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Minor" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: v1.0.0 label Major oluÅŸturulduktan sonra korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Major" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklemek mevcut document version deÄŸerini deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then belge versiyonu "v1.0.0" olmalÄ±dÄ±r

  @documents @regression @batch14
  Scenario: Label silmek mevcut document version deÄŸerini deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± eklenen version labelÄ± siler
    Then belge versiyonu "v1.0.0" olmalÄ±dÄ±r

  @documents @regression @batch14
  Scenario: Label ekleme iptali mevcut version deÄŸerini deÄŸiÅŸtirmemelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label iÅŸlemini iptal eder
    Then belge versiyonu "v1.0.0" olmalÄ±dÄ±r

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Download kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then Download butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Preview kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then admin iÃ§in Preview butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Move kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then Move iÅŸlemi gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Copy kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then Copy butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklendikten sonra New version kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then New version butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Label eklendikten sonra Classify kullanÄ±labilir kalmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then Classify butonu gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Ä°ki label refresh sonrasÄ±nda birlikte korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± ikinci version labelÄ± ekler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmelidir
    And ikinci version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Ä°lk label silinip refresh yapÄ±ldÄ±ÄŸÄ±nda ikinci label korunmalÄ±dÄ±r
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± "v1.0.0" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± benzersiz bir version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    When kullanÄ±cÄ± ikinci version labelÄ± ekler
    And kullanÄ±cÄ± eklenen version labelÄ± siler
    And kullanÄ±cÄ± belge detay sayfasÄ±nÄ± yeniler
    Then eklenen version label gÃ¶rÃ¼ntÃ¼lenmemelidir
    And ikinci version label gÃ¶rÃ¼ntÃ¼lenmelidir

  @documents @regression @batch14
  Scenario: Patch ve eski versiyon farklÄ± label taÅŸÄ±yabilmelidir
    Given admin kullanÄ±cÄ± benzersiz bir TXT belge yÃ¼klemiÅŸtir
    When kullanÄ±cÄ± yeni versiyon sayfasÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± "Patch" versiyon tipini seÃ§er
    And kullanÄ±cÄ± yeni versiyon dosyasÄ±nÄ± yÃ¼kler
    And kullanÄ±cÄ± yeni versiyonu kaydeder
    When kullanÄ±cÄ± "v1.0.0" versiyonuna benzersiz label ekler
    Then eklenen label "v1.0.0" versiyonunda gÃ¶rÃ¼ntÃ¼lenmelidir
    When kullanÄ±cÄ± "v1.0.1" versiyonu iÃ§in label ekleme alanÄ±nÄ± aÃ§ar
    And kullanÄ±cÄ± ikinci benzersiz version label girer
    And kullanÄ±cÄ± version label Add baÄŸlantÄ±sÄ±na tÄ±klar
    Then ikinci version label gÃ¶rÃ¼ntÃ¼lenmelidir
 # =====================================================
# BATCH 15 - DOCUMENT RENAME
# PATCH /v1/documents/{id}/title
# =====================================================

@batch15 @documents @rename @endpoint_PATCH_v1_documents_id_title
Scenario: B15-01 Admin document detail sayfasında rename edit kontrolünü görebilir
  Given Batch 15 için yeni bir document oluşturulur
  Then document rename edit butonu görüntülenmelidir

@batch15 @documents @rename
Scenario: B15-02 Rename edit kontrolü inline rename editorunu açar
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then document rename editoru görüntülenmelidir

@batch15 @documents @rename
Scenario: B15-03 Rename editoru mevcut document başlığını input içinde gösterir
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename alanında mevcut document başlığı bulunmalıdır

@batch15 @documents @rename
Scenario: B15-04 Rename editorunda confirm kontrolü bulunur
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename confirm butonu görüntülenmelidir

@batch15 @documents @rename
Scenario: B15-05 Rename editorunda cancel kontrolü bulunur
  Given Batch 15 için yeni bir document oluşturulur
  When kullanıcı document rename editorunu açar
  Then rename cancel butonu görüntülenmelidir

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