package runners;

/*
 * Paralel koşu shard'ı 12 (bkz. ShardFilter). Glue, plugin ve feature seçimi
 * TestRunner'dan miras alınır (@Suite/@ConfigurationParameter @Inherited).
 * Varsayılan "mvn test" bu sınıfı çalıştırmaz (Surefire include deseni
 * Test*.java); yalnızca tools/parallel/run-parallel.ps1 -Dtest ile seçer.
 */
public class ShardRunner12 extends TestRunner {
}
