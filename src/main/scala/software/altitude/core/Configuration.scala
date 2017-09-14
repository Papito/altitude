package software.altitude.core

import software.altitude.core.{Const => C}

import scala.collection.immutable.HashMap

class Configuration(configOverride: Map[String, Any] = new HashMap()) {
  def getString(key: String) = data.getOrElse(key, "").asInstanceOf[String]
  def getFlag(key: String) = data.getOrElse(key, false).asInstanceOf[Boolean]
  def getInt(key: String) = data.getOrElse(key, 0).asInstanceOf[Int]
  def datasourceType = data.get("datasource").get.asInstanceOf[C.DatasourceType.Value]
  def fileStoreType = data.get("filestore").get.asInstanceOf[C.FileStoreType.Value]

  private val default = HashMap(
    "app.name" -> "Altitude",
    "dataDir" -> "data",
    "previewDir" -> "p",

    "datasource" -> C.DatasourceType.SQLITE,
    "filestore" -> C.FileStoreType.FS,
    "importMode" -> C.ImportMode.COPY,

    "preview.box.pixels" -> 200,

    // safeguard for maximum records allowed at once without pagination
    "db.max_records" -> 1000,
    "db.postgres.user" -> "altitude",
    "db.postgres.password" -> "dba",
    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude",

    "db.sqlite.url" -> s"jdbc:sqlite:${Environment.root}data/db"
  )

  private val test = default ++ HashMap(
    "testDir" -> "./test-data",
    "fileStoreDir" -> "./test-data/file-store",
    "dataDir" -> "./test-data/file-store/1",
    "importMode" -> C.ImportMode.COPY,
    "filestore" -> C.FileStoreType.FS,

    "db.postgres.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.postgres.user" -> "altitude-test",

    "db.sqlite.url" -> "jdbc:sqlite:./test-data/test.sqlite.db"
  )

  private val prod = default ++ HashMap()

  val data: Map[String, Any] = Environment.ENV match {
    case Environment.TEST => test ++ configOverride
    case Environment.DEV => default ++ configOverride
    case Environment.PROD => prod ++ configOverride
  }
}