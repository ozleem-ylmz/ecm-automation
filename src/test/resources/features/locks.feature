@coverage-gap @locks
Feature: Checkout kilidi, çok kullanıcılı çalışma ve bayat sayfa durumları

  # ---------------------------------------------------------------------
  # Batch 26 — Locks / multi-user (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: runtime.rs assert_document_access (izin her istekte yeniden değerlendirilir); TagManager.tsx hata gösterimi.
  # Belgeyi başka kullanıcı yazar: yaratıcının otomatik belge izni (RQ-19) sonucu karıştırmasın.
  # "editor" satırı planın rolüdür ve BUG-10'u yakalar (klasör editor rolü düzenleme veremiyor);
  # "manager" satırı bayat sayfa kuralını çalışan bir yazma rolüyle doğrular.
  @batch26 @ACL-16
  Scenario Outline: ACL-16 Belge sayfası açıkken yetkisi kaldırılan kullanıcının sonraki işlemi reddedilir ve yenilemede erişim reddi görür (<klasör rolü>)
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "düzenleyici" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "paylaşım klasörü" oluşturulmuştur
    And "paylaşım klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "paylaşım klasörü" klasöründe "düzenleyici" kullanıcısına "<klasör rolü>" rolü verilmiştir
    And "yazar" "paylaşım klasörü" klasörüne "paylaşılan belge" belgesini yüklemiştir
    And "düzenleyici" tarayıcıda "paylaşılan belge" belgesinin detay sayfasını açmıştır
    And kullanıcı açık sayfada belgeyi düzenleyebilmelidir
    And "paylaşılan belge" belgesinin mevcut durumu kaydedilmiştir
    When yönetici "paylaşım klasörü" klasöründe "düzenleyici" kullanıcısının izin kuralını kaldırır
    And kullanıcı açık sayfada belgeye etiket eklemeyi dener
    Then açık sayfadaki işlem 403 ile reddedilmeli ve hata mesajı arayüzde görünmelidir
    And "paylaşılan belge" belgesi kaydedilen durumundan değişmemiş olmalıdır
    And kullanıcı sayfayı yenilediğinde belgeyi görüntüleme izni olmadığı mesajını görmelidir

    @known-bug
    Examples: Planın rolü (Edit, no upload)
      | klasör rolü |
      | editor      |

    Examples: Çalışan yazma rolü (Upload & edit)
      | klasör rolü |
      | manager     |

  # ECM kaynağı: silinmiş belge üzerinde yazma işlemleri reddedilir; DocumentDetail.tsx hata gösterimi
  @batch26 @DOC-17
  Scenario: DOC-17 Belge sayfası açıkken başka kullanıcı belgeyi silerse sonraki işlem hata verir ve sahte başarı göstermez
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "çalışma klasörü" oluşturulmuştur
    And "çalışma klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "çalışma klasörü" klasörüne "silinecek belge" belgesini yüklemiştir
    And "yazar" tarayıcıda "silinecek belge" belgesinin detay sayfasını açmıştır
    When "silinecek belge" belgesi API ile soft delete edilmiştir
    And "silinecek belge" belgesinin mevcut durumu kaydedilmiştir
    And kullanıcı açık sayfada belgeyi yeniden adlandırmayı dener
    Then açık sayfadaki işlem bir istemci hatasıyla reddedilmeli ve hata mesajı arayüzde görünmelidir
    And "silinecek belge" belgesi kaydedilen durumundan değişmemiş olmalıdır

  # ECM kaynağı: locks.rs acquire (süresi dolan kilit yeniden alınabilir; min TTL uygulanmaz)
  @batch26 @LOCK-01
  Scenario: LOCK-01 Süresi dolan checkout kilidi başka kullanıcı tarafından zorla kaldırmadan alınabilir
    Given yönetici API oturumu açmıştır
    And "birinci yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ikinci yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "birinci yazar" API ile giriş yapmıştır
    And "ikinci yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "birinci yazar" kullanıcısına "manager" rolü verilmiştir
    And "kilit klasörü" klasöründe "ikinci yazar" kullanıcısına "manager" rolü verilmiştir
    And "birinci yazar" "kilit klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    When "birinci yazar" "kilit belgesi" belgesini 2 saniyelik kilitle checkout yapar
    And "ikinci yazar" "kilit belgesi" belgesinde "checkout" işlemini dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And "kilit belgesi" belgesinin kilit süresi dolana kadar beklendiğinde kilit kalkmalıdır
    When "ikinci yazar" "kilit belgesi" belgesinde "checkout" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "kilit belgesi" belgesinin kilidi "ikinci yazar" kullanıcısında olmalıdır
    When "ikinci yazar" "kilit belgesi" belgesinde "checkin" işlemini dener
    Then son API yanıtı başarılı olmalıdır

  # ECM kaynağı: locks.rs checkin_document ("Lock token does not match..." / "This document is not checked out.")
  @batch26 @LOCK-02
  Scenario: LOCK-02 Yanlış kilit anahtarıyla check-in ve checkout edilmemiş belgede check-in reddedilir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And yönetici "kilit klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    And "yönetici" "kilit belgesi" belgesini checkout yapmıştır
    When "yönetici" "kilit belgesi" belgesinde yanlış kilit anahtarıyla check-in yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Lock token does not match the current checkout." içermelidir
    And "kilit belgesi" belgesi kilitli olmalıdır
    When "yönetici" "kilit belgesi" belgesinde doğru kilit anahtarıyla check-in yapmayı dener
    Then son API yanıtı başarılı olmalıdır
    And "kilit belgesi" belgesinin kilidi kalkmış olmalıdır
    When "yönetici" "kilit belgesi" belgesinde doğru kilit anahtarıyla check-in yapmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "This document is not checked out." içermelidir

  # ECM kaynağı: locks.rs checkin_document (lock.locked_by != user -> locked_by_other 409)
  @batch26 @LOCK-03
  Scenario: LOCK-03 Kilidi tutmayan kullanıcı check-in yaparak başkasının kilidini kaldıramaz
    Given yönetici API oturumu açmıştır
    And "kilit sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "diğer yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilit sahibi" API ile giriş yapmıştır
    And "diğer yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "kilit sahibi" kullanıcısına "manager" rolü verilmiştir
    And "kilit klasörü" klasöründe "diğer yazar" kullanıcısına "manager" rolü verilmiştir
    And "kilit sahibi" "kilit klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    And "kilit sahibi" "kilit belgesi" belgesini checkout yapmıştır
    When "diğer yazar" "kilit belgesi" belgesinde kilit anahtarı olmadan check-in yapmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And "kilit belgesi" belgesinin kilidi "kilit sahibi" kullanıcısında olmalıdır

  # ECM kaynağı: locks.rs ensure_not_locked_by_other; DocumentDetail.tsx hata gösterimi (sayfa kilitten önce açıldı)
  @batch26 @LOCK-04
  Scenario: LOCK-04 Sayfa açıldıktan sonra başka kullanıcı kilitlerse değişiklik 409 ile reddedilir ve arayüz bunu gösterir
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "kilit klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    And "yazar" tarayıcıda "kilit belgesi" belgesinin detay sayfasını açmıştır
    When "yönetici" "kilit belgesi" belgesini checkout yapmıştır
    And "kilit belgesi" belgesinin mevcut durumu kaydedilmiştir
    And kullanıcı açık sayfada belgeyi yeniden adlandırmayı dener
    Then açık sayfadaki işlem 409 ile reddedilmeli ve hata mesajı arayüzde görünmelidir
    And "kilit belgesi" belgesi kaydedilen durumundan değişmemiş olmalıdır

  # ECM kaynağı: ROLES.md:18 (restore ve hard delete kilitten muaf); DocumentDetail.tsx butonları lockedByOther ile kapatır (RQ-11).
  # Silinmiş belgeyi kilitleme kurulumu bilinen BUG-06 yolunu kullanır: admin rolündeki (geçici) hesap silinmiş belgeyi
  # checkout edebiliyor; normal kullanıcı için silinmiş belge 403 (folder_inactive) döner.
  @batch26 @LOCK-05
  Scenario: LOCK-05 Hard delete ve restore checkout kilidinden muaftır, arayüz durumu kanıt olarak kaydedilir
    Given yönetici API oturumu açmıştır
    And "kilit sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilit sahibi" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "kilit sahibi" kullanıcısına "manager" rolü verilmiştir
    And "kilit sahibi" "kilit klasörü" klasörüne "kalıcı silinecek belge" belgesini yüklemiştir
    And "kilit sahibi" "kilit klasörü" klasörüne "geri yüklenecek belge" belgesini yüklemiştir
    And "kilitleyen yönetici" isimli "admin" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilitleyen yönetici" API ile giriş yapmıştır
    And "kilit sahibi" "kalıcı silinecek belge" belgesini checkout yapmıştır
    When yönetici kilitli "kalıcı silinecek belge" belgesini kalıcı olarak siler
    Then son API yanıtı başarılı olmalıdır
    And "kalıcı silinecek belge" belgesi artık bulunmamalıdır
    Given "geri yüklenecek belge" belgesi API ile soft delete edilmiştir
    And "kilitleyen yönetici" soft delete edilmiş "geri yüklenecek belge" belgesini checkout yapmayı dener
    And "geri yüklenecek belge" belgesinin kilidi "kilitleyen yönetici" kullanıcısında olmalıdır
    Then yönetici arayüzde silinmiş "geri yüklenecek belge" belgesindeki Restore ve Hard delete butonlarının durumu RQ-11 kanıtı olarak kaydedilmelidir
    When yönetici "geri yüklenecek belge" belgesini geri yüklemeyi dener
    Then son API yanıtı başarılı olmalıdır
    And "geri yüklenecek belge" belgesinin silinme durumu "false" olmalıdır
    And "geri yüklenecek belge" belgesinin kilidi kalkmış olmalıdır

  # ECM kaynağı: engine/documents/admin.rs admin_restore_document ("Clear any stale checkout lock left over from before the soft-delete")
  @batch26 @LOCK-06
  Scenario: LOCK-06 Silinmiş belgede kalan kilit geri yükleme sonrasında temizlenir ve belge yönetilebilir olur
    Given yönetici API oturumu açmıştır
    And "kilit sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilit sahibi" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "kilit sahibi" kullanıcısına "manager" rolü verilmiştir
    And "kilit sahibi" "kilit klasörü" klasörüne "kilitli silinen belge" belgesini yüklemiştir
    And "kilitleyen yönetici" isimli "admin" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilitleyen yönetici" API ile giriş yapmıştır
    And "kilitli silinen belge" belgesi API ile soft delete edilmiştir
    And "kilitleyen yönetici" soft delete edilmiş "kilitli silinen belge" belgesini checkout yapmayı dener
    And "kilitli silinen belge" belgesinin kilidi "kilitleyen yönetici" kullanıcısında olmalıdır
    When yönetici "kilitli silinen belge" belgesini geri yüklemeyi dener
    Then son API yanıtı başarılı olmalıdır
    And "kilitli silinen belge" belgesinin kilidi kalkmış olmalıdır
    And "yönetici" "kilitli silinen belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |

  # ECM kaynağı: hooks/useAutoCheckout.ts (yeni sürüm sayfası kilitler; unmount/Cancel ile bırakır)
  @batch26 @LOCK-07
  Scenario: LOCK-07 Yeni sürüm sayfasını açan kullanıcı belgeyi otomatik kilitler, ayrılınca kilit kalkar
    Given yönetici API oturumu açmıştır
    And "diğer yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "diğer yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "diğer yazar" kullanıcısına "manager" rolü verilmiştir
    And "diğer yazar" "kilit klasörü" klasörüne "sürüm belgesi" belgesini yüklemiştir
    When yönetici arayüzde "sürüm belgesi" belgesinin yeni sürüm sayfasını açar
    Then "sürüm belgesi" belgesinin kilidi "yönetici" kullanıcısında olmalıdır
    When "diğer yazar" "sürüm belgesi" belgesinde "yeniden adlandırma" işlemini dener
    Then son API yanıtının durum kodu 409 olmalıdır
    When yönetici yeni sürüm sayfasından Cancel ile ayrılır
    Then "sürüm belgesi" belgesinin kilidi kalkmış olmalıdır
    And "diğer yazar" "sürüm belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |

  # ECM kaynağı: documents.rs force_release (admin + kapsamlı manager); runtime.rs can_manage_document_scoped
  @batch26 @RBAC-06
  Scenario: RBAC-06 Manager kapsamındaki belgenin kilidini zorla kaldırabilir, kapsam dışındakini kaldıramaz, normal kullanıcı hiç kaldıramaz
    Given yönetici API oturumu açmıştır
    And "ekip yöneticisi" isimli "manager" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "başka yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "ekip yöneticisi" API ile giriş yapmıştır
    And "ekip üyesi" API ile giriş yapmıştır
    And "başka yazar" API ile giriş yapmıştır
    And "ekip" isimli yeni bir grup oluşturulmuştur
    And yönetici "ekip yöneticisi" kullanıcısını "ekip" grubuna ekler
    And yönetici "ekip üyesi" kullanıcısını "ekip" grubuna ekler
    And yetki devralması kapalı "ekip klasörü" oluşturulmuştur
    And yetki devralması kapalı "dış klasör" oluşturulmuştur
    And "ekip klasörü" klasöründe "ekip" grubuna "editor" rolü verilmiştir
    And "ekip klasörü" klasöründe "ekip üyesi" kullanıcısına "manager" rolü verilmiştir
    And "ekip klasörü" klasöründe "başka yazar" kullanıcısına "manager" rolü verilmiştir
    And "dış klasör" klasöründe "ekip üyesi" kullanıcısına "manager" rolü verilmiştir
    And "dış klasör" klasöründe "ekip yöneticisi" kullanıcısına "viewer" rolü verilmiştir
    And "ekip üyesi" "ekip klasörü" klasörüne "kapsam içi belge" belgesini yüklemiştir
    And "ekip üyesi" "dış klasör" klasörüne "kapsam dışı belge" belgesini yüklemiştir
    And "ekip üyesi" "kapsam içi belge" belgesini checkout yapmıştır
    And "ekip üyesi" "kapsam dışı belge" belgesini checkout yapmıştır
    When "ekip yöneticisi" "kapsam içi belge" belgesinin kilidini zorla kaldırmayı dener
    Then son API yanıtı başarılı olmalıdır
    And "kapsam içi belge" belgesinin kilidi kalkmış olmalıdır
    When "ekip yöneticisi" "kapsam dışı belge" belgesinin kilidini zorla kaldırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "kapsam dışı belge" belgesinin kilidi "ekip üyesi" kullanıcısında olmalıdır
    Given "ekip üyesi" "kapsam içi belge" belgesini checkout yapmıştır
    When "başka yazar" "kapsam içi belge" belgesinin kilidini zorla kaldırmayı dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And "kapsam içi belge" belgesinin kilidi "ekip üyesi" kullanıcısında olmalıdır

  # ECM kaynağı: okuma yolu her sayfa yüklemesinde güncel sürümü döner; DocumentDetail.tsx Download
  @batch26 @VER-08
  Scenario: VER-08 Yönetici yeni sürüm yükleyince okuyucu yenilemede güncel sürümü ve yeni içeriği görür
    Given yönetici API oturumu açmıştır
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "sürüm klasörü" oluşturulmuştur
    And "sürüm klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And yönetici "sürüm klasörü" klasörüne "okunan belge" belgesini yüklemiştir
    And "okur" tarayıcıda "okunan belge" belgesinin detay sayfasını açmıştır
    And kullanıcı açık sayfada "v1.0.0" sürümünü görmelidir
    When yönetici "okunan belge" belgesine "QA minor içerik" içerikli minor sürüm ekler
    Then kullanıcı sayfayı yenileyince "v1.1.0" sürümünü görmeli ve indirdiğinde "QA minor içerik" almalıdır

  # ECM kaynağı: kilit kullanıcıya değil satıra bağlıdır; force release admin/kapsamlı manager
  @batch26 @LOCK-08
  Scenario: LOCK-08 Kilidi tutan kullanıcı pasifleştirilse de kilit sürer ve yönetici zorla kaldırabilir
    Given yönetici API oturumu açmıştır
    And "kilit sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "kilit sahibi" API ile giriş yapmıştır
    And yetki devralması kapalı "kilit klasörü" oluşturulmuştur
    And "kilit klasörü" klasöründe "kilit sahibi" kullanıcısına "manager" rolü verilmiştir
    And "kilit sahibi" "kilit klasörü" klasörüne "kilit belgesi" belgesini yüklemiştir
    And "kilit sahibi" "kilit belgesi" belgesini checkout yapmıştır
    When yönetici "kilit sahibi" hesabını pasifleştirir
    Then "kilit belgesi" belgesinin kilidi "kilit sahibi" kullanıcısında olmalıdır
    When "yönetici" "kilit belgesi" belgesinde "yeniden adlandırma" işlemini dener
    Then son API yanıtının durum kodu 409 olmalıdır
    When "yönetici" "kilit belgesi" belgesinin kilidini zorla kaldırmayı dener
    Then son API yanıtı başarılı olmalıdır
    And "yönetici" "kilit belgesi" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
