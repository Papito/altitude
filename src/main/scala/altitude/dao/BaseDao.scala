package altitude.dao

import java.util.regex.Pattern

import altitude.models.{BaseModel, User}
import altitude.models.search.{Query, QueryResult}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
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

  def add(json: JsObject)(implicit user: User, txId: TransactionId): JsObject
  def deleteByQuery(q: Query)(implicit user: User, txId: TransactionId): Int
  def getById(id: String)(implicit user: User, txId: TransactionId): Option[JsObject]
  def getByIds(id: Set[String])(implicit user: User, txId: TransactionId): List[JsObject]
  def getAll(implicit user: User, txId: TransactionId): List[JsObject] = query(Query(user)).records
  def query(q: Query)(implicit user: User, txId: TransactionId): QueryResult

  def deleteById(id: String)(implicit user: User, txId: TransactionId): Int = {
    val q: Query = Query(user, Map(C.Base.ID -> id))
    deleteByQuery(q)
  }

  def updateById(id: String, data: JsObject, fields: List[String])(implicit user: User, txId: TransactionId): Int = {
    val q: Query = Query(user, Map(C.Base.ID -> id))
    updateByQuery(q, data, fields)
  }

  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit user: User, txId: TransactionId): Int

  def increment(id: String, field: String, count: Int = 1)(implicit user: User, txId: TransactionId)
  def decrement(id: String, field: String, count: Int = 1)(implicit user: User, txId: TransactionId)
}
