package altitude
import scala.collection.immutable.HashMap

class Configuration(additionalConfiguration: Map[String, Any] = new HashMap()) {
  def getString(key: String) = config.getOrElse(key, "").asInstanceOf[String]
  def getFlag(key: String) = config.getOrElse(key, false).asInstanceOf[Boolean]
  def getInt(key: String) = config.getOrElse(key, 0).asInstanceOf[Int]

  val default = HashMap(
    "app.name" -> "Altitude",
    "datasource" -> "sqlite", // mongo, postgres, sqlite

    "preview.box.pixels" -> 200,

    // safeguard for maximum records allowed at once without pagination
    "db.max_records" -> 1000,
    "db.postgres.user" -> "altitude",
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude",

    "db.sqlite.url" -> "jdbc:sqlite:db.sqlite",

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

  private val config: Map[String, Any] =  Environment.ENV match {
    case Environment.TEST => test ++ additionalConfiguration
    case Environment.DEV => default ++ additionalConfiguration
    case Environment.PROD => prod ++ additionalConfiguration
  }
}