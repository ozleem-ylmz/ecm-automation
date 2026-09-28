@coverage-gap @documents @status
Feature: Belge durum geçişleri

  # ECM kaynağı: src/engine/documents/admin.rs:26-37 is_legal_transition,
  # :53-60 geçersiz durum değeri, :69-75 "Illegal status transition".
  # draft -> under_review geçişi documents.feature içinde zaten test edildiği için
  # burada tekrar edilmez.
  @STAT-01
  Scenario Outline: STAT-01 <başlangıç> durumundaki belge <hedef> durumuna geçişte <sonuç>
    Given yönetici API oturumu açmıştır
    And yönetici API ile yeni bir sınıfsız "durum belgesi" oluşturmuştur
    And "durum belgesi" belgesi API ile "<başlangıç>" durumuna getirilmiştir
    When yönetici "durum belgesi" belgesinin durumunu "<hedef>" yapmayı dener
    Then son API yanıtının durum kodu <kod> olmalıdır
    And "durum belgesi" belgesinin durumu "<beklenen>" olmalıdır

    Examples: İzin verilen geçişler
      | başlangıç    | hedef        | sonuç            | kod | beklenen     |
      | draft        | published    | kabul edilmeli   | 200 | published    |
      | draft        | archived     | kabul edilmeli   | 200 | archived     |
      | under_review | draft        | kabul edilmeli   | 200 | draft        |
      | under_review | published    | kabul edilmeli   | 200 | published    |
      | under_review | archived     | kabul edilmeli   | 200 | archived     |
      | published    | under_review | kabul edilmeli   | 200 | under_review |
      | published    | archived     | kabul edilmeli   | 200 | archived     |
      | archived     | draft        | kabul edilmeli   | 200 | draft        |

    Examples: Yasak geçişler
      | başlangıç | hedef        | sonuç            | kod | beklenen  |
      | published | draft        | reddedilmeli     | 422 | published |
      | archived  | under_review | reddedilmeli     | 422 | archived  |
      | archived  | published    | reddedilmeli     | 422 | archived  |
      | draft     | bogus        | reddedilmeli     | 422 | draft     |

  # ---------------------------------------------------------------------
  # Batch 24 — Document lifecycle (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: src/api/rest/idempotency.rs (reserve-first; aynı gövde -> kayıtlı yanıt, farklı gövde -> 409)
  @batch24 @DOC-15
  Scenario: DOC-15 Aynı Idempotency-Key ile tekrarlanan yükleme ikinci belge oluşturmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "yükleme klasörü" oluşturulmuştur
    When yönetici "yükleme klasörü" klasörüne aynı Idempotency-Key ile aynı belgeyi iki kez yükler
    Then ikinci yanıt aynı belge kimliğini döndürmeli ve tek belge oluşmalıdır
    When yönetici aynı Idempotency-Key ile farklı gövdeli yükleme gönderir
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata kodu "idempotency_key_reused" olmalıdır
    And "farklı gövdeli belge" başlığıyla hiçbir belge oluşmamış olmalıdır

  # ECM kaynağı: documents.rs soft_delete_document / restore_document (canlı belge restore, silinmiş belge silme -> 404)
  @batch24 @DOC-10
  Scenario: DOC-10 Silinmiş belgeyi tekrar silmek ve silinmemiş belgeyi geri yüklemek hata verir, durum bozulmaz
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "silme klasörü" oluşturulmuştur
    And yönetici "silme klasörü" klasörüne "silinecek belge" belgesini yüklemiştir
    When "yönetici" "silinecek belge" belgesinde "geri yükleme" işlemini dener
    Then son API yanıtının durum kodu 404 olmalıdır
    And "silinecek belge" belgesinin silinme durumu "false" olmalıdır
    When "yönetici" "silinecek belge" belgesinde "soft delete" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "silinecek belge" belgesinin silinme durumu "true" olmalıdır
    When "yönetici" "silinecek belge" belgesinde "soft delete" işlemini dener
    Then son API yanıtının durum kodu 404 olmalıdır
    And "silinecek belge" belgesinin silinme durumu "true" olmalıdır
    When "yönetici" "silinecek belge" belgesinde "geri yükleme" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "silinecek belge" belgesinin silinme durumu "false" olmalıdır
    When "yönetici" "silinecek belge" belgesinde "geri yükleme" işlemini dener
    Then son API yanıtının durum kodu 404 olmalıdır
    And "silinecek belge" belgesinin silinme durumu "false" olmalıdır

  # ECM kaynağı: engine/documents/copy.rs (current_version_only + copied_from SPECIFIC ilişki).
  # Etiketlerin kopyalanıp kopyalanmayacağı plan notuna göre açık gereksinim: kanıt olarak eklenir.
  @batch24 @DOC-13
  Scenario: DOC-13 Kopya yeni kimlik alır, kaynaktan bağımsızdır ve copied_from soyağacı kaydedilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "kopya klasörü" oluşturulmuştur
    And yönetici "kopya klasörü" klasörüne "kaynak belge" belgesini yüklemiştir
    And "kaynak belge" belgesine 2 yama sürümü eklenmiştir
    And "kaynak belge" belgesine "qa-kaynak" etiketi eklenmiştir
    And "kaynak belge" belgesi API ile "under_review" durumuna getirilmiştir
    When yönetici "kaynak belge" belgesini aynı klasöre kopyalar
    Then "kaynak belge" kopyası yeni kimlikli, taslak durumunda ve kaynağın güncel içeriğiyle oluşmalıdır
    And "kaynak belge" kopyasında kaynağa işaret eden copied_from ilişkisi kaynağın o anki sürümüne bağlı olmalıdır
    And kaynağa yeni sürüm eklenince "kaynak belge" kopyası değişmemelidir

  # ECM kaynağı: frontend pages/DocumentNewUnclassified.tsx (title-match -> "Save new version")
  @batch24 @DOC-16
  Scenario: DOC-16 Aynı klasörde aynı başlıkta belge yüklenirken arayüz yeni sürüm önerir ve mevcut belgeye sürüm ekler
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "rapor klasörü" oluşturulmuştur
    And yönetici "rapor klasörü" klasörüne "rapor belgesi" belgesini yüklemiştir
    When yönetici arayüzde "rapor klasörü" klasörüne "rapor belgesi" ile aynı başlıkta dosya yükler
    Then "rapor belgesi" belgesinin sürümü "1.0.1" olmalı ve klasörde o başlıkta tek belge bulunmalıdır

  # ECM kaynağı: frontend hooks/useDocumentPreview.ts previewKind + DocumentDetail.tsx (pdf iframe / image / none)
  @batch24 @DOC-19
  Scenario: DOC-19 PDF ve PNG önizlenir, desteklenmeyen türde önizleme sunulmaz ama indirme çalışır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "önizleme klasörü" oluşturulmuştur
    And yönetici "önizleme klasörü" klasörüne "pdf" türünde "pdf belgesi" belgesini yüklemiştir
    And yönetici "önizleme klasörü" klasörüne "png" türünde "png belgesi" belgesini yüklemiştir
    And yönetici "önizleme klasörü" klasörüne "zip" türünde "zip belgesi" belgesini yüklemiştir
    Then yönetici arayüzde "pdf belgesi" belgesinin önizlemesinde PDF görüntüleyiciyi görmelidir
    And yönetici arayüzde "png belgesi" belgesinin önizlemesinde görselin çözümlendiğini görmelidir
    And yönetici arayüzde "zip belgesi" belgesi için önizleme sunulmamalı ama indirme aynı içeriği vermelidir
