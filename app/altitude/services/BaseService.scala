package altitude.services

import altitude.dao.{Transaction, BaseDao}
import altitude.models.BaseModel
import altitude.util.log
import global.Altitude
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import net.codingwell.scalaguice.InjectorExtensions._


abstract class BaseService[Model <: BaseModel] {
  protected val DAO: BaseDao
  protected def app = Altitude.getInstance()
  protected val txManager = app.injector.instance[AbstractTransactionManager]
  def add(obj: Model)(implicit txArg: Option[Transaction]): Future[JsValue] = {
    log.info("Transaction defined?: " + tx.isDefined)
    txManager.withTransaction[Future[JsValue]] {
      val f: Future[JsValue] = DAO.add(obj.toJson)
      f map {res => res}
    }
  }

  def getById(id: String)(implicit tx: Option[Transaction]): Future[JsValue] = {
    txManager.asReadOnly[Future[JsValue]] {
      val f: Future[JsValue] = DAO.getById(id)
      f map {res => res}
    }
  }
}
