package altitude.service

import altitude.Cleaners.Cleaner
import altitude.Validators.ModelDataValidator
import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.util.{Query, QueryResult}
import altitude.{NotFoundException, DuplicateException, Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

import scala.language.implicitConversions

abstract class BaseService[Model <: BaseModel] {
  protected val app: Altitude
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO: BaseDao
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  // object cleaner
  protected val CLEANER: Option[Cleaner] = None
  // object validator, invoked AFTER the cleaner
  protected val VALIDATOR: Option[ModelDataValidator] = None

  /**
   * Add a single document
   * @param objIn the document to be added
   * @param queryForDup query to find duplicate documents
   *
   * @return the document, complete with its ID in the database
   */
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

  /**
   * Update a document by ID with select field values (does not overwrite the document)
   *
   * @param id id of the document to be updated
   * @param data JSON data for the update document, which is NOT used to overwrite the existing one
   * @param fields fields to be updated with new values, taken from <code>data</code>
   * @param queryForDup query to find a document that will violate a constraint after updating
   *                    this document with this particular data set. For example, prevent
   *                    updating a record with a certain email if that email already exists somewhere
   *                    else. The DAO layer will relegate this to the storage engine to deal with,
   *                    if this is missed.
   *
   * @return number of documents updated - 0 or 1
   */
  def updateById(id: String, data: Model, fields: List[String], queryForDup: Option[Query] = None)
                (implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    val cleaned = cleanAndValidate(data)

    val existing = if (queryForDup.isDefined) query(queryForDup.get) else QueryResult.EMPTY

    if (existing.nonEmpty) {
      log.debug(s"Duplicate found for $data and query: ${queryForDup.get.params}")
      throw DuplicateException(data, existing.records.head)
    }

    txManager.withTransaction[Int] {
      DAO.updateById(id, cleaned, fields)
    }
  }

  /**
   * Update multiple documents by query with select field values (does not overwrite the document).
   * Faster but less safe - constraint violations are handled by the store directly.
   *
   * @param query the query
   * @param data JSON data for the update documents, which is NOT used to overwrite the existing one
   * @param fields fields to be updated with new values, taken from <code>data</code>
   * @throws RuntimeException if attempting to update all documents with an empty query
   *
   * @return number of documents updated
   */
  def updateByQuery(query: Query, data: JsObject, fields: List[String])
                   (implicit ctx: Context, txId: TransactionId): Int = {
    if (query.params.isEmpty) {
      throw new RuntimeException("Cannot update [ALL] document with an empty Query")
    }

    txManager.withTransaction[Int] {
      DAO.updateByQuery(query, data, fields)
    }
  }

  /**
   * User the service class-wide definitions of data cleaner and validator
   *
   * @param objIn object t clean and validate
   *
   * @return copy of the original document, cleaned and validated, if any of those steps are defined
   */
  def cleanAndValidate(objIn: Model): JsObject = {
    val cleaned = CLEANER match {
      case Some(cleaner) => cleaner.clean(objIn.toJson)
      case None => objIn.toJson
    }

    VALIDATOR match {
      case Some(validator) =>
        validator.validate(cleaned)
        cleaned
      case None => cleaned
    }
  }

  /**
   * Gert a single record by ID
   *
   * @param id record id as string
   *
   * @return the document
   */
  def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      DAO.getById(id) match {
        case Some(obj) => obj
        case None => throw NotFoundException(s"Cannot find ID '$id'")
      }
    }
  }

  /**
   * Get all documents, which you want to do only sporadically, for not-growing sets of data
   */
  def getAll(implicit ctx: Context, txId: TransactionId = new TransactionId): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      DAO.getAll
    }
  }

  /**
   * Get multiple documents using a Query
   */
  def query(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): QueryResult = {
    txManager.asReadOnly[QueryResult] {
      DAO.query(query)
    }
  }

  /**
   * Delete a document by its ID
   *
   * @return number of documents deleted - 0 or 1
   */
  def deleteById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    txManager.withTransaction[Int] {
      DAO.deleteById(id)
    }
  }

  /**
   * Delete one or more document by query.
   *
   * @throws RuntimeException if attempting to delete all documents with an empty query
   * @return number of documents deleted
   */
  def deleteByQuery(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): Int = {
    if (query.params.isEmpty) {
      throw new RuntimeException("Cannot delete [ALL] document with an empty Query")
    }

    txManager.withTransaction[Int] {
      DAO.deleteByQuery(query)
    }
  }
}
