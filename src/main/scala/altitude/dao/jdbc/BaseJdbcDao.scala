package altitude.dao.jdbc

import java.sql.Connection

import altitude.dao.BaseDao
import altitude.models.search.{Query, QueryResult}
import altitude.models.{BaseModel, User}
import altitude.transactions.{JdbcTransactionManager, TransactionId}
import altitude.{Const => C, Util}
import net.codingwell.scalaguice.InjectorExtensions._
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.{MapListHandler, ScalarHandler}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.collection.JavaConversions._

abstract class BaseJdbcDao(val tableName: String) extends BaseDao {
  private final val log = LoggerFactory.getLogger(getClass)

  protected final def txManager = app.injector.instance[JdbcTransactionManager]

  protected def conn(implicit txId: TransactionId): Connection = {
    // get transaction from the global lookup
    txManager.transaction.getConnection
  }

  protected def CORE_SQL_VALS_FOR_INSERT: String
  protected def DEFAULT_SQL_COLS_FOR_SELECT: String
  // used instead of DEFAULT_SQL_COLS_FOR_SELECT if needed in a separate DAO
  protected def JSON_FUNC: String
  protected def CURRENT_TIME_FUNC: String
  protected def DATETIME_TO_DB_FUNC(datetime: Option[DateTime]): String

  protected def GET_DATETIME_FROM_REC(field: String, rec: Map[String, AnyRef]): Option[DateTime]

  protected val CORE_SQL_COLS_FOR_INSERT = s"${C("Base.ID")}"
  protected val SYSTEM_TABLE = "system"

  protected def utcNow = Util.utcNow

  protected def dtAsJsString(dt: DateTime) = JsString(Util.isoDateTime(Some(dt)))

  protected lazy val SQL_QUERY_BUILDER = new SqlQueryBuilder(DEFAULT_SQL_COLS_FOR_SELECT, tableName)


  // SQL to select the whole record, in very simple cases
  protected val ONE_SQL = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $tableName
       WHERE ${C("Base.ID")} = ?"""

  override def add(jsonIn: JsObject)(implicit user: User, txId: TransactionId): JsObject = {
    val sql: String =s"""
      INSERT INTO $tableName ($CORE_SQL_COLS_FOR_INSERT)
           VALUES ($CORE_SQL_VALS_FOR_INSERT)"""

    addRecord(jsonIn, sql, List[Object]())
  }

  override def getById(id: String)(implicit user: User, txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$tableName'", C.LogTag.DB)
    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(ONE_SQL, List(id))
    if (rec.isDefined) Some(makeModel(rec.get)) else None
  }

  override def deleteByQuery(q: Query)(implicit user: User, txId: TransactionId): Int = {
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

  override def query(q: Query)(implicit user: User, txId: TransactionId): QueryResult =
    this.query(q, SQL_QUERY_BUILDER)

  def query(query: Query, sqlQueryBuilder: SqlQueryBuilder)
                    (implicit user: User, txId: TransactionId): QueryResult = {
    val sqlQuery: SqlQuery = sqlQueryBuilder.toSelectQuery(query)
    val recs = manyBySqlQuery(sqlQuery.queryString, sqlQuery.selectBindValues)

    // do not perform a count query if we got zero results in the first place
    val count: Int =  getQueryResultCount(query, sqlQuery.selectBindValues)

    log.debug(s"Found [$count] records. Retrieved [${recs.length}] records")
    log.debug(recs.map(_.toString()).mkString("\n"))
    QueryResult(records = recs.map{makeModel}, total = count, query = Some(query))
  }

  protected def getQueryResultCount(query: Query, values: List[Object] = List())
                                   (implicit  user: User, txId: TransactionId): Int = {
    val sqlCountQuery: SqlQuery = SQL_QUERY_BUILDER.toSelectQuery(query, countOnly = true)
    val runner: QueryRunner = new QueryRunner()

    // We are defensive with different JDBC drivers operating with either java.lang.Int or java.lang.Long
    runner.query(conn, sqlCountQuery.queryString, new ScalarHandler[AnyRef]("count"),  values: _*) match {
      case v: java.lang.Integer => v.intValue
      case v: java.lang.Long => v.asInstanceOf[Long].toInt
    }
  }

  protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object])
                         (implicit  user: User, txId: TransactionId): JsObject = {
    log.info(s"JDBC INSERT: $jsonIn")

    val existingObjId: Option[String] = (jsonIn \ C("Base.ID")).asOpt[String]

    // create ID unless there is an override
    val id = if (existingObjId.isDefined) existingObjId.get else BaseModel.genId

    // prepend ID, as it is required for any record (in base implementation)
    val values: List[Object] = id :: vals
    log.debug(s"INSERT SQL: $q. ARGS: ${values.toString()}")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, values:_*)

    val recordJson = jsonIn ++ JsObject(Seq(
      C("Base.ID") -> JsString(id)))

    log.debug(s"Added: $recordJson")
    recordJson
  }

  protected def addRecords(q: String, vals: List[Object])
                         (implicit  user: User, txId: TransactionId) = {
    log.debug(s"INSERT MULTIPLE SQL: $q. ARGS: ${vals.toString()}")
    new QueryRunner().update(conn, q, vals:_*)
  }

  protected def manyBySqlQuery(sql: String, values: List[Object] = List())
                              (implicit txId: TransactionId): List[Map[String, AnyRef]] = {
    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), values: _*)
    log.debug(s"Found ${res.size()} records", C.LogTag.DB)
    res.map{_.toMap[String, AnyRef]}.toList
  }

  protected def oneBySqlQuery(sql: String, vals: List[Object] = List())
                             (implicit txId: TransactionId): Option[Map[String, AnyRef]] = {
    log.debug(s"SQL: $sql")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), vals:_*)

    log.debug(s"Found ${res.size()} records", C.LogTag.DB)

    if (res.size() == 0)
      return None

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.head

    log.debug(s"RECORD: $rec")
    Some(rec.toMap[String, AnyRef])
  }

  override def getByIds(ids: Set[String])
                       (implicit user: User, txId: TransactionId): List[JsObject] = {
    val placeholders = ids.toSeq.map(x => "?")
    val sql = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $tableName
       WHERE id IN (${placeholders.mkString(",")})
      """

