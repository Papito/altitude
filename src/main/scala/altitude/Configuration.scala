package altitude

class Configuration(additionalConfiguration: Map[String, String] = new scala.collection.immutable.HashMap()) {
  private val config = new scala.collection.mutable.HashMap[String, String]()
  def get(key: String) = config.getOrElse(key, "")

  // FIXME: must come from files

  val default = collection.immutable.HashMap(
    "datasource" -> "postgres", // mongo
    "db.user" -> "altitude",
    "db.password" -> "dba",
    "db.url" -> "jdbc:postgresql://localhost/altitude"
  )

  val test = default ++ collection.immutable.HashMap(
    "db.url" -> "jdbc:postgresql://localhost/altitude-test",
    "db.user" -> "altitude-test"
  ) ++ additionalConfiguration

  val dev = default ++ collection.immutable.HashMap() ++ additionalConfiguration

  val live = default ++ collection.immutable.HashMap() ++ additionalConfiguration
}