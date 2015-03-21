package altitude.dao

import play.api.libs.json.JsValue

import scala.concurrent.Future

trait BaseDao {
  def add(json: JsValue)(implicit tx: Option[Transaction]): Future[JsValue]
  def getById(id: String)(implicit tx: Option[Transaction]): Future[JsValue]
}
