package altitude.models


object Stats {
  final val TOTAL_ASSETS = "total_assets"
  final val TOTAL_BYTES = "total_bytes"
  final val UNCATEGORIZED_ASSETS = "uncategorized_assets"
  final val RECYCLED_ASSETS = "recycled_assets"
  final val RECYCLED_BYTES = "recycles_bytes"
}

case class Stats(stats: List[Stat]) {

  private val lookup: Map[String, Stat] = stats.foldLeft(Map[String, Stat]()) {
    (res, stat) => res + (stat.dimension -> stat)
  }

  def getStatValue(key: String): Int = {
    if (!lookup.contains(key)) {
      throw new RuntimeException(s"No stats for '$key'")
    }

    lookup.get(key).get.dimVal
  }
}
