package altitude.service

import altitude.Cleaners.Cleaner
import altitude.Validators.Validator
import altitude.dao.BaseDao
import altitude.exceptions.{DuplicateException, NotFoundException}
import altitude.models.BaseModel
import altitude.models.search.{Query, QueryResult}
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import altitude.{Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel](app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO: BaseDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]
  protected val VALIDATOR: Option[Validator] = None
  protected val CLEANER: Option[Cleaner] = None

  def add(objIn: Model, queryForDup: Option[Query] = None)
         (implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    val cleaned = cleanAndValidate(objIn)

    val existing = if (queryForDup.isDefined) query(queryForDup.get) else QueryResult.EMPTY

    if (existing.nonEmpty) {
      log.debug(s"Duplicate found for $objIn and query: ${queryForDup.get.params}")
      throw DuplicateException(objIn, existing.records.head)
    }

    txManager.withTransaction[JsObject] {
      DAO.add(cleaned)
    }
  }

  def updateById(id: String, objIn: Model, fields: List[String], queryForDup: Option[Query] = None)
                (implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    val cleaned = cleanAndValidate(objIn)

    val existing = if (queryForDup.isDefined) query(queryForDup.get) else QueryResult.EMPTY

    if (existing.nonEmpty) {
      log.debug(s"Duplicate found for $objIn and query: ${queryForDup.get.params}")
      throw DuplicateException(objIn, existing.records.head)
    }

    txManager.withTransaction[Int] {
      DAO.updateById(id, cleaned, fields)
    }
  }

  def cleanAndValidate(objIn: Model): JsObject = {
    val cleaned = CLEANER match {
      case None => objIn.toJson
      case _ => CLEANER.get.clean(objIn.toJson)
    }

    // validate
    VALIDATOR match {
      case Some(validator) => {
        VALIDATOR.get.validate(cleaned)
        cleaned
      }
      case _ => cleaned
    }
  }

  def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      val res: Option[JsObject] = DAO.getById(id)

      res.isDefined match {
        case true => res.get
        case false => throw NotFoundException(s"Cannot find ID '$id'")
      }
    }
  }

  def getAll(implicit ctx: Context, txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.getAll
    }
  }

  def query(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): QueryResult = {
    txManager.asReadOnly[QueryResult] {
      DAO.query(query)
    }
  }

  def deleteById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    txManager.withTransaction[Int] {
      DAO.deleteById(id)
    }
  }

  def deleteByQuery(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    txManager.withTransaction[Int] {
      DAO.deleteByQuery(query)
    }
  }
}
