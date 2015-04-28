package altitude.dao.postgres


import java.sql.Connection

import altitude.Util.log
import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.models.search.Query
import altitude.services.JdbcTransactionManager
import altitude.{Const => C, Util}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao(protected val tableName: String) extends BaseDao {

  protected def conn(implicit txId: TransactionId): Connection =
    JdbcTransactionManager.transaction.conn

  protected val coreSqlColsForInsert = s"${C.Base.ID}, ${C.Base.CREATED_AT}"
  protected val coreSqlColsForSelect = s"${C.Base.ID}, ${C.Base.CREATED_AT}, ${C.Base.UPDATED_AT}"
  protected val coreSqlValuesForInsert = "?, TO_TIMESTAMP(?)"

  protected def utcNow = Util.utcNow

  protected def dtAsJsString(dt: DateTime) = JsString(Util.isoDateTime(Some(dt)))

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val q: String =s"""
      INSERT INTO $tableName ($coreSqlColsForInsert)
           VALUES ($coreSqlValuesForInsert)"""

    addRecord(jsonIn, q, List[Object]())
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    val recOpt = getRecordById(id)

    if (recOpt == None) {
      return Future[Option[JsObject]] {None}
    }

    val rec = recOpt.get

    val createdAtMilis = rec.getOrElse(C.Base.CREATED_AT, 0d).asInstanceOf[Double].toLong
    val createdAt: DateTime = new DateTime(createdAtMilis)

    val updatedAtMilis = rec.getOrElse(C.Base.UPDATED_AT, 0d).asInstanceOf[Double].toLong
    val updatedAt: DateTime = new DateTime(updatedAtMilis)

    val res = Json.obj(
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

    Future[Option[JsObject]] {Some(res)}
  }

  override def query(q: Query)(implicit txId: TransactionId): Future[List[JsObject]] = {
    throw new NotImplementedError()
  }

  protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object])(implicit txId: TransactionId): Future[JsObject] = {
    log.info(s"POSTGRES INSERT: $jsonIn", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val id = BaseModel.genId
    val createdAt = utcNow

    val values: List[Object] = id :: createdAt.getMillis.asInstanceOf[Object] :: vals

    log.debug(s"SQL: $q. ARGS: ${values.toString()}")
    run.update(conn, q, values:_*)

    Future[JsObject] {
      jsonIn ++ JsObject(Seq(
        C.Base.ID -> JsString(id),
        C.Base.CREATED_AT -> dtAsJsString{createdAt}))
    }
  }

  protected def getRecordById(id: String)(implicit txId: TransactionId): Option[Map[String, AnyRef]] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q =s"""
      SELECT ${C.Base.ID}, *,
             EXTRACT(EPOCH FROM created_at) AS created_at,
             EXTRACT(EPOCH FROM updated_at) AS updated_at
        FROM $tableName
       WHERE ${C.Base.ID} = ?"""

    oneBySqlQuery(q, List(id))
  }

  protected def oneBySqlQuery(q: String, vals: List[Object])(implicit txId: TransactionId): Option[Map[String, AnyRef]] = {
    val run: QueryRunner = new QueryRunner()

    log.debug(s"SQL: $q")
    val res = run.query(conn, q, new MapListHandler(), vals:_*)

    log.debug(s"Found ${res.size()} records", C.tag.DB)

    if (res.size() == 0)
      return None

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)

    log.debug(s"Record: $rec")
    Some(rec.toMap[String, AnyRef])
  }

  def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = {
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