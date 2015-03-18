package altitude.services

import altitude.dao.BaseDao
import altitude.models.BaseModel
import global.Altitude
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel] {
  protected val DAO: BaseDao
  protected val app = Altitude.getInstance()

  def add(obj: Model): Future[JsValue] = {
    val f: Future[JsValue] = DAO.add(obj.toJson)
    f map {res => res}
  }

  def getById(id: String): Future[JsValue] = {
    val f: Future[JsValue] = DAO.getById(id)
    f map {res => res}
  }
}
