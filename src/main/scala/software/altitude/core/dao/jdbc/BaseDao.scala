package software.altitude.core.dao.jdbc

import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.apache.commons.dbutils.handlers.ScalarHandler
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.AltitudeAppContext
import software.altitude.core.ConstraintException
import software.altitude.core.RequestContext
import software.altitude.core.Util
import software.altitude.core.dao.jdbc.querybuilder.SqlQuery
import software.altitude.core.dao.jdbc.querybuilder.SqlQueryBuilder
import software.altitude.core.models.BaseModel
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.{Const => C}

import java.util.regex.Pattern
import scala.jdk.CollectionConverters._


abstract class BaseDao {
  val appContext: AltitudeAppContext

  private final val log = LoggerFactory.getLogger(getClass)

  val tableName: String

  protected final def txManager: TransactionManager = appContext.txManager

  protected def defaultSqlColsForSelect: List[String]

  protected val sqlQueryBuilder: SqlQueryBuilder[Query] = new SqlQueryBuilder[Query](defaultSqlColsForSelect, tableName)

  // if supported, DB function to store native JSON data
  protected def jsonFunc: String
  // DB current time function
  protected def nowTimeFunc: String

  // conversion function to go from Java time to DB time
  protected def dateTimeToDbFunc(datetime: Option[DateTime]): String
  // opposite of the above
  protected def getDateTimeFromRec(field: String, rec: Map[String, AnyRef]): Option[DateTime]

  // common fields for new records, and their placeholders - mostly to avoid repetition
  protected val coreSqlColsForInsert: List[String] = List(C.Base.ID, C.Base.REPO_ID)
  protected def coreSqlValsForInsert: String = "?, ?"

  // this is the valid ID pattern
  private val VALID_ID_PATTERN = Pattern.compile("[a-z0-9]+")

  /**
   * Verify that a DB id to be used is valid
   */
  private def verifyId(id: String): Unit = {
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

  // SQL to select the whole record, in very simple cases
  protected val oneRecSelectSql: String = s"""
      SELECT ${defaultSqlColsForSelect.mkString(", ")}
        FROM $tableName
       WHERE ${C.Base.ID} = ? AND ${C.Base.REPO_ID} = ?"""

  /**
   * Add a single record
   * @param jsonIn JsObject OR a model
   * @return JsObject of the added record, with ID of the record in the databases
   */
  def add(jsonIn: JsObject): JsObject = {
    val sql: String = s"""
      INSERT INTO $tableName (${coreSqlColsForInsert.mkString(", ")})
           VALUES ($coreSqlValsForInsert)"""

    addRecord(jsonIn, sql, List[Any]())
  }

  /**
   * Gert a single record by ID
   *
   * @param id record id as string
   * @return optional JsObject, which implicitly can be turned into an instance of a concrete domain
   *         model. This method does NOT throw a NotFound error, as it is not assumed it is always
   *         and error.
   */
  def getById(id: String): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$tableName'")
    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(oneRecSelectSql, List(id, RequestContext.getRepository.id.get))
    if (rec.isDefined) Some(makeModel(rec.get)) else None
  }

  def getAll: List[JsObject] = query(new Query()).records

  /**
   * Delete a document by its ID
   *
   * @return number of documents deleted - 0 or 1
   */
  def deleteById(id: String): Int = {
    val q: Query = new Query().add(C.Base.ID -> id)
    deleteByQuery(q)
  }

  /**
   * Update a document by ID with select field values (does not overwrite the document)
   *
   * @param id id of the document to be updated
   * @param data JSON data for the update document, which is NOT used to overwrite the existing one
   * @param fields fields to be updated with new values, taken from <code>data</code>
   *
   * @return number of documents updated - 0 or 1
   */
  def updateById(id: String, data: JsObject, fields: List[String])
                : Int = {
    val q: Query = new Query().add(C.Base.ID -> id)
    updateByQuery(q, data, fields)
  }

  /**
   * Delete one or more document by query.
   *
   * @return number of documents deleted
   */
  def deleteByQuery(q: Query): Int = {
    log.debug(s"Deleting record by query: $q")
    val fieldPlaceholders: List[String] = q.params.keys.map(_ + " = ?").toList

    val sql = s"""
      DELETE
        FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${fieldPlaceholders.mkString(",")}
      """

    log.debug(s"Delete SQL: $sql, with values: ${q.params.values.toList}")
    val runner: QueryRunner = new QueryRunner()
    val numDeleted = runner.update(
      RequestContext.getConn, sql, RequestContext.getRepository.id.get :: q.params.values.toList.map(_.asInstanceOf[Object]): _*)
    log.debug(s"Deleted records: $numDeleted")
    numDeleted
  }

