@coverage-gap @search
Feature: Belge listesi, arama, facet'ler ve kayıtlı aramalar

  # ---------------------------------------------------------------------
  # Batch 29 — Search / facets / saved searches (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # Tüm sorgular senaryoya özgü başlık önekiyle sınırlanır.
  # ---------------------------------------------------------------------

  # ECM kaynağı: api/rest/documents_query.rs (status, folder_id, created_by, sort_by, order)
  @batch29 @SRCH-01
  Scenario: SRCH-01 Belge listesi durum, klasör, sahip filtreleri ve sıralamayı doğru uygular
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "birinci klasör" oluşturulmuştur
    And yetki devralması kapalı "ikinci klasör" oluşturulmuştur
    And "ikinci klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yönetici" "birinci klasör" klasörüne önekli "a" başlıklı "belge a" belgesini yüklemiştir
    And "yönetici" "birinci klasör" klasörüne önekli "b" başlıklı "belge b" belgesini yüklemiştir
    And "yazar" "ikinci klasör" klasörüne önekli "c" başlıklı "belge c" belgesini yüklemiştir
    And "belge b" belgesi API ile "under_review" durumuna getirilmiştir
    Then önekli listede "status=under_review" filtresi yalnız "belge b" belgelerini döndürmelidir
    And önekli listede "status=draft" filtresi yalnız "belge a, belge c" belgelerini döndürmelidir
    And önekli listede "folder_id={ikinci klasör}" filtresi yalnız "belge c" belgelerini döndürmelidir
    And önekli listede "created_by={yazar}" filtresi yalnız "belge c" belgelerini döndürmelidir
    And önekli listede "folder_id={birinci klasör}&status=draft" filtresi yalnız "belge a" belgelerini döndürmelidir
    And önekli listede "sort_by=title&order=asc" sıralaması "belge a, belge b, belge c" sırasını vermelidir
    And önekli listede "sort_by=title&order=desc" sıralaması "belge c, belge b, belge a" sırasını vermelidir

  # ECM kaynağı: documents_query.rs limit 1..200, offset >= 0
  @batch29 @SRCH-02
  Scenario: SRCH-02 Sayfalama sınırları uygulanır ve sayfalar örtüşmez
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "sayfa klasörü" oluşturulmuştur
    And "yönetici" "sayfa klasörü" klasörüne önekli "a" başlıklı "belge a" belgesini yüklemiştir
    And "yönetici" "sayfa klasörü" klasörüne önekli "b" başlıklı "belge b" belgesini yüklemiştir
    And "yönetici" "sayfa klasörü" klasörüne önekli "c" başlıklı "belge c" belgesini yüklemiştir
    And "yönetici" "sayfa klasörü" klasörüne önekli "d" başlıklı "belge d" belgesini yüklemiştir
    And "yönetici" "sayfa klasörü" klasörüne önekli "e" başlıklı "belge e" belgesini yüklemiştir
    Then önekli liste 2'li sayfalarla örtüşmeden toplam 5 belge vermelidir
    When yönetici belge listesini "limit=0" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "'limit' must be at least 1" içermelidir
    When yönetici belge listesini "limit=10000" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "'limit' must be at most 200" içermelidir
    When yönetici belge listesini "offset=-1" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "'offset' must not be negative" içermelidir

  # ECM kaynağı: GET /documents/facets (aynı filtre kümesi) + durum değişikliği
  @batch29 @SRCH-03
  Scenario: SRCH-03 Aynı filtre için facet sayıları listeyle tutarlıdır ve durum değişince güncellenir
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "facet klasörü" oluşturulmuştur
    And "yönetici" "facet klasörü" klasörüne önekli "a" başlıklı "belge a" belgesini yüklemiştir
    And "yönetici" "facet klasörü" klasörüne önekli "b" başlıklı "belge b" belgesini yüklemiştir
    And "yönetici" "facet klasörü" klasörüne önekli "c" başlıklı "belge c" belgesini yüklemiştir
    And "belge c" belgesi API ile "under_review" durumuna getirilmiştir
    Then önekli facet'lerde durum sayıları "draft=2, under_review=1" olmalı ve listeyle tutarlı olmalıdır
    When "belge a" belgesi API ile "published" durumuna getirilmiştir
    Then önekli facet'lerde durum sayıları "draft=1, under_review=1, published=1" olmalı ve listeyle tutarlı olmalıdır

  # ECM kaynağı: başlık / etiket araması + soft delete / restore görünürlüğü
  @batch29 @SRCH-04
  Scenario: SRCH-04 Yeniden adlandırma, etiket kaldırma, silme ve geri yükleme sonrası arama sonuçları güncellenir
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "arama klasörü" oluşturulmuştur
    And "yönetici" "arama klasörü" klasörüne önekli "değişen" başlıklı "değişen belge" belgesini yüklemiştir
    And "değişen belge" belgesine "konu" arama etiketi eklenmiştir
    And "değişen belge" belgesinin mevcut durumu kaydedilmiştir
    When "yönetici" "değişen belge" belgesinde "yeniden adlandırma" işlemini dener
    Then "<güncel başlık>" başlık aramasında "değişen belge" belgesi "bulunmalıdır"
    And "<eski başlık>" başlık aramasında "değişen belge" belgesi "bulunmalıdır"
    And "konu" etiket aramasında "değişen belge" belgesi "bulunmalıdır"
    When yönetici "değişen belge" belgesinden "konu" etiketini kaldırır
    Then "konu" etiket aramasında "değişen belge" belgesi "bulunmamalıdır"
    When "değişen belge" belgesi API ile soft delete edilmiştir
    Then "<güncel başlık>" başlık aramasında "değişen belge" belgesi "bulunmamalıdır"
    When yönetici "değişen belge" belgesini geri yükler
    Then "<güncel başlık>" başlık aramasında "değişen belge" belgesi "bulunmalıdır"

  # ECM kaynağı: liste ve facet sorguları aynı ACL filtresini uygular
  @batch29 @SRCH-08
  Scenario: SRCH-08 İzin verilince belge kullanıcının arama ve facet sonuçlarına girer, kaldırılınca çıkar
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And "arayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "arayan" API ile giriş yapmıştır
    And yetki devralması kapalı "izole klasör" oluşturulmuştur
    And "yönetici" "izole klasör" klasörüne önekli "gizli" başlıklı "izole belge" belgesini yüklemiştir
    Then "arayan" önekli listede ve facet'lerde 0 belge görmelidir
    When "izole klasör" klasöründe "arayan" kullanıcısına "viewer" rolü verilmiştir
    Then "arayan" önekli listede ve facet'lerde 1 belge görmelidir
    When yönetici "izole klasör" klasöründe "arayan" kullanıcısının izin kuralını kaldırır
    Then "arayan" önekli listede ve facet'lerde 0 belge görmelidir

  # ECM kaynağı: documents_query.rs tag_names + tag_mode any/all; geçersiz tag id
  @batch29 @SRCH-05
  Scenario: SRCH-05 tag_mode any en az bir etiketi, all tüm etiketleri taşıyan belgeleri döndürür
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "etiket klasörü" oluşturulmuştur
    And "yönetici" "etiket klasörü" klasörüne önekli "iki etiketli" başlıklı "belge xy" belgesini yüklemiştir
    And "yönetici" "etiket klasörü" klasörüne önekli "tek etiketli" başlıklı "belge x" belgesini yüklemiştir
    And "belge xy" belgesine "x" arama etiketi eklenmiştir
    And "belge xy" belgesine "y" arama etiketi eklenmiştir
    And "belge x" belgesine "x" arama etiketi eklenmiştir
    Then "x" ve "y" etiketleriyle "any" modunda arama yalnız "belge xy, belge x" belgelerini döndürmelidir
    And "x" ve "y" etiketleriyle "all" modunda arama yalnız "belge xy" belgelerini döndürmelidir
    When yönetici belge listesini "tags=gecersiz-kimlik" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "invalid tag id" içermelidir

  # ECM kaynağı: documents_query.rs props (class_id zorunlu) + engine tip/operatör kontrolleri
  @batch29 @SRCH-06
  Scenario: SRCH-06 Özelliğe göre filtre sınıf ister, bilinmeyen özellik, uygunsuz operatör ve tip hataları reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "özellik klasörü" oluşturulmuştur
    And yönetici "adet:numeric:zorunlu" özellikleriyle "sayısal sınıf" sınıfını oluşturmuştur
    And "yönetici" "sayısal sınıf" sınıfıyla "özellik klasörü" klasörüne "{\"adet\": 1}" metadatasıyla "bir adet" belgesini yüklemiştir
    And "yönetici" "sayısal sınıf" sınıfıyla "özellik klasörü" klasörüne "{\"adet\": 3}" metadatasıyla "üç adet" belgesini yüklemiştir
    And "yönetici" "sayısal sınıf" sınıfıyla "özellik klasörü" klasörüne "{\"adet\": 5}" metadatasıyla "beş adet" belgesini yüklemiştir
    When yönetici "[{\"key\": \"adet\", \"op\": \"gt\", \"value\": 2}]" özellik filtresiyle "" sınıfında arama yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "class_id is required when filtering by properties." içermelidir
    When yönetici "[{\"key\": \"yok\", \"op\": \"eq\", \"value\": 1}]" özellik filtresiyle "sayısal sınıf" sınıfında arama yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Unknown property 'yok'" içermelidir
    When yönetici "[{\"key\": \"adet\", \"op\": \"contains\", \"value\": \"1\"}]" özellik filtresiyle "sayısal sınıf" sınıfında arama yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Operator 'contains' is not valid" içermelidir
    When yönetici "[{\"key\": \"adet\", \"op\": \"gt\", \"value\": \"abc\"}]" özellik filtresiyle "sayısal sınıf" sınıfında arama yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "must be a numeric" içermelidir

  # ECM kaynağı: SavedSearches.tsx (+ Save search, çip uygular, × siler) + saved-searches doğrulamaları
  @batch29 @SRCH-07
  Scenario: SRCH-07 Kullanıcı aramayı kaydeder, çip filtreyi uygular, silinir; ad 120 karakteri aşamaz
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "arama klasörü" oluşturulmuştur
    And "yönetici" "arama klasörü" klasörüne önekli "kayıtlı" başlıklı "kayıtlı belge" belgesini yüklemiştir
    When yönetici arayüzde önekle başlık filtresi uygulayıp "önekli arama" adıyla aramayı kaydeder
    And yönetici başlık filtresini temizleyip "önekli arama" kayıtlı arama çipine tıklar
    Then başlık filtresi kayıtlı aramadaki önekle doldurulmuş olmalıdır
    When yönetici arayüzde "önekli arama" kayıtlı aramasını siler
    Then "önekli arama" kayıtlı araması artık listelenmemelidir
    When yönetici 121 karakterlik adla kayıtlı arama oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Name must be at most 120 characters." içermelidir
    When yönetici filtre alanı metin olan kayıtlı arama oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır

  # ECM kaynağı: postgres_repository/documents.rs — d.title ILIKE format!("%{q}%") kaçışsız.
  # Türkçe harf duyarsızlığı (RQ-13) şu an çalışıyor; %, _ ve ters eğik çizgi ({BS}) literal değil joker/kaçış olarak işleniyor.
  @batch29 @SRCH-09 @known-bug
  Scenario: SRCH-09 Başlık aramasında özel karakterler literal çalışır, istanbul araması İSTANBUL başlığını bulur
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "özel karakter klasörü" oluşturulmuştur
    And "yönetici" "özel karakter klasörü" klasörüne önekli "50% indirim" başlıklı "yüzde belgesi" belgesini yüklemiştir
    And "yönetici" "özel karakter klasörü" klasörüne önekli "a_b rapor" başlıklı "alt çizgi belgesi" belgesini yüklemiştir
    And "yönetici" "özel karakter klasörü" klasörüne önekli "O'Brien" başlıklı "kesme belgesi" belgesini yüklemiştir
    And "yönetici" "özel karakter klasörü" klasörüne önekli "İSTANBUL Raporu" başlıklı "istanbul belgesi" belgesini yüklemiştir
    And "yönetici" "özel karakter klasörü" klasörüne önekli "ters{BS}eğik" başlıklı "ters eğik belgesi" belgesini yüklemiştir
    Then önekli başlık aramasında "istanbul" terimi yalnız "istanbul belgesi" belgelerini döndürmelidir
    And önekli başlık aramasında "O'" terimi yalnız "kesme belgesi" belgelerini döndürmelidir
    And önekli başlık aramasında "%" terimi yalnız "yüzde belgesi" belgelerini döndürmelidir
    And önekli başlık aramasında "_" terimi yalnız "alt çizgi belgesi" belgelerini döndürmelidir
    And önekli başlık aramasında "{BS}" terimi yalnız "ters eğik belgesi" belgelerini döndürmelidir

  # ECM kaynağı: kayıtlı arama SPA'ya ait filtre anlık görüntüsüdür; çalıştırma liste ACL'iyle yapılır
  @batch29 @SRCH-10
  Scenario: SRCH-10 Kayıtlı arama kullanıcının artık erişemediği belgeleri çalıştırıldığında döndürmez
    Given yönetici API oturumu açmıştır
    And "arayan" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "arayan" API ile giriş yapmıştır
    And yönetici "" özellikleriyle "aranan sınıf" sınıfını oluşturmuştur
    And "aranan sınıf" sınıfında "arayan" kullanıcısına "viewer" rolü verilmiştir
    And yetki devralması kapalı "sınıf klasörü" oluşturulmuştur
    And "sınıf klasörü" klasöründe "arayan" kullanıcısına "viewer" rolü verilmiştir
    And "yönetici" "aranan sınıf" sınıfıyla "sınıf klasörü" klasörüne "{}" metadatasıyla "sınıflı belge" belgesini yüklemiştir
    And "arayan" "aranan sınıf" sınıfı filtresiyle "sınıf araması" kayıtlı aramasını oluşturmuştur
    Then "arayan" "sınıf araması" kayıtlı aramasını çalıştırdığında "sınıflı belge" belgesi "bulunmalıdır"
    When yönetici "aranan sınıf" sınıfındaki "arayan" kullanıcısının izin satırını kaldırır
    Then "arayan" "sınıf araması" kayıtlı aramasını çalıştırdığında "sınıflı belge" belgesi "bulunmamalıdır"

  # ---------------------------------------------------------------------
  # Batch 35 — Plan dışı: sıralama sözleşmesi (documents_query.rs sort_by/order + engine/documents/read.rs)
  # ---------------------------------------------------------------------
  @batch35 @SRT-01
  Scenario: SRT-01 Oluşturma ve güncelleme zamanına göre sıralama doğru, geçersiz sıralama parametreleri reddedilir
    Given yönetici API oturumu açmıştır
    And benzersiz bir arama öneki belirlenmiştir
    And yetki devralması kapalı "sıra klasörü" oluşturulmuştur
    And "yönetici" "sıra klasörü" klasörüne önekli "b" başlıklı "belge b" belgesini yüklemiştir
    And "yönetici" "sıra klasörü" klasörüne önekli "C" başlıklı "belge C" belgesini yüklemiştir
    And "yönetici" "sıra klasörü" klasörüne önekli "a" başlıklı "belge a" belgesini yüklemiştir
    And "yönetici" "sıra klasörü" klasörüne önekli "Ç" başlıklı "belge Ç" belgesini yüklemiştir
    Then önekli listede "sort_by=created_at&order=asc" sıralaması "belge b, belge C, belge a, belge Ç" sırasını vermelidir
    And önekli listede "sort_by=created_at&order=desc" sıralaması "belge Ç, belge a, belge C, belge b" sırasını vermelidir
    When "yönetici" "belge C" belgesinde şu işlemleri başarıyla yapabilmelidir
      | statü under_review |
    Then önekli listede "sort_by=updated_at&order=desc" sıralaması "belge C, belge Ç, belge a, belge b" sırasını vermelidir
    And önekli listede "sort_by=title&order=asc" başlık sıralaması RQ-26 kanıtı olarak kaydedilmelidir; Türkçe kullanıcı beklentisi "belge a, belge b, belge C, belge Ç"
    When yönetici belge listesini "sort_by=bogus" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Invalid sort_by 'bogus'; expected updated_at|created_at|title|prop:<key>." içermelidir
    When yönetici belge listesini "order=sideways" parametresiyle istemeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Invalid order 'sideways'; expected 'asc' or 'desc'" içermelidir
