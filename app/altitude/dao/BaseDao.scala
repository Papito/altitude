package altitude.dao

import play.api.libs.json.JsValue

import scala.concurrent.Future

trait BaseDao {
  def add(json: JsValue)(implicit txId: Int): Future[JsValue]
  def getById(id: String)(implicit txId: Int): Future[JsValue]
}
