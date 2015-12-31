package altitude.dao

import altitude.Altitude
import altitude.exceptions.NotFoundException
import altitude.models.search.Query
import altitude.transactions.TransactionId
import play.api.libs.json.JsObject
import altitude.{Const => C}

trait BaseDao {
  val app: Altitude
  protected val MAX_RECORDS = app.config.getInt("db.max_records")

  def add(json: JsObject)(implicit txId: TransactionId): JsObject
  def deleteByQuery(q: Query)(implicit txId: TransactionId): Int
  def getById(id: String)(implicit txId: TransactionId): Option[JsObject]
  def getAll()(implicit txId: TransactionId): List[JsObject] = query(Query())
  def query(q: Query)(implicit txId: TransactionId): List[JsObject]

  def deleteById(id: String)(implicit txId: TransactionId): Int = {
    val q: Query = Query(Map(C.Base.ID -> id))
    deleteByQuery(q)
  }

  def update(id: String, data: JsObject)(implicit txId: TransactionId): JsObject = {
    val obj: Option[JsObject] = getById(id)

    obj.isDefined match {
      case false => throw new NotFoundException(s"Cannot update object. ID $id does not exist")
      case true => update(obj.get, data)
    }
  }

  def update(jsonObj: JsObject, data: JsObject)(implicit txId: TransactionId): JsObject
}
