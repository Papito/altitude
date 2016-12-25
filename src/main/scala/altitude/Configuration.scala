package altitude

import scala.collection.immutable.HashMap

class Configuration(additionalConfiguration: Map[String, Any] = new HashMap()) {
  def getString(key: String) = data.getOrElse(key, "").asInstanceOf[String]
  def getFlag(key: String) = data.getOrElse(key, false).asInstanceOf[Boolean]
  def getInt(key: String) = data.getOrElse(key, 0).asInstanceOf[Int]

  private val default = HashMap(
    "app.name" -> "Altitude",
    "dataDir" -> "data",
    "previewDir" -> "p",

    "datasource" -> "sqlite", // mongo, postgres, sqlite

    "preview.box.pixels" -> 200,

    // safeguard for maximum records allowed at once without pagination
    "db.max_records" -> 1000,
    "db.postgres.user" -> "altitude",
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude",

    "db.sqlite.url" -> s"jdbc:sqlite:${Environment.root}data/db",

    "db.mongo.host" -> "localhost",
    "db.mongo.db" -> "altitude",
    "db.mongo.port" -> "27017"
  )

  private val test = default ++ HashMap(
    "dataDir" -> "tmp/test/data",

    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.postgres.user" -> "altitude-test",

    "db.sqlite.url" -> "jdbc:sqlite:tmp/test/test.sqlite.db",

    "db.mongo.db" -> s"altitude-test",
    "db.mongo.port" -> "27018"
  )

  private val prod = default ++ HashMap()

  val data: Map[String, Any] = Environment.ENV match {
    case Environment.TEST => test ++ additionalConfiguration
    case Environment.DEV => default ++ additionalConfiguration
    case Environment.PROD => prod ++ additionalConfiguration
  }
}