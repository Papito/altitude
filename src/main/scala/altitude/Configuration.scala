package altitude
import collection.immutable.HashMap

class Configuration(additionalConfiguration: Map[String, String] = new HashMap(),
                    val isTest: Boolean,
                    val isProd: Boolean) {
  // at least one ENV should be chosen
  require(isTest || isProd)
  // but not two or more at the same time
  require(List(isTest, isProd).count(_ == true) == 1)

  def get(key: String) = config.getOrElse(key, "")

  // FIXME: must come from files

  val default = HashMap(
    "datasource" -> "postgres", // mongo
    "db.user" -> "altitude",
    "db.password" -> "dba",
    "db.url" -> "jdbc:postgresql://localhost/altitude"
  )

  val test = default ++ HashMap(
    "db.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.user" -> "altitude-test"
  ) ++ additionalConfiguration

  val prod = default ++ HashMap() ++ additionalConfiguration

  private val config: Map[String, String] = if (isTest) test else prod
}