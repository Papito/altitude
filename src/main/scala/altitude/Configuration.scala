package altitude

class Configuration {
  private val config = new scala.collection.mutable.HashMap[String, String]()
  def get(key: String) = config.getOrElse(key, "")
}
