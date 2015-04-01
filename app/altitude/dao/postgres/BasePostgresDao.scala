package altitude.dao.postgres


import altitude.dao.{JdbcTransaction, BaseDao}
import altitude.util.log
import altitude.{Const => C}
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao(private val tableName: String) extends BaseDao {

  override def add(json: JsValue)(implicit tx: Option[JdbcTransaction]): Future[JsValue] = {
    log.info("POSTGRES INSERT")

    val run: QueryRunner = new QueryRunner

    val q: String = "INSERT INTO asset (id) VALUES(?)"
    run.update(tx.get.getConnection, q, (json \ "id").as[String])

    Future[JsValue] {
      json
     }
  }

  override def getById(id: String)(implicit tx: Option[JdbcTransaction]): Future[JsValue] = {
    log.info("POSTGRES SELECT")
    val run: QueryRunner = new QueryRunner()

    val q: String = "SELECT id FROM asset WHERE id = ?"
    val res = run.query(tx.get.getConnection, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records")
    if (res.size() == 0)
      return Future[JsValue](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)
    Future[JsValue] {
      Json.obj(
        C.Common.ID -> rec.get("id").toString
      )
    }
  }
}