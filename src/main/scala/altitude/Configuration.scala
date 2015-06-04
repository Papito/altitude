package altitude
import collection.immutable.HashMap

class Configuration(additionalConfiguration: Map[String, String] = new HashMap()) {
  def get(key: String) = config.getOrElse(key, "")

  val default = HashMap(
    "datasource" -> "mongo",

    "db.postgres.user" -> "altitude",
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude",

    "db.mongo.host" -> "localhost",
    "db.mongo.db" -> "altitude",
    "db.mongo.port" -> "27017"
  )

  val test = default ++ HashMap(
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.postgres.user" -> "altitude-test",

    "db.mongo.db" -> "altitude-test"
  )

  val prod = default ++ HashMap()

  private val config: Map[String, String] =  Environment.ENV match {
    case Environment.TEST => test ++ additionalConfiguration
    case Environment.DEV => default ++ additionalConfiguration
    case Environment.PROD => prod ++ additionalConfiguration
  }
}