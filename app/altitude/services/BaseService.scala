package altitude.services

import altitude.dao.BaseDao
import altitude.models.{Asset, BaseModel}
import global.Altitude
import play.api.libs.json.JsValue
import scala.language.implicitConversions


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
