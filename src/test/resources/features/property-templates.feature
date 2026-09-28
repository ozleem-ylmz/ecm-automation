@coverage-gap @property-templates
Feature: Özellik şablonları

  # ---------------------------------------------------------------------
  # Batch 27 — Property templates (ECM_FULL_E2E_AUTOMATION_MASTER_PLAN.xlsx)
  # ---------------------------------------------------------------------

  # ECM kaynağı: engine/classes/templates.rs (ad zorunlu ve tekil; tür text|numeric|boolean|date|jsonb)
  @batch27 @PT-01
  Scenario: PT-01 Özellik şablonu oluşturulur ve güncellenir, ad tekildir, boş ad ve geçersiz tür reddedilir
    Given yönetici API oturumu açmıştır
    When yönetici "qa şablonu" adlı "text" türünde özellik şablonu oluşturur
    And yönetici "qa şablonu" şablonuyla aynı adda şablon oluşturmayı dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "A property template with this name already exists." içermelidir
    When yönetici "" adı ve "text" türüyle şablon oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "Template name is required." içermelidir
    When yönetici "<benzersiz>" adı ve "weird" türüyle şablon oluşturmayı dener
    Then son API yanıtının durum kodu 422 olmalıdır
    And son API yanıtının hata mesajı "expected one of text|numeric|boolean|date|jsonb" içermelidir
    When yönetici "qa şablonu" şablonunu yeniden adlandırır
    Then "qa şablonu" şablonu güncel adıyla listelenmelidir

  # ECM kaynağı: templates.rs (sistem şablonları değiştirilemez/silinemez). Özellikler şablona data_type adıyla bağlanır
  # (postgres_repository/classes.rs), bu yüzden "kullanımdaki" şablon yalnız sistem şablonlarıdır; özel şablonun
  # kullanımda olma dalı API ile kurulamaz (RQ-20).
  @batch27 @PT-02
  Scenario: PT-02 Sistem şablonları değiştirilemez ve silinemez
    Given yönetici API oturumu açmıştır
    When yönetici "text" sistem şablonunu "güncelleme" işlemiyle değiştirmeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "System templates cannot be modified." içermelidir
    When yönetici "text" sistem şablonunu "silme" işlemiyle değiştirmeyi dener
    Then son API yanıtının durum kodu 409 olmalıdır
    And son API yanıtının hata mesajı "System templates cannot be deleted." içermelidir
    And "text" sistem şablonu değişmeden kalmalıdır
