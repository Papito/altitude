package altitude.dao

import altitude.Altitude
import altitude.models.search.Query
import altitude.transactions.TransactionId
import play.api.libs.json.JsObject

trait BaseDao {
  val app: Altitude
  def add(json: JsObject)(implicit txId: TransactionId): JsObject
  def getById(id: String)(implicit txId: TransactionId): JsObject
  def getAll()(implicit txId: TransactionId): List[JsObject] = throw new NotImplementedError
  def query(q: Query)(implicit txId: TransactionId): List[JsObject]
}