  /**
   * Delete by SQL.
   *
   * @return number of documents deleted
   */
  def deleteBySql(sql: String, bindValues: List[Object]): Int = {
    log.debug(s"Delete SQL: $sql, with values: $bindValues")
    val runner: QueryRunner = new QueryRunner()
    val numDeleted = runner.update(RequestContext.getConn, sql, bindValues: _*)
    log.debug(s"Deleted records: $numDeleted")
    numDeleted
  }

  /**
   * Get multiple documents using a Query
   */
  def query(q: Query): QueryResult = {
    this.query(q, sqlQueryBuilder)
  }

  /**
   * Internal version for querying with a customized query builder
   */
  protected def query(query: Query, sqlQueryBuilder: SqlQueryBuilder[Query])
           : QueryResult = {
    val sqlQuery: SqlQuery = sqlQueryBuilder.buildSelectSql(query)

    val recs = manyBySqlQuery(sqlQuery.sqlAsString, sqlQuery.bindValues)
    val count: Int = getQueryResultCount(query, sqlQueryBuilder, sqlQuery.bindValues)

    log.debug(s"Found [$count] records. Retrieved [${recs.length}] records")
    if (recs.nonEmpty) {
      log.debug(recs.map(_.toString()).mkString("\n"))
    }
    QueryResult(records = recs.map{makeModel}, total = count, rpp = query.rpp, sort = query.sort.toList)
  }

  protected def getQueryResultCount(query: Query, sqlQueryBuilder: SqlQueryBuilder[Query], values: List[Any] = List())
                                   : Int = {
    val sqlCountQuery: SqlQuery = sqlQueryBuilder.buildCountSql(query)
    getQueryResultCountBySql(sqlCountQuery.sqlAsString, values)
  }

  protected def getQueryResultCountBySql(sql: String, values: List[Any] = List())
                                   : Int = {
    val runner: QueryRunner = new QueryRunner()

    // We are defensive with different JDBC drivers operating with either java.lang.Int or java.lang.Long
    runner.query(RequestContext.getConn, sql, new ScalarHandler[AnyRef]("count"), values.map(_.asInstanceOf[Object]): _*) match {
      case v: java.lang.Integer => v.intValue
      case v: java.lang.Long => v.asInstanceOf[Long].toInt
      case null => 0
    }
  }

  /**
   * Internally used method to add records. It's convenient for classes overriding
   * the add() method, as it accepts a ready-to-go SQL query, with bind methods.
   * This function takes care of the actual plumbing common to all add() methods.
   */
  protected def addRecord(jsonIn: JsObject, sql: String, values: List[Any])
                         : JsObject = {
    log.info(s"JDBC INSERT: $jsonIn")

    val existingObjId: Option[String] = (jsonIn \ C.Base.ID).asOpt[String]

    // create ID unless there is an override
    val id = if (existingObjId.isDefined) existingObjId.get else BaseModel.genId
    verifyId(id)

    // prepend ID and REPO ID, as it is required for most records
    val _values: List[Any] = combineInsertValues(id, values)
    log.debug(s"INSERT SQL: $sql. ARGS: ${_values.toString()}")

    val runner: QueryRunner = new QueryRunner()
    runner.update(RequestContext.getConn, sql, _values.map(_.asInstanceOf[Object]): _*)

    val recordJson = jsonIn ++ Json.obj(C.Base.ID -> id)

    log.debug(s"Added: $recordJson")
    recordJson
  }

  /**
   * Create an array of values to insert, joining them in proper order.
   * The base implementation assumes that it's [ID, REPO ID] + [THE REST],
   * but it may not always be the case.
   *
   * @param id is always required
   * @param vals any arbitrary values
   * @return array of values to be bound to columns
   */
  protected def combineInsertValues(id: String, vals: List[Any]): List[Any] =
    id :: RequestContext.getRepository.id.get :: vals

  protected def manyBySqlQuery(sql: String, values: List[Any] = List())
                              : List[Map[String, AnyRef]] = {
    log.debug(s"Running SQL query [$sql] with $values")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(RequestContext.getConn, sql, new MapListHandler(), values.map(_.asInstanceOf[Object]): _*).asScala.toList
    log.debug(s"Found ${res.length} records")
    // FIXME: is spacing here a scala Style violation?
    res.map{_.asScala.toMap[String, AnyRef]}.toList
  }

