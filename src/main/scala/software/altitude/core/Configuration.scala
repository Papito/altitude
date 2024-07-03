package software.altitude.core

import software.altitude.core.{Const => C}

import scala.collection.immutable.HashMap

class Configuration(configOverride: Map[String, Any] = new HashMap()) {
  def getString(key: String): String = data.getOrElse(key, "").asInstanceOf[String]
  def getFlag(key: String): Boolean = data.getOrElse(key, false).asInstanceOf[Boolean]
  def getInt(key: String): Int = data.getOrElse(key, 0).asInstanceOf[Int]

  private val default = HashMap(
    "dataDir" -> "data",

    "dataSource" -> C.DatasourceType.POSTGRES,
    "fileStore" -> C.FileStoreType.FS,

    "previewBoxPixels" -> 200,

    "postgresUser" -> "altitude",
    "postgresPassword" -> "dba",
    "postgresUrl" -> "jdbc:postgresql://localhost/altitude",

    "sqliteUrl" -> "jdbc:sqlite:data/db/altitude.db"
  )

  private val test = default ++ HashMap(
    "testDir" -> "./test-data",
    "dataDir" -> "./test-data/data",
    "fileStore" -> C.FileStoreType.FS,

    "postgresUrl" -> "jdbc:postgresql://localhost:5433/altitude-test",
    "postgresUser" -> "altitude-test",
    "postgresPassword" -> "testdba",

    "sqliteUrl" -> "jdbc:sqlite:./test-data/test.sqlite.db"
  )

  private val prod = default ++ HashMap()

  val data: Map[String, Any] = Environment.ENV match {
    case Environment.TEST => test ++ configOverride
    case Environment.DEV => default ++ configOverride
    case Environment.PROD => prod ++ configOverride
  }
}
