@coverage-gap @journey
Feature: Uçtan uca arayüz yolculukları

  # ---------------------------------------------------------------------
  # Batch 35 — Plan dışı yeni testler. Tek belge üzerinde arayüzden art arda
  # yapılan işlemler: her adım gerçek tıklama + API ile kalıcı durum doğrulaması
  # (StrengthenSteps), sonunda denetim izi sırası ve aktörü doğrulanır.
  # ---------------------------------------------------------------------

  @batch35 @JRN-01
  Scenario: JRN-01 Yönetici arayüzden bir belgeyi etiketler, sınıflandırır, yeniden adlandırır, yeni sürüm yükler, taşır, kilitler, indirir ve siler
    Given admin kullanıcı API ile oluşturulan benzersiz bir TXT belgenin detay sayfasındadır
    Then "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Classify" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Rename" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Move" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Check out" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Check in" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Preview" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Delete" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And açık belgenin denetim kaydında "document.create, tag.assign, document.classify, document.rename, document.new_version, document.move, document.checkout, document.checkin, document.soft_delete" eylemleri sırasıyla "yönetici" aktörüyle bulunmalıdır

  @batch35 @JRN-02 @multiuser
  Scenario: JRN-02 Klasörde manager yetkili normal kullanıcı kendi belgesinde arayüzden tüm düzenleme işlemlerini yapar
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "yazar klasörü" oluşturulmuştur
    And "yazar klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "yazar klasörü" klasörüne "yazar belgesi" belgesini yüklemiştir
    And "yazar" arayüzde kendi yüklediği "yazar belgesi" belgesinin detay sayfasındadır
    Then "Tag" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Rename" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "New version" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Check out" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Check in" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Preview" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Download" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And "Delete" işlemi açık belgede gerçekten çalışmalı ve sonucu API ile doğrulanmalıdır
    And açık belgenin denetim kaydında "document.create, tag.assign, document.rename, document.new_version, document.checkout, document.checkin, document.soft_delete" eylemleri sırasıyla "yazar" aktörüyle bulunmalıdır