  /**
   * Internal method to return a UNIQUE object from DB. Does not just get the first one.
   *
   * @throws ConstraintException if a DB constraint is missed and more than one record is found
   */
  def oneBySqlQuery(sql: String, values: List[Any] = List()): Option[Map[String, AnyRef]] = {
    log.debug(s"Running SQL query [$sql] with $values")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(RequestContext.getConn, sql, new MapListHandler(), values.map(_.asInstanceOf[Object]): _*).asScala.toList
    log.debug(s"Found ${res.length} records")

    if (res.isEmpty) {
      return None
    }

    if (res.length > 1) {
      throw ConstraintException("getById should return only a single result")
    }

    val rec = res.head

    log.debug(s"RECORD: $rec")
    Some(rec.asScala.toMap)
  }

  def getByIds(ids: Set[String])
                       : List[JsObject] = {
    val placeholders = ids.toSeq.map(_ => "?")
    val sql = s"""
      SELECT ${defaultSqlColsForSelect.mkString(", ")}
        FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND id IN (${placeholders.mkString(",")})
      """

    log.debug(s"SQL: $sql with values: ${ids.toList}")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(RequestContext.getConn, sql, new MapListHandler(), RequestContext.getRepository.id.get :: ids.toList: _*).asScala.toList
    log.debug(s"Found ${res.length} records")
    val recs = res.map{_.asScala.toMap[String, AnyRef]}
    recs.map{makeModel}
  }

  def updateByQuery(q: Query, json: JsObject, fields: List[String])
                            : Int = {
    log.debug(s"Updating record by query $q with data $json for fields: $fields")

    val queryFieldPlaceholders: List[String] = q.params.keys.map(_ + " = ?").toList
    val updateFieldPlaceholders: List[String] = json.fields.filter {
      // extract only the json elements we want to update
      v: (String, JsValue) => fields.contains(v._1)}.map {
      v: (String, JsValue) => s"${v._1} = ?"
    }.toList

    val sql = s"""
      UPDATE $tableName
         SET ${C.Base.UPDATED_AT} = $nowTimeFunc, ${updateFieldPlaceholders.mkString(", ")}
       WHERE ${C.Base.REPO_ID} = ? AND ${queryFieldPlaceholders.mkString(",")}
      """

    val dataUpdateValues: List[Any] = json.fields.filter {
      // extract only the json elements we want to update
      v: (String, JsValue) => fields.contains(v._1)}.map {
      // convert the values to string
      v: (String, JsValue) => {
        val jsVal: JsValue = v._2

        jsVal match {
          case JsTrue => 1
          case JsFalse => 0
          case _ => jsVal.as[String]
        }
      }
    }.toList

    val valuesForAllPlaceholders = dataUpdateValues ::: List(RequestContext.getRepository.id.get) ::: q.params.values.toList
    val runner: QueryRunner = new QueryRunner()

    val numUpdated = runner.update(RequestContext.getConn, sql, valuesForAllPlaceholders.map(_.asInstanceOf[Object]): _*)
    numUpdated
  }

  def increment(id: String, field: String, count: Int = 1)
                        : Unit = {
    val sql = s"""
      UPDATE $tableName
         SET $field = $field + $count
       WHERE ${C.Base.REPO_ID} = ? AND id = ?
      """
    log.debug(s"INCR SQL: $sql, for $id")

    val runner: QueryRunner = new QueryRunner()
    runner.update(RequestContext.getConn, sql, RequestContext.getRepository.id.get, id)
  }

  def decrement(id: String, field: String, count: Int = 1)
                        : Unit = {
    increment(id, field, -count)
  }

  /**
   * Make a string of SQL placeholders for a list - such as "? , ? ,?, ?"
   */
  protected def makeSqlPlaceholders(s: Seq[AnyRef]): String = s.map(_ => "?").mkString(",")

  /**
   * Implementations should define this method, which returns an optional
   * JSON object which is guaranteed to serialize into a valid model backing this class.
   * JSON can be constructed directly, but best to create a model instance first
   * and return it, triggering implicit conversion.
   */
  protected def makeModel(rec: Map[String, AnyRef]): JsObject

  /**
   *  Given a model and an SQL record, calculate and set properties common to most models
   */
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type
}
