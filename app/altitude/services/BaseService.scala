package altitude.services

import altitude.dao.BaseDao
import altitude.models.BaseModel
import global.Altitude
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import net.codingwell.scalaguice.InjectorExtensions._


abstract class BaseService[Model <: BaseModel] {
  protected val DAO: BaseDao
  protected def app = Altitude.getInstance()
  protected val tx = app.injector.instance[AbstractTransactionManager]

  def add(obj: Model): Future[JsValue] = {
    tx.transaction[Future[JsValue]] {
      val f: Future[JsValue] = DAO.add(obj.toJson)
      f map {res => res}
    }
  }

  def getById(id: String): Future[JsValue] = {
    tx.readOnly[Future[JsValue]] {
      val f: Future[JsValue] = DAO.getById(id)
      f map {res => res}
    }
  }
}
