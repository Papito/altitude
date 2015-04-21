package altitude.dao.postgres


import java.sql.Connection

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.services.JdbcTransactionManager
import altitude.Util.log
import altitude.{Const => C, Util}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao(protected val tableName: String) extends BaseDao {

  protected def conn(implicit txId: TransactionId): Connection =
    JdbcTransactionManager.transaction.conn

  protected val coreSqlColsForInsert = s"${C.Base.ID}, ${C.Base.CREATED_AT}"
  protected val coreSqlValuesForInsert = "?, TO_TIMESTAMP(?)"

  protected def utcNow = Util.utcNow

  protected def dtAsJsString(dt: DateTime) = JsString(Util.isoDateTime(Some(dt)))

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val q: String =
      s"""
         |INSERT INTO $tableName ($coreSqlColsForInsert)
         |     VALUES ($coreSqlValuesForInsert)
         |""".stripMargin

    _add(jsonIn, q, List[Object]())
  }

  protected def _add(jsonIn: JsObject, q: String, vals: List[Object])(implicit txId: TransactionId): Future[JsObject] = {
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

  override def getById(id: String)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q: String =
      s"""
         |SELECT ${C.Base.ID},
         |       EXTRACT(EPOCH FROM created_at) AS created_at,
         |       EXTRACT(EPOCH FROM updated_at) AS updated_at
         |  FROM $tableName
         | WHERE ${C.Base.ID} = ?
         |""".stripMargin

    val res = run.query(conn, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records", C.tag.DB)
    if (res.size() == 0)
      return Future[JsObject](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")
    val rec = res.get(0)

    val createdAtMilis = rec.get(C.Base.CREATED_AT).asInstanceOf[Double].toLong
    val createdAt: DateTime = new DateTime(createdAtMilis)

    val updatedAtMilis = rec.get(C.Base.UPDATED_AT).asInstanceOf[Double].toLong
    val updatedAt: DateTime = new DateTime(updatedAtMilis)

    Future[JsObject] {
      Json.obj(
        C.Base.ID -> rec.get(C.Base.ID).toString,
        C.Base.CREATED_AT -> {createdAtMilis match {
          case 0L => JsNull
          case _ => Util.isoDateTime(Some(createdAt))
        }},
        C.Base.UPDATED_AT -> {updatedAtMilis match {
          case 0L => JsNull
          case _ => Util.isoDateTime(Some(updatedAt))
        }}
      )
    }
  }
}