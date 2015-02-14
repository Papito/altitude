package altitude.dao

import play.api.libs.json.JsValue

import scala.concurrent.Future

trait BaseDao {
  def add(json: JsValue): Future[JsValue]
  def getById(id: String): Future[JsValue]
}
