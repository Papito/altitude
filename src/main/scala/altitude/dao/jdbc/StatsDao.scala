package altitude.dao.jdbc

import altitude.{Const => C, Altitude}
import altitude.models.Stats
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class StatsDao (val app: Altitude) extends BaseJdbcDao("trash") with altitude.dao.StatsDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Stats(stats = Map(
    C("Stats.ALL_ASSETS") -> rec.get(C("Stats.ALL_ASSETS")).get.asInstanceOf[Int],
    C("Stats.ALL_ASSET_BYTES") -> rec.get(C("Stats.ALL_ASSET_BYTES")).get.asInstanceOf[Int],
    C("Stats.UNCATEGORIZED_ASSETS") -> rec.get(C("Stats.UNCATEGORIZED_ASSETS")).get.asInstanceOf[Int],
    C("Stats.RECYCLED_ASSETS") -> rec.get(C("Stats.RECYCLED_ASSETS")).get.asInstanceOf[Int],
    C("Stats.RECYCLED_BYTES") -> rec.get(C("Stats.RECYCLED_BYTES")).get.asInstanceOf[Int]
  ))
}
