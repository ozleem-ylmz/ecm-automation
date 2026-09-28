@coverage-gap @concurrency
Feature: Eşzamanlı işlemlerde tutarlılık

  # ---------------------------------------------------------------------
  # Batch 35 — Plan dışı yeni testler. Aynı kaynağa aynı anda gelen isteklerde
  # kaybeden taraf 409 almalı, sunucu hatası olmamalı, kalıcı durum tutarlı kalmalıdır.
  # ---------------------------------------------------------------------

  # ECM kaynağı: db/migrations/0009_document_version.sql UNIQUE(document_id, major, minor, patch);
  # api/rest/error.rs 23505 -> 409 eşlemesi yeni sürüm yolunda uygulanmıyor (kaybeden 500 alıyor).
  @batch35 @CON-01 @known-bug
  Scenario: CON-01 Aynı belgeye eşzamanlı yeni sürüm kayıtlarında sunucu hatası olmaz, sürüm geçmişi tutarlı kalır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "yarış klasörü" oluşturulmuştur
    And yönetici "yarış klasörü" klasörüne "yarış belgesi" belgesini yüklemiştir
    When yönetici "yarış belgesi" belgesine aynı anda 4 yeni sürüm kaydetmeyi dener
    Then eşzamanlı sürüm isteklerinin hiçbiri sunucu hatası vermemeli ve "yarış belgesi" belgesinin sürüm geçmişi tutarlı kalmalıdır

  # ECM kaynağı: klasör adı (parent_id, name) benzersiz indeksi -> 409 "A folder with this name already exists."
  @batch35 @CON-02
  Scenario: CON-02 Aynı adla eşzamanlı alt klasör isteklerinden yalnız biri oluşur, diğerleri 409 alır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "yarış klasörü" oluşturulmuştur
    Then "yarış klasörü" altında aynı adla 5 eşzamanlı alt klasör isteğinden tam olarak biri oluşmalı, diğerleri 409 almalıdır

  # ECM kaynağı: POST /tags get-or-create (TAG-03) — eşzamanlı çağrıda da tek kayıt
  @batch35 @CON-03
  Scenario: CON-03 Aynı etiketin eşzamanlı oluşturulması tek etiket ve tek kimlik üretir
    Given yönetici API oturumu açmıştır
    Then aynı etiket adıyla 5 eşzamanlı istek tek bir etiket oluşturmalı ve hepsi aynı kimliği döndürmelidir

  # ECM kaynağı: engine/documents/locks.rs checkout (başkasında kilit -> 409 conflict)
  @batch35 @CON-04 @multiuser
  Scenario: CON-04 Yönetici ve kullanıcı aynı anda checkout ettiğinde tam olarak biri kilidi alır
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "yarış klasörü" oluşturulmuştur
    And "yarış klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "yarış klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    Then yönetici ve "yazar" "kilit belgesi" belgesini aynı anda checkout ettiğinde tam olarak biri kilidi almalı, diğeri 409 almalıdır
