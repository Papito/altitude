package altitude.service

import altitude.dao.BaseDao
import altitude.exceptions.NotFoundException
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.{Const => C, Altitude}
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.JsObject
import altitude.Validators.Validator
import altitude.Cleaners.Cleaner

import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel](app: Altitude) {
  protected val DAO: BaseDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]
  protected val VALIDATOR: Option[Validator] = None
  protected val CLEANER: Option[Cleaner] = None

  def add(objIn: Model)(implicit txId: TransactionId = new TransactionId): JsObject = {
    // clean
    val cleaned = CLEANER match {
      case None => objIn.toJson
      case _ => CLEANER.get.clean(objIn.toJson)
    }

    // validate
    VALIDATOR match {
      case Some(validator) => VALIDATOR.get.validate(cleaned)
      case None => 
    }

    txManager.withTransaction[JsObject] {
      DAO.add(cleaned)
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      val res: Option[JsObject] = DAO.getById(id)

      res.isDefined match {
        case true => res.get
        case false => throw new NotFoundException(C.IdType.ID, id)
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
