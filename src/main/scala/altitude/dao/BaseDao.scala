package altitude.dao

import java.util.regex.Pattern

import altitude.models.BaseModel
import altitude.models.search.{Query, QueryResult}
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject

trait BaseDao {
  val app: Altitude
  protected val MAX_RECORDS = app.config.getInt("db.max_records")

  private val VALID_ID_PATTERN = Pattern.compile("[a-z0-9]+")
  protected def verifyId(id: String) = {
    if (id == null) {
      throw new IllegalArgumentException("ID is not defined")
    }

    if (id.length != BaseModel.ID_LEN) {
      throw new IllegalArgumentException(s"ID length should be ${BaseModel.ID_LEN}. Was: [${id.length}]")
    }

    if (!VALID_ID_PATTERN.matcher(id).find()) {
      throw new IllegalArgumentException(s"ID [$id] is not alphanumeric")
    }
  }

  def add(json: JsObject)(implicit ctx: Context): JsObject
  def deleteByQuery(q: Query)(implicit ctx: Context): Int
  def getById(id: String)(implicit ctx: Context): Option[JsObject]
  def getByIds(id: Set[String])(implicit ctx: Context): List[JsObject]
  def getAll(implicit ctx: Context): List[JsObject] = query(Query()).records
  def query(q: Query)(implicit ctx: Context): QueryResult

  def deleteById(id: String)(implicit ctx: Context): Int = {
    val q: Query = Query(Map(C.Base.ID -> id))
    deleteByQuery(q)
  }

  def updateById(id: String, data: JsObject, fields: List[String])(implicit ctx: Context): Int = {
    val q: Query = Query(Map(C.Base.ID -> id))
    updateByQuery(q, data, fields)
  }

  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit ctx: Context): Int

  def increment(id: String, field: String, count: Int = 1)(implicit ctx: Context)
  def decrement(id: String, field: String, count: Int = 1)(implicit ctx: Context)
}