    log.debug(s"SQL: $sql with values: ${ids.toList}")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), ids.toList: _*)
    log.debug(s"Found ${res.size()} records", C.LogTag.DB)
    val recs = res.map{_.toMap[String, AnyRef]}.toList
    recs.map{makeModel}
  }

  override def updateByQuery(q: Query, json: JsObject, fields: List[String])
                            (implicit user: User, txId: TransactionId): Int = {
    log.debug(s"Updating record by query $q with data $json for fields: $fields")

    val queryFieldPlaceholders: List[String] = q.params.keys.map(_ + " = ?").toList
    val updateFieldPlaceholders: List[String] = json.keys.filter(fields.contains(_)).map(_ + " = ?").toList

    val sql = s"""
      UPDATE $tableName
         SET ${C("Base.UPDATED_AT")} = $CURRENT_TIME_FUNC, ${updateFieldPlaceholders.mkString(", ")}
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

  override def increment(id: String, field: String, count: Int = 1)
                        (implicit user: User, txId: TransactionId) = {
    val sql = s"""
      UPDATE $tableName
         SET $field = $field + $count
       WHERE id = ?
      """
    log.debug(s"INCR SQL: $sql, for $id")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, id)
  }

  override def decrement(id: String,  field: String, count: Int = 1)
                        (implicit user: User, txId: TransactionId) = {
    increment(id, field, -count)
  }

  // Make a string of SQL placeholders for a list - such as "? , ? ,?, ?"
  protected def makeSqlPlaceholders(s: Seq[AnyRef]): String = s.map(x => "?").mkString(",")

  /*
    Implementations should define this method, which returns an optional
    JSON object which is guaranteed to serialize into a valid model backing this class.
    JSON can be constructed directly, but best to create a model instance first
    and return it, triggering implicit conversion.
   */
  protected def makeModel(rec: Map[String, AnyRef]): JsObject

  /* Given a model and an SQL record, calculate and set properties common to most models
   */
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit
}