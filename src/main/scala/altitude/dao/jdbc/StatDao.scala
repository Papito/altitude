package altitude.dao.jdbc

import altitude.{Const => C, Altitude}
import altitude.models.{Stat, Stats}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class StatDao (val app: Altitude) extends BaseJdbcDao("stats") with altitude.dao.StatDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Stat(
    rec.get("dimension").get.asInstanceOf[String], rec.get("dim_val").get.asInstanceOf[Int])
}
