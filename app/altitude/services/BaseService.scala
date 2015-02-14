package altitude.services

import altitude.dao.BaseDao
import altitude.models.BaseModel
import global.Altitude
import play.api.libs.json.JsValue

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao
  protected val app = Altitude.getInstance()

  def add(model: Model): Future[Model] = {
    val f: Future[JsValue] = DAO.add(model.toJson)
    f map {res => model}
  }
}
