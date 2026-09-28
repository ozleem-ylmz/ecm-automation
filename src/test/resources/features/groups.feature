@coverage-gap @groups
Feature: Grup yönetimi ve üyelik

  # ---------------------------------------------------------------------
  # Batch 22 — Groups (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: src/engine/admin.rs create_group / update_group; 0037 büyük/küçük harf duyarsız tekillik
  @batch22 @GRP-01
  Scenario: GRP-01 Grup adı boş olamaz, büyük küçük harf duyarsız tekildir ve yeniden adlandırılabilir
    Given yönetici API oturumu açmıştır
    And "qa grubu" isimli yeni bir grup oluşturulmuştur
    When yönetici boş adla grup oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Group name is required." içermelidir
    When yönetici "qa grubu" adının küçük harfli haliyle grup oluşturmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "A group with this name already exists" içermelidir
    And "qa grubu" adında yalnız bir grup bulunmalıdır
    When yönetici "qa grubu" grubunu yeniden adlandırır
    Then "qa grubu" grubunun adı yeni adı olmalıdır

  # ECM kaynağı: src/engine/admin.rs update_group / delete_group (ALL_USERS_GROUP_ID korumaları)
  @batch22 @GRP-03
  Scenario: GRP-03 All Users sistem grubu silinemez, yeniden adlandırılamaz ve pasifleştirilemez
    Given yönetici API oturumu açmıştır
    When yönetici All Users grubunu silmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The All Users system group cannot be deleted." içermelidir
    When yönetici All Users grubunu yeniden adlandırmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The All Users system group cannot be renamed." içermelidir
    When yönetici All Users grubunu pasifleştirmeyi dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "The All Users system group cannot be deactivated." içermelidir
    And All Users grubu "All Users" adıyla aktif kalmalıdır

  # ECM kaynağı: src/engine/admin.rs add_group_member; auth.rs require_auth (pasif hesap)
  @batch22 @GRP-05
  Scenario: GRP-05 Pasif kullanıcı gruba eklenince erişim kazanmaz ve aynı üye iki kez eklenince tek üyelik oluşur
    Given yönetici API oturumu açmıştır
    And "pasif kullanıcı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "pasif kullanıcı" API ile giriş yapmıştır
    And "okuma grubu" isimli yeni bir grup oluşturulmuştur
    And yetki devralması kapalı "grup klasörü" oluşturulmuştur
    And "grup klasörü" klasöründe "okuma grubu" grubuna "viewer" rolü verilmiştir
    And yönetici "grup klasörü" klasörüne "grup belgesi" belgesini yüklemiştir
    And yönetici "pasif kullanıcı" hesabını pasifleştirir
    When yönetici "pasif kullanıcı" kullanıcısını "okuma grubu" grubuna ekler
    And yönetici "pasif kullanıcı" kullanıcısını "okuma grubu" grubuna tekrar eklemeyi dener
    Then tekrar ekleme isteği 204 ya da 409 ile sonuçlanmalıdır
    And "okuma grubu" grubunun üyeleri arasında "pasif kullanıcı" tam bir kez listelenmelidir
    When "pasif kullanıcı" "grup belgesi" belgesinde "okuma" işlemini dener
    Then son API yanıtının durum kodu 403 olmalıdır
    And son API yanıtının hata mesajı "This account has been deactivated" içermelidir

  # ECM kaynağı: src/engine/admin.rs update_group; folder ACL satırları group_id ile bağlıdır
  @batch22 @GRP-06
  Scenario: GRP-06 Grup yeniden adlandırılınca klasör yetkisi grup kimliğine bağlı kalır ve üyelerin erişimi sürer
    Given yönetici API oturumu açmıştır
    And "grup üyesi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "grup üyesi" API ile giriş yapmıştır
    And "okuma grubu" isimli yeni bir grup oluşturulmuştur
    And yönetici "grup üyesi" kullanıcısını "okuma grubu" grubuna ekler
    And yetki devralması kapalı "grup klasörü" oluşturulmuştur
    And "grup klasörü" klasöründe "okuma grubu" grubuna "viewer" rolü verilmiştir
    And yönetici "grup klasörü" klasörüne "grup belgesi" belgesini yüklemiştir
    And "grup üyesi" "grup belgesi" belgesini okuyup içeriğini indirebilmelidir
    When yönetici "okuma grubu" grubunu yeniden adlandırır
    Then "okuma grubu" grubunun adı yeni adı olmalıdır
    And "grup üyesi" "grup belgesi" belgesini okuyup içeriğini indirebilmelidir
    And "grup klasörü" klasörünün yetki listesinde "okuma grubu" grubunun izni korunmuş olmalıdır

  # ECM kaynağı: src/api/rest/auth.rs me_groups (All Users hariç tutulur)
  @batch22 @ME-01
  Scenario: ME-01 Kullanıcının kendi grup listesi üyelik eklenip çıkarıldıkça güncellenir ve All Users listelenmez
    Given yönetici API oturumu açmıştır
    And "üye" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "üye" API ile giriş yapmıştır
    And "qa grubu" isimli yeni bir grup oluşturulmuştur
    Then "üye" kendi grup listesinde "qa grubu" grubunu görmemelidir
    When yönetici "üye" kullanıcısını "qa grubu" grubuna ekler
    Then "üye" kendi grup listesinde "qa grubu" grubunu görmelidir
    When yönetici "üye" kullanıcısını "qa grubu" grubundan çıkarır
    Then "üye" kendi grup listesinde "qa grubu" grubunu görmemelidir
