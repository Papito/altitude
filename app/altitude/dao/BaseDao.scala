package altitude.dao

import global.Altitude
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.Future

trait BaseDao {
  protected def app = Altitude.getInstance()

  def add(json: JsObject)(implicit txId: TransactionId): Future[JsObject]
  def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]]
  def getAll()(implicit txId: TransactionId): Future[List[JsObject]] = {
    throw new NotImplementedError
  }
}
