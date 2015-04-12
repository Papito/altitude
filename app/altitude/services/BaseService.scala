package altitude.services

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import global.Altitude
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions


abstract class BaseService[Model <: BaseModel] {
  protected val DAO: BaseDao
  protected def app = Altitude.getInstance()
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def add(obj: Model)(implicit txId: TransactionId = new TransactionId): Future[JsValue] = {
    txManager.withTransaction[Future[JsValue]] {
      val f = DAO.add(obj.toJson)
      f map {res => res}
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): Future[JsValue] = {
    txManager.asReadOnly[Future[JsValue]] {
      val f = DAO.getById(id)
      f map {res => res}
    }
  }

  def getAll()(implicit txId: TransactionId = new TransactionId): Future[List[JsValue]] =
    txManager.asReadOnly[Future[List[JsValue]]] {
      val f = DAO.getAll
      f map {res => res}
    }
}
