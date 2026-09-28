@coverage-gap @audit
Feature: Denetim kayıtları ve geçmiş

  # ---------------------------------------------------------------------
  # Batch 33 — Audit & history (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # Denetim kayıtları eşzamansız yazılır; doğrulamalar koşul tabanlı yoklama ile yapılır.
  # ---------------------------------------------------------------------

  # ECM kaynağı: api/rest/audit.rs list_audit_logs (target_id) + belge yaşam döngüsü emit_audit çağrıları
  @batch33 @AUD-01
  Scenario: AUD-01 Belge yaşam döngüsü işlemleri doğru aktör, eylem ve önce/sonra değerleriyle denetime yazılır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yetki devralması kapalı "hedef klasör" oluşturulmuştur
    And yönetici "denetim klasörü" klasörüne "denetlenen belge" belgesini yüklemiştir
    When "yönetici" "denetlenen belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma   |
      | statü under_review   |
      | taşıma hedef klasör  |
      | soft delete          |
      | geri yükleme         |
    Then "denetlenen belge" belgesinin denetim kayıtları sırasıyla "document.create, document.rename, document.status_change, document.move, document.soft_delete, document.restore" eylemlerini yönetici aktörüyle içermelidir

  # ECM kaynağı: emit_audit yalnız başarılı işlemden sonra çağrılır (403/409/422 kayda geçmez)
  @batch33 @AUD-02
  Scenario: AUD-02 Reddedilen işlemler başarılı işlem olarak denetime yazılmaz
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And "okur" API ile giriş yapmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And "denetim klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "denetim klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "denetim klasörü" klasörüne "denetlenen belge" belgesini yüklemiştir
    And "yazar" "denetlenen belge" belgesini checkout yapmıştır
    And "denetlenen belge" belgesinin denetim kaydı sayısı kaydedilmiştir
    When "okur" "denetlenen belge" belgesinde "yeniden adlandırma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    When "yönetici" "denetlenen belge" belgesinde "statü under_review" işlemini dener
    Then son API yanıtının durum kodu 409 olmalıdır
    When "yönetici" "denetlenen belge" belgesinde "statü bogus" işlemini dener
    Then son API yanıtı başarısız olmalıdır
    When "yazar" "denetlenen belge" belgesinde "yeniden adlandırma" işlemini dener
    Then son API yanıtı başarılı olmalıdır
    And "denetlenen belge" belgesinin denetim kayıtlarına yalnız son başarılı "document.rename" eklenmiş olmalıdır

  # ECM kaynağı: folder.acl_grant / folder.acl_revoke (hedef = ACL satırı), user.login_failed, user.deactivate
  @batch33 @AUD-03
  Scenario: AUD-03 İzin verme ve kaldırma, başarısız giriş ve kullanıcı pasifleştirme denetime yazılır
    Given yönetici API oturumu açmıştır
    And "denetlenen kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    When yönetici "denetim klasörü" klasöründe "denetlenen kullanıcı" kullanıcısına okuma izni verip kaldırır
    And "denetlenen kullanıcı" yanlış parolayla giriş yapmayı dener
    And yönetici "denetlenen kullanıcı" hesabını pasifleştirir
    Then izin kaydı için "folder.acl_grant" ve "folder.acl_revoke" kayıtları klasör kimliğiyle yazılmış olmalıdır
    And "denetlenen kullanıcı" kullanıcısı için "user.login_failed" kaydı "denetlenen kullanıcı" aktörüyle bulunmalıdır
    And "denetlenen kullanıcı" kullanıcısı için "user.deactivate" kaydı "yönetici" aktörüyle bulunmalıdır

  # ECM kaynağı: api/rest/audit.rs export_audit_logs_csv (aynı filtreler)
  @batch33 @AUD-04
  Scenario: AUD-04 Denetim CSV dışa aktarımı filtreye uyar ve sorguyla aynı kayıtları geçerli CSV olarak verir
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yönetici "denetim klasörü" klasörüne "denetlenen belge" belgesini yüklemiştir
    And "yönetici" "denetlenen belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
      | statü under_review |
    Then "denetlenen belge" belgesi için filtreli CSV dışa aktarımı sorguyla aynı kayıtları geçerli CSV olarak vermelidir

  # ECM kaynağı: components/HistoryTimeline.tsx (resource=documents)
  @batch33 @AUD-05
  Scenario: AUD-05 Belge geçmiş paneli işlemleri aktörüyle gösterir, geri yükleme sonrası eski girdiler korunur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yönetici "denetim klasörü" klasörüne "geçmişli belge" belgesini yüklemiştir
    And "yönetici" "geçmişli belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
      | soft delete        |
      | geri yükleme       |
    Then yönetici arayüzde "geçmişli belge" belgesinin geçmiş panelinde "created this document, renamed this document, deleted this document, restored this document" ifadelerini görmelidir

  # ECM kaynağı: document.read / document.download denetimi; atlama işareti RQ-12
  @batch33 @AUD-07
  Scenario: AUD-07 Yöneticinin izinsiz alandaki belgeyi okuması ve indirmesi denetime yazılır
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "izole klasör" oluşturulmuştur
    And "izole klasör" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "yazar" "izole klasör" klasörüne "izole belge" belgesini yüklemiştir
    When yönetici "izole belge" belgesini okuyup indirir
    Then "izole belge" belgesi için yönetici okuma ve indirme kayıtları bulunmalı, atlama bilgisi RQ-12 kanıtı olarak kaydedilmelidir

  # ECM kaynağı: engine/history.rs folder_history -> load_history(Folder, id); ACL olayları hedef türü 'acl'.
  # HistoryTimeline.tsx 'folder.acl_grant' için "granted access" cümlesine sahip (ön yüz bu kaydı bekliyor).
  @batch33 @AUD-08 @known-bug
  Scenario: AUD-08 Klasör geçmişi yeniden adlandırma, pasifleştirme, izin ve sahiplik devrini aktörüyle gösterir
    Given yönetici API oturumu açmıştır
    And "yeni sahip" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "geçmişli klasör" oluşturulmuştur
    When yönetici "geçmişli klasör" klasörünü yeniden adlandırır, pasifleştirip aktifleştirir, "yeni sahip" kullanıcısına izin verir ve sahipliğini devreder
    Then yönetici arayüzde "geçmişli klasör" klasörünün geçmiş panelinde "renamed this folder, deactivated this folder, reactivated this folder, transferred ownership" ifadelerini görmelidir
    And "geçmişli klasör" klasörünün geçmişi "folder.rename, folder.inactivate, folder.reactivate, folder.owner_transfer, folder.acl_grant" eylemlerini yönetici aktörüyle içermelidir

  # ECM kaynağı: api/rest/classes.rs class_history (var olmayan sınıf 404)
  @batch33 @AUD-09
  Scenario: AUD-09 Sınıf geçmişi oluşturma, özellik ekleme ve pasifleştirmeyi kaydeder
    Given yönetici API oturumu açmıştır
    And yönetici "ref:text:opsiyonel" özellikleriyle "geçmişli sınıf" sınıfını oluşturmuştur
    When yönetici "geçmişli sınıf" sınıfına "ek" özelliğini "text" türünde zorunlu olarak eklemeyi dener
    And yönetici "geçmişli sınıf" sınıfını pasifleştirir
    Then "geçmişli sınıf" sınıfının geçmişi "class.create, class.property_add, class.deactivate" eylemlerini yönetici aktörüyle içermeli ve var olmayan sınıf 404 dönmelidir

  # Batch 35 — Plan dışı. ECM kaynağı: api/rest/audit.rs get_audit_log (GET /admin/audit-logs/{id}, yönetici)
  @batch35 @AUD-10
  Scenario: AUD-10 Denetim kaydı ayrıntısı listedeki kayıtla aynıdır, bilinmeyen kimlik 404, yönetici olmayan 403 alır
    Given yönetici API oturumu açmıştır
    And "meraklı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "meraklı" API ile giriş yapmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yönetici "denetim klasörü" klasörüne "denetlenen belge" belgesini yüklemiştir
    And "yönetici" "denetlenen belge" belgesinde şu işlemleri başarıyla yapabilmelidir
      | yeniden adlandırma |
    Then "denetlenen belge" belgesinin denetim kaydı ayrıntısı listedeki kayıtla aynı olmalı, bilinmeyen kimlik 404 ve "meraklı" için 403 dönmelidir
