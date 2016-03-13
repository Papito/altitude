package altitude.dao.jdbc

import altitude.Altitude
import altitude.models.BaseModel
import play.api.libs.json.{JsObject, Json}

class VoidJdbcDao (val app: Altitude) extends BaseJdbcDao("") {
  protected def CORE_SQL_VALS_FOR_INSERT = ""
  protected def DEFAULT_SQL_COLS_FOR_SELECT = ""
  protected def CURRENT_TIME_FUNC = ""
  protected def JSON_PLACEHOLDER = ""
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = Unit
  protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()
}
