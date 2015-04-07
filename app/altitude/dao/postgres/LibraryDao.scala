package altitude.dao.postgres

import altitude.models.Asset
import altitude.{Const => C}
import altitude.dao.TransactionId
import altitude.util.log
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override def add(json: JsValue)(implicit txId: TransactionId): Future[JsValue] = {
    val asset = json: Asset
    log.info(s"POSTGRES ASSET INSERT: $asset", C.tag.DB)
    val run: QueryRunner = new QueryRunner

    val q: String = s"""
         INSERT INTO $tableName (id, media_type, media_subtype) VALUES(?, ?, ?)
    """

    run.update(conn, q,
      asset.id, asset.mediaType.mediaType, asset.mediaType.mediaSubtype)

    Future[JsValue] {
      json
    }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsValue] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q: String = "SELECT id, media_type, media_subtype FROM asset WHERE id = ?"
    val res = run.query(conn, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records", C.tag.DB)
    if (res.size() == 0)
      return Future[JsValue](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)
    Future[JsValue] {
      Json.obj(
        C.Common.ID -> rec.get("id").toString,
        C.Asset.MEDIA_TYPE -> rec.get("media_type").toString,
        C.Asset.MEDIA_SUBTYPE -> rec.get("media_subtype").toString
      )
    }
  }
}
