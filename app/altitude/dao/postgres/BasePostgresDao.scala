package altitude.dao.postgres


import java.sql.Connection

import altitude.dao.{BaseDao, TransactionId}
import altitude.services.JdbcTransactionManager
import altitude.util.log
import altitude.{Const => C}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao(protected val tableName: String) extends BaseDao {

  protected def conn(implicit txId: TransactionId): Connection =
    JdbcTransactionManager.transaction.conn

  override def add(json: JsValue)(implicit txId: TransactionId): Future[JsValue] = {
    log.info(s"POSTGRES INSERT: $json", C.tag.DB)
    val run: QueryRunner = new QueryRunner

    val q: String = s"INSERT INTO $tableName (${C.Base.ID}) VALUES(?)"
    run.update(conn, q, (json \ C.Base.ID).as[String])

    Future[JsValue] {
      json
     }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsValue] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q: String = s"SELECT ${C.Base.ID} FROM $tableName WHERE ${C.Base.ID} = ?"
    val res = run.query(conn, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records", C.tag.DB)
    if (res.size() == 0)
      return Future[JsValue](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)
    Future[JsValue] {
      Json.obj(
        C.Base.ID -> rec.get(C.Base.ID).toString
      )
    }
  }
}