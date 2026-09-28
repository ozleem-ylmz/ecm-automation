package runners;

/*
 * Paralel koşu shard'ı 5 (bkz. ShardFilter). Glue, plugin ve feature seçimi
 * TestRunner'dan miras alınır (@Suite/@ConfigurationParameter @Inherited).
 * Varsayılan "mvn test" bu sınıfı çalıştırmaz (Surefire include deseni
 * Test*.java); yalnızca tools/parallel/run-parallel.ps1 -Dtest ile seçer.
 */
public class ShardRunner5 extends TestRunner {
}
