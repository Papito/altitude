package altitude.dao.jdbc

import java.sql.Connection

import altitude.dao.BaseDao
import altitude.exceptions.NotFoundException
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

abstract class BaseJdbcDao extends BaseDao {
  val log = LoggerFactory.getLogger(getClass)

  protected val tableName: String
  protected val jdbcTxManager = new JdbcTransactionManager(app)

  protected def conn(implicit txId: TransactionId): Connection = {
    // get transaction from the global lookup
    jdbcTxManager.transaction.conn
  }

  protected val CORE_SQL_COLS_FOR_INSERT = s"${C.Base.ID}, ${C.Base.CREATED_AT}"
  protected val CORE_SQL_VALS_FOR_INSERT = "?, TO_TIMESTAMP(?)"

  protected val DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *,
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """

  protected def utcNow = Util.utcNow

  protected def dtAsJsString(dt: DateTime) = JsString(Util.isoDateTime(Some(dt)))

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

  override def query(query: Query)(implicit txId: TransactionId): List[JsObject] = {
    val (sqlColumns, sqlValues) = query.params.unzip
    // create pairs of column names and value placeholders, to be joined in the final clause
    val whereClauses: List[String] = for (column <- sqlColumns.toList) yield  s"$column = ?"

    val whereClause = whereClauses.length match {
      case 0 => ""
      case _ => s"""WHERE ${whereClauses.mkString("AND")}"""
    }

    val sql = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM $tableName
        $whereClause
       LIMIT ${query.rpp}"""

    val recs = manyBySqlQuery(sql, sqlValues.toList)

    log.debug(s"Found: ${recs.length}")
    recs.map{makeModel}
  }

  protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object])(implicit txId: TransactionId): JsObject = {
    //log.info(s"POSTGRES INSERT: $jsonIn", C.tag.DB)
    val id = BaseModel.genId
    val createdAt = utcNow

    // prepend ID and CREATED AT to the values, as those are required for any record
    val values: List[Object] = id :: createdAt.getMillis.asInstanceOf[Object] :: vals
    //log.debug(s"SQL: $q. ARGS: ${values.toString()}")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, values:_*)

    jsonIn ++ JsObject(Seq(
      C.Base.ID -> JsString(id),
      C.Base.CREATED_AT -> dtAsJsString{createdAt}))
  }

  protected def manyBySqlQuery(sql: String, vals: List[Object])(implicit txId: TransactionId): List[Map[String, AnyRef]] = {
    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), vals: _*)
    log.debug(s"Found ${res.size()} records", C.LogTag.DB)
    res.map{_.toMap[String, AnyRef]}.toList
  }

  protected def oneBySqlQuery(sql: String, vals: List[Object])(implicit txId: TransactionId): Option[Map[String, AnyRef]] = {
    //log.debug(s"SQL: $sql")

    val runner: QueryRunner = new QueryRunner()
    val res = runner.query(conn, sql, new MapListHandler(), vals:_*)

    log.debug(s"Found ${res.size()} records", C.LogTag.DB)

    if (res.size() == 0)
      throw new NotFoundException(C.IdType.QUERY, sql)

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)

    //log.debug(s"Record: $rec")
    Some(rec.toMap[String, AnyRef])
  }

  /*
    Implementations should define this method, which returns an optional
    JSON object which is guaranteed to serialize into a valid model of interest.
    JSON can be constructed directly, but best to create a model instance first
    and return it, triggering implicit conversion.
   */
  protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val createdAtMilis = rec.getOrElse(C.Base.CREATED_AT, 0d).asInstanceOf[Double].toLong
    val createdAt: DateTime = new DateTime(createdAtMilis)

    val updatedAtMilis = rec.getOrElse(C.Base.UPDATED_AT, 0d).asInstanceOf[Double].toLong
    val updatedAt: DateTime = new DateTime(updatedAtMilis)

    Json.obj(
      C.Base.ID -> {rec.get(C.Base.ID).isDefined match {
        case false => JsNull
        case _ => rec.get(C.Base.ID).get.toString
      }},
      C.Base.CREATED_AT -> {createdAtMilis match {
        case 0d => JsNull
        case _ => Util.isoDateTime(Some(createdAt))
      }},
      C.Base.UPDATED_AT -> {updatedAtMilis match {
        case 0d => JsNull
        case _ => Util.isoDateTime(Some(updatedAt))
      }}
    )
  }

  /* Given a model and an SQL record, decipher and set certain core properties
   */
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = {
    val createdAtMilis = rec.getOrElse(C.Base.CREATED_AT, 0d).asInstanceOf[Double].toLong
    if (createdAtMilis != 0d) {
      model.createdAt = new DateTime(createdAtMilis)
    }

    val updatedAtMilis = rec.getOrElse(C.Base.UPDATED_AT, 0d).asInstanceOf[Double].toLong
    if (updatedAtMilis != 0d) {
      model.createdAt = new DateTime(createdAtMilis)
    }
  }
}