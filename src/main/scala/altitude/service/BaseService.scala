package altitude.service

import altitude.{Const => C, Altitude}
import altitude.dao.BaseDao
import altitude.exceptions.NotFoundException
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject

import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel](app: Altitude) {
  protected val DAO: BaseDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def add(obj: Model)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.withTransaction[JsObject] {
      DAO.add(obj.toJson)
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      val res: Option[JsObject] = DAO.getById(id)

      res.isDefined match {
        case false => throw new NotFoundException(C.IdType.ID, id)
        case true => res.get
      }
    }
  }

  def getAll()(implicit txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.getAll
    }
  }

  def query(query: Query)(implicit txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.query(query)
    }
  }
}
