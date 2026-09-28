@coverage-gap @downloads
Feature: İndirme sözleşmesi ve dosya bütünlüğü

  # ---------------------------------------------------------------------
  # Batch 35 — Plan dışı yeni testler (uçtan uca doğrulama genişletmesi)
  # Arayüzün indirme dosya adı DL-01 (documents.feature) içindedir.
  # ---------------------------------------------------------------------

  # ECM kaynağı: api/rest/documents.rs parse_range_header (tek aralık, "bytes=-N" ve çoklu aralık desteklenmez -> 200 tam dosya)
  # + build_attachment_response (206 + Content-Range, her yanıtta Accept-Ranges: bytes), engine 416 range_not_satisfiable
  @batch35 @DL-02
  Scenario: DL-02 Belge içeriği HTTP Range ile doğru parçalar halinde indirilir, geçersiz aralık 416 döner
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "indirme klasörü" oluşturulmuştur
    And yönetici "indirme klasörü" klasörüne "aralik.txt" adlı "text/plain" türünde "0123456789ABCDEFGHIJ" içerikli "aralıklı belge" belgesini yüklemiştir
    Then "aralıklı belge" belgesinin içeriği şu bayt aralıklarıyla doğru indirilmelidir:
      | aralık        | durum | içerik aralığı | gövde                |
      | (yok)         | 200   | -              | 0123456789ABCDEFGHIJ |
      | bytes=2-5     | 206   | bytes 2-5/20   | 2345                 |
      | bytes=5-      | 206   | bytes 5-19/20  | 56789ABCDEFGHIJ      |
      | bytes=0-0     | 206   | bytes 0-0/20   | 0                    |
      | bytes=18-100  | 206   | bytes 18-19/20 | IJ                   |
      | bytes=25-30   | 416   | -              | -                    |
      | bytes=5-2     | 416   | -              | -                    |
      | bytes=-3      | 200   | -              | 0123456789ABCDEFGHIJ |
      | bytes=0-1,4-5 | 200   | -              | 0123456789ABCDEFGHIJ |
    When "yönetici" "aralıklı belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeni sürüm |
    Then "aralıklı belge" belgesinin ilk sürüm içeriği şu bayt aralıklarıyla doğru indirilmelidir:
      | aralık       | durum | içerik aralığı | gövde |
      | bytes=10-14  | 206   | bytes 10-14/20 | ABCDE |
      | bytes=20-    | 416   | -              | -     |

  # ECM kaynağı: document_version.checksum_sha256 / file_size_bytes (GET /documents/{id}/versions/{vid})
  @batch35 @DL-03
  Scenario: DL-03 Her sürümün kayıtlı SHA-256 sağlama toplamı ve boyutu indirilen baytlarla birebir aynıdır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "indirme klasörü" oluşturulmuştur
    And yönetici "indirme klasörü" klasörüne "butunluk.txt" adlı "text/plain" türünde "İlk sürüm içeriği — Ğüşiöç" içerikli "bütünlük belgesi" belgesini yüklemiştir
    When "yönetici" "bütünlük belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeni sürüm |
      | yeni sürüm |
    Then "bütünlük belgesi" belgesinin her sürümünün sağlama toplamı ve boyutu indirilen baytlarla aynı olmalıdır

  # ECM kaynağı: api/rest/routes.rs with_security_headers (nosniff, DENY, CSP) + Content-Disposition: attachment
  @batch35 @DL-04 @security
  Scenario Outline: DL-04 Etkin içerik (<tür>) indirildiğinde çalıştırılmaz, güvenlik başlıklarıyla ek olarak sunulur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "indirme klasörü" oluşturulmuştur
    And yönetici "indirme klasörü" klasörüne "<dosya>" adlı "<tür>" türünde "<içerik>" içerikli "etkin belge" belgesini yüklemiştir
    Then "etkin belge" belgesi indirildiğinde etkin içerik güvenlik başlıklarıyla ek olarak sunulmalıdır
    Examples:
      | tür           | dosya     | içerik                                                                   |
      | text/html     | x.html    | <script>alert(document.cookie)</script>                                  |
      | image/svg+xml | x.svg     | <svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'></svg>          |

  # ECM kaynağı: engine/documents/filename.rs build_download_filename + documents.rs build_attachment_response
  # (format!("attachment; filename=\"{}\"") ham UTF-8; RFC 6266 filename* yok). Fetch/Java/Python istemcileri
  # başlık baytlarını ISO-8859-1 okur -> "SÃ¶zleÅŸme". BUG-17 düzeltilirse arayüz de bu adı gösterecek.
  @batch35 @DL-05 @known-bug
  Scenario: DL-05 Türkçe dosya adı indirme başlığında standart istemcinin çözebileceği biçimde korunur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "indirme klasörü" oluşturulmuştur
    And yönetici "indirme klasörü" klasörüne "Sözleşme özeti.txt" adlı "text/plain" türünde "içerik" içerikli "türkçe adlı belge" belgesini yüklemiştir
    Then "türkçe adlı belge" belgesinin indirme başlığı "Sözleşme özeti.txt" dosya adını standart HTTP istemcisinin çözebileceği biçimde taşımalıdır
