package altitude.dao.postgres


import java.sql.Connection

import altitude.dao.{BaseDao, TransactionId}
import altitude.models.BaseModel
import altitude.services.JdbcTransactionManager
import altitude.util.log
import altitude.{Const => C, Util}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import play.api.libs.json.{JsString, JsObject, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao(protected val tableName: String) extends BaseDao {

  protected def conn(implicit txId: TransactionId): Connection =
    JdbcTransactionManager.transaction.conn

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    log.info(s"POSTGRES INSERT: $jsonIn", C.tag.DB)
    val run: QueryRunner = new QueryRunner

    // append the id
    val id = BaseModel.genId

    val createdAt: String = Util.isoDateTime(Some(Util.utcNow))

    val q: String = s"INSERT INTO $tableName (${C.Base.ID}, ${C.Base.CREATED_AT}) VALUES(?, ?)"
    run.update(conn, q, id, createdAt)

    val json: JsObject = jsonIn ++ JsObject(Seq(
      C.Base.ID -> JsString(BaseModel.genId),
      C.Base.CREATED_AT -> JsString(createdAt)
    ))

    Future[JsObject] {
      json
     }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q: String = s"SELECT ${C.Base.ID} FROM $tableName WHERE ${C.Base.ID} = ?"
    val res = run.query(conn, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records", C.tag.DB)
    if (res.size() == 0)
      return Future[JsObject](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)
    Future[JsObject] {
      Json.obj(
        C.Base.ID -> rec.get(C.Base.ID).toString
      )
    }
  }
}