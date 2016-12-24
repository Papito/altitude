package altitude.dao.jdbc

import altitude.Altitude
import altitude.models.BaseModel
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

class VoidJdbcDao (val app: Altitude) extends BaseJdbcDao("") {
  override protected def CORE_SQL_VALS_FOR_INSERT = ""
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = ""
  override protected def CURRENT_TIME_FUNC = ""
  override protected def JSON_FUNC = ""
  protected def GET_DATETIME_FROM_REC(field: String, rec: Map[String, AnyRef]): Option[DateTime] =
    throw new NotImplementedError()
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type = model
  protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()
  protected def DATETIME_TO_DB_FUNC(datetime: Option[DateTime]): String =
    throw new NotImplementedError()
}
