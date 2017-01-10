package altitude.dao.jdbc

import altitude.Altitude
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

abstract class SearchDao(val app: Altitude) extends BaseJdbcDao("asset") with altitude.dao.SearchDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()
}

