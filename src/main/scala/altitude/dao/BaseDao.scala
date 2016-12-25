package altitude.dao

import java.util.regex.Pattern

import altitude.models.BaseModel
import altitude.models.search.{Query, QueryResult}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject

object BaseDao {
  // this is the valid ID pattern
  private val VALID_ID_PATTERN = Pattern.compile("[a-z0-9]+")

  /**
   * Verify that a DB id to be used is valid
   */
  def verifyId(id: String) = {
    if (id == null) {
      throw new IllegalArgumentException("ID is not defined")
    }

    if (id.length != BaseModel.ID_LEN) {
      throw new IllegalArgumentException(s"ID length should be ${BaseModel.ID_LEN}. Was: [${id.length}]")
    }

    if (!VALID_ID_PATTERN.matcher(id).find()) {
      throw new IllegalArgumentException(s"ID [$id] is not alphanumeric")
    }
  }
}

trait BaseDao {
  val app: Altitude
  protected val MAX_RECORDS = app.config.getInt("db.max_records")

  /**
   * Add a single record
   * @param json JsObject OR a model
   * @return JsObject of the added record, with ID of the record in the databases
   */
  def add(json: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject

  /**
   * Delete one or more document by query.
   *
   * @throws RuntimeException if attempting to delete all documents with an empty query
   * @return number of documents deleted
   */
  def deleteByQuery(q: Query)(implicit ctx: Context, txId: TransactionId): Int

  /**
   * Gert a single record by ID
   *
   * @param id record id as string
   * @return optional JsObject, which implicitly can be turned into an instance of a concrete domain
   *         model. This method does NOT throw a NotFound error, as it is not assumed it is always
   *         and error.
   */
  def getById(id: String)(implicit ctx: Context, txId: TransactionId): Option[JsObject]

  /**
   * Get multiple records by ID lookup

   * @param id array of document IDs
   */
  def getByIds(id: Set[String])(implicit ctx: Context, txId: TransactionId): List[JsObject]


  /**
   * Get all documents, which will literally crash your machine if you do it by accident on
   * a massive document set
   */
  def getAll(implicit ctx: Context, txId: TransactionId): List[JsObject] = query(Query()).records

  /**
   * Get multiple documents using a Query
   */
  def query(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult

  /**
   * Delete a document by its ID
   *
   * @return number of documents deleted - 0 or 1
   */
  def deleteById(id: String)(implicit ctx: Context, txId: TransactionId): Int = {
    val q: Query = Query(Map(C.Base.ID -> id))
    deleteByQuery(q)
  }

  /**
   * Update a document by ID with select field values (does not overwrite the document)
   *
   * @param id id of the document to be updated
   * @param data JSON data for the update document, which is NOT used to overwrite the existing one
   * @param fields fields to be updated with new values, taken from "data"
   *
   * @return number of documents updated - 0 or 1
   */
  def updateById(id: String, data: JsObject, fields: List[String])(implicit ctx: Context, txId: TransactionId): Int = {
    val q: Query = Query(Map(C.Base.ID -> id))
    updateByQuery(q, data, fields)
  }

  /**
   * Update multiple documents by query with select field values (does not overwrite the document)
   *
   * @param q the query
   * @param data JSON data for the update documents, which is NOT used to overwrite the existing one
   * @param fields fields to be updated with new values, taken from "data"
   *
   * @return number of documents updated - 0 or 1
   */
  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit ctx: Context, txId: TransactionId): Int

  /**
   * Increment an integer field in a table by X
   *
   * @param id record id
   * @param field field to increment
   * @param count the X
   */
  def increment(id: String, field: String, count: Int = 1)(implicit ctx: Context, txId: TransactionId)

  /**
   * Decrement an integer field in a table by X
   *
   * @param id record id
   * @param field field to decrement
   * @param count the X
   */
  def decrement(id: String, field: String, count: Int = 1)(implicit ctx: Context, txId: TransactionId)
}
