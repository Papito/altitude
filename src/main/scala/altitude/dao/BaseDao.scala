package altitude.dao

import altitude.models.search.{Query, QueryResult}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import play.api.libs.json.JsObject

trait BaseDao {
  val app: Altitude
  protected val MAX_RECORDS = app.config.getInt("db.max_records")

  def add(json: JsObject)(implicit txId: TransactionId): JsObject
  def deleteByQuery(q: Query)(implicit txId: TransactionId): Int
  def getById(id: String)(implicit txId: TransactionId): Option[JsObject]
  def getByIds(id: Set[String])(implicit txId: TransactionId): List[JsObject]
  def getAll(implicit txId: TransactionId): List[JsObject] = query(Query()).records
  def query(q: Query)(implicit txId: TransactionId): QueryResult

  def deleteById(id: String)(implicit txId: TransactionId): Int = {
    val q: Query = Query(Map(C("Base.ID") -> id))
    deleteByQuery(q)
  }

  def updateById(id: String, data: JsObject, fields: List[String])(implicit txId: TransactionId): Int = {
    val q: Query = Query(Map(C("Base.ID") -> id))
    updateByQuery(q, data, fields)
  }

  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit txId: TransactionId): Int

  def increment(id: String, field: String, count: Int = 1)(implicit txId: TransactionId)
  def decrement(id: String, field: String, count: Int = 1)(implicit txId: TransactionId)
}
