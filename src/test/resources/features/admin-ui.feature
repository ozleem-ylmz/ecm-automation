@coverage-gap @admin-ui
Feature: Yönetim arayüzleri ve ön yüz / arka uç tutarlılığı

  # ---------------------------------------------------------------------
  # Batch 31 — Admin UI & FE↔BE consistency (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: React varsayılan metin kaçışı; DocumentDetail/Documents/FolderDetail/SavedSearches
  @batch31 @DATA-01 @critical
  Scenario: DATA-01 Başlık, klasör adı, sürüm açıklaması ve kayıtlı arama adına girilen HTML/betik arayüzde çalışmaz
    Given yönetici API oturumu açmıştır
    And yönetici betik içeren başlık, klasör adı, sürüm açıklaması ve kayıtlı arama adıyla kayıtlar oluşturmuştur
    Then betik içeren kayıtlar liste, detay, geçmiş, klasör ve kayıtlı arama görünümlerinde çalışmadan düz metin gösterilmelidir

  # ECM kaynağı: DocumentDetail.tsx canEdit = canEditDocument(can_write, ...) — is_deleted kontrol edilmiyor (ECM tc148)
  @batch31 @DOC-18 @known-bug
  Scenario: DOC-18 Soft delete edilmiş sınıfsız ve sınıflı belgede New version kontrolü sunulmamalıdır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "silinmiş klasör" oluşturulmuştur
    And yönetici "silinmiş klasör" klasörüne "silinmiş sınıfsız belge" belgesini yüklemiştir
    And yönetici "" özellikleriyle "silinmiş sınıf" sınıfını oluşturmuştur
    And "yönetici" "silinmiş sınıf" sınıfıyla "silinmiş klasör" klasörüne "{}" metadatasıyla "silinmiş sınıflı belge" belgesini yüklemiştir
    And "silinmiş sınıfsız belge" belgesi API ile soft delete edilmiştir
    And "silinmiş sınıflı belge" belgesi API ile soft delete edilmiştir
    Then yönetici arayüzde silinmiş "silinmiş sınıfsız belge" belgesinde Deleted rozetini görmeli ve New version kontrolü sunulmamalıdır
    And yönetici arayüzde silinmiş "silinmiş sınıflı belge" belgesinde Deleted rozetini görmeli ve New version kontrolü sunulmamalıdır

  # ECM kaynağı: README storage-gc/versions/delete ("current ... highest remaining version is promoted")
  @batch31 @SYS-02
  Scenario: SYS-02 Depolama temizliği yalnız seçilen sürümü siler, güncel sürüm silinirse kalan en yüksek sürüm güncel olur
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "gc klasörü" oluşturulmuştur
    And yönetici "gc klasörü" klasörüne "gc belgesi" belgesini yüklemiştir
    And "yönetici" "gc belgesi" belgesine "QA gc v2" içerikli yeni bir yama sürümü eklemiştir
    And "yönetici" "gc belgesi" belgesine "QA gc v3" içerikli yeni bir yama sürümü eklemiştir
    And "gc belgesi" belgesinin sürümleri "v1", "v2" ve "v3" olarak adlandırılmıştır
    When yönetici depolama temizliğinde "gc belgesi" belgesinin "v1" sürümünü siler
    Then "v1" sürümünün içeriği artık 404 dönmelidir
    And "gc belgesi" belgesinde 2 sürüm kalmalı ve güncel sürüm "v3" olmalıdır
    And "gc belgesi" belgesinin güncel içeriği "QA gc v3" olmalıdır
    When yönetici depolama temizliğinde "gc belgesi" belgesinin "v3" sürümünü siler
    Then "gc belgesi" belgesinde 1 sürüm kalmalı ve güncel sürüm "v2" olmalıdır
    And "gc belgesi" belgesinin güncel içeriği "QA gc v2" olmalıdır

  # ECM kaynağı: pages/AuditLogs.tsx (eylem filtresi, satır ayrıntısı Before/After state)
  @batch31 @AUD-06
  Scenario: AUD-06 Admin denetim kayıtları sayfasında eylem filtresi ve kayıt ayrıntısı çalışır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "denetim klasörü" oluşturulmuştur
    And yönetici "denetim klasörü" klasörünü benzersiz bir adla yeniden adlandırmıştır
    Then yönetici denetim kayıtlarında "folder.rename" eylemini filtreleyip "denetim klasörü" klasörünün kaydını ayrıntısıyla görmelidir

  # ECM kaynağı: pages/Classes.tsx + ClassDetail.tsx + AddPropertyModal.tsx + ValidationRulesModal.tsx
  @batch31 @CLS-14
  Scenario: CLS-14 Admin arayüzde sınıf oluşturur, özellik ve kural ekler, pasifleştirir; değişiklikler kalıcıdır
    Given yönetici API oturumu açmıştır
    When yönetici arayüzde "kod" anahtarlı zorunlu metin özelliğiyle "arayüz sınıfı" sınıfını oluşturur
    And yönetici arayüzde "arayüz sınıfı" sınıfına "adet" anahtarlı sayısal özellik ekler
    And yönetici arayüzde "arayüz sınıfı" sınıfının "kod" özelliğine en fazla 5 karakter kuralı ekler
    And yönetici arayüzde "arayüz sınıfı" sınıfını pasifleştirir
    Then yenilemeden sonra "arayüz sınıfı" sınıfı pasif görünmeli, API'de 2 özellik ve "kod" üzerinde kural bulunmalı, yükleme sayfasında devre dışı olmalıdır

  # ECM kaynağı: DocumentDetail.tsx STATUS_OPTIONS sabit (is_legal_transition: published -> draft yasak) — BUG-01 kök nedeni
  @batch31 @FE-01 @known-bug
  Scenario: FE-01 Yayımlanmış belgede durum seçimi Draft sunmamalıdır
    Given yönetici API oturumu açmıştır
    And yetki devralması kapalı "durum klasörü" oluşturulmuştur
    And yönetici "durum klasörü" klasörüne "yayımlanmış belge" belgesini yüklemiştir
    And "yayımlanmış belge" belgesi API ile "published" durumuna getirilmiştir
    Then yönetici arayüzde "yayımlanmış belge" belgesinin durum seçeneklerinde "Draft" sunulmamalıdır

  # ECM kaynağı: DocumentDetail.tsx can_write / can_rename / can_manage bayrakları
  @batch31 @FE-02
  Scenario: FE-02 Viewer için belge sayfasında yazma kontrolleri sunulmaz, indirme ve önizleme sunulur
    Given yönetici API oturumu açmıştır
    And "yazar" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "okur" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "yazar" API ile giriş yapmıştır
    And yetki devralması kapalı "okuma klasörü" oluşturulmuştur
    And "okuma klasörü" klasöründe "yazar" kullanıcısına "manager" rolü verilmiştir
    And "okuma klasörü" klasöründe "okur" kullanıcısına "viewer" rolü verilmiştir
    And "yazar" "okuma klasörü" klasörüne "okunan belge" belgesini yüklemiştir
    Then "okur" arayüzde "okunan belge" belgesinde yazma kontrollerini görmemeli ama indirme ve önizlemeyi görmelidir

  # ECM kaynağı: FolderDetail.tsx canRename / canManageFolder (sahip evet, düz yazma yetkilisi hayır)
  @batch31 @FE-03
  Scenario: FE-03 Klasör sahibi kendi klasöründe yönetim kontrollerini görür, sahip olmayan düzenleyici görmez
    Given yönetici API oturumu açmıştır
    And "klasör sahibi" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "düzenleyici" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And yetki devralması kapalı "sahip klasörü" oluşturulmuştur
    And yönetici "sahip klasörü" klasörünün sahipliğini "klasör sahibi" kullanıcısına devretmiştir
    And "sahip klasörü" klasöründe "düzenleyici" kullanıcısına "manager" rolü verilmiştir
    Then "klasör sahibi" arayüzde "sahip klasörü" klasöründe Rename, Inactivate ve Add grant kontrollerini "görmelidir"
    And "düzenleyici" arayüzde "sahip klasörü" klasöründe Rename, Inactivate ve Add grant kontrollerini "görmemelidir"

  # ECM kaynağı: pages/Groups.tsx + GroupDetail.tsx (UserPicker, Add, Remove)
  @batch31 @GRP-04
  Scenario: GRP-04 Admin arayüzde grup oluşturur, üye ekler ve çıkarır; değişiklik yenilemeden sonra korunur
    Given yönetici API oturumu açmıştır
    And "üye adayı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    When yönetici arayüzde "arayüz grubu" grubunu oluşturur
    And yönetici arayüzde "arayüz grubu" grubuna "üye adayı" kullanıcısını ekler
    Then yönetici grup sayfasını yenilediğinde üyeler arasında "üye adayı" görmelidir
    And "arayüz grubu" grubunun üyeleri arasında "üye adayı" tam bir kez listelenmelidir
    When yönetici arayüzde "arayüz grubu" grubundan "üye adayı" kullanıcısını çıkarır
    Then yönetici grup sayfasını yenilediğinde üyeler arasında "üye adayı" görmemelidir
    And "arayüz grubu" grubunun üyeleri arasında "üye adayı" bulunmamalıdır

  # ECM kaynağı: pages/PropertyTemplates.tsx (oluşturma; düzenleme açıklamayı günceller)
  @batch31 @PT-03
  Scenario: PT-03 Admin arayüzde özellik şablonu oluşturur ve açıklamasını düzenler, değişiklik kalıcıdır
    Given yönetici API oturumu açmıştır
    When yönetici arayüzde "numeric" türünde "arayüz şablonu" şablonunu oluşturur
    And yönetici arayüzde "arayüz şablonu" şablonunun açıklamasını "QA güncel açıklama" yapar
    Then yenilemeden sonra "arayüz şablonu" şablonu "numeric" türü ve "QA güncel açıklama" açıklamasıyla görünmeli ve API aynı değerleri döndürmelidir

  # ECM kaynağı: pages/Roles.tsx (özel rol izinleri; sistem rolü read-only)
  @batch31 @RBAC-08
  Scenario: RBAC-08 Admin arayüzde özel rol oluşturup izinlerini kaydeder, sistem rolü düzenlenemez görünür
    Given yönetici API oturumu açmıştır
    When yönetici arayüzde "document:read,folder:read" izinleriyle "arayüz rolü" özel rolünü oluşturur
    Then yönetici roller sayfasını yenilediğinde "arayüz rolü" rolünde yalnız "document:read,folder:read" izinleri işaretli olmalıdır
    And yönetici arayüzde viewer sistem rolünü açtığında izin kutuları devre dışı ve kaydetme kontrolü olmamalıdır

  # ECM kaynağı: components/AdminGuard.tsx (admin değilse /classes'a yönlendirir) + API admin korumaları
  @batch31 @SYS-01
  Scenario: SYS-01 Sistem durumu, denetim kayıtları ve depolama temizliği sayfaları yalnız admin'e açıktır
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" tarayıcıda oturum açmıştır
    Then "test kullanıcısı" arayüzde "/admin/system" adresine gittiğinde yönetim verisi görmeden "/classes" adresine yönlendirilmelidir
    And "test kullanıcısı" arayüzde "/admin/audit-logs" adresine gittiğinde yönetim verisi görmeden "/classes" adresine yönlendirilmelidir
    And "test kullanıcısı" arayüzde "/admin/storage-gc" adresine gittiğinde yönetim verisi görmeden "/classes" adresine yönlendirilmelidir
    And "test kullanıcısı" arayüzde "/admin/users" adresine gittiğinde yönetim verisi görmeden "/classes" adresine yönlendirilmelidir

  # ECM kaynağı: pages/Users.tsx (New user, Edit, Deactivate) + API doğrulaması
  @batch31 @USER-06
  Scenario: USER-06 Admin arayüzde kullanıcı oluşturur, rolünü değiştirir ve pasifleştirir; pasif kullanıcı giriş yapamaz
    Given yönetici API oturumu açmıştır
    When yönetici arayüzde "arayüz kullanıcısı" adında yeni bir standart kullanıcı oluşturur
    And yönetici arayüzde "arayüz kullanıcısı" kullanıcısının rolünü "manager" yapar
    And yönetici arayüzde "arayüz kullanıcısı" kullanıcısını pasifleştirir
    Then yönetici kullanıcılar sayfasını yenilediğinde "arayüz kullanıcısı" satırında "Account manager" ve "Inactive" görmelidir
    And yönetici "arayüz kullanıcısı" hesabını "manager" rolünde ve pasif görmelidir
    And "arayüz kullanıcısı" yeniden giriş yapmaya çalıştığında pasif hesap hatası almalıdır

  # ECM kaynağı: api/rest/health.rs get_info (admin), config_endpoint.rs (herkes, yalnız All Users id)
  @batch31 @SYS-03
  Scenario: SYS-03 Sistem bilgisi yalnız admin'e açıktır, yapılandırma ucu gizli değer içermez
    Given yönetici API oturumu açmıştır
    And "test kullanıcısı" isimli "user" hesap rolünde yeni bir test kullanıcısı oluşturulmuştur
    And "test kullanıcısı" API ile giriş yapmıştır
    Then "yönetici" için sistem bilgisi ucu 200 dönmeli ve gizli yapılandırma içermemelidir
    And "test kullanıcısı" için sistem bilgisi ucu 403 dönmeli ve gizli yapılandırma içermemelidir
    And "test kullanıcısı" için sistem yapılandırma ucu yalnız All Users grup kimliğini döndürmelidir
