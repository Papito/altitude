package altitude.dao

import altitude.models.search.Query
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait BaseDao {
  def add(json: JsObject)(implicit txId: TransactionId): JsObject
  def getById(id: String)(implicit txId: TransactionId): Option[JsObject]
  def getAll()(implicit txId: TransactionId): List[JsObject] = throw new NotImplementedError
  def query(q: Query)(implicit txId: TransactionId): List[JsObject]
}
