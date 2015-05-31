package altitude
import collection.immutable.HashMap

class Configuration(additionalConfiguration: Map[String, String] = new HashMap()) {
  def get(key: String) = config.getOrElse(key, "")

  val default = HashMap(
    "datasource" -> "postgres", // mongo
    "db.postgres.user" -> "altitude",
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude"
  )

  val test = default ++ HashMap(
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.postgres.user" -> "altitude-test"
  ) ++ additionalConfiguration

  val prod = default ++ HashMap() ++ additionalConfiguration

  private val config: Map[String, String] =  Environment.ENV match {
    case Environment.TEST => test
    case Environment.DEV => default
    case Environment.PROD => prod
  }
}