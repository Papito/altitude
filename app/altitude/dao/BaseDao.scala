package altitude.dao

import global.Altitude
import play.api.libs.json.JsValue

import scala.concurrent.Future

trait BaseDao {
  protected def app = Altitude.getInstance()

  def add(json: JsValue)(implicit txId: TransactionId): Future[JsValue]
  def getById(id: String)(implicit txId: TransactionId): Future[JsValue]
  def getAll()(implicit txId: TransactionId): Future[List[JsValue]] = {
    throw new NotImplementedError
  }
}
