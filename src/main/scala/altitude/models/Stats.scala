package altitude.models

case class Stats(stats: List[Stat]) {

  private val lookup: Map[String, Stat] = stats.foldLeft(Map[String, Stat]()) {
    (res, stat) => res + (stat.dimension -> stat)
  }
  println(lookup)

  def getStat(key: String): Stat = {
    if (!lookup.contains(key)) {
      throw new RuntimeException(s"No stats for '$key'")
    }

    lookup.get(key).get
  }
}
