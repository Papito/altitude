package altitude.dao

import altitude.Altitude
import altitude.models.User
import altitude.models.search.{QueryResult, Query}
import altitude.transactions.TransactionId
import play.api.libs.json.JsObject

class NotImplementedDao(val app: Altitude) extends BaseDao {
  def add(json: JsObject)(implicit user: User, txId: TransactionId): JsObject =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def deleteByQuery(q: Query)(implicit user: User, txId: TransactionId): Int =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def getById(id: String)(implicit user: User, txId: TransactionId): Option[JsObject] =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING BASE SERVICE CLASS METHOD!")
  def getByIds(id: Set[String])(implicit user: User, txId: TransactionId): List[JsObject] =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def query(q: Query)(implicit user: User, txId: TransactionId): QueryResult =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit user: User, txId: TransactionId): Int =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def increment(id: String, field: String, count: Int = 1)(implicit user: User, txId: TransactionId) =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def decrement(id: String, field: String, count: Int = 1)(implicit user: User, txId: TransactionId) =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
}
