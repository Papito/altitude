package altitude.dao.jdbc

import java.sql.Connection

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.transactions.{JdbcTransactionManager, TransactionId}
import altitude.{Const => C, Util}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.collection.JavaConversions._
import net.codingwell.scalaguice.InjectorExtensions._

abstract class BaseJdbcDao(val tableName: String) extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  protected final def txManager = app.injector.instance[JdbcTransactionManager]

  protected def conn(implicit txId: TransactionId): Connection = {
    // get transaction from the global lookup
    txManager.transaction.getConnection
  }

  protected def CORE_SQL_VALS_FOR_INSERT: String
  protected def DEFAULT_SQL_COLS_FOR_SELECT: String
  protected def JSON_PLACEHOLDER: String
  protected def CURRENT_TIME_FUNC: String
  protected val CORE_SQL_COLS_FOR_INSERT = s"${C.Base.ID}"
  protected val VERSION_TABLE_NAME = "db_version"

  protected def utcNow = Util.utcNow

  protected def dtAsJsString(dt: DateTime) = JsString(Util.isoDateTime(Some(dt)))

  protected lazy val SQL_QUERY_BUILDER = new SqlQueryBuilder(DEFAULT_SQL_COLS_FOR_SELECT, tableName)


  // SQL to select the whole record, in very simple cases
  protected val ONE_SQL = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $tableName
       WHERE ${C.Base.ID} = ?"""

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val sql: String =s"""
      INSERT INTO $tableName ($CORE_SQL_COLS_FOR_INSERT)
           VALUES ($CORE_SQL_VALS_FOR_INSERT)"""

    addRecord(jsonIn, sql, List[Object]())
  }

  override def getById(id: String)(implicit txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$tableName'", C.LogTag.DB)
    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(ONE_SQL, List(id))

    rec match {
      case None => None
      case _ => Some(makeModel(rec.get))
    }
  }

  override def deleteByQuery(q: Query)(implicit txId: TransactionId): Int = {
    if (q.params.isEmpty) {
      return 0
    }

    log.debug(s"Deleting record by query: $q")
    val fieldPlaceholders: List[String] = q.params.keys.map(_ + " = ?").toList

    val sql = s"""
      DELETE
        FROM $tableName
       WHERE ${fieldPlaceholders.mkString(",")}
      """

    log.debug(s"Delete SQL: $sql, with values: ${q.params.values.toList}")
    val runner: QueryRunner = new QueryRunner()
    val numDeleted = runner.update(conn, sql,  q.params.values.toList:_*)
    log.debug(s"Deleted records: $numDeleted")
    numDeleted
  }

  override def query(query: Query)(implicit txId: TransactionId): List[JsObject] = {
    val sqlQuery: SqlQuery = SQL_QUERY_BUILDER.toSelectQuery(query)
    val recs = manyBySqlQuery(sqlQuery.queryString, sqlQuery.selectBindValues)
    log.debug(s"Found: ${recs.length}")
    log.debug(recs.map(_.toString()).mkString("\n"))
    recs.map{makeModel}
  }

  protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object], id: Option[String] = None)
                         (implicit txId: TransactionId): JsObject = {
    log.info(s"JDBC INSERT: $jsonIn")

    // create ID unless there is an override
    val _id = id.isDefined match {
      case false => BaseModel.genId
      case true => id.get
    }
    val createdAt = utcNow

    // prepend ID and CREATED AT to the values, as those are required for any record
    val values: List[Object] = _id :: vals
    log.debug(s"INSERT SQL: $q. ARGS: ${values.toString()}")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, values:_*)

    val recordJson = jsonIn ++ JsObject(Seq(
      C.Base.ID -> JsString(_id),
      C.Base.CREATED_AT -> dtAsJsString{createdAt}))

    log.debug(s"Added: $recordJson")
    recordJson
  }

  protected def manyBySqlQuery(sql: String, vals: List[Object] = List())(implicit txId: TransactionId): List[Map[String, AnyRef]] = {
    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), vals: _*)
    log.debug(s"Found ${res.size()} records", C.LogTag.DB)
    res.map{_.toMap[String, AnyRef]}.toList
  }

  protected def oneBySqlQuery(sql: String, vals: List[Object] = List())(implicit txId: TransactionId): Option[Map[String, AnyRef]] = {
    //log.debug(s"SQL: $sql")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), vals:_*)

    log.debug(s"Found ${res.size()} records", C.LogTag.DB)

    if (res.size() == 0)
      return None

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)

    log.debug(s"RECORD: $rec")
    Some(rec.toMap[String, AnyRef])
  }

  override def updateByQuery(q: Query, json: JsObject, fields: List[String])(implicit txId: TransactionId): Int = {
    log.debug(s"Updating record by query $q with data $json for fields: $fields")

    val queryFieldPlaceholders: List[String] = q.params.keys.map(_ + " = ?").toList
    val updateFieldPlaceholders: List[String] = json.keys.filter(fields.contains(_)).map(_ + " = ?").toList

    val sql = s"""
      UPDATE $tableName
         SET ${C.Base.UPDATED_AT} = $CURRENT_TIME_FUNC, ${updateFieldPlaceholders.mkString(", ")}
       WHERE ${queryFieldPlaceholders.mkString(",")}
      """

    val dataUpdateValues = json.fields.filter{
      // extract only the json elements we want to update
      v: (String, JsValue) => fields.contains(v._1)}.map{
      // convert the values to string
      v: (String, JsValue) => v._2.as[String]}.toList.reverse

    log.debug(s"Update SQL: $sql, with query values: ${q.params.values.toList} and data: $dataUpdateValues")
    val runner: QueryRunner = new QueryRunner()
    val valuesForAllPlaceholders = dataUpdateValues ::: q.params.values.toList

    val numUpdated = runner.update(conn, sql,  valuesForAllPlaceholders:_*)
    numUpdated
  }

  override def increment(id: String, field: String, count: Int = 1)(implicit txId: TransactionId) = {
    val sql = s"""
      UPDATE $tableName
         SET $field = $field + $count
       WHERE id = ?
      """
    log.debug(s"INCR SQL: $sql, for $id")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, id)
  }

  override def decrement(id: String,  field: String, count: Int = 1)(implicit txId: TransactionId) = {
    increment(id, field, -count)
  }


  /*
    Implementations should define this method, which returns an optional
    JSON object which is guaranteed to serialize into a valid model of interest.
    JSON can be constructed directly, but best to create a model instance first
    and return it, triggering implicit conversion.
   */
  protected def makeModel(rec: Map[String, AnyRef]): JsObject

  /* Given a model and an SQL record, decipher and set certain core properties
   */
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit
}