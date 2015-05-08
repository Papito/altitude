package altitude.services

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.Altitude
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject

import scala.concurrent.Future
import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel](app: Altitude) {
  protected val DAO: BaseDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def add(obj: Model)(implicit txId: TransactionId = new TransactionId): Future[JsObject] = {
    txManager.withTransaction[JsObject] {
      DAO.add(obj.toJson)
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): Future[Option[JsObject]] = {
    txManager.asReadOnly[Option[JsObject]] {
      DAO.getById(id)
    }
  }

  def getAll()(implicit txId: TransactionId = new TransactionId): Future[List[JsObject]] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.getAll
    }
  }

  def query(query: Query)(implicit txId: TransactionId = new TransactionId): Future[List[JsObject]] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.query(query)
    }
  }
}
