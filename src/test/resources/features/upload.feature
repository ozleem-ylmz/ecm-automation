@coverage-gap @upload
Feature: Dosya yükleme kuralları ve dosya bütünlüğü

  # ---------------------------------------------------------------------
  # Batch 32 — Upload & file handling (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: doc_management_engine.rs engellenen uzantılar (exe, msi, msp, com, bat, cmd, sh, ps1, jar) + MIME doğrulaması
  @batch32 @DOC-04
  Scenario: DOC-04 Engellenen uzantı ve MIME türü yüklenemez, geçersiz MIME reddedilir, belge oluşmaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                           | gövde                                                                                                                                  | durum | mesaj                                              |
      | POST /v1/documents/unclassified | {"title":"{M} exe","metadata":{},"mime_type":"text/plain","original_filename":"a.exe","folder_id":"{yükleme klasörü}","content":[120]}   | 422 | File type '.exe' is not allowed for upload |
      | POST /v1/documents/unclassified | {"title":"{M} sh","metadata":{},"mime_type":"text/plain","original_filename":"a.sh","folder_id":"{yükleme klasörü}","content":[120]}     | 422 | File type '.sh' is not allowed for upload  |
      | POST /v1/documents/unclassified | {"title":"{M} ps1","metadata":{},"mime_type":"text/plain","original_filename":"a.PS1","folder_id":"{yükleme klasörü}","content":[120]}   | 422 | File type '.ps1' is not allowed for upload |
      | POST /v1/documents/unclassified | {"title":"{M} msd","metadata":{},"mime_type":"application/x-msdownload","original_filename":"a.txt","folder_id":"{yükleme klasörü}","content":[120]} | 422 | MIME type 'application/x-msdownload' is not allowed |
      | POST /v1/documents/unclassified | {"title":"{M} abc","metadata":{},"mime_type":"abc","original_filename":"a.txt","folder_id":"{yükleme klasörü}","content":[120]}         | 422 | 'abc' is not a valid MIME type |
    And işaretli başlıkla hiçbir belge oluşmamış olmalıdır

  # ECM kaynağı: boş içerik reddi ("file is empty")
  @batch32 @DOC-05
  Scenario: DOC-05 Boş (0 bayt) dosya yüklenemez
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    Then aşağıdaki istekler beklenen durum ve mesajları döndürmelidir
      | istek                           | gövde                                                                                                                        | durum | mesaj         |
      | POST /v1/documents/unclassified | {"title":"{M} boş","metadata":{},"mime_type":"text/plain","original_filename":"e.txt","folder_id":"{yükleme klasörü}","content":[]} | 422 | file is empty |
    And işaretli başlıkla hiçbir belge oluşmamış olmalıdır

  # ECM kaynağı: MAX_UPLOAD_BYTES = 2 MiB (JSON gövdeli yükleme)
  @batch32 @DOC-06
  Scenario: DOC-06 JSON yükleme 2 MB'ı aşarsa reddedilir, sınırın altı kabul edilir ve bayt bayt aynı indirilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    When yönetici "yükleme klasörü" klasörüne 2096128 baytlık içerikle JSON yüklemesi yapar
    Then yükleme 201 dönmeli ve indirilen içerik yüklenen baytlarla aynı olmalıdır
    When yönetici "yükleme klasörü" klasörüne 2097153 baytlık içerikle JSON yüklemesi yapar
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "File exceeds 2 MB limit" içermelidir

  # ECM kaynağı: DocumentNewUnclassified.tsx (> 2 MB -> /documents/unclassified/stream) + documents.rs multipart kuralları
  @batch32 @DOC-07
  Scenario: DOC-07 2 MB üstü dosya akış ile yüklenir ve bayt bayt aynı indirilir, hatalı multipart istekleri reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    When yönetici arayüzde "yükleme klasörü" klasörüne 5 MB'lık dosya yükler
    Then "akış belgesi" indirildiğinde yüklenen dosyayla bayt bayt aynı olmalıdır
    When yönetici akış ucuna "yalnız dosya parçası" gönderir
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "the 'metadata' part must be sent before the 'file' part" içermelidir
    When yönetici akış ucuna "dosyadan sonra metadata" gönderir
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "the 'metadata' part must be sent before the 'file' part" içermelidir
    When yönetici akış ucuna "yalnız metadata parçası" gönderir
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "missing 'file' part in multipart upload" içermelidir

  # ECM kaynağı: UTF-8 başlık/dosya adı saklama; parantezli dosya adı sunucu hatası (500) veriyor
  @batch32 @DOC-08 @known-bug
  Scenario: DOC-08 Türkçe ve boşluklu başlık ile parantezli dosya adı kaydedilir ve indirmede korunur
    Given yönetici API oturumu açmıştır
    And benzersiz bir doğrulama işareti belirlenmiştir
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    When yönetici "yükleme klasörü" klasörüne "Şirket Sözleşmesi – Ğüşiöç {M}" başlığı ve "Sözleşme dosyası (1).txt" dosya adıyla Türkçe içerikli belge yükler
    Then unicode belgenin başlığı, dosya adı ve içeriği korunmalıdır
