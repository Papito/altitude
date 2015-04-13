package altitude.services

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import global.Altitude
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions


abstract class BaseService[Model <: BaseModel] {
  protected val DAO: BaseDao
  protected def app = Altitude.getInstance()
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def add(obj: Model)(implicit txId: TransactionId = new TransactionId): Future[JsObject] = {
    txManager.withTransaction[Future[JsObject]] {
      val f = DAO.add(obj.toJson)
      f map {res => res}
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): Future[JsObject] = {
    txManager.asReadOnly[Future[JsObject]] {
      val f = DAO.getById(id)
      f map {res => res}
    }
  }

  def getAll()(implicit txId: TransactionId = new TransactionId): Future[List[JsObject]] =
    txManager.asReadOnly[Future[List[JsObject]]] {
      val f = DAO.getAll
      f map {res => res}
    }
}
