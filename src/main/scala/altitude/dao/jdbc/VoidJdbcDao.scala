package altitude.dao.jdbc

import altitude.Altitude
import altitude.models.BaseModel
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

class VoidJdbcDao (val app: Altitude) extends BaseJdbcDao("") {
  override protected def CORE_SQL_VALS_FOR_INSERT = ""
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = ""
  override protected def CURRENT_TIME_FUNC = ""
  override protected def JSON_PLACEHOLDER = ""
  override protected def DATETIME_TO_SQL(time: Option[DateTime]): String = ""
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = Unit
  protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()
}
